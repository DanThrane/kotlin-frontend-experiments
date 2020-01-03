package dk.thrane.playground

import dk.thrane.playground.io.AsyncByteOutStream
import dk.thrane.playground.io.AsyncByteInStream
import dk.thrane.playground.io.ByteCollector
import dk.thrane.playground.io.asyncAccept
import dk.thrane.playground.io.asyncRead
import dk.thrane.playground.io.asyncWrite
import dk.thrane.playground.io.readByte
import dk.thrane.playground.io.readFully
import dk.thrane.playground.io.readUnsignedByte
import jdk.net.ExtendedSocketOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList

private val log = Log("Server")
private val sha1 = MessageDigest.getInstance("SHA-1")
private const val websocketGuid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

class AsyncHttpClientSession(
    val socketId: String,
    val ins: AsyncByteInStream,
    val outs: AsyncByteOutStream
) {
    var closing: Boolean = false
    var currentRequestHeader: RequestHeader? = null
}

enum class HttpMethod {
    GET
}

interface AsyncHttpRequestHandler {
    suspend fun AsyncHttpClientSession.handleRequest(method: HttpMethod, path: String)
}

interface AsyncWebSocketRequestHandler {
    suspend fun AsyncHttpClientSession.handleBinaryFrame(frame: ByteArray) {}
    suspend fun AsyncHttpClientSession.handleTextFrame(frame: String) {}
    suspend fun AsyncHttpClientSession.handleNewConnection() {}
    suspend fun AsyncHttpClientSession.handleClosedConnection() {}
}

private suspend fun handleClient(
    socket: AsynchronousSocketChannel,
    httpRequestHandler: AsyncHttpRequestHandler? = null,
    webSocketRequestHandler: AsyncWebSocketRequestHandler? = null
) {
    log.debug("Accepting new connection from $socket")

    val ins = run {
        val readBuffer = ByteBuffer.allocate(1024 * 32)
        val collector = ByteCollector(1024 * 64)

        AsyncByteInStream(
            collector,
            readBuffer,
            readMore = { socket.asyncRead(readBuffer) }
        )
    }

    val outs = AsyncByteOutStream(ByteBuffer.allocate(1024 * 32), writeData = { socket.asyncWrite(it) })

    val client = AsyncHttpClientSession(UUID.randomUUID().toString(), ins, outs)

    coroutineScope {
        while (isActive) {
            val requestLine = runCatching { ins.readLine() }.getOrNull() ?: break
            val tokens = requestLine.split(" ")
            if (tokens.size < 3 || !requestLine.endsWith("HTTP/1.1")) {
                break
            }

            val method = tokens.first().toUpperCase()
            if (method != "GET") {
                client.sendHttpResponse(405, defaultHeaders())
                break
            }

            val path = tokens.getOrNull(1) ?: break

            val requestHeaders = ArrayList<Header>()
            while (true) {
                val headerLine = ins.readLine()
                if (headerLine.isEmpty()) break
                requestHeaders.add(
                    Header(
                        headerLine.substringBefore(':'),
                        headerLine.substringAfter(':').trim()
                    )
                )
            }

            if (webSocketRequestHandler != null &&
                requestHeaders.any { it.header.equals("Upgrade", true) && it.value == "websocket" }
            ) {
                log.info("WS $path")
                // The following headers are required to be present
                val key = requestHeaders.find { it.header.equals("Sec-WebSocket-Key", true) }
                val origin = requestHeaders.find { it.header.equals("Origin", true) }
                val version = requestHeaders.find { it.header.equals("Sec-WebSocket-Version", true) }

                if (key == null || origin == null || version == null) {
                    client.sendHttpResponse(400, defaultHeaders())
                    break
                }

                // We only speak WebSocket as defined in RFC 6455
                if (!version.value.split(",").map { it.trim() }.contains("13")) {
                    client.sendHttpResponse(
                        400, defaultHeaders() + listOf(
                            Header("Sec-WebSocket-Version", "13")
                        )
                    )
                    break
                }

                val responseHeaders = ArrayList<Header>()

                // Prove to the client that we did in fact receive the request
                responseHeaders.add(
                    Header(
                        "Sec-WebSocket-Accept",
                        Base64.getEncoder().encodeToString(
                            sha1.digest((key.value + websocketGuid).toByteArray())
                        )
                    )
                )

                // Yes, we really want to upgrade this connection.
                responseHeaders.add(Header("Connection", "Upgrade"))
                responseHeaders.add(Header("Upgrade", "websocket"))

                client.sendHttpResponse(101, defaultHeaders() + responseHeaders)

                val frameAssembler = WSFrameAssembler(client, object : WSFrameHandler<AsyncHttpClientSession> {
                    override suspend fun handleBinaryFrame(session: AsyncHttpClientSession, frame: ByteArray) {
                        with(session) {
                            with(webSocketRequestHandler) {
                                handleBinaryFrame(frame)
                            }
                        }
                    }

                    override suspend fun handleTextFrame(session: AsyncHttpClientSession, frame: String) {
                        with(session) {
                            with(webSocketRequestHandler) {
                                handleTextFrame(frame)
                            }
                        }
                    }

                    override suspend fun handlePingFrame(session: AsyncHttpClientSession, frame: ByteArray) {
                        session.outs.sendWebsocketFrame(WebSocketOpCode.PONG, frame)
                    }

                    override suspend fun handlePongFrame(session: AsyncHttpClientSession, frame: ByteArray) {
                        // Do nothing
                    }
                })

                messageLoop@ while (!client.closing) {
                    if (!frameAssembler.readFrame(client.ins)) {
                        break@messageLoop
                    }
                }
                log.debug("Done! ${client.closing}")
            } else {
                log.info("$method $path")

                if (httpRequestHandler != null) {
                    with(client) {
                        with(httpRequestHandler) {
                            handleRequest(HttpMethod.GET, path)
                        }
                    }
                } else {
                    client.sendHttpResponse(404, defaultHeaders())
                }
            }
        }
    }
}

