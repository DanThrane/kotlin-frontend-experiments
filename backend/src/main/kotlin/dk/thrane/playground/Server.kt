package dk.thrane.playground

import java.io.*
import java.net.ServerSocket
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.*
import java.util.*
import kotlin.collections.ArrayList

class HttpClient(
    val socketId: String,
    val ins: BufferedInputStream,
    val outs: BufferedOutputStream
) {
    var closing: Boolean = false
}

enum class HttpMethod {
    GET
}

interface HttpRequestHandler {
    fun HttpClient.handleRequest(method: HttpMethod, path: String)
}

interface WebSocketRequestHandler {
    fun HttpClient.handleBinaryFrame(frame: ByteArray) {}
    fun HttpClient.handleTextFrame(frame: String) {}
    fun HttpClient.handleNewConnection() {}
    fun HttpClient.handleClosedConnection() {}
}

fun startServer(
    socket: ServerSocket = ServerSocket(8080, 4096),
    httpRequestHandler: HttpRequestHandler? = null,
    webSocketRequestHandler: WebSocketRequestHandler? = null
) {
    val sha1 = MessageDigest.getInstance("SHA-1")
    val websocketGuid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

    println("Server is ready at ${socket.inetAddress.hostName}:${socket.localPort}")
    while (true) {
        val rawClient = socket.accept()
        Thread {
            rawClient.use { rawClient ->
                while (true) {
                    val ins = rawClient.inputStream.buffered()
                    val outs = rawClient.outputStream.buffered()

                    val client = HttpClient(UUID.randomUUID().toString(), ins, outs)

                    val requestLine = ins.readLine() ?: break
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
                        val headerLine = ins.readLine() ?: break
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
                        println("Websocket connection at $path")
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

                        fun handleFrame(fin: Boolean, opcode: WebSocketOpCode?, payload: ByteArray): Boolean {
                            if (!fin || opcode == WebSocketOpCode.CONTINUATION) {
                                if (opcode !== WebSocketOpCode.CONTINUATION) {
                                    // First frame has !fin and opcode != CONTINUATION
                                    // Remaining frames will have opcode CONTINUATION
                                    // Last frame will have fin and opcode CONTINUATION
                                    fragmentationPtr = 0
                                    fragmentationOpcode = opcode
                                }

                                if (fragmentationPtr + payload.size >= fragmentationBuffer.size) {
                                    println("Dropping connection. Packet size exceeds limit.")
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
                                    println("Type: $opcode")
                                    println("Raw payload: ${payload.toList()}")
                                }
                            }
                            return false
                        }

                        messageLoop@ while (!client.closing) {
                            val initialByte = ins.read()
                            if (initialByte == -1) {
                                println("No more bytes!")
                                break
                            }

                            val fin = (initialByte and (0x01 shl 7)) != 0
                            // We don't care about rsv1,2,3
                            val opcode = WebSocketOpCode.values().find { it.opcode == (initialByte and 0x0F) }

                            val maskAndPayload = ins.readByteOrThrow()
                            val mask = (maskAndPayload and (0x01 shl 7)) != 0
                            val payloadLength: Long = run {
                                val payloadB1 = (maskAndPayload and 0b01111111)
                                when {
                                    payloadB1 < 126 -> return@run payloadB1.toLong()
                                    payloadB1 == 126 -> {
                                        val b1 = ins.readByteOrThrow()
                                        val b2 = ins.readByteOrThrow()

                                        ((b1 shl 8) or (b2)).toLong()
                                    }
                                    payloadB1 == 127 -> {
                                        val buffer = ByteArray(8)
                                        repeat(8) { buffer[it] = ins.readByteOrThrow().toByte() }

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
                                repeat(4) { buffer[it] = ins.readByteOrThrow().toByte() }
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
                                println("just done")
                                break@messageLoop
                            }
                        }
                        println("Done! ${client.closing}")
                    } else {
                        println("$method $path")

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
        }.start()
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

private fun BufferedInputStream.readLine(): String? {
    val buffer = ByteArray(4096)
    var idx = 0
    while (idx < buffer.size) {
        val next = read()
        if (next == -1) return null

        if (next == '\r'.toInt()) {
            val after = read()
            if (after == '\n'.toInt()) {
                break
            } else {
                buffer[idx++] = next.toByte()
                buffer[idx++] = after.toByte()
                continue
            }
        }
        if (next == '\n'.toInt()) break
        buffer[idx++] = next.toByte()
    }

    return String(buffer, 0, idx, Charsets.UTF_8)
}

private fun BufferedInputStream.readFully(buffer: ByteArray) {
    var idx = 0
    while (idx < buffer.size) {
        val read = read(buffer)
        if (read == -1) throw IOException("Unexpected EOF")
        idx += read
    }
}

private inline fun BufferedInputStream.readByteOrThrow(): Int {
    val result = read()
    if (result == -1) throw IOException("Unexpected EOF")
    return result
}

val secureRandom = SecureRandom()

fun HttpClient.sendWebsocketFrame(
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

    outs.write((0b1000 shl 4) or opcode.opcode)
    val maskBit = if (mask) 0b1 else 0b0

    val size = length - offset
    val initialPayloadByte = when {
        size < 126 -> size
        size < 65536 -> 126
        else -> 127
    }
    outs.write((maskBit shl 7) or initialPayloadByte)
    if (initialPayloadByte == 126) {
        outs.write(size shr 8)
        outs.write(size and 0xFF)
    } else if (initialPayloadByte == 127) {
        outs.write((size shr (64 - 8 * 1)) and 0xFF)
        outs.write((size shr (64 - 8 * 2)) and 0xFF)
        outs.write((size shr (64 - 8 * 3)) and 0xFF)
        outs.write((size shr (64 - 8 * 4)) and 0xFF)
        outs.write((size shr (64 - 8 * 5)) and 0xFF)
        outs.write((size shr (64 - 8 * 6)) and 0xFF)
        outs.write((size shr (64 - 8 * 7)) and 0xFF)
        outs.write((size shr (64 - 8 * 8)) and 0xFF)
    }

    if (maskingKey != null) {
        for (index in offset until (offset + length)) {
            payload[index] = (maskingKey[index % 4].toInt() xor payload[index].toInt()).toByte()
        }
    }

    outs.write(payload, offset, length)
    outs.flush()
}

fun HttpClient.sendHttpResponseWithFile(file: File) {
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
    file.inputStream().copyTo(outs)
    outs.flush()
}

fun HttpClient.sendHttpResponse(statusCode: Int, headers: List<Header>) {
    outs.write("HTTP/1.1 $statusCode S$statusCode\r\n".toByteArray())
    headers.forEach { (header, value) ->
        outs.write("$header: $value\r\n".toByteArray())
    }
    outs.write("\r\n".toByteArray())
    outs.flush()
}

