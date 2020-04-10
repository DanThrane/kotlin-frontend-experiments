package dk.thrane.playground.database

import dk.thrane.playground.Log

internal sealed class Event {
    abstract val transactionId: Long

    data class Create<Doc : Document>(override val transactionId: Long, val document: DocumentWithHeader<Doc>) : Event()

    data class Update<Doc : Document>(
        override val transactionId: Long,
        val oldDocument: DocumentWithHeader<Doc>,
        val newDocument: DocumentWithHeader<Doc>
    ) : Event() {
        init {
            require(newDocument.header.id == oldDocument.header.id) { "IDs of the two documents do not match ($this)" }
        }
    }

    data class Delete<Doc : Document>(
        override val transactionId: Long,
        val document: DocumentWithHeader<Doc>
    ) : Event()

    data class OpenTransaction(override val transactionId: Long, val user: String) : Event()
    data class Commit(override val transactionId: Long) : Event()
    data class Rollback(override val transactionId: Long) : Event()
}

/**
 * Reads and writes the event log
 */
internal class EventLog {
    fun addEntry(event: Event) {
        log.debug("Adding event: $event")
    }

    fun readLog(): Sequence<Event> {
        return emptySequence()
    }

    companion object {
        private val log = Log("EventLog")
    }
}
