package dk.thrane.playground.site

import dk.thrane.playground.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement

private val style = css {
    padding = 8.px
    borderRadius = 8.px
    border = "1px solid ${Theme.primary.base}"
    outline = "none"

    (matchSelf().withPseudoClass("focus")) {
        border = "2px solid ${Theme.primary.c70}"
    }
}

fun Element.styledInput(
    attrs: CommonAttributes<HTMLInputElement> = CommonAttributes(),
    type: String,
    children: (HTMLInputElement).() -> Unit = {}
) {
    input(attrs.withClasses(style), type = type, children = children)
}
