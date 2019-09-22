package dk.thrane.playground

import dk.thrane.playground.edu.CourseController
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlin.collections.ArrayList

object ConnectionController : Controller() {
    private val knownConnections = HashMap<String, Set<Int>>()

    override fun configureController() {
        implement(Connections.open) {
            synchronized(knownConnections) {
                val current = knownConnections[socketId] ?: emptySet()
                knownConnections[socketId] = current + request[OpenConnectionSchema.id]
            }

            respond { }
        }

        implement(Connections.close) {
            synchronized(knownConnections) {
                val current = knownConnections[socketId] ?: emptySet()
                val newSet = current - request[CloseConnectionSchema.id]
                if (newSet.isNotEmpty()) {
                    knownConnections[socketId] = newSet
                } else {
                    knownConnections.remove(socketId)
                }
            }

            respond { }
        }
    }

    internal val prehandler: PreHandler = handler@{ rpc, message ->
        val connectionId = message[EmptyRequestSchema.connectionId]

        // We consider conn 0 to be implicitly initialized by WebSocket open
        if (connectionId == 0) return@handler PreHandlerAction.Continue

        val requestId = message[EmptyRequestSchema.requestId]
        val socketId = socketId

        val connsForSocket = knownConnections[socketId] ?: emptySet()
        if (connectionId !in connsForSocket) {
            PreHandlerAction.Terminate(
                EmptyRPC.outgoingResponse(
                    connectionId,
                    requestId,
                    ResponseCode.BAD_REQUEST,
                    BoundOutgoingMessage(EmptySchema)
                )
            )
        } else {
            PreHandlerAction.Continue
        }
    }
}

sealed class PreHandlerAction {
    object Continue : PreHandlerAction()
    class Terminate(val responseMessage: BoundOutgoingMessage<ResponseSchema<EmptySchema>>) : PreHandlerAction()
}

typealias PreHandler = HttpClient.(
    rpc: RPC<*, *>?,
    message: BoundMessage<RequestSchema<EmptySchema>>
) -> PreHandlerAction

typealias PostHandler = HttpClient.(
    rpc: RPC<*, *>?,
    message: BoundMessage<RequestSchema<EmptySchema>>
) -> Unit

abstract class BaseServer : HttpRequestHandler, WebSocketRequestHandler {
    private val controllers = ArrayList<Controller>()
    private val preHandlers = ArrayList<PreHandler>()
    private val postHandlers = ArrayList<PostHandler>()

    init {
        addController(ConnectionController)
        addMiddlewarePreHandling(ConnectionController.prehandler)
    }

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

        var connectionId: Int? = null
        var requestId: Int? = null
        var rpcUsedForHandling: RPC<*, *>? = null
        var didHandleMessage = false
        val requestMessage = BoundMessage<RequestSchema<EmptySchema>>(message)

        try {
            val requestName = requestMessage[EmptyRequestSchema.requestName]
            requestId = requestMessage[EmptyRequestSchema.requestId]
            connectionId = requestMessage[EmptyRequestSchema.connectionId]

            // TODO We need a unified place to set connection id and request id
            controllers@ for (controller in controllers) {
                println("$controller and $requestName")
                val (rpc, handler) = controller.findHandler(requestName) ?: continue
                rpcUsedForHandling = rpc

                for (preHandler in preHandlers) {
                    when (val handlerAction = preHandler(rpc, requestMessage)) {
                        PreHandlerAction.Continue -> {
                            // Do nothing (Continue to next handler)
                        }

                        is PreHandlerAction.Terminate -> {
                            didHandleMessage = true
                            sendMessage(handlerAction.responseMessage.build())
                            break@controllers
                        }
                    }
                }

                @Suppress("UNCHECKED_CAST")
                // These casts are non-sense but it doesn't really matter since we enforce type safety through the
                // controller interface
                handleRPC(
                    connectionId,
                    requestId,
                    rpc as RPC<EmptySchema, EmptySchema>,
                    handler as RPCHandler<EmptySchema, EmptySchema>,
                    requestMessage
                )
                break
            }
        } catch (ex: RPCException) {
            ex.printStackTrace()
            sendMessage(
                EmptyRPC.outgoingResponse(
                    connectionId ?: -1,
                    requestId ?: -1,
                    ResponseCode.BAD_REQUEST,
                    BoundOutgoingMessage(EmptySchema)
                ).build()
            )
            return
        }

