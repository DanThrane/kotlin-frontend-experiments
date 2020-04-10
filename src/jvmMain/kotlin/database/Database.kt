package dk.thrane.playground.database

import dk.thrane.playground.Log
import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Transaction internal constructor(
    internal val user: DatabaseUser,
    internal val timestamp: Long
) {
    internal val ownEventLog = ArrayList<Event>()
}

class Database {
    private val timestamps = AtomicLong(0)

    private val eventLog = EventLog()
    private val store = HashMap<DocumentCompanion<*>, HashMapWithList<DocumentId, DocumentWithHeader<*>>>()
    private var didInit = false
    private val commitMutex = Mutex()

    suspend fun createTransaction(user: DatabaseUser): Transaction {
        val transaction = Transaction(
            user,
            timestamps.getAndIncrement()
        )

        open(transaction, user.id)
        return transaction
    }

    fun registerType(companion: DocumentCompanion<*>) {
        require(!didInit) { "registerType() must be called before initializeStore()!" }
        store[companion] = HashMapWithList()
    }

    private suspend fun <Doc : Document> addToStore(
        documentWithHeader: DocumentWithHeader<Doc>
    ) {
        val type = documentWithHeader.companion
        store.getValue(type).addEntry(documentWithHeader.header.id, documentWithHeader)
        // Notify indexes here
    }

    suspend fun <Doc : Document> getSnapshot(
        transaction: Transaction,
        type: DocumentCompanion<Doc>
    ): Flow<DocumentWithHeader<Doc>> {
        val allEntries = store[type]?.values ?: ArrayList() // TODO we actually need a lock to read the values
        @Suppress("UNCHECKED_CAST")
        return flowOfValidDocuments(transaction, allEntries as Collection<List<DocumentWithHeader<Doc>>>)
    }

    private fun <Doc : Document> flowOfValidDocuments(
        transaction: Transaction,
        allEntries: Collection<List<DocumentWithHeader<Doc>>>
    ): Flow<DocumentWithHeader<Doc>> {
        return flow {
            for (documentVersions in allEntries) {
                for (i in documentVersions.indices.reversed()) {
                    val doc = documentVersions[i]
                    log.debug("Inspecting: $doc")
                    if (transaction.timestamp == doc.header.createdBy) {
                        if (doc.header.deleting) {
                            // Deleted in our transaction
                            break
                        }

                        emit(doc)
                        break
                    }

                    if (!doc.header.dirty) {
                        val existsInTransaction = transaction.timestamp >= doc.header.createdBy

                        if (existsInTransaction && doc.header.deleting) {
                            // Document with this ID has been deleted in our transaction
                            break
                        } else if (existsInTransaction) {
                            emit(doc)
                            break
                        }
                    }
                }
            }
        }
    }

    suspend fun initializeStore() {
        require(!didInit) { "Already initialized!" }
        didInit = true

        val transactions = HashMap<Long, ArrayList<Event>>()
        eventLog.readLog().forEach { event ->
            when (event) {
                is Event.OpenTransaction -> {
                    transactions[event.transactionId] = ArrayList()
                }

                is Event.Commit -> {
                    // TODO we need to commit these changes
                    transactions.remove(event.transactionId)
                }

                is Event.Rollback -> {
                    val events = transactions[event.transactionId]
                    if (events == null) {
                        log.warn("Found no events for transaction: ${event.transactionId}")
                    }
                }

                else -> {
                    val events = transactions[event.transactionId]
                    if (events == null) {
                        log.warn("Unknown transaction! ${event.transactionId}")
                    } else {
                        events.add(event)
                        applyEvent(event, null)
                    }
                }
            }
        }
    }

    suspend fun <Doc : Document> create(
        transaction: Transaction,
        companion: DocumentCompanion<Doc>,
        document: Doc
    ): DocumentId {
        val docId = UUID.randomUUID().toString()
        applyEvent(
            Event.Create(
                transaction.timestamp,
                DocumentWithHeader(
                    DocumentHeader(
                        docId,
                        transaction.timestamp,
                        -1,
                        deleting = false,
                        dirty = true
                    ),
                    document,
                    companion
                )
            ),
            transaction
        )
        return docId
    }

    suspend fun <Doc : Document> update(
        transaction: Transaction,
        oldDocument: DocumentWithHeader<Doc>,
        newDocument: Doc
    ) {
        val id = oldDocument.header.id
        val companion = oldDocument.companion

        applyEvent(
            Event.Update(
                transaction.timestamp,
                oldDocument,
                DocumentWithHeader(
                    DocumentHeader(id, transaction.timestamp, oldDocument.header.createdBy),
                    newDocument,
                    companion
                )
            ),
            transaction
        )
    }

