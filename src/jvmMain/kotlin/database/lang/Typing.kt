package dk.thrane.playground.database.lang

import dk.thrane.playground.database.*
import kotlinx.serialization.*

var Expression<*>.resolvedType: ResolvedType by delegate()
var Program.runtimeScope: Scope by delegate()
var Program.scope: ScopeTemplate by delegate()
var FieldAccess<*, *>.isPartOfAssignment: Boolean by delegate()
var Expression<*>.parent: Expression<*>? by delegate()
var Expression<*>.parentScope: Program? by delegate()

sealed class ResolvedType(val typeTag: String) {
    object TUnknown : ResolvedType("Nothing")
    object TInt : ResolvedType("Int")
    object TString : ResolvedType("String")
    object TDouble : ResolvedType("Double")
    object TBoolean : ResolvedType("Boolean")
    object TUnit : ResolvedType("Unit")
    class TObject(
        val name: String,
        val fields: List<Pair<String, ResolvedType>>,
        val companion: DocumentCompanion<*>?,
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
                "Any", TUnknown.typeTag -> TUnknown
                else -> knownObjects[typeTag] ?: throw IllegalStateException("Unknown type! $typeTag")
            }
        }

        fun registerSystemType(descriptor: SerialDescriptor) {
            registerTypeInternal(null, descriptor)
        }

        fun <Doc : Document> registerType(companion: DocumentCompanion<Doc>) {
            registerTypeInternal(companion, companion.serializer.descriptor)
        }

        private fun registerTypeInternal(
            companion: DocumentCompanion<*>?,
            descriptor: SerialDescriptor
        ) {
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
                                TObject(simpleNameOfField, emptyList(), null, true)
                            }
                        }

                        PrimitiveKind.SHORT -> throw NotImplementedError()
                        PrimitiveKind.BYTE -> throw NotImplementedError()
                        PrimitiveKind.LONG -> TInt // TODO Not true
                        PrimitiveKind.FLOAT -> throw NotImplementedError()
                        StructureKind.LIST -> throw NotImplementedError()
                        StructureKind.MAP -> throw NotImplementedError()
                        UnionKind.ENUM_KIND -> throw NotImplementedError()
                        StructureKind.OBJECT -> {
                            if (desc.serialName == "kotlin.Unit") {
                                TUnit
                            } else {
                                throw IllegalStateException("Unsupported field type: ${desc.serialName}")
                            }
                        }

                        else -> {
                            throw IllegalStateException("Unsupported field type: ${desc.serialName}")
                        }
                    }

                    elementName to kind
                }
                .toMutableList()

            knownObjects[myName] = TObject(myName, fields, companion)

            mustVisit.forEach { dependency ->
                if (dependency.serialName.substringAfterLast('.').removeSuffix("?") != myName) {
                    registerTypeInternal(null, dependency)
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

        fun isAssignable(lhs: ResolvedType, rhs: ResolvedType): Boolean {
            if (lhs == TUnknown || rhs == TUnknown) return true
            return lhs == rhs
        }
    }
}

fun typeCheck(
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
            var opaqueCounter = 0
            val mapper = HashMap<VariableReference<*>, Int>()

            for (variable in node.variables) {
                typeCheck(variable, node, parentScope)

                when (variable.resolvedType) {
                    ResolvedType.TInt -> mapper[variable] = intCounter++
                    ResolvedType.TString -> mapper[variable] = stringCounter++
                    ResolvedType.TDouble -> mapper[variable] = doubleCounter++
                    ResolvedType.TBoolean -> mapper[variable] = booleanCounter++
                    is ResolvedType.TObject -> mapper[variable] = objectCounter++
                    ResolvedType.TUnknown -> mapper[variable] = opaqueCounter++
                    ResolvedType.TUnit -> throw IllegalStateException("Variables cannot be unit")
                }
            }

            node.resolvedType = ResolvedType.TUnit
            node.scope = ScopeTemplate(
                intCounter,
                stringCounter,
                booleanCounter,
                doubleCounter,
                objectCounter,
                opaqueCounter,
                mapper
            )
            node.runtimeScope = Scope(parentScope?.runtimeScope, node.scope)

            when (node) {
                is Loop -> {
                    typeCheck(node.condition, node, node)
                    if (node.condition.resolvedType != ResolvedType.TBoolean) {
                        throw IllegalStateException("condition is not boolean")
                    }
                }
                is Then, is Otherwise, is Fetch<*> -> {
                    // Do nothing
                }
            }

            typeCheck(node.body, node, node)
        }

        is Call -> {
            val (resolvedCallHeader, resolvedCall) = FunctionRegistry.find(node.procedureCall)
                ?: throw IllegalStateException("Unknown call")
            // We assume that all calls have processed their variable definitions

            if (node.arguments.size != resolvedCallHeader.arguments.size) {
                throw IllegalStateException("Bad number of arguments given")
            }

            for ((index, arg) in node.arguments.withIndex()) {
                typeCheck(arg, node, parentScope)
                val expectedType = resolvedCallHeader.arguments[index].resolvedType
                if (!ResolvedType.isAssignable(expectedType, arg.resolvedType)) {
                    throw IllegalStateException("Bad type of argument $index")
                }
            }

            node.resolvedType = resolvedCallHeader.returnType
        }

        is Fetch<*> -> {
            typeCheck(node.body, node, parentScope)
            node.resolvedType = ResolvedType.TUnit
        }

        is FieldAccess<*, *> -> {
            typeCheck(node.ref, node, parentScope)
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
            typeCheck(node.ref, node, parentScope)
            typeCheck(node.expression, node, parentScope)

            if (!ResolvedType.isAssignable(node.ref.resolvedType, node.expression.resolvedType)) {
                throw IllegalStateException("Bad type of assignment")
            }

            node.resolvedType = ResolvedType.TUnit
        }

        is FieldAssignment<*> -> {
            typeCheck(node.access, node, parentScope)
            typeCheck(node.expression, node, parentScope)

            if (node.access.resolvedType != node.expression.resolvedType) {
                throw IllegalStateException("Bad type of assignment")
            }

            node.resolvedType = ResolvedType.TUnit
        }

        is Block -> {
            for (expr in node.expressions) {
                typeCheck(expr, node, parentScope)
            }
            node.resolvedType = ResolvedType.TUnit
        }

        is Debug -> {
            typeCheck(node.expression, node, parentScope)
            node.resolvedType = ResolvedType.TUnit
        }

        is Literal -> {
            // Do nothing
            node.resolvedType = when (node) {
                is IntLiteral -> ResolvedType.TInt
                is StringLiteral -> ResolvedType.TString
                is DoubleLiteral -> ResolvedType.TDouble
                is BooleanLiteral -> ResolvedType.TBoolean
                NullLiteral -> throw IllegalStateException()
            }
        }

        is Equals -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            node.resolvedType = ResolvedType.TBoolean
        }

        is NotEquals -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            node.resolvedType = ResolvedType.TBoolean
        }

        is GreaterThan -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is LessThan -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is GreaterThanEquals -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is LessThanEquals -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            if (node.left.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType !in setOf(ResolvedType.TInt, ResolvedType.TDouble)) {
                throw IllegalStateException("lhs not number")
            }
            node.resolvedType = ResolvedType.TBoolean
        }

        is Plus -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
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
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
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
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
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
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
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
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            if (node.left.resolvedType != ResolvedType.TInt) {
                throw IllegalStateException("lhs not number")
            }
            if (node.right.resolvedType != ResolvedType.TInt) {
                throw IllegalStateException("lhs not number")
            }

            node.resolvedType = ResolvedType.TInt
        }

        is And -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)

            if (node.left.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("lhs not boolean")
            if (node.right.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("rhs not boolean")
            node.resolvedType = ResolvedType.TBoolean
        }

        is Or -> {
            typeCheck(node.left, node, parentScope)
            typeCheck(node.right, node, parentScope)
            if (node.left.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("lhs not boolean")
            if (node.right.resolvedType != ResolvedType.TBoolean) throw IllegalStateException("rhs not boolean")
            node.resolvedType = ResolvedType.TBoolean
        }

        is Given -> {
            typeCheck(node.condition, node, parentScope)
            typeCheck(node.then, node, parentScope)
            if (node.otherwise != null) {
                typeCheck(node.otherwise, node, parentScope)
            }

            if (node.condition.resolvedType != ResolvedType.TBoolean) {
                throw IllegalStateException("Condition is not boolean")
            }

            node.resolvedType = ResolvedType.TUnit
        }
    }
}
