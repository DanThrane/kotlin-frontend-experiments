package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.StaticData
import org.w3c.dom.Element

private val commentFormStyle = css {
    display = "flex"
    justifyContent = "center"
    width = 100.percent
    marginTop = 16.px

    (byTag(("input"))) {
        marginLeft = 16.px
        flexGrow = "1"
    }
}

fun Element.comments() {
    surface(elevation = 1) {
        div {
            // Comment section
            text("No comments posted")
        }

        div(A(klass = commentFormStyle)) {
            avatar(AuthenticationStore.principal, { it?.username ?: "Guest" })

            styledInput(type = "text") {
                setAttribute("placeholder", "Comment goes here...")
            }
        }
    }
}
