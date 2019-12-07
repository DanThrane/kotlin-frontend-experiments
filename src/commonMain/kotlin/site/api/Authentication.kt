package dk.thrane.playground.site.api

import dk.thrane.playground.*
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

object Authentication : RPCNamespace("authentication") {
    val login by call(LoginRequest.serializer(), LoginResponse.serializer())
    val logout by call(LogoutRequest.serializer(), EmptyMessage.serializer())
    val whoami by call(EmptyMessage.serializer(), Principal.serializer())
}

@Serializable
data class LoginRequest(
    @SerialId(1)
    val username: String,

    @SerialId(2)
    val password: String
)

@Serializable
data class LoginResponse(
    @SerialId(1)
    val token: String
)

@Serializable
data class LogoutRequest(
    @SerialId(1)
    val token: String
)

@Serializable
data class Principal(
    @SerialId(1)
    val username: String,

    @SerialId(2)
    val role: PrincipalRole
)

enum class PrincipalRole {
    USER,
    ADMIN
}
