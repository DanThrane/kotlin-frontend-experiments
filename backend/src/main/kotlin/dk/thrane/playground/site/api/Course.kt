package dk.thrane.playground.site.api

import dk.thrane.playground.*

object Courses : RPCNamespace("courses") {
    val list by call(PaginationRequest, Page(Course))
    val view by call(ViewCourse, Course)
    val create by call(CreateCourse, Course)
}

object Course : MessageSchema<Course>() {
    val id = string(0)
    val name = string(1)
}

fun Course(
    id: String,
    name: String
): BoundOutgoingMessage<Course> = buildOutgoing(Course) { msg ->
    msg[Course.id] = id
    msg[Course.name] = name
}

object ViewCourse : MessageSchema<ViewCourse>() {
    val id = string(0)
}

object CreateCourse : MessageSchema<CreateCourse>() {
    val name = string(1)
}

class Page<T : MessageSchema<T>>(val schema: T) : MessageSchema<Page<T>>() {
    val itemsPerPage = int(0)
    val itemsInTotal = int(1)
    val items = listObj(2, schema)
}

object PaginationRequest : MessageSchema<PaginationRequest>() {
    val page = int(0)
    val itemsPerPage = int(1)
}

data class NormalizedPaginationRequest(val page: Int, val itemsPerPage: Int) {
    constructor(message: BoundMessage<PaginationRequest>) : this(
        message[PaginationRequest.page],
        message[PaginationRequest.itemsPerPage]
    )

    init {
        if (itemsPerPage !in setOf(10, 25, 50, 100, 250)) {
            throw RPCException(ResponseCode.BAD_REQUEST, "Bad itemsPerPage")
        }
    }
}
