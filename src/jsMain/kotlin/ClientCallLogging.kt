package dk.thrane.playground

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@UseExperimental(ExperimentalTime::class)
actual fun <Res> RPC<*, Res>.logCallEnded(
    result: Result<Res>,
    duration: Duration
) {
    if (result.isSuccess) {
        console.log("<-- OK $requestName ${duration.inMilliseconds}ms", result.getOrNull())
    } else {
        val exception = result.exceptionOrNull()
        if (exception is RPCException) {
            console.log("<-- ${exception.statusCode} $requestName ${exception.message} ${duration.inMilliseconds}ms")
        } else {
            console.log("<-- INTERNAL ERROR $requestName ${duration.inMilliseconds}", exception)
        }
    }
}

actual fun <Req> RPC<Req, *>.logCallStarted(requestMessage: Req) {
    console.log("--> $requestName", requestMessage)
}
