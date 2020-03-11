package dk.thrane.playground

import dk.thrane.playground.serialization.MessageFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import kotlin.js.Promise

fun Int8Array.asByteArray(): ByteArray = unsafeCast<ByteArray>()

class JSWSConnection internal constructor(
    location: String,
    private val scope: CoroutineScope
) : WSConnection() {
    private val log = Log(this::class.js.name)
    private val socket: WebSocket = WebSocket(location)
    private val subscriptions = HashMap<Int, MessageSubscription<*, *>>()
    private val onCloseHandlers = ArrayList<suspend () -> Unit>()
    private var onOpenPromise: Promise<Unit>
    private var currentResponseHeader: ResponseHeader? = null
    private var requestIdCounter = 0

    init {
        socket.binaryType = BinaryType.ARRAYBUFFER

        onOpenPromise = Promise { resolve, _ ->
            socket.addEventListener("open", {
                resolve(Unit)
            })
        }

        socket.addEventListener("message", { e ->
            val frame = Int8Array((e as MessageEvent).data as ArrayBuffer).asByteArray()

            val capturedResponseHandler = currentResponseHeader
            if (capturedResponseHandler == null) {
                // TODO We need error handling for this
                this.currentResponseHeader = MessageFormat.load(ResponseHeader.serializer(), frame)
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
                        RPCResult.Success(body)
                    } else {
                        @Suppress("ThrowableNotThrown")
                        RPCResult.Failure<Any?>(RPCException(statusCode, statusCode.name))
                    }

                    GlobalScope.launch {
                        handler.handler(newCapturedResponseHandler, result)
                    }
                } else {
                    log.debug("Couldn't find handler for response!")
                }

                currentResponseHeader = null
            }
        })

        socket.addEventListener("close", { _ ->
            scope.launch {
                onCloseHandlers.forEach { it() }
            }
        })
    }

    override suspend fun retrieveRequestId(): Int = requestIdCounter++

    override suspend fun awaitOpen() {
        onOpenPromise.await()
        require(isOpen())
    }

    override fun isOpen(): Boolean = socket.readyState == WebSocket.OPEN

    override suspend fun sendFrames(frames: List<ByteArray>) {
        frames.forEach { buffer ->
            socket.send(buffer.unsafeCast<Int8Array>())
        }
    }

    override suspend fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: suspend (header: ResponseHeader, RPCResult<Res>) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        subscriptions[requestId] = MessageSubscription(rpc, handler)
    }

    override suspend fun removeSubscription(requestId: Int) {
        subscriptions.remove(requestId)
    }

    override suspend fun addOnCloseHandler(handler: suspend () -> Unit) {
        onCloseHandlers.add(handler)
    }

    override suspend fun removeOnCloseHandler(onClose: suspend () -> Unit) {
        onCloseHandlers.remove(onClose)
    }
}
