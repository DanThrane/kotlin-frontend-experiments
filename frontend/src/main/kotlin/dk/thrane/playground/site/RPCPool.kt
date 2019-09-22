package dk.thrane.playground.site

import dk.thrane.playground.WSConnectionPool
import kotlin.browser.document

val connectionPool = WSConnectionPool("ws://${document.location!!.host}")
