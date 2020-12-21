package dk.thrane.playground.psql

interface DBRow {
    val size: Int
    val columnDefinitions: Array<ColumnDefinition<*>>
    fun getUntyped(index: Int): Any?
    fun getUntypedByName(name: String, fallbackIndex: Int? = null): Any?
    operator fun <KtType> get(index: Int, columnDefinition: ColumnDefinition<KtType>): KtType?
    operator fun <KtType> get(index: Int, columnDefinition: PGType<KtType>): KtType?
}

internal class MutableDBRow(override val size: Int) : DBRow {
    private val columns = arrayOfNulls<Column<*>>(size)
    private val internalColumnDefinitions = arrayOfNulls<ColumnDefinition<*>>(size)

    @Suppress("UNCHECKED_CAST")
    override val columnDefinitions: Array<ColumnDefinition<*>>
        get() = internalColumnDefinitions as Array<ColumnDefinition<*>>

    operator fun <KtType> set(index: Int, column: Column<KtType>) {
        columns[index] = column
        internalColumnDefinitions[index] = column.definition
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

    override fun getUntypedByName(name: String, fallbackIndex: Int?): Any? {
        val colIndex = columnDefinitions.indexOfFirst { it.name == name }.takeIf { it != -1 } ?: fallbackIndex

        if (colIndex == null || colIndex !in columnDefinitions.indices) {
            throw IllegalArgumentException(
                "Unknown column '$name'. " +
                        "Anonymous fallback: $fallbackIndex). " +
                        "Known columns: ${columnDefinitions.toList()}"
            )
        }

        return getUntyped(colIndex)
    }

    override fun toString(): String = "DBRow(" + columns.toList() + ")"
}
