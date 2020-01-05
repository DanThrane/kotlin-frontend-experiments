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

    override fun decodeTaggedNotNullMark(tag: Pair<Int, String>): Boolean = row.getUntyped(tag.first) != null
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
