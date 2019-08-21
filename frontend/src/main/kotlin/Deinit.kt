import org.w3c.dom.*

fun Element.onDeinit(deinit: DeinitHook) {
    RegisteredHooks.attachHooks(deinit, this)
}

typealias DeinitHook = () -> Unit

object RegisteredHooks {
    private const val hookAttribute = "data-hook"
    private var counter: Int = 0

    private val attachedHooks = HashMap<Int, DeinitHook>()

    fun attachHooks(hook: DeinitHook, node: Element) {
        runCatching {
            val id = counter++
            attachedHooks[id] = hook
            node.setAttribute(hookAttribute, id.toString())
        }
    }

    fun runHooksFor(element: Element, recurse: Boolean = true) {
        runCatching {
            val hook = element.getAttribute(hookAttribute)
            if (recurse) {
                val children = element.querySelectorAll(":scope *[$hookAttribute]")
                for (idx in (children.length - 1) downTo 0) {
                    runHooksFor(children[idx] as Element, recurse = false)
                }
            }

            if (hook != null) {
                attachedHooks.remove(hook.toInt())?.invoke()
            }
        }
    }
}

fun deleteNode(node: Element) {
    RegisteredHooks.runHooksFor(node)
    node.parentElement?.removeChild(node)
}
