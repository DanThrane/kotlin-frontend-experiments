package dk.thrane.playground

import dk.thrane.playground.designsystem.button
import dk.thrane.playground.designsystem.navBar
import dk.thrane.playground.widgets.post
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

fun main() {
    document.addEventListener("DOMContentLoaded", {
        try {
            CustomTag.define(App())

            document.head!!.toCursor().apply {
                css {
                    root {
                        setVariable(Theme.primary, "#3860B8")
                        setVariable(Theme.onPrimary, "#FFFFFF")
                        setVariable(Theme.surface, "white")
                        setVariable(Theme.onSurface, "black")
                        setVariable(Theme.hoverOnSurface, "rgba(0, 0, 0, 0.1)")
                        setVariable(Theme.contentTopOffset, 77.px)
                        userSelect = "none"
                        fontFamily = "Roboto"
                        fontSize = 11.pt
                        boxSizing = "border-box"
                    }

                    (byTag("body")) {
                        margin = 0.px
                    }
                }

                title {
                    boundElement(applicationTitle) {
                        text("${applicationTitle.current} | Fie.dog")
                    }
                }
            }

            document.body!!.toCursor().apply {
                app {  }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    })
}

val applicationTitle = stateOf("Title")
fun CustomTag.useTitle(title: String) {
    lateinit var previousTitle: String
    onMount {
        previousTitle = applicationTitle.current
        applicationTitle.current = title
    }

    onUnmount { applicationTitle.current = previousTitle }
}

object Theme {
    val surface = CSSVar("surface")
    val onSurface = CSSVar("on-surface")
    val primary = CSSVar("primary")
    val onPrimary = CSSVar("on-primary")
    val contentTopOffset = CSSVar("content-top-offset")

    val hoverOnSurface = CSSVar("hover-on-surface")
}

class App : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)
        navBar {
            h1 {
                text("🐶 ")
                boundElement(applicationTitle) {
                    text(applicationTitle.current)
                }
            }

            button {
                text("Next ⏭")
            }

            button {
                text("Random 🎲")
            }
        }

        post {  }
    }

    companion object {
        const val tag = "dt-app"
    }
}

inline fun NodeCursor<*>.app(
    attrs: CommonAttributes<CustomTagWrapper<App>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<App>>.() -> Unit) = {}
) {
    baseElement(App.tag, attrs, children)
}
