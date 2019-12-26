package dk.thrane.playground

import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

data class MessageSubscription<Req, Res>(
    val rpc: RPC<Req, Res>,
    val handler: (header: ResponseHeader, response: Result<Res>) -> Unit
)

interface WSConnection {
    suspend fun awaitOpen()
    fun isOpen(): Boolean

    suspend fun send(buffer: ByteArray)

    fun <Req, Res> addSubscription(
        requestId: Int,
        rpc: RPC<Req, Res>,
        handler: (header: ResponseHeader, Result<Res>) -> Unit
    )

    fun removeSubscription(requestId: Int)

    fun addOnCloseHandler(handler: suspend () -> Unit)
    fun removeOnCloseHandler(onClose: suspend () -> Unit)

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

@UseExperimental(ExperimentalTime::class)
expect fun <Res> RPC<*, Res>.logCallEnded(exception: RPCException?, response: Res?, duration: Duration)

@UseExperimental(ExperimentalTime::class)
suspend fun <Req, Res> RPC<Req, Res>.call(
    connectionWithAuth: ConnectionWithAuthorization,
    message: Req
): Res {
    // This method returns a deferred such that the connection can be returned quickly. We only need the connection
    // while we are sending data. The connection already handles multiple incoming responses.

    val start = MonoClock.markNow()
    logCallStarted(message)
    val (connection, virtualConnection, auth) = connectionWithAuth
    val requestId = connection.retrieveRequestId()

    var handler: ((header: ResponseHeader, Result<Res>) -> Unit)? = null
    connection.addSubscription(requestId, this) { header, result ->
        while (handler == null) {
            // Waiting for handler to be initialized (should be soon)
        }

        handler!!(header, result)
    }

    val obj = RequestHeader(virtualConnection.id, requestId, requestName, auth ?: "", true)
    connection.send(
        ProtoBuf.dump(
            RequestHeader.serializer(),
            obj
        )
    )

    connection.send(
        ProtoBuf.dump(
            requestSerializer,
            message
        )
    )

    return suspendCoroutine { cont ->
        handler = { header, result ->
            val time = start.elapsedNow()
            if (result.isFailure) {
                val exception = result.exceptionOrNull() as RPCException
                logCallEnded(exception, null, time)
                cont.resumeWithException(exception)
            } else {
                val responseMessage = result.getOrNull()!!
                logCallEnded(null, responseMessage, time)
                cont.resume(responseMessage)
            }

            connection.removeSubscription(requestId)
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
