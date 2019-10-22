package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement

private val dropdownStyle = css {
    position = "relative"
    left = (-80).percent
}

private val formStyle = css {
    (matchSelf().directChild(byTag("div"))) {
        marginTop = 12.px
    }
}

private val innerStyle = css {
    position = "fixed"
    display = "inline-block"
}

fun Element.headerLogin() {
    val isHidden = BoundData(true)
    val username = Reference<HTMLInputElement>()
    val password = Reference<HTMLInputElement>()

    div {
        /*
        outlineButton {
            on(Events.click) {
                isHidden.currentValue = !isHidden.currentValue
            }

            text("Hello, ")
            boundText(AuthenticationStore.principal) { principal ->
                principal?.username ?: "Guest"
            }
        }
         */

        avatar(AuthenticationStore.principal, { it?.username ?: "Guest" }) {
            on(Events.click) {
                isHidden.currentValue = !isHidden.currentValue
            }
        }

        div {
            div(A(klass = innerStyle)) {
                surface(A(klass = dropdownStyle)) {
                    boundClassByPredicate(isHidden, hiddenClass)

                    cardStack(
                        AuthenticationStore.principal,
                        CardInStack(
                            { AuthenticationStore.principal.currentValue == null },
                            {
                                form(A(klass = formStyle)) {
                                    on(Events.submit) { e ->
                                        e.preventDefault()
                                        isHidden.currentValue = true

                                        AuthenticationStore.login(username.current.value, password.current.value)
                                    }

                                    div {
                                        label {
                                            text("Username")
                                            br()
                                            styledInput(A(ref = username), type = "text")
                                        }
                                    }

                                    div {
                                        label {
                                            text("Password")
                                            br()
                                            styledInput(A(ref = password), type = "password")
                                        }
                                    }

                                    div {
                                        primaryButton(A(klass = fullWidth), type = "submit") {
                                            text("Login")
                                        }
                                    }
                                }
                            }
                        ),

                        CardInStack(
                            { AuthenticationStore.principal.currentValue != null },
                            {
                                div {
                                    primaryButton(A(klass = fullWidth)) {
                                        on(Events.click) { e ->
                                            e.preventDefault()
                                            AuthenticationStore.logout()
                                            isHidden.currentValue = true
                                        }

                                        text("Logout")
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
    }
}
