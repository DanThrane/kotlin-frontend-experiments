package dk.thrane.playground.site

import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import kotlin.math.max
import kotlin.math.min
import dk.thrane.playground.*

private val elevations = (1..10).map { elevation ->
    css {
        boxShadow = boxShadow(1, 1, elevation + 5, 0, "rgba(0, 0, 0, 0.25)")
    }
}

private val style = css {
    backgroundColor = Theme.surface.toString()
    color = Theme.onSurface.toString()
    padding = 16.px
    borderRadius = 3.px
}

private val verticalSpacingStyle = css {
    marginBottom = 8.px
}

fun Element.surface(
    attrs: CommonAttributes<HTMLDivElement> = CommonAttributes(),
    elevation: Int = 1,
    verticalSpacing: Boolean = true,
    children: HTMLDivElement.() -> Unit
) {
    div(
        attrs.withClasses(
            style,
            elevations[max(0, min(elevation, elevations.size))],
            if (verticalSpacing) verticalSpacingStyle else ""
        ),
        children
    )
}
