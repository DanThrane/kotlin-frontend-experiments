import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.browser.document

data class Reference<T>(var currentOrNull: T? = null) {
    val current: T get() = currentOrNull!!
}

inline fun <T : Node> Node.baseElement(
    tag: String,
    attrs: CommonAttributes<T> = CommonAttributes(),
    children: T.() -> Unit = {}
) {
    with(attrs) {
        val element = document.createElement(tag)
        attributes.forEach { (name, value) ->
            element.setAttribute(name, value.toString())
        }

        if (classes.isNotEmpty()) {
            element.setAttribute("class", classes.joinToString(" "))
        }

        @Suppress("UNCHECKED_CAST")
        val t = element as T

        if (ref != null) ref.currentOrNull = t
        t.children()

        appendChild(element)
    }
}

fun Node.text(value: String): Text {
    val node = document.createTextNode(value)
    appendChild(node)
    return node
}

fun Node.on(eventName: String, eventHandler: (Event) -> Unit) {
    addEventListener(eventName, eventHandler)
}

inline fun Node.a(
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

inline fun Node.div(
    attrs: CommonAttributes<HTMLDivElement> = CommonAttributes(),
    children: (HTMLDivElement.() -> Unit) = {}
) {
    baseElement("div", attrs, children)
}

inline fun Node.ul(
    attrs: CommonAttributes<HTMLUListElement> = CommonAttributes(),
    children: (HTMLUListElement.() -> Unit) = {}
) {
    baseElement("ul", attrs, children)
}

inline fun Node.li(
    attrs: CommonAttributes<HTMLLIElement> = CommonAttributes(),
    children: (HTMLLIElement.() -> Unit) = {}
) {
    baseElement("li", attrs, children)
}

inline fun Node.form(
    attrs: CommonAttributes<HTMLFormElement> = CommonAttributes(),
    children: (HTMLFormElement.() -> Unit) = {}
) {
    baseElement("form", attrs, children)
}

inline fun Node.input(
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

inline fun Node.textarea(
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

inline fun Node.button(
    attrs: CommonAttributes<HTMLButtonElement> = CommonAttributes(),
    children: (HTMLButtonElement.() -> Unit) = {}
) {
    baseElement(
        "button",
        attrs,
        children
    )
}

typealias A<T> = CommonAttributes<T>

data class CommonAttributes<T : Node>(
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
