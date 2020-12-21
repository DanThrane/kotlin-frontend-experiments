package dk.thrane.playground

import kotlinx.serialization.json.Json

actual fun hs256(key: ByteArray, message: ByteArray): ByteArray =
    throw NotImplementedError("hs256 not supported for JS")

actual val JWT.Companion.default: JWT by lazy { JWT(Json.plain, JSBase64Encoder) }
