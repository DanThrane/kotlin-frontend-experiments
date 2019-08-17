import org.w3c.dom.Node

class BoundData<T>(initialValue: T) {
    private val handlers = HashSet<(T) -> Unit>()

    var currentValue: T = initialValue
        set(value) {
            field = value
            handlers.forEach { it(value) }
        }

    fun addHandler(handler: (T) -> Unit) {
        handlers.add(handler)
        handler(currentValue)
    }

    fun removeHandler(handler: (T) -> Unit) {
        handlers.remove(handler)
    }
}

fun <Data> Node.boundText(
    data: BoundData<Data>,
    template: (Data) -> String
){
    val node = text("")
    data.addHandler { node.nodeValue = template(it) }
}