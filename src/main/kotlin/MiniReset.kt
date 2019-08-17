/*! minireset.css v0.0.5 | MIT License | github.com/jgthms/minireset.css */

val reset = css {
    listOf(
        "html",
        "body",
        "p",
        "ol",
        "ul",
        "li",
        "dl",
        "dt",
        "dd",
        "blockquote",
        "figure",
        "fieldset",
        "legend",
        "textarea",
        "pre",
        "iframe",
        "hr",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6"
    ).forEach { tag ->
        (byTag(tag)) {
            margin = 0.px
            padding = 0.px
        }
    }

    (1..6).forEach { headingLevel ->
        (byTag("h$headingLevel")) {
            fontSize = 100.percent
            fontWeight = "normal"
        }
    }

    (byTag("ul")) {
        listStyle = "none"
    }

    listOf(
        "button",
        "input",
        "select",
        "textarea"
    ).forEach { tag ->
        (byTag(tag)) {
            margin = 0.px
        }
    }

    (byTag("html")) {
        boxSizing = "border-box"
    }

    (matchAny() or (matchAny().withPseudoClass("before")) or (matchAny().withPseudoClass("after"))) {
        boxSizing = "inherit"
    }

    listOf(
        "img",
        "video"
    ).forEach { tag ->
        height = "auto"
        maxWidth = 100.percent
    }

    (byTag("iframe")) {
        border = "0"
    }

    (byTag("table")) {
        borderCollapse = "collapse"
        borderSpacing = "0"
    }

    (byTag("td") or byTag("th")) {
        padding = "0"
        textAlign = "left"
    }
}
