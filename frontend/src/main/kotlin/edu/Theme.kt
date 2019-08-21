package edu

import ColorShades
import RGB

object Theme {
    val primary = ColorShades(RGB.create(0x673AB7))
    val secondary = ColorShades(RGB.create(0X03A9F4))
    val background = RGB.create(0xE5E5E5)
    val surface = RGB.create(0xFFFFFF)
    val error = RGB.create(0xB00020)

    val onPrimary = RGB.create(0xFFFFFF)
    val onSecondary = RGB.create(0xFFFFFF)
    val onBackground = RGB.create(0x000000)
    val onSurface = RGB.create(0x000000)
    val onError = RGB.create(0xFFFFFF)
}
