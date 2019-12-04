package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.FontAwesome
import dk.thrane.playground.components.icon
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement

private val baseStyle = css {
    backgroundColor = "transparent"
    border = "0"
    width = 32.px
    height = 32.px
    outline = "none"
    fontSize = 20.px
    cursor = "pointer"

    (matchSelf().withPseudoClass("hover")) {
        transform = "scale(1.4)"
    }
}

private val confirmStyle = css {
    color = Theme.confirmColor.toString()
}

private val denyStyle = css {
    color = Theme.denyColor.toString()
}

fun Element.confirmButton(children: HTMLButtonElement.() -> Unit) {
    button(A(classes = setOf(baseStyle, confirmStyle))) {
        icon(FontAwesome.CHECK)
        children()
    }
}

fun Element.denyButton(children: HTMLButtonElement.() -> Unit) {
    button(A(classes = setOf(baseStyle, denyStyle))) {
        icon(FontAwesome.TIMES)
        children()
    }
}
