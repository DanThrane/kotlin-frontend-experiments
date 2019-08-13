import org.w3c.dom.HTMLStyleElement
import kotlin.browser.document
import kotlin.reflect.KProperty

object CSS {
    val styleReference = Reference<HTMLStyleElement>()
}

fun rawCSS(style: String) {
    if (CSS.styleReference.currentOrNull == null) {
        document.body!!.baseElement("style", A(ref = CSS.styleReference))
    }

    CSS.styleReference.currentOrNull!!.innerHTML += style
}

fun globalCSS(selector: String, vararg rules: Pair<String, String>) {
    globalCSS(selector, rules.toMap())
}

fun globalCSS(selector: String, rules: Map<String, String>) {
    val cssRuleBuilder = StringBuilder()
    cssRuleBuilder.append("$selector {\n")
    rules.forEach { (k, v) -> cssRuleBuilder.append("  $k:$v;\n") }
    cssRuleBuilder.append("}\n")
    rawCSS(cssRuleBuilder.toString())
}

private var cssNamespaceId = 0
fun css(builder: CSSBuilder.() -> Unit): String {
    val className = "c-${cssNamespaceId++}"
    val cssBuilder = CSSBuilder().also(builder)
    globalCSS(".$className", cssBuilder.properties)
    cssBuilder.rules.forEach { (selector, props) ->
        globalCSS(".$className $selector", props)
    }

    return className
}

class CSSBuilder : CSSSelectorContext, CSSPropertyListBuilder() {
    val rules = HashMap<String, Map<String, String>>()

    operator fun CSSSelector.invoke(builder: CSSPropertyListBuilder.() -> Unit) {
        rules[textValue] = CSSPropertyListBuilder().also(builder).properties
    }
}

open class CSSPropertyListBuilder {
    val properties: MutableMap<String, String> = HashMap()

    fun add(property: String, value: String) {
        properties[property] = value
    }
}

class WriteOnlyProperty() : RuntimeException("Write only property")

var CSSPropertyListBuilder.color: String by CSSDelegate()
var CSSPropertyListBuilder.backgroundColor: String by CSSDelegate()
var CSSPropertyListBuilder.display: String by CSSDelegate()
var CSSPropertyListBuilder.padding: String by CSSDelegate()
var CSSPropertyListBuilder.border: String by CSSDelegate()
var CSSPropertyListBuilder.borderRadius: String by CSSDelegate()
var CSSPropertyListBuilder.width: String by CSSDelegate()
var CSSPropertyListBuilder.height: String by CSSDelegate()
var CSSPropertyListBuilder.margin: String by CSSDelegate()
var CSSPropertyListBuilder.marginTop: String by CSSDelegate()
var CSSPropertyListBuilder.marginLeft: String by CSSDelegate()
var CSSPropertyListBuilder.marginRight: String by CSSDelegate()
var CSSPropertyListBuilder.marginBottom: String by CSSDelegate()
var CSSPropertyListBuilder.alignItems: String by CSSDelegate()
var CSSPropertyListBuilder.justifyContent: String by CSSDelegate()
var CSSPropertyListBuilder.justifyItems: String by CSSDelegate()
var CSSPropertyListBuilder.boxSizing: String by CSSDelegate()
var CSSPropertyListBuilder.resize: String by CSSDelegate()

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

fun CSSSelectorContext.byTag(tagName: String) = CSSSelector(tagName)
fun CSSSelectorContext.byClass(className: String) = CSSSelector(".$className")
fun CSSSelectorContext.byId(idName: String) = CSSSelector("#$idName")
fun CSSSelectorContext.byNamespace(namespace: String) = CSSSelector("$namespace:|*")
fun CSSSelectorContext.matchAny() = CSSSelector("*")
fun CSSSelectorContext.withNoNamespace() = CSSSelector("|*")
fun CSSSelectorContext.attributePresent(
    tagName: String,
    attributeName: String
) = CSSSelector("$tagName[$attributeName]")

fun CSSSelectorContext.attributeEquals(
    tagName: String,
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$tagName[$attributeName=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelectorContext.attributeListContains(
    tagName: String,
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$tagName[$attributeName~=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelectorContext.attributeEqualsHyphen(
    tagName: String,
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$tagName[$attributeName|=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelectorContext.attributeStartsWith(
    tagName: String,
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$tagName[$attributeName^=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelectorContext.attributeEndsWith(
    tagName: String,
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$tagName[$attributeName\$=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelectorContext.attributeContains(
    tagName: String,
    attributeName: String,
    value: String,
    caseInsensitive: Boolean = false
) = CSSSelector("$tagName[$attributeName*=$value${if (caseInsensitive) " i" else ""}]")

fun CSSSelector.withPseudoClass(className: String) = CSSSelector("$textValue:$className")
fun CSSSelector.withPseudoElement(element: String) = CSSSelector("$textValue::$element")

infix fun CSSSelector.adjacentSibling(other: CSSSelector) = CSSSelector("$textValue + ${other.textValue}")
infix fun CSSSelector.anySibling(other: CSSSelector) = CSSSelector("$textValue ~ ${other.textValue}")
infix fun CSSSelector.directChild(other: CSSSelector) = CSSSelector("$textValue > ${other.textValue}")
infix fun CSSSelector.descendant(other: CSSSelector) = CSSSelector("$textValue ${other.textValue}")

infix fun CSSSelector.or(other: CSSSelector) = CSSSelector("$textValue, ${other.textValue}")
infix fun CSSSelector.and(other: CSSSelector) = CSSSelector("$textValue${other.textValue}")

val Int.px get() = "${this}px"
val Int.vh get() = "${this}vh"
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
