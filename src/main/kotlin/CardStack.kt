import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

data class CardInStack(
    val dependencies: List<BoundData<*>> = emptyList(),
    val predicate: () -> Boolean,
    val card: Element.() -> Unit
)

fun Element.cardStack(vararg cards: CardInStack, dependencies: List<BoundData<*>> = emptyList()) {
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

    val allDependencies = (cards.flatMap { it.dependencies } + dependencies).toSet().toList()
    val handlers = allDependencies.map { it.addHandler { selectCard() } }
    onDeinit {
        @Suppress("UNCHECKED_CAST")
        allDependencies.zip(handlers).forEach { (dep, handler) -> dep.removeHandler(handler as (Any?) -> Unit) }
    }

    ready = true
    createRoot()
    selectCard()
}