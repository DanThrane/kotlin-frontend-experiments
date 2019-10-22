package dk.thrane.playground.site

import dk.thrane.playground.*
import org.w3c.dom.Element

private val wrapper = css {
    maxWidth = 1200.px
    margin = "0 auto"
}

private val inner = css {
    marginTop = 16.px
    marginLeft = 32.px
    marginRight = 32.px
}

fun Element.content(block: Element.() -> Unit) {
    div(A(klass = wrapper)) {
        div(A(klass = inner)) {
            block()
        }
    }
}
