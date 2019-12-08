package dk.thrane.playground.site.api

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Serializable
data class PaginationRequest(
    @SerialId(1)
    override val page: Int,

    @SerialId(2)
    override val itemsPerPage: Int
) : WithPaginationRequest

val WithPaginationRequest.offset: Int get() = page * itemsPerPage

interface WithPaginationRequest {
    val page: Int
    val itemsPerPage: Int
}

@Serializable
data class Page<T>(
    @SerialId(1) val page: Int,
    @SerialId(2) val itemsPerPage: Int,
    @SerialId(3) val items: List<T>
)
