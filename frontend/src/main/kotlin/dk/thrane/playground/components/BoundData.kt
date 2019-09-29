package dk.thrane.playground.components

import dk.thrane.playground.text
import org.w3c.dom.Element

interface ImmutableBoundData<T> {
    val currentValue: T
    fun addHandler(handler: (T) -> Unit): (T) -> Unit
    fun removeHandler(handler: (T) -> Unit)
}

interface MutableBoundData<T> : ImmutableBoundData<T> {
    override var currentValue: T

    // Note: I really don't care about people purposefully modifying the value when it shouldn't be.
    fun asImmutable(): ImmutableBoundData<T> = this
}

class MultiBind(private val delegates: List<ImmutableBoundData<*>>) : ImmutableBoundData<Unit> {
    constructor(vararg delegates: ImmutableBoundData<*>) : this(listOf(*delegates))

    override val currentValue: Unit = Unit
    private val internalHandlers = HashSet<(Unit) -> Unit>()

    init {
        delegates.forEach { delegate ->
            delegate.addHandler {
                internalHandlers.forEach { handler ->
                    handler(Unit)
                }
            }
        }
    }

    override fun addHandler(handler: (Unit) -> Unit): (Unit) -> Unit {
        internalHandlers.add(handler)
        handler(Unit)
        return handler
    }

    override fun removeHandler(handler: (Unit) -> Unit) {
        internalHandlers.remove(handler)
    }
}

class BoundData<T>(initialValue: T) : MutableBoundData<T> {
    private val handlers = HashSet<(T) -> Unit>()

    override var currentValue: T = initialValue
        set(value) {
            field = value
            handlers.forEach { it(value) }
        }

    override fun addHandler(handler: (T) -> Unit): (T) -> Unit {
        handlers.add(handler)
        handler(currentValue)
        return handler
    }

    override fun removeHandler(handler: (T) -> Unit) {
        handlers.remove(handler)
    }
}

fun <Data> Element.boundText(
    data: ImmutableBoundData<Data>,
    template: (Data) -> String
) {
    val node = text("")
    data.addHandler { node.nodeValue = template(it) }
}

fun <Data> Element.boundClass(
    data: ImmutableBoundData<Data>,
    classes: (Data) -> Set<String>
) {
    val node = this
    val baseClasses = node.className
    val existingClasses = node.className
    data.addHandler { newData ->
        node.className = (classes(newData) + baseClasses).joinToString(" ") + " " + existingClasses
    }
}

fun <Data> Element.boundClassByPredicate(
    data: ImmutableBoundData<Data>,
    vararg classes: String,
    predicate: (Data) -> Boolean
) {
    val node = this
    val baseClasses = node.className
    data.addHandler { newData ->
        if (predicate(newData)) {
            node.className = (setOf(*classes) + baseClasses).joinToString(" ")
        } else {
            node.className = baseClasses
        }
    }
}

fun Element.boundClassByPredicate(
    data: ImmutableBoundData<Boolean>,
    vararg classes: String
) {
    boundClassByPredicate(data, classes = *classes) { it }
}
