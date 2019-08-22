package edu

import org.w3c.dom.Element
import dk.thrane.playground.*

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
    }
}