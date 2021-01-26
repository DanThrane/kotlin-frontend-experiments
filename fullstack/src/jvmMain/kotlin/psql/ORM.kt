package dk.thrane.playground.psql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.TaggedEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

internal class PrimitiveDecoder(
    private val row: DBRow,
    private val index: Int,
    override val serializersModule: SerializersModule = EmptySerializersModule
) : Decoder {
    private val value: Any? get() = row.getUntyped(index)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        throw IllegalStateException("Not supported")
    }

    override fun decodeBoolean(): Boolean = value as Boolean
    override fun decodeByte(): Byte = value as Byte
    override fun decodeChar(): Char = value as Char
    override fun decodeDouble(): Double = value as Double
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        when (row.columnDefinitions[index].type) {
            PGType.Int2, PGType.Int4, PGType.Int8, PGType.Numeric -> {
                return (value as Number).toInt()
            }

            PGType.Varchar, PGType.Text -> {
                return enumDescriptor.getElementIndex((value as String))
            }

            else -> {
                throw IllegalStateException("Cannot deserialize enum in $row (index = $index)")
            }
        }
    }
    override fun decodeFloat(): Float = value as Float
    override fun decodeInt(): Int = value as Int
    override fun decodeLong(): Long = value as Long
    override fun decodeShort(): Short = value as Short
    override fun decodeString(): String = value as String

    override fun decodeNotNullMark(): Boolean = value != null

    override fun decodeNull(): Nothing? {
        require(value == null)
        return null
    }

    /*
    override fun decodeUnit() {
        require(value == Unit)
    }
     */
}

internal class PostgresRootEncoder(
    private val target: Array<Any?>,
    private val headers: List<PGType<*>>,
    private val nameToIndex: Map<String, List<Int>>,
    override val serializersModule: SerializersModule = EmptySerializersModule
) : Encoder {
    override fun beginStructure(
        descriptor: SerialDescriptor,
    ): CompositeEncoder {
        return PostgresRowEncoder(target, headers, nameToIndex)
    }

    private fun unexpected(): Nothing = throw IllegalStateException("Unexpected")

    override fun encodeBoolean(value: Boolean) = unexpected()
    override fun encodeByte(value: Byte) = unexpected()
    override fun encodeChar(value: Char) = unexpected()
    override fun encodeDouble(value: Double) = unexpected()
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = unexpected()
    override fun encodeFloat(value: Float) = unexpected()
    override fun encodeInt(value: Int) = unexpected()
    override fun encodeLong(value: Long) = unexpected()
    override fun encodeNull() = unexpected()
    override fun encodeShort(value: Short) = unexpected()
    override fun encodeString(value: String) = unexpected()
}

@OptIn(ExperimentalSerializationApi::class)
internal class PostgresRowEncoder(
    private val target: Array<Any?>,
    private val headers: List<PGType<*>>,
    private val nameToIndex: Map<String, List<Int>>,
    override val serializersModule: SerializersModule = EmptySerializersModule
) : CompositeEncoder {
    init {
        require(headers.size == target.size)
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        require(headers[index] == PGType.Bool)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        TODO()
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        require(headers[index] == PGType.Char)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        require(headers[index] == PGType.Float8)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        require(headers[index] == PGType.Float4)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        require(headers[index] == PGType.Int4)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        require(headers[index] == PGType.Int8)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    private fun encodeAnyElement(
        value: Any?,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<*>
    ) {
        val name = descriptor.getElementName(index)

        when {
            value == null -> {
                nameToIndex.getValue(name).forEach { i -> target[i] = null }
            }

            serializer.descriptor.kind == SerialKind.ENUM && value is Enum<*> -> {
                val ordinal = value.ordinal
                when (headers[index]) {
                    PGType.Int2 -> nameToIndex.getValue(name).forEach { i -> target[i] = ordinal.toShort() }
                    PGType.Int4 -> nameToIndex.getValue(name).forEach { i -> target[i] = ordinal }
                    PGType.Int8 -> nameToIndex.getValue(name).forEach { i -> target[i] = ordinal.toLong() }
                    PGType.Numeric -> nameToIndex.getValue(name).forEach { i -> target[i] = ordinal }
                    PGType.Text -> nameToIndex.getValue(name).forEach { i -> target[i] = value.name }
                    else -> {
                        throw IllegalArgumentException("Bad type: ${headers[index]}")
                    }
                }
            }

            serializer.descriptor.kind is StructureKind.LIST &&
                    serializer.descriptor.getElementDescriptor(0).kind == PrimitiveKind.BYTE -> {
                val byteArray = value as? ByteArray ?: throw IllegalStateException("Expected byte array for index $index")
                nameToIndex.getValue(name).forEach { i -> target[i] = byteArray }
            }

            else -> {
                throw IllegalStateException("Unsupported type (idx = $index): $value")
            }
        }
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        encodeAnyElement(value, descriptor, index, serializer)
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeAnyElement(value, descriptor, index, serializer)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        require(headers[index] == PGType.Int2)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        require(headers[index] == PGType.Text)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}
