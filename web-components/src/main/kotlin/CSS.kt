import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSS
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty

fun NodeCursor<*>.css(style: String) {
    return baseElement<HTMLStyleElement>("style") {
        node.innerHTML = style
    }
}

fun NodeCursor<*>.css(builder: CSSBuilder.() -> Unit) {
    val cssBuilder = CSSBuilder().also(builder)
    val cssRuleBuilder = StringBuilder()
    cssBuilder.rules.forEach { (selector, props) ->
        cssRuleBuilder.append("$selector {\n")
        props.forEach { (k, v) -> cssRuleBuilder.append("  $k:$v;\n") }
        cssRuleBuilder.append("}\n")
    }
    return css(cssRuleBuilder.toString())
}

class CSSBuilder : CSSSelectorContext, CSSPropertyListBuilder() {
    val rules = ArrayList<Pair<String, Map<String, String>>>()

    operator fun CSSSelector.invoke(builder: CSSPropertyListBuilder.() -> Unit) {
        rules.add(textValue to CSSPropertyListBuilder().also(builder).properties)
    }
}

open class CSSPropertyListBuilder {
    val properties: MutableMap<String, String> = HashMap()

    fun add(property: String, value: String) {
        properties[property] = value
    }
}

class WriteOnlyProperty() : RuntimeException("Write only property")

var CSSPropertyListBuilder.textDecoration: String by CSSDelegate()
var CSSPropertyListBuilder.color: String by CSSDelegate()
var CSSPropertyListBuilder.transition: String by CSSDelegate()
var CSSPropertyListBuilder.position: String by CSSDelegate()
var CSSPropertyListBuilder.top: String by CSSDelegate()
var CSSPropertyListBuilder.bottom: String by CSSDelegate()
var CSSPropertyListBuilder.left: String by CSSDelegate()
var CSSPropertyListBuilder.right: String by CSSDelegate()
var CSSPropertyListBuilder.backgroundColor: String by CSSDelegate()
var CSSPropertyListBuilder.content: String by CSSDelegate()
var CSSPropertyListBuilder.opacity: String by CSSDelegate()
var CSSPropertyListBuilder.outline: String by CSSDelegate()
var CSSPropertyListBuilder.display: String by CSSDelegate()
var CSSPropertyListBuilder.padding: String by CSSDelegate()
var CSSPropertyListBuilder.paddingTop: String by CSSDelegate()
var CSSPropertyListBuilder.paddingBottom: String by CSSDelegate()
var CSSPropertyListBuilder.paddingLeft: String by CSSDelegate()
var CSSPropertyListBuilder.paddingRight: String by CSSDelegate()
var CSSPropertyListBuilder.cursor: String by CSSDelegate()
var CSSPropertyListBuilder.transform: String by CSSDelegate()
var CSSPropertyListBuilder.border: String by CSSDelegate()
var CSSPropertyListBuilder.borderRadius: String by CSSDelegate()
var CSSPropertyListBuilder.width: String by CSSDelegate()
var CSSPropertyListBuilder.height: String by CSSDelegate()
var CSSPropertyListBuilder.overflowX: String by CSSDelegate()
var CSSPropertyListBuilder.overflowY: String by CSSDelegate()
var CSSPropertyListBuilder.margin: String by CSSDelegate()
var CSSPropertyListBuilder.marginTop: String by CSSDelegate()
var CSSPropertyListBuilder.marginLeft: String by CSSDelegate()
var CSSPropertyListBuilder.marginRight: String by CSSDelegate()
var CSSPropertyListBuilder.marginBottom: String by CSSDelegate()
var CSSPropertyListBuilder.alignItems: String by CSSDelegate()
var CSSPropertyListBuilder.justifyContent: String by CSSDelegate()
var CSSPropertyListBuilder.justifyItems: String by CSSDelegate()
var CSSPropertyListBuilder.flexDirection: String by CSSDelegate()
var CSSPropertyListBuilder.flexWrap: String by CSSDelegate()
var CSSPropertyListBuilder.flexFlow: String by CSSDelegate()
var CSSPropertyListBuilder.flexGrow: String by CSSDelegate()
var CSSPropertyListBuilder.flexShrink: String by CSSDelegate()
var CSSPropertyListBuilder.flexBasis: String by CSSDelegate()
var CSSPropertyListBuilder.boxSizing: String by CSSDelegate()
var CSSPropertyListBuilder.resize: String by CSSDelegate()
var CSSPropertyListBuilder.fontSize: String by CSSDelegate()
var CSSPropertyListBuilder.fontWeight: String by CSSDelegate()
var CSSPropertyListBuilder.fontFamily: String by CSSDelegate()
var CSSPropertyListBuilder.listStyle: String by CSSDelegate()
var CSSPropertyListBuilder.maxWidth: String by CSSDelegate()
var CSSPropertyListBuilder.maxHeight: String by CSSDelegate()
var CSSPropertyListBuilder.minHeight: String by CSSDelegate()
var CSSPropertyListBuilder.minWidth: String by CSSDelegate()
var CSSPropertyListBuilder.borderCollapse: String by CSSDelegate()
var CSSPropertyListBuilder.borderSpacing: String by CSSDelegate()
var CSSPropertyListBuilder.textAlign: String by CSSDelegate()
var CSSPropertyListBuilder.boxShadow: String by CSSDelegate()
var CSSPropertyListBuilder.userSelect: String by CSSDelegate()
var CSSPropertyListBuilder.textTransform: String by CSSDelegate()
var CSSPropertyListBuilder.letterSpacing: String by CSSDelegate()
var CSSPropertyListBuilder.gridTemplateColumns: String by CSSDelegate()
var CSSPropertyListBuilder.gridGap: String by CSSDelegate()
var CSSPropertyListBuilder.zIndex: String by CSSDelegate()