    suspend fun <Doc : Document> delete(
        transaction: Transaction,
        documentWithHeader: DocumentWithHeader<Doc>
    ) {
        val id = documentWithHeader.header.id
        val document = documentWithHeader.document
        val companion = documentWithHeader.companion

        applyEvent(
            Event.Delete(
                transaction.timestamp,
                DocumentWithHeader(
                    DocumentHeader(id, transaction.timestamp, documentWithHeader.header.createdBy, deleting = true),
                    document,
                    companion
                )
            ),
            transaction
        )
    }

    suspend fun open(transaction: Transaction, user: String) {
        applyEvent(Event.OpenTransaction(transaction.timestamp, user), transaction)
    }

    suspend fun commit(transaction: Transaction) {
        val ourEntries = ArrayList<DocumentWithHeader<*>>()
        try {
            commitMutex.withLock {
                for (event in transaction.ownEventLog) {
                    when (event) {
                        is Event.Create<*> -> {
                            val doc = event.document
                            val id = doc.header.id
                            for (entry in store.getValue(doc.companion).getValue(id)) {
                                if (entry.header.createdBy == transaction.timestamp) {
                                    ourEntries.add(entry)
                                }
                            }
                        }

                        is Event.Update<*> -> {
                            checkIfWriteConflict(event.newDocument, transaction, ourEntries)
                        }

                        is Event.Delete<*> -> {
                            checkIfWriteConflict(event.document, transaction, ourEntries)
                        }
                    }
                }

                for (entry in ourEntries) {
                    entry.header.dirty = false
                }

                applyEvent(Event.Commit(transaction.timestamp), transaction)
            }
        } catch (ex: Throwable) {
            rollback(transaction)
            throw ex
        }
    }

    private fun checkIfWriteConflict(
        doc: DocumentWithHeader<*>,
        transaction: Transaction,
        ourEntries: ArrayList<DocumentWithHeader<*>>
    ) {
        val ourReadTimestamp = doc.header.previousVersion

        for (entry in store.getValue(doc.companion).getValue(doc.header.id)) {
            if (entry.header.createdBy == transaction.timestamp) {
                ourEntries.add(entry)
            } else if (entry.header.dirty) {
                require(entry.header.previousVersion != -1L) { "Duplicate ID generated (or invalid data in stream)" }

                if (ourReadTimestamp >= entry.header.previousVersion) {
                    // Someone else is working with a version as old or older than ours
                    throw RPCException(ResponseCode.TRY_AGAIN, "Write conflict. Try again.")
                }
            } else if (!entry.header.dirty) {
                if (transaction.timestamp < entry.header.createdBy) {
                    // Someone created an update we didn't know update
                    throw RPCException(ResponseCode.TRY_AGAIN, "Write conflict. Try again.")
                }
            }
        }
    }

    suspend fun rollback(transaction: Transaction) {
        eventLog.addEntry(Event.Rollback(transaction.timestamp))
    }

    private suspend fun applyEvent(event: Event, transaction: Transaction?) {
        transaction?.ownEventLog?.add(event)

        when (event) {
            is Event.Create<*> -> addToStore(event.document as DocumentWithHeader<*>)

            is Event.Update<*> -> addToStore(event.newDocument)

            is Event.Delete<*> -> {
                addToStore(event.document.copy(header = event.document.header.copy(deleting = true)))
            }

            is Event.Rollback, is Event.Commit, is Event.OpenTransaction -> {
                // Do nothing.
                // Rollback is really just a no-op (don't commit the data).
                // Commit will be handled when we call commit. When replaying the log we don't need to do anything.
                // OpenTransaction doesn't really matter when replaying.
            }
        }
        eventLog.addEntry(event)
    }

    companion object {
        private val log = Log("DocumentStore")
    }
}

class HashMapWithList<K, V>(
    private val delegate: HashMap<K, List<V>> = HashMap()
) : MutableMap<K, List<V>> by delegate {
    private val mutex = Mutex()

    suspend fun addEntry(key: K, value: V) {
        mutex.withLock {
            val existing = delegate[key] ?: emptyList()
            delegate[key] = existing + value
        }
    }

    suspend fun removeEntry(key: K, value: V) {
        mutex.withLock {
            val existing = delegate[key]
            if (existing != null) {
                val newList = existing.filter { it !== value }
                if (newList.isEmpty()) {
                    delegate.remove(key)
                } else {
                    delegate[key] = newList
                }
            }
        }
    }
}

data class DocumentWithHeader<Doc : Document>(
    val header: DocumentHeader,
    val document: Doc,
    val companion: DocumentCompanion<Doc>
)
