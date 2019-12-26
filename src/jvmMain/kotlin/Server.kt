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

private val secureRandom = SecureRandom()
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

                // TODO We definitely need fix the GC issues here.
                val fragmentationBuffer = ByteArray(1024 * 256)
                var fragmentationPtr = -1
                var fragmentationOpcode: WebSocketOpCode? = null

                suspend fun handleFrame(fin: Boolean, opcode: WebSocketOpCode?, payload: ByteArray): Boolean {
                    if (!fin || opcode == WebSocketOpCode.CONTINUATION) {
                        if (opcode !== WebSocketOpCode.CONTINUATION) {
                            // First frame has !fin and opcode != CONTINUATION
                            // Remaining frames will have opcode CONTINUATION
                            // Last frame will have fin and opcode CONTINUATION
                            fragmentationPtr = 0
                            fragmentationOpcode = opcode
                        }

                        if (fragmentationPtr + payload.size >= fragmentationBuffer.size) {
                            log.info("Dropping connection. Packet size exceeds limit.")
                            return true
                        }

                        System.arraycopy(payload, 0, fragmentationBuffer, fragmentationPtr, payload.size)
                        fragmentationPtr += payload.size

                        if (!fin) return false

                        val copiedPayload = fragmentationBuffer.copyOf(fragmentationPtr)
                        val copiedOpcode = fragmentationOpcode

                        fragmentationPtr = -1
                        fragmentationOpcode = null

                        return handleFrame(true, copiedOpcode, copiedPayload)
                    }

                    when (opcode) {
                        WebSocketOpCode.TEXT -> {
                            with(client) {
                                with(webSocketRequestHandler) {
                                    handleTextFrame(payload.toString(Charsets.UTF_8))
                                }
                            }
                        }

                        WebSocketOpCode.BINARY -> {
                            with(client) {
                                with(webSocketRequestHandler) {
                                    handleBinaryFrame(payload)
                                }
                            }
                        }

                        WebSocketOpCode.PING -> {
                            client.sendWebsocketFrame(
                                WebSocketOpCode.PONG,
                                payload
                            )
                        }

                        else -> {
                            log.info("Type: $opcode")
                            log.info("Raw payload: ${payload.toList()}")
                        }
                    }
                    return false
                }

                messageLoop@ while (!client.closing) {
                    val initialByte = runCatching { ins.readUnsignedByte() }.getOrNull() ?: break

                    val fin = (initialByte and (0x01 shl 7)) != 0
                    // We don't care about rsv1,2,3
                    val opcode = WebSocketOpCode.values().find { it.opcode == (initialByte and 0x0F) }

                    val maskAndPayload = ins.readUnsignedByte()
                    val mask = (maskAndPayload and (0x01 shl 7)) != 0
                    val payloadLength: Long = run {
                        val payloadB1 = (maskAndPayload and 0b01111111)
                        when {
                            payloadB1 < 126 -> return@run payloadB1.toLong()
                            payloadB1 == 126 -> {
                                val b1 = ins.readUnsignedByte()
                                val b2 = ins.readUnsignedByte()

                                ((b1 shl 8) or (b2)).toLong()
                            }
                            payloadB1 == 127 -> {
                                val buffer = ByteArray(8)
                                repeat(8) { buffer[it] = ins.readByte() }

                                buffer[0].toLong() shl (64 - 8) or
                                        (buffer[1].toLong() shl (64 - 8 * 2)) or
                                        (buffer[2].toLong() shl (64 - 8 * 3)) or
                                        (buffer[3].toLong() shl (64 - 8 * 4)) or
                                        (buffer[4].toLong() shl (64 - 8 * 5)) or
                                        (buffer[5].toLong() shl (64 - 8 * 6)) or
                                        (buffer[6].toLong() shl (64 - 8 * 7)) or
                                        (buffer[7].toLong())

                            }
                            else -> throw IllegalStateException()
                        }
                    }

                    val maskingKey = if (mask) {
                        val buffer = ByteArray(4)
                        repeat(4) { buffer[it] = ins.readByte() }
                        buffer
                    } else {
                        null
                    }

                    if (payloadLength > Int.MAX_VALUE) TODO()

                    val payload = ByteArray(payloadLength.toInt())
                    ins.readFully(payload)
                    if (maskingKey != null) {
                        payload.forEachIndexed { index, byte ->
                            payload[index] = (byte.toInt() xor maskingKey[index % 4].toInt()).toByte()
                        }
                    }

                    if (handleFrame(fin, opcode, payload)) {
                        log.debug("just done")
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

enum class WebSocketOpCode(val opcode: Int) {
    CONTINUATION(0x0),
    TEXT(0x1),
    BINARY(0x2),
    CONNECTION_CLOSE(0x8),
    PING(0x9),
    PONG(0xA)
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

suspend fun AsyncHttpClientSession.sendWebsocketFrame(
    opcode: WebSocketOpCode,
    payload: ByteArray,
    offset: Int = 0,
    length: Int = payload.size,
    mask: Boolean = false
) {
    val maskingKey = if (!mask) {
        null
    } else {
        val buf = ByteArray(4)
        secureRandom.nextBytes(buf)
        buf
    }

    outs.write(((0b1000 shl 4) or opcode.opcode).toByte())
    val maskBit = if (mask) 0b1 else 0b0

    val size = length - offset
    val initialPayloadByte = when {
        size < 126 -> size
        size < 65536 -> 126
        else -> 127
    }
    outs.write(((maskBit shl 7) or initialPayloadByte).toByte())
    if (initialPayloadByte == 126) {
        outs.write((size shr 8).toByte())
        outs.write((size and 0xFF).toByte())
    } else if (initialPayloadByte == 127) {
        outs.write(((size shr (64 - 8 * 1)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 2)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 3)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 4)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 5)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 6)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 7)) and 0xFF).toByte())
        outs.write(((size shr (64 - 8 * 8)) and 0xFF).toByte())
    }

    if (maskingKey != null) {
        for (index in offset until (offset + length)) {
            payload[index] = (maskingKey[index % 4].toInt() xor payload[index].toInt()).toByte()
        }
    }

    outs.write(payload, offset, length)
    outs.flush()
}
