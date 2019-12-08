package dk.thrane.playground.site.api

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Serializable
data class Invite(
    @SerialId(1) val invitedBy: String,
    @SerialId(2) val eventId: String,
    @SerialId(3) val createdAt: Long
)

@Serializable
data class InviteListenRequest(
    @SerialId(1) val maxResults: Int,
    @SerialId(2) val openSubscription: Boolean = true
)
