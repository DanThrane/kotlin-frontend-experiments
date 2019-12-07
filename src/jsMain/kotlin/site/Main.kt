package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.FontAwesome
import dk.thrane.playground.components.reset
import dk.thrane.playground.components.router
import dk.thrane.playground.components.toasts
import kotlin.browser.document

private val globalTheme = css {
    margin = 0.px
    padding = 0.px
    fontFamily = "'Poppins', sans-serif"

    backgroundColor = Theme.backgroundColor.toString()

    (byTag("a")) {
        color = Theme.primaryTextColor.toString()
    }
}

private val rootContainer = css {
    display = "flex"
    flexDirection = "column"
    minHeight = 100.vh
}

fun main() {
    val log = Log("Main")
    rawCSS("@import url('https://fonts.googleapis.com/css?family=Poppins&display=swap');\n")

    // Adding this to a class makes it too specific
    rawCSS(
        """
            body {
                color: ${Theme.primaryTextColor}
            }
            
        """.trimIndent()
    )

    FontAwesome.load()

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
