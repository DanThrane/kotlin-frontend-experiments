package dk.thrane.playground.database

import dk.thrane.playground.AttributeStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

// AST
sealed class Expression<E> {
    val attributes = AttributeStore()
}

class Call<E>(
    val procedureCall: String,
    val arguments: List<Expression<*>>
) : Expression<E>()

sealed class Program : Expression<Unit>() {
    abstract val variables: List<VariableReference<*>>
    abstract val body: Expression<*>
}

class Given(
    val condition: Expression<Boolean>,
    val then: Then,
    val otherwise: Otherwise?
) : Expression<Unit>()

class Then(
    override val variables: List<VariableReference<*>>,
    override val body: Expression<*>
) : Program()

class Otherwise(
    override val variables: List<VariableReference<*>>,
    override val body: Expression<*>
) : Program()

class Loop(
    val condition: Expression<Boolean>,
    override val variables: List<VariableReference<*>>,
    override val body: Expression<*>
) : Program()

class FieldAccess<Doc, Field>(
    val ref: Expression<Doc>,
    val field: KProperty1<Doc, Field>
) : Expression<Field>()

data class VariableReference<E>(
    val name: String,
    val type: String
) : Expression<E>()

class Assignment<E>(
    val ref: VariableReference<E>,
    val expression: Expression<E>
) : Expression<Unit>()

class FieldAssignment<E>(
    val access: FieldAccess<*, E>,
    val expression: Expression<E>
) : Expression<Unit>()

class Fetch<E>(
    override val variables: List<VariableReference<*>>,
    override val body: Expression<*>
) : Program()

class Yield<E>(val value: Expression<E>) : Expression<Unit>()

class Block(val expressions: List<Expression<*>>) : Expression<Unit>()

class Debug(val expression: Expression<*>) : Expression<Unit>()

sealed class Literal<E> : Expression<E>() {
    abstract val value: E
}

class IntLiteral(override val value: Int) : Literal<Int>()
class StringLiteral(override val value: String) : Literal<String>()
class DoubleLiteral(override val value: Double) : Literal<Double>()
class BooleanLiteral(override val value: Boolean) : Literal<Boolean>()
object NullLiteral : Literal<Nothing?>() {
    override val value: Nothing? = null
}

class Equals(val left: Expression<*>, val right: Expression<*>) : Expression<Boolean>()
class NotEquals(val left: Expression<*>, val right: Expression<*>) : Expression<Boolean>()
class GreaterThan(val left: Expression<*>, val right: Expression<*>) : Expression<Boolean>()
class LessThan(val left: Expression<*>, val right: Expression<*>) : Expression<Boolean>()
class GreaterThanEquals(val left: Expression<*>, val right: Expression<*>) : Expression<Boolean>()
class LessThanEquals(val left: Expression<*>, val right: Expression<*>) : Expression<Boolean>()

class Plus(val left: Expression<Int>, val right: Expression<Int>) : Expression<Int>()
class Minus(val left: Expression<Int>, val right: Expression<Int>) : Expression<Int>()
class Divide(val left: Expression<Int>, val right: Expression<Int>) : Expression<Int>()
class Multiply(val left: Expression<Int>, val right: Expression<Int>) : Expression<Int>()
class Mod(val left: Expression<Int>, val right: Expression<Int>) : Expression<Int>()

class And(val left: Expression<Boolean>, val right: Expression<Boolean>) : Expression<Boolean>()
class Or(val left: Expression<Boolean>, val right: Expression<Boolean>) : Expression<Boolean>()

// DSL
class RemoteTransaction

open class ProgramBuilder {
    val builderForVariables = ArrayList<VariableReference<*>>()
    val builderForBlock = ArrayList<Expression<*>>()

    infix fun <E> VariableReference<E>.assign(expr: Expression<E>) {
        builderForBlock.add(Assignment(this, expr))
    }

    infix fun VariableReference<Int>.assign(expr: Int) {
        builderForBlock.add(Assignment(this, IntLiteral(expr)))
    }

    infix fun <E> VariableReference<String>.assign(expr: String) {
        builderForBlock.add(Assignment(this, StringLiteral(expr)))
    }

