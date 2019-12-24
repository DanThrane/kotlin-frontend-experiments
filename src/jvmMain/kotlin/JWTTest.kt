package dk.thrane.playground

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.util.*

@Serializable
data class DummyBody(val iss: String, val exp: Long)

fun main() {
    val jwt = JWT(Json(JsonConfiguration.Default), JVMBase64Encoder)
    val algAndKey = HS256WithKey("test")
    val token = jwt.create(algAndKey, DummyBody("joe", 1300819380L), DummyBody.serializer())
    println(token)
    println(jwt.validate(token) )
    println(jwt.verify(token, algAndKey))
}

object JVMBase64Encoder : Base64Encoder {
    override fun decode(message: String): ByteArray {
        return Base64.getUrlDecoder().decode(message)
    }

    override fun encode(message: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encode(message).toString(Charsets.UTF_8)
    }
}
