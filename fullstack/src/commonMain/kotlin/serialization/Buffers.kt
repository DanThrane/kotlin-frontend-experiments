package dk.thrane.playground.serialization

class InputBuffer(public val array: ByteArray) {
    public var ptr = 0

    fun read(): Int = (array[ptr++].toInt() and 0xff)
}

fun InputBuffer.readInt(): Int {
    val ch1 = read()
    val ch2 = read()
    val ch3 = read()
    val ch4 = read()

    return (ch1 shl 24) or (ch2 shl 16) or (ch3 shl 8) or (ch4 shl 0)
}

fun InputBuffer.readShort(): Short {
    val ch1 = read()
    val ch2 = read()

    return ((ch1 shl 8) or (ch2 shl 0)).toShort()
}

fun InputBuffer.readLong(): Long {
    val result = (array[ptr + 0].toLong() shl 56) or
            ((array[ptr + 1].toLong() and 255) shl 48) or
            ((array[ptr + 2].toLong() and 255) shl 40) or
            ((array[ptr + 3].toLong() and 255) shl 32) or
            ((array[ptr + 4].toLong() and 255) shl 24) or
            (array[ptr + 5].toLong() and 255 shl 16) or
            (array[ptr + 6].toLong() and 255 shl 8) or
            (array[ptr + 7].toLong() and 255 shl 0)
    ptr += 8
    return result
}

expect fun InputBuffer.readDouble(): Double

fun InputBuffer.readFully(destination: ByteArray) {
    require(ptr + destination.size <= array.size) { "Not enough bytes in buffer!" }
    array.copyInto(destination, startIndex = ptr, endIndex = ptr + destination.size)
    ptr += destination.size
}

class OutputBuffer(public val array: ByteArray) {
    public var ptr: Int = 0

    fun writeByte(value: Int) {
        array[ptr++] = value.toByte()
    }

    fun writeByte(value: Byte) {
        array[ptr++] = value
    }
}

fun OutputBuffer.writeFully(bytes: ByteArray) {
    require(ptr + bytes.size < array.size) { "Buffer overflow" }
    bytes.copyInto(array, ptr)
    ptr += bytes.size
}

expect fun OutputBuffer.writeDouble(value: Double)

fun OutputBuffer.writeInt(v: Int) {
    val cPtr = ptr
    array[cPtr] = (v shr (24) and 0xFF).toByte()
    array[cPtr + 1] = (v shr (16) and 0xFF).toByte()
    array[cPtr + 2] = (v shr (8) and 0xFF).toByte()
    array[cPtr + 3] = (v shr (0) and 0xFF).toByte()
    ptr += 4
}

fun OutputBuffer.writeShort(v: Short) {
    val cPtr = ptr
    array[cPtr] = (v.toInt() shr (8) and 0xFF).toByte()
    array[cPtr + 1] = (v.toInt() shr (0) and 0xFF).toByte()
    ptr += 2
}

fun OutputBuffer.writeLong(v: Long) {
    val cPtr = ptr
    array[cPtr] = (v shr (56) and 0xFF).toByte()
    array[cPtr + 1] = (v shr (48) and 0xFF).toByte()
    array[cPtr + 2] = (v shr (40) and 0xFF).toByte()
    array[cPtr + 3] = (v shr (32) and 0xFF).toByte()
    array[cPtr + 4] = (v shr (24) and 0xFF).toByte()
    array[cPtr + 5] = (v shr (16) and 0xFF).toByte()
    array[cPtr + 6] = (v shr (8) and 0xFF).toByte()
    array[cPtr + 7] = (v shr (0) and 0xFF).toByte()
    ptr += 8
}
