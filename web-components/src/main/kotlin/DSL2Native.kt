import org.w3c.dom.*

inline fun NodeCursor<*>.a(
    attrs: CommonAttributes<HTMLAnchorElement> = CommonAttributes(),
    href: String = "javascript:void(0)",
    children: (NodeCursor<HTMLAnchorElement>.() -> Unit) = {}
) {
    baseElement(
        "a",
        attrs.mergeWith(mapOf("href" to href)),
        children
    )
}

inline fun NodeCursor<*>.div(
    attrs: CommonAttributes<HTMLDivElement> = CommonAttributes(),
    children: (NodeCursor<HTMLDivElement>.() -> Unit) = {}
) {
    baseElement("div", attrs, children)
}

inline fun NodeCursor<*>.h1(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (NodeCursor<HTMLHeadingElement>.() -> Unit) = {}
) {
    baseElement("h1", attrs, children)
}

inline fun NodeCursor<*>.h2(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (NodeCursor<HTMLHeadingElement>.() -> Unit) = {}
) {
    baseElement("h2", attrs, children)
}

inline fun NodeCursor<*>.h3(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (NodeCursor<HTMLHeadingElement>.() -> Unit) = {}
) {
    baseElement("h3", attrs, children)
}

inline fun NodeCursor<*>.h4(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (NodeCursor<HTMLHeadingElement>.() -> Unit) = {}
) {
    baseElement("h4", attrs, children)
}

inline fun NodeCursor<*>.h5(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (NodeCursor<HTMLHeadingElement>.() -> Unit) = {}
) {
    baseElement("h5", attrs, children)
}

inline fun NodeCursor<*>.h6(
    attrs: CommonAttributes<HTMLHeadingElement> = CommonAttributes(),
    children: (NodeCursor<HTMLHeadingElement>.() -> Unit) = {}
) {
    baseElement("h6", attrs, children)
}

inline fun NodeCursor<*>.ul(
    attrs: CommonAttributes<HTMLUListElement> = CommonAttributes(),
    children: (NodeCursor<HTMLUListElement>.() -> Unit) = {}
) {
    baseElement("ul", attrs, children)
}

inline fun NodeCursor<*>.li(
    attrs: CommonAttributes<HTMLLIElement> = CommonAttributes(),
    children: (NodeCursor<HTMLLIElement>.() -> Unit) = {}
) {
    baseElement("li", attrs, children)
}

inline fun NodeCursor<*>.form(
    attrs: CommonAttributes<HTMLFormElement> = CommonAttributes(),
    children: (NodeCursor<HTMLFormElement>.() -> Unit) = {}
) {
    baseElement("form", attrs, children)
}

inline fun NodeCursor<*>.input(
    attrs: CommonAttributes<HTMLInputElement> = CommonAttributes(),
    placeholder: String? = null,
    type: String? = null,
    name: String? = null,
    required: Boolean? = null,
    children: (NodeCursor<HTMLInputElement>.() -> Unit) = {}
) {
    baseElement(
        "input",
        attrs.mergeWith(
            mapOf(
                "placeholder" to placeholder,
                "type" to type,
                "name" to name,
                "required" to required?.toString()
            )
        ),
        children
    )
}

inline fun NodeCursor<*>.label(
    attrs: CommonAttributes<HTMLLabelElement> = CommonAttributes(),
    children: (NodeCursor<HTMLLabelElement>.() -> Unit) = {}
) {
    baseElement("label", attrs, children)
}

inline fun NodeCursor<*>.br(
    attrs: CommonAttributes<HTMLFormElement> = CommonAttributes()
) {
    baseElement("br", attrs)
}

enum class WrapType {
    soft,
    hard
}

inline fun NodeCursor<*>.textarea(
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
    children: (NodeCursor<HTMLTextAreaElement>.() -> Unit) = {}
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

inline fun NodeCursor<*>.button(
    attrs: CommonAttributes<HTMLButtonElement> = CommonAttributes(),
    type: String? = "button",
    children: (NodeCursor<HTMLButtonElement>.() -> Unit) = {}
) {
    baseElement(
        "button",
        attrs.mergeWith(mapOf("type" to type)),
        children
    )
}

inline fun NodeCursor<*>.span(
    attrs: CommonAttributes<HTMLSpanElement> = CommonAttributes(),
    children: (NodeCursor<HTMLSpanElement>.() -> Unit) = {}
) {
    baseElement("span", attrs, children)
}

inline fun NodeCursor<*>.video(
    attrs: CommonAttributes<HTMLVideoElement> = CommonAttributes(),
    src: String? = null,
    autoplay: Boolean? = null,
    showControls: Boolean? = null,
    children: (NodeCursor<HTMLVideoElement>.() -> Unit) = {}
) {
    baseElement(
        "video",
        attrs.mergeWith(
            mapOf(
                "src" to src,
                "autoplay" to if (autoplay == true) "" else null,
                "controls" to showControls
            )
        ),
        children
    )
}

inline fun NodeCursor<*>.i(
    attrs: CommonAttributes<HTMLSpanElement> = CommonAttributes(),
    children: (NodeCursor<HTMLSpanElement>.() -> Unit) = {}
) {
    baseElement("i", attrs, children)
}

inline fun NodeCursor<*>.slot(
    attrs: CommonAttributes<HTMLSlotElement> = CommonAttributes(),
    name: String? = null,
    children: (NodeCursor<HTMLSlotElement>.() -> Unit) = {}
) {
    baseElement("slot", attrs.mergeWith(mapOf("name" to name)), children)
}

inline fun NodeCursor<*>.title(
    attrs: CommonAttributes<HTMLTitleElement> = CommonAttributes(),
    children: (NodeCursor<HTMLTitleElement>.() -> Unit) = {}
) {
    baseElement("title", attrs, children)
}