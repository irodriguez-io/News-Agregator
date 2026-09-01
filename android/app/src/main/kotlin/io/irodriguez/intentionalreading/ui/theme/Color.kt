package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
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
    val firstVisible = first.color.toOklch()
    val secondVisible = second.color.toOklch()
    val firstHue = when {
        firstVisible.chroma <= NEGLIGIBLE_CHROMA -> second.oklch.hue
        secondVisible.chroma <= NEGLIGIBLE_CHROMA -> first.oklch.hue
        else -> firstVisible.hue
    }
    val secondHue = when {
        secondVisible.chroma <= NEGLIGIBLE_CHROMA -> firstHue
        firstVisible.chroma <= NEGLIGIBLE_CHROMA -> second.oklch.hue
        else -> secondVisible.hue
    }
    val hueDelta = ((secondHue - firstHue + 540.0) % 360.0) - 180.0
    return Oklch(
        lightness = firstVisible.lightness * firstFraction + secondVisible.lightness * secondFraction,
        chroma = firstVisible.chroma * firstFraction + secondVisible.chroma * secondFraction,
        hue = (firstHue + hueDelta * secondFraction + 360.0) % 360.0,
    ).toSrgbColor()
}

internal fun Color.toAuthoredColor(): AuthoredColor = AuthoredColor(
    color = this,
    oklch = toOklch(),
)

internal fun Color.toOklch(): Oklch {
    val argb = toArgb()
    val red = srgbToLinear((argb ushr 16 and 0xFF) / 255.0)
    val green = srgbToLinear((argb ushr 8 and 0xFF) / 255.0)
    val blue = srgbToLinear((argb and 0xFF) / 255.0)

    val l = cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue)
    val m = cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue)
    val s = cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue)
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return Oklch(
        lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        chroma = sqrt(a * a + b * b),
        hue = (atan2(b, a) * 180.0 / PI + 360.0) % 360.0,
    )
}

internal fun Oklch.toSrgbColor(): Color {
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

private fun srgbToLinear(value: Double): Double = if (value <= 0.04045) {
    value / 12.92
} else {
    ((value + 0.055) / 1.055).pow(2.4)
}

private fun linearToSrgb(value: Double): Double = if (value <= 0.0031308) {
    12.92 * value
} else {
    1.055 * value.pow(1.0 / 2.4) - 0.055
}

private const val NEGLIGIBLE_CHROMA = 0.0001