    infix fun <E> VariableReference<Double>.assign(expr: Double) {
        builderForBlock.add(Assignment(this, DoubleLiteral(expr)))
    }

    infix fun <E> VariableReference<Boolean>.assign(expr: Boolean) {
        builderForBlock.add(Assignment(this, BooleanLiteral(expr)))
    }

    infix fun <E> FieldAccess<*, E>.assign(expr: Expression<E>) {
        builderForBlock.add(FieldAssignment(this, expr))
    }

    infix fun FieldAccess<*, Int>.assign(expr: Int) {
        builderForBlock.add(FieldAssignment(this, IntLiteral(expr)))
    }

    infix fun <E> FieldAccess<*, String>.assign(expr: String) {
        builderForBlock.add(FieldAssignment(this, StringLiteral(expr)))
    }

    infix fun <E> FieldAccess<*, Double>.assign(expr: Double) {
        builderForBlock.add(FieldAssignment(this, DoubleLiteral(expr)))
    }

    infix fun <E> FieldAccess<*, Boolean>.assign(expr: Boolean) {
        builderForBlock.add(FieldAssignment(this, BooleanLiteral(expr)))
    }
}

infix fun Expression<*>.equals(right: Expression<*>): Expression<Boolean> {
    return Equals(this, right)
}

infix fun Expression<*>.notEquals(right: Expression<*>): Expression<Boolean> {
    return NotEquals(this, right)
}

infix fun Expression<*>.greaterThan(right: Expression<*>): Expression<Boolean> {
    return GreaterThan(this, right)
}

infix fun Expression<*>.greaterThanEquals(right: Expression<*>): Expression<Boolean> {
    return GreaterThanEquals(this, right)
}

infix fun Expression<*>.lessThan(right: Expression<*>): Expression<Boolean> {
    return LessThan(this, right)
}

infix fun Expression<*>.lessThanEquals(right: Expression<*>): Expression<Boolean> {
    return LessThanEquals(this, right)
}

infix fun Expression<Int>.plus(right: Expression<Int>): Expression<Int> {
    return Plus(this, right)
}

infix fun Expression<Int>.minus(right: Expression<Int>): Expression<Int> {
    return Minus(this, right)
}

infix fun Expression<Int>.multiply(right: Expression<Int>): Expression<Int> {
    return Multiply(this, right)
}

infix fun Expression<Int>.divide(right: Expression<Int>): Expression<Int> {
    return Divide(this, right)
}

infix fun Expression<Int>.mod(right: Expression<Int>): Expression<Int> {
    return Mod(this, right)
}

infix fun Expression<Boolean>.and(right: Expression<Boolean>): Expression<Boolean> {
    return And(this, right)
}

infix fun Expression<Boolean>.or(right: Expression<Boolean>): Expression<Boolean> {
    return Or(this, right)
}

class FetchBuilder<E> : ProgramBuilder()

fun <E : Any> ProgramBuilder.variable(type: KClass<E>): ReadOnlyProperty<Any?, VariableReference<E>> {
    return object : ReadOnlyProperty<Any?, VariableReference<E>> {
        var didAdd = false
        var typeName = type.simpleName ?: throw IllegalArgumentException("Unknown type")

        override fun getValue(thisRef: Any?, property: KProperty<*>): VariableReference<E> {
            val ref = VariableReference<E>(property.name, typeName)

            if (!didAdd) {
                builderForVariables.add(ref)
                didAdd = true
            }

            return ref
        }
    }
}

fun ProgramBuilder.loop(condition: Expression<Boolean>, body: ProgramBuilder.() -> Unit) {
    val program = ProgramBuilder().also(body)
    builderForBlock.add(Loop(condition, program.builderForVariables, Block(program.builderForBlock)))
}

fun ProgramBuilder.given(
    condition: Expression<Boolean>,
    then: ProgramBuilder.() -> Unit,
    otherwise: (ProgramBuilder.() -> Unit)?
) {
    val thenProgram = ProgramBuilder().also(then)
    val otherwiseProgram = if (otherwise == null) null else ProgramBuilder().also(otherwise)
    builderForBlock.add(
        Given(
            condition,
            Then(thenProgram.builderForVariables, Block(thenProgram.builderForBlock)),
            if (otherwiseProgram == null) {
                null
            } else {
                Otherwise(otherwiseProgram.builderForVariables, Block(otherwiseProgram.builderForBlock))
            }
        )
    )
}

