package dk.thrane.playground.site.api

import dk.thrane.playground.*

object Authentication : RPCNamespace("authentication") {
    val login by call(LoginRequest, LoginResponse)
    val logout by call(LogoutRequest, EmptySchema)
    val whoami by call(EmptySchema, PrincipalSchema)
}

object LoginRequest : MessageSchema<LoginRequest>() {
    val username = string(0)
    val password = string(1)
}

fun LoginRequest(username: String, password: String) = buildOutgoing(LoginRequest) { msg ->
    msg[LoginRequest.username] = username
    msg[LoginRequest.password] = password
}

object LoginResponse : MessageSchema<LoginResponse>() {
    val token = string(0)
}

fun LoginResponse(token: String) = buildOutgoing(LoginResponse) { msg ->
    msg[LoginResponse.token] = token
}

object LogoutRequest : MessageSchema<LogoutRequest>() {
    val token = string(0)
}

fun LogoutRequest(token: String) = buildOutgoing(LogoutRequest) { msg ->
    msg[LogoutRequest.token] = token
}

object PrincipalSchema : MessageSchema<PrincipalSchema>() {
    val username = string(0)
    val role = string(1)
}

fun PrincipalSchema(username: String, role: String) = buildOutgoing(PrincipalSchema) { msg ->
    msg[PrincipalSchema.username] = username
    msg[PrincipalSchema.role] = role
}

fun BoundMessage<PrincipalSchema>.toModel() = Principal(
    this[PrincipalSchema.username],
    PrincipalRole.valueOf(this[PrincipalSchema.role])
)

enum class PrincipalRole {
    USER,
    ADMIN
}

data class Principal(
    val username: String,
    val role: PrincipalRole
)
