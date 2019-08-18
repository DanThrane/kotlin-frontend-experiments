package edu

import kotlin.browser.window
import kotlin.js.Promise

abstract class RPCNamespace(val namespace: String) {
    fun <Request, Response> rpc(name: String): RPC<Request, Response> = RPC(namespace, name)
}

data class RPC<Request, Response>(val namespace: String, val name: String)

fun <Request, Response> RPC<Request, Response>.call(request: Request): Promise<Response> {
    return Promise { resolve, reject ->
        // Just returning some fake data after some time
        window.setTimeout({
            @Suppress("UNCHECKED_CAST")
            when (this) {
                CoursesBackend.list -> {
                    resolve(listOf(Course("Foo"), Course("Bar")) as Response)
                }
            }
        }, 1000)

        // I think the parsing should be done via delegate properties. The Kotlin documentation even has an example
        // for JSON. I think we should explore this option. We would essentially be doing lazy parsing of attributes.
        // This could easily work if we model the wire protocol according to this.
    }
}
