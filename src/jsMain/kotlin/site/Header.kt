package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.Router
import dk.thrane.playground.components.routeLink
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Element

private val headerStyle = css {
    display = "flex"
    alignItems = "center"
    justifyItems = "center"
    flexDirection = "row"
    height = 64.px
    marginLeft = 32.px
    marginRight = 32.px
    marginBottom = 32.px
}

private val logoStyle = css {
    fontSize = 24.px
    textDecoration = "none"
}

private fun Element.logo() {
    routeLink(A(klass = logoStyle), href = "/overview") {
        text(PRODUCT_NAME)
    }
}

private val menuStyle = css {
    display = "flex"
    alignItems = "center"
    justifyContent = "center"
    flexGrow = "1"

    (byTag("a")) {
        padding = 16.px
        textDecoration = "none"
        fontSize = 18.px
    }

    (byTag("a").withPseudoClass("hover")) {
        backgroundColor = Theme.backgroundColor.darken(25).toString()
    }
}

private fun Element.menu() {
    div(A(klass = menuStyle)) {
        routeLink(href = "/overview") {
            text("Home")
        }
        routeLink(href = "/test") {
            text("Test Page")
        }
        routeLink(href = "/test2") {
            text("Menu Link")
        }
    }
}

private val userMenuStyle = css {
    display = "flex"
    alignItems = "center"
    justifyContent = "center"
}

private fun Element.userMenu() {
    div(A(klass = userMenuStyle)) {
        avatar(AuthenticationStore.principal, { it?.username ?: "Guest" }) {
            on("click") {
                scope.launch {
                    AuthenticationStore.logout()
                    Router.push("/login")
                }
            }
        }
    }
}

fun Element.header() {
    div(A(klass = headerStyle)) {
        logo()
        menu()
        userMenu()
    }
}
