package dk.thrane.playground.io

import dk.thrane.playground.defaultBufferPool
import dk.thrane.playground.useInstance

suspend fun AsyncByteInStream.readByte(): Byte {
    return defaultBufferPool.useInstance { buffer ->
        read(buffer, maxBytes = 1)
        buffer[0]
    }
}

suspend fun AsyncByteInStream.readUnsignedByte(): Int {
    return readByte().toInt() and 0xFF
}

suspend fun AsyncByteInStream.readFully(
    destination: ByteArray,
    offset: Int = 0,
    maxBytes: Int = destination.size - offset
) {
    var ptr = offset
    while (ptr < offset + maxBytes) {
        val read = read(destination, ptr, (offset + maxBytes) - ptr)
        if (read > 0) ptr += read
    }
}
