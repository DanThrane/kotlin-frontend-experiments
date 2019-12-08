package dk.thrane.playground

import kotlinx.coroutines.*
import kotlinx.serialization.protobuf.ProtoBuf
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Int8Array
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import kotlin.browser.window
import kotlin.js.Promise
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

fun Int8Array.asByteArray(): ByteArray = unsafeCast<ByteArray>()

data class MessageSubscription<Req, Res>(
    val rpc: RPC<Req, Res>,
    val handler: (header: ResponseHeader, response: Result<Res>) -> Unit
)

class WSConnection internal constructor(
    location: String,
    private val scope: CoroutineScope
) {
    private val log = Log(this::class.js.name)
    private val socket: WebSocket = WebSocket(location)
    private val subscriptions = HashMap<Int, MessageSubscription<*, *>>()
    private val onCloseHandlers = ArrayList<suspend () -> Unit>()
    private var onOpenPromise: Promise<Unit>

    private var currentResponseHeader: ResponseHeader? = null

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
                this.currentResponseHeader = ProtoBuf.load(ResponseHeader.serializer(), frame)
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
                        ProtoBuf.load(handler.rpc.responseSerializer, frame)
                    } else {
                        null
                    }

                    val result = if (statusCode == ResponseCode.OK) {
                        Result.success(body)
                    } else {
                        @Suppress("ThrowableNotThrown")
                        Result.failure(RPCException(statusCode, statusCode.name))
                    }

                    handler.handler(newCapturedResponseHandler, result)
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

    suspend fun awaitOpen() {
        onOpenPromise.await()
        require(isOpen())
    }

    fun isOpen(): Boolean = socket.readyState == WebSocket.OPEN

    fun send(buffer: ArrayBufferView) {
        socket.send(buffer)
    }

    fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: (header: ResponseHeader, Result<Res>) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        subscriptions[requestId] = MessageSubscription(rpc, handler)
    }

    fun addOnCloseHandler(handler: suspend () -> Unit) {
        onCloseHandlers.add(handler)
    }

    fun removeSubscription(requestId: Int) {
        subscriptions.remove(requestId)
    }

    fun removeOnCloseHandler(onClose: suspend () -> Unit) {
        onCloseHandlers.remove(onClose)
    }
}

typealias QueuedPromise<T> = Pair<(T) -> Unit, (Throwable) -> Unit>

class WSConnectionPool(
    private val location: String,
    private val poolSize: Int = 4,
    private val scope: CoroutineScope = GlobalScope
) {
    private var connectionIdCounter: Int = 0
    private val pool = Array<PooledConnection?>(poolSize) { null }
    private val queue = ArrayList<QueuedPromise<Pair<Int, WSConnection>>>()

    suspend fun borrowConnection(): Pair<Int, WSConnection> {
        for (idx in pool.indices) {
            val pooledConnection = pool[idx]

            when {
                pooledConnection == null -> {
                    val connection = WSConnection(location, scope)
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

        return Promise<Pair<Int, WSConnection>> { resolve, reject ->
            queue.add(Pair(resolve, reject))
        }.await()
    }

    fun returnConnection(idx: Int) {
        val conn = pool.getOrNull(idx) ?: return
        conn.inUse = false

        if (queue.isNotEmpty()) {
            val (resolve, _) = queue.removeAt(0)
            resolve(Pair(idx, conn.conn))
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

data class VirtualConnection(
    internal val id: Int,
    internal val autoReconnect: Boolean,
    internal val onOpen: suspend () -> Unit,
    internal val onClose: suspend () -> Unit
)

val STATELESS_CONNECTION = VirtualConnection(id = 0, autoReconnect = false, onOpen = {}, onClose = {})

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

data class ConnectionWithAuthorization internal constructor(
    val connection: WSConnection,
    val virtualConnection: VirtualConnection = STATELESS_CONNECTION,
    val authorization: String? = null
)

private var requestIdCounter = 0

suspend fun <Req, Res> RPC<Req, Res>.call(
    pool: WSConnectionPool,
    message: Req,
    vc: VirtualConnection = STATELESS_CONNECTION,
    auth: String? = null
): Res {
    return pool.useConnection(vc) { conn ->
        call(conn.copy(authorization = auth), message)
    }.await()
}

fun <Req, Res> RPC<Req, Res>.call(
    connectionWithAuth: ConnectionWithAuthorization,
    message: Req
): Deferred<Res> {
    // This method returns a deferred such that the connection can be returned quickly. We only need the connection
    // while we are sending data. The connection already handles multiple incoming responses.

    val start = window.performance.now()
    console.log("Calling --> ${this.requestName}", message, start)
    val (connection, virtualConnection, auth) = connectionWithAuth
    val requestId = requestIdCounter++

    return Promise<Res> { resolve, reject ->
        connection.addSubscription(requestId, this) { header, result ->
            val time = window.performance.now() - start
            if (result.isFailure) {
                val exception = result.exceptionOrNull() as RPCException
                console.log("[${exception.statusCode}] --> ${this.requestName} ($time ms)", exception)
                reject(exception)
            } else {
                val responseMessage = result.getOrNull()!!
                val responseCode = ResponseCode.valueOf(header.statusCode)
                console.log("[$responseCode] ${this.requestName} ($time ms)", responseMessage)
                resolve(responseMessage)
            }

            connection.removeSubscription(requestId)
        }

        val obj = RequestHeader(virtualConnection.id, requestId, requestName, auth ?: "", true)

        connection.send(
            ProtoBuf.dump(
                RequestHeader.serializer(),
                obj
            ).unsafeCast<Int8Array>()
        )

        connection.send(
            ProtoBuf.dump(
                requestSerializer,
                message
            ).unsafeCast<Int8Array>()
        )
    }.asDeferred()
}
