package dk.thrane.playground.site

import kotlin.browser.document
import dk.thrane.playground.*

private val globalTheme = css {
    margin = 0.px
    padding = 0.px

    (matchAny()) {
        fontFamily = "'Roboto', sans-serif"
    }
}

private val rootContainer = css {
    display = "flex"
    flexDirection = "column"
    height = 100.vh
}

private val contentContainer = css {
    backgroundColor = Theme.background.toString()
    color = Theme.onBackground.toString()
    flexGrow = "10"
    flexShrink = "1"
    flexBasis = "auto"
    height = 100.percent
}

fun main() {
    rawCSS("@import url('https://fonts.googleapis.com/css?family=Roboto:400,500&display=swap');")

    val body = document.body!!
    body.classList.add(reset)
    body.classList.add(globalTheme)

    body.div(A(klass = rootContainer)) {
        toasts()
        header()

        div(A(klass = contentContainer)) {
            router {
                route(
                    route = {},
                    children = {
                        Header.activePage.currentValue = SitePage.HOME
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
                        Header.activePage.currentValue = SitePage.CALENDAR
                        text("Calendar")
                        repeat(10) {
                            Toasts.push(Toast(ToastType.INFO, "This is a test $it", 1000L))
                        }
                    }
                )
            }
        }
    }
}
