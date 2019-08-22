package dk.thrane.playground

import org.khronos.webgl.*

external class TextEncoder {
    fun encode(value: String): Uint8Array
}

external class TextDecoder {
    fun decode(bytes: Uint8Array): String
}

fun String.encodeToUTF8(): ByteArray {
    val encoder = TextEncoder()
    return Int8Array(encoder.encode(this).buffer).unsafeCast<ByteArray>()
}

fun stringFromUtf8(bytes: ByteArray): String {
    return TextDecoder().decode(Uint8Array(bytes.unsafeCast<Int8Array>().buffer))
}

class ByteStreamJS(buffer: ByteArray) : ByteStream(buffer) {
    override fun readDouble(): Double {
        val arr = Uint8Array(8)
        repeat(8) { arr[it] = read().toByte() }
        return Float64Array(arr.buffer)[0]
    }
}

class ByteOutStreamJS(private val buffer: Uint8Array) : ByteOutStream() {
    private var ptr = 0

    override fun flush() {
        // Do nothing
    }

    override fun writeDouble(value: Double) {
        Float64Array(buffer.buffer, ptr)[0] = value
        ptr += 8
    }

    override fun writeByte(value: Int) {
        buffer[ptr++] = value.toByte()
    }

    override fun writeByte(value: Byte) {
        buffer[ptr++] = value
    }
}
