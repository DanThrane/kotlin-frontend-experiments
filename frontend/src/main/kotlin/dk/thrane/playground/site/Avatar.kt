package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.BoundData
import dk.thrane.playground.components.ImmutableBoundData
import dk.thrane.playground.components.boundText
import org.w3c.dom.Element

private val style = css {
    width = 30.px
    height = 30.px
    borderRadius = 30.px
    display = "flex"
    justifyContent = "center"
    alignItems = "center"
    userSelect = "none"
    cursor = "pointer"
    padding = 6.px
    backgroundColor = Theme.secondary.c60.toString()
    textTransform = "uppercase"
    color = "white"
}

fun <E> Element.avatar(data: ImmutableBoundData<E>, usernameExtractor: (E) -> String, block: Element.() -> Unit = {}) {
    div(A(klass = style)) {
        boundText(data) { usernameExtractor(it).take(2) }

        block()
    }
}
