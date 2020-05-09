package dk.thrane.playground.database

import dk.thrane.playground.Log
import dk.thrane.playground.serialization.InputBuffer
import dk.thrane.playground.serialization.MessageFormat
import dk.thrane.playground.serialization.BorrowedSerializationOfMessage
import dk.thrane.playground.serialization.readInt
import dk.thrane.playground.stackTraceToString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.system.exitProcess
import kotlin.time.measureTime

internal sealed class Event {
    abstract val transactionId: Long

    data class Create<Doc : Document>(
        override val transactionId: Long,
        val document: DocumentWithHeader<Doc>
    ) : Event() {
        companion object {
            const val typeTag = "create"
        }
    }

    data class Update<Doc : Document>(
        override val transactionId: Long,
        val oldDocument: DocumentWithHeader<Doc>,
        val newDocument: DocumentWithHeader<Doc>
    ) : Event() {
        init {
            require(newDocument.header.id == oldDocument.header.id) { "IDs of the two documents do not match ($this)" }
        }

        companion object {
            const val typeTag = "update"
        }
    }

    data class Delete<Doc : Document>(
        override val transactionId: Long,
        val document: DocumentWithHeader<Doc>
    ) : Event() {
        companion object {
            const val typeTag = "delete"
        }
    }

    data class OpenTransaction(override val transactionId: Long, val user: String) : Event() {
        companion object {
            const val typeTag = "open_transaction"
        }
    }

    data class Commit(override val transactionId: Long) : Event() {
        companion object {
            const val typeTag = "commit"
        }
    }

    data class Rollback(override val transactionId: Long) : Event() {
        companion object {
            const val typeTag = "rollback"
        }
    }
}

internal class BorrowedSerializableEvent(
    val header: EventHeader,
    val doc1: BorrowedSerializationOfMessage?,
    val doc2: BorrowedSerializationOfMessage?
)

internal class SerializableEvent(
    val header: EventHeader,
    val doc1: ByteArray?,
    val doc2: ByteArray?
)

@Serializable
internal data class EventHeader(
    val type: String,
    val transactionId: Long,
    val docType: String?,
    val docHeader1: DocumentHeader?,
    val docHeader2: DocumentHeader?,
    val docSize1: Int,
    val docSize2: Int,
    val transactionOpenedBy: String?
) {
    init {
        require(docHeader1 == null || docSize1 >= 0)
        require(docHeader2 == null || docSize2 >= 0)
    }
}

private fun Event.toSerializable(registry: Map<String, DocumentCompanion<*>>): BorrowedSerializableEvent {
    return when (this) {
        is Event.Create<*> -> {
            @Suppress("UNCHECKED_CAST")
            val serializer = registry[document.companion.name]?.serializer as KSerializer<Document>?
                ?: throw IllegalArgumentException("Unknown document type!")

            val doc = MessageFormat.default.borrowSerialized(serializer, document.document)

            BorrowedSerializableEvent(
                EventHeader(
                    Event.Create.typeTag,
                    transactionId,
                    document.companion.name,
                    document.header,
                    null,
                    doc.size,
                    -1,
                    null
                ),
                doc,
                null
            )
        }

        is Event.Update<*> -> {
            @Suppress("UNCHECKED_CAST")
            val serializer = registry[oldDocument.companion.name]?.serializer as KSerializer<Document>?
                ?: throw IllegalArgumentException("Unknown document type!")

            val doc = MessageFormat.default.borrowSerialized(serializer, oldDocument.document)
            val doc2 = MessageFormat.default.borrowSerialized(serializer, newDocument.document)

            BorrowedSerializableEvent(
                EventHeader(
                    Event.Update.typeTag,
                    transactionId,
                    oldDocument.companion.name,
                    oldDocument.header,
                    newDocument.header,
                    doc.size,
                    doc2.size,
                    null
                ),
                doc,
                doc2
            )
        }

        is Event.Delete<*> -> {
            @Suppress("UNCHECKED_CAST")
            val serializer = registry[document.companion.name]?.serializer as KSerializer<Document>?
                ?: throw IllegalArgumentException("Unknown document type!")

            val doc = MessageFormat.default.borrowSerialized(serializer, document.document)

            BorrowedSerializableEvent(
                EventHeader(
                    Event.Delete.typeTag,
                    transactionId,
                    document.companion.name,
                    document.header,
                    null,
                    doc.size,
                    -1,
                    null
                ),
                doc,
                null
            )
        }
        is Event.OpenTransaction -> {
            BorrowedSerializableEvent(
                EventHeader(
                    Event.OpenTransaction.typeTag,
                    transactionId,
                    null,
                    null,
                    null,
                    -1,
                    -1,
                    user
                ),
                null,
                null
            )
        }

        is Event.Commit -> {
            BorrowedSerializableEvent(
                EventHeader(
                    Event.Commit.typeTag,
                    transactionId,
                    null,
                    null,
                    null,
                    -1,
                    -1,
                    null
                ),
                null,
                null
            )
        }

        is Event.Rollback -> {
            BorrowedSerializableEvent(
                EventHeader(
                    Event.Rollback.typeTag,
                    transactionId,
                    null,
                    null,
                    null,
                    -1,
                    -1,
                    null
                ),
                null,
                null
            )
        }
    }
}

