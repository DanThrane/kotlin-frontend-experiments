import kotlin.math.max
import kotlin.math.min

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

data class ColorShades(val base: RGB) {
    val c100 = base.darken(100)
    val c90 = base.darken(80)
    val c80 = base.darken(60)
    val c70 = base.darken(40)
    val c60 = base.darken(20)
    val c50 = base
    val c40 = base.lighten(20)
    val c30 = base.lighten(40)
    val c20 = base.lighten(60)
    val c10 = base.lighten(80)
    val c0 = base.lighten(100)
}