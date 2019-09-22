package dk.thrane.playground.site

import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import kotlin.math.max
import kotlin.math.min
import dk.thrane.playground.*

private val elevations = (1..10).map { elevation ->
    css {
        backgroundColor = Theme.surface.toString()
        color = Theme.onSurface.toString()
        boxShadow = boxShadow(1, 1, elevation + 5, 0, "rgba(0, 0, 0, 0.25)")
        padding = 16.px
        borderRadius = 3.px
    }
}

fun Element.surface(
    attrs: CommonAttributes<HTMLDivElement> = CommonAttributes(),
    elevation: Int = 1,
    children: HTMLDivElement.() -> Unit
) {
    div(
        attrs.copy(classes = attrs.classes + elevations[max(0, min(elevation, elevations.size))]),
        children
    )
}
