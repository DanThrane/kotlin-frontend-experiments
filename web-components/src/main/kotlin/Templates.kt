import kotlinx.browser.document
import org.w3c.dom.*

fun createTemplate(block: NodeCursor<DocumentFragment>.() -> Unit): HTMLTemplateElement {
    val dummyFragment = document.createDocumentFragment().also { it.toCursor().block() }
    val template = document.createElement("template") as HTMLTemplateElement

    for (i in 0 until dummyFragment.children.length) {
        template.innerHTML += dummyFragment.children[i]!!.outerHTML
    }

    return template
}

fun HTMLElement.withShadow(block: NodeCursor<ShadowRoot>.() -> Unit) {
    val shadow = attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
    shadow.toCursor().block()
}