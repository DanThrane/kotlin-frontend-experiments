package dk.thrane.playground

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class RPCNamespace(val namespace: String) {
    fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> call(
        request: Req,
        response: Res
    ): ReadOnlyProperty<RPCNamespace, RPC<Req, Res>> = object : ReadOnlyProperty<RPCNamespace, RPC<Req, Res>> {
        private var value: RPC<Req, Res>? = null

        override fun getValue(thisRef: RPCNamespace, property: KProperty<*>): RPC<Req, Res> {
            val captured = value
            if (captured == null) value = RPC(thisRef.namespace, property.name, request, response)
            return value!!
        }
    }
}

class RPC<Req : MessageSchema<Req>, Res : MessageSchema<Res>>(
    val namespace: String,
    val name: String,
    val requestPayload: Req,
    val responsePayload: Res
) {
    val requestName = "${namespace}.${name}"
    val request = RequestSchema(requestPayload)
    val response = ResponseSchema(responsePayload)

    fun outgoingRequest(
        connectionId: Int,
        requestId: Int,
        authorization: String? = null,
        request: BoundOutgoingMessage<Req>
    ): BoundOutgoingMessage<RequestSchema<Req>> {
        val outgoing = BoundOutgoingMessage(this.request)
        outgoing[this.request.connectionId] = connectionId
        outgoing[this.request.requestId] = requestId
        outgoing[this.request.requestName] = requestName
        outgoing[this.request.authorization] = authorization
        outgoing[this.request.payload] = request
        return outgoing
    }

    fun outgoingResponse(
        connectionId: Int,
        requestId: Int,
        responseCode: ResponseCode,
        response: BoundOutgoingMessage<Res>
    ): BoundOutgoingMessage<ResponseSchema<Res>> {
        val outgoing = BoundOutgoingMessage(this.response)
        outgoing[this.response.connectionId] = connectionId
        outgoing[this.response.requestId] = requestId
        outgoing[this.response.statusCode] = responseCode.statusCode
        outgoing[this.response.response] = response
        return outgoing
    }

    override fun toString() = "RPC($requestName)"
}

object EmptySchema : MessageSchema<EmptySchema>() {
    // Empty
}

val EmptyRequestSchema = RequestSchema(EmptySchema)
val EmptyResponseSchema = ResponseSchema(EmptySchema)

fun EmptyOutgoingMessage() = buildOutgoing(EmptySchema) {}

object OpenConnectionSchema : MessageSchema<OpenConnectionSchema>() {
    val id = int(0)
}

object CloseConnectionSchema : MessageSchema<CloseConnectionSchema>() {
    val id = int(0)
}

object Connections : RPCNamespace("connections") {
    val open by call(OpenConnectionSchema, EmptySchema)
    val close by call(CloseConnectionSchema, EmptySchema)
}

class RequestSchema<R : MessageSchema<R>>(schema: R) : MessageSchema<RequestSchema<R>>() {
    val connectionId = int(0)
    val requestId = int(1)
    val requestName = string(2)
    val authorization = stringNullable(3)
    val payload = obj(4, schema)
}

class ResponseSchema<R : MessageSchema<R>>(schema: R) : MessageSchema<ResponseSchema<R>>() {
    val connectionId = int(0)
    val requestId = int(1)
    val statusCode = byte(2)
    val response = obj(3, schema)
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
