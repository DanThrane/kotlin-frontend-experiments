package dk.thrane.playground.psql

internal data class PreparedStatementAllocation(val name: String, val freshlyAllocated: Boolean)

internal class PreparedStatementCache(private val connectionId: Int) {
    private data class CacheEntry(val statement: String, val statementName: String)
    private val cache = HashMap<String, CacheEntry>()
    private var statementId = 0

    fun getOrAllocateStatementName(statement: String): PreparedStatementAllocation {
        val cachedEntry = cache[statement]
        if (cachedEntry != null) return PreparedStatementAllocation(cachedEntry.statementName, false)
        val entry = CacheEntry(statement, "ps${connectionId}-${statementId++}")
        cache[statement] = entry
        return PreparedStatementAllocation(entry.statementName, true)
    }
}