fun Int.literal() = IntLiteral(this)
fun String.literal() = StringLiteral(this)
fun Boolean.literal() = BooleanLiteral(this)
fun Double.literal() = DoubleLiteral(this)

fun <E> RemoteTransaction.fetch(block: FetchBuilder<E>.() -> Unit): Fetch<E> {
    val fetchBuilder = FetchBuilder<E>().also(block)
    return Fetch(fetchBuilder.builderForVariables, Block(fetchBuilder.builderForBlock))
}

fun ProgramBuilder.debug(expr: Expression<*>) {
    builderForBlock.add(Debug(expr))
}

fun <E> FetchBuilder<E>.yield(expr: Expression<E>) {
    builderForBlock.add(Yield(expr))
}

operator fun <Doc, Field> Expression<Doc>.get(field: KProperty1<Doc, Field>): FieldAccess<Doc, Field> {
    return FieldAccess(this, field)
}

// DSL -> AST -> Wire format -> AST -> Execution
// TODO Translate from AST to wire format
// TODO Translate from wire format to AST

/*
// Find by query
db.transaction {
    scan(TodoMessage)
        .filter { message: Expression<TodoMessage> ->
            message[TodoMessage::done] equals true
        }
        .fetch() // Sequence<TodoMessage>
}

// Set all entries as done if they fulfill some stupid requirement
db.transaction {
    scan(TodoMessage)
        .filter { message ->
            message[TodoMessage:message] contains "Fie"
        }
        .fetchAndUpdate { oldMessage: TodoMessage ->
            // Should this be an expression or should it do a round-trip?
            //   Easiest and more expressive:
            //     Do a round-trip, similar to how Hibernate and other ORMs would do it
            //   Hardest but more efficient:
            //     Don't do a round trip and copy in some generic way. This is how databases
            //     would do it.

            // I think we should give the choice and start by implementing the easiest one.

            oldMessage.copy(done = true)
        }
}

// Delete by query
db.transaction {
    scan(TodoMessage)
        .deleteWhere { message ->
            message[TodoMessage::done] equals true
        }
}

db.transaction {
    val a = scan(TodoMessage)
    val b = scan(TodoMessage)

    (a join b) // DocumentSequence<JoinedTables>
        .filter { t ->
            val isADone = t[a][TodoMessage::done]
            val isBDone = t[b][TodoMessage::done]

            isADone and isBDone
         }
         .fetch()
}

We can't really use normal control flow if we wish to send it to the backend for processing. In fact we should make
it very clear that this is not possible.

We could, however, build these from scratch.

db.transaction {
    val aSeq by open(TodoMessage)
    val bSeq by open(TodoMessage)

    fetch {
        loop((aSeq.peek() notEquals null) and (bSeq.peek() notEquals null)) {
            val a = aSeq.next()
            val b = bSeq.next()

            given((a[TodoMessage::message] contains "Fie") and b[TodoMessage::done]) then {
                yield(a) // Refers to the fetch scope
            }
        }
    }

    Program(
        variables = listOf(
            DocumentInput(TodoMessage, "aSeq"), // Generated automatically. Using var names for ease of understanding
            DocumentInput(TodoMessage, "bSeq")
        ),
        instructions = listOf(
            Fetch(
                "fetch1",
                Loop(
                    And(
                        NotEquals(
                            Peek("aSeq"),
                            Literal(null)
                        ),
                        NotEquals(
                            Peek("bSeq"),
                            Literal(null)
                        )
                    ),
                    variables = listOf(
                        Next("aSeq", "a"),
                        Next("bSeq", "b")
                    ),
                    instructions = listOf(
                        Given(
                            And(
                                TextContains(
                                    FieldReference("a", "message"),
                                    "Fie"
                                ),
                                FieldReference("b", "done")
                            ),
                            variables = emptyList(),
                            instructions = listOf(
                                Yield("a", fetch1")
                            )
                        )
                    )
                )
            )
        )
    )
}
 */
