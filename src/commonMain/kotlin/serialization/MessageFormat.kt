package dk.thrane.playground.serialization

import dk.thrane.playground.Log
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import kotlin.math.max

class MessageFormat(
    private val maxMessageSize: Int = 1024 * 64,
    override val context: SerialModule = EmptyModule
) : BinaryFormat {
    override fun <T> dump(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val out = OutputBuffer(ByteArray(maxMessageSize))

        val writer = internalDump(serializer.descriptor)
        serializer.serialize(writer, value)

        val field = ObjectField(writer.fieldBuilder)
        field.serialize(out)
        return out.array.sliceArray(0 until out.ptr)
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
                log.debug("new writer has ${maxId + 1} fields")
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
            fieldBuilder[index] = BinaryField(byteArrayOf(value))
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

        override fun <T : Any> encodeNullableSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
        ) {
            if (value == null) {
                fieldBuilder[index] = NullField
            } else {
                val writer = internalDump(serializer.descriptor)
                serializer.serialize(writer, value)
                fieldBuilder[index] = ObjectField(writer.fieldBuilder)
            }
        }

        override fun <T> encodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
        ) {
            if (value == null) {
                fieldBuilder[index] = NullField
            } else {
                val writer = internalDump(serializer.descriptor)
                serializer.serialize(writer, value)
                fieldBuilder[index] = ObjectField(writer.fieldBuilder)
            }
        }

        override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
            fieldBuilder[index] = IntField(value.toInt())
        }

        override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
            fieldBuilder[index] = BinaryField(value.encodeToByteArray())
        }

        override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
            fieldBuilder[index] = ObjectField(emptyList())
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

        private fun unexpectedUsage(): Nothing = throw IllegalStateException("Did not expect to be called")

        override fun decodeBoolean(): Boolean = unexpectedUsage()
        override fun decodeByte(): Byte = unexpectedUsage()
        override fun decodeChar(): Char = unexpectedUsage()
        override fun decodeDouble(): Double = unexpectedUsage()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = unexpectedUsage()
        override fun decodeFloat(): Float = unexpectedUsage()
        override fun decodeInt(): Int = unexpectedUsage()
        override fun decodeLong(): Long = unexpectedUsage()
        override fun decodeNull(): Nothing? = unexpectedUsage()
        override fun decodeShort(): Short = unexpectedUsage()
        override fun decodeString(): String = unexpectedUsage()
        override fun decodeUnit() = unexpectedUsage()
    }

    private inner class ObjectReader(private val field: ObjectField) : CompositeDecoder {
        override val updateMode: UpdateMode = UpdateMode.BANNED
        override val context: SerialModule
            get() = this@MessageFormat.context
        private var idx = 0

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = field.fields.size

        override fun decodeSequentially(): Boolean = true

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            log.warn("Why am I being called?")
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
            return deserializer.deserialize(RootDecoder(field.getObject(index)))
        }

        override fun <T> decodeSerializableElement(
            descriptor: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
        ): T {
            // This doesn't seem correct. For some reason it works.
            if (field.fields[index].type == FieldType.NULL) return null as T
            return deserializer.deserialize(RootDecoder(field.getObject(index)))
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
