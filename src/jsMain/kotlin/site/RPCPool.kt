package dk.thrane.playground.site

import dk.thrane.playground.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.browser.document
import kotlin.js.Promise

val connectionPool = WSConnectionPool("ws://${document.location!!.host}")

/**
 * Calls an RPC with the default connectionPool and the default authorization token.
 */
suspend fun <Req, Res> RPC<Req, Res>.call(
    message: Req,
    vc: VirtualConnection = STATELESS_CONNECTION
): Res {
    return connectionPool.useConnection(vc) { conn ->
        call(conn.copy(authorization = AuthenticationStore.token.currentValue), message)
    }.await()
}
