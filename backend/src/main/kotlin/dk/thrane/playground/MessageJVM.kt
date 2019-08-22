package dk.thrane.playground

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

class ByteStreamJVM(buffer: ByteArray) : ByteStream(buffer) {
    override fun readDouble(): Double {
        return java.lang.Double.longBitsToDouble(readLong())
    }
}

class ByteOutStreamJVM(private val stream: BufferedOutputStream) : ByteOutStream() {
    override fun flush() {
        stream.flush()
    }

    override fun writeDouble(value: Double) {
        writeLong(java.lang.Double.doubleToLongBits(value))
    }

    override fun writeByte(value: Int) {
        stream.write(value)
    }

    override fun writeByte(value: Byte) {
        stream.write(value.toInt())
    }

    override fun writeFully(bytes: ByteArray) {
        stream.write(bytes)
    }
}

fun String.encodeToUTF8() = toByteArray(Charsets.UTF_8)
fun stringFromUtf8(array: ByteArray) = String(array, Charsets.UTF_8)

fun main() {
    val out = BoundOutgoingMessage(TestMessage)

    out[TestMessage.text] = "Testing!"
    out[TestMessage.nested] = {
        it[TestMessage.text] = "qweasdqwe"
        it[TestMessage.nested] = null
    }

    val byteArrayOutputStream = ByteArrayOutputStream()
    write(ByteOutStreamJVM(byteArrayOutputStream.buffered()), out.build())

    val message = parse(ByteStreamJVM(byteArrayOutputStream.toByteArray()))
    println(message)
}