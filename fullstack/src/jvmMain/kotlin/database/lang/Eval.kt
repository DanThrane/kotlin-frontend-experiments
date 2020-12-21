package dk.thrane.playground.database.lang

import dk.thrane.playground.database.*
import dk.thrane.playground.serialization.*

// TODO This might need to return something other than E (Objects are a problem)
@Suppress("UNCHECKED_CAST")
fun evaluate(node: Expression<*>): Any? {
    when (node) {
        is Call -> {
            val (header, fn) = FunctionRegistry.find(node.procedureCall)!!
            return fn(node, node.arguments.map { evaluate(it) })
        }

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
                    ObjectEndIndicator -> {
                        return null
                    }
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
                        ResolvedType.TUnknown -> result
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
                        ResolvedType.TUnknown -> scope[node.ref] = evaluate(node.expression) as Any
                        is ResolvedType.TObject -> scope[node.ref] = evaluate(node.expression) as Any
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
                is ResolvedType.TObject -> return null
            }

            return Unit
        }

        is Block -> {
            for (expr in node.expressions) {
                evaluate(expr)
            }
            return Unit
        }

        is Debug -> {
            val message = evaluate(node.expression)
            println(message)
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