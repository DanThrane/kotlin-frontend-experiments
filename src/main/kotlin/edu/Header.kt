package edu

import A
import alignItems
import and
import backgroundColor
import byClass
import byTag
import color
import content
import css
import display
import div
import flexDirection
import fontSize
import h1
import BoundData
import boundClass
import boundClassByPredicate
import height
import marginBottom
import marginRight
import opacity
import org.w3c.dom.Element
import outline
import paddingLeft
import percent
import position
import pt
import px
import routeLink
import text
import textDecoration
import top
import transition
import width
import withPseudoClass
import withPseudoElement

private const val ACTIVE_PAGE_CLASS = "active"

private val outerBoxStyle = css {
    height = 80.px
    backgroundColor = Theme.primary.base.toString()
    color = Theme.onPrimary.toString()
    paddingLeft = 16.px
    marginBottom = 16.px
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
    }

    (byTag("a").withPseudoClass("hover").withPseudoElement("before")) {
        opacity = "1"
        transition = "opacity 0.25s ease-in"
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
    div(A(klass = outerBoxStyle)) {
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