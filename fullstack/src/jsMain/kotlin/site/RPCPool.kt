package dk.thrane.playground.site

import dk.thrane.playground.*
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope

val connection = JSWSConnection("ws://${document.location!!.host}", GlobalScope)
/*
val authenticator: Authenticator = { conn ->
    ConnectionWithAuthorization(conn, AuthenticationStore.getAccessTokenOrRefresh())
}
 */

/**
 * Calls an RPC with the default connectionPool and the default authorization token.
 */
suspend fun <Req, Res> RPC<Req, Res>.call(message: Req): Res {
    return call(connection, TODO("authenticator"), message)
}
