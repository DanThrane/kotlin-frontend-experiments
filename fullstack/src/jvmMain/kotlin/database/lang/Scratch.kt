package dk.thrane.playground.database.lang

import dk.thrane.playground.database.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.UnitSerializer
import java.io.File
import java.time.LocalDateTime
import kotlin.time.measureTime

@Serializable
data class TodoMessage(val message: String, val done: Boolean) : Document {
    companion object : DocumentCompanion<TodoMessage>(TodoMessage::class) {
        override val serializer: KSerializer<TodoMessage> = serializer()

        override fun OperationContext<Unit, TodoMessage>.verifyRead() {
            println("Verifying read of ${docWithHeader}")
        }

        override fun OperationContext<Unit, TodoMessage>.verifyCreate() {

        }

        override fun OperationContext<Unit, TodoMessage>.verifyUpdate(newDocument: TodoMessage) {

        }

        override fun OperationContext<Unit, TodoMessage>.verifyDelete() {

        }
    }
}

suspend fun main() {
    initializeRuntime()

    val logFile = File("/home/dan/db.log")
    val db = Database(logFile).apply {
        // TODO Currently the type must be registered in two separate systems
        ResolvedType.registerType(TodoMessage)
        registerType(TodoMessage)
        ResolvedType.registerSystemType(DocWithHeader.serializer(UnitSerializer()).descriptor)

        initializeStore()
    }

    val time = measureTime {
        val program = fetch<TodoMessage> {
            val current by variable<DocWithHeader<TodoMessage>>()
            val flow by variable<Any>()
            flow assign call("open", TodoMessage.name.literal())


            loop(call("hasNext", flow)) {
                current assign call("next", flow)
                debug(current)
                debug(current[DocWithHeader<TodoMessage>::header][DocumentHeader::id])

                // TODO This doesn't work because the type-system doesn't know about generics.
                //  Instead it wrote unit since that is what we passed it via the serializer.
                //  It might be a bit hard to hack support for this into the language given we need to play ball
                //  with kx.serialization.
                //debug(current[DocWithHeader<TodoMessage>::doc][TodoMessage::done])
            }
        }

        typeCheck(program, null, null)

        db.useTransaction(DatabaseUser("foobar")) { t ->
            program.transaction = t
            program.db = db

            evaluate(program)
        }
    }

    println("test took $time")
    db.stop()
}

/**
 * DocumentWithHeader for use in scripts
 */
@Serializable
data class DocWithHeader<Doc : Document>(
    val header: DocumentHeader,
    val doc: Doc
)