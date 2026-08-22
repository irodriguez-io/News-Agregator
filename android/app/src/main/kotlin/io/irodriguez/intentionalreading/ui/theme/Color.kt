package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal data class Oklch(
    val lightness: Double,
    val chroma: Double,
    val hue: Double,
)

internal data class AuthoredColor(
    val color: Color,
    val oklch: Oklch,
)

internal fun mixOklch(
    first: AuthoredColor,
    firstFraction: Double,
    second: AuthoredColor,
): Color {
    require(firstFraction in 0.0..1.0)
    val secondFraction = 1.0 - firstFraction
    val hueDelta = ((second.oklch.hue - first.oklch.hue + 540.0) % 360.0) - 180.0
    return Oklch(
        lightness = first.oklch.lightness * firstFraction + second.oklch.lightness * secondFraction,
        chroma = first.oklch.chroma * firstFraction + second.oklch.chroma * secondFraction,
        hue = (first.oklch.hue + hueDelta * secondFraction + 360.0) % 360.0,
    ).toSrgbColor()
}

private fun Oklch.toSrgbColor(): Color {
    val hueRadians = hue * PI / 180.0
    val a = chroma * cos(hueRadians)
    val b = chroma * sin(hueRadians)

    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)

    val red = linearToSrgb(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s)
    val green = linearToSrgb(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s)
    val blue = linearToSrgb(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)
    return Color(
        red = (red.coerceIn(0.0, 1.0) * 255.0).roundToInt(),
        green = (green.coerceIn(0.0, 1.0) * 255.0).roundToInt(),
        blue = (blue.coerceIn(0.0, 1.0) * 255.0).roundToInt(),
    )
}

private fun linearToSrgb(value: Double): Double = if (value <= 0.0031308) {
    12.92 * value
} else {
    1.055 * value.pow(1.0 / 2.4) - 0.055
}
