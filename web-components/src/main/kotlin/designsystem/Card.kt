package dk.thrane.playground.designsystem

import dk.thrane.playground.*
import org.w3c.dom.HTMLElement

class Card : TemplatedTag(tag, template) {
    companion object {
        const val tag = "dt-card"

        init {
            define(Card())
        }

        private val template = createTemplate {
            css {
                host {
                    backgroundColor = variable(Theme.surface)
                    padding = 16.px
                    color = variable(Theme.onSurface)
                    display = "block"
                    boxShadow = boxShadow(0, 1, 2, 2, "rgba(0, 0, 0, 0.2)")
                }
            }

            div {
                slot()
            }
        }
    }
}

inline fun NodeCursor<*>.card(
    attrs: CommonAttributes<HTMLElement> = CommonAttributes(),
    children: (NodeCursor<HTMLElement>.() -> Unit) = {}
) {
    baseElement(Card.tag, attrs, children)
}
