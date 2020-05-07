package dk.thrane.playground.database

import dk.thrane.playground.AttributeKey
import dk.thrane.playground.serialization.*
import kotlinx.serialization.*
import kotlin.time.measureTime

@Serializable
data class TodoMessage(val message: String, val done: Boolean)

@Serializable
data class Nested(val next: NestedNext)

@Serializable
data class NestedNext(val value: Int)

fun main() {
    ResolvedType.registerType(TodoMessage.serializer().descriptor)
    ResolvedType.registerType(Nested.serializer().descriptor)

    val time = measureTime {
        with(RemoteTransaction()) {
            val program = fetch<TodoMessage> {
                val counter by variable(Int::class)
                val message by variable(Nested::class)
                counter assign 0

                //debug(message[Nested::next][NestedNext::value])
                message[Nested::next][NestedNext::value] assign 42
                //debug(message[Nested::next][NestedNext::value])

                loop(counter lessThan 10.literal()) {
                    counter assign (counter plus 1.literal())
                    debug(counter)
                }
            }

            process(program, null, null)
            repeat(1000) {
                evaluate(program.body)
            }
        }
    }

    println(time)

    val nativeTime = measureTime {
        repeat(1000) {
            var counter = 0
            var message = Nested(NestedNext(42))

            while (counter < 10) {
                counter++
                println(counter)
            }
        }
    }
    println(nativeTime)
}

data class CallDefinition(
    val name: String,
    val arguments: List<VariableReference<*>>,
    val returnType: ResolvedType
)

sealed class ResolvedType(val typeTag: String) {
    object TInt : ResolvedType("Int")
    object TString : ResolvedType("String")
    object TDouble : ResolvedType("Double")
    object TBoolean : ResolvedType("Boolean")
    object TUnit : ResolvedType("Unit")
    class TObject(
        val name: String,
        val fields: List<Pair<String, ResolvedType>>,
        internal val placeholder: Boolean = false
    ) : ResolvedType(name)

    companion object {
        private val knownObjects = HashMap<String, TObject>()

        fun fromTypeTag(typeTag: String): ResolvedType {
            return when (typeTag) {
                TInt.typeTag -> TInt
                TString.typeTag -> TString
                TDouble.typeTag -> TDouble
                TBoolean.typeTag -> TBoolean
                TUnit.typeTag -> TUnit
                else -> knownObjects[typeTag] ?: throw IllegalStateException("Unknown type! $typeTag")
            }
        }

        fun registerType(descriptor: SerialDescriptor) {
            val myName = descriptor.serialName.substringAfterLast('.').removeSuffix("?")
            if (descriptor.kind != StructureKind.CLASS) {
                throw IllegalArgumentException("Type to be registered must be a class")
            }

            val mustVisit = ArrayList<SerialDescriptor>()
            val fields = descriptor.elementDescriptors()
                .mapIndexed { idx, desc ->
                    val elementName = descriptor.getElementName(idx)
                    val kind = when (desc.kind) {
                        PrimitiveKind.BOOLEAN -> TBoolean
                        PrimitiveKind.INT -> TInt
                        PrimitiveKind.DOUBLE -> TDouble
                        PrimitiveKind.STRING -> TString
                        StructureKind.CLASS -> {
                            val simpleNameOfField = desc.serialName.substringAfterLast('.').removeSuffix("?")
                            knownObjects[simpleNameOfField] ?: run {
                                mustVisit.add(desc)
                                TObject(simpleNameOfField, emptyList(), true)
                            }
                        }

                        PrimitiveKind.SHORT -> TODO()
                        PrimitiveKind.BYTE -> TODO()
                        PrimitiveKind.LONG -> TODO()
                        PrimitiveKind.FLOAT -> TODO()
                        StructureKind.LIST -> TODO()
                        StructureKind.MAP -> TODO()
                        UnionKind.ENUM_KIND -> TODO()

                        else -> {
                            throw IllegalStateException("Unsupported field type: ${desc.serialName}")
                        }
                    }

                    elementName to kind
                }
                .toMutableList()

            knownObjects[myName] = TObject(myName, fields)

            mustVisit.forEach { dependency ->
                if (dependency.serialName.substringAfterLast('.').removeSuffix("?") != myName) {
                    registerType(dependency)
                }
            }

            for (i in fields.indices) {
                val (name, type) = fields[i]
                if (type is TObject && type.placeholder) {
                    fields[i] =
                        name to (knownObjects[type.name] ?: throw IllegalStateException("Could not resolve $type"))
                }
            }
        }
    }
}

