import org.w3c.dom.Element
import org.w3c.dom.get
import kotlin.dom.clear

class ListComponent<T>(
    private val node: Element,
    private val template: Element.(T) -> Unit
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
        deleteNode(node.childNodes[idx] as Element)
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

fun <T> Element.list(ref: Reference<ListComponent<T>>, template: Element.(T) -> Unit) {
    ref.currentOrNull = ListComponent(this, template)
}