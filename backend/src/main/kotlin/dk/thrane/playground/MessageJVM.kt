package dk.thrane.playground

class ByteStreamJVM(buffer: ByteArray) : ByteStream(buffer) {
    override fun readDouble(): Double {
        return java.lang.Double.longBitsToDouble(readLong())
    }
}