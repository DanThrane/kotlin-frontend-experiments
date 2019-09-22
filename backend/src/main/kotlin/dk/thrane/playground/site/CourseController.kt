package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.api.*
import dk.thrane.playground.site.service.AuthenticationService
import kotlin.system.measureNanoTime

class CourseController(
    private val auth: AuthenticationService
) : Controller() {
    override fun configureController() {
        implement(Courses.list) {
            log.info("auth took: " + measureNanoTime {
                val user = auth.validateToken(authorization)
            })

            respond {
                message[schema.items] = listOf(
                    Course("1", "Course 101"),
                    Course("2", "Course 201")
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
            val user = auth.validateToken(authorization)
            log.info("User is $user")

            val id = request[ViewCourse.id]

            respond {
                message[schema.id] = id
                message[schema.name] = "Course Test"
            }
        }
    }

    companion object {
        private val log = Log("CourseController")
    }
}
