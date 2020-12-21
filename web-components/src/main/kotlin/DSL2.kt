import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.events.Event
import org.w3c.dom.get
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyMap
import kotlin.collections.emptySet
import kotlin.collections.filterNotNull
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toMutableMap

data class Reference<T>(var currentOrNull: T? = null) {
    val current: T get() = currentOrNull!!
}

data class NodeCursor<T : Node>(val node: T, var cursor: Int = node.childNodes.length)

fun <T : Node> T.toCursor() = NodeCursor(this)

inline fun <T : Node> NodeCursor<*>.baseElement(
    tag: String,
    attrs: CommonAttributes<T> = CommonAttributes(),
    children: NodeCursor<T>.() -> Unit = {}
) {
    with(attrs) {
        val element = document.createElement(tag)
        attributes.forEach { (name, value) ->
            element.setAttribute(name, value.toString())
        }

        if (classes.isNotEmpty() || klass != null) {
            element.setAttribute("class", (classes + klass).filterNotNull().joinToString(" "))
        }

        @Suppress("UNCHECKED_CAST")
        val t = element as T

        if (ref != null) ref.currentOrNull = t
        t.toCursor().children()

        add(element)
    }
}

fun NodeCursor<*>.add(element: Node) {
    if (cursor == node.childNodes.length) {
        console.log("Just appending")
        node.appendChild(element)
    } else {
        val child = node.childNodes[cursor]!!
        node.insertBefore(element, child)
    }

    cursor++
}

fun NodeCursor<*>.text(value: String): Text {
    val node = document.createTextNode(value)
    add(node)
    return node
}

fun Node.on(eventName: String, eventHandler: (Event) -> Unit): (Event) -> Unit {
    addEventListener(eventName, eventHandler)
    return eventHandler
}

fun NodeCursor<*>.on(eventName: String, eventHandler: (Event) -> Unit): (Event) -> Unit {
    node.addEventListener(eventName, eventHandler)
    return eventHandler
}

typealias A<T> = CommonAttributes<T>

data class CommonAttributes<T : Node>(
    val klass: String? = null,
    val classes: Set<String> = emptySet(),
    val attributes: Map<String, Any> = emptyMap(),
    val ref: Reference<T>? = null
) {
    fun mergeWith(additional: Map<String, Any?>): CommonAttributes<T> {
        return copy(attributes = mergeAttributes(additional))
    }

    private fun mergeAttributes(additionalAttributes: Map<String, Any?>): Map<String, Any> {
        val result = attributes.toMutableMap()
        additionalAttributes.forEach { (name, value) ->
            if (value != null) result[name] = value
        }
        return result
    }
}

fun <T : Node> CommonAttributes<T>.withClasses(vararg classes: String): CommonAttributes<T> {
    return copy(classes = this.classes + classes)
}

abstract external class CustomTagWrapper<T : CustomTag> : HTMLElement

val <T : CustomTag> CustomTagWrapper<T>.inst: T
    get() = asDynamic().inst as T

val <T : CustomTag> NodeCursor<CustomTagWrapper<T>>.inst: T
    get() = node.inst
