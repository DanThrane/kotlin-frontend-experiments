package dk.thrane.playground

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.w3c.dom.Element

private object ElementScope {
    private val log = Log(this::class.js.name)

    private const val DATA_ATTRIBUTE = "data-scope"
    private var counter = 0
    private val scopes = HashMap<Int, CoroutineScope>()

    fun createOrGetScope(element: Element): CoroutineScope {
        val id = element.getAttribute(DATA_ATTRIBUTE)?.toIntOrNull()

        return if (id == null) {
            val allocatedId = counter++
            val newScope = CoroutineScope(Job())
            element.onDeinit {
                newScope.cancel("Deleting node")
                scopes.remove(allocatedId)
            }
            scopes[allocatedId] = newScope
            element.setAttribute(DATA_ATTRIBUTE, "$allocatedId")

            newScope
        } else {
            scopes[id] ?: throw IllegalStateException("No scope exists for element: $element with id $id")
        }
    }
}

val Element.scope: CoroutineScope
    get() = ElementScope.createOrGetScope(this)