private fun SerializableEvent.toEvent(registry: Map<String, DocumentCompanion<*>>): Event {
    return when (header.type) {
        Event.Create.typeTag -> {
            val companion =
                registry[header.docType] ?: throw IllegalStateException("Unknown doc type: ${header.docType}")

            @Suppress("UNCHECKED_CAST")
            Event.Create(
                header.transactionId,
                DocumentWithHeader(
                    header.docHeader1 ?: throw IllegalStateException("Missing docHeader1 in create"),
                    MessageFormat.default.load(
                        companion.serializer,
                        doc1 ?: throw IllegalStateException("Missing doc1 in create")
                    ),
                    companion as DocumentCompanion<Document>
                )
            )
        }
        Event.Update.typeTag -> {
            val companion =
                registry[header.docType] ?: throw IllegalStateException("Unknown doc type: ${header.docType}")

            @Suppress("UNCHECKED_CAST")
            Event.Update(
                header.transactionId,
                DocumentWithHeader(
                    header.docHeader1 ?: throw IllegalStateException("Missing docHeader1 in delete"),
                    MessageFormat.default.load(
                        companion.serializer,
                        doc1 ?: throw IllegalStateException("Missing doc1 in delete")
                    ),
                    companion as DocumentCompanion<Document>
                ),
                DocumentWithHeader(
                    header.docHeader2 ?: throw IllegalStateException("Missing docHeader2 in delete"),
                    MessageFormat.default.load(
                        companion.serializer,
                        doc2 ?: throw IllegalStateException("Missing doc2 in delete")
                    ),
                    companion
                )
            )
        }

        Event.Delete.typeTag -> {
            val companion =
                registry[header.docType] ?: throw IllegalStateException("Unknown doc type: ${header.docType}")

            @Suppress("UNCHECKED_CAST")
            Event.Delete(
                header.transactionId,
                DocumentWithHeader(
                    header.docHeader1 ?: throw IllegalStateException("Missing docHeader1 in delete"),
                    MessageFormat.default.load(
                        companion.serializer,
                        doc1 ?: throw IllegalStateException("Missing doc1 in delete")
                    ),
                    companion as DocumentCompanion<Document>
                )
            )
        }

        Event.Rollback.typeTag -> {
            Event.Rollback(header.transactionId)
        }

        Event.Commit.typeTag -> {
            Event.Commit(header.transactionId)
        }

        Event.OpenTransaction.typeTag -> {
            Event.OpenTransaction(
                header.transactionId,
                header.transactionOpenedBy ?: throw IllegalStateException("Missing user in openTransaction")
            )
        }

        else -> {
            throw IllegalArgumentException("Unknown event type: ${header.type}")
        }
    }
}

/**
 * Reads and writes the event log
 */
