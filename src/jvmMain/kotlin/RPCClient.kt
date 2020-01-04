package dk.thrane.playground

import dk.thrane.playground.serialization.MessageFormat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JVMWSConnection(
    hostname: String,
    port: Int,
    path: String = "/"
) : WSConnection {
    private val requestIdCounter = AtomicInteger(0)

    // A mutex for sending messages. sendFrames() requires that all messages are sent in a single transaction.
    private val sendMutex = Mutex()

    private val closeHandlerMutex = Mutex()
    private val onCloseHandlers = ArrayList<suspend () -> Unit>()

    private val subscriptions = HashMap<Int, MessageSubscription<*, *>>()
    private var currentResponseHeader: ResponseHeader? = null

    // A mutex for manipulating general state of this class
    private val mutex = Mutex()
    // Contains a list of continuations that are waiting for a the connection to open
    private val awaitOpenContinuations = ArrayList<Continuation<Unit>>()
    // A simple flag determining if we are currently open
    private var isOpen = false

    private val wsClient = WebSocketClient(
        hostname,
        port,
        path = path,
        callbackHandler = object : WebSocketClientCallbacks {
            override suspend fun onOpen(client: WebSocketClient) {
                mutex.withLock {
                    isOpen = true
                    awaitOpenContinuations.forEach {
                        it.resume(Unit)
                    }

                    awaitOpenContinuations.clear()
                }
            }

            override suspend fun onClose(client: WebSocketClient) {
                mutex.withLock {
                    closeHandlerMutex.withLock {
                        isOpen = false
                        onCloseHandlers.forEach { it() }
                    }
                }
            }

            override suspend fun handleBinaryFrame(client: WebSocketClient, frame: ByteArray) {
                val capturedResponseHandler = currentResponseHeader
                if (capturedResponseHandler == null) {
                    // TODO We need error handling for this
                    currentResponseHeader = MessageFormat.load(ResponseHeader.serializer(), frame)
                }

                val newCapturedResponseHandler = currentResponseHeader
                if ((capturedResponseHandler != null && newCapturedResponseHandler != null) ||
                    (newCapturedResponseHandler != null && !newCapturedResponseHandler.hasBody)
                ) {
                    val requestId = newCapturedResponseHandler.requestId
                    val statusCode = ResponseCode.valueOf(newCapturedResponseHandler.statusCode)
                    @Suppress("UNCHECKED_CAST")
                    val handler = subscriptions[requestId] as MessageSubscription<Any?, Any?>?

                    if (handler != null) {
                        val body = if (newCapturedResponseHandler.hasBody) {
                            MessageFormat.load(handler.rpc.responseSerializer, frame)
                        } else {
                            null
                        }

                        val result = if (statusCode == ResponseCode.OK) {
                            Result.Success(body)
                        } else {
                            @Suppress("ThrowableNotThrown")
                            Result.Failure<Any?>(RPCException(statusCode, statusCode.name))
                        }
                        handler.handler(newCapturedResponseHandler, result)
                    } else {
                        log.debug("Couldn't find handler for response!")
                    }

                    currentResponseHeader = null
                }
            }
        }
    )

    init {
        wsClient.start()
    }

    override suspend fun awaitOpen() {
        return suspendCoroutine { cont ->
            runBlocking {
                mutex.withLock {
                    if (isOpen) cont.resume(Unit)
                    awaitOpenContinuations.add(cont)
                }
            }
        }
    }

    override fun isOpen(): Boolean = isOpen

    override suspend fun sendFrames(frames: List<ByteArray>) {
        sendMutex.withLock {
            frames.forEach { buffer ->
                wsClient.sendBinaryFrame(buffer)
            }
        }
    }

    override suspend fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: suspend (header: ResponseHeader, Result<Res>) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        subscriptions[requestId] = MessageSubscription(rpc, handler)
    }

    override suspend fun removeSubscription(requestId: Int) {
        subscriptions.remove(requestId)
    }

    override suspend fun addOnCloseHandler(handler: suspend () -> Unit) {
        closeHandlerMutex.withLock {
            onCloseHandlers.add(handler)
        }
    }

    override suspend fun removeOnCloseHandler(onClose: suspend () -> Unit) {
        closeHandlerMutex.withLock {
            onCloseHandlers.remove(onClose)
        }
    }

    override suspend fun retrieveRequestId(): Int = requestIdCounter.getAndIncrement()

    companion object {
        private val log = Log("RPCClient")
    }
}
