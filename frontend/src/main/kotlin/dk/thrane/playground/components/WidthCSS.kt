package dk.thrane.playground.components

import dk.thrane.playground.boxSizing
import dk.thrane.playground.css
import dk.thrane.playground.percent
import dk.thrane.playground.width

val fullWidth = css {
    boxSizing = "border-box"
    width = 100.percent
}
