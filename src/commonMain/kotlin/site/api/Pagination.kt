package dk.thrane.playground.site.api

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Serializable
data class PaginationRequest(
    @SerialId(1)
    val page: Int,

    @SerialId(2)
    val itemsPerPage: Int
)

val PaginationRequest.offset: Int get() = page * itemsPerPage
