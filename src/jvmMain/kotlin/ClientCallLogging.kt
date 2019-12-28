package dk.thrane.playground

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private val callLogger = Log("RPCCall")

actual fun <Req> RPC<Req, *>.logCallStarted(requestMessage: Req) {
    callLogger.info("--> $requestName ($requestMessage)")
}

@UseExperimental(ExperimentalTime::class)
actual fun <Res> RPC<*, Res>.logCallEnded(
    result: Result<Res>,
    duration: Duration
) {
    callLogger.info("<-- $requestName ${duration.inMilliseconds}ms ($result)")
}
