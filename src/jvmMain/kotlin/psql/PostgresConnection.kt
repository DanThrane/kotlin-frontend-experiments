package dk.thrane.playground.psql

import dk.thrane.playground.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import java.math.BigDecimal
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class PostgresConnection(connectionParameters: PostgresConnectionParameters) {
    private val conn = InternalPostgresConnection(connectionParameters)
    private val typeCache = PGTypeCache()
    private val preparedStatementCache = PreparedStatementCache()

    suspend fun open() {
        conn.open()
        typeCache.refreshTypeCache(conn)
    }

    fun sendQuery(query: String): Flow<DBRow> {
        return conn
            .sendCommand(FrontendMessage.Query(query))
            .mapToDbRow()
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    fun sendPreparedStatement(
        statement: String,
        header: List<PGType<*>>,
        values: Flow<List<Any?>>,
        flushRate: Int = 50
    ): Flow<DBRow> {
        return flow {
            val (psName, newStatement) = preparedStatementCache.getOrAllocateStatementName(statement)
            val headerSize = header.size
            if (newStatement) {
                val typeOids = header.map {
                    typeCache.lookupTypeOidByName(it.typeName)
                        ?: throw IllegalArgumentException("Unknown type oid for: $it")
                }.toIntArray()

                conn.sendMessage(FrontendMessage.Parse(psName, statement, typeOids))
            }

            var rowIdx = 0
            var startOfLastChunk = 0
            values.collect { row ->
                if (row.size != headerSize) throw IllegalArgumentException("Bad number of columns in row: $row")
                if (startOfLastChunk == rowIdx - 1) {
                    // New chunk has just begun
                    conn.sendMessageNow(
                        FrontendMessage.Describe(FrontendMessage.CloseTarget.PREPARED_STATEMENT, psName)
                    )
                }

                val serialized = row.mapIndexed { colIdx, col ->
                    val textual: String? = when (header[colIdx]) {
                        PGType.Int2, PGType.Int4, PGType.Int8 -> col?.toString()
                        PGType.Text, PGType.Json, PGType.Jsonb, PGType.Xml -> col?.toString()
                        PGType.Bool -> (col as Boolean?)?.toString()
                        PGType.Float4, PGType.Float8, PGType.Numeric -> col?.toString()
                        PGType.Char -> col?.toString()
                        PGType.Bytea -> {
                            val value = col as ByteArray?
                            if (value == null) null
                            else "\\x" + bytesToHex(value)
                        }
                        PGType.Void, is PGType.Unknown -> throw NotImplementedError("Unknown type")
                    }

                    textual?.toByteArray(Charsets.UTF_8)
                }.toTypedArray()

                conn.sendMessageNow(FrontendMessage.Bind("$rowIdx", psName, ShortArray(0), serialized, ShortArray(0)))
                conn.sendMessageNow(FrontendMessage.Execute("$rowIdx", 0))

                if (rowIdx % flushRate == 0 && rowIdx != 0) {
                    // TODO This will suspend until the rows have been processed by the DB and the user
                    // Using a channelFlow instead means we can continue to send more queries to the DB while it is
                    // being processed.
                    emitAll(conn.sendCommandNow(FrontendMessage.Sync).mapToDbRow(offset = startOfLastChunk))
                    startOfLastChunk = rowIdx
                }

                rowIdx++
            }

            if (startOfLastChunk != rowIdx - 1) {
                emitAll(conn.sendCommandNow(FrontendMessage.Sync).mapToDbRow(offset = startOfLastChunk))
            }
        }
    }

    private fun Flow<BackendMessage>.mapToDbRow(offset: Int = 0): Flow<DBRow> {
        var rowDescription: List<ColumnDefinition<*>>? = null
        var commandIndex = -1 + offset
        return mapNotNull { message ->
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
                                    stringColumns[index]?.toShort(),
                                    commandIndex
                                )
                            }

                            PGType.Int4 -> {
                                Column(
                                    ColumnDefinition(name, PGType.Int4),
                                    stringColumns[index]?.toInt(),
                                    commandIndex
                                )
                            }

                            PGType.Int8 -> {
                                Column(
                                    ColumnDefinition(name, PGType.Int8),
                                    stringColumns[index]?.toLong(),
                                    commandIndex
                                )
                            }

                            PGType.Text, PGType.Json, PGType.Jsonb, PGType.Xml, is PGType.Unknown -> {
                                Column(
                                    @Suppress("UNCHECKED_CAST")
                                    ColumnDefinition(name, type as PGType<String>),
                                    stringColumns[index],
                                    commandIndex
                                )
                            }

                            PGType.Void -> {
                                Column(
                                    ColumnDefinition(name, PGType.Void),
                                    Unit,
                                    commandIndex
                                )
                            }

                            PGType.Bool -> {
                                Column(
                                    ColumnDefinition(name, PGType.Bool),
                                    stringColumns[index]?.equals("true", ignoreCase = true),
                                    commandIndex
                                )
                            }

                            PGType.Float4 -> {
                                Column(
                                    ColumnDefinition(name, PGType.Float4),
                                    stringColumns[index]?.toFloat(),
                                    commandIndex
                                )
                            }

                            PGType.Float8 -> {
                                Column(
                                    ColumnDefinition(name, PGType.Float8),
                                    stringColumns[index]?.toDouble(),
                                    commandIndex
                                )
                            }

                            PGType.Numeric -> {
                                Column(
                                    ColumnDefinition(name, PGType.Numeric),
                                    stringColumns[index]?.let { BigDecimal(it) },
                                    commandIndex
                                )
                            }

                            PGType.Char -> {
                                Column(
                                    ColumnDefinition(name, PGType.Char),
                                    stringColumns[index]?.first(),
                                    commandIndex
                                )
                            }

                            PGType.Bytea -> {
                                Column(
                                    ColumnDefinition(name, PGType.Bytea),
                                    stringColumns[index]?.let { hexToBytes(it.substring(2)) },
                                    commandIndex
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

    companion object {
        private val log = Log("PostgresConnection")
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
data class Column<KtType>(
    val definition: ColumnDefinition<KtType>,
    val value: KtType?,
    val commandIndex: Int = -1
)

@Suppress("FunctionName")
fun <KtType> AnonymousColumn(type: PGType<KtType>, value: KtType?): Column<KtType> =
    Column(ColumnDefinition("", type), value, 0)

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

suspend fun PostgresConnection.sendPreparedStatement(statement: String, values: List<Column<*>>): Flow<DBRow> {
    return sendPreparedStatement(statement, values.map { it.definition.type }, flowOf(values.map { it.value }))
}

@Serializable
data class DeadBeef(val foobar: ByteArray)

@UseExperimental(ExperimentalTime::class)
suspend fun main() {
    val connection = PostgresConnection(
        PostgresConnectionParameters(
            username = "kotlin",
            password = "kotlin",
            database = "kotlin",
            hostname = "localhost"
        )
    )

    val serializer = DeadBeef.serializer()

    connection.open()
    /*
    println(
        connection
            .sendQuery("SELECT E'\\\\xDEADBEEF'::bytea as foobar;")
            .mapRows(DeadBeef.serializer())
            .toList()
    )
     */

    connection.sendQuery("create table if not exists foo(bar int4)").collect()

    val time = measureTime {
        var complete = 0
        connection.sendPreparedStatement(
            "insert into foo(bar) values($1)",
            listOf(PGType.Int4),
            (0..100_000).asFlow().map { listOf(it) },
            flushRate = 1500
        ).collect {
            complete++
            println("Hi!")
        }
    }
    println("Insertion took $time")
}
