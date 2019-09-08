package dk.thrane.playground.edu

import org.w3c.dom.Element
import dk.thrane.playground.*
import dk.thrane.playground.edu.api.Courses
import dk.thrane.playground.edu.api.PaginationRequest
import org.khronos.webgl.Uint8Array
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
    val pool = WSConnectionPool("ws://${document.location!!.host}")

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
                pool.useConnection { conn ->
                    Courses.list.call(
                        conn,
                        buildOutgoing(PaginationRequest) { msg ->
                            msg[PaginationRequest.itemsPerPage] = 25
                            msg[PaginationRequest.page] = 0
                        }
                    ).then {
                        println("Got a result back!")
                        console.log(it)
                    }.catch { fail ->
                        console.log("Failure", fail)
                    }
                }
            }
        }
    }
}