package dk.thrane.playground.site

import org.w3c.dom.Element
import dk.thrane.playground.*
import dk.thrane.playground.components.*
import dk.thrane.playground.site.api.*

private val container = css {
    maxWidth = 1200.px
    margin = "0 auto"
}

private val innerContainer = css {
    marginTop = 16.px
    marginLeft = 32.px
    marginRight = 32.px
}

private val followerStats = css {
    display = "flex"
    fontSize = 15.pt

    (matchSelf().directChild(matchAny())) {
        marginRight = 64.px
    }
}

private val headerRow = css {
    display = "flex"
    justifyItems = "center"

    (byClass("tags")) {
        marginLeft = 10.px
        fontSize = 20.pt
    }
}

private val spacer = css {
    flexGrow = "1"
}

fun Element.profile(name: String) {
    div(A(klass = container)) {
        surface(A(klass = innerContainer), elevation = 1) {
            val remoteDataComponent = remoteDataWithLoading<Snacker> { data ->
                div(A(klass = headerRow)) {
                    h1 {
                        text(data.username)
                    }

                    div(A(klass = "tags")) {
                        data.tags.forEach {
                            text(it.emoji)
                        }
                    }

                    div(A(klass = spacer))

                    primaryButton {
                        text("Follow")

                        on(Events.click) {
                            Snackers.toggleFollow
                                .call(FindByString(name).toOutgoing())
                                .then { Router.refresh() }
                        }
                    }
                }

                div(A(klass = followerStats)) {
                    div {
                        text(data.followerCount.toString())
                        text(" ")
                        text("Followers")
                    }
                }
            }

            remoteDataComponent.fetchData {
                Snackers.view.call(FindByString(name), Snacker)
            }
        }
    }
}

