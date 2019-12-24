package dk.thrane.playground

interface RPCHandlerContext<Req, Res> {
    val request: Req
    val authorization: String?
    val socketId: String
    val rpc: RPC<Req, Res>
}

class RPCHandlerContextImpl<Req, Res>(
    override val request: Req,
    override val authorization: String?,
    override val socketId: String,
    override val rpc: RPC<Req, Res>
) : RPCHandlerContext<Req, Res>

fun <Req, Res> RPCHandlerContext<Req, Res>.respond(
    response: Res,
    code: ResponseCode = ResponseCode.OK
): Pair<ResponseCode, Res> {
    return Pair(code, response)
}

typealias RPCHandler<Req, Res> = suspend RPCHandlerContext<Req, Res>.() -> Pair<ResponseCode, Res>
typealias UntypedRPChandler = suspend RPCHandlerContext<*, *>.() -> Pair<ResponseCode, *>

