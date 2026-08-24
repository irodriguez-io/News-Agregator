package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
)

val LocalIntentionalReadingTokens = staticCompositionLocalOf<IntentionalReadingTokens> {
    error("Intentional Reading tokens were not provided")
}

private data class AuthoredPalette(
    val bg: AuthoredColor,
    val surface: AuthoredColor,
    val fg: AuthoredColor,
    val muted: AuthoredColor,
    val border: AuthoredColor,
    val accent: AuthoredColor,
)

private val LightAuthored = AuthoredPalette(
    bg = AuthoredColor(Color(0xFFF2F6FB), Oklch(0.972, 0.008, 255.0)), // oklch(0.972 0.008 255)
    surface = AuthoredColor(Color(0xFFFCFEFF), Oklch(0.995, 0.003, 255.0)), // oklch(0.995 0.003 255)
    fg = AuthoredColor(Color(0xFF0F1725), Oklch(0.205, 0.03, 260.0)), // oklch(0.205 0.03 260)
    muted = AuthoredColor(Color(0xFF58616F), Oklch(0.49, 0.025, 260.0)), // oklch(0.49 0.025 260)
    border = AuthoredColor(Color(0xFFCFD6E2), Oklch(0.875, 0.018, 260.0)), // oklch(0.875 0.018 260)
    accent = AuthoredColor(Color(0xFF0B2D72), Oklch(0.322424, 0.12543, 262.24)), // oklch(0.322424 0.12543 262.24)
)

private val DarkAuthored = AuthoredPalette(
    bg = AuthoredColor(Color(0xFF050A15), Oklch(0.145, 0.025, 260.0)), // oklch(0.145 0.025 260)
    surface = AuthoredColor(Color(0xFF0D1522), Oklch(0.195, 0.03, 260.0)), // oklch(0.195 0.03 260)
    fg = AuthoredColor(Color(0xFFE6ECF3), Oklch(0.94, 0.012, 255.0)), // oklch(0.94 0.012 255)
    muted = AuthoredColor(Color(0xFF9AA6B4), Oklch(0.72, 0.025, 255.0)), // oklch(0.72 0.025 255)
    border = AuthoredColor(Color(0xFF2F3848), Oklch(0.34, 0.03, 260.0)), // oklch(0.34 0.03 260)
    accent = AuthoredColor(Color(0xFF7FAAFA), Oklch(0.74, 0.12543, 262.24)), // oklch(0.74 0.12543 262.24)
)

private val LightTokens = tokensFrom(LightAuthored)
private val DarkTokens = tokensFrom(DarkAuthored)

internal fun lightTokens(): IntentionalReadingTokens = LightTokens

internal fun darkTokens(): IntentionalReadingTokens = DarkTokens

private fun tokensFrom(authored: AuthoredPalette): IntentionalReadingTokens = IntentionalReadingTokens(
    bg = authored.bg.color,
    surface = authored.surface.color,
    fg = authored.fg.color,
    muted = authored.muted.color,
    border = authored.border.color,
    accent = authored.accent.color,
    accentSoft = mixOklch(authored.accent, 0.12, authored.surface),
    surfaceHover = mixOklch(authored.fg, 0.05, authored.surface),
    strongBorder = mixOklch(authored.fg, 0.30, authored.border),
    quietInk = mixOklch(authored.fg, 0.78, authored.muted),
    toastSurface = mixOklch(authored.fg, 0.94, authored.surface),
    toastInk = mixOklch(authored.surface, 0.96, authored.fg),
    backdrop = authored.fg.color.copy(alpha = 0.42f),
)