class CSSDelegate(val name: String? = null) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        throw WriteOnlyProperty()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        (thisRef as CSSPropertyListBuilder).add(name ?: transformName(property.name), value)
    }

    private fun transformName(name: String): String {
        val builder = StringBuilder()
        for (c in name) {
            if (c.isUpperCase()) {
                builder.append("-")
                builder.append(c.toLowerCase())
            } else {
                builder.append(c)
            }
        }
        return builder.toString()
    }
}

fun Char.isUpperCase(): Boolean = toUpperCase() == this
fun Char.isLowerCase(): Boolean = toLowerCase() == this

data class CSSSelector(val textValue: String)

interface CSSSelectorContext

val CSSSelectorContext.host: CSSSelector
    get() = CSSSelector(":host")

val CSSSelectorContext.root: CSSSelector
    get() = CSSSelector(":root")

val CSSSelectorContext.empty: CSSSelector
    get() = CSSSelector("")

fun CSSSelectorContext.inHost(fn: CSSSelector.() -> CSSSelector): CSSSelector {
    return empty.fn().inHost()
}

fun CSSSelector.inHost() = CSSSelector(":host(${textValue.removePrefix(":host")})")
fun CSSSelectorContext.byTag(tagName: String) = CSSSelector(tagName)
fun CSSSelectorContext.byClass(className: String) = CSSSelector(".$className")
fun CSSSelectorContext.byId(idName: String) = CSSSelector("#$idName")
fun CSSSelectorContext.byNamespace(namespace: String) = CSSSelector("$namespace:|*")
fun CSSSelectorContext.matchAny() = CSSSelector("*")
fun CSSSelectorContext.withNoNamespace() = CSSSelector("|*")
fun CSSSelector.attributePresent(
    attributeName: String
): CSSSelector {
    val result = CSSSelector("$textValue[$attributeName]")
    if (textValue == ":host") {
        return result.inHost()
    }
    return result
}

fun CSSSelector.attributeEquals(
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
): CSSSelector {
    val result = CSSSelector("$textValue[$attributeName=$value${if (caseInsensitive) " i" else ""}]")
    if (textValue == ":host") return result.inHost()
    return result
}

fun CSSSelector.attributeListContains(
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
): CSSSelector {
    val result = CSSSelector("$textValue[$attributeName~=$value${if (caseInsensitive) " i" else ""}]")
    if (textValue == ":host") return result.inHost()
    return result
}

fun CSSSelector.attributeEqualsHyphen(
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
): CSSSelector {
    val result = CSSSelector("$textValue[$attributeName|=$value${if (caseInsensitive) " i" else ""}]")
    if (textValue == ":host") return result.inHost()
    return result
}

fun CSSSelector.attributeStartsWith(
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$textValue[$attributeName^=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelector.attributeEndsWith(
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
): CSSSelector {
    val result = CSSSelector("$textValue[$attributeName\$=$value${if (caseInsensitive) " i" else ""}]")
    if (textValue == ":host") return result.inHost()
    return result
}

fun CSSSelector.attributeContains(
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
): CSSSelector {
    val result = CSSSelector("$textValue[$attributeName*=$value${if (caseInsensitive) " i" else ""}]")
    if (textValue == ":host") return result.inHost()
    return result
}

fun CSSSelector.withPseudoClass(className: String) = CSSSelector("$textValue:$className")
fun CSSSelector.withPseudoElement(element: String) = CSSSelector("$textValue::$element")

infix fun CSSSelector.adjacentSibling(other: CSSSelector) = CSSSelector("$textValue + ${other.textValue}")
infix fun CSSSelector.anySibling(other: CSSSelector) = CSSSelector("$textValue ~ ${other.textValue}")
infix fun CSSSelector.directChild(other: CSSSelector) = CSSSelector("$textValue > ${other.textValue}")
infix fun CSSSelector.descendant(other: CSSSelector) = CSSSelector("$textValue ${other.textValue}")

infix fun CSSSelector.or(other: CSSSelector) = CSSSelector("$textValue, ${other.textValue}")
infix fun CSSSelector.and(other: CSSSelector) = CSSSelector("$textValue${other.textValue}")

val Int.pt get() = "${this}pt"
val Int.px get() = "${this}px"
val Int.vh get() = "${this}vh"
val Int.vw get() = "${this}vw"
val Int.em get() = "${this}px"
val Int.percent get() = "${this}%"

data class CSSVar(val name: String)

fun CSSPropertyListBuilder.variable(v: CSSVar, default: String? = null): String {
    if (default != null) {
        return "var(--${v.name}, $default)"
    } else {
        return "var(--${v.name})"
    }
}

fun CSSPropertyListBuilder.setVariable(v: CSSVar, value: String) {
    add("--${v.name}", value)
}

fun CSSPropertyListBuilder.setVariable(v: CSSVar, value: RGB) {
    setVariable(v, value.toString())
}

fun boxShadow(
    offsetX: Int,
    offsetY: Int,
    blurRadius: Int = 0,
    spreadRadius: Int = 0,
    color: String? = null
): String {
    return buildString {
        append("${offsetX}px ${offsetY}px ")
        append("${blurRadius}px ${spreadRadius}px")
        if (color != null) {
            append(" ")
            append(color)
        }
    }
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
