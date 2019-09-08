package dk.thrane.playground.edu

import dk.thrane.playground.*
import dk.thrane.playground.edu.api.*

class CourseController : Controller() {
    override fun configureController() {
        implement(Courses.list) {
            respond {
                message[schema.items] = listOf(
                    buildOutgoing(Course) { c ->
                        c[Course.id] = "1"
                        c[Course.name] = "Course 101"
                    },
                    buildOutgoing(Course) { c ->
                        c[Course.id] = "2"
                        c[Course.name] = "Course 201"
                    }
                )

                message[schema.itemsPerPage] = request[PaginationRequest.itemsPerPage]
                message[schema.itemsInTotal] = 2
            }
        }

        implement(Courses.create) {
            respond {
                message[schema.id] = "TODO!"
                message[schema.name] = request[CreateCourse.name]
            }
        }

        implement(Courses.view) {
            val id = request[ViewCourse.id]

            respond {
                message[schema.id] = id
                message[schema.name] = "Course Test"
            }
        }
    }
}