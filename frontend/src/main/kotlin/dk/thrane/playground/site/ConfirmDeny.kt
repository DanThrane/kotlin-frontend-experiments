package dk.thrane.playground.site

import dk.thrane.playground.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement

private val baseStyle = css {
    backgroundColor = "transparent"
    border = "0"
    width = 32.px
    height = 32.px
    outline = "none"
}

private val confirmStyle = css {
    color = "#43A047 !important"
}

private val denyStyle = css {
    color = "#D50000 !important"
}

fun Element.confirmButton(children: HTMLButtonElement.() -> Unit) {
    button(A(classes = setOf(baseStyle, confirmStyle))) {
        text("✔️")
        children()
    }
}

fun Element.denyButton(children: HTMLButtonElement.() -> Unit) {
    button(A(classes = setOf(baseStyle, denyStyle))) {
        text("❌")
        children()
    }
}
