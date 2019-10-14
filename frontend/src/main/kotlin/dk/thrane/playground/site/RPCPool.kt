package dk.thrane.playground.site

import dk.thrane.playground.*
import kotlin.browser.document
import kotlin.js.Promise

val connectionPool = WSConnectionPool("ws://${document.location!!.host}")

/**
 * Calls an RPC with the default connectionPool and the default authorization token.
 */
fun <Req : MessageSchema<Req>, Res : MessageSchema<Res>> RPC<Req, Res>.call(
    message: BoundOutgoingMessage<Req>,
    vc: VirtualConnection = STATELESS_CONNECTION
): Promise<BoundMessage<Res>> {
    return connectionPool.useConnection(vc) { conn ->
        call(conn.copy(authorization = AuthenticationStore.token.currentValue), message)
    }.then { it }
}