private val typeAttribute = AttributeKey<ResolvedType>("type")
var Expression<*>.resolvedType: ResolvedType
    get() = attributes[typeAttribute]
    set(value) {
        attributes[typeAttribute] = value
    }

private val scopeAttribute = AttributeKey<Scope>("scope-runtime")
var Program.runtimeScope: Scope
    get() = attributes[scopeAttribute]
    set(value) {
        attributes[scopeAttribute] = value
    }

var Program.scope: ScopeTemplate
    get() = attributes[ScopeTemplate.attribute]
    set(value) {
        attributes[ScopeTemplate.attribute] = value
    }

private val isPartOfAssignmentAttribute = AttributeKey<Boolean>("is-part-of-assignment")
var FieldAccess<*, *>.isPartOfAssignment
    get() = attributes[isPartOfAssignmentAttribute]
    set(value) {
        attributes[isPartOfAssignmentAttribute] = value
    }

data class ScopeTemplate(
    val ints: Int,
    val strings: Int,
    val booleans: Int,
    val doubles: Int,
    val objects: Int,
    val mapper: HashMap<VariableReference<*>, Int>
) {
    companion object {
        val attribute = AttributeKey<ScopeTemplate>("scope-template")
    }
}

fun ResolvedType.TObject.createObject(): ObjectField {
    val fieldList = ArrayList<Field>()
    for ((_, type) in fields) {
        when (type) {
            ResolvedType.TInt -> fieldList.add(IntField(0))
            ResolvedType.TString -> fieldList.add(BinaryField(ByteArray(0)))
            ResolvedType.TDouble -> fieldList.add(DoubleField(0.0))
            ResolvedType.TBoolean -> fieldList.add(BooleanField(false))
            ResolvedType.TUnit -> throw IllegalStateException()
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
    private val mapper = template.mapper

    operator fun get(ref: VariableReference<*>): Any? {
        val mappedIndex = mapper[ref] ?: return null
        return when (ref.resolvedType) {
            ResolvedType.TInt -> ints[mappedIndex]
            ResolvedType.TString -> strings[mappedIndex]
            ResolvedType.TBoolean -> booleans[mappedIndex]
            ResolvedType.TDouble -> doubles[mappedIndex]
            ResolvedType.TUnit -> throw IllegalStateException("Variables cannot be of type Unit")
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

            is ResolvedType.TObject -> throw IllegalArgumentException("Cannot set object value")
        }
    }
}

private val parentAttribute = AttributeKey<Expression<*>?>("parent")
var Expression<*>.parent
    get() = attributes[parentAttribute]
    set(value) {
        attributes[parentAttribute] = value
    }

private val parentScopeAttribute = AttributeKey<Program?>("parentScope")
var Expression<*>.parentScope
    get() = attributes.getOrNull(parentScopeAttribute)
    set(value) {
        attributes[parentScopeAttribute] = value
    }

val callsByName: Map<String, CallDefinition> = emptyMap()
val calls: Map<String, (args: List<Any?>) -> Any?> = emptyMap()

fun process(
    node: Expression<*>,
    parent: Expression<*>?,
    parentScope: Program?
) {
    node.parent = parent
    node.parentScope = parentScope

    when (node) {
        is Program -> {
            var intCounter = 0
            var stringCounter = 0
            var booleanCounter = 0
            var doubleCounter = 0
            var objectCounter = 0
            val mapper = HashMap<VariableReference<*>, Int>()

            for (variable in node.variables) {
                process(variable, node, parentScope)

                when (variable.resolvedType) {
                    ResolvedType.TInt -> mapper[variable] = intCounter++
                    ResolvedType.TString -> mapper[variable] = stringCounter++
                    ResolvedType.TDouble -> mapper[variable] = doubleCounter++
                    ResolvedType.TBoolean -> mapper[variable] = booleanCounter++
                    is ResolvedType.TObject -> mapper[variable] = objectCounter++
                    ResolvedType.TUnit -> throw IllegalStateException("Variables cannot be unit")
                }
            }

            node.resolvedType = ResolvedType.TUnit
            node.scope = ScopeTemplate(intCounter, stringCounter, booleanCounter, doubleCounter, objectCounter, mapper)
            node.runtimeScope = Scope(parentScope?.runtimeScope, node.scope)

            when (node) {
                is Loop -> {
                    process(node.condition, node, node)
                    if (node.condition.resolvedType != ResolvedType.TBoolean) {
                        throw IllegalStateException("condition is not boolean")
                    }
                }
                is Then, is Otherwise, is Fetch<*> -> {
                    // Do nothing
                }
            }

            process(node.body, node, node)
        }

        is Call -> {
            val resolvedCall = callsByName[node.procedureCall] ?: throw IllegalStateException("Unknown call")
            // We assume that all calls have processed their variable definitions

            if (node.arguments.size != resolvedCall.arguments.size) {
                throw IllegalStateException("Bad number of arguments given")
            }

            for ((index, arg) in node.arguments.withIndex()) {
                process(arg, node, parentScope)
                val expectedType = resolvedCall.arguments[index].resolvedType
                if (expectedType != arg.resolvedType) {
                    throw IllegalStateException("Bad type of argument $index")
                }
            }

            node.resolvedType = resolvedCall.returnType
        }

        is Fetch<*> -> {
            process(node.body, node, parentScope)
            node.resolvedType = ResolvedType.TUnit
        }

        is FieldAccess<*, *> -> {
            process(node.ref, node, parentScope)
            val objType = node.ref.resolvedType as? ResolvedType.TObject
                ?: throw IllegalStateException("Cannot access fields of non-object type")

            val name = node.field.name
            val fieldToAccess =
                objType.fields.find { it.first == name } ?: throw IllegalStateException("Unknown field: $name")

            node.resolvedType = fieldToAccess.second
            node.isPartOfAssignment = parent is FieldAssignment<*>
        }

        is VariableReference -> {
            var currentScope: Program? = parentScope
            var found = false
            while (currentScope != null) {
                found = currentScope.variables.any { it.name == node.name }
                if (found) break
                currentScope = currentScope.parentScope
            }

            if (!found && parent is Program) {
                // Check if this is a variable declaration in the program node
                if (parent.variables.any { it.name == node.name }) {
                    found = true
                }
            }

            if (!found) {
                throw IllegalStateException("Unknown variable: $node")
            }

            node.resolvedType = ResolvedType.fromTypeTag(node.type)
        }

        is Assignment<*> -> {
            process(node.ref, node, parentScope)
            process(node.expression, node, parentScope)

            if (node.ref.resolvedType != node.expression.resolvedType) {
                throw IllegalStateException("Bad type of assignment")
            }

            node.resolvedType = ResolvedType.TUnit
        }

        is FieldAssignment<*> -> {
            process(node.access, node, parentScope)
            process(node.expression, node, parentScope)

            if (node.access.resolvedType != node.expression.resolvedType) {
                throw IllegalStateException("Bad type of assignment")
            }

            node.resolvedType = ResolvedType.TUnit
        }

        is Yield<*> -> {
            process(node.value, node, parentScope)
            node.resolvedType = ResolvedType.TUnit
        }

        is Block -> {
            for (expr in node.expressions) {
                process(expr, node, parentScope)
            }
            node.resolvedType = ResolvedType.TUnit
        }

        is Debug -> {
            process(node.expression, node, parentScope)
            node.resolvedType = ResolvedType.TUnit
        }

        is Literal -> {
            // Do nothing
            node.resolvedType = when (node) {
                is IntLiteral -> ResolvedType.TInt
                is StringLiteral -> ResolvedType.TString
                is DoubleLiteral -> ResolvedType.TDouble
                is BooleanLiteral -> ResolvedType.TBoolean
                NullLiteral -> TODO()
            }
        }

        is Equals -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            node.resolvedType = ResolvedType.TBoolean
        }

        is NotEquals -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            node.resolvedType = ResolvedType.TBoolean
        }

        is GreaterThan -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is LessThan -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is GreaterThanEquals -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is LessThanEquals -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is Plus -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("rhs not number")
            }

            node.resolvedType =
                if (node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble) {
                    ResolvedType.TDouble
                } else {
                    ResolvedType.TInt
                }
        }

        is Minus -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType =
                if (node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble) {
                    ResolvedType.TDouble
                } else {
                    ResolvedType.TInt
                }
        }

        is Divide -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType =
                if (node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble) {
                    ResolvedType.TDouble
                } else {
                    ResolvedType.TInt
                }
        }

        is Multiply -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType =
                if (node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble) {
                    ResolvedType.TDouble
                } else {
                    ResolvedType.TInt
                }

        }

        is Mod -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType != ResolvedType.TInt) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType != ResolvedType.TInt) {
                throw IllegalStateException("lhs not number")
            }

            node.resolvedType = ResolvedType.TInt
        }

        is And -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)

            if (node.left.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("lhs not boolean")
            if (node.right.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("rhs not boolean")
            node.resolvedType = ResolvedType.TBoolean
        }

        is Or -> {
            process(node.left, node, parentScope)
            process(node.right, node, parentScope)
            if (node.left.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("lhs not boolean")
            if (node.right.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("rhs not boolean")
            node.resolvedType = ResolvedType.TBoolean
        }

        is Given -> {
            process(node.condition, node, parentScope)
            process(node.then, node, parentScope)
            if (node.otherwise != null) {
                process(node.otherwise, node, parentScope)
            }

            if (node.condition.resolvedType != ResolvedType.TBoolean) {
                throw IllegalStateException("Condition is not boolean")
            }

            node.resolvedType = ResolvedType.TUnit
        }
    }
}

// TODO This might need to return something other than E (Objects are a problem)
@Suppress("UNCHECKED_CAST")
fun evaluate(node: Expression<*>): Any? {
    when (node) {
        is Call -> TODO()

        is Then -> {
            evaluate(node.body)
            return Unit
        }

        is Otherwise -> {
            evaluate(node.body)
            return Unit
        }

        is Loop -> {
            while (evaluate(node.condition) as Boolean) {
                evaluate(node.body)
            }
            return Unit
        }

        is Fetch<*> -> {
            evaluate(node.body)
            return Unit
        }

        is Given -> {
            if (evaluate(node.condition) as Boolean) {
                evaluate(node.then)
            }
            return Unit
        }

        is FieldAccess<*, *> -> {
            val evaluatedRef = evaluate(node.ref)
            val objField = if (evaluatedRef is ObjectField) {
                evaluatedRef
            } else if (evaluatedRef is Pair<*, *>) {
                (evaluatedRef.first as ObjectField).fields[evaluatedRef.second as Int] as ObjectField
            } else {
                throw IllegalStateException()
            }

            val type = node.ref.resolvedType
            require(type is ResolvedType.TObject)

            val idx = type.fields.indexOfFirst { it.first == node.field.name }
            if (idx == -1) throw IllegalStateException("Unknown field")

            if (node.isPartOfAssignment) {
                return Pair(objField, idx)
            } else {
                return when (val field = objField.fields[idx]) {
                    is ByteField -> field.value
                    is IntField -> field.value
                    is LongField -> field.value
                    is DoubleField -> field.value
                    is BinaryField -> field.value.decodeToString() // TODO Not always true
                    is BooleanField -> field.value
                    ObjectEndIndicator -> TODO()
                    NullField -> null
                    is ObjectField -> field
                }
            }
        }

        is VariableReference -> {
            var scope: Scope? = node.parentScope!!.runtimeScope
            while (scope != null) {
                val result = scope[node]

                if (result != null) {
                    return when (node.resolvedType) {
                        ResolvedType.TInt -> result as Int
                        ResolvedType.TString -> result as String
                        ResolvedType.TDouble -> result as Double
                        ResolvedType.TBoolean -> result as Boolean
                        ResolvedType.TUnit -> throw IllegalStateException("Variables cannot be Unit")
                        is ResolvedType.TObject -> result as ObjectField
                    }
                }

                scope = scope.parent
            }
            throw IllegalStateException("Variable not found in scope")
        }

        is Assignment<*> -> {
            var scope: Scope? = node.parentScope!!.runtimeScope
            while (scope != null) {
                val result = scope[node.ref]

                if (result != null) {
                    when (node.ref.resolvedType) {
                        ResolvedType.TInt -> scope[node.ref] = evaluate(node.expression) as Any
                        ResolvedType.TString -> scope[node.ref] = evaluate(node.expression) as Any
                        ResolvedType.TBoolean -> scope[node.ref] = evaluate(node.expression) as Any
                        ResolvedType.TDouble -> scope[node.ref] = evaluate(node.expression) as Any
                        ResolvedType.TUnit -> throw IllegalStateException("Cannot assign Unit")
                        is ResolvedType.TObject -> {
                            TODO()
                        }
                    }
                    return Unit
                }

                scope = scope.parent
            }

            throw IllegalStateException("Variable not found in scope")
        }

        is FieldAssignment<*> -> {
            val (fieldToAssign, idx) = evaluate(node.access) as Pair<ObjectField, Int>
            val evaluatedExpression = evaluate(node.expression)

            when (node.expression.resolvedType) {
                ResolvedType.TInt -> {
                    fieldToAssign.fields[idx] = IntField(evaluatedExpression as Int)
                }
                ResolvedType.TString -> {
                    fieldToAssign.fields[idx] = BinaryField((evaluatedExpression as String).encodeToByteArray())
                }
                ResolvedType.TDouble -> {
                    fieldToAssign.fields[idx] = DoubleField(evaluatedExpression as Double)
                }
                ResolvedType.TBoolean -> {
                    fieldToAssign.fields[idx] = BooleanField(evaluatedExpression as Boolean)
                }
                ResolvedType.TUnit -> throw IllegalStateException()
                is ResolvedType.TObject -> TODO()
            }

            return Unit
        }

        is Yield<*> -> TODO()

        is Block -> {
            for (expr in node.expressions) {
                evaluate(expr)
            }
            return Unit
        }

        is Debug -> {
            println(evaluate(node.expression))
            return Unit
        }

        is IntLiteral -> return node.value

        is StringLiteral -> return node.value

        is DoubleLiteral -> return node.value

        is BooleanLiteral -> return node.value

        NullLiteral -> return null

        is Equals -> {
            return (evaluate(node.left) == evaluate(node.right))
        }

        is NotEquals -> {
            return (evaluate(node.left) != evaluate(node.right))
        }

        is GreaterThan -> {
            return ((evaluate(node.left) as Comparable<Any?>) > (evaluate(node.right) as Comparable<Any?>))
        }

        is LessThan -> {
            return ((evaluate(node.left) as Comparable<Any?>) < (evaluate(node.right) as Comparable<Any?>))
        }

        is GreaterThanEquals -> {
            return ((evaluate(node.left) as Comparable<Any?>) >= (evaluate(node.right) as Comparable<Any?>))
        }

        is LessThanEquals -> {
            return ((evaluate(node.left) as Comparable<Any?>) <= (evaluate(node.right) as Comparable<Any?>))
        }

        is Plus -> {
            val lhs = evaluate(node.left) as Number
            val rhs = evaluate(node.right) as Number

            val shouldBeDouble =
                node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble

            if (shouldBeDouble) {
                return lhs.toDouble() + rhs.toDouble()
            } else {
                return lhs.toInt() + rhs.toInt()
            }
        }

        is Minus -> {
            val lhs = evaluate(node.left) as Number
            val rhs = evaluate(node.right) as Number

            val shouldBeDouble =
                node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble

            if (shouldBeDouble) {
                return lhs.toDouble() - rhs.toDouble()
            } else {
                return lhs.toInt() - rhs.toInt()
            }
        }

        is Divide -> {
            val lhs = evaluate(node.left) as Number
            val rhs = evaluate(node.right) as Number

            val shouldBeDouble =
                node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble

            if (shouldBeDouble) {
                return lhs.toDouble() / rhs.toDouble()
            } else {
                return lhs.toInt() / rhs.toInt()
            }
        }

        is Multiply -> {
            val lhs = evaluate(node.left) as Number
            val rhs = evaluate(node.right) as Number

            val shouldBeDouble =
                node.left.resolvedType == ResolvedType.TDouble || node.right.resolvedType == ResolvedType.TDouble

            if (shouldBeDouble) {
                return lhs.toDouble() * rhs.toDouble()
            } else {
                return lhs.toInt() * rhs.toInt()
            }
        }

        is Mod -> {
            return (evaluate(node.left) as Int % evaluate(node.right) as Int)
        }

        is And -> {
            return (evaluate(node.left) as Boolean && evaluate(node.right) as Boolean)
        }

        is Or -> {
            return (evaluate(node.left) as Boolean || evaluate(node.right) as Boolean)
        }
    }
}
