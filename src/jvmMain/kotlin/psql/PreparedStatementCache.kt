package dk.thrane.playground.psql

data class PreparedStatementAllocation(val name: String, val freshlyAllocated: Boolean)

class PreparedStatementCache {
    private data class CacheEntry(val statement: String, val statementName: String)
    private val cache = HashMap<String, CacheEntry>()
    private var statementId = 0

    fun getOrAllocateStatementName(statement: String): PreparedStatementAllocation {
        val cachedEntry = cache[statement]
        if (cachedEntry != null) return PreparedStatementAllocation(cachedEntry.statementName, false)
        val entry = CacheEntry(statement, "ps${statementId++}")
        cache[statement] = entry
        return PreparedStatementAllocation(entry.statementName, true)
    }
}
