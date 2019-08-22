package edu

import kotlin.browser.document
import dk.thrane.playground.*
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

private val globalTheme = css {
    margin = 0.px
    padding = 0.px

    (matchAny()) {
        fontFamily = "'Roboto', sans-serif"
    }
}

private val rootContainer = css {
    display = "flex"
    flexDirection = "column"
    height = 100.vh
}

private val contentContainer = css {
    backgroundColor = Theme.background.toString()
    color = Theme.onBackground.toString()
    flexGrow = "10"
    flexShrink = "1"
    flexBasis = "auto"
    height = 100.percent
}

fun main() {
    rawCSS("@import url('https://fonts.googleapis.com/css?family=Roboto:400,500&display=swap');")

    val body = document.body!!
    body.classList.add(reset)
    body.classList.add(globalTheme)

    console.log(byteArrayOf(1, 2, 3, 4))
    console.log("Hello, World".encodeToUTF8())
    console.log(stringFromUtf8("Hello, World".encodeToUTF8()))

    val out = BoundOutgoingMessage(TestMessage)

    out[TestMessage.text] = "Testing!"
    out[TestMessage.nested] = {
        it[TestMessage.text] = "qweasdqwe"
        it[TestMessage.nested] = null
    }

    val buffer = Uint8Array(1024 * 1024)
    write(ByteOutStreamJS(buffer), out.build())

    val message = parse(ByteStreamJS(Int8Array(buffer.buffer).unsafeCast<ByteArray>()))
    console.log(message)
    val boundMessage = BoundMessage<TestMessage>(message as ObjectField)
    console.log(boundMessage[TestMessage.text])
    console.log(boundMessage[TestMessage.nested]?.get(TestMessage.text))

    body.div(A(klass = rootContainer)) {
        toasts()
        header()

        div(A(klass = contentContainer)) {
            router {
                route(
                    route = {},
                    children = {
                        Header.activePage.currentValue = Page.HOME
                        text("Root")
                    }
                )

                route(
                    route = {
                        +"courses"
                    },

                    children = {
                        courses()
                    }
                )

                route(
                    route = {
                        +"calendar"
                    },

                    children = {
                        Header.activePage.currentValue = Page.CALENDAR
                        text("Calendar")
                        repeat(10) {
                            Toasts.push(Toast(ToastType.INFO, "This is a test $it", 1000L))
                        }
                    }
                )
            }
        }
    }
}