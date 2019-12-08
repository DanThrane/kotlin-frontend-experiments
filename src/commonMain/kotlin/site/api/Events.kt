package dk.thrane.playground.site.api

import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

object Events : RPCNamespace("events") {
    val create by call(CreateEventRequest.serializer(), CreateEventResponse.serializer())
    val addOrganizer by call(AddOrganizerRequest.serializer(), EmptyMessage.serializer())
    val removeOranigzer by call(RemoveOrganizerRequest.serializer(), EmptyMessage.serializer())
    val listOrganizedByMe by call(ListOrganizedByMeRequest.serializer(), Page.serializer(Event.serializer()))
}

@Serializable
data class CreateEventRequest(
    @SerialId(1) val title: String,
    @SerialId(2) val startDate: Long,
    @SerialId(3) val endDate: Long = -1
)

@Serializable
data class CreateEventResponse(val eventId: String)

@Serializable
data class AddOrganizerRequest(
    @SerialId(1) val eventId: String,
    @SerialId(2) val organizer: String
)

@Serializable
data class RemoveOrganizerRequest(
    @SerialId(1) val eventId: String,
    @SerialId(2) val organizer: String
)

@Serializable
data class Event(
    @SerialId(1) val eventId: String,
    @SerialId(2) val organizers: List<String>,
    @SerialId(3) val title: String,
    @SerialId(4) val description: String,
    @SerialId(5) val startDate: Long,
    @SerialId(6) val endDate: Long = -1
)

@Serializable
data class ListOrganizedByMeRequest(
    @SerialId(1) override val page: Int,
    @SerialId(2) override val itemsPerPage: Int
) : WithPaginationRequest
