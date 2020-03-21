package dk.thrane.playground.psql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.*
import kotlinx.serialization.internal.TaggedEncoder
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule

// TODO This is by no means done but it does show that it is possible
internal class PostgresRootDecoder(override val context: SerialModule, private val row: DBRow) : Decoder {
    override val updateMode = UpdateMode.BANNED

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return PostgresDecoder(row, context)
    }

    override fun decodeNotNullMark(): Boolean = true
    private fun unexpected(): Nothing = throw IllegalStateException("Did not expect to be called")

    override fun decodeBoolean(): Boolean = unexpected()
    override fun decodeByte(): Byte = unexpected()
    override fun decodeChar(): Char = unexpected()
    override fun decodeDouble(): Double = unexpected()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = unexpected()
    override fun decodeFloat(): Float = unexpected()
    override fun decodeInt(): Int = unexpected()
    override fun decodeLong(): Long = unexpected()
    override fun decodeNull(): Nothing? = unexpected()
    override fun decodeShort(): Short = unexpected()
    override fun decodeString(): String = unexpected()
    override fun decodeUnit() = unexpected()
}

internal class PostgresDecoder(private val row: DBRow, override val context: SerialModule) : CompositeDecoder {
    override val updateMode: UpdateMode = UpdateMode.BANNED

    override fun decodeSequentially(): Boolean = true
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw NotImplementedError()

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        row.getUntyped(index) as Boolean

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = row.getUntyped(index) as Byte
    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = row.getUntyped(index) as Char
    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = row.getUntyped(index) as Double
    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = row.getUntyped(index) as Float
    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = row.getUntyped(index) as Int
    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = row.getUntyped(index) as Long
    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = row.getUntyped(index) as Short
    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = row.getUntyped(index) as String

    override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
        // Empty
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Empty
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>
    ): T {
        @Suppress("UNCHECKED_CAST")
        if (row.getUntyped(index) == null) return null as T

        if (deserializer.descriptor.kind == StructureKind.LIST) {
            if (row.columnDefinitions[index].type == PGType.Bytea) {
                // TODO How to check if this is actually correct output type?
                @Suppress("UNCHECKED_CAST")
                return row[index, PGType.Bytea] as T
            }
            TODO()
        } else {
            throw IllegalStateException("Cannot deserialize row (unsupported): " +
                    "${deserializer.descriptor} at index $index")
        }
    }

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
    ): T? {
        return decodeSerializableElement(descriptor, index, deserializer)
    }

    override fun <T : Any> updateNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        old: T?
    ): T? = throw NotImplementedError()

    override fun <T> updateSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        old: T
    ): T = throw NotImplementedError()
}

internal fun <T> Flow<DBRow>.mapRows(serializer: KSerializer<T>): Flow<T> {
    return map { row ->
        serializer.deserialize(PostgresRootDecoder(EmptyModule, row))
    }
}

@OptIn(InternalSerializationApi::class)
internal class PostgresByteaEncoder(private val target: ByteArray) : TaggedEncoder<Int>() {
    override fun SerialDescriptor.getTag(index: Int): Int = index

    override fun encodeTaggedByte(tag: Int, value: Byte) {
        target[tag] = value
    }
}

internal class PostgresRowEncoder(
    private val target: Array<Any?>,
    private val headers: List<PGType<*>>,
    private val nameToIndex: Map<String, List<Int>>,
    override val context: SerialModule
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

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (serializer.descriptor.kind == UnionKind.ENUM_KIND && value is Enum<*>) {
            val name = descriptor.getElementName(index)
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
        } else {
            throw IllegalStateException("Unsupported type (idx = $index): $value")
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeNullableSerializableElement(descriptor, index, serializer, value)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        require(headers[index] == PGType.Int2)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        require(headers[index] == PGType.Text)
        nameToIndex.getValue(descriptor.getElementName(index)).forEach { i -> target[i] = value }
    }

    override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}
