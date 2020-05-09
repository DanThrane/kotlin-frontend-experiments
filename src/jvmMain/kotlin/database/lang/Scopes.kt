package dk.thrane.playground.database.lang

import dk.thrane.playground.database.VariableReference
import dk.thrane.playground.serialization.*

data class ScopeTemplate(
    val ints: Int,
    val strings: Int,
    val booleans: Int,
    val doubles: Int,
    val objects: Int,
    val opaque: Int,
    val mapper: HashMap<VariableReference<*>, Int>
)

fun ResolvedType.TObject.createObject(): ObjectField {
    val fieldList = ArrayList<Field>()
    for ((_, type) in fields) {
        when (type) {
            ResolvedType.TInt -> fieldList.add(IntField(0))
            ResolvedType.TString -> fieldList.add(BinaryField(ByteArray(0)))
            ResolvedType.TDouble -> fieldList.add(DoubleField(0.0))
            ResolvedType.TBoolean -> fieldList.add(BooleanField(false))
            ResolvedType.TUnit -> {
                // Do nothing?
            }
            is ResolvedType.TObject -> fieldList.add(type.createObject())
        }
    }

    return ObjectField(fieldList)
}

class Scope(val parent: Scope?, template: ScopeTemplate) {
    val ints: IntArray = IntArray(template.ints)
    val strings: Array<String> = Array(template.strings) { "" }
    val booleans: BooleanArray = BooleanArray(template.booleans)
    val doubles: DoubleArray = DoubleArray(template.doubles)
    val objects: Array<ObjectField> = Array(template.objects) { idx ->
        val type = template.mapper.keys
            .filter { it.resolvedType is ResolvedType.TObject }
            .find { template.mapper[it] == idx }?.resolvedType as ResolvedType.TObject?
            ?: throw IllegalStateException()

        type.createObject()
    }
    val opaque: Array<Any> = Array(template.opaque) { Any() }
    private val mapper = template.mapper

    operator fun get(ref: VariableReference<*>): Any? {
        val mappedIndex = mapper[ref] ?: return null
        return when (ref.resolvedType) {
            ResolvedType.TInt -> ints[mappedIndex]
            ResolvedType.TString -> strings[mappedIndex]
            ResolvedType.TBoolean -> booleans[mappedIndex]
            ResolvedType.TDouble -> doubles[mappedIndex]
            ResolvedType.TUnit -> throw IllegalStateException("Variables cannot be of type Unit")
            ResolvedType.TUnknown -> opaque[mappedIndex]
            is ResolvedType.TObject -> objects[mappedIndex]
        }
    }

    operator fun set(ref: VariableReference<*>, value: Any) {
        val mappedIndex = mapper[ref] ?: throw IllegalArgumentException("Not in scope: $ref")
        return when (ref.resolvedType) {
            ResolvedType.TInt -> ints[mappedIndex] =
                (value as? Int) ?: throw IllegalArgumentException("Bad type of value")

            ResolvedType.TString -> strings[mappedIndex] =
                (value as? String) ?: throw IllegalArgumentException("Bad type of value")

            ResolvedType.TBoolean -> booleans[mappedIndex] =
                (value as? Boolean) ?: throw IllegalArgumentException("Bad type of value")

            ResolvedType.TDouble -> doubles[mappedIndex] =
                (value as? Double) ?: throw IllegalArgumentException("Bad type of value")

            ResolvedType.TUnit -> throw IllegalStateException("Cannot assign variables of type unit")

            ResolvedType.TUnknown -> {
                opaque[mappedIndex] = value
            }

            is ResolvedType.TObject -> objects[mappedIndex] = (value as? ObjectField)
                ?: throw IllegalArgumentException("Bad type of value")
        }
    }
}