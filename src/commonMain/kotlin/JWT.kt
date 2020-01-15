package dk.thrane.playground

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

sealed class JWTAlgorithmAndKey(val algorithm: JWTAlgorithm)

class HS256WithKey(val key: String) : JWTAlgorithmAndKey(JWTAlgorithm.HS256)

data class DecodedJWT(val header: JsonObject, val body: JsonObject)

enum class JWTAlgorithm(val encodedName: String) {
    HS256("HS256")
}

interface Base64Encoder {
    fun decode(message: String): ByteArray
    fun encode(message: ByteArray): String
}

class JWT(private val json: Json, private val base64Encoder: Base64Encoder) {
    fun <T> create(algAndKey: JWTAlgorithmAndKey, claims: T, serializer: KSerializer<T>): String {
        val header = base64Encoder.encode(
            """{"typ":"JWT","alg":"${algAndKey.algorithm.encodedName}"}"""
                .encodeToByteArray(throwOnInvalidSequence = true)
        )

        val body =
            base64Encoder.encode(json.stringify(serializer, claims).encodeToByteArray(throwOnInvalidSequence = true))

        val signature = when (algAndKey) {
            is HS256WithKey -> {
                base64Encoder.encode(
                    hs256(
                        algAndKey.key.encodeToByteArray(throwOnInvalidSequence = true),
                        "$header.$body".encodeToByteArray(throwOnInvalidSequence = true)
                    )
                )
            }
        }

        return buildString {
            append(header)
            append('.')
            append(body)
            append('.')
            append(signature)
        }
    }

    fun validate(token: String): DecodedJWT {
        val splitToken = token.split('.')
        require(splitToken.size == 3)
        val (header, body, _) = splitToken

        val parsedHeader = runCatching {
            json.parseJson(base64Encoder.decode(header).decodeToString(throwOnInvalidSequence = true)).jsonObject
        }.getOrNull() ?: throw IllegalArgumentException("Bad token. Header segment is invalid.")

        val parsedBody = runCatching {
            json.parseJson(base64Encoder.decode(body).decodeToString(throwOnInvalidSequence = true)).jsonObject
        }.getOrNull() ?: throw IllegalArgumentException("Bad token. Body segment is invalid.")

        return DecodedJWT(parsedHeader, parsedBody)
    }

    fun verify(token: String, expectedAlgorithmAndKey: JWTAlgorithmAndKey): DecodedJWT {
        val splitToken = token.split('.')
        require(splitToken.size == 3)
        val (header, body, signature) = splitToken
        val decodedJWT = validate(token)
        val algorithm =
            decodedJWT.header.getPrimitiveOrNull("alg")?.contentOrNull ?: throw JWTVerificationException("No algorithm")

        if (algorithm != expectedAlgorithmAndKey.algorithm.encodedName) {
            throw JWTVerificationException("Bad algorithm.")
        }

        when (expectedAlgorithmAndKey) {
            is HS256WithKey -> {
                val expectedSignature = base64Encoder.encode(
                    hs256(
                        expectedAlgorithmAndKey.key.encodeToByteArray(throwOnInvalidSequence = true),
                        "$header.$body".encodeToByteArray(throwOnInvalidSequence = true)
                    )
                )

                if (expectedSignature != signature) {
                    throw JWTVerificationException("Signature is invalid")
                }
            }
        }

        return decodedJWT
    }

    companion object
}

expect val JWT.Companion.default: JWT

class JWTVerificationException(message: String) : RuntimeException(message)

expect fun hs256(key: ByteArray, message: ByteArray): ByteArray
