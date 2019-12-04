package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.components.ImmutableBoundData
import dk.thrane.playground.components.StaticData
import dk.thrane.playground.components.boundElement
import org.w3c.dom.Element

private val highlightStyle = css {
    fontWeight = "700"
}

private fun Element.highlight(text: String) {
    span(A(klass = highlightStyle)) { text(text) }
}

private val inviteStyle = css {
    paddingTop = 8.px
    paddingBottom = 8.px
}

private fun Element.invite(eventData: ImmutableBoundData<EventSummary>) {
    boundElement(eventData) { ev ->
        styled(inviteStyle) {
            text("${ev.date}: ")
            highlight(ev.invitedBy)
            text(" has invited you to ")
            highlight(ev.title)

            confirmButton {
                on("click") { ev ->
                    console.log("Confirm")
                }
            }

            denyButton {
                on("click") { ev ->
                    console.log("Deny")
                }
            }
        }
    }
}

private val rootStyle = css {
    marginBottom = 32.px
}

fun Element.invitations() {
    styled(rootStyle) {
        h2 { text("Invitations") }
        repeat(10) {
            invite(
                StaticData(
                    EventSummary(
                        "$it",
                        "${(it + 1).toString().padStart(2, '0')} Jan 20",
                        "Event $it",
                        "Dan Sebastian Thrane",
                        "Dan Sebastian Thrane"
                    )
                )
            )
        }
    }
}
