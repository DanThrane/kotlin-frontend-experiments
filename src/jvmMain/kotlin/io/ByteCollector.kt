package dk.thrane.playground.io

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import kotlin.math.min

class ByteCollector(public val maxCapacity: Int) {
    private val internalArray = ByteArray(maxCapacity)
    private var writePointer = 0
    private var readPointer = 0
    private val mutex = Mutex()
    private var readMark = -1

    public suspend fun deposit(buffer: ByteBuffer) {
        buffer.flip()
        try {
            val array = buffer.array()
            val offset = buffer.arrayOffset()
            val bytesToCopy = buffer.remaining()

            mutex.withLock {
                val internalCapacity = internalArray.size - writePointer
                if (internalCapacity < bytesToCopy) {
                    val internalCapacityAfterMove = internalCapacity + readPointer
                    require(internalCapacityAfterMove > bytesToCopy)
                    val newWritePointer = internalArray.size - readPointer
                    System.arraycopy(internalArray, readPointer, internalArray, 0, newWritePointer)

                    writePointer = newWritePointer
                    readPointer = 0
                }

                System.arraycopy(array, offset, internalArray, writePointer, bytesToCopy)
                writePointer += bytesToCopy
            }
        } finally {
            buffer.clear()
        }
    }

    public suspend fun createMark() {
        mutex.withLock {
            readMark = readPointer
        }
    }

    public suspend fun resetToMark() {
        mutex.withLock {
            require(readMark > 0)
            readPointer = readMark
            readMark = -1
        }
    }

    public suspend fun readUntilDelimiter(
        pattern: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        maxBytes: Int = destination.size - destinationOffset
    ): Int {
        mutex.withLock {
            for (i in readPointer until writePointer) {
                // TODO This is not very efficient for large patterns
                var found = true
                for (j in pattern.indices) {
                    if (internalArray.getOrNull(i + j) != pattern[j]) {
                        found = false
                        break
                    }
                }

                if (found) {
                    val bytesToCopy = i - readPointer + pattern.size
                    require(bytesToCopy <= maxBytes)
                    System.arraycopy(internalArray, readPointer, destination, destinationOffset, bytesToCopy)
                    readPointer += bytesToCopy
                    return bytesToCopy
                }
            }

            return -1
        }
    }

    public suspend fun read(
        destination: ByteArray,
        destinationOffset: Int = 0,
        maxBytes: Int = destination.size - destinationOffset
    ): Int {
        mutex.withLock {
            val bytesToCopy = min(maxBytes, writePointer - readPointer)
            require(bytesToCopy <= maxBytes)
            System.arraycopy(internalArray, readPointer, destination, destinationOffset, bytesToCopy)
            readPointer += bytesToCopy
            return bytesToCopy
        }
    }
}
