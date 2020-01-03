package dk.thrane.playground

import dk.thrane.playground.io.AsyncByteOutStream
import java.security.SecureRandom

private val secureRandom = SecureRandom()

suspend fun AsyncByteOutStream.sendWebsocketFrame(
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

    write(((0b1000 shl 4) or opcode.opcode).toByte())
    val maskBit = if (mask) 0b1 else 0b0

    val size = length - offset
    val initialPayloadByte = when {
        size < 126 -> size
        size < 65536 -> 126
        else -> 127
    }
    write(((maskBit shl 7) or initialPayloadByte).toByte())
    if (initialPayloadByte == 126) {
        write((size shr 8).toByte())
        write((size and 0xFF).toByte())
    } else if (initialPayloadByte == 127) {
        write(((size shr (64 - 8 * 1)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 2)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 3)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 4)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 5)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 6)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 7)) and 0xFF).toByte())
        write(((size shr (64 - 8 * 8)) and 0xFF).toByte())
    }

    if (maskingKey != null) {
        for (index in offset until (offset + length)) {
            payload[index] = (maskingKey[index % 4].toInt() xor payload[index].toInt()).toByte()
        }
    }

    write(payload, offset, length)
    flush()
}
