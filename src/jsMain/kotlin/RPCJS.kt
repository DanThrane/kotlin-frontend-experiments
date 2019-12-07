package dk.thrane.playground

import kotlinx.coroutines.*
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import kotlin.browser.window
import kotlin.js.Promise

class WSConnection internal constructor(
    location: String,
    private val scope: CoroutineScope
) {
    private val socket: WebSocket = WebSocket(location)
    private val subscriptions = HashMap<Int, suspend (Result<BoundMessage<*>>) -> Unit>()
    private val onCloseHandlers = ArrayList<suspend () -> Unit>()
    private var onOpenPromise: Promise<Unit>

    init {
        socket.binaryType = BinaryType.ARRAYBUFFER

        onOpenPromise = Promise { resolve, _ ->
            socket.addEventListener("open", {
                resolve(Unit)
            })
        }

        socket.addEventListener("message", { e ->
            val data = (e as MessageEvent).data as ArrayBuffer

            val stream = ByteStreamJS(Int8Array(data).unsafeCast<ByteArray>())
            val parseMessage = parseMessage(stream) as ObjectField
            val boundResponse = BoundMessage<ResponseSchema<EmptySchema>>(parseMessage)

            val requestId = boundResponse[EmptyResponseSchema.requestId]
            val statusCode = ResponseCode.values().find {
                it.statusCode == boundResponse[EmptyResponseSchema.statusCode]
            }!!

            val result = if (statusCode == ResponseCode.OK) {
                Result.success(boundResponse as BoundMessage<*>)
            } else {
                @Suppress("ThrowableNotThrown")
                Result.failure(RPCException(statusCode, statusCode.name))
            }

            scope.launch {
                subscriptions[requestId]?.invoke(result)
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

    fun <R : MessageSchema<R>> addSubscription(
        requestId: Int,
        handler: suspend (Result<BoundMessage<RequestSchema<R>>>) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        subscriptions[requestId] = handler as suspend (Result<BoundMessage<*>>) -> Unit
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

        println("Going in the queue!!!!")
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
                buildOutgoing(OpenConnectionSchema) {
                    it[OpenConnectionSchema.id] = vc.id
                }
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
                            buildOutgoing(CloseConnectionSchema) { it[CloseConnectionSchema.id] = vc.id }
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

suspend fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> RPC<Req, Res>.call(
    pool: WSConnectionPool,
    message: BoundOutgoingMessage<Req>,
    vc: VirtualConnection = STATELESS_CONNECTION,
    auth: String? = null
): BoundMessage<Res> {
    return pool.useConnection(vc) { conn ->
        call(conn.copy(authorization = auth), message)
    }
}

suspend fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> RPC<Req, Res>.call(
    connectionWithAuth: ConnectionWithAuthorization,
    message: BoundOutgoingMessage<Req>
): BoundMessage<Res> {
    val start = window.performance.now()
    console.log("Calling --> ${this.requestName}", message)
    val (connection, virtualConnection, auth) = connectionWithAuth
    val requestId = requestIdCounter++
    val stream = ByteOutStreamJS(Uint8Array(1024 * 64))
    writeMessage(stream, outgoingRequest(virtualConnection.id, requestId, auth, message).build())

    return Promise<BoundMessage<Res>> { resolve, reject ->
        connection.addSubscription<Res>(requestId) { result ->
            val time = window.performance.now() - start
            if (result.isFailure) {
                val exception = result.exceptionOrNull() as RPCException
                console.log("[${exception.statusCode}] --> ${this.requestName} ($time ms)", exception)
                reject(exception)
            } else {
                @Suppress("UNCHECKED_CAST")
                val responseMessage = result.getOrNull()!! as BoundMessage<ResponseSchema<Res>>
                val responseCode = ResponseCode.valueOf(responseMessage[response.statusCode])
                console.log("[$responseCode] ${this.requestName} ($time ms)", responseMessage[response.response])
                resolve(responseMessage[response.response])
            }

            connection.removeSubscription(requestId)
        }

        connection.send(stream.viewMessage())
    }.await()
}
