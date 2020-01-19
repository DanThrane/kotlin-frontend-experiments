package dk.thrane.playground.psql

import dk.thrane.playground.Log
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class PreparedStatementId(val id: String, val header: List<PGType<*>>)

class PostgresConnection(connectionParameters: PostgresConnectionParameters) {
    private val conn = InternalPostgresConnection(connectionParameters)
    private val typeCache = PGTypeCache()
    private val connectionId = connectionIdAllocator.getAndIncrement()
    private val preparedStatementCache = PreparedStatementCache(connectionId)
    private val hasCalledOpen = AtomicBoolean(false)
    private var transactionOpen = AtomicBoolean(false)

    suspend fun open() {
        if (hasCalledOpen.compareAndSet(false, true)) {
            conn.open()
            typeCache.refreshTypeCache(conn)
        }
    }

    suspend fun begin() {
        log.debug("begin()")
        if (!transactionOpen.compareAndSet(false, true)) {
            // TODO Close connection
            throw IllegalStateException("Transaction already open!")
        }
        return conn.sendCommand(FrontendMessage.Query("begin")).collect()
    }

    suspend fun commit() {
        log.debug("commit()")
        if (!transactionOpen.compareAndSet(true, false)) {
            // TODO Close connection
            throw IllegalStateException("Transaction already closed!")
        }
        return conn.sendCommand(FrontendMessage.Query("commit")).collect()
    }

    suspend fun rollback() {
        log.debug("rollback()")
        if (!transactionOpen.compareAndSet(true, false)) {
            // TODO Close connection
            throw IllegalStateException("Transaction already closed!")
        }
        return conn.sendCommand(FrontendMessage.Query("rollback")).collect()
    }

    fun sendQuery(query: String): Flow<DBRow> {
        return conn
            .sendCommand(FrontendMessage.Query(query))
            .mapToDbRow()
    }

    suspend fun sendCommand(query: String) {
        sendQuery(query).collect()
    }

    /**
     * Creates a prepared statement and returns
     */
    suspend fun createNativePreparedStatement(
        statement: String,
        header: List<PGType<*>>,
        flush: Boolean = true
    ): PreparedStatementId {
        log.debug("Prepared statement: (Statement: $statement) (Header: $header)")
        val (psName, newStatement) = preparedStatementCache.getOrAllocateStatementName(statement)
        if (newStatement) {
            val typeOids = header.map {
                typeCache.lookupTypeOidByName(it.typeName)
                    ?: throw IllegalArgumentException("Unknown type oid for: $it")
            }.toIntArray()

            conn.sendMessage(FrontendMessage.Parse(psName, statement, typeOids))
            if (flush) conn.sendSync().collect()
        }

        return PreparedStatementId(psName, header)
    }

    /**
     * Invokes an existing prepared statement using [statement] with a flow of [values]
     *
     * Once every [flushRate] commands the database will be asked to execute the statements
     */
    fun invokePreparedStatement(
        statement: PreparedStatementId,
        values: Flow<List<Any?>>,
        flushRate: Int = 50
    ): Flow<DBRow> {
        return flow {
            var dirty = false
            var rowIdx = 0
            var startOfLastChunk = 0
            values.collect { row ->
                if (row.size != statement.header.size) {
                    throw IllegalArgumentException("Bad number of columns in row: $row")
                }

                if (!dirty) {
                    // New chunk has just begun
                    conn.sendMessage(
                        FrontendMessage.Describe(FrontendMessage.CloseTarget.PREPARED_STATEMENT, statement.id)
                    )
                }

                val serialized = row.mapIndexed { colIdx, col ->
                    val textual: String? = when (statement.header[colIdx]) {
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

                conn.sendMessage(
                    FrontendMessage.Bind(
                        "",
                        statement.id,
                        ShortArray(0),
                        serialized,
                        ShortArray(0)
                    )
                )
                conn.sendMessage(FrontendMessage.Execute("", 0))
                dirty = true

                if (rowIdx % flushRate == 0 && rowIdx != 0) {
                    // TODO This will suspend until the rows have been processed by the DB and the user
                    // Using a channelFlow instead means we can continue to send more queries to the DB while it is
                    // being processed.
                    emitAll(conn.sendCommand(FrontendMessage.Sync).mapToDbRow(offset = startOfLastChunk))
                    startOfLastChunk = rowIdx
                    dirty = false
                }

                rowIdx++
            }

            if (dirty) {
                emitAll(conn.sendCommand(FrontendMessage.Sync).mapToDbRow(offset = startOfLastChunk))
            }
        }
    }

    /**
     * Deletes a prepared statement identified by [id]
     */
    suspend fun deletePreparedStatement(id: PreparedStatementId) {
        conn.sendMessage(FrontendMessage.Close(FrontendMessage.CloseTarget.PREPARED_STATEMENT, id.id))
    }

    private fun Flow<BackendMessage>.mapToDbRow(offset: Int = 0): Flow<DBRow> {
        var rowDescription: List<ColumnDefinition<*>>? = null
        var commandIndex = -1 + offset
        return mapNotNull { message ->
            when (message) {
                is BackendMessage.ErrorResponse -> {
                    throw PostgresException.Generic(message.fields)
                }

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
                                    (ColumnDefinition(
                                        name,
                                        type as PGType<String>
                                    )),
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
                                    stringColumns[index]?.equals("t", ignoreCase = true),
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PostgresConnection

        if (connectionId != other.connectionId) return false

        return true
    }

    override fun hashCode(): Int {
        return connectionId
    }

    companion object {
        private val log = Log("PostgresConnection")

        private val connectionIdAllocator = AtomicInteger(0)
    }
}

@Serializable
data class DeadBeef(val foobar: ByteArray)

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

    /*
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
 */

    /*
    connection.sendQuery("create table if not exists bytes(bar bytea)").collect()
    connection.sendPreparedStatement(
        "insert into bytes(bar) values ($1)",
        listOf(AnonymousColumn(PGType.Bytea, byteArrayOf(1, 3, 3, 7)))
    ).collect { println(it) }
     */

    /*
    connection.sendQuery("create table if not exists binary_json(foo jsonb)").collect()
    connection.sendPreparedStatement(
        "insert into binary_json(foo) values ($1)",
        listOf(AnonymousColumn(PGType.Jsonb, "[1, 3, 3, 7]"))
    ).collect()

    connection.sendPreparedStatement("select * from binary_json", emptyList()).collect { println(it) }
     */

    @Serializable
    data class Echo(val a: Int, val b: Int, val c: Int)

    @Serializable
    data class EchoOut(val a: Int, val b: Int, val c: Int, val d: Int)

    val echo2 = PreparedStatement("select ?a, ?b, ?b, ?c", Echo.serializer(), EchoOut.serializer()).asQuery()
    println(echo2(connection, Echo(1, 3, 7)).toList())
}
