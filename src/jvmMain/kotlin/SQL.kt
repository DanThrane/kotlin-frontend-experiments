package dk.thrane.playground

import java.net.URL
import java.sql.*

typealias DBConnectionPool = ObjectPool<Connection>

class SQLField<Type : JdbcType<*>>(
    val name: String,
    val type: String,
    val jdbcType: Type,
    val notNull: Boolean = false
) {
    override fun toString(): String = name
}

abstract class SQLTable(val name: String, val schema: String? = null) {
    private val backingFields = ArrayList<SQLField<*>>()
    val fields get() = backingFields.toList()

    internal fun <K, T : JdbcType<K>> addField(
        name: String,
        type: String,
        jdbcType: T,
        notNull: Boolean
    ): SQLField<T> {
        val element = SQLField(name, type, jdbcType, notNull)
        backingFields.add(element)
        return element
    }

    override fun toString(): String {
        return if (schema != null) "${schema}.$name"
        else name
    }

    abstract fun migration(handler: MigrationHandler)
}

fun SQLTable.varchar(name: String, size: Int, notNull: Boolean = false) =
    addField(name, "varchar($size)", JdbcType.TString, notNull)

fun SQLTable.int(
    name: String,
    notNull: Boolean = false
) = addField(name, "int", JdbcType.TInt, notNull)

fun SQLTable.blob(
    name: String,
    notNull: Boolean = false
) = addField(name, "blob", JdbcType.TBlob, notNull)

fun SQLTable.long(
    name: String,
    notNull: Boolean = false
) = addField(name, "bigint", JdbcType.TLong, notNull)

sealed class JdbcType<T> {
    object TArray : JdbcType<java.sql.Array>()
    object TString : JdbcType<String>()
    object TBoolean : JdbcType<Boolean>()
    object TByte : JdbcType<Byte>()
    object TShort : JdbcType<Short>()
    object TInt : JdbcType<Int>()
    object TLong : JdbcType<Long>()
    object TFloat : JdbcType<Float>()
    object TDouble : JdbcType<Double>()
    object TBytes : JdbcType<ByteArray>()
    object TDate : JdbcType<Date>()
    object TTime : JdbcType<Time>()
    object Timestamp : JdbcType<java.sql.Timestamp>()
    object TURL : JdbcType<URL>()
    object TBlob : JdbcType<Blob>()
}

class SQLRow(private val map: HashMap<SQLField<*>, Any?> = HashMap()) {
    operator fun <KType, T : JdbcType<KType>> get(key: SQLField<T>): KType {
        @Suppress("UNCHECKED_CAST")
        return map[key] as KType
    }

    fun <KType, T : JdbcType<KType>> getOrNull(key: SQLField<T>): KType? {
        val value = map[key] ?: return null

        @Suppress("UNCHECKED_CAST")
        return value as KType
    }

    operator fun <KType, T : JdbcType<KType>> set(key: SQLField<T>, value: KType) {
        map[key] = value
    }

    fun getUntyped(key: SQLField<*>): Any? {
        return map[key]
    }

    override fun toString(): String = "SQLRow($map)"
}

fun ResultSetEnhanced.mapTable(table: SQLTable): SQLRow {
    val map = HashMap<SQLField<*>, Any?>()

    columnNames.zip(tableNames).forEachIndexed { idx, (column, tableName) ->
        if (!table.name.equals(tableName, ignoreCase = true)) return@forEachIndexed

        val field = table.fields.find { fieldName ->
            column.equals(fieldName.name, ignoreCase = true)
        } ?: return@forEachIndexed

        map[field] = when (field.jdbcType) {
            JdbcType.TArray -> getArray(idx + 1)
            JdbcType.TString -> getString(idx + 1)
            JdbcType.TBoolean -> getBoolean(idx + 1)
            JdbcType.TByte -> getByte(idx + 1)
            JdbcType.TShort -> getShort(idx + 1)
            JdbcType.TInt -> getInt(idx + 1)
            JdbcType.TLong -> getLong(idx + 1)
            JdbcType.TFloat -> getFloat(idx + 1)
            JdbcType.TDouble -> getDouble(idx + 1)
            JdbcType.TBytes -> getBytes(idx + 1)
            JdbcType.TDate -> getDate(idx + 1)
            JdbcType.TTime -> getTime(idx + 1)
            JdbcType.Timestamp -> getTimestamp(idx + 1)
            JdbcType.TURL -> getURL(idx + 1)
            JdbcType.TBlob -> getBlob(idx + 1)
        }
    }

    return SQLRow(map)
}

class ResultSetEnhanced(private val delegate: ResultSet) : ResultSet by delegate {
    private val metadata = metaData

    val columnNames: List<String> by lazy {
        (1..metadata.columnCount).map { idx ->
            metadata.getColumnLabel(idx)
        }
    }

    val tableNames: List<String> by lazy {
        (1..metadata.columnCount).map { idx ->
            metadata.getTableName(idx)
        }
    }
}

fun ResultSet.enhance(): ResultSetEnhanced = ResultSetEnhanced(this)

fun SQLTable.creationScript(): String {
    val builder = StringBuilder()
    builder.append("create table ${name}(")
    fields.forEachIndexed { idx, field ->
        builder.append(field.name)
        builder.append(" ")
        builder.append(field.type)

        if (field.notNull) {
            builder.append(" not null")
        }

        if (idx != fields.lastIndex) {
            builder.append(", ")
        }
    }
    builder.append(")")
    return builder.toString()
}

