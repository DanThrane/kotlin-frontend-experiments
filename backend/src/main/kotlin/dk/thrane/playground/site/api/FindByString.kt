package dk.thrane.playground.site.api

import dk.thrane.playground.*

object FindByStringSchema : MessageSchema<FindByStringSchema>() {
    val id = string(0)
}

data class FindByString(val id: String):
    OutgoingConverter<FindByStringSchema> {
    override fun toOutgoing() =
        buildOutgoing(FindByStringSchema) { message ->
            message[FindByStringSchema.id] = id
        }

    companion object :
        IngoingConverter<FindByString, FindByStringSchema> {
        override fun fromIngoing(message: BoundMessage<FindByStringSchema>) =
            FindByString(
                message[FindByStringSchema.id]
            )
    }
}
