package edu

import div
import list
import org.w3c.dom.Element
import remoteDataWithLoading
import text
import ListComponent

data class Course(val name: String)

object CoursesBackend : RPCNamespace("courses") {
    val list = rpc<Unit, List<Course>>("list")
}

fun Element.courses() {
    Header.activePage.currentValue = Page.COURSES

    div {
        text("Courses!")

        div {
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