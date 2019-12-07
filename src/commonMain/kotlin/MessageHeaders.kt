package dk.thrane.playground

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Serializable
data class RequestHeader(
    @SerialId(1)
    val connectionId: Int,

    @SerialId(2)
    val requestId: Int,

    @SerialId(3)
    val requestName: String,

    @SerialId(4)
    val authorization: String = "",

    @SerialId(5)
    val hasBody: Boolean
)

@Serializable
data class ResponseHeader(
    @SerialId(1)
    val connectionId: Int,

    @SerialId(2)
    val requestId: Int,

    @SerialId(3)
    val statusCode: Byte,

    @SerialId(4)
    val hasBody: Boolean
)
