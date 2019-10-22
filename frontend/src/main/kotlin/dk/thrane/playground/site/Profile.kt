package dk.thrane.playground.site

import org.w3c.dom.Element
import dk.thrane.playground.*
import dk.thrane.playground.components.*
import dk.thrane.playground.site.api.*

private val followerStats = css {
    display = "flex"
    fontSize = 15.pt
    justifyContent = "space-evenly"

    (matchSelf().directChild(matchAny())) {
        marginRight = 64.px
    }
}

private val headerRow = css {
    display = "flex"
    alignItems = "center"

    (byClass("tags")) {
        marginLeft = 10.px
        fontSize = 20.pt
    }

    (byTag("h1")) {
        marginLeft = 12.px
    }
}

private val spacer = css {
    flexGrow = "1"
}

fun Element.profile(name: ImmutableBoundData<String>) {
    val data = BoundData<Snacker?>(null)
    fun reload() {
        Snackers.view.call(FindByString(name.currentValue), Snacker).then { data.currentValue = it }
    }

    name.addHandler { reload() }

    content {
        surface(elevation = 1) {
            div(A(klass = headerRow)) {
                avatar(name, { it })

                h1 { boundText(data) { it?.username ?: name.currentValue } }

                div(A(klass = spacer))

                primaryButton {
                    text("Follow")

                    on(Events.click) {
                        Snackers.toggleFollow
                            .call(FindByString(name.currentValue).toOutgoing())
                            .then { reload() }
                    }
                }
            }
        }

        surface(elevation = 1) {
            div(A(klass = followerStats)) {
                div {
                    boundText(data) {
                        if (it == null) {
                            ""
                        } else {
                            if (it.followerCount != 1) {
                                "${it.followerCount} Followers"
                            } else {
                                "${it.followerCount} Follower"
                            }
                        }
                    }
                }
            }
        }

        h1 { text("Feed") }

        repeat(10) {
            feedEntry()
        }
    }
}

