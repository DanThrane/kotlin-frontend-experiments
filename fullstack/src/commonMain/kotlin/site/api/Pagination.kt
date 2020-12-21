package dk.thrane.playground.site.api

import kotlinx.serialization.Serializable

@Serializable
data class PaginationRequest(
    override val page: Int,
    override val itemsPerPage: Int
) : WithPaginationRequest

val WithPaginationRequest.offset: Int get() = page * itemsPerPage

interface WithPaginationRequest {
    val page: Int
    val itemsPerPage: Int
}

@Serializable
data class Page<T>(
    val page: Int,
    val itemsPerPage: Int,
    val items: List<T>
)
