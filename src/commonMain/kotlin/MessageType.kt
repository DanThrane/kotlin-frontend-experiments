package dk.thrane.playground

interface OutgoingConverter<Schema : MessageSchema<Schema>> {
    fun toOutgoing(): BoundOutgoingMessage<Schema>
}

interface IngoingConverter<Type : OutgoingConverter<Schema>, Schema : MessageSchema<Schema>> {
    fun fromIngoing(message: BoundMessage<Schema>): Type
}

