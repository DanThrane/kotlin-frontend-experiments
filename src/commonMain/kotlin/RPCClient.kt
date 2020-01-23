package dk.thrane.playground

import dk.thrane.playground.serialization.MessageFormat
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

data class MessageSubscription<Req, Res>(
    val rpc: RPC<Req, Res>,
    val handler: suspend (header: ResponseHeader, response: Result<Res>) -> Unit
)

sealed class Result<T> {
    data class Success<T>(val result: T) : Result<T>()
    data class Failure<T>(val exception: Throwable) : Result<T>()
}

abstract class WSConnection : Connection {
    abstract suspend fun awaitOpen()
    abstract fun isOpen(): Boolean

    abstract suspend fun sendFrames(frames: List<ByteArray>)

    abstract suspend fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: suspend (header: ResponseHeader, Result<Res>) -> Unit
    )

    abstract suspend fun removeSubscription(requestId: Int)

    abstract suspend fun addOnCloseHandler(handler: suspend () -> Unit)
    abstract suspend fun removeOnCloseHandler(onClose: suspend () -> Unit)

    override suspend fun <Request, Response> call(
        rpc: RPC<Request, Response>,
        connectionWithAuth: ConnectionWithAuthorization,
        header: RequestHeader,
        request: Request
    ): Response {
        val requestId = retrieveRequestId()

        var handler: (suspend (header: ResponseHeader, Result<Response>) -> Unit)? = null
        addSubscription(requestId, rpc) { responseHeader, result ->
            rpcClientLog.debug("Waiting for handler")
            while (handler == null) {
                // Waiting for handler to be initialized (should be soon)
            }
            rpcClientLog.debug("Handler is ready")
            handler!!(responseHeader, result)
            rpcClientLog.debug("Called handler")
        }

        rpcClientLog.debug("Serializing header and body")
        val frames = listOf(
            MessageFormat.dump(
                RequestHeader.serializer(),
                header
            ),
            MessageFormat.dump(
                rpc.requestSerializer,
                request
            )
        )
        rpcClientLog.debug("Sending frames")
        sendFrames(frames)
        rpcClientLog.debug("Done")

        return suspendCoroutine { cont ->
            handler = { _, result ->
                removeSubscription(requestId)

                when (result) {
                    is Result.Success -> cont.resume(result.result)
                    is Result.Failure -> cont.resumeWithException(result.exception)
                }
            }
        }
    }
}

data class VirtualConnection(
    internal val id: Int,
    internal val autoReconnect: Boolean,
    internal val onOpen: suspend () -> Unit,
    internal val onClose: suspend () -> Unit
)

val STATELESS_CONNECTION = VirtualConnection(id = 0, autoReconnect = false, onOpen = {}, onClose = {})

data class VCWithAuth(val virtualConnection: VirtualConnection, val authorization: String? = null)

data class ConnectionWithAuthorization internal constructor(
    val connection: Connection,
    val virtualConnection: VirtualConnection = STATELESS_CONNECTION,
    val authorization: String? = null
)

expect fun <Req> RPC<Req, *>.logCallStarted(requestMessage: Req)

expect fun <Res> RPC<*, Res>.logCallEnded(result: Result<Res>, duration: Duration)

private val rpcClientLog = Log("RPCCall")

interface Connection {
    suspend fun retrieveRequestId(): Int

    suspend fun <Request, Response> call(
        rpc: RPC<Request, Response>,
        connectionWithAuth: ConnectionWithAuthorization,
        header: RequestHeader,
        request: Request
    ): Response
}

suspend fun <Req, Res> RPC<Req, Res>.call(
    connectionWithAuth: ConnectionWithAuthorization,
    message: Req
): Res {
    // TODO:
    //  This method returns a deferred such that the connection can be returned quickly. We only need the connection
    //  while we are sending data. The connection already handles multiple incoming responses.

    val start = MonoClock.markNow()
    logCallStarted(message)

    val requestHeader = RequestHeader(
        connectionWithAuth.virtualConnection.id,
        connectionWithAuth.connection.retrieveRequestId(),
        requestName,
        true,
        connectionWithAuth.authorization
    )

    val result = try {
        Result.Success(connectionWithAuth.connection.call(this, connectionWithAuth, requestHeader, message))
    } catch (ex: Throwable) {
        Result.Failure<Res>(ex)
    }

    val duration = start.elapsedNow()
    logCallEnded(result, duration)

    when (result) {
        is Result.Success -> return result.result
        is Result.Failure -> throw result.exception
    }
}

