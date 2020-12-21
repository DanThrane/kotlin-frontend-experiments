package dk.thrane.playground.site.testing

import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.Serializable

@Serializable data class Message(val message: String)

object Testing : RPCNamespace("testing") {
    val oneWay by call(Message.serializer(), EmptyMessage.serializer())
    val requestResponse by call(Message.serializer(), Message.serializer())
    val error by call(Message.serializer(), Message.serializer())
}
