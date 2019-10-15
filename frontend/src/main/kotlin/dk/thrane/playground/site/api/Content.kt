package dk.thrane.playground.site.api

import dk.thrane.playground.*

object Contents : RPCNamespace("contents") {
    val viewFeed by call(ViewFeedRequestSchema, ViewFeedResponseSchema)
    val postContent by call(ContentSchema, FindByStringSchema)
}

typealias ViewFeedRequestSchema = PaginationRequestSchema
typealias ViewFeedRequest = PaginationRequest

object ViewFeedResponseSchema : MessageSchema<ViewFeedResponseSchema>() {
    val page = obj(0, PageSchema)
    val items = listObj(1, ContentSchema)
}

data class ViewFeedResponse(val page: Page, val items: List<Content>): OutgoingConverter<ViewFeedResponseSchema> {
    override fun toOutgoing() = buildOutgoing(ViewFeedResponseSchema) { message ->
        message[ViewFeedResponseSchema.page] = page.toOutgoing()
        message[ViewFeedResponseSchema.items] = items.map { it.toOutgoing() }
    }

    companion object : IngoingConverter<ViewFeedResponse, ViewFeedResponseSchema> {
        override fun fromIngoing(message: BoundMessage<ViewFeedResponseSchema>) = ViewFeedResponse(
            Page.fromIngoing(message[ViewFeedResponseSchema.page]),
            message[ViewFeedResponseSchema.items].map { Content.fromIngoing(it) }
        )
    }
}

object ContentSchema : MessageSchema<ContentSchema>() {
    val id = string(0)
    val type = string(1)
    val tags = listString(2)

    // Used for types: "IMG", "VIDEO"
    val src = stringNullable(3)
}

sealed class Content(val type: String) : OutgoingConverter<ContentSchema> {
    abstract val id: String
    abstract val tags: List<SnackTag>

    protected fun writeCommon(message: BoundOutgoingMessage<ContentSchema>) {
        message[ContentSchema.id] = id
        message[ContentSchema.type] = type
        message[ContentSchema.tags] = tags.map { it.name }
    }

    data class Image(
        override val id: String,
        override val tags: List<SnackTag>,
        val src: String
    ) : Content(IMG) {
        override fun toOutgoing(): BoundOutgoingMessage<ContentSchema> = buildOutgoing(ContentSchema) { message ->
            writeCommon(message)
            message[ContentSchema.src] = src
        }
    }

    data class Video(
        override val id: String,
        override val tags: List<SnackTag>,
        val src: String
    ) : Content(VIDEO) {
        override fun toOutgoing(): BoundOutgoingMessage<ContentSchema> = buildOutgoing(ContentSchema) { message ->
            writeCommon(message)
            message[ContentSchema.src] = src
        }
    }

    companion object : IngoingConverter<Content, ContentSchema> {
        private const val IMG = "IMG"
        private const val VIDEO = "VIDEO"

        private fun readTags(message: BoundMessage<ContentSchema>) = message[ContentSchema.tags].map {
            SnackTag.fromString(it) ?: throw RPCException(ResponseCode.BAD_REQUEST, "Unknown tag")
        }

        override fun fromIngoing(message: BoundMessage<ContentSchema>): Content = when (message[ContentSchema.type]) {
            IMG -> Image(
                message[ContentSchema.id],
                readTags(message),
                message[ContentSchema.src]!!
            )

            VIDEO -> Video(
                message[ContentSchema.id],
                readTags(message),
                message[ContentSchema.src]!!
            )

            else -> throw RPCException(ResponseCode.BAD_REQUEST, "Unknown type")
        }
    }
}
