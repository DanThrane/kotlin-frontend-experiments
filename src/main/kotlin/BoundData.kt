import org.w3c.dom.Element

class BoundData<T>(initialValue: T) {
    private val handlers = HashSet<(T) -> Unit>()

    var currentValue: T = initialValue
        set(value) {
            field = value
            handlers.forEach { it(value) }
        }

    fun addHandler(handler: (T) -> Unit): (T) -> Unit {
        handlers.add(handler)
        handler(currentValue)
        return handler
    }

    fun removeHandler(handler: (T) -> Unit) {
        handlers.remove(handler)
    }
}

fun <Data> Element.boundText(
    data: BoundData<Data>,
    template: (Data) -> String
) {
    val node = text("")
    data.addHandler { node.nodeValue = template(it) }
}

fun <Data> Element.boundClass(
    data: BoundData<Data>,
    baseClasses: Set<String> = emptySet(),
    classes: (Data) -> Set<String>
) {
    val node = this
    val existingClasses = node.className
    data.addHandler { newData ->
        node.className = (classes(newData) + baseClasses).joinToString(" ") + " " + existingClasses
    }
}

fun <Data> Element.boundClassByPredicate(
    data: BoundData<Data>,
    vararg classes: String,
    baseClasses: Set<String> = emptySet(),
    predicate: (Data) -> Boolean
) {
    val node = this
    data.addHandler { newData ->
        if (predicate(newData)) {
            node.className = (setOf(*classes) + baseClasses).joinToString(" ")
        } else {
            node.className = baseClasses.joinToString(" ")
        }
    }
}
