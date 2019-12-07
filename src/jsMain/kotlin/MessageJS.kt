package dk.thrane.playground

import org.khronos.webgl.*

external class TextEncoder {
    fun encode(value: String): Uint8Array
}

external class TextDecoder {
    fun decode(bytes: Uint8Array): String
}

actual fun String.encodeToUTF8(): ByteArray {
    val encoder = TextEncoder()
    return Int8Array(encoder.encode(this).buffer).unsafeCast<ByteArray>()
}

actual fun stringFromUtf8(array: ByteArray): String {
    return TextDecoder().decode(Uint8Array(array.unsafeCast<Int8Array>().buffer))
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

    fun viewMessage(): ArrayBufferView = buffer.subarray(0, ptr)

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
