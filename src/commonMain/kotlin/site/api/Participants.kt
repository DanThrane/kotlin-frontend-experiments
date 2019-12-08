package dk.thrane.playground.site.api

import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

object Participants : RPCNamespace("participants") {

}

enum class ParticipationStatus {
    NO_ANSWER,
    GOING,
    NOT_GOING
}

data class UpdateParticipationStatus(
    val eventId: String,
    val participationStatus: ParticipationStatus
)

@Serializable
data class EventParticipants(
    @SerialId(1) val numberOfParticipants: Int,
    @SerialId(2) val preview: List<String>
)
