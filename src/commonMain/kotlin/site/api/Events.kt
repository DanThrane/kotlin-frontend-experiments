package dk.thrane.playground.site.api

import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.Serializable

object Events : RPCNamespace("events") {
    val create by call(CreateEventRequest.serializer(), CreateEventResponse.serializer())
    val addOrganizer by call(AddOrganizerRequest.serializer(), EmptyMessage.serializer())
    val removeOranigzer by call(RemoveOrganizerRequest.serializer(), EmptyMessage.serializer())
    val listOrganizedByMe by call(ListOrganizedByMeRequest.serializer(), Page.serializer(Event.serializer()))
}

@Serializable
data class CreateEventRequest(
    val title: String,
    val startDate: Long,
    val endDate: Long = -1
)

@Serializable
data class CreateEventResponse(val eventId: String)

@Serializable
data class AddOrganizerRequest(
    val eventId: String,
    val organizer: String
)

@Serializable
data class RemoveOrganizerRequest(
    val eventId: String,
    val organizer: String
)

@Serializable
data class Event(
    val eventId: String,
    val organizers: List<String>,
    val title: String,
    val description: String,
    val startDate: Long,
    val endDate: Long = -1
)

@Serializable
data class ListOrganizedByMeRequest(
    override val page: Int,
    override val itemsPerPage: Int
) : WithPaginationRequest