        if (!didHandleMessage) {
            var didSendPreHandler = false
            handlers@ for (preHandler in preHandlers) {
                when (val handlerAction = preHandler(null, requestMessage)) {
                    PreHandlerAction.Continue -> {
                        // Do nothing (Continue to next handler)
                    }

                    is PreHandlerAction.Terminate -> {
                        sendMessage(handlerAction.responseMessage.build())
                        didSendPreHandler = true
                        break@handlers
                    }
                }
            }

            if (!didSendPreHandler) {
                val outgoing = EmptyRPC.outgoingResponse(
                    connectionId!!,
                    requestId!!,
                    ResponseCode.NOT_FOUND,
                    BoundOutgoingMessage(EmptySchema)
                )

                sendMessage(outgoing.build())
            }
        }

        for (postHandler in postHandlers) {
            postHandler(rpcUsedForHandling, requestMessage)
        }
    }

    private fun HttpClient.sendMessage(outgoing: ObjectField) {
        defaultBufferPool.useInstance { buffer ->
            val out = ByteOutStreamJVM(buffer)
            writeMessage(out, outgoing)
            sendWebsocketFrame(WebSocketOpCode.BINARY, buffer, 0, out.ptr)
        }
    }

    private fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> HttpClient.handleRPC(
        connectionId: Int,
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: RPCHandler<Req, Res>,
        requestMessage: BoundMessage<RequestSchema<Req>>
    ) {
        try {
            val payload = requestMessage[rpc.request.payload]
            val authorization = requestMessage[rpc.request.authorization]

            println("Handling $rpc ($requestId) with $payload")
            val (statusCode, response) = RPCHandlerContext(payload, authorization, socketId, rpc).handler()
            val outgoing = rpc.outgoingResponse(connectionId, requestId, statusCode, response)

            sendMessage(outgoing.build())
        } catch (ex: Throwable) {
            val statusCode = if (ex is RPCException) {
                ex.statusCode
            } else {
                ex.printStackTrace()
                ResponseCode.INTERNAL_ERROR
            }

            val outgoing =
                EmptyRPC.outgoingResponse(connectionId, requestId, statusCode, BoundOutgoingMessage(EmptySchema))
            sendMessage(outgoing.build())
        }
    }
}

/**
 * An empty [RPC] used for generating responses without a payload. This is useful for errors.
 */
private val EmptyRPC = RPC("~empty~", "~empty~", EmptySchema, EmptySchema)

class RPCHandlerContext<Req : MessageSchema<Req>, Res : MessageSchema<Res>>(
    val request: BoundMessage<Req>,
    val authorization: String?,
    val socketId: String,
    val rpc: RPC<Req, Res>
)

class RespondingContext<Res : MessageSchema<Res>>(
    val schema: Res,
    val message: BoundOutgoingMessage<Res>
)

inline fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> RPCHandlerContext<Req, Res>.respond(
    code: ResponseCode = ResponseCode.OK,
    builder: RespondingContext<Res>.() -> Unit
): Pair<ResponseCode, BoundOutgoingMessage<Res>> {
    val message = BoundOutgoingMessage(rpc.responsePayload)
    RespondingContext(rpc.responsePayload, message).builder()
    return Pair(code, message)
}

typealias RPCHandler<Req, Res> = RPCHandlerContext<Req, Res>.() -> Pair<ResponseCode, BoundOutgoingMessage<Res>>
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

class TestServer : BaseServer() {
    init {
        addController(CourseController())
    }
}

fun main(args: Array<String>) {
    val server = TestServer()
    val dbPool = ConnectionPool("org.h2.Driver", "jdbc:h2:mem:data;DB_CLOSE_DELAY=-1")

    val migrations = MigrationHandler(dbPool)
    migrations.addScript("test") { conn ->
        conn.prepareStatement("create table foo(bar int);").executeUpdate()
    }

    if ("--migrate" in args || true) {
        migrations.runMigrations()
    }

    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
