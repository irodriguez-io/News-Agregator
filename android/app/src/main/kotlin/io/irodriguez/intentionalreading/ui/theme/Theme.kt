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
    CompositionLocalProvider(
        LocalIntentionalReadingTokens provides tokens,
        LocalIntentionalReadingShapes provides IntentionalReadingShapes,
        LocalIntentionalReadingSpacing provides IntentionalReadingSpacing,
    ) {
        MaterialTheme(
            colorScheme = intentionalReadingColorScheme(tokens, darkTheme),
            typography = IntentionalReadingTypography,
            shapes = IntentionalReadingMaterialShapes,
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
        primary = tokens.primary,
        onPrimary = tokens.onPrimary,
        primaryContainer = tokens.primarySoft,
        onPrimaryContainer = tokens.primary,
        inversePrimary = tokens.primary,
        secondary = tokens.secondary,
        onSecondary = tokens.surface,
        secondaryContainer = tokens.tonal,
        onSecondaryContainer = tokens.onTonal,
        tertiary = tokens.tertiary,
        onTertiary = tokens.surface,
        tertiaryContainer = tokens.container,
        onTertiaryContainer = tokens.fg,
        background = tokens.bg,
        onBackground = tokens.fg,
        surface = tokens.surface,
        onSurface = tokens.fg,
        surfaceVariant = tokens.container,
        onSurfaceVariant = tokens.muted,
        surfaceTint = (if (darkTheme) tokens.bg else tokens.tertiary).copy(alpha = 0.10f),
        inverseSurface = tokens.toastSurface,
        inverseOnSurface = tokens.toastInk,
        error = tokens.error,
        onError = tokens.surface,
        errorContainer = tokens.container,
        onErrorContainer = tokens.fg,
        outline = tokens.outlineControl,
        outlineVariant = tokens.outlineVariant,
        scrim = tokens.backdrop,
        surfaceBright = tokens.surface,
        surfaceDim = tokens.bg,
        surfaceContainer = tokens.container,
        surfaceContainerHigh = tokens.surfaceHover,
        surfaceContainerHighest = tokens.surfaceHover,
        surfaceContainerLow = tokens.card,
        surfaceContainerLowest = tokens.bg,
        primaryFixed = tokens.primary,
        primaryFixedDim = tokens.primarySoft,
        onPrimaryFixed = tokens.onPrimary,
        onPrimaryFixedVariant = tokens.quiet,
        secondaryFixed = tokens.secondary,
        secondaryFixedDim = tokens.tonal,
        onSecondaryFixed = tokens.surface,
        onSecondaryFixedVariant = tokens.onTonal,
        tertiaryFixed = tokens.tertiary,
        tertiaryFixedDim = tokens.quiet,
        onTertiaryFixed = tokens.surface,
        onTertiaryFixedVariant = tokens.fg,
    )
