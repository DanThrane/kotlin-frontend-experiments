package dk.thrane.playground

import kotlin.time.Duration

private val callLogger = Log("RPCCall")

actual fun <Req> RPC<Req, *>.logCallStarted(requestMessage: Req) {
    callLogger.info("--> $requestName ($requestMessage)")
}

actual fun <Res> RPC<*, Res>.logCallEnded(
    result: RPCResult<Res>,
    duration: Duration
) {
    callLogger.info("<-- $requestName ${duration.inMilliseconds}ms ($result)")
}
