package dk.thrane.playground

import java.net.URL
import java.sql.*

fun main() {
    val pool = ObjectPool<Connection>(
        size = 1,
        itemGenerator = {
            Class.forName("org.h2.Driver")
            DriverManager.getConnection("jdbc:h2:mem:data;DB_CLOSE_DELAY=-1", "sa", "")
        },
        reset = {}
    )

    pool.useInstance { conn ->
        conn.prepareStatement(MyTable.creationScript()).executeUpdate()

        conn.insert(
            MyTable,
            (0 until 100).map { idx ->
                SQLRow().also { row ->
                    row[MyTable.a] = "A: $idx"
                    row[MyTable.b] = "B: $idx"
                    row[MyTable.c] = "C: $idx"
                    row[MyTable.d] = "D: $idx"
                    row[MyTable.e] = "E: $idx"
                }
            }
        )

        conn.prepareStatement("select * from $MyTable").mapQuery { row ->
            row.mapTable(MyTable)
        }.forEach { println(it) }
    }
}

class SQLField<Type : JdbcType<*>>(
    val name: String,
    val type: String,
    val jdbcType: Type,
    val notNull: Boolean = false
) {
    override fun toString(): String = "($name: $type)"
}

abstract class SQLTable(val name: String, val schema: String? = null) {
    private val backingFields = ArrayList<SQLField<*>>()
    val fields get() = backingFields.toList()

    internal fun addField(field: SQLField<*>) {
        backingFields.add(field)
    }

    override fun toString(): String {
        return if (schema != null) "${schema}.$name"
        else name
    }
}

fun SQLTable.varchar(name: String, size: Int, notNull: Boolean = false) =
    SQLField(name, "varchar($size)", JdbcType.TString).also { addField(it) }

fun SQLTable.int(name: String, notNull: Boolean = false) = SQLField(name, "int", JdbcType.TInt).also { addField(it) }

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
}

class SQLRow(private val map: HashMap<SQLField<*>, Any?> = HashMap()) {
    operator fun <KType, T : JdbcType<KType>> get(key: SQLField<T>): KType? {
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

object MyTable : SQLTable("my_table") {
    val a = varchar("a", 256)
    val b = varchar("b", 256)
    val c = varchar("c", 256)
    val d = varchar("d", 256)
    val e = varchar("e", 256)
}

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
            }
        }

        statement.addBatch()
        statement.clearParameters()
    }

    return statement.executeBatch()
}

fun <R> PreparedStatement.mapQuery(mapper: (ResultSetEnhanced) -> R): List<R> {
    return executeQuery().enhance().mapToResult(mapper)
}

fun <R> ResultSetEnhanced.mapToResult(mapper: (ResultSetEnhanced) -> R): List<R> {
    val result = ArrayList<R>()
    while (next()) {
        result.add(mapper(this))
    }
    return result
}
