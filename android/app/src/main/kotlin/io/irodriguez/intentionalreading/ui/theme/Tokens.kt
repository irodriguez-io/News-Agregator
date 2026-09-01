package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
data class IntentionalReadingTokens(
    val bg: Color,
    val surface: Color,
    val fg: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
    val surfaceHover: Color,
    val strongBorder: Color,
    val quietInk: Color,
    val toastSurface: Color,
    val toastInk: Color,
    val backdrop: Color,
    val primary: Color,
    val secondary: Color,
    val tonal: Color,
    val tertiary: Color,
    val error: Color,
    val card: Color,
    val container: Color,
    val primarySoft: Color,
    val outlineVariant: Color,
    val outlineControl: Color,
    val quiet: Color,
    val onPrimary: Color,
    val onTonal: Color,
)

val LocalIntentionalReadingTokens = staticCompositionLocalOf<IntentionalReadingTokens> {
    error("Intentional Reading tokens were not provided")
}

internal data class SeedPalette(
    val bg: AuthoredColor,
    val surface: AuthoredColor,
    val fg: AuthoredColor,
    val muted: AuthoredColor,
    val border: AuthoredColor,
    val primary: AuthoredColor,
    val secondary: AuthoredColor,
    val tonal: AuthoredColor,
    val tertiary: AuthoredColor,
    val error: AuthoredColor,
)

private val LightSeeds = SeedPalette(
    bg = Color(0xFFF7F9FD).toAuthoredColor(),
    surface = Color(0xFFFFFFFF).toAuthoredColor(),
    fg = Color(0xFF181C1F).toAuthoredColor(),
    muted = Color(0xFF454655).toAuthoredColor(),
    border = Color(0xFF757686).toAuthoredColor(),
    primary = Color(0xFF1B2CC1).toAuthoredColor(),
    secondary = Color(0xFF3856BF).toAuthoredColor(),
    tonal = Color(0xFFABD2FA).toAuthoredColor(),
    tertiary = Color(0xFF212B56).toAuthoredColor(),
    error = Color(0xFFBA1A1A).toAuthoredColor(),
)

private val DarkSeeds = darkSeedsFrom(LightSeeds)
private val LightTokens = tokensFrom(LightSeeds, dark = false)
private val DarkTokens = tokensFrom(DarkSeeds, dark = true)

internal fun lightSeeds(): SeedPalette = LightSeeds

internal fun darkSeeds(): SeedPalette = DarkSeeds

internal fun lightTokens(): IntentionalReadingTokens = LightTokens

internal fun darkTokens(): IntentionalReadingTokens = DarkTokens

private fun darkSeedsFrom(light: SeedPalette): SeedPalette = SeedPalette(
    bg = light.bg.derive(lightness = 0.145, chroma = 0.025),
    surface = light.surface.derive(
        lightness = 0.195,
        chroma = 0.030,
        hue = light.bg.oklch.hue,
    ),
    fg = light.fg.derive(lightness = 0.940, chroma = 0.012),
    muted = light.muted.derive(lightness = 0.720, chroma = 0.025),
    border = light.border.derive(lightness = 0.340, chroma = 0.030),
    primary = light.primary.derive(lightness = 0.740),
    secondary = light.secondary.derive(lightness = 0.700),
    tonal = light.tonal.derive(lightness = 0.420),
    tertiary = light.tertiary.derive(lightness = 0.300),
    error = light.error.derive(lightness = 0.720),
)

private fun AuthoredColor.derive(
    lightness: Double,
    chroma: Double = oklch.chroma,
    hue: Double = oklch.hue,
): AuthoredColor {
    val derived = Oklch(lightness = lightness, chroma = chroma, hue = hue)
    return AuthoredColor(color = derived.toSrgbColor(), oklch = derived)
}

private fun tokensFrom(seeds: SeedPalette, dark: Boolean): IntentionalReadingTokens {
    val card = if (dark) mixOklch(seeds.fg, 0.06, seeds.surface) else seeds.surface.color
    val container = if (dark) {
        mixOklch(seeds.fg, 0.10, seeds.surface)
    } else {
        mixOklch(seeds.primary, 0.05, seeds.bg)
    }
    val primarySoft = mixOklch(seeds.primary, if (dark) 0.14 else 0.12, seeds.surface)
    val quiet = mixOklch(seeds.fg, 0.78, seeds.muted)
    val outlineVariant = decorativeOutline(seeds.border, card, dark)
    val outlineControl = controlBoundary(seeds, card)
    val onPrimary = higherContrast(Color.White, seeds.bg.color, seeds.primary.color)
    val onTonal = higherContrast(LightSeeds.tertiary.color, DarkSeeds.fg.color, seeds.tonal.color)

    return IntentionalReadingTokens(
        bg = seeds.bg.color,
        surface = seeds.surface.color,
        fg = seeds.fg.color,
        muted = seeds.muted.color,
        border = seeds.border.color,
        accent = seeds.primary.color,
        accentSoft = primarySoft,
        surfaceHover = mixOklch(seeds.fg, 0.05, seeds.surface),
        strongBorder = outlineControl,
        quietInk = quiet,
        toastSurface = mixOklch(seeds.fg, 0.94, seeds.surface),
        toastInk = mixOklch(seeds.surface, 0.96, seeds.fg),
        backdrop = seeds.fg.color.copy(alpha = 0.42f),
        primary = seeds.primary.color,
        secondary = seeds.secondary.color,
        tonal = seeds.tonal.color,
        tertiary = seeds.tertiary.color,
        error = seeds.error.color,
        card = card,
        container = container,
        primarySoft = primarySoft,
        outlineVariant = outlineVariant,
        outlineControl = outlineControl,
        quiet = quiet,
        onPrimary = onPrimary,
        onTonal = onTonal,
    )
}

private fun decorativeOutline(border: AuthoredColor, card: Color, dark: Boolean): Color {
    val visibleBorder = border.color.toOklch()
    val lightness = card.toOklch().lightness + if (dark) 0.15 else -0.15
    return Oklch(
        lightness = lightness.coerceIn(0.0, 1.0),
        chroma = visibleBorder.chroma,
        hue = border.oklch.hue,
    ).toSrgbColor()
}

private fun controlBoundary(seeds: SeedPalette, card: Color): Color =
    listOf(seeds.border.color, seeds.muted.color).firstOrNull { candidate ->
        contrastRatio(candidate, card) >= CONTROL_CONTRAST_FLOOR
    } ?: error("No seed clears the control-boundary contrast floor")

private fun higherContrast(first: Color, second: Color, background: Color): Color =
    if (contrastRatio(first, background) >= contrastRatio(second, background)) first else second

private fun contrastRatio(first: Color, second: Color): Double {
    val firstLuminance = relativeLuminance(first)
    val secondLuminance = relativeLuminance(second)
    return (max(firstLuminance, secondLuminance) + 0.05) /
        (min(firstLuminance, secondLuminance) + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    val argb = color.toArgb()
    val red = linearChannel((argb ushr 16 and 0xFF) / 255.0)
    val green = linearChannel((argb ushr 8 and 0xFF) / 255.0)
    val blue = linearChannel((argb and 0xFF) / 255.0)
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

private fun linearChannel(value: Double): Double = if (value <= 0.04045) {
    value / 12.92
} else {
    ((value + 0.055) / 1.055).pow(2.4)
}

private const val CONTROL_CONTRAST_FLOOR = 3.0
