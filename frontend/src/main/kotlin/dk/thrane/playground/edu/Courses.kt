package dk.thrane.playground.edu

import org.w3c.dom.Element
import dk.thrane.playground.*
import dk.thrane.playground.edu.api.Course
import dk.thrane.playground.edu.api.Courses
import dk.thrane.playground.edu.api.PaginationRequest
import kotlin.browser.document
import kotlin.browser.window

private val container = css {
    width = 900.px
    margin = "0 auto"
}

private val coursesSurface = css {
    width = 500.px
    height = 300.px
}

fun Element.courses() {
    Header.activePage.currentValue = SitePage.COURSES
    val pool = WSConnectionPool("ws://${document.location!!.host}")

    div(A(klass = container)) {
        text("Courses!")

        surface(A(klass = coursesSurface), elevation = 1) {
            val listComponent = list<BoundMessage<Course>> { course ->
                div {
                    text("Name: ")
                    text(course[Course.name])
                }
            }

            val remoteDataComponent = remoteDataWithLoading<List<BoundMessage<Course>>> { data ->
                listComponent.setList(data)
            }

            remoteDataComponent.fetchData {
                Courses.list.call(
                    pool,
                    buildOutgoing(PaginationRequest) { msg ->
                        msg[PaginationRequest.itemsPerPage] = 25
                        msg[PaginationRequest.page] = 0
                    }
                ).then { page ->
                    val schema = Courses.list.responsePayload
                    page[schema.items]
                }
            }
        }

        flex {
            primaryButton {
                on(Events.click) {
                    window.alert("You clicked!")
                }

                text("This is a button")
            }

            outlineButton {
                on(Events.click) {
                    window.alert("outline!")
                }

                text("Outline!")
            }
        }
    }
}