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

suspend fun AsyncByteInStream.readInt(): Int {
    val ch1 = readUnsignedByte()
    val ch2 = readUnsignedByte()
    val ch3 = readUnsignedByte()
    val ch4 = readUnsignedByte()

    return (ch1 shl 24) or (ch2 shl 16) or (ch3 shl 8) or (ch4 shl 0)
}

suspend fun AsyncByteInStream.readLong(): Long {
    return (readUnsignedByte().toLong() shl 56) or
            ((readUnsignedByte().toLong() and 255) shl 48) or
            ((readUnsignedByte().toLong() and 255) shl 40) or
            ((readUnsignedByte().toLong() and 255) shl 32) or
            ((readUnsignedByte().toLong() and 255) shl 24) or
            ((readUnsignedByte().toLong() and 255) shl 16) or
            ((readUnsignedByte().toLong() and 255) shl 8) or
            ((readUnsignedByte().toLong() and 255) shl 0)
}
