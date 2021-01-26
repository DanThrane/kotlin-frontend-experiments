package dk.thrane.playground

// https://github.com/angryziber/kotlin-webcomponents

import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event

typealias AttributeListener = (name: String, oldVald: String?, newVal: String?) -> Unit

abstract class CustomTag(val tag: String) {
    lateinit var element: HTMLElement
    protected val observedAttributes: Array<String> = emptyArray()
    private val mountListeners = ArrayList<() -> Unit>()
    private val unmountListeners = ArrayList<() -> Unit>()
    private val attributeListeners = ArrayList<AttributeListener>()

    fun onMount(listener: () -> Unit) {
        mountListeners.add(listener)
    }

    fun onUnmount(listener: () -> Unit) {
        unmountListeners.add(listener)
    }

    fun onAttributeChanged(listener: AttributeListener) {
        attributeListeners.add(listener)
    }

    @JsName("init")
    open fun init(element: HTMLElement) {
        this.element = element
    }

    @JsName("mounted")
    open fun mounted() {
        mountListeners.forEach { it() }
    }

    @JsName("unmounted")
    open fun unmounted() {
        unmountListeners.forEach { it() }
    }

    @JsName("attributeChanged")
    open fun attributeChanged(name: String, oldVal: String?, newVal: String) {
        attributeListeners.forEach { it(name, oldVal, newVal) }
    }

    fun observe(prop: String) {
        observedAttributes.asDynamic().push(prop)
    }

    companion object {
        private val wrapImpl = jsFunction("impl", jsCode = ES6_CLASS_ADAPTER) as (impl: CustomTag) -> () -> dynamic

        fun define(tag: CustomTag, tagName: String = tag.tag, extends: String? = undefined) {
            window.customElements.define(tagName, wrapImpl(tag), ElementDefinitionOptions(extends))
        }
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> CustomTag.attr(name: String): Attribute<T> = when (T::class) {
    Int::class -> IntAttribute(this, name) as Attribute<T>
    String::class -> TextAttribute(this, name) as Attribute<T>
    Boolean::class -> BooleanAttribute(this, name) as Attribute<T>
    else -> throw IllegalArgumentException("Unsupported attribute type: ${T::class}")
}

interface Attribute<T> {
    val name: String
    val tag: CustomTag
    var current: T?
}

fun <T> Attribute<T>.asState(): State<T?> {
    val state = stateOf(current)
    tag.onAttributeChanged { name, _, _ ->
        if (name == name) {
            state.current = current
        }
    }
    return state
}

class TextAttribute(
    override val tag: CustomTag,
    override val name: String
) : Attribute<String> {
    init {
        tag.observe(name)
    }

    override var current: String?
        get() = tag.element.attr(name)
        set(value) {
            if (value == null) {
                tag.element.removeAttribute(name)
            } else {
                tag.element.attr(name, value)
            }
        }
}

class IntAttribute(
    override val tag: CustomTag,
    override val name: String
) : Attribute<Int> {
    private val delegate = TextAttribute(tag, name)

    override var current: Int?
        get() = delegate.current?.toInt()
        set(value) {
            delegate.current = value?.toString()
        }
}

class BooleanAttribute(
    override val tag: CustomTag,
    override val name: String
) : Attribute<Boolean> {
    private val delegate = TextAttribute(tag, name)

    override var current: Boolean?
        get() = when (delegate.current) {
            null -> null
            "false" -> false
            else -> true
        }
        set(value) {
            delegate.current = value?.toString()
        }
}

abstract class TemplatedTag(tag: String, private val template: HTMLTemplateElement) : CustomTag(tag) {
    protected lateinit var shadow: ShadowRoot

    override fun init(element: HTMLElement) {
        super.init(element)
        shadow = element.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
        shadow.appendChild(template.content.cloneNode(true))
    }
}

@JsName("Function")
private external fun <T> jsFunction(vararg params: String, jsCode: String): T

// language=es6
private const val ES6_CLASS_ADAPTER = """return class extends HTMLElement {
    static get observedAttributes() {return impl.observedAttributes}
    constructor() {super(); this.inst = new impl.constructor(); this.inst.init(this)}
    connectedCallback() {this.inst.mounted()}
    disconnectedCallback() {this.inst.unmounted()}
    attributeChangedCallback(attrName, oldVal, newVal) {this.inst.attributeChanged(attrName, oldVal, newVal)}
}"""

fun <T : ParentNode> ParentNode.find(selector: String): T =
    querySelector(selector)?.unsafeCast<T>() ?: error("No such element: $selector")

fun <T : Element> Element.findAll(selector: String) = querySelectorAll(selector).asList().unsafeCast<List<T>>()
inline fun Element.attr(name: String) = getAttribute(name)
inline fun Element.attr(name: String, value: String) = setAttribute(name, value)


data class EventHandle(val element: CustomTag, val type: String)

fun EventHandle.listen(handler: (Event) -> Unit): (Event) -> Unit {
    element.element.addEventListener(type, handler)
    return handler
}

fun EventHandle.removeListener(handler: (Event) -> Unit): (Event) -> Unit {
    element.element.removeEventListener(type, handler)
    return handler
}

fun EventHandle.trigger() {
    element.element.dispatchEvent(Event(type))
}
