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
    override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray {
        val out = OutputBuffer(ByteArray(maxMessageSize))

        val writer = internalDump(serializer.descriptor)
        serializer.serialize(writer, obj)

        val field = ObjectField(writer.fieldBuilder)
        field.serialize(out)
        return out.array.sliceArray(0 until out.ptr)
    }

    private fun internalDump(desc: SerialDescriptor): Writer {
        when (desc.kind) {
            StructureKind.CLASS, UnionKind.OBJECT, is PolymorphicKind -> {
                val elementCount = desc.elementsCount

                if (elementCount == 0) {
                    return Writer(SparseFieldArray(0, allowResize = false))
                }

                // First we figure out how many fields this object will contain:
                val ids = IntArray(elementCount)
                var maxId = -1

                for (i in 0 until elementCount) {
                    val serialId = desc.getElementAnnotations(i).find { it is SerialId } as? SerialId
                    ids[i] = serialId?.id ?: i
                    maxId = max(ids[i], maxId)
                }

                require(maxId >= 0)
                log.debug("new writer has ${maxId + 1} fields")
                return Writer(SparseFieldArray(maxId + 1, allowResize = false))
            }

            StructureKind.LIST -> return Writer(SparseFieldArray(32, allowResize = true))

            else -> throw NotImplementedError("Unsupported kind: ${desc.kind}")
        }
    }

    override fun <T> load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val objectField = Field.deserialize(InputBuffer(bytes)) as? ObjectField ?: throw BadMessageException()
        return Reader(objectField).decode(deserializer)
    }

    private inner class Writer(val fieldBuilder: SparseFieldArray) : TaggedEncoder<Int>() {
        override val context: SerialModule
            get() = this@MessageFormat.context

        override fun SerialDescriptor.getTag(index: Int): Int {
            return (getElementAnnotations(index).find { it is SerialId } as? SerialId)?.id ?: index
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
            log.debug("beginStructure($desc, $currentTagOrNull)")
            val tag = currentTagOrNull ?: return this
            val writer = internalDump(desc)
            fieldBuilder[tag] = ObjectField(writer.fieldBuilder)
            return writer
        }

        override fun encodeTaggedBoolean(tag: Int, value: Boolean) {
            fieldBuilder[tag] = BooleanField(value)
        }

        override fun encodeTaggedByte(tag: Int, value: Byte) {
            fieldBuilder[tag] = ByteField(value)
        }

        @UseExperimental(ExperimentalStdlibApi::class)
        override fun encodeTaggedChar(tag: Int, value: Char) {
            fieldBuilder[tag] = BinaryField("$value".encodeToByteArray())
        }

        override fun encodeTaggedDouble(tag: Int, value: Double) {
            fieldBuilder[tag] = DoubleField(value)
        }

        override fun encodeTaggedEnum(tag: Int, enumDescription: SerialDescriptor, ordinal: Int) {
            fieldBuilder[tag] = IntField(ordinal)
        }

        override fun encodeTaggedFloat(tag: Int, value: Float) {
            fieldBuilder[tag] = DoubleField(value.toDouble())
        }

        override fun encodeTaggedInt(tag: Int, value: Int) {
            fieldBuilder[tag] = IntField(value)
        }

        override fun encodeTaggedLong(tag: Int, value: Long) {
            fieldBuilder[tag] = LongField(value)
        }

        override fun encodeTaggedNotNullMark(tag: Int) {
            // Do nothing
        }

        override fun encodeTaggedNull(tag: Int) {
            fieldBuilder[tag] = NullField
        }

        override fun encodeTaggedShort(tag: Int, value: Short) {
            fieldBuilder[tag] = IntField(value.toInt())
        }

        @UseExperimental(ExperimentalStdlibApi::class)
        override fun encodeTaggedString(tag: Int, value: String) {
            fieldBuilder[tag] = BinaryField(value.encodeToByteArray())
        }

        override fun encodeTaggedUnit(tag: Int) {
            fieldBuilder[tag] = ObjectField(emptyList())
        }
    }

    private inner class Reader(private val field: ObjectField) : TaggedDecoder<Int>() {
        override val context: SerialModule
            get() = this@MessageFormat.context

        override fun SerialDescriptor.getTag(index: Int): Int {
            return (getElementAnnotations(index).find { it is SerialId } as? SerialId)?.id ?: index
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val tag = currentTagOrNull ?: return this
            return Reader(field.getObject(tag))
        }

        override fun decodeCollectionSize(desc: SerialDescriptor): Int = field.fields.size

        override fun decodeTaggedBoolean(tag: Int): Boolean = field.getBoolean(tag)

        override fun decodeTaggedByte(tag: Int): Byte = field.getByte(tag)

        @UseExperimental(ExperimentalStdlibApi::class)
        override fun decodeTaggedChar(tag: Int): Char = field.getBinary(tag).decodeToString().single()

        override fun decodeTaggedDouble(tag: Int): Double = field.getDouble(tag)

        override fun decodeTaggedEnum(tag: Int, enumDescription: SerialDescriptor): Int = field.getInt(tag)

        override fun decodeTaggedFloat(tag: Int): Float = field.getDouble(tag).toFloat()

        override fun decodeTaggedInt(tag: Int): Int = field.getInt(tag)

        override fun decodeTaggedLong(tag: Int): Long = field.getLong(tag)

        override fun decodeTaggedNotNullMark(tag: Int): Boolean = field.fields[tag] != NullField

        override fun decodeTaggedNull(tag: Int): Nothing? {
            require(field.fields[tag] == NullField)
            return null
        }

        override fun decodeTaggedShort(tag: Int): Short = field.getInt(tag).toShort()

        @UseExperimental(ExperimentalStdlibApi::class)
        override fun decodeTaggedString(tag: Int): String = field.getBinary(tag).decodeToString()

        override fun decodeTaggedUnit(tag: Int) {
            val emptyObject = field.getObject(tag)
            require(emptyObject.fields.isEmpty())
        }
    }

    companion object : BinaryFormat {
        private val log = Log("MessageFormat")
        public val default = MessageFormat()

        override val context: SerialModule get() = default.context
        override fun <T> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray = default.dump(serializer, obj)
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
