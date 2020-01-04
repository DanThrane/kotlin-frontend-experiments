package dk.thrane.playground.site.api

import kotlinx.serialization.Serializable

@Serializable
data class Invite(
    val invitedBy: String,
    val eventId: String,
    val createdAt: Long
)

@Serializable
data class InviteListenRequest(
    val maxResults: Int,
    val openSubscription: Boolean = true
)
