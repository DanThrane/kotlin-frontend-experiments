package edu

import div
import reset
import routeLink
import router
import Route
import css
import fontFamily
import fontWeight
import margin
import matchAny
import padding
import px
import rawCSS
import text
import kotlin.browser.document

val globalTheme = css {
    margin = 0.px
    padding = 0.px

    (matchAny()) {
        fontFamily = "'Roboto', sans-serif"
    }
}

fun main() {
    rawCSS("@import url('https://fonts.googleapis.com/css?family=Roboto:400,500&display=swap');")

    val body = document.body!!
    body.classList.add(reset)
    body.classList.add(globalTheme)

    body.div {
        header()

        router {
            route(
                route = {},
                children = {
                    Header.activePage.currentValue = Page.HOME
                    text("Root")
                }
            )

            route(
                route = {
                    +"courses"
                },

                children = {
                    courses()
                }
            )

            route(
                route = {
                    +"calendar"
                },

                children = {
                    Header.activePage.currentValue = Page.CALENDAR
                    text("Calendar")
                }
            )
        }
    }
}