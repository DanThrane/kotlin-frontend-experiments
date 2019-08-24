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

object CoursesBackend : RPCNamespace("courses") {
    val list = rpc<Unit, List<Course>>("list")
}

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
    var webSocketConn: WebSocket? = null
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

            remoteDataComponent.fetchData { CoursesBackend.list.call(Unit) }
        }

        button {
            text("Do websockets!")

            on("click") {
                val conn = webSocketConn
                if (conn != null) {
                    val message = BoundOutgoingMessage(TestMessage)
                    message[TestMessage.text] = "Hello!"
                    message[TestMessage.nested] = { nested ->
                        nested[TestMessage.text] = "Nested"
                        nested[TestMessage.nested] = null
                    }
                    writeMessage(streamOut, message.build())
                    conn.send(streamOut.viewMessage())
                } else {
                    val webSocket = WebSocket("ws://${document.location!!.host}").also { webSocketConn = it }
                    webSocket.binaryType = BinaryType.ARRAYBUFFER
                    webSocket.addEventListener("open", {
                        println("We are open")
                    })

                    webSocket.onmessage = { event ->
                        val data = event.data as ArrayBuffer
                        val stream = ByteStreamJS(Int8Array(data).unsafeCast<ByteArray>())
                        val parseMessage = parseMessage(stream)
                        console.log(parseMessage)
                    }
                }
            }
        }
    }
}