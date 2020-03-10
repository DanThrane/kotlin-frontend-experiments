package dk.thrane.playground

import dk.thrane.playground.serialization.MessageFormat
import kotlinx.serialization.KSerializer
import java.io.File

sealed class PreHandlerAction {
    object Continue : PreHandlerAction()
    class Terminate(val code: ResponseCode) : PreHandlerAction()
}

typealias PreHandler = AsyncHttpClientSession.(
    rpc: RPC<*, *>?,
    header: RequestHeader,
    requestPayload: Any?
) -> PreHandlerAction

typealias PostHandler = AsyncHttpClientSession.(
    rpc: RPC<*, *>?,
    requestPayload: Any?
) -> Unit

abstract class BaseServer : AsyncHttpRequestHandler, AsyncWebSocketRequestHandler {
    private val controllers = ArrayList<Controller>()
    private val preHandlers = ArrayList<PreHandler>()
    private val postHandlers = ArrayList<PostHandler>()

    protected fun addController(controller: Controller) {
        controllers.add(controller)
        controller.configure()
    }

    protected fun addMiddlewarePreHandling(handler: PreHandler) {
        preHandlers.add(handler)
    }

    protected fun addMiddlewarePostHandling(handler: PostHandler) {
        postHandlers.add(handler)
    }

    override suspend fun AsyncHttpClientSession.handleRequest(method: HttpMethod, path: String) {
        val rootDir = File(".").normalize().absoluteFile
        val rootDirPath = rootDir.absolutePath + "/"

        if (path == "/favicon.ico") {
            sendHttpResponse(404, defaultHeaders())
        } else if (path.startsWith("/assets/")) {
            val file = File(rootDir, path)
                .normalize()
                .takeIf { it.absolutePath.startsWith(rootDirPath) && it.exists() && it.isFile }

            if (file == null) {
                sendHttpResponse(404, defaultHeaders())
            } else {
                sendHttpResponseWithFile(file)
            }
        } else if (path.startsWith("/src/")) {
            val file = File(File(rootDir, "assets"), path)
                .normalize()
                .takeIf { it.absolutePath.startsWith(rootDirPath) && it.exists() && it.isFile }

            if (file == null) {
                sendHttpResponse(404, defaultHeaders())
            } else {
                sendHttpResponseWithFile(file)
            }
        } else {
            sendHttpResponseWithFile(File(rootDir, "index.html"))
        }
    }

    private suspend fun AsyncHttpClientSession.handleRequestFrame(frame: ByteArray) {
        currentRequestHeader = try {
            MessageFormat.load(RequestHeader.serializer(), frame)
        } catch (ex: Throwable) {
            log.warn("Caught an exception parsing message")
            log.warn(ex.stackTraceToString())

            outs.sendWebsocketFrame(WebSocketOpCode.CONNECTION_CLOSE, ByteArray(0))
            closing = true
            return
        }
    }

