import org.w3c.dom.*

fun Node.onDeinit(deinit: DeinitHook) {
    RegisteredHooks.attachHooks(deinit, this)
}

typealias DeinitHook = () -> Unit

object RegisteredHooks {
    private const val hookAttribute = "data-hook"
    private var counter: Int = 0

    private val attachedHooks = HashMap<Int, DeinitHook>()

    fun attachHooks(hook: DeinitHook, node: Node) {
        runCatching {
            val id = counter++
            val element = (node as Element)
            attachedHooks[id] = hook
            element.setAttribute(hookAttribute, id.toString())
        }
    }

    fun runHooksFor(node: Node, recurse: Boolean = true) {
        runCatching {
            val element = node as Element
            val hook = element.getAttribute(hookAttribute)
            if (recurse) {
                val children = element.querySelectorAll(":scope *[$hookAttribute]")
                for (idx in (children.length - 1) downTo 0) {
                    runHooksFor(children[idx]!!, recurse = false)
                }
            }

            if (hook != null) {
                attachedHooks.remove(hook.toInt())?.invoke()
            }
        }
    }
}

fun deleteNode(node: Node) {
    RegisteredHooks.runHooksFor(node)
    node.parentElement?.removeChild(node)
}
