package dk.thrane.playground.serialization

fun Field.Companion.deserialize(buffer: InputBuffer): Field {
    return when (FieldType.valueOf(buffer.read()) ?: throw BadMessageException()) {
        FieldType.BYTE -> ByteField(buffer.read().toByte())
        FieldType.INT -> IntField(buffer.readInt())
        FieldType.LONG -> LongField(buffer.readLong())
        FieldType.DOUBLE -> DoubleField(buffer.readDouble())
        FieldType.BOOLEAN -> BooleanField(buffer.read() != 0)
        FieldType.OBJ_END -> ObjectEndIndicator
        FieldType.NULL -> NullField

        FieldType.BINARY -> {
            val length = buffer.readInt()
            val destinationBuffer = ByteArray(length)
            buffer.readFully(destinationBuffer)
            BinaryField(destinationBuffer)
        }

        FieldType.OBJ_START -> {
            val fields = ArrayList<Field>()
            while (true) {
                val element = Field.deserialize(buffer)
                if (element == ObjectEndIndicator) break
                fields.add(element)
            }

            ObjectField(fields)
        }
    }
}

fun Field.serialize(out: OutputBuffer) {
    val field = this
    out.writeByte(field.type.ordinal)
    when (field) {
        is ByteField -> out.writeByte(field.value)
        is IntField -> out.writeInt(field.value)
        is LongField -> out.writeLong(field.value)
        is DoubleField -> out.writeDouble(field.value)
        is BooleanField -> out.writeByte(if (field.value) 1 else 0)
        ObjectEndIndicator -> {
        }
        NullField -> {
        }

        is BinaryField -> {
            val payload = field.value
            out.writeInt(payload.size)
            out.writeFully(payload)
        }

        is ObjectField -> {
            field.fields.forEach { child ->
                child.serialize(out)
            }

            ObjectEndIndicator.serialize(out)
        }
    }
}