    private suspend fun AsyncHttpClientSession.handleRequestBodyFrame(frame: ByteArray?) {
        val requestHeader = currentRequestHeader
        requireNotNull(requestHeader)

        var rpcUsedForHandling: RPC<*, *>? = null
        var didHandleMessage = false

        try {
            val foundHandler: Pair<RPC<*, *>, UntypedRPChandler>? =
                controllers
                    .asSequence()
                    .mapNotNull { it.findHandler(requestHeader.requestName) }
                    .take(1)
                    .toList()
                    .singleOrNull()

            val requestMessage = if (foundHandler?.first != null && requestHeader.hasBody) {
                require(frame != null)

                val rpc = foundHandler.first
                try {
                    MessageFormat.load(rpc.requestSerializer, frame)
                } catch (ex: Throwable) {
                    log.debug(ex.stackTraceToString())
                    throw RPCException(ResponseCode.BAD_REQUEST, "Invalid request message")
                }
            } else {
                null
            }

            val didPreHandlerTerminate = run {
                for (preHandler in preHandlers) {
                    when (val handlerAction = preHandler(foundHandler?.first, requestHeader, requestMessage)) {
                        PreHandlerAction.Continue -> {
                            // Do nothing (Continue to next handler)
                        }

                        is PreHandlerAction.Terminate -> {
                            didHandleMessage = true
                            sendMessage(
                                ResponseHeader(
                                    requestHeader.requestId,
                                    handlerAction.code.statusCode,
                                    false
                                ),
                                ResponseHeader.serializer()
                            )
                            return@run true
                        }
                    }
                }

                false
            }

            if (foundHandler != null && !didPreHandlerTerminate) {
                val (rpc, handler) = foundHandler
                rpcUsedForHandling = rpc

                if (!didPreHandlerTerminate) {
                    @Suppress("UNCHECKED_CAST")
                    // These casts are non-sense but it doesn't really matter since we enforce type safety through the
                    // controller interface
                    handleRPC(
                        requestHeader,
                        rpc as RPC<Any?, Any?>,
                        handler as RPCHandler<Any?, Any?>,
                        requestMessage
                    )

                    didHandleMessage = true
                }
            }
        } catch (ex: RPCException) {
            ex.printStackTrace()
            didHandleMessage = true
            sendMessage(
                ResponseHeader(
                    requestHeader.requestId,
                    ResponseCode.BAD_REQUEST.statusCode,
                    false
                ),
                ResponseHeader.serializer()
            )
        }

        if (!didHandleMessage) {
            sendMessage(
                ResponseHeader(
                    requestHeader.requestId,
                    ResponseCode.NOT_FOUND.statusCode,
                    false
                ),
                ResponseHeader.serializer()
            )
        }

        for (postHandler in postHandlers) {
            try {
                postHandler(rpcUsedForHandling, requestHeader)
            } catch (ex: Throwable) {
                log.info("Post handler threw an exception!")
                log.info(ex.stackTraceToString())
            }
        }
    }

    override suspend fun AsyncHttpClientSession.handleBinaryFrame(frame: ByteArray) {
        if (currentRequestHeader == null) {
            handleRequestFrame(frame)

            if (currentRequestHeader?.hasBody == false) {
                try {
                    handleRequestBodyFrame(null)
                } finally {
                    currentRequestHeader = null
                }
            }
        } else {
            try {
                handleRequestBodyFrame(frame)
            } finally {
                currentRequestHeader = null
            }
        }
    }

    private suspend fun <T> AsyncHttpClientSession.sendMessage(message: T, serializer: KSerializer<T>) {
        outs.sendWebsocketFrame(WebSocketOpCode.BINARY, MessageFormat.dump(serializer, message))
    }

    private suspend fun <Req, Res> AsyncHttpClientSession.handleRPC(
        header: RequestHeader,
        rpc: RPC<Req, Res>,
        handler: RPCHandler<Req, Res>,
        payload: Req
    ) {
        val start = System.nanoTime()
        try {
            val authorization = header.authorization.takeIf { !it.isNullOrBlank() }

            log.info("$rpc requestId=${header.requestId} payload=$payload")
            val (statusCode, response) = handler(
                RPCHandlerContextImpl(
                    payload,
                    authorization,
                    socketId,
                    rpc
                )
            )

            sendMessage(
                ResponseHeader(header.requestId, statusCode.statusCode, true),
                ResponseHeader.serializer()
            )
            sendMessage(response, rpc.responseSerializer)
        } catch (ex: Throwable) {
            val statusCode = if (ex is RPCException) {
                ex.statusCode
            } else {
                ex.printStackTrace()
                ResponseCode.INTERNAL_ERROR
            }

            sendMessage(
                ResponseHeader(header.requestId, statusCode.statusCode, false),
                ResponseHeader.serializer()
            )
        }

        val end = System.nanoTime()
        log.debug("$rpc requestId=${header.requestId} took ${end - start} nanos")
    }

    companion object {
        private val log = Log("BaseServer")
    }
}
