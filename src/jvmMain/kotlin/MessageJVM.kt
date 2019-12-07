package dk.thrane.playground

class ByteStreamJVM(buffer: ByteArray) : ByteStream(buffer) {
    override fun readDouble(): Double {
        return java.lang.Double.longBitsToDouble(readLong())
    }
}

class ByteOutStreamJVM(private val stream: ByteArray) : ByteOutStream() {
    var ptr: Int = 0
        private set

    fun reset() {
        ptr = 0
    }

    override fun flush() {
        // Do nothing
    }

    override fun writeDouble(value: Double) {
        writeLong(java.lang.Double.doubleToLongBits(value))
    }

    override fun writeByte(value: Int) {
        stream[ptr++] = value.toByte()
    }

    override fun writeByte(value: Byte) {
        stream[ptr++] = value
    }

    override fun writeFully(bytes: ByteArray) {
        System.arraycopy(bytes, 0, stream, ptr, bytes.size)
        ptr += bytes.size
    }
}

actual fun String.encodeToUTF8() = toByteArray(Charsets.UTF_8)
actual fun stringFromUtf8(array: ByteArray) = String(array, Charsets.UTF_8)
