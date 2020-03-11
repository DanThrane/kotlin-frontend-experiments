package dk.thrane.playground

import dk.thrane.playground.io.*
import dk.thrane.playground.site.api.Authentication
import dk.thrane.playground.site.api.LoginRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.security.SecureRandom
import kotlin.time.*

interface WebSocketClientCallbacks {
    suspend fun onOpen(client: WebSocketClient) {}
    suspend fun onClose(client: WebSocketClient) {}
    suspend fun handleTextFrame(client: WebSocketClient, frame: String) {}
    suspend fun handleBinaryFrame(client: WebSocketClient, frame: ByteArray) {}
}

class WebSocketClient(
    private val hostname: String,
    private val port: Int,
    private val callbackHandler: WebSocketClientCallbacks,
    private val path: String = "/",
    private val pingPeriod: Duration = 1.minutes,
    private val origin: String = hostname
) {
    private val mutex = Mutex()
    private var session: ActiveSession? = null
    private var isRunning = false

    private data class ActiveSession(
        val socket: AsynchronousSocketChannel,
        val ins: AsyncByteInStream,
        val outs: AsyncByteOutStream,
        val processingJob: Job
    )

    private suspend fun openSession(): ActiveSession {
        mutex.withLock {
            require(session == null)

            val socket = AsynchronousSocketChannel.open()
            socket.setOption(StandardSocketOptions.TCP_NODELAY, true)
            socket.asyncConnect(InetSocketAddress(hostname, port))

            val outs = AsyncByteOutStream(ByteBuffer.allocate(1024 * 64), writeData = { socket.asyncWrite(it) })
            val ins = run {
                val byteCollector = ByteCollector(1024 * 64)
                val readBuffer = ByteBuffer.allocate(1024 * 64)
                AsyncByteInStream(byteCollector, readBuffer, readMore = { socket.asyncRead(readBuffer) })
            }

            // This heavily assumes that we are not sending anything outside of the ASCII range
            val key = ByteArray(64).apply { random.nextBytes(this) }
            val handshake = "GET $path HTTP/1.1\r\n" +
                    "Origin: $origin\r\n" +
                    "Sec-WebSocket-Version: 13\r\n" +
                    "Sec-WebSocket-Key: ${JVMBase64Encoder.encode(key)}\r\n" +
                    "Upgrade: websocket\r\n" +
                    "\r\n"

            outs.write(handshake.toByteArray())
            outs.flush()

            val responseLine = ins.readLine()
            if (!responseLine.startsWith("HTTP/1.1")) {
                socket.close()
                throw IOException("Bad response from server: $responseLine")
            }

            val (_, statusCode, _) = responseLine.split(" ")
            if (statusCode != "101") {
                socket.close()
                throw IOException("Bad response from server: $responseLine")
            }

            // We do not use any headers. Just wait for them to end.
            while (true) {
                if (ins.readLine().isEmpty()) {
                    break
                }
            }

            val activeSession = ActiveSession(socket, ins, outs, Job())
            session = activeSession
            return activeSession
        }
    }

    fun start(): Job {
        return GlobalScope.launch {
            mutex.withLock {
                require(!isRunning)
                isRunning = true
            }

            while (isRunning) {
                val activeSession = try {
                    openSession()
                } catch (ex: Throwable) {
                    log.warn("Unable to open session!")
                    log.warn(ex.stackTraceToString())
                    delay(1000L)
                    continue
                }

                callbackHandler.onOpen(this@WebSocketClient)

                val scope = CoroutineScope(activeSession.processingJob + Dispatchers.Default)
                val processingJob = startProcessing(activeSession, scope)
                val pingJob = startPingProcessing(activeSession, scope)

                try {
                    select<Unit> {
                        processingJob.onAwait {}
                        pingJob.onAwait {}
                    }

                    log.info("Closing normally. Won't attempt to restart!")
                    runCatching { activeSession.socket.close() }
                    callbackHandler.onClose(this@WebSocketClient)
                } catch (ex: Throwable) {
                    val level = if (ex is IOException) LogLevel.DEBUG else LogLevel.WARN
                    log.message(level, "Caught exception in WebSocket client processing loop:")
                    log.message(level, ex.stackTraceToString())

                    log.info("Are we still running? $isRunning")
                    callbackHandler.onClose(this@WebSocketClient)

                    runCatching {
                        activeSession.socket.close()
                    }

                    session = null
                }
            }
        }
    }

    suspend fun stop() {
        mutex.withLock {
            require(isRunning)
            val activeSession = session
            require(activeSession != null)

            isRunning = false
            activeSession.socket.close()
            activeSession.processingJob.cancel("stop() was called")
            session = null
        }
    }

    suspend fun sendBinaryFrame(frame: ByteArray) {
        val activeSession = session
        require(activeSession != null)
        activeSession.outs.sendWebsocketFrame(WebSocketOpCode.BINARY, frame)
    }

    suspend fun sendTextFrame(frame: String) {
        val activeSession = session
        require(activeSession != null)
        activeSession.outs.sendWebsocketFrame(WebSocketOpCode.TEXT, frame.toByteArray(Charsets.UTF_8))
    }

    private fun startProcessing(session: ActiveSession, scope: CoroutineScope): Deferred<Unit> = with(session) {
        scope.async {
            val frameAssembler = WSFrameAssembler(session, object : WSFrameHandler<ActiveSession> {
                override suspend fun handleBinaryFrame(session: ActiveSession, frame: ByteArray) {
                    callbackHandler.handleBinaryFrame(this@WebSocketClient, frame)
                }

                override suspend fun handleTextFrame(session: ActiveSession, frame: String) {
                    callbackHandler.handleTextFrame(this@WebSocketClient, frame)
                }

                override suspend fun handlePingFrame(session: ActiveSession, frame: ByteArray) {
                    session.outs.sendWebsocketFrame(WebSocketOpCode.PONG, frame)
                }

                override suspend fun handlePongFrame(session: ActiveSession, frame: ByteArray) {
                    // Do nothing
                }
            })

            while (isActive) {
                try {
                    if (!frameAssembler.readFrame(ins)) {
                        throw IOException("Connection was closed")
                    }
                } catch (ex: Throwable) {
                    throw IOException("An exception occurred (connection closed?)", ex)
                }
            }
        }
    }

    private fun startPingProcessing(session: ActiveSession, scope: CoroutineScope): Deferred<Unit> {
        return scope.async {
            while (isActive) {
                session.outs.sendWebsocketFrame(WebSocketOpCode.PING, pingBytes)
                delay(pingPeriod.inMilliseconds.toLong())
            }
        }
    }

    companion object {
        private val pingBytes = byteArrayOf(1, 3, 3, 7)
        private val random = SecureRandom()
        private val log = Log("WebSocketClient")
    }
}

