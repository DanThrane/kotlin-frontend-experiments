package dk.thrane.playground.database

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.measureTime

@Serializable
data class Todo(val owner: String, val id: String, val message: String, val done: Boolean) : Document {
    companion object : DocumentCompanion<Todo>(Todo::class) {
        override val serializer = serializer()

        override fun OperationContext<Unit, Todo>.verifyRead() {
            require(docWithHeader.document.owner == transaction.user.id) { "Only the owner can read this message!" }
        }

        override fun OperationContext<Unit, Todo>.verifyCreate() {
            require(docWithHeader.document.owner == transaction.user.id) { "Only the owner can create this message!" }
        }

        override fun OperationContext<Unit, Todo>.verifyUpdate(newDocument: Todo) {
            require(docWithHeader.document.owner == transaction.user.id) { "Only the owner can update this message!" }
            require(newDocument.owner == transaction.user.id) { "Only the owner can update this message!" }
        }

        override fun OperationContext<Unit, Todo>.verifyDelete() {
            require(docWithHeader.document.owner == transaction.user.id) { "Only the owner can delete this message!" }
        }
    }
}

suspend fun main() {
    val wait = true
    val logFile = File("/home/dthrane/db.log")
    val db = Database(logFile).apply {
        registerType(Todo)
        initializeStore()
    }

    exitProcess(0)
    if (wait) {
        println("Waiting for signal!")
        Scanner(System.`in`).nextLine()
    }

    println("Ready!")
    val user = DatabaseUser("user")
    val time = measureTime {
        (0 until 1000).map { tId ->
            GlobalScope.launch {
                val myTime = measureTime {
                    db.useTransaction(user) { t ->
                        val createTime = measureTime {
                            repeat(1_000) {
                                db.create(t, Todo, Todo("user", "first-$tId-$it", "Testing", false))
                            }
                        }
                        println("$tId took $createTime in creation")
                    }
                }
                println("$tId took $myTime")
            }
        }.forEach { it.join() }
    }

    println("Done in $time")
    db.stop()
}

suspend inline fun Database.useTransaction(user: DatabaseUser, block: (Transaction) -> Unit) {
    val transaction = createTransaction(user)
    try {
        block(transaction)
        commit(transaction)
    } catch (ex: Throwable) {
        rollback(transaction)
        throw ex
    }
}
