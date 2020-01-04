package dk.thrane.playground.serialization

actual fun InputBuffer.readDouble(): Double {
    return java.lang.Double.longBitsToDouble(readLong())
}

actual fun OutputBuffer.writeDouble(value: Double) {
    return writeLong(java.lang.Double.doubleToLongBits(value))
}
