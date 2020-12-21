package dk.thrane.playground.io

import java.nio.ByteBuffer
import kotlin.math.min

class AsyncByteOutStream(
    private val writeBuffer: ByteBuffer,
    private val writeData: suspend (buffer: ByteBuffer) -> Int
) {
    suspend fun write(data: ByteBuffer) {
        if (writeBuffer.remaining() < data.remaining()) {
            flush()
        }

        writeBuffer.put(data)
    }

    suspend fun write(data: ByteArray, offset: Int = 0, length: Int = data.size - offset) {
        if (writeBuffer.remaining() < length) {
            flush()
        }

        var internalPtr = offset
        while (internalPtr < length) {
            if (writeBuffer.remaining() == 0) flush()

            val remaining = min(length - internalPtr, writeBuffer.remaining())
            writeBuffer.put(data, internalPtr, remaining)
            internalPtr += remaining
        }
    }

    suspend fun write(byte: Byte) {
        if (writeBuffer.remaining() < 1) {
            flush()
        }

        writeBuffer.put(byte)
    }

    suspend fun flush() {
        writeBuffer.flip()
        while (writeBuffer.remaining() > 0) {
            writeData(writeBuffer)
        }
        writeBuffer.clear()
    }
}
