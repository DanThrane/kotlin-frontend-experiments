package dk.thrane.playground.site.api

import dk.thrane.playground.*

object Snackers : RPCNamespace("snackers") {
    val whoami by call(EmptySchema, SnackerSchema)
    val view by call(FindByStringSchema, SnackerSchema)
    val viewFollowers by call(ViewFollowersRequestSchema, FollowersSchema)
    val toggleFollow by call(FollowRequestSchema, EmptySchema)
}

object SnackerSchema : MessageSchema<SnackerSchema>() {
    val username = string(0)
    val followerCount = int(1)
    val tags = listString(2)
}

object FollowersSchema : MessageSchema<FollowersSchema>() {
    val username = string(0)
    val followers = listString(1)
}

typealias FollowRequestSchema = FindByStringSchema
typealias FollowRequest = FindByStringSchema

object ViewFollowersRequestSchema : MessageSchema<ViewFollowersRequestSchema>() {
    val username = string(0)
    val pagination = obj(1, PaginationRequestSchema)
}

data class ViewFollowersRequest(
    val username: String,
    val pagination: PaginationRequest
): OutgoingConverter<ViewFollowersRequestSchema> {
    override fun toOutgoing() = buildOutgoing(ViewFollowersRequestSchema) { message ->
        message[ViewFollowersRequestSchema.username] = username
        message[ViewFollowersRequestSchema.pagination] = pagination.toOutgoing()
    }

    companion object : IngoingConverter<ViewFollowersRequest, ViewFollowersRequestSchema> {
        override fun fromIngoing(message: BoundMessage<ViewFollowersRequestSchema>) = ViewFollowersRequest(
            message[ViewFollowersRequestSchema.username],
            PaginationRequest.fromIngoing(message[ViewFollowersRequestSchema.pagination])
        )
    }
}

data class Snacker(
    val username: String,
    val followerCount: Int,
    val tags: List<String>
) : OutgoingConverter<SnackerSchema> {
    override fun toOutgoing() = buildOutgoing(SnackerSchema) { msg ->
        msg[SnackerSchema.username] = username
        msg[SnackerSchema.followerCount] = followerCount
        msg[SnackerSchema.tags] = tags
    }

    companion object : IngoingConverter<Snacker, SnackerSchema> {
        override fun fromIngoing(message: BoundMessage<SnackerSchema>) = Snacker(
            message[SnackerSchema.username],
            message[SnackerSchema.followerCount],
            message[SnackerSchema.tags]
        )
    }
}

data class Followers(val username: String, val followers: List<String>): OutgoingConverter<FollowersSchema> {
    override fun toOutgoing() = buildOutgoing(FollowersSchema) { message ->
        message[FollowersSchema.username] = username
        message[FollowersSchema.followers] = followers
    }

    companion object : IngoingConverter<Followers, FollowersSchema> {
        override fun fromIngoing(message: BoundMessage<FollowersSchema>) = Followers(
            message[FollowersSchema.username],
            message[FollowersSchema.followers]
        )
    }
}
