package dk.thrane.playground.site

import dk.thrane.playground.*
import kotlinx.coroutines.GlobalScope
import kotlin.browser.document

val connectionPool = WSConnectionPool(
    connectionFactory = {
        JSWSConnection("ws://${document.location!!.host}", GlobalScope)
    }
)

/**
 * Calls an RPC with the default connectionPool and the default authorization token.
 */
suspend fun <Req, Res> RPC<Req, Res>.call(message: Req): Res {
    return connectionPool.useConnection { conn ->
        call(conn.copy(authorization = AuthenticationStore.getAccessTokenOrRefresh()), message)
    }
}
