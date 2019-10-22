package dk.thrane.playground.site

import org.w3c.dom.Element
import dk.thrane.playground.*
import dk.thrane.playground.components.BoundData
import dk.thrane.playground.components.boundClassByPredicate
import dk.thrane.playground.components.routeLink

private const val ACTIVE_PAGE_CLASS = "active"

private val style = css {
    height = 64.px
    backgroundColor = Theme.primary.base.toString()
    color = Theme.onPrimary.toString()
    paddingLeft = 16.px
    display = "flex"
    alignItems = "center"
    flexDirection = "row"

    (byTag("h1")) {
        fontSize = 20.pt
        marginRight = 100.px
    }

    (byTag("a")) {
        color = Theme.onPrimary.toString()
        textDecoration = "none"
        outline = "0"
    }

    ((byTag("a") and byClass(ACTIVE_PAGE_CLASS)).withPseudoElement("before")) {
        opacity = "1"
    }

    (byTag("a").withPseudoElement("before")) {
        opacity = "0"

        content = "''"
        display = "inline-block"
        width = 100.percent
        marginRight = (-100).percent
        height = 2.px
        backgroundColor = Theme.onPrimary.toString()
        position = "relative"
        top = 12.px
        transition = "opacity 0.25s ease-in"
    }

    (byTag("a").withPseudoClass("hover").withPseudoElement("before")) {
        opacity = "1"
    }

    (byClass("spacer")) {
        flexGrow = "1"
    }

    (matchSelf().directChild(matchAny())) {
        marginRight = 20.px
    }
}

object Header

fun Element.header() {
    div(A(klass = style)) {
        h1 {
            routeLink(href = "/") {
                text("\uD83C\uDF5F\uD83D\uDCAC")
            }
        }

        div(A(klass = "spacer"))

        headerLogin()
    }
}
