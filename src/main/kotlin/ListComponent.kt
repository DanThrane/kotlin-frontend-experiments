import org.w3c.dom.Node
import org.w3c.dom.get
import kotlin.dom.clear

class ListComponent<T>(
    private val node: Node,
    private val template: Node.(T) -> Unit
) {
    private val backingData = ArrayList<T>()

    val data: List<T>
        get() = backingData.toList()

    fun add(item: T) {
        backingData.add(item)
        node.template(item)
    }

    fun remove(item: T) {
        removeAt(backingData.indexOf(item))
    }

    fun removeAt(idx: Int) {
        if (idx !in backingData.indices) {
            throw IllegalArgumentException("Index out of bounds $idx !in 0..${backingData.size}")
        }

        backingData.removeAt(idx)
        node.removeChild(node.childNodes[idx]!!)
    }

    fun clear() {
        backingData.clear()
        node.clear()
    }

    fun setList(list: List<T>) {
        clear()
        list.forEach { add(it) }
    }
}

fun <T> Node.list(ref: Reference<ListComponent<T>>, template: Node.(T) -> Unit) {
    ref.currentOrNull = ListComponent(this, template)
}