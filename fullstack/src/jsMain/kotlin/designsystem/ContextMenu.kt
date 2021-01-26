package dk.thrane.playground.designsystem

import dk.thrane.playground.*
import org.w3c.dom.HTMLElement

class ContextMenu : CustomTag(tag) {
    val open = attr<Boolean>("open")

    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                display = "none"
                backgroundColor = variable(Theme.surface)
                color = variable(Theme.onSurface)
                boxShadow = boxShadow(0, 1, 5, 0, "rgba(0, 0, 0, 0.2)")
                minWidth = 200.px
            }

            (host.attributeEquals(open.name, "true")) {
                display = "grid"
                gridTemplateColumns = "1fr"
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-context-menu"

        init {
            define(ContextMenu())
        }
    }
}

inline fun NodeCursor<*>.contextMenu(
    attrs: CommonAttributes<CustomTagWrapper<ContextMenu>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<ContextMenu>>.() -> Unit) = {}
) {
    baseElement(ContextMenu.tag, attrs, children)
}
class ContextMenuItem : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)
        css {
            host {
                cursor = "pointer"
                padding = 12.px
            }

            (inHost { withPseudoClass("hover") }) {
                backgroundColor = variable(Theme.hoverOnSurface)
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-context-menu-item"

        init {
            define(ContextMenuItem())
        }
    }
}

inline fun NodeCursor<*>.contextMenuItem(
    attrs: CommonAttributes<CustomTagWrapper<ContextMenuItem>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<ContextMenuItem>>.() -> Unit) = {}
) {
    baseElement(ContextMenuItem.tag, attrs, children)
}
