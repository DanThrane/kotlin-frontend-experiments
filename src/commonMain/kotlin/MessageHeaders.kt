package dk.thrane.playground

import kotlinx.serialization.Serializable

@Serializable
data class RequestHeader(
    val connectionId: Int,
    val requestId: Int,
    val requestName: String,
    val hasBody: Boolean,
    val authorization: String? = null
)

@Serializable
data class ResponseHeader(
    val connectionId: Int,
    val requestId: Int,
    val statusCode: Byte,
    val hasBody: Boolean
)
