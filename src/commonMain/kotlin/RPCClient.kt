package dk.thrane.playground

import dk.thrane.playground.serialization.MessageFormat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.MonoClock

data class MessageSubscription<Req, Res>(
    val rpc: RPC<Req, Res>,
    val handler: suspend (header: ResponseHeader, response: RPCResult<Res>) -> Unit
)

sealed class RPCResult<T> {
    data class Success<T>(val result: T) : RPCResult<T>()
    data class Failure<T>(val exception: Throwable) : RPCResult<T>()

    companion object {
        inline fun <T> runCode(block: () -> T): RPCResult<T> {
            return try {
                Success(block())
            } catch (ex: Throwable) {
                Failure(ex)
            }
        }
    }
}

abstract class WSConnection : Connection {
    abstract suspend fun awaitOpen()
    abstract fun isOpen(): Boolean

    abstract suspend fun sendFrames(frames: List<ByteArray>)

    abstract suspend fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: suspend (header: ResponseHeader, RPCResult<Res>) -> Unit
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
        val requestId = header.requestId
        var handler: (suspend (header: ResponseHeader, RPCResult<Response>) -> Unit)? = null
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
                    is RPCResult.Success -> cont.resume(result.result)
                    is RPCResult.Failure -> cont.resumeWithException(result.exception)
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

expect fun <Res> RPC<*, Res>.logCallEnded(result: RPCResult<Res>, duration: Duration)

private val rpcClientLog = Log("RPCCall")

interface Connection {
    val withoutAuthentication: ConnectionWithAuthorization
        get() = ConnectionWithAuthorization(this)

    suspend fun retrieveRequestId(): Int

    suspend fun <Request, Response> call(
        rpc: RPC<Request, Response>,
        connectionWithAuth: ConnectionWithAuthorization,
        header: RequestHeader,
        request: Request
    ): Response
}

typealias Authenticator = suspend (connection: Connection) -> ConnectionWithAuthorization

suspend fun <Req, Res> RPC<Req, Res>.call(
    connection: Connection,
    authenticator: Authenticator,
    message: Req
): Res {
    val connWithAuth = authenticator(connection)
    return call(connWithAuth, message)
}

suspend fun <Req, Res> RPC<Req, Res>.call(
    connectionWithAuth: ConnectionWithAuthorization,
    message: Req
): Res {
    val start = MonoClock.markNow()
    logCallStarted(message)

    val requestHeader = RequestHeader(
        connectionWithAuth.connection.retrieveRequestId(),
        requestName,
        true,
        connectionWithAuth.authorization
    )

    val result = RPCResult.runCode {
        connectionWithAuth.connection.call(this, connectionWithAuth, requestHeader, message)
    }

    val duration = start.elapsedNow()
    logCallEnded(result, duration)

    when (result) {
        is RPCResult.Success -> return result.result
        is RPCResult.Failure -> throw result.exception
    }
}