class WSConnectionPool(
    private val connectionFactory: () -> WSConnection,
    private val poolSize: Int = 4
) {
    private var connectionIdCounter: Int = 0
    private val pool = Array<PooledConnection?>(poolSize) { null }
    private val queue = ArrayList<Continuation<Pair<Int, WSConnection>>>()

    suspend fun borrowConnection(): Pair<Int, WSConnection> {
        for (idx in pool.indices) {
            val pooledConnection = pool[idx]

            when {
                pooledConnection == null -> {
                    val connection = connectionFactory()
                    connection.awaitOpen()

                    pool[idx] = PooledConnection(connection, true, HashSet())
                    return Pair(idx, connection)
                }

                !pooledConnection.conn.isOpen() -> {
                    pool[idx] = null
                    return borrowConnection()
                }

                !pooledConnection.inUse -> {
                    return Pair(idx, pooledConnection.conn)
                }
            }
        }

        return suspendCoroutine { cont ->
            queue.add(cont)
        }
    }

    fun returnConnection(idx: Int) {
        val conn = pool.getOrNull(idx) ?: return
        conn.inUse = false

        if (queue.isNotEmpty()) {
            val continuation = queue.removeAt(0)
            continuation.resume(Pair(idx, conn.conn))
        }
    }

    suspend fun openConnection(
        autoReconnect: Boolean = true,
        onOpen: suspend () -> Unit = {},
        onClose: suspend () -> Unit = {}
    ): VirtualConnection {
        val vc = VirtualConnection(
            id = connectionIdCounter++,
            autoReconnect = autoReconnect,
            onOpen = onOpen,
            onClose = onClose
        )

        return openConnection(vc)
    }

    private suspend fun openConnection(vc: VirtualConnection): VirtualConnection {
        val (idx, conn) = borrowConnection()
        return try {
            conn.addOnCloseHandler {
                vc.onClose()
                if (vc.autoReconnect) {
                    openConnection(vc)
                }
            }

            pool[idx]!!.virtualConnections += vc
            Connections.open.call(
                ConnectionWithAuthorization(conn),
                OpenConnectionSchema(vc.id)
            )
            vc.onOpen()

            vc
        } finally {
            returnConnection(idx)
        }
    }

    suspend fun closeConnection(vc: VirtualConnection) {
        for (conn in pool) {
            if (conn === null) continue

            val didRemove = conn.virtualConnections.remove(vc)
            if (didRemove) {
                if (conn.conn.isOpen()) {
                    try {
                        Connections.close.call(
                            ConnectionWithAuthorization(conn.conn),
                            CloseConnectionSchema(vc.id)
                        )
                    } finally {
                        vc.onClose()
                    }
                }

                conn.conn.removeOnCloseHandler(vc.onClose)
                break
            }
        }
    }

    companion object {
        private data class PooledConnection(
            val conn: WSConnection,
            var inUse: Boolean,
            val virtualConnections: MutableSet<VirtualConnection>
        )
    }
}

suspend fun <R> WSConnectionPool.useConnection(
    virtualConnection: VirtualConnection = STATELESS_CONNECTION,
    authorization: String? = null,
    block: suspend (ConnectionWithAuthorization) -> R
): R {
    val (idx, conn) = borrowConnection()
    return try {
        block(ConnectionWithAuthorization(conn, virtualConnection, authorization))
    } finally {
        returnConnection(idx)
    }
}

suspend fun <Req, Res> RPC<Req, Res>.call(
    pool: WSConnectionPool,
    message: Req,
    vc: VirtualConnection = STATELESS_CONNECTION,
    auth: String? = null
): Res {
    return pool.useConnection(vc) { conn ->
        call(conn.copy(authorization = auth), message)
    }
}
