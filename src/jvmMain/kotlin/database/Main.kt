package dk.thrane.playground.database

import kotlinx.serialization.Serializable

@Serializable
data class Todo(val id: String, val message: String, val done: Boolean) : Document {
    companion object : DocumentCompanion<Todo>(Todo::class) {
        override fun OperationContext<Unit, Todo>.verifyRead() {
            // OK
        }

        override fun OperationContext<Unit, Todo>.verifyCreate() {
            // OK
        }

        override fun OperationContext<Unit, Todo>.verifyUpdate(newDocument: Todo) {
            // OK
        }
    }
}

suspend fun main() {
    val db = Database().apply {
        registerType(Todo)
        initializeStore()
    }

    val user = DatabaseUser("user")
    db.useTransaction(user) { t ->
        db.create(t, Todo, Todo("first", "Testing", false))
    }

    println("Done")
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
