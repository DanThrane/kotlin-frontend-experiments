package dk.thrane.playground

import java.io.File

class TestServer : HttpRequestHandler, WebSocketRequestHandler {
    private val controllers = ArrayList<Controller>()

    init {
        controllers.add(object : Controller() {
            override fun configureController() {
                implement(Dummy.test) { request ->
                    val text = request[TestMessage.text]

                    buildOutgoing(TestMessage) { out ->
                        out[TestMessage.text] = "Hello!$text"
                        out[TestMessage.messages] = listOf(1, 2, 3, 4)
                    }
                }
            }
        })

        controllers.forEach { it.configure() }
    }

    override fun HttpClient.handleRequest(method: HttpMethod, path: String) {
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
        } else {
            sendHttpResponseWithFile(File(rootDir, "index.html"))
        }
    }

    override fun HttpClient.handleBinaryFrame(frame: ByteArray) {
        val message = try {
            parseMessage(ByteStreamJVM(frame)) as ObjectField
        } catch (ex: Throwable) {
            println("Caught an exception parsing message")
            ex.printStackTrace()

            sendWebsocketFrame(WebSocketOpCode.CONNECTION_CLOSE, ByteArray(0))
            closing = true
            return
        }

        val requestMessage = BoundMessage<RequestSchema<EmptySchema>>(message)
        val requestName = requestMessage[EmptyRequestSchema.requestName]
        val requestId = requestMessage[EmptyRequestSchema.requestId]
        val authorization = requestMessage[EmptyRequestSchema.authorization]

        for (controller in controllers) {
            val (rpc, handler) = controller.findHandler(requestName) ?: continue

            @Suppress("UNCHECKED_CAST")
            // These casts are non-sense but it doesn't really matter since we enforce type safety through the
            // controller interface
            handleRPC(
                requestId,
                rpc as RPC<EmptySchema, EmptySchema>,
                handler as RPCHandler<EmptySchema, EmptySchema>,
                requestMessage
            )
            break
        }
    }

    private fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> HttpClient.handleRPC(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: RPCHandler<Req, Res>,
        requestMessage: BoundMessage<RequestSchema<Req>>
    ) {
        try {
            val payload = requestMessage[rpc.request.payload]
            val response = handler(payload)
            val outgoing = rpc.outgoingResponse(requestId, ResponseCode.OK, response)

            defaultBufferPool.useInstance { buffer ->
                val out = ByteOutStreamJVM(buffer)
                writeMessage(out, outgoing.build())
                sendWebsocketFrame(WebSocketOpCode.BINARY, buffer, 0, out.ptr)
            }
        } catch (ex: Throwable) {
            val statusCode = if (ex is RPCException) {
                ex.statusCode
            } else {
                ex.printStackTrace()
                ResponseCode.INTERNAL_ERROR
            }

            defaultBufferPool.useInstance { buffer ->
                val outgoing = BoundOutgoingMessage(EmptyResponseSchema)
                outgoing[EmptyResponseSchema.requestId] = requestId
                outgoing[EmptyResponseSchema.statusCode] = statusCode.statusCode
                val out = ByteOutStreamJVM(buffer)
                writeMessage(out, outgoing.build())
                sendWebsocketFrame(WebSocketOpCode.BINARY, buffer, 0, out.ptr)
            }
        }
    }
}

typealias RPCHandler<Req, Res> = (call: BoundMessage<Req>) -> BoundOutgoingMessage<Res>
typealias UntypedRPChandler = (call: BoundMessage<*>) -> BoundOutgoingMessage<*>

abstract class Controller {
    private var isConfiguring = true
    private val handlers = ArrayList<Pair<RPC<*, *>, UntypedRPChandler>>()

    fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> implement(
        rpc: RPC<Req, Res>,
        handler: RPCHandler<Req, Res>
    ) {
        check(isConfiguring)

        @Suppress("UNCHECKED_CAST")
        handlers.add(rpc to handler as UntypedRPChandler)
    }

    fun findHandler(requestName: String): Pair<RPC<*, *>, UntypedRPChandler>? {
        return handlers.find { it.first.requestName == requestName }
    }

    fun configure() {
        isConfiguring = true
        configureController()
        isConfiguring = false
    }

    protected abstract fun configureController()
}


fun main() {
    val server = TestServer()

    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
