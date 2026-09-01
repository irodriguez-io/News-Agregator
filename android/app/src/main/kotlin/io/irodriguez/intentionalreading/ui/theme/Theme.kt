package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.irodriguez.intentionalreading.domain.model.Appearance

@Composable
fun IntentionalReadingTheme(
    appearance: Appearance,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearance) {
        Appearance.LIGHT -> false
        Appearance.DARK -> true
        Appearance.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = if (darkTheme) darkTokens() else lightTokens()
    CompositionLocalProvider(LocalIntentionalReadingTokens provides tokens) {
        MaterialTheme(
            colorScheme = intentionalReadingColorScheme(tokens),
            typography = IntentionalReadingTypography,
            content = content,
        )
    }
}

internal fun intentionalReadingColorScheme(
    tokens: IntentionalReadingTokens,
    darkTheme: Boolean = false,
): ColorScheme =
    // Every role is supplied below, so this factory contributes no default Material palette values.
    lightColorScheme(
        primary = tokens.accent,
        onPrimary = tokens.surface,
        primaryContainer = tokens.accentSoft,
        onPrimaryContainer = tokens.accent,
        inversePrimary = tokens.accent,
        secondary = tokens.fg,
        onSecondary = tokens.surface,
        secondaryContainer = tokens.surfaceHover,
        onSecondaryContainer = tokens.fg,
        tertiary = tokens.quietInk,
        onTertiary = tokens.surface,
        tertiaryContainer = tokens.surfaceHover,
        onTertiaryContainer = tokens.fg,
        background = tokens.bg,
        onBackground = tokens.fg,
        surface = tokens.surface,
        onSurface = tokens.fg,
        surfaceVariant = tokens.surfaceHover,
        onSurfaceVariant = tokens.muted,
        surfaceTint = tokens.surface.copy(alpha = 0f),
        inverseSurface = tokens.toastSurface,
        inverseOnSurface = tokens.toastInk,
        error = tokens.fg,
        onError = tokens.surface,
        errorContainer = tokens.surfaceHover,
        onErrorContainer = tokens.fg,
        outline = tokens.border,
        outlineVariant = tokens.strongBorder,
        scrim = tokens.backdrop,
        surfaceBright = tokens.surface,
        surfaceDim = tokens.bg,
        surfaceContainer = tokens.surface,
        surfaceContainerHigh = tokens.surfaceHover,
        surfaceContainerHighest = tokens.surfaceHover,
        surfaceContainerLow = tokens.bg,
        surfaceContainerLowest = tokens.bg,
        primaryFixed = tokens.accent,
        primaryFixedDim = tokens.accentSoft,
        onPrimaryFixed = tokens.surface,
        onPrimaryFixedVariant = tokens.quietInk,
        secondaryFixed = tokens.fg,
        secondaryFixedDim = tokens.quietInk,
        onSecondaryFixed = tokens.surface,
        onSecondaryFixedVariant = tokens.muted,
        tertiaryFixed = tokens.quietInk,
        tertiaryFixedDim = tokens.muted,
        onTertiaryFixed = tokens.surface,
        onTertiaryFixedVariant = tokens.fg,
    )
