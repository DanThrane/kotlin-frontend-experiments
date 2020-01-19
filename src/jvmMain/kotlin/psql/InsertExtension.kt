package dk.thrane.playground.psql

import kotlinx.serialization.KSerializer
import kotlinx.serialization.elementNames

fun <Input> createInsertStatement(
    tableName: String,
    serializer: KSerializer<Input>
): PreparedStatement<Input, EmptyTable> {
    return PreparedStatement(
        buildString {
            append("insert into ")
            append(tableName)
            append(" (")
            append(serializer.descriptor.elementNames().joinToString(", "))
            append(") values (")
            append(serializer.descriptor.elementNames().joinToString(", ") { "?${it}" })
            append(")")
        },
        serializer,
        EmptyTable.serializer()
    )
}

fun <Input> createInsertStatement(
    table: SQLTable,
    serializer: KSerializer<Input>
): PreparedStatement<Input, EmptyTable> {
    return createInsertStatement(table.tableName, serializer)
}
