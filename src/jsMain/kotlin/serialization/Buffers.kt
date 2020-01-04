package dk.thrane.playground.serialization

import org.khronos.webgl.*

actual fun InputBuffer.readDouble(): Double {
    val arr = Uint8Array(8)
    repeat(8) { arr[it] = read().toByte() }
    return Float64Array(arr.buffer)[0]
}

actual fun OutputBuffer.writeDouble(value: Double) {
    Float64Array(array.unsafeCast<Int8Array>().buffer, ptr)[0] = value
    ptr += 8
}

