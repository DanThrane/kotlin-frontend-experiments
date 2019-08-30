package dk.thrane.playground

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class RPCNamespace(val namespace: String) {
    fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> rpc(
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
        requestId: Int,
        authorization: String? = null,
        request: BoundOutgoingMessage<Req>
    ): BoundOutgoingMessage<RequestSchema<Req>> {
        val outgoing = BoundOutgoingMessage(this.request)
        outgoing[this.request.requestId] = requestId
        outgoing[this.request.requestName] = requestName
        outgoing[this.request.authorization] = authorization
        outgoing[this.request.payload] = request
        return outgoing
    }

    fun outgoingResponse(
        requestId: Int,
        responseCode: ResponseCode,
        response: BoundOutgoingMessage<Res>
    ): BoundOutgoingMessage<ResponseSchema<Res>> {
        val outgoing = BoundOutgoingMessage(this.response)
        outgoing[this.response.requestId] = requestId
        outgoing[this.response.statusCode] = responseCode.statusCode
        outgoing[this.response.response] = response
        return outgoing
    }
}

object EmptySchema : MessageSchema<EmptySchema>() {
    // Empty
}

val EmptyRequestSchema = RequestSchema(EmptySchema)
val EmptyResponseSchema = ResponseSchema(EmptySchema)

class RequestSchema<R : MessageSchema<R>>(schema: R) : MessageSchema<RequestSchema<R>>() {
    val requestId = int(0)
    val requestName = string(1)
    val authorization = stringNullable(2)
    val payload = obj(3, schema)
}

class ResponseSchema<R : MessageSchema<R>>(schema: R) : MessageSchema<ResponseSchema<R>>() {
    val requestId = int(0)
    val statusCode = byte(1)
    val response = obj(2, schema)
}

enum class ResponseCode(val statusCode: Byte) {
    OK(0),
    BAD_REQUEST(1),
    UNAUTHORIZED(2),
    FORBIDDEN(3),
    NOT_FOUND(4),
    INTERNAL_ERROR(127)
}

open class RPCException(
    val statusCode: ResponseCode,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
