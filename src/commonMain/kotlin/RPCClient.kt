package dk.thrane.playground

import dk.thrane.playground.serialization.MessageFormat
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
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

data class ConnectionWithAuthorization internal constructor(
    val connection: Connection,
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
    private val pool = Array<PooledConnection?>(poolSize) { null }
    private val queue = ArrayList<Continuation<Pair<Int, WSConnection>>>()

    suspend fun borrowConnection(): Pair<Int, WSConnection> {
        for (idx in pool.indices) {
            val pooledConnection = pool[idx]

            when {
                pooledConnection == null -> {
                    val connection = connectionFactory()
                    connection.awaitOpen()

                    pool[idx] = PooledConnection(connection, true)
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

    companion object {
        private data class PooledConnection(
            val conn: WSConnection,
            var inUse: Boolean
        )
    }
}

suspend fun <R> WSConnectionPool.useConnection(
    authorization: String? = null,
    block: suspend (ConnectionWithAuthorization) -> R
): R {
    val (idx, conn) = borrowConnection()
    return try {
        block(ConnectionWithAuthorization(conn, authorization))
    } finally {
        returnConnection(idx)
    }
}

suspend fun <Req, Res> RPC<Req, Res>.call(
    pool: WSConnectionPool,
    message: Req,
    auth: String? = null
): Res {
    return pool.useConnection { conn ->
        call(conn.copy(authorization = auth), message)
    }
}
