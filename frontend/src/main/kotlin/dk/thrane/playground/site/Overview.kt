package dk.thrane.playground.site

import dk.thrane.playground.*
import org.w3c.dom.Element

private val overviewStyle = css {
    display = "flex"
    flexDirection = "column"
    width = 70.vw
    margin = "0 auto"

    (byTag("h2")) {
        fontSize = 48.px
    }
}

fun Element.overviewPage() {
    div(A(klass = overviewStyle)) {
        invitations()
        upcomingEvents()
    }
}
