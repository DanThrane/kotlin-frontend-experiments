package dk.thrane.playground.site.api

import dk.thrane.playground.EmptySchema
import dk.thrane.playground.MessageSchema
import dk.thrane.playground.RPCNamespace
import dk.thrane.playground.buildOutgoing

object Authentication : RPCNamespace("authentication") {
    val login by call(LoginRequest, LoginResponse)
    val logout by call(LogoutRequest, EmptySchema)
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

