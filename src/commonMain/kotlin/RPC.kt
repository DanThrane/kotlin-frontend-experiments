package dk.thrane.playground

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class RPCNamespace(val namespace: String) {
    fun <Req, Res> call(
        request: KSerializer<Req>,
        response: KSerializer<Res>
    ): ReadOnlyProperty<RPCNamespace, RPC<Req, Res>> = object : ReadOnlyProperty<RPCNamespace, RPC<Req, Res>> {
        private var value: RPC<Req, Res>? = null

        override fun getValue(thisRef: RPCNamespace, property: KProperty<*>): RPC<Req, Res> {
            val captured = value
            if (captured == null) value = RPC(thisRef.namespace, property.name, request, response)
            return value!!
        }
    }
}

class RPC<Req, Res>(
    val namespace: String,
    val name: String,
    val requestSerializer: KSerializer<Req>,
    val responseSerializer: KSerializer<Res>
) {
    val requestName = "${namespace}.${name}"

    override fun toString() = "RPC($requestName)"
}

@Serializable
data class OpenConnectionSchema(
    val id: Int
)

@Serializable
class CloseConnectionSchema(
    val id: Int
)

@Serializable
object EmptyMessage

object Connections : RPCNamespace("connections") {
    val open by call(OpenConnectionSchema.serializer(), EmptyMessage.serializer())
    val close by call(CloseConnectionSchema.serializer(), EmptyMessage.serializer())
}

enum class ResponseCode(val statusCode: Byte) {
    OK(0),
    BAD_REQUEST(1),
    UNAUTHORIZED(2),
    FORBIDDEN(3),
    NOT_FOUND(4),
    INTERNAL_ERROR(127);

    companion object {
        fun valueOf(code: Byte): ResponseCode {
            return values().find { it.statusCode == code } ?: INTERNAL_ERROR
        }
    }
}

open class RPCException(
    val statusCode: ResponseCode,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
