package dk.thrane.playground

import dk.thrane.playground.io.AsyncByteInStream
import dk.thrane.playground.io.readByte
import dk.thrane.playground.io.readFully
import dk.thrane.playground.io.readUnsignedByte

interface WSFrameHandler<Session> {
    suspend fun handleBinaryFrame(session: Session, frame: ByteArray)
    suspend fun handleTextFrame(session: Session, frame: String)
    suspend fun handlePingFrame(session: Session, frame: ByteArray)
    suspend fun handlePongFrame(session: Session, frame: ByteArray)
}

class WSFrameAssembler<Session>(
    private val session: Session,
    private val frameHandler: WSFrameHandler<Session>
) {
    private val fragmentationBuffer = ByteArray(1024 * 256)
    private var fragmentationPtr = -1
    private var fragmentationOpcode: WebSocketOpCode? = null

    private suspend fun handleFrame(fin: Boolean, opcode: WebSocketOpCode?, payload: ByteArray): Boolean {
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
                frameHandler.handleTextFrame(session, payload.toString(Charsets.UTF_8))
            }

            WebSocketOpCode.BINARY -> {
                frameHandler.handleBinaryFrame(session, payload)
            }

            WebSocketOpCode.PING -> {
                frameHandler.handlePingFrame(session, payload)
            }

            WebSocketOpCode.PONG -> {
                frameHandler.handlePongFrame(session, payload)
            }

            else -> {
                log.info("Type: $opcode")
                log.info("Raw payload: ${payload.toList()}")
            }
        }
        return false
    }

    suspend fun readFrame(ins: AsyncByteInStream): Boolean {
        val initialByte = runCatching { ins.readUnsignedByte() }.getOrNull() ?: return false

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

        if (payloadLength > maxPayloadSize) throw IllegalStateException("Too big payload")

        val payload = ByteArray(payloadLength.toInt())
        ins.readFully(payload)
        if (maskingKey != null) {
            payload.forEachIndexed { index, byte ->
                payload[index] = (byte.toInt() xor maskingKey[index % 4].toInt()).toByte()
            }
        }

        if (handleFrame(fin, opcode, payload)) {
            log.debug("just done")
            return false
        }

        return true
    }

    companion object {
        private val log = Log("WSFrameAssembler")
        private const val maxPayloadSize = 1024 * 512
    }
}
