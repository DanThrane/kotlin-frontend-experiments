import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

val flexCss = css { display = "flex" }

object AlignItems {
    val stretch = css { alignItems = "stretch" }
    val center = css { alignItems = "center" }
    val start = css { alignItems = "start" }
    val end = css { alignItems = "end" }
}

object JustifyItems {
    val stretch = css { justifyItems = "stretch" }
    val center = css { justifyItems = "center" }
    val start = css { justifyItems = "start" }
    val end = css { justifyItems = "end" }
}

object JustifyContent {
    val center = css { justifyContent = "center" }
    val start = css { justifyContent = "start" }
    val end = css { justifyContent = "end" }
    val flexStart = css { justifyContent = "flex-start" }
    val flexEnd = css { justifyContent = "flex-end" }
    val left = css { justifyContent = "left" }
    val right = css { justifyContent = "right" }
    val normal = css { justifyContent = "normal" }
    val spaceBetween = css { justifyContent = "space-between" }
    val spaceAround = css { justifyContent = "space-around" }
    val spaceEvenly = css { justifyContent = "space-evenly" }
    val stretch = css { justifyContent = "stretch" }
    val safeCenter = css { justifyContent = "safe center" }
    val unsafeCenter = css { justifyContent = "unsafe center" }
}

inline fun Element.flex(
    attrs: CommonAttributes<HTMLDivElement> = CommonAttributes(),
    children: HTMLDivElement.() -> Unit
) {
    return div(
        attrs.copy(classes = attrs.classes + setOf(flexCss)),
        children = children
    )
}