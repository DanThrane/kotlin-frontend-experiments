package dk.thrane.playground.psql

private val HEX_ARRAY = "0123456789abcdef".toCharArray()

fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
        val v: Int = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = HEX_ARRAY[v ushr 4]
        hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
    }
    return String(hexChars)
}

fun hexToBytes(hexString: String): ByteArray {
    val output = ByteArray(hexString.length / 2)
    for (i in output.indices) {
        val first = hexString[i * 2].toByte()
        val second = hexString[i * 2 + 1].toByte()

        val msb = if (first >= '0'.toByte() && first <= '9'.toByte()) {
            first - '0'.toByte()
        } else if (first >= 'a'.toByte() && first <= 'f'.toByte()) {
            first - 'a'.toByte() + 10
        } else if (first >= 'A'.toByte() && first <= 'F'.toByte()) {
            first - 'A'.toByte() + 10
        } else {
            throw IllegalArgumentException("invalid hex char at ${i * 2}")
        }

        val lsb = if (second >= '0'.toByte() && second <= '9'.toByte()) {
            second - '0'.toByte()
        } else if (second >= 'a'.toByte() && second <= 'f'.toByte()) {
            second - 'a'.toByte() + 10
        } else if (second >= 'A'.toByte() && second <= 'F'.toByte()) {
            second - 'A'.toByte() + 10
        } else {
            throw IllegalArgumentException("invalid hex char at ${i * 2 + 1}")
        }

        output[i] = ((msb shl 4) or (lsb) and 0xFF).toByte()
    }

    return output
}
