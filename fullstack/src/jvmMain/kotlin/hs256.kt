package dk.thrane.playground

import kotlinx.serialization.json.Json
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun hs256(key: ByteArray, message: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    val keySpec = SecretKeySpec(key, "HmacSHA256")
    mac.init(keySpec)
    return mac.doFinal(message)
}

actual val JWT.Companion.default: JWT by lazy { JWT(Json.plain, JVMBase64Encoder) }
