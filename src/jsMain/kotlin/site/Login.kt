package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.CardInStack
import dk.thrane.playground.components.Router
import dk.thrane.playground.components.cardStack
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement

private val loginRootStyle = css {
    display = "flex"
    alignItems = "center"
    justifyContent = "center"
    height = 100.vh
    width = 100.vw
    flexDirection = "column"
}

private val loginBoxStyle = css {
    minWidth = 300.px
    maxWidth = 600.px
    padding = 32.px
    backgroundColor = Theme.backgroundColor.darken(15).toString()
    borderRadius = 8.px
    boxSizing = "border-box"

    (matchAny()) {
        fontSize = 20.px
    }

    (byTag("input")) {
        borderRadius = 8.px
        backgroundColor = Theme.backgroundColor.darken(25).toString()
        border = "0"
        padding = 12.px
        width = 100.percent
        outline = "none"
        marginBottom = 8.px
    }

    (byTag("input").withPseudoElement("placeholder")) {
        color = Theme.primaryTextColor.darken(40).toString()
    }

    (byTag("button")) {
        marginTop = 16.px
        backgroundColor = Theme.backgroundColor.darken(25).toString()
        borderRadius = 8.px
        padding = 16.px
        width = 100.percent
        border = "0"
        textAlign = "center"
        cursor = "pointer"
    }

    (byTag("button").withPseudoClass("hover")) {
        backgroundColor = Theme.backgroundColor.darken(45).toString()
    }
}

private val titleStyle = css {
    marginBottom = 16.px
    fontSize = 32.px
}

fun Element.loginPage() {
    val usernameRef = Reference<HTMLInputElement>()
    val passwordRef = Reference<HTMLInputElement>()

    cardStack(
        AuthenticationStore.principal,
        CardInStack(
            predicate = { AuthenticationStore.principal.currentValue != null },
            card = {
                Router.push("/overview")
            }
        ),

        CardInStack(
            predicate = { AuthenticationStore.principal.currentValue == null },
            card = {
                div(A(klass = loginRootStyle)) {
                    div(A(klass = titleStyle)) {
                        h1 {
                            text(PRODUCT_NAME)
                        }
                    }

                    div(A(klass = loginBoxStyle)) {
                        form {
                            on("submit") { e ->
                                e.preventDefault()
                                scope.launch {
                                    AuthenticationStore.login(
                                        usernameRef.current.value,
                                        passwordRef.current.value
                                    )
                                }
                            }

                            input(
                                A(ref = usernameRef),
                                type = "text",
                                placeholder = "Username",
                                name = "username",
                                required = true
                            )

                            input(
                                A(ref = passwordRef),
                                type = "password",
                                placeholder = "Password",
                                name = "password",
                                required = true
                            )

                            button(type = "submit") {
                                text("Login")
                            }
                        }
                    }
                }
            }
        )
    )
}
