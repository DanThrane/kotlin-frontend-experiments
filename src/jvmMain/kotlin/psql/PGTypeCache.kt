package dk.thrane.playground.psql

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull

class PGTypeCache {
    private data class Type(val oid: Int, val typeName: String, val typeLength: Int)

    private val typeCache = HashMap<Int, Type>()
    private val typeCacheByName = HashMap<String, Type>()

    private val typeNameToPgType = PGType::class.sealedSubclasses
        .mapNotNull { it.objectInstance }
        .map { it.typeName to it }
        .toMap()

    suspend fun refreshTypeCache(conn: InternalPostgresConnection) {
        conn
            .sendCommand(
                FrontendMessage.Query(
                    """
                        select oid, typname, typlen
                        from pg_catalog.pg_type
                        where typnamespace = 11
                    """
                )
            )
            .mapNotNull { message ->
                if (message is BackendMessage.DataRow) {
                    val stringColumns = message.columns.mapNotNull {
                        if (it is BackendMessage.Column.Data) String(it.bytes, Charsets.UTF_8)
                        else null
                    }

                    Type(stringColumns[0].toInt(), stringColumns[1], stringColumns[2].toInt())
                } else {
                    null
                }
            }
            .collect {
                typeCache[it.oid] = it
                typeCacheByName[it.typeName] = it
            }
    }

    private fun Type.toPGType(): PGType<*> {
        return typeNameToPgType[typeName] ?: PGType.Unknown(typeName)
    }

    fun lookupTypeByOid(oid: Int): PGType<*>? = typeCache[oid]?.toPGType()
    fun lookupTypeByName(name: String): PGType<*>? = typeCacheByName[name]?.toPGType()

    companion object {
   }
}
