package dk.thrane.playground.psql

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.internal.nullable
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import java.math.BigDecimal

class PostgresConnection(connectionParameters: PostgresConnectionParameters) {
    private val conn = InternalPostgresConnection(connectionParameters)
    private val typeCache = PGTypeCache()

    suspend fun open() {
        conn.open()
        typeCache.refreshTypeCache(conn)
    }

    suspend fun sendQuery(query: String): Flow<DBRow> {
        var rowDescription: List<ColumnDefinition<*>>? = null
        var commandIndex = -1

        return conn
            .sendCommand(FrontendMessage.Query(query))
            .mapNotNull { message ->
                when (message) {
                    is BackendMessage.RowDescription -> {
                        commandIndex++
                        rowDescription = message.fields.map { field ->
                            val type = typeCache.lookupTypeByOid(field.typeObjectId)
                                ?: PGType.Unknown(field.typeObjectId.toString())
                            val name = field.name

                            ColumnDefinition(name, type)
                        }

                        null
                    }

                    is BackendMessage.DataRow -> {
                        val description = rowDescription
                        check(description != null) { "Row description is null" }
                        val row = MutableDBRow(description.size)

                        val stringColumns = message.columns.map {
                            if (it is BackendMessage.Column.Data) String(it.bytes, Charsets.UTF_8)
                            else null
                        }

                        description.forEachIndexed { index, (name, type) ->
                            val column: Column<*> = when (type) {
                                PGType.Int2 -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Int2),
                                        commandIndex,
                                        stringColumns[index]?.toShort()
                                    )
                                }

                                PGType.Int4 -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Int4),
                                        commandIndex,
                                        stringColumns[index]?.toInt()
                                    )
                                }

                                PGType.Int8 -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Int8),
                                        commandIndex,
                                        stringColumns[index]?.toLong()
                                    )
                                }

                                PGType.Text, PGType.Json, PGType.Jsonb, PGType.Xml, is PGType.Unknown -> {
                                    Column(
                                        @Suppress("UNCHECKED_CAST")
                                        ColumnDefinition(name, type as PGType<String>),
                                        commandIndex,
                                        stringColumns[index]
                                    )
                                }

                                PGType.Void -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Void),
                                        commandIndex,
                                        Unit
                                    )
                                }

                                PGType.Bool -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Bool),
                                        commandIndex,
                                        stringColumns[index]?.equals("true", ignoreCase = true)
                                    )
                                }

                                PGType.Float4 -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Float4),
                                        commandIndex,
                                        stringColumns[index]?.toFloat()
                                    )
                                }

                                PGType.Float8 -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Float8),
                                        commandIndex,
                                        stringColumns[index]?.toDouble()
                                    )
                                }

                                PGType.Numeric -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Numeric),
                                        commandIndex,
                                        stringColumns[index]?.let { BigDecimal(it) }
                                    )
                                }

                                PGType.Char -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Char),
                                        commandIndex,
                                        stringColumns[index]?.first()
                                    )
                                }

                                PGType.Bytea -> {
                                    Column(
                                        ColumnDefinition(name, PGType.Bytea),
                                        commandIndex,
                                        stringColumns[index]?.let { hexToBytes(it.substring(2)) }
                                    )
                                }
                            }

                            row[index] = column
                        }

                        row
                    }

                    else -> null
                }
            }
    }
}

interface DBRow {
    val size: Int
    val columnDefinitions: Array<ColumnDefinition<*>>
    fun getUntyped(index: Int): Any?
    operator fun <KtType> get(index: Int, columnDefinition: ColumnDefinition<KtType>): KtType?
    operator fun <KtType> get(index: Int, columnDefinition: PGType<KtType>): KtType?
}

private class MutableDBRow(override val size: Int) : DBRow {
    private val columns = arrayOfNulls<Column<*>>(size)
    private val internalColumnDefinitions = arrayOfNulls<Column<*>>(size)

    @Suppress("UNCHECKED_CAST")
    override val columnDefinitions: Array<ColumnDefinition<*>>
        get() = internalColumnDefinitions as Array<ColumnDefinition<*>>

    operator fun <KtType> set(index: Int, column: Column<KtType>) {
        columns[index] = column
    }

    @Suppress("UNCHECKED_CAST")
    override fun getUntyped(index: Int): Any? = columns[index]?.value

    @Suppress("UNCHECKED_CAST")
    override fun <KtType> get(index: Int, columnDefinition: ColumnDefinition<KtType>): KtType? {
        return (getUntyped(index) as KtType?)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <KtType> get(index: Int, columnDefinition: PGType<KtType>): KtType? {
        return (getUntyped(index) as KtType)
    }

    override fun toString(): String = "DBRow(" + columns.toList() + ")"
}

data class ColumnDefinition<KtType>(val name: String, val type: PGType<KtType>)
data class Column<KtType>(val definition: ColumnDefinition<KtType>, val commandIndex: Int, val value: KtType?)

sealed class PGType<KtType>(val typeName: String) {
    override fun toString() = typeName

    object Int2 : PGType<Short>("int2")
    object Int4 : PGType<Int>("int4")
    object Int8 : PGType<Long>("int8")
    object Text : PGType<String>("text")
    object Json : PGType<String>("json")
    object Jsonb : PGType<String>("jsonb")
    object Void : PGType<Unit>("void")
    object Xml : PGType<String>("xml")
    object Bool : PGType<Boolean>("bool")
    object Float4 : PGType<Float>("float")
    object Float8 : PGType<Double>("double")
    object Numeric : PGType<BigDecimal>("numeric")
    object Char : PGType<kotlin.Char>("char")
    object Bytea : PGType<ByteArray>("bytea")

    /*
        const val money = "money"
        const val bytea = "bytea"
        const val date = "date"
        const val time = "time"
        const val timestamp = "timestamp"
        const val jsonb = "jsonb"
        const val void = "void"
        const val uuid = "uuid"
        const val any = "any"
     */

    class Unknown(typeName: String) : PGType<String>(typeName) {
        override fun toString(): String = "Unknown($typeName)"
    }
}

@Serializable
data class DeadBeef(val foobar: ByteArray)

fun main() {
    val connection = PostgresConnection(
        PostgresConnectionParameters(
            username = "kotlin",
            password = "kotlin",
            database = "kotlin",
            hostname = "localhost"
        )
    )

    val serializer = DeadBeef.serializer()

    runBlocking {
        connection.open()
        println(
            connection
                .sendQuery("SELECT E'\\\\xDEADBEEF'::bytea as foobar;")
                .mapRows(DeadBeef.serializer())
                .toList()
        )
    }
}
