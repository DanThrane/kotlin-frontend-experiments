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

inline fun Element.a(
    attrs: CommonAttributes<HTMLAnchorElement> = CommonAttributes(),
    href: String = "javascript:void(0)",
    children: (HTMLAnchorElement.() -> Unit) = {}
) {
    baseElement(
        "a",
        attrs.mergeWith(mapOf("href" to href)),
        children
    )
}

inline fun Element.div(
    attrs: CommonAttributes<HTMLDivElement> = CommonAttributes(),
    children: (HTMLDivElement.() -> Unit) = {}
) {
    baseElement("div", attrs, children)
}

inline fun Element.h1(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (HTMLHeadingElement.() -> Unit) = {}
) {
    baseElement("h1", attrs, children)
}

inline fun Element.h2(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (HTMLHeadingElement.() -> Unit) = {}
) {
    baseElement("h2", attrs, children)
}

inline fun Element.h3(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (HTMLHeadingElement.() -> Unit) = {}
) {
    baseElement("h3", attrs, children)
}

inline fun Element.h4(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (HTMLHeadingElement.() -> Unit) = {}
) {
    baseElement("h4", attrs, children)
}

inline fun Element.h5(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (HTMLHeadingElement.() -> Unit) = {}
) {
    baseElement("h5", attrs, children)
}

inline fun Element.h6(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (HTMLHeadingElement.() -> Unit) = {}
) {
    baseElement("h6", attrs, children)
}

inline fun Element.ul(
    attrs: CommonAttributes<HTMLUListElement> = CommonAttributes(),
    children: (HTMLUListElement.() -> Unit) = {}
) {
    baseElement("ul", attrs, children)
}

inline fun Element.li(
    attrs: CommonAttributes<HTMLLIElement> = CommonAttributes(),
    children: (HTMLLIElement.() -> Unit) = {}
) {
    baseElement("li", attrs, children)
}

inline fun Element.form(
    attrs: CommonAttributes<HTMLFormElement> = CommonAttributes(),
    children: (HTMLFormElement.() -> Unit) = {}
) {
    baseElement("form", attrs, children)
}

inline fun Element.input(
    attrs: CommonAttributes<HTMLInputElement> = CommonAttributes(),
    placeholder: String? = null,
    type: String? = null,
    children: (HTMLInputElement.() -> Unit) = {}
) {
    baseElement(
        "input",
        attrs.mergeWith(
            mapOf(
                "placeholder" to placeholder,
                "type" to type
            )
        ),
        children
    )
}

enum class WrapType {
    soft,
    hard
}

inline fun Element.textarea(
    attrs: CommonAttributes<HTMLTextAreaElement> = CommonAttributes(),
    placeholder: String? = null,
    rows: Int? = null,
    cols: Int? = null,
    disabled: Boolean? = null,
    name: String? = null,
    required: Boolean? = null,
    wrap: WrapType? = null,
    readOnly: Boolean? = null,
    minLength: Int? = null,
    maxLength: Int? = null,
    autoFocus: Boolean? = null,
    autoComplete: Boolean? = null,
    spellcheck: Boolean? = null,
    children: (HTMLTextAreaElement.() -> Unit) = {}
) {
    baseElement(
        "textarea",
        attrs.mergeWith(
            mapOf(
                "placeholder" to placeholder,
                "rows" to rows?.toString(),
                "cols" to cols?.toString(),
                "disabled" to disabled?.toString(),
                "name" to name,
                "required" to required?.toString(),
                "wrap" to wrap?.name,
                "readonly" to readOnly?.toString(),
                "minlength" to minLength?.toString(),
                "maxlength" to maxLength?.toString(),
                "autofocus" to autoFocus?.toString(),
                "autocomplete" to autoComplete?.toString(),
                "spellcheck" to spellcheck?.toString()
            )
        ),
        children
    )
}

inline fun Element.button(
    attrs: CommonAttributes<HTMLButtonElement> = CommonAttributes(),
    type: String? = "button",
    children: (HTMLButtonElement.() -> Unit) = {}
) {
    baseElement(
        "button",
        attrs.mergeWith(mapOf("type" to type)),
        children
    )
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
