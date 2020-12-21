package dk.thrane.playground.database.lang

import dk.thrane.playground.database.*
import dk.thrane.playground.serialization.MessageFormat
import dk.thrane.playground.serialization.ObjectField
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer

var Program.transaction: Transaction by delegate()
var Program.db: Database by delegate()

data class CallDefinition(
    val name: String,
    val arguments: List<VariableReference<*>>,
    val returnType: ResolvedType
)

typealias FunctionCall = (callExpression: Call<*>, args: List<Any?>) -> Any?

object FunctionRegistry {
    private val headers: MutableMap<String, CallDefinition> = hashMapOf()
    private val bodies: MutableMap<String, FunctionCall> = hashMapOf()

    fun find(name: String): Pair<CallDefinition, FunctionCall>? {
        val header = headers[name]
        val body = bodies[name]
        if (header == null || body == null) return null
        return Pair(header, body)
    }

    fun insert(header: CallDefinition, body: FunctionCall) {
        header.arguments.forEach { it.resolvedType = ResolvedType.fromTypeTag(it.type) }
        headers[header.name] = header
        bodies[header.name] = body
    }
}

fun initializeRuntime() {
    FunctionRegistry.insert(
        CallDefinition(
            "yield",
            listOf(VariableReference<Nothing>("element", ResolvedType.TUnknown.typeTag)),
            ResolvedType.TUnit
        ),
        body = { _, (element) -> println("Yielding: $element") }
    )

    FunctionRegistry.insert(
        CallDefinition(
            "create",
            listOf(
                VariableReference<String>("type", ResolvedType.TString.typeTag),
                VariableReference<Nothing>("document", ResolvedType.TUnknown.typeTag)
            ),
            ResolvedType.TUnit
        ),
        body = { expr, (type, document) ->
            type as String
            document as ObjectField

            runBlocking {
                val transaction =
                    expr.parentScope!!.transaction // TODO inject into some global scope or something similar
                val db = expr.parentScope!!.db

                val resolvedType = ResolvedType.fromTypeTag(type) as ResolvedType.TObject
                @Suppress("UNCHECKED_CAST")
                val documentCompanion = resolvedType.companion!! as DocumentCompanion<Document>
                val doc = MessageFormat.default.load(documentCompanion.serializer, document)

                db.create(
                    transaction,
                    documentCompanion,
                    doc
                )
            }

            Unit
        }
    )

    FunctionRegistry.insert(
        CallDefinition(
            "double",
            listOf(VariableReference<Int>("int", ResolvedType.TInt.typeTag)),
            ResolvedType.TInt
        ),
        body = { _, (int) ->
            (int as Int) * 2
        }
    )

    FunctionRegistry.insert(
        CallDefinition(
            "open",
            listOf(
                VariableReference<String>("type", ResolvedType.TString.typeTag)
            ),
            ResolvedType.TUnknown
        ),
        body = { expr, (type) ->
            type as String
            val transaction =
                expr.parentScope!!.transaction // TODO inject into some global scope or something similar
            val db = expr.parentScope!!.db

            val resolvedType = ResolvedType.fromTypeTag(type) as ResolvedType.TObject
            @Suppress("UNCHECKED_CAST")
            val documentCompanion = resolvedType.companion!! as DocumentCompanion<Document>

            db.getSnapshot(transaction, documentCompanion)
        }
    )

    FunctionRegistry.insert(
        CallDefinition(
            "hasNext",
            listOf(
                VariableReference<Nothing>("iterator", ResolvedType.TUnknown.typeTag)
            ),
            ResolvedType.TBoolean
        ),
        body = { expr, (iterator) ->
            @Suppress("UNCHECKED_CAST")
            iterator as Iterator<DocumentWithHeader<*>>

            iterator.hasNext()
        }
    )

    val next: FunctionCall = { expr, (iterator) ->
        @Suppress("UNCHECKED_CAST")
        iterator as Iterator<DocumentWithHeader<*>>

        val nextEntry = iterator.next()

        @Suppress("UNCHECKED_CAST")
        val serializer = DocWithHeader.serializer(nextEntry.companion.serializer)
                as KSerializer<DocWithHeader<Document>>

        MessageFormat.default.dumpToField(
            serializer,
            DocWithHeader(nextEntry.header, nextEntry.document)
        )
    }

    FunctionRegistry.insert(
        CallDefinition(
            "next",
            listOf(
                VariableReference<Nothing>("iterator", ResolvedType.TUnknown.typeTag)
            ),
            ResolvedType.TUnknown
        ),
        next
    )
}