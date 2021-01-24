package dk.thrane.playground.designsystem

import dk.thrane.playground.*
import org.w3c.dom.HTMLElement

private fun NodeCursor<*>.buttonCss() {
    css {
        val selectors = listOf(byTag("a"), byTag("button"))
        (selectors.anyOf()) {
            cursor = "pointer"
            backgroundColor = variable(Theme.surface)
            color = variable(Theme.onSurface)
            borderRadius = 5.px
            padding = 12.px
            borderWidth = 2.px
            borderStyle = "solid"
            borderColor = variable(Theme.primary)
            display = "block"
            minWidth = 150.px
            textAlign = "center"
            transition = "transform .125s"
        }

        (selectors.map { it.withPseudoClass("hover") }.anyOf()) {
            transform = "scale(1.05)"
        }
    }
}

class Button : CustomTag(tag) {
    val type = attr<String>("type")

    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        buttonCss()
        nativeButton {
            type.asState().listen { newType ->
                node.type = newType ?: "button"
            }

            slot()
        }
    }

    companion object {
        const val tag = "dt-button"

        init {
            define(Button())
        }
    }
}

inline fun NodeCursor<*>.button(
    attrs: CommonAttributes<CustomTagWrapper<Button>> = CommonAttributes(),
    type: String? = "button",
    children: (NodeCursor<CustomTagWrapper<Button>>.() -> Unit) = {}
) {
    baseElement(Button.tag, attrs.mergeWith(mapOf("type" to type)), children)
}

class LinkButton : CustomTag(tag) {
    val href = attr<String>("href")

    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)
        buttonCss()

        a {
            // TODO More attributes
            href.asState().listen { newHref ->
                node.href = newHref ?: "javascript:void(0)"
            }

            slot()
        }
    }

    companion object {
        const val tag = "dt-link-button"

        init {
            define(LinkButton())
        }
    }
}

inline fun NodeCursor<*>.linkButton(
    attrs: CommonAttributes<CustomTagWrapper<LinkButton>> = CommonAttributes(),
    href: String = "javascript:void(0)",
    children: (NodeCursor<CustomTagWrapper<LinkButton>>.() -> Unit) = {}
) {
    baseElement(LinkButton.tag, attrs.mergeWith(mapOf("href" to href)), children)
}