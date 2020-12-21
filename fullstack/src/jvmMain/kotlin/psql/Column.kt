package dk.thrane.playground.psql

data class ColumnDefinition<KtType>(val name: String, val type: PGType<KtType>)
data class Column<KtType>(
    val definition: ColumnDefinition<KtType>,
    val value: KtType?,
    val commandIndex: Int = -1
)
