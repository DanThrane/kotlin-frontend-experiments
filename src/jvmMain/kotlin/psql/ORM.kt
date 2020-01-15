package dk.thrane.playground.psql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.*

// TODO This is by no means done but it does show that it is possible
class PostgresDecoder(private val row: DBRow) : TaggedDecoder<Pair<Int, String>>() {
    override fun SerialDescriptor.getTag(index: Int): Pair<Int, String> = index to getElementName(index)

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        val tag = currentTagOrNull ?: return this
        if (desc.kind == StructureKind.LIST) {
            return PostgresListDecoder(row, tag)
        }
        return this
    }

    override fun decodeTaggedNotNullMark(tag: Pair<Int, String>): Boolean =
        row.getUntypedByName(tag.second, tag.first) != null

    override fun decodeTaggedBoolean(tag: Pair<Int, String>): Boolean {
        return row.getUntypedByName(tag.second, tag.first) as Boolean
    }

    override fun decodeTaggedByte(tag: Pair<Int, String>): Byte {
        return row.getUntypedByName(tag.second, tag.first) as Byte
    }

    override fun decodeTaggedChar(tag: Pair<Int, String>): Char {
        return row.getUntypedByName(tag.second, tag.first) as Char
    }

    override fun decodeTaggedDouble(tag: Pair<Int, String>): Double {
        return row.getUntypedByName(tag.second, tag.first) as Double
    }

    override fun decodeTaggedEnum(tag: Pair<Int, String>, enumDescription: SerialDescriptor): Int {
        return super.decodeTaggedEnum(tag, enumDescription)
    }

    override fun decodeTaggedFloat(tag: Pair<Int, String>): Float {
        return row.getUntypedByName(tag.second, tag.first) as Float
    }

    override fun decodeTaggedInt(tag: Pair<Int, String>): Int {
        return row.getUntypedByName(tag.second, tag.first) as Int
    }

    override fun decodeTaggedLong(tag: Pair<Int, String>): Long {
        return row.getUntypedByName(tag.second, tag.first) as Long
    }

    override fun decodeTaggedNull(tag: Pair<Int, String>): Nothing? {
        if (row.getUntypedByName(tag.second, tag.first) == null) {
            return null
        }

        throw IllegalStateException("Expected null but value was not null")
    }

    override fun decodeTaggedShort(tag: Pair<Int, String>): Short {
        return row.getUntypedByName(tag.second, tag.first) as Short
    }

    override fun decodeTaggedString(tag: Pair<Int, String>): String {
        return row.getUntypedByName(tag.second, tag.first) as String
    }

    override fun decodeTaggedUnit(tag: Pair<Int, String>) {
    }
}

class PostgresListDecoder(private val row: DBRow, private val listTag: Pair<Int, String>) : TaggedDecoder<Int>() {
    private val bytea = row[listTag.first, PGType.Bytea]!!
    override fun SerialDescriptor.getTag(index: Int): Int = index
    override fun decodeTaggedByte(tag: Int): Byte = bytea[tag]
    override fun decodeCollectionSize(desc: SerialDescriptor): Int = bytea.size
}

fun <T> Flow<DBRow>.mapRows(serializer: KSerializer<T>): Flow<T> {
    return map { row ->
        serializer.deserialize(PostgresDecoder(row))
    }
}

class PostgresRowEncoder(
    private val target: Array<Any?>,
    private val headers: List<PGType<*>>,
    private val nameToIndex: Map<String, List<Int>>
) : TaggedEncoder<Pair<Int, String>>() {
    init {
        require(headers.size == target.size)
    }

    override fun SerialDescriptor.getTag(index: Int): Pair<Int, String> = index to getElementName(index)

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        val tag = currentTagOrNull ?: return this
        TODO()
    }

    override fun encodeTaggedBoolean(tag: Pair<Int, String>, value: Boolean) {
        require(headers[tag.first] == PGType.Bool)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedByte(tag: Pair<Int, String>, value: Byte) {
        TODO()
    }

    override fun encodeTaggedChar(tag: Pair<Int, String>, value: Char) {
        require(headers[tag.first] == PGType.Char)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedDouble(tag: Pair<Int, String>, value: Double) {
        require(headers[tag.first] == PGType.Float8)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedEnum(tag: Pair<Int, String>, enumDescription: SerialDescriptor, ordinal: Int) {
        when (headers[tag.first]) {
            PGType.Int2 -> nameToIndex.getValue(tag.second).forEach { i -> target[i] = ordinal.toShort() }
            PGType.Int4 -> nameToIndex.getValue(tag.second).forEach { i -> target[i] = ordinal }
            PGType.Int8 -> nameToIndex.getValue(tag.second).forEach { i -> target[i] = ordinal.toLong() }
            PGType.Numeric -> nameToIndex.getValue(tag.second).forEach { i -> target[i] = ordinal }
            PGType.Text -> nameToIndex.getValue(tag.second).forEach { i -> target[i] = enumDescription.getElementName(ordinal) }
            else -> {
                throw IllegalArgumentException("Bad type: ${headers[tag.first]}")
            }
        }
    }

    override fun encodeTaggedFloat(tag: Pair<Int, String>, value: Float) {
        require(headers[tag.first] == PGType.Float4)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedInt(tag: Pair<Int, String>, value: Int) {
        require(headers[tag.first] == PGType.Int4)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedLong(tag: Pair<Int, String>, value: Long) {
        require(headers[tag.first] == PGType.Int8)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedNull(tag: Pair<Int, String>) {
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = null }
    }

    override fun encodeTaggedShort(tag: Pair<Int, String>, value: Short) {
        require(headers[tag.first] == PGType.Int2)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }

    override fun encodeTaggedString(tag: Pair<Int, String>, value: String) {
        require(headers[tag.first] == PGType.Text)
        nameToIndex.getValue(tag.second).forEach { i -> target[i] = value }
    }
}
