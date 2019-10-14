package dk.thrane.playground.site.api

import dk.thrane.playground.*

object PageSchema : MessageSchema<PageSchema>() {
    val itemsPerPage = int(0)
    val itemsInTotal = int(1)
}

data class Page(val itemsPerPage: Int, val itemsInTotal: Int) : OutgoingConverter<PageSchema> {
    override fun toOutgoing() = buildOutgoing(PageSchema) { msg ->
        msg[PageSchema.itemsInTotal] = itemsInTotal
        msg[PageSchema.itemsPerPage] = itemsPerPage
    }

    companion object : IngoingConverter<Page, PageSchema> {
        override fun fromIngoing(message: BoundMessage<PageSchema>) = Page(
            message[PageSchema.itemsPerPage],
            message[PageSchema.itemsInTotal]
        )
    }
}

object PaginationRequestSchema : MessageSchema<PaginationRequestSchema>() {
    val page = int(0)
    val itemsPerPage = int(1)
}

data class PaginationRequest(val page: Int, val itemsPerPage: Int) : OutgoingConverter<PaginationRequestSchema> {
    override fun toOutgoing() = buildOutgoing(PaginationRequestSchema) { msg ->
        msg[PaginationRequestSchema.page] = page
        msg[PaginationRequestSchema.itemsPerPage] = itemsPerPage
    }

    companion object : IngoingConverter<PaginationRequest, PaginationRequestSchema> {
        override fun fromIngoing(message: BoundMessage<PaginationRequestSchema>) = PaginationRequest(
            message[PaginationRequestSchema.page],
            message[PaginationRequestSchema.itemsPerPage]
        )
    }
}

val PaginationRequest.offset: Int get() = page * itemsPerPage
