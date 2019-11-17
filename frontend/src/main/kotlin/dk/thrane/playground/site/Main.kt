package dk.thrane.playground.site

import kotlin.browser.document
import dk.thrane.playground.*
import dk.thrane.playground.components.*

private val globalTheme = css {
    margin = 0.px
    padding = 0.px

    (matchAny()) {
        fontFamily = "'Poppins', sans-serif"
    }
}

private val rootContainer = css {
    display = "flex"
    flexDirection = "column"
    minHeight = 100.vh

    backgroundColor = Theme.backgroundColor.toString()

    (matchAny()) {
        color = Theme.primaryTextColor.toString()
    }
}

fun main() {
    rawCSS("@import url('https://fonts.googleapis.com/css?family=Poppins&display=swap');")

    val body = document.body!!
    body.classList.add(reset)
    body.classList.add(globalTheme)

    body.div(A(klass = rootContainer)) {
        toasts()

        router {
            route(
                route = { },

                children = {
                    text("Root")
                }
            )

            route(
                route = { +"login" },
                children = { loginPage() }
            )

            route(
                route = { +"overview" },
                children = {
                    header()
                    overviewPage()
                }
            )
        }
    }
}
