package dk.thrane.playground

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun hs256(key: ByteArray, message: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    val keySpec = SecretKeySpec(key, "HmacSHA256")
    mac.init(keySpec)
    return  mac.doFinal(message)
}
