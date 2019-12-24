package dk.thrane.playground

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.math.ceil

object JSBase64Encoder : Base64Encoder {
    override fun decode(message: String): ByteArray {
        val output = Uint8Array(ceil((message.length * 6) / 8.0).toInt())
        var outputPtr = 0

        var uint24 = 0
        for (messageIdx in message.indices) {
            val nMod4 = messageIdx % 4
            uint24 = uint24 or (base64ToUint6(charCodeAt(message, messageIdx)) shl (18 - 6 * nMod4))
            if (nMod4 == 3 || messageIdx == message.lastIndex) {
                for (i in 0 until 3) {
                    if (outputPtr >= output.length) break
                    output[outputPtr++] = ((uint24 ushr (16 ushr i and 24)) and 255).toByte()
                }
                uint24 = 0
            }
        }

        return Int8Array(output.buffer).unsafeCast<ByteArray>()
    }

    override fun encode(message: ByteArray): String {
        val nativeArray = Uint8Array(message.unsafeCast<Int8Array>().buffer)
        val padding = (3 - (nativeArray.length % 3)) % 3

        var nUint24 = 0
        val builder = StringBuilder()
        for (idx in message.indices) {
            val nMod3 = idx % 3
            val toInt = nativeArray[idx].toInt()
            nUint24 = nUint24 or (toInt shl (16 ushr nMod3 and 24))

            if (nMod3 == 2 || idx == message.lastIndex) {
                builder.append(fromCharCode(uint6ToBase64((nUint24 ushr 18) and 63)))
                builder.append(fromCharCode(uint6ToBase64((nUint24 ushr 12) and 63)))
                builder.append(fromCharCode(uint6ToBase64((nUint24 ushr 6) and 63)))
                builder.append(fromCharCode(uint6ToBase64(nUint24 and 63)))
                nUint24 = 0
            }
        }

        return builder.toString().let {
            when (padding) {
                0 -> it
                else -> it.substring(0, it.length - padding)
            }
        }
    }

    private fun charCodeAt(message: String, idx: Int): Int {
        return js("message.charCodeAt(idx)")
    }

    private fun fromCharCode(charCode: Int): String {
        return js("String.fromCharCode(charCode)") as String
    }

    private fun uint6ToBase64(int: Int): Int {
        return when {
            int < 26 -> {
                int + 65
            }

            int < 52 -> {
                int + 71
            }

            int < 62 -> {
                int - 4
            }

            int == 62 -> {
                45
            }

            int == 63 -> {
                95
            }

            else -> {
                65
            }
        }
    }

    private fun base64ToUint6(int: Int): Int {
       return when (int) {
            in 65..90 -> {
                int - 65
            }
            in 97..122 -> {
                int - 71
            }
            in 48..57 -> {
                int + 4
            }
            45 -> {
                62
            }
            95 -> {
                63
            }
            else -> {
                0
            }
        }
    }
}
