package edu

import div
import reset
import routeLink
import router
import Route
import text
import kotlin.browser.document

fun main() {
    val body = document.body!!
    body.classList.add(reset)

    body.div {
        text("Hello, World!")

        div {
            routeLink(href = "/") {
                text("Root")
            }
        }

        div {
            routeLink(href = "/foo") {
                text("/foo")
            }
        }

        div {
            routeLink(href = "/bar") {
                text("/bar")
            }
        }

        div {
            routeLink(href = "/baz") {
                text("/baz")
            }
        }

        router {
            route(
                route = {},
                children = {
                    text("Root")
                }
            )

            route(
                route = {
                    +"bar"
                },

                children = {
                   text("bar")
                }
            )

            route(
                route = {
                    +"foo"
                },

                children = {
                   text("foo")
                }
            )

            route(
                route = {
                    +"baz"
                },

                children = {
                   text("baz")
                }
            )
        }
    }
}