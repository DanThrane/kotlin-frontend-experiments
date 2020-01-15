package dk.thrane.playground

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

actual fun <Res> RPC<*, Res>.logCallEnded(
    result: Result<Res>,
    duration: Duration
) {
    if (result is Result.Success) {
        console.log("<-- OK $requestName ${duration.inMilliseconds}ms", result.result)
    } else if (result is Result.Failure) {
        val exception = result.exception
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
