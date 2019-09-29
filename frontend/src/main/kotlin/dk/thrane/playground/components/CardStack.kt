package dk.thrane.playground.components

import dk.thrane.playground.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

data class CardInStack(
    val predicate: () -> Boolean,
    val card: Element.() -> Unit
)

fun <T> Element.cardStack(dependency: ImmutableBoundData<T>, vararg cards: CardInStack) {
    var ready = false // Used to avoid running handler immediately for each dependency

    lateinit var root: Element
    fun createRoot(): Element {
        val ref = Reference<HTMLDivElement>()
        div(A(ref = ref))
        root = ref.current
        return root
    }

    fun selectCard() {
        if (!ready) return

        deleteNode(root)
        createRoot()

        val selectedCard = cards.find { it.predicate() }
        selectedCard?.run {
            root.card()
        }
    }

    val handler = dependency.addHandler { selectCard() }
    onDeinit { dependency.removeHandler(handler) }

    ready = true
    createRoot()
    selectCard()
}
