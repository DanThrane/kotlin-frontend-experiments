package dk.thrane.playground.widgets

import dk.thrane.playground.*
import org.w3c.dom.HTMLElement

class Post : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                overflow = "hidden"
                height = "calc(100vh - ${Theme.contentTopOffset})"
                marginLeft = 50.px
                marginRight = 50.px
                display = "flex"
                alignItems = "center"
                boxSizing = "border-box"
            }

            (byTag("img")) {
                objectFit = "contain"
                maxWidth = 30.vw
                maxHeight = 80.vh
            }
        }

        useTitle("Fie")

        img(src = "https://fie.dog/media/fie2.jpg")
    }

    companion object {
        const val tag = "dt-post"

        init {
            define(Post())
        }
    }
}

inline fun NodeCursor<*>.post(
    attrs: CommonAttributes<CustomTagWrapper<Post>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<Post>>.() -> Unit) = {}
) {
    baseElement(Post.tag, attrs, children)
}
