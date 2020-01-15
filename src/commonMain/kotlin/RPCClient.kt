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

interface WSConnection {
    suspend fun awaitOpen()
    fun isOpen(): Boolean

    suspend fun sendFrames(frames: List<ByteArray>)

    suspend fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: suspend (header: ResponseHeader, Result<Res>) -> Unit
    )

    suspend fun removeSubscription(requestId: Int)

    suspend fun addOnCloseHandler(handler: suspend () -> Unit)
    suspend fun removeOnCloseHandler(onClose: suspend () -> Unit)

    suspend fun retrieveRequestId(): Int
}

data class VirtualConnection(
    internal val id: Int,
    internal val autoReconnect: Boolean,
    internal val onOpen: suspend () -> Unit,
    internal val onClose: suspend () -> Unit
)

val STATELESS_CONNECTION = VirtualConnection(id = 0, autoReconnect = false, onOpen = {}, onClose = {})

data class ConnectionWithAuthorization internal constructor(
    val connection: WSConnection,
    val virtualConnection: VirtualConnection = STATELESS_CONNECTION,
    val authorization: String? = null
)

expect fun <Req> RPC<Req, *>.logCallStarted(requestMessage: Req)

expect fun <Res> RPC<*, Res>.logCallEnded(result: Result<Res>, duration: Duration)

private val rpcClientLog = Log("RPCCall")

suspend fun <Req, Res> RPC<Req, Res>.call(
    connectionWithAuth: ConnectionWithAuthorization,
    message: Req
): Res {
    // TODO:
    //  This method returns a deferred such that the connection can be returned quickly. We only need the connection
    //  while we are sending data. The connection already handles multiple incoming responses.

    val start = MonoClock.markNow()
    logCallStarted(message)
    val (connection, virtualConnection, auth) = connectionWithAuth
    val requestId = connection.retrieveRequestId()

    var handler: (suspend (header: ResponseHeader, Result<Res>) -> Unit)? = null
    connection.addSubscription(requestId, this) { header, result ->
        rpcClientLog.debug("Waiting for handler")
        while (handler == null) {
            // Waiting for handler to be initialized (should be soon)
        }
        rpcClientLog.debug("Handler is ready")
        handler!!(header, result)
        rpcClientLog.debug("Called handler")
    }

    rpcClientLog.debug("Serializing header and body")
    val obj = RequestHeader(virtualConnection.id, requestId, requestName, true, auth)
    val frames = listOf(
        MessageFormat.dump(
            RequestHeader.serializer(),
            obj
        ),
        MessageFormat.dump(
            requestSerializer,
            message
        )
    )
    rpcClientLog.debug("Sending frames")
    connection.sendFrames(frames)
    rpcClientLog.debug("Done")

    return suspendCoroutine { cont ->
        handler = { header, result ->
            val duration = start.elapsedNow()
            logCallEnded(result, duration)
            connection.removeSubscription(requestId)

            when (result) {
                is Result.Success -> cont.resume(result.result)
                is Result.Failure -> cont.resumeWithException(result.exception)
            }
        }
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
