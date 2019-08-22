package edu

import org.w3c.dom.Element
import dk.thrane.playground.*

private const val ACTIVE_PAGE_CLASS = "active"

private val style = css {
    height = 80.px
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
        marginRight = 20.px
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
}

enum class Page {
    HOME,
    COURSES,
    CALENDAR
}

object Header {
    val activePage = BoundData(Page.HOME)
}

fun Element.header() {
    div(A(klass = style)) {
        h1 {
            routeLink(href = "/") {
                text("Board")
            }
        }

        routeLink(href = "/") {
            boundClassByPredicate(Header.activePage, ACTIVE_PAGE_CLASS) { it == Page.HOME }
            text("Home")
        }

        routeLink(href = "/courses") {
            boundClassByPredicate(Header.activePage, ACTIVE_PAGE_CLASS) { it == Page.COURSES }
            text("Courses")
        }

        routeLink(href = "/calendar") {
            boundClassByPredicate(Header.activePage, ACTIVE_PAGE_CLASS) { it == Page.CALENDAR }
            text("Calendar")
        }
    }
}