fun startServer(
    address: SocketAddress = InetSocketAddress(8080),
    httpRequestHandler: AsyncHttpRequestHandler? = null,
    webSocketRequestHandler: AsyncWebSocketRequestHandler? = null,
    wait: Boolean = true
) {
    val supervisorJob = SupervisorJob()
    val acceptScope = CoroutineScope(Dispatchers.Default + supervisorJob)
    val asyncSocket = AsynchronousServerSocketChannel.open().bind(address)

    log.info("Server is ready at $address")
    val job = acceptScope.launch {
        while (true) {
            val acceptJob = SupervisorJob()
            val clientScope = (acceptScope + acceptJob)

            val rawClient = asyncSocket.asyncAccept()
            rawClient.setOption(StandardSocketOptions.TCP_NODELAY, true)
            clientScope.launch {
                handleClient(rawClient, httpRequestHandler, webSocketRequestHandler)
            }

            if (!isActive) {
                acceptJob.cancel()
                break
            }
        }
    }

    if (wait) {
        runBlocking { job.join() }
    }
}

fun defaultHeaders(payloadSize: Long = 0): List<Header> = listOf(
    dateHeader(),
    Header("Server", "Experiment"),
    Header("Content-Length", payloadSize.toString())
)

private fun dateHeader(timestamp: Long = System.currentTimeMillis()): Header {
    val localDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.of("GMT")
    )

    val value = buildString {
        append(
            when (localDateTime.dayOfWeek) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"

                // We assume that the end of time occurs on a monday
                null -> "Mon"
            }
        )

        append(", ")
        append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
        append(" ")
        append(
            when (localDateTime.month) {
                Month.JANUARY -> "Jan"
                Month.FEBRUARY -> "Feb"
                Month.MARCH -> "Mar"
                Month.APRIL -> "Apr"
                Month.MAY -> "May"
                Month.JUNE -> "Jun"
                Month.JULY -> "Jul"
                Month.AUGUST -> "Aug"
                Month.SEPTEMBER -> "Sep"
                Month.OCTOBER -> "Oct"
                Month.NOVEMBER -> "Nov"
                Month.DECEMBER -> "Dev"
                null -> "Jan"
            }
        )
        append(" ")
        append(localDateTime.year)
        append(" ")
        append(localDateTime.hour.toString().padStart(2, '0'))
        append(":")
        append(localDateTime.minute.toString().padStart(2, '0'))
        append(":")
        append(localDateTime.second.toString().padStart(2, '0'))
        append(" GMT")
    }

    return Header("Date", value)
}

data class Header(val header: String, val value: String)

suspend fun AsyncHttpClientSession.sendHttpResponseWithFile(file: File) {
    sendHttpResponse(
        200,
        defaultHeaders(payloadSize = file.length()) +
                listOf(
                    Header(
                        "Content-Type",
                        mimeTypes["." + file.extension] ?: "text/plain"
                    )
                )
    )

    withContext(Dispatchers.IO) {
        defaultBufferPool.useInstance { buffer ->
            file.inputStream().use { fIns ->
                var bytes = fIns.read(buffer)
                while (bytes >= 0) {
                    outs.write(buffer, 0, bytes)
                    bytes = fIns.read(buffer)
                }
            }
        }
    }

    outs.flush()
}

suspend fun AsyncHttpClientSession.sendHttpResponse(statusCode: Int, headers: List<Header>) {
    outs.write("HTTP/1.1 $statusCode S$statusCode\r\n".toByteArray())
    headers.forEach { (header, value) ->
        outs.write("$header: $value\r\n".toByteArray())
    }
    outs.write("\r\n".toByteArray())
    outs.flush()
}
