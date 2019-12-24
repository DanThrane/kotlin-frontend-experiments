package dk.thrane.playground.db

import com.github.jasync.sql.db.ResultSet
import dk.thrane.playground.MigrationHandler
import org.joda.time.DateTime

class SQLField<Type : SqlType<*>>(
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

    internal fun <K, T : SqlType<K>> addField(
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
    addField(name, "varchar($size)", SqlType.TString, notNull)

fun SQLTable.int(
    name: String,
    notNull: Boolean = false
) = addField(name, "int", SqlType.TInt, notNull)

fun SQLTable.bytea(
    name: String,
    notNull: Boolean = false
) = addField(name, "bytea", SqlType.TBytea, notNull)

fun SQLTable.long(
    name: String,
    notNull: Boolean = false
) = addField(name, "bigint", SqlType.TLong, notNull)

sealed class SqlType<T> {
    object TString : SqlType<String>()
    object TBoolean : SqlType<Boolean>()
    object TByte : SqlType<Byte>()
    object TShort : SqlType<Short>()
    object TInt : SqlType<Int>()
    object TLong : SqlType<Long>()
    object TFloat : SqlType<Float>()
    object TDouble : SqlType<Double>()
    object TBytes : SqlType<ByteArray>()
    object Timestamp : SqlType<DateTime>()
    object TBytea : SqlType<ByteArray>()
}

class SQLRow(private val map: HashMap<SQLField<*>, Any?> = HashMap()) {
    operator fun <KType, T : SqlType<KType>> get(key: SQLField<T>): KType {
        @Suppress("UNCHECKED_CAST")
        return map[key] as KType
    }

    fun <KType, T : SqlType<KType>> getOrNull(key: SQLField<T>): KType? {
        val value = map[key] ?: return null

        @Suppress("UNCHECKED_CAST")
        return value as KType
    }

    operator fun <KType, T : SqlType<KType>> set(key: SQLField<T>, value: KType) {
        map[key] = value
    }

    fun getUntyped(key: SQLField<*>): Any? {
        return map[key]
    }

    override fun toString(): String = "SQLRow($map)"
}

fun ResultSet.mapTable(table: SQLTable): List<SQLRow> {
    val map = HashMap<SQLField<*>, Any?>()

    return map { row ->
        table.fields.forEach { column ->
            map[column] = row[column.name]
        }

        SQLRow(map)
    }
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

suspend fun AsyncDBConnection.insert(
    table: SQLTable,
    row: SQLRow
) {
    sendPreparedStatement(
        "insert into $table values (${table.fields.joinToString(", ") { "?" }})",
        table.fields.map { row.getUntyped(it) }
    )
}
