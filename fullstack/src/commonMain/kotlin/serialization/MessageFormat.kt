package dk.thrane.playground.serialization

import dk.thrane.playground.Log
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import kotlin.math.max

expect class ByteArrayPool(generator: () -> ByteArray, numberOfElements: Int) {
    fun borrowInstance(): Pair<Int, ByteArray>
    fun returnInstance(id: Int)
}

data class BorrowedSerializationOfMessage(
    private val owner: MessageFormat,
    val id: Int,
    val borrowedBytes: ByteArray,
    val size: Int
) {
    fun release() {
        owner.returnSerialized(this)
    }
}

class MessageFormat(
    private val maxMessageSize: Int = 1024 * 64,
    override val context: SerialModule = EmptyModule
) : BinaryFormat {
    private val pool = ByteArrayPool({ ByteArray(maxMessageSize) }, 256)

    fun <T> dumpToField(serializer: SerializationStrategy<T>, value: T): ObjectField {
        val writer = internalDump(serializer.descriptor)
        serializer.serialize(writer, value)
        return ObjectField(writer.fieldBuilder.toTypedArray().toMutableList())
    }

    override fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val field = dumpToField(serializer, value)

        val (id, buffer) = pool.borrowInstance()
        try {
            val out = OutputBuffer(buffer)
            field.serialize(out)

            // This is, without a doubt, causing a lot of garbage to be generated
            return out.array.sliceArray(0 until out.ptr)
        } finally {
            pool.returnInstance(id)
        }
    }

    fun <T> borrowSerialized(serializer: SerializationStrategy<T>, value: T): BorrowedSerializationOfMessage {
        val (id, buffer) = pool.borrowInstance()
        try {
            val out = OutputBuffer(buffer)

            val writer = internalDump(serializer.descriptor)
            serializer.serialize(writer, value)

            // TODO This crashes when going directly to toMutableList()
            val field = ObjectField(writer.fieldBuilder.toTypedArray().toMutableList())
            field.serialize(out)
            return BorrowedSerializationOfMessage(this, id, buffer, out.ptr)
        } catch (ex: Throwable) {
            pool.returnInstance(id)
            throw ex
        }
    }

    fun returnSerialized(message: BorrowedSerializationOfMessage) {
        pool.returnInstance(message.id)
    }

    private fun internalDump(desc: SerialDescriptor): RootEncoder {
        when (desc.kind) {
            StructureKind.CLASS, StructureKind.OBJECT, is PolymorphicKind -> {
                val elementCount = desc.elementsCount

                if (elementCount == 0) {
                    return RootEncoder(SparseFieldArray(0, allowResize = false))
                }

                // First we figure out how many fields this object will contain:
                val ids = IntArray(elementCount)
                var maxId = -1

                for (i in 0 until elementCount) {
                    ids[i] = i
                    maxId = max(ids[i], maxId)
                }

                require(maxId >= 0)
                return RootEncoder(SparseFieldArray(maxId + 1, allowResize = false))
            }

            StructureKind.LIST -> return RootEncoder(SparseFieldArray(32, allowResize = true))

            else -> throw NotImplementedError("Unsupported kind: ${desc.kind}")
        }
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val objectField = Field.deserialize(InputBuffer(bytes)) as? ObjectField ?: throw BadMessageException()
        return RootDecoder(objectField).decode(deserializer)
    }

    fun <T> load(deserializer: DeserializationStrategy<T>, obj: ObjectField): T {
        return RootDecoder(obj).decode(deserializer)
    }

    private inner class RootEncoder(val fieldBuilder: SparseFieldArray) : Encoder {
        override val context = this@MessageFormat.context

        override fun beginStructure(
            descriptor: SerialDescriptor,
            vararg typeSerializers: KSerializer<*>
        ): CompositeEncoder {
            return ObjectEncoder(fieldBuilder)
        }

        private fun unexpectedUsage(): Nothing =
            throw IllegalStateException("All serialized objects must be wrapped in an object")

        override fun encodeBoolean(value: Boolean) = unexpectedUsage()
        override fun encodeByte(value: Byte) = unexpectedUsage()
        override fun encodeChar(value: Char) = unexpectedUsage()
        override fun encodeDouble(value: Double) = unexpectedUsage()
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = unexpectedUsage()
        override fun encodeFloat(value: Float) = unexpectedUsage()
        override fun encodeInt(value: Int) = unexpectedUsage()
        override fun encodeLong(value: Long) = unexpectedUsage()
        override fun encodeNull() {

        }

        override fun encodeShort(value: Short) = unexpectedUsage()
        override fun encodeString(value: String) = unexpectedUsage()
        override fun encodeUnit() = unexpectedUsage()
    }

    private inner class ObjectEncoder(val fieldBuilder: SparseFieldArray) : CompositeEncoder {
        override val context: SerialModule
            get() = this@MessageFormat.context

        override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
            fieldBuilder[index] = BooleanField(value)
        }

        override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
            fieldBuilder[index] = ByteField(value)
        }

        override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
            fieldBuilder[index] = BinaryField("$value".encodeToByteArray())
        }

        override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
            fieldBuilder[index] = DoubleField(value)
        }

        override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
            fieldBuilder[index] = DoubleField(value.toDouble())
        }

        override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
            fieldBuilder[index] = IntField(value)
        }

        override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
            fieldBuilder[index] = LongField(value)
        }

        override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            fieldBuilder[index] = IntField(value.toInt())
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            fieldBuilder[index] = BinaryField(value.encodeToByteArray())
        }

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            fieldBuilder[index] = ObjectField(ArrayList())
        }

        private fun encodePrimitiveValue(index: Int, value: Any?) {
            val field = when (value) {
                null -> NullField
                is Boolean -> BooleanField(value)
                is Byte -> ByteField(value)
                is Char -> BinaryField("$value".encodeToByteArray())
                is Double -> DoubleField(value)
                is Float -> DoubleField(value.toDouble())
                is Int -> IntField(value)
                is Long -> LongField(value)
                is Short -> IntField(value.toInt())
                is String -> BinaryField(value.encodeToByteArray())
                Unit -> ObjectField(ArrayList())
                else -> throw IllegalStateException("non primitive value passed to encodePrimitiveValue($value)")
            }

            fieldBuilder[index] = field
        }

        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            when (serializer.descriptor.kind) {
                is PrimitiveKind -> encodePrimitiveValue(index, value)

                UnionKind.ENUM_KIND -> {
                    encodePrimitiveValue(index, (value as Enum<*>).ordinal)
                }

                else -> {
                    if (value == null) {
                        fieldBuilder[index] = NullField
                    } else {
                        val writer = internalDump(serializer.descriptor)
                        serializer.serialize(writer, value)
                        fieldBuilder[index] = ObjectField(writer.fieldBuilder.toTypedArray().toMutableList())
                    }
                }
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            when (serializer.descriptor.kind) {
                is PrimitiveKind -> encodePrimitiveValue(index, value)

                UnionKind.ENUM_KIND -> {
                    encodePrimitiveValue(index, (value as Enum<*>).ordinal)
                }

                else -> {
                    if (value == null) {
                        fieldBuilder[index] = NullField
                    } else {
                        val writer = internalDump(serializer.descriptor)
                        serializer.serialize(writer, value)
                        fieldBuilder[index] = ObjectField(writer.fieldBuilder.toTypedArray().toMutableList())
                    }
                }
            }
        }

        override fun endStructure(descriptor: SerialDescriptor) {
        }
    }

    private inner class RootDecoder(private val field: Field) : Decoder {
        override val context: SerialModule
            get() = this@MessageFormat.context
        override val updateMode: UpdateMode = UpdateMode.BANNED

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            return ObjectReader(field as? ObjectField ?: throw IllegalStateException("Expected an object"))
        }

        override fun decodeNotNullMark(): Boolean {
            return field.type != FieldType.NULL
        }

        override fun decodeBoolean(): Boolean = (field as BooleanField).value
        override fun decodeByte(): Byte = (field as BinaryField).value.single()
        override fun decodeChar(): Char = (field as BinaryField).value.decodeToString().single()
        override fun decodeDouble(): Double = (field as DoubleField).value
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = (field as IntField).value
        override fun decodeFloat(): Float = (field as DoubleField).value.toFloat()
        override fun decodeInt(): Int = (field as IntField).value
        override fun decodeLong(): Long = (field as LongField).value
        override fun decodeNull(): Nothing? {
            require(field is NullField)
            return null
        }

        override fun decodeShort(): Short = (field as IntField).value.toShort()
        override fun decodeString(): String = (field as BinaryField).value.decodeToString()
        override fun decodeUnit() {
            require(field is ObjectField)
        }
    }

    private inner class ObjectReader(private val field: ObjectField) : CompositeDecoder {
        override val updateMode: UpdateMode = UpdateMode.BANNED
        override val context: SerialModule
            get() = this@MessageFormat.context
        private var idx = 0

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = field.fields.size

        override fun decodeSequentially(): Boolean = true

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (idx < descriptor.elementsCount) {
                return idx++
            } else {
                return CompositeDecoder.READ_DONE
            }
        }

        override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = field.getBoolean(index)
        override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = field.getByte(index)
        override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
            field.getBinary(index).decodeToString().single()

        override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = field.getDouble(index)
        override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
            field.getDouble(index).toFloat()

        override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = field.getInt(index)
        override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = field.getLong(index)
        override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = field.getInt(index).toShort()
        override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
            field.getBinary(index).decodeToString()

        override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            require(field.fields[index].type == FieldType.OBJ_START)
        }

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>
        ): T? {
            if (field.fields[index].type == FieldType.NULL) return null
            return deserializer.deserialize(RootDecoder(field.fields[index]))
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
        ): T {
            // This doesn't seem correct. For some reason it works.
            if (field.fields[index].type == FieldType.NULL) return null as T
            return deserializer.deserialize(RootDecoder(field.fields[index]))
        }

        override fun endStructure(descriptor: SerialDescriptor) {
        }

        override fun <T : Any> updateNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            old: T?
        ): T? = throw IllegalStateException("Not supported")

        override fun <T> updateSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
        ): T = throw IllegalStateException("Not supported")
    }

    companion object : BinaryFormat {
        private val log = Log("MessageFormat")
        public val default = MessageFormat()

        override val context: SerialModule get() = default.context
        override fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray =
            default.dump(serializer, value)

        override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T =
            default.load(deserializer, bytes)

        private class SparseFieldArray(initialCapacity: Int, val allowResize: Boolean) : List<Field> {
            private var realArray = Array<Field>(initialCapacity) { NullField }
            private var maxIndex = 0

            override val size: Int
                get() = if (!allowResize) realArray.size else maxIndex + 1

            fun toArray(): Array<Field> {
                if (!allowResize || maxIndex == realArray.lastIndex) {
                    return realArray
                }

                return realArray.sliceArray(0..maxIndex)
            }

            override operator fun get(index: Int): Field {
                return realArray[index]
            }

            operator fun set(index: Int, value: Field) {
                if (index > realArray.lastIndex) {
                    if (!allowResize) throw IndexOutOfBoundsException("$index > ${realArray.lastIndex}")
                    val newArray = Array<Field>(realArray.size * 2) { NullField }
                    realArray.copyInto(newArray)
                    realArray = newArray
                }

                realArray[index] = value
                maxIndex = max(maxIndex, index)
            }

            override fun contains(element: Field): Boolean = toArray().contains(element)

            override fun containsAll(elements: Collection<Field>): Boolean {
                val arr = toArray()
                return elements.all { arr.contains(it) }
            }

            override fun indexOf(element: Field): Int = toArray().indexOf(element)

            override fun isEmpty(): Boolean = toArray().isEmpty()

            override fun iterator(): Iterator<Field> = toArray().iterator()

            override fun lastIndexOf(element: Field): Int = toArray().lastIndexOf(element)

            override fun listIterator(): ListIterator<Field> = throw NotImplementedError()

            override fun listIterator(index: Int): ListIterator<Field> = throw NotImplementedError()

            override fun subList(fromIndex: Int, toIndex: Int): List<Field> = throw NotImplementedError()
        }
    }
}

inline fun <T> MessageFormat.useSerialized(
    serializationStrategy: SerializationStrategy<T>,
    value: T,
    block: (ByteArray) -> Unit
) {
    val serialized = borrowSerialized(serializationStrategy, value)
    try {
        block(serialized.borrowedBytes)
    } finally {
        returnSerialized(serialized)
    }
}
