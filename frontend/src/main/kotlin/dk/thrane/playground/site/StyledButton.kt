package dk.thrane.playground.site

import dk.thrane.playground.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement

private val baseButton = css {
    padding = 10.px
    borderRadius = 5.px
    display = "block"
    border = "0"
    cursor = "pointer"

    (matchSelf().withPseudoClass("focus")) {
        outline = "none"
    }
}

private val primaryStyle = css {
    backgroundColor = Theme.primary.base.toString()
    color = Theme.onPrimary.toString()

    (matchSelf().withPseudoClass("hover")) {
        backgroundColor = Theme.primary.c40.toString()
    }
}

private val outlineStyle = css {
    color = Theme.onBackground.toString()
    backgroundColor = Theme.background.toString()
    border = "1px dashed ${Theme.onBackground}"
    padding = 10.px

    (matchSelf().withPseudoClass("hover")) {
        border = "1px solid ${Theme.primary.base}"
        color = Theme.primary.base.toString()
    }
}

fun Element.primaryButton(
    attrs: CommonAttributes<HTMLButtonElement> = CommonAttributes(),
    type: String? = "button",
    children: HTMLButtonElement.() -> Unit
) {
    button(attrs.withClasses(baseButton, primaryStyle), type = type, children = children)
}

fun Element.outlineButton(
    attrs: CommonAttributes<HTMLButtonElement> = CommonAttributes(),
    type: String? = "button",
    children: HTMLButtonElement.() -> Unit
) {
    button(attrs.withClasses(baseButton, outlineStyle), type = type, children = children)
}
