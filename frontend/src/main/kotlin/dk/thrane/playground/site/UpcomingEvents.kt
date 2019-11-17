package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.*
import org.w3c.dom.Element

data class EventSummary(
    val id: String,
    val date: String,
    val title: String,
    val invitedBy: String,
    val createdBy: String
)

private val upcomingEventStyle = css {
    textDecoration = "none"

    (byTag("div")) {
        backgroundColor = Theme.backgroundColor.darken(25).toString()
        borderRadius = 8.px
        padding = 16.px
        width = 90.px
    }
}

private fun Element.upcomingEventBox(eventData: ImmutableBoundData<EventSummary>) {
    boundElement(eventData) { event ->
        routeLink(A(klass = upcomingEventStyle), href = "/events/${event.id}") {
            div {
                h3 { text(event.date) }
                span { text(event.title) }
            }
        }
    }
}

private val events = css {
    marginTop = 16.px
    marginBottom = 16.px
    display = "flex"
    flexDirection = "row"
    flexWrap = "wrap"

    (matchSelf().directChild(matchAny())) {
        marginRight = 8.px
        marginBottom = 8.px
    }
}

fun Element.upcomingEvents() {
    div {
        h2 { text("Upcoming") }

        div(A(klass = events)) {
            repeat(20) {
                upcomingEventBox(
                    StaticData(
                        EventSummary(
                            "$it",
                            "${it + 1} Jan 20",
                            "Event $it",
                            "Dan Sebastian Thrane",
                            "Dan Sebastian Thrane"
                        )
                    )
                )
            }
        }
    }
}
