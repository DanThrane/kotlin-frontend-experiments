package dk.thrane.playground.serialization

import dk.thrane.playground.Log
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BorrowedSerializationOfMessage

        if (owner != other.owner) return false
        if (id != other.id) return false
        if (!borrowedBytes.contentEquals(other.borrowedBytes)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = owner.hashCode()
        result = 31 * result + id
        result = 31 * result + borrowedBytes.contentHashCode()
        result = 31 * result + size
        return result
    }
}

@OptIn(ExperimentalSerializationApi::class)
class MessageFormat(
    private val maxMessageSize: Int = 1024 * 64,
    override val serializersModule: SerializersModule = EmptySerializersModule,
) : BinaryFormat {
    private val pool = ByteArrayPool({ ByteArray(maxMessageSize) }, 256)

    fun <T> dumpToField(serializer: SerializationStrategy<T>, value: T): ObjectField {
        val writer = internalDump(serializer.descriptor)
        serializer.serialize(writer, value)
        return ObjectField(writer.fieldBuilder.toTypedArray().toMutableList())
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
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

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val objectField = Field.deserialize(InputBuffer(bytes)) as? ObjectField ?: throw BadMessageException()
        return RootDecoder(objectField).decodeSerializableValue(deserializer)
    }

    fun <T> decodeFromField(deserializer: DeserializationStrategy<T>, obj: ObjectField): T {
        return RootDecoder(obj).decodeSerializableValue(deserializer)
    }

    private inner class RootEncoder(val fieldBuilder: SparseFieldArray) : Encoder {
        override val serializersModule: SerializersModule
            get() = this@MessageFormat.serializersModule

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
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
        //override fun encodeUnit() = unexpectedUsage()
    }

    private inner class ObjectEncoder(val fieldBuilder: SparseFieldArray) : CompositeEncoder {
        override val serializersModule: SerializersModule
            get() = this@MessageFormat.serializersModule

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

        /*
        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            fieldBuilder[index] = ObjectField(ArrayList())
        }
         */

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

                SerialKind.ENUM -> {
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

                SerialKind.ENUM -> {
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
        override val serializersModule: SerializersModule get() = this@MessageFormat.serializersModule
        //override val updateMode: UpdateMode = UpdateMode.BANNED

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
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
        /*
        override fun decodeUnit() {
            require(field is ObjectField)
        }
         */
    }

    private inner class ObjectReader(private val field: ObjectField) : CompositeDecoder {
        override val serializersModule: SerializersModule
            get() = this@MessageFormat.serializersModule
        private var idx = 0

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = field.fields.size

        override fun decodeSequentially(): Boolean = true

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (idx < descriptor.elementsCount) {
                return idx++
            } else {
                return CompositeDecoder.DECODE_DONE
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

        /*
        override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            require(field.fields[index].type == FieldType.OBJ_START)
        }
         */

        override fun <T : Any> decodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            previousValue: T?
        ): T? {
            if (field.fields[index].type == FieldType.NULL) return null
            return deserializer.deserialize(RootDecoder(field.fields[index]))
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            previousValue: T?
        ): T {
            // This doesn't seem correct. For some reason it works.
            if (field.fields[index].type == FieldType.NULL) return null as T
            return deserializer.deserialize(RootDecoder(field.fields[index]))
        }

        override fun endStructure(descriptor: SerialDescriptor) {
        }
    }

    companion object : BinaryFormat {
        private val log = Log("MessageFormat")
        public val default = MessageFormat()
        override val serializersModule: SerializersModule get() = default.serializersModule

        override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray =
            default.encodeToByteArray(serializer, value)

        override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T =
            default.decodeFromByteArray(deserializer, bytes)

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
