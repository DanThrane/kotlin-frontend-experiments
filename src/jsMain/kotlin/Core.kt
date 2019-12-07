package dk.thrane.playground

import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.browser.document

data class Reference<T>(var currentOrNull: T? = null) {
    val current: T get() = currentOrNull!!
}

inline fun <T : Element> Element.baseElement(
    tag: String,
    attrs: CommonAttributes<T> = CommonAttributes(),
    children: T.() -> Unit = {}
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
        t.children()

        appendChild(element)
    }
}

fun Element.text(value: String): Text {
    val node = document.createTextNode(value)
    appendChild(node)
    return node
}

fun Element.on(eventName: String, eventHandler: (Event) -> Unit) {
    addEventListener(eventName, eventHandler)
}

typealias A<T> = CommonAttributes<T>

data class CommonAttributes<T : Element>(
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

fun <T : Element> CommonAttributes<T>.withClasses(vararg classes: String): CommonAttributes<T> {
    return copy(classes = this.classes + classes)
}
