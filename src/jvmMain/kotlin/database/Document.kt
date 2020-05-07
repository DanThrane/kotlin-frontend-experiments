package dk.thrane.playground.database

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

typealias DocumentId = String

@Serializable
data class DocumentHeader(
    val id: DocumentId,
    val createdBy: Long,
    val previousVersion: Long,
    val deleting: Boolean = false,
    var dirty: Boolean = true
)

interface Document

abstract class DocumentCompanion<Doc : Document>(val type: KClass<Doc>) {
    val name = type.simpleName ?: throw IllegalArgumentException("Documents must have a name!")
    abstract val serializer: KSerializer<Doc>

    private val internalOperations = ArrayList<Operation<*, Doc>>()
    val operations: List<Operation<*, Doc>> get() = internalOperations

    abstract fun OperationContext<Unit, Doc>.verifyRead()
    abstract fun OperationContext<Unit, Doc>.verifyCreate()
    abstract fun OperationContext<Unit, Doc>.verifyUpdate(newDocument: Doc)
    abstract fun OperationContext<Unit, Doc>.verifyDelete()

    fun <Request> operation(
        verify: OperationContext<Request, Doc>.() -> Unit,
        invoke: OperationContext<Request, Doc>.() -> Unit
    ) {
        internalOperations.add(object : Operation<Request, Doc> {
            override fun OperationContext<Request, Doc>.verify() = verify()
            override fun OperationContext<Request, Doc>.invoke() = invoke()
        })
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentCompanion<*>

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String = "DocumentCompanion($name)"
}

data class OperationContext<Request, Doc : Document>(
    val request: Request,
    val docWithHeader: DocumentWithHeader<Doc>,
    val transaction: Transaction
)

interface Operation<Request, Doc : Document> {
    fun OperationContext<Request, Doc>.verify()
    fun OperationContext<Request, Doc>.invoke()
}
