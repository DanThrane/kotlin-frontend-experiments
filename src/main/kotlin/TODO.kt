import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.get
import kotlin.dom.clear

val todoCSS = css {
    (byTag("input")) {
        border = "0"
        padding = 10.px
        backgroundColor = "pink"
    }
}

fun Node.todoApp() {
    val newItemInput = Reference<HTMLInputElement>()
    val listEl = Reference<ListComponent<String>>()

    div(A(classes = setOf(todoCSS))) {
        div {
            list(listEl) { item ->
                flex {
                    text(item)
                    a {
                        text("Remove")
                        on("click") {
                            listEl.current.remove(item)
                        }
                    }
                }
            }
        }

        form {
            input(A(ref = newItemInput), placeholder = "New Item 22")

            on("submit") {
                it.preventDefault()
                println(newItemInput.current.value)

                listEl.current.add(newItemInput.current.value)
                newItemInput.current.value = ""
            }
        }

        a {
            text("Clear")
            on("click") {
                listEl.current.clear()
            }
        }

        a {
            text("Data plz")
            on("click") {
                println(listEl.current.data)
            }
        }
    }
}

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

