package dk.thrane.playground

import java.util.*

object JVMBase64Encoder : Base64Encoder {
    override fun decode(message: String): ByteArray {
        return Base64.getUrlDecoder().decode(message)
    }

    override fun encode(message: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encode(message).toString(Charsets.UTF_8)
    }
}
