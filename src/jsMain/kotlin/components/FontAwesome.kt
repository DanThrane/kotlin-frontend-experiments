package dk.thrane.playground.components

import dk.thrane.playground.A
import dk.thrane.playground.i
import org.w3c.dom.Element
import kotlin.browser.document

enum class FontAwesomeStyle(val prefix: String) {
    SOLID("fas")
}

object FontAwesome {
    const val CHECK = "check"
    const val TIMES = "times"

    fun load() {
        val head = document.head!!
        head.appendChild(document.createElement("link").apply {
            setAttribute("rel", "stylesheet")
            setAttribute("type", "text/css")
            setAttribute("href", "/assets/css/all.css")
        })
    }
}

fun Element.icon(
    name: String,
    style: FontAwesomeStyle = FontAwesomeStyle.SOLID
) {
    i(A(klass = "${style.prefix} fa-${name}"))
}