internal class EventLog(private val logFile: File) {
    private lateinit var outs: FileOutputStream
    private lateinit var bufferedOuts: BufferedOutputStream
    private val registry = HashMap<String, DocumentCompanion<*>>()
    private val executor = Executors.newSingleThreadExecutor()

    fun register(companion: DocumentCompanion<*>) {
        registry[companion.name] = companion
    }

    fun openForWriting() {
        outs = FileOutputStream(logFile, true)
        bufferedOuts = outs.buffered()
    }

    fun addEntry(event: Event, waitForSync: Boolean = false) {
        // TODO when do we really care about data reaching the disk?
        //  For now we will assume we don't ever care about data reaching disk before continuing.

        // TODO Technically this could crash before being submitted (in case of too large a message)
        val serializable = event.toSerializable(registry)
        val message = MessageFormat.default.borrowSerialized(EventHeader.serializer(), serializable.header)

        val future = executor.submit {
            try {
                val size = message.size
                val buf = bufferedOuts
                buf.write(size shr (24) and 0xFF)
                buf.write(size shr (16) and 0xFF)
                buf.write(size shr (8) and 0xFF)
                buf.write(size shr (0) and 0xFF)
                buf.write(message.borrowedBytes, 0, message.size)

                if (serializable.doc1 != null) {
                    buf.write(serializable.doc1.borrowedBytes, 0, serializable.doc1.size)
                }

                if (serializable.doc2 != null) {
                    buf.write(serializable.doc2.borrowedBytes, 0, serializable.doc2.size)
                }

                if (waitForSync) {
                    buf.flush()
                    outs.fd.sync()
                }

                Unit
            } finally {
                message.release()
                serializable.doc1?.release()
                serializable.doc2?.release()
            }
        }

        if (waitForSync) future.get()
    }

    fun readLog(): Sequence<Event> {
        return sequence {
            if (!logFile.exists()) return@sequence

            val allEntries = ArrayList<Event>()
            var read = 0
            FileInputStream(logFile).buffered().use { ins ->
                val sizeBuffer = InputBuffer(ByteArray(4))
                while (true) {
                    var bytesRead: Int
                    try {
                        sizeBuffer.ptr = 0
                        bytesRead = ins.readNBytes(sizeBuffer.array, 0, 4)
                        if (bytesRead < 4) break
                        read += 4

                        val messageSize = sizeBuffer.readInt()
                        val messageBuffer = ByteArray(messageSize)
                        bytesRead = ins.readNBytes(messageBuffer, 0, messageSize)
                        if (bytesRead < messageSize) throw IllegalStateException("Corrupt file (event data missing)")
                        read += messageSize + 4

                        val eventHeader = MessageFormat.default.load(EventHeader.serializer(), messageBuffer)
                        val doc1 = if (eventHeader.docSize1 > 0) {
                            val docBuffer = ByteArray(eventHeader.docSize1)
                            bytesRead = ins.readNBytes(docBuffer, 0, docBuffer.size)
                            if (bytesRead < docBuffer.size) {
                                throw IllegalStateException("Unexpected EOF")
                            }
                            read += bytesRead
                            docBuffer
                        } else {
                            null
                        }

                        val doc2 = if (eventHeader.docSize2 > 0) {
                            val docBuffer = ByteArray(eventHeader.docSize1)
                            bytesRead = ins.readNBytes(docBuffer, 0, docBuffer.size)
                            if (bytesRead < docBuffer.size) {
                                throw IllegalStateException("Unexpected EOF")
                            }
                            read += bytesRead
                            docBuffer
                        } else {
                            null
                        }

                        val e = SerializableEvent(eventHeader, doc1, doc2).toEvent(registry)
                        yield(e)
                        allEntries.add(e)
                    } catch (ex: Throwable) {
                        // TODO This is not good enough. We need to write a checkpoint or something to recover
                        //  this file.
                        log.warn("Log is corrupt")
                        log.warn(ex.stackTraceToString())
                        log.warn("No more entries from the log will be processed")
                        break
                    }
                }
            }
        }
    }

    fun close() {
        executor.shutdown()
        bufferedOuts.close()
    }

    companion object {
        private val log = Log("EventLog")
    }
}
