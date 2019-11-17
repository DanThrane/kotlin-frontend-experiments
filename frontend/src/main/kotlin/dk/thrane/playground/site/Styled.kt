package dk.thrane.playground.site

import dk.thrane.playground.A
import dk.thrane.playground.div
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

inline fun Element.styled(klass: String, children: HTMLDivElement.() -> Unit) {
    div(A(klass = klass), children)
}
