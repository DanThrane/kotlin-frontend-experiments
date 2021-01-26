package dk.thrane.playground.designsystem

import dk.thrane.playground.*
import org.w3c.dom.HTMLElement

class NavBar : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                padding = 16.px
                boxShadow = boxShadow(0, 1, 2, 2, "rgba(0, 0, 0, 0.2)")
                width = 100.percent
                display = "flex"
                boxSizing = "border-box"
                alignItems = "center"
            }

            (byTag("h1").slotted()) {
                margin = 0.px
                flexGrow = "1"
            }

            (byTag(Button.tag).slotted() or byTag(LinkButton.tag).slotted()) {
                marginLeft = 8.px
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-nav-bar"

        init {
            define(NavBar())
        }
    }
}

inline fun NodeCursor<*>.navBar(
    attrs: CommonAttributes<CustomTagWrapper<NavBar>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<NavBar>>.() -> Unit) = {}
) {
    baseElement(NavBar.tag, attrs, children)
}
