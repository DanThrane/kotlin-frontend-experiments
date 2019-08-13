import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.Node
import kotlin.browser.window
import kotlin.math.min
import kotlin.math.max

object Theme {
    val BACKGROUND = CSSVar("background")
    val BACKGROUND_P = Array(5) { CSSVar("background-p$it") }
    val BACKGROUND_N = Array(5) { CSSVar("background-N$it") }

    val ACTION = CSSVar("action")
    val ACTION_P = Array(5) { CSSVar("action-p$it") }
    val ACTION_N = Array(5) { CSSVar("action-n$it") }

    val FOREGROUND = CSSVar("foreground")
}

data class RGB(val r: Int, val g: Int, val b: Int) {
    companion object {
        fun create(value: Int): RGB {
            val r = value shr 16
            val g = (value shr 8) and (0x00FF)
            val b = (value) and (0x0000FF)
            return RGB(r, g, b)
        }

        fun create(value: String): RGB {
            return create(value.removePrefix("#").toInt(16))
        }
    }

    override fun toString(): String = "rgb($r, $g, $b)"
}

fun RGB.lighten(amount: Int): RGB {
    require(amount >= 0)
    return RGB(min(255, r + amount), min(255, g + amount), min(255, b + amount))
}

fun RGB.darken(amount: Int): RGB {
    require(amount >= 0)
    return RGB(max(0, r - amount), max(0, g - amount), max(0, b - amount))
}

val microTheme = css {
    val background = RGB.create(0x15202b)
    setVariable(Theme.BACKGROUND, "#15202b")
    Theme.BACKGROUND_P.forEachIndexed { idx, variable ->
        setVariable(variable, background.lighten((idx + 1) * 5))
    }

    Theme.BACKGROUND_N.forEachIndexed { idx, variable ->
        setVariable(variable, background.darken((idx + 1) * 5))
    }

    val action = RGB.create(0x1da1f2)
    setVariable(Theme.ACTION, action)
    Theme.ACTION_P.forEachIndexed { idx, variable ->
        setVariable(variable, action.lighten((idx + 1) * 5))
    }

    Theme.ACTION_N.forEachIndexed { idx, variable ->
        setVariable(variable, action.darken((idx + 1) * 5))
    }

    setVariable(Theme.FOREGROUND, "white")

    (matchAny()) {
        color = variable(Theme.FOREGROUND)
    }

    backgroundColor = variable(Theme.BACKGROUND)
    width = 100.percent
    height = 100.vh

    display = "flex"
    justifyContent = "center"

    listOf("input", "textarea").forEach { tag ->
        (byTag(tag)) {
            padding = 16.px
            borderRadius = 3.px
            backgroundColor = variable(Theme.BACKGROUND_P[2])
            border = "0"
            boxSizing = "border-box"
        }

        ((byTag(tag).withPseudoClass("hover")) or (byTag(tag).withPseudoClass("focus"))) {
            backgroundColor = variable(Theme.BACKGROUND_P[4])
        }
    }

    (byTag("button").withPseudoClass("hover")) {
        backgroundColor = variable(Theme.ACTION_P[2])
    }

    (byTag("input").withPseudoElement("placeholder")) {
        color = variable(Theme.FOREGROUND)
    }

    (byTag("button")) {
        borderRadius = 3.px
        padding = 10.px
        backgroundColor = variable(Theme.ACTION)
        color = variable(Theme.FOREGROUND)
        border = "0"
    }
}

private val mainContainer = css {
    width = 700.px
}

private val createPost = css {
    (byTag("textarea")) {
        width = 100.percent
        resize = "none"
        marginBottom = 8.px
    }
}

class MicroBlog {
    fun mount(node: Node) = with(node) {
        val root = Reference<HTMLDivElement>()
        div(A(classes = setOf(microTheme), ref = root)) {
            onDeinit {
                println("Running deinit handler")
            }

            div(A(classes = setOf(mainContainer))) {
                onDeinit {
                    println("Deinit 2")
                }

                div(A(classes = setOf(createPost))) {
                    onDeinit {
                        println("Deinit 3")
                    }

                    val charactersRemaining = BoundData(240)
                    val textArea = Reference<HTMLTextAreaElement>()

                    textarea(A(ref = textArea), rows = 3, maxLength = 240) {
                        on("input") {
                            charactersRemaining.currentValue = 240 - textArea.current.value.length
                        }
                    }

                    flex(A(classes = setOf(JustifyContent.spaceEvenly))) {
                        onDeinit {
                            println("Deinit 4")
                        }

                        boundText(charactersRemaining) { "$it characters remaining" }
                        button {
                            on("click") { e ->
                                e.preventDefault()
                                deleteNode(root.current)
                            }

                            text("Deinit Test")
                        }
                    }
                }

                div {
                    onDeinit {
                        println("Deinit 5")
                    }
                }

            }
        }

        div {
            onDeinit {
                println("I should not run")
            }

            text("I should not be removed")
        }
    }
}

class BoundData<T>(initialValue: T) {
    private val handlers = HashSet<(T) -> Unit>()

    var currentValue: T = initialValue
        set(value) {
            field = value
            handlers.forEach { it(value) }
        }

    fun addHandler(handler: (T) -> Unit) {
        handlers.add(handler)
        handler(currentValue)
    }

    fun removeHandler(handler: (T) -> Unit) {
        handlers.remove(handler)
    }
}

fun <Data> Node.boundText(
    data: BoundData<Data>,
    template: (Data) -> String
){
    val node = text("")
    data.addHandler { node.nodeValue = template(it) }
}