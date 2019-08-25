package edu

import org.w3c.dom.Element
import dk.thrane.playground.*
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.ARRAYBUFFER
import org.w3c.dom.BinaryType
import org.w3c.dom.WebSocket
import kotlin.browser.document

data class Course(val name: String)

private val container = css {
    width = 900.px
    margin = "0 auto"
}

private val coursesSurface = css {
    width = 500.px
    height = 300.px
}

fun Element.courses() {
    Header.activePage.currentValue = Page.COURSES
    var webSocketConn: WSConnection? = null
    val streamOut = ByteOutStreamJS(Uint8Array(1024 * 64))

    div(A(klass = container)) {
        text("Courses!")

        surface(A(klass = coursesSurface), elevation = 1) {
            val listComponent = list<Course> { course ->
                div {
                    text("Course")
                    text(course.name)
                }
            }

            val remoteDataComponent = remoteDataWithLoading<List<Course>> { data ->
                listComponent.setList(data)
            }
        }

        button {
            text("Do websockets2!")

            on("click") {
                val conn = webSocketConn
                if (conn != null) {
                    val connectionWithAuth = ConnectionWithAuthorization(conn)
                    val message = BoundOutgoingMessage(TestMessage)
                    message[TestMessage.text] = "Hello!"
                    message[TestMessage.nested] = { nested ->
                        nested[TestMessage.text] = "Nested"
                    }
                    message[TestMessage.messages] = listOf(1, 2, 3, 4, 5, 6)

                    Dummy.test.call(connectionWithAuth, message).then {
                        println("Got a result back!")
                        console.log(it)
                        console.log(it[TestMessage.text])
                    }.catch { fail ->
                        console.log("Fail", fail)
                    }
                } else {
                    webSocketConn = WSConnection("ws://${document.location!!.host}").also { webSocketConn = it }
                }
            }
        }
    }
}