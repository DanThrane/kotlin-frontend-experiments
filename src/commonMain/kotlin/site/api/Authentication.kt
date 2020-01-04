package dk.thrane.playground.site.api

import dk.thrane.playground.*
import kotlinx.serialization.Serializable

object Authentication : RPCNamespace("authentication") {
    val login by call(LoginRequest.serializer(), LoginResponse.serializer())
    val logout by call(LogoutRequest.serializer(), EmptyMessage.serializer())
    val whoami by call(EmptyMessage.serializer(), Principal.serializer())
    val refresh by call(RefreshRequest.serializer(), RefreshResponse.serializer())
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String
)

@Serializable
data class LogoutRequest(
    val token: String
)

@Serializable
data class Principal(
    val username: String,
    val role: PrincipalRole
)

enum class PrincipalRole {
    USER,
    ADMIN
}

typealias RefreshRequest = RefreshResponse

@Serializable
data class RefreshResponse(
    val token: String
)
