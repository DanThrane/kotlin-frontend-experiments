package dk.thrane.playground.psql

import dk.thrane.playground.Log
import dk.thrane.playground.io.*
import dk.thrane.playground.serialization.OutputBuffer
import dk.thrane.playground.stackTraceToString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.security.MessageDigest

data class PostgresConnectionParameters(
    val username: String,
    val password: String,
    val database: String,
    val hostname: String,
    val port: Int = 5432,
    val options: Map<String, String> = emptyMap()
)

class InternalPostgresConnection(private val connectionParameters: PostgresConnectionParameters) {
    // Session related values
    private var currentJob: Job? = null
    private var session: Session? = null
    private val openLock = Any()

    // Buffers
    private val messageOutBuffer = OutputBuffer(ByteArray(1024 * 1024))

    // State for outgoing traffic
    private val readyForQuery = Semaphore(permits = 1, acquiredPermits = 1) // We have not yet received ReadyForQuery
    private val sendMutex = Mutex()

    // State for ingoing traffic
    private var ingoingChannel: SendChannel<BackendMessage>? = null
    private val ingoingChannelMutex = Mutex()

    fun open(): Job {
        synchronized(openLock) {
            if (currentJob != null) throw IllegalStateException("Database connection is already open!")

            val rootJob = Job()
            val scope = CoroutineScope(rootJob + Dispatchers.Default)

            currentJob = rootJob
            openConnection(scope)
            return rootJob
        }
    }

    private fun openConnection(scope: CoroutineScope) {
        scope.launch {
            val address = InetSocketAddress(connectionParameters.hostname, connectionParameters.port)
            val socket = AsynchronousSocketChannel.open()
            socket.setOption(StandardSocketOptions.TCP_NODELAY, true)
            socket.asyncConnect(address)

            val outs = AsyncByteOutStream(ByteBuffer.allocate(1024 * 64), writeData = { socket.asyncWrite(it) })
            val ins = run {
                val byteCollector = ByteCollector(1024 * 64)
                val readBuffer = ByteBuffer.allocate(1024 * 64)
                AsyncByteInStream(byteCollector, readBuffer, readMore = { socket.asyncRead(readBuffer) })
            }

            val session = Session(socket, ins, outs)
            this@InternalPostgresConnection.session = session

            with(connectionParameters) {
                session.sendMessage(
                    FrontendMessage.StartupMessage(
                        listOf(
                            "user" to username,
                            "database" to database
                        ) + options.map { it.key to it.value }
                    )
                )
            }

            authLoop@ while (isActive) {
                when (val message = session.readMessage()) {
                    is BackendMessage.Authentication -> {
                        when (message) {
                            BackendMessage.Authentication.Ok -> {
                                break@authLoop
                            }

                            BackendMessage.Authentication.ClearTextPassword -> {
                                session.sendMessage(FrontendMessage.Password(connectionParameters.password))
                            }

                            is BackendMessage.Authentication.MD5Password -> {
                                md5Digest.update(connectionParameters.password.toByteArray(Charsets.UTF_8))
                                md5Digest.update(connectionParameters.username.toByteArray(Charsets.UTF_8))
                                val passwordHash = bytesToHex(md5Digest.digest())

                                md5Digest.update(passwordHash.toByteArray(Charsets.UTF_8))
                                md5Digest.update(message.salt)
                                val saltedHash = bytesToHex(md5Digest.digest())

                                session.sendMessage(
                                    FrontendMessage.Password("md5$saltedHash"),
                                    flush = true
                                )
                            }
                        }
                    }

                    else -> {
                        log.debug("Ignoring message: $message")
                    }
                }
            }

            log.info("Postgres connection has been opened!")
            startIngoingProcessing(scope, session)
        }
    }

    private fun startIngoingProcessing(scope: CoroutineScope, session: Session) {
        scope.launch {
            while (isActive) {
                val message = session.readMessage()
                when (message) {
                    is BackendMessage.NoticeResponse -> {
                        log.warn(
                            "NOTICE FROM POSTGRES: \n" +
                                    message.fields.joinToString("\n") { "  ${it.type}: ${it.value}" }
                        )
                    }

                    is BackendMessage.ReadyForQuery -> {
                        log.debug("<-- $message")
                        ingoingChannel?.close()
                        ingoingChannel = null
                        readyForQuery.release()
                    }

                    is BackendMessage.RowDescription, is BackendMessage.DataRow,
                    is BackendMessage.EmptyQueryResponse, is BackendMessage.CommandComplete,
                    is BackendMessage.ErrorResponse, BackendMessage.ParseComplete -> {
                        if (ingoingChannel == null) {
                            log.warn("Discarding $message")
                        }
                        ingoingChannel?.send(message)
                    }

                    else -> {
                        log.info("Unhandled message: $message")
                    }
                }
            }
        }
    }

    suspend fun sendMessage(message: FrontendMessage, awaitReadyForQuery: Boolean = true, flush: Boolean = false) {
        // Take the semaphore if we can (because it is ready and needs to be consumed) or wait for it
        if (!readyForQuery.tryAcquire() && awaitReadyForQuery) readyForQuery.acquire()
        val session = session ?: throw IllegalStateException("Not yet connected")

        sendMutex.withLock {
            session.sendMessage(message, flush = flush)
        }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun sendCommand(message: FrontendMessage, awaitReadyForQuery: Boolean = true): Flow<BackendMessage> {
        return channelFlow {
            // Take the semaphore if we can (because it is ready and needs to be consumed) or wait for it
            if (!readyForQuery.tryAcquire() && awaitReadyForQuery) readyForQuery.acquire()
            ingoingChannel = channel
            sendMessage(message, false, flush = true)
            awaitClose()
        }
    }

    private suspend fun Session.sendMessage(
        message: FrontendMessage,
        flush: Boolean = true
    ) {
        require(messageOutBuffer.ptr == 0)
        message.serialize(messageOutBuffer)
        outs.write(messageOutBuffer.array, 0, messageOutBuffer.ptr)
        messageOutBuffer.ptr = 0
        if (flush) {
            outs.flush()
        }
    }

    private suspend fun Session.readMessage(): BackendMessage {
        while (true) {
            val readMessage = BackendMessage.readMessage(ins)
            if (readMessage != null) return readMessage
        }
    }

    private data class Session(
        val socket: AsynchronousSocketChannel,
        val ins: AsyncByteInStream,
        val outs: AsyncByteOutStream
    )

    companion object {
        private val log = Log("PostgresConnection")
        private val md5Digest = MessageDigest.getInstance("MD5")!!
    }
}

suspend fun InternalPostgresConnection.sendMessageNow(message: FrontendMessage, flush: Boolean = false) {
    sendMessage(message, awaitReadyForQuery = false, flush = flush)
}

fun InternalPostgresConnection.sendCommandNow(message: FrontendMessage): Flow<BackendMessage> {
    return sendCommand(message, awaitReadyForQuery = false)
}
