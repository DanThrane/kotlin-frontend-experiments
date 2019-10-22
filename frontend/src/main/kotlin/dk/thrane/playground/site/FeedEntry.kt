package dk.thrane.playground.site

import dk.thrane.playground.*
import org.w3c.dom.Element

private val style = css {
    marginBottom = 32.px
}

private val mediaContainerStyle = css {
    display = "flex"
    justifyContent = "center"
    maxHeight = 350.px
    width = 100.percent
}

private fun Element.mediaContainer(block: Element.() -> Unit) {
    div(A(klass = mediaContainerStyle)) {
        block()
    }
}

fun Element.feedEntry() {
    div(A(klass = style)) {
        surface(elevation = 1) {
            mediaContainer {
                video(
                    src = "https://fie.dog/media/fie.mp4",
                    autoplay = false,
                    showControls = true
                )
            }
        }

        comments()
    }
}
