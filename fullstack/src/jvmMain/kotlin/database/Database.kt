package dk.thrane.playground.database

import dk.thrane.playground.Log
import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cliffc.high_scale_lib.NonBlockingHashMap
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

class Transaction internal constructor(
    val user: DatabaseUser,
    internal val timestamp: Long
) {
    internal val ownEventLog = ArrayList<Event>()
}

class Database(logFile: File) {
    private val timestamps = AtomicLong(0)

    private val eventLog = EventLog(logFile)
    private val store = HashMap<DocumentCompanion<*>, HashMapWithList<DocumentId, DocumentWithHeader<*>>>()
    private val nameToCompanion = HashMap<String, DocumentCompanion<*>>()
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
        nameToCompanion[companion.name] = companion
        eventLog.register(companion)
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
                    val events = transactions[event.transactionId] ?: run {
                        log.warn("Unknown transaction! ${event.transactionId}")
                        return@forEach
                    }

                    for (event in events) {
                        applyEvent(event, null)

                    }
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
                    }
                }
            }
        }

        eventLog.openForWriting()
    }

    fun stop() {
        eventLog.close()
    }

    private suspend fun <Doc : Document> addToStore(
        documentWithHeader: DocumentWithHeader<Doc>
    ) {
        val type = documentWithHeader.companion
        store.getValue(type).addEntry(documentWithHeader.header.id, documentWithHeader)
        // Notify indexes here
    }

    fun <Doc : Document> getSnapshot(
        transaction: Transaction,
        type: DocumentCompanion<Doc>
    ): Iterator<DocumentWithHeader<Doc>> {
        val allEntries = store[type]?.values ?: ArrayList() // TODO we actually need a lock to read the values
        @Suppress("UNCHECKED_CAST")
        return flowOfValidDocuments(transaction, allEntries as Collection<List<DocumentWithHeader<Doc>>>)
    }

    private fun <Doc : Document> flowOfValidDocuments(
        transaction: Transaction,
        allEntries: Collection<List<DocumentWithHeader<Doc>>>
    ): Iterator<DocumentWithHeader<Doc>> {
        return iterator {
            for (documentVersions in allEntries) {
                for (i in documentVersions.indices.reversed()) {
                    val doc = documentVersions[i]
                    log.debug("Inspecting: $doc")

                    if (transaction.timestamp == doc.header.createdBy) {
                        if (doc.header.deleting) {
                            // Deleted in our transaction
                            break
                        }

                        val canRead = with(OperationContext(Unit, doc, transaction)) {
                            with(doc.companion) {
                                runCatching { verifyRead() }.isSuccess
                            }
                        }

                        if (canRead) yield(doc)
                        break
                    }

                    if (!doc.header.dirty) {
                        val existsInTransaction = transaction.timestamp >= doc.header.createdBy

                        if (existsInTransaction && doc.header.deleting) {
                            // Document with this ID has been deleted in our transaction
                            break
                        } else if (existsInTransaction) {
                            val canRead = with(OperationContext(Unit, doc, transaction)) {
                                with(doc.companion) {
                                    runCatching { verifyRead() }.isSuccess
                                }
                            }

                            if (canRead) yield(doc)
                            break
                        }
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
        val docWithHeader = DocumentWithHeader(
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

        with(OperationContext(Unit, docWithHeader, transaction)) {
            with(companion) {
                verifyCreate()
            }
        }

        applyEvent(
            Event.Create(
                transaction.timestamp,
                docWithHeader
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

        with(OperationContext(Unit, oldDocument, transaction)) {
            with(companion) {
                verifyUpdate(newDocument)
            }
        }

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

        with(OperationContext(Unit, documentWithHeader, transaction)) {
            with(companion) {
                verifyDelete()
            }
        }

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
        when (event) {
            is Event.Create<*> -> {
                val doc = if (transaction == null) {
                    event.document.copy(header = event.document.header.copy(dirty = false))
                } else {
                    event.document
                }

                addToStore(doc)
            }

            is Event.Update<*> -> {
                val doc = if (transaction == null) {
                    event.newDocument.copy(header = event.newDocument.header.copy(dirty = false))
                } else {
                    event.newDocument
                }
                addToStore(doc)
            }

            is Event.Delete<*> -> {
                val baseDoc = event.document.copy(header = event.document.header.copy(deleting = true))
                val doc = if (transaction == null) {
                    baseDoc.copy(header = baseDoc.header.copy(dirty = false))
                } else {
                    baseDoc
                }

                addToStore(doc)
            }

            is Event.Rollback, is Event.Commit, is Event.OpenTransaction -> {
                // Do nothing.
                // Rollback is really just a no-op (don't commit the data).
                // Commit will be handled when we call commit. When replaying the log we don't need to do anything.
                // OpenTransaction doesn't really matter when replaying.
            }
        }

        if (transaction != null) {
            transaction.ownEventLog.add(event)
            eventLog.addEntry(event, event is Event.Commit)
        }
    }

    companion object {
        private val log = Log("DocumentStore")
    }
}

class HashMapWithList<K, V>(
    private val delegate: NonBlockingHashMap<K, List<V>> = NonBlockingHashMap()
) : MutableMap<K, List<V>> by delegate {
    // TODO This is probably really stupid
    private val mutexes = Array(128) { Mutex() }

    suspend fun addEntry(key: K, value: V) {
        val mutex = mutexes[key.hashCode().absoluteValue % mutexes.size]
        mutex.withLock {
            val existing = delegate[key] ?: emptyList()
            delegate[key] = existing + value
        }
    }

    suspend fun removeEntry(key: K, value: V) {
        val mutex = mutexes[key.hashCode().absoluteValue % mutexes.size]
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