fun Connection.insert(
    table: SQLTable,
    rows: List<SQLRow>
): IntArray {
    val statement = prepareStatement("insert into $table values (${table.fields.joinToString(", ") { "?" }})")
    rows.forEach { row ->
        @Suppress("UNCHECKED_CAST")
        table.fields.forEachIndexed { index, sqlField ->
            val value = row.getUntyped(sqlField) ?: return@forEachIndexed

            @Suppress("UNUSED_VARIABLE")
            val ignored = when (sqlField.jdbcType) {
                JdbcType.TArray -> TODO()
                JdbcType.TString -> statement.setString(index + 1, value as String?)
                JdbcType.TBoolean -> statement.setBoolean(index + 1, value as Boolean)
                JdbcType.TByte -> statement.setByte(index + 1, value as Byte)
                JdbcType.TShort -> statement.setShort(index + 1, value as Short)
                JdbcType.TInt -> statement.setInt(index + 1, value as Int)
                JdbcType.TLong -> statement.setLong(index + 1, value as Long)
                JdbcType.TFloat -> statement.setFloat(index + 1, value as Float)
                JdbcType.TDouble -> statement.setDouble(index + 1, value as Double)
                JdbcType.TBytes -> statement.setBytes(index + 1, value as ByteArray)
                JdbcType.TDate -> statement.setDate(index + 1, value as Date)
                JdbcType.TTime -> statement.setTime(index + 1, value as Time)
                JdbcType.Timestamp -> statement.setTimestamp(index + 1, value as Timestamp)
                JdbcType.TURL -> statement.setURL(index + 1, value as URL)
                JdbcType.TBlob -> statement.setBlob(index + 1, value as Blob)
            }
        }

        statement.addBatch()
        statement.clearParameters()
    }

    return statement.executeBatch()
}

fun Connection.statement(statement: String): EnhancedPreparedStatement =
    EnhancedPreparedStatement(this, statement)

fun Connection.statement(statement: String, block: EnhancedPreparedStatement.() -> Unit): PreparedStatement =
    EnhancedPreparedStatement(this, statement).apply(block).toPreparedStatement()

fun Connection.statement(statement: String, parameters: Map<String, Any?>): PreparedStatement =
    EnhancedPreparedStatement(this, statement).apply {
        for ((k, v) in parameters)  {
            when (v) {
                null -> {
                    setParameterAsNull(k)
                }

                is String -> setParameter(k, v)
                is Boolean -> setParameter(k, v)
                is Byte -> setParameter(k, v)
                is Short -> setParameter(k, v)
                is Int -> setParameter(k, v)
                is Long -> setParameter(k, v)
                is Float -> setParameter(k, v)
                is Double -> setParameter(k, v)
                is ByteArray -> setParameter(k, v)
                is Date -> setParameter(k, v)
                is Time -> setParameter(k, v)
                is Timestamp -> setParameter(k, v)
                is URL -> setParameter(k, v)
                is Blob -> setParameter(k, v)
                else -> throw IllegalArgumentException("Unknown type for '$k'. Received: $v")
            }
        }
    }.toPreparedStatement()

class EnhancedPreparedStatement(
    connection: Connection,
    statement: String
) {
    private val parameterNamesToIndex: Map<String, List<Int>>
    private val preparedStatement: PreparedStatement
    private val boundValues = HashSet<String>()

    init {
        val parameterNamesToIndex = HashMap<String, List<Int>>()

        val queryBuilder = StringBuilder()
        var parameterIndex = 1 // Parameters start at 1
        var stringIndex = 0
        while (stringIndex < statement.length) {
            val nextParameter = statement.indexOf('?', stringIndex)
            if (nextParameter == -1) {
                queryBuilder.append(statement.substring(stringIndex))
                break
            }

            queryBuilder.append(statement.substring(stringIndex, nextParameter + 1)) // include '?'
            queryBuilder.append(' ')

            val endOfParameterName = statement.indexOf(' ', nextParameter + 1).takeIf { it != -1 } ?: statement.length
            val parameterName = statement.substring(nextParameter + 1, endOfParameterName)
            stringIndex = endOfParameterName + 1

            parameterNamesToIndex[parameterName] =
                (parameterNamesToIndex[parameterName] ?: emptyList()) + listOf(parameterIndex)

            parameterIndex++
        }

        this.parameterNamesToIndex = parameterNamesToIndex
        this.preparedStatement = connection.prepareStatement(queryBuilder.toString())
    }

    fun setParameterAsNull(name: String) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setObject(index, null)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: String) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setString(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Boolean) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setBoolean(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Byte) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setByte(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Short) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setShort(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Int) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setInt(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Long) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setLong(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Float) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setFloat(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Double) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setDouble(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: ByteArray) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setBytes(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Date) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setDate(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Time) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setTime(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Timestamp) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setTimestamp(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: URL) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setURL(index, value)
        }
        boundValues.add(name)
    }

    fun setParameter(name: String, value: Blob) {
        val indices = parameterNamesToIndex[name] ?: throw IllegalArgumentException("Unknown parameter '$name'")
        for (index in indices) {
            preparedStatement.setBlob(index, value)
        }
        boundValues.add(name)
    }

    fun toPreparedStatement(allowUnboundValues: Boolean = false): PreparedStatement {
        if (allowUnboundValues) return preparedStatement

        check(boundValues == parameterNamesToIndex.keys) {
            "This prepared statement has unbound values!\n" +
            "Statement: $preparedStatement\n" +
            "Missing values: ${parameterNamesToIndex.keys - boundValues}\n" +
            "Given values: $boundValues"
        }

        return preparedStatement
    }
}

fun <R> PreparedStatement.mapQuery(mapper: (ResultSetEnhanced) -> R): List<R> {
    return executeQuery().enhance().mapToResult(mapper)
}

fun <R> ResultSetEnhanced.mapToResult(mapper: (ResultSetEnhanced) -> R): List<R> {
    use {
        val result = ArrayList<R>()
        while (next()) {
            result.add(mapper(this))
        }
        return result
    }
}
