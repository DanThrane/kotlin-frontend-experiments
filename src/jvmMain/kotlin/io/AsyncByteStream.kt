package dk.thrane.playground.io

import dk.thrane.playground.Log
import dk.thrane.playground.defaultBufferPool
import dk.thrane.playground.useInstance
import java.nio.ByteBuffer

class AsyncByteStream(
    private val collector: ByteCollector,
    private val buffer: ByteBuffer,
    private val readMore: suspend () -> Int
) {
    suspend fun readLine(): String {
        defaultBufferPool.useInstance { lineBuffer ->
            while (true) {
                val bytesRead = collector.readUntilDelimiter(newLinePattern, lineBuffer)
                if (bytesRead == -1) {
                    readAndDeposit()
                } else {
                    return String(lineBuffer, 0, bytesRead, Charsets.UTF_8).replace("\r\n", "")
                }
            }
        }

        throw IllegalStateException("EOF reached before line was reached")
    }

    suspend fun read(
        destination: ByteArray,
        destinationOffset: Int = 0,
        maxBytes: Int = destination.size - destinationOffset
    ): Int {
        val read = collector.read(destination, destinationOffset, maxBytes)
        if (read > 0) return read
        readAndDeposit()
        return read(destination, destinationOffset, maxBytes)
    }

    private suspend fun readAndDeposit() {
        val read = readMore()
        if (read == -1) throw IllegalStateException("End of Stream")
        collector.deposit(buffer)
    }

    companion object {
        private val newLinePattern = byteArrayOf('\r'.toByte(), '\n'.toByte())
        private val log = Log("AsyncByteStream")
    }
}
