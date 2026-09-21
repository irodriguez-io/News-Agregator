package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import io.irodriguez.intentionalreading.domain.model.Appearance

@Composable
fun IntentionalReadingTheme(
    appearance: Appearance,
    reducedMotion: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearance) {
        Appearance.LIGHT -> false
        Appearance.DARK -> true
        Appearance.SYSTEM -> isSystemInDarkTheme()
    }
    val reducedMotionEnabled = reducedMotion()
    val targetFraction = if (darkTheme) 1f else 0f
    // A fixed light-to-dark axis also lets an interrupted switch reverse from its current colour.
    // animateFloatAsState starts at the first target, so a cold start never fades from another scheme.
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = appearanceTransitionSpec(reducedMotionEnabled),
        label = "appearance scheme",
    )
    // Snap in this composition too, without waiting for the animation's next frame.
    val darkFraction = if (reducedMotionEnabled) targetFraction else animatedFraction
    val tokens = blendTokens(lightTokens(), darkTokens(), darkFraction)
    val lightScheme = remember { intentionalReadingColorScheme(lightTokens(), darkTheme = false) }
    val darkScheme = remember { intentionalReadingColorScheme(darkTokens(), darkTheme = true) }
    CompositionLocalProvider(
        LocalIntentionalReadingTokens provides tokens,
        LocalIntentionalReadingShapes provides IntentionalReadingShapes,
        LocalIntentionalReadingSpacing provides IntentionalReadingSpacing,
    ) {
        MaterialTheme(
            colorScheme = blendColorSchemes(lightScheme, darkScheme, darkFraction),
            typography = IntentionalReadingTypography,
            shapes = IntentionalReadingMaterialShapes,
            content = content,
        )
    }
}

// Material 3 Standard (motionEasingStandardInterpolator), not Material 2 FastOutSlowIn.
private val AppearanceStandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

internal fun appearanceTransitionSpec(reducedMotion: Boolean): FiniteAnimationSpec<Float> =
    if (reducedMotion) snap() else tween(durationMillis = 300, easing = AppearanceStandardEasing)

/** Blend derived roles so each colour travels only between its own authored endpoints. */
internal fun blendColorSchemes(
    from: ColorScheme,
    to: ColorScheme,
    fraction: Float,
): ColorScheme {
    if (fraction <= 0f) return from
    if (fraction >= 1f) return to

    fun blend(start: Color, end: Color): Color =
        lerp(start.convert(ColorSpaces.Oklab), end.convert(ColorSpaces.Oklab), fraction)

    return from.copy(
        primary = blend(from.primary, to.primary),
        onPrimary = blend(from.onPrimary, to.onPrimary),
        primaryContainer = blend(from.primaryContainer, to.primaryContainer),
        onPrimaryContainer = blend(from.onPrimaryContainer, to.onPrimaryContainer),
        inversePrimary = blend(from.inversePrimary, to.inversePrimary),
        secondary = blend(from.secondary, to.secondary),
        onSecondary = blend(from.onSecondary, to.onSecondary),
        secondaryContainer = blend(from.secondaryContainer, to.secondaryContainer),
        onSecondaryContainer = blend(from.onSecondaryContainer, to.onSecondaryContainer),
        tertiary = blend(from.tertiary, to.tertiary),
        onTertiary = blend(from.onTertiary, to.onTertiary),
        tertiaryContainer = blend(from.tertiaryContainer, to.tertiaryContainer),
        onTertiaryContainer = blend(from.onTertiaryContainer, to.onTertiaryContainer),
        background = blend(from.background, to.background),
        onBackground = blend(from.onBackground, to.onBackground),
        surface = blend(from.surface, to.surface),
        onSurface = blend(from.onSurface, to.onSurface),
        surfaceVariant = blend(from.surfaceVariant, to.surfaceVariant),
        onSurfaceVariant = blend(from.onSurfaceVariant, to.onSurfaceVariant),
        surfaceTint = blend(from.surfaceTint, to.surfaceTint),
        inverseSurface = blend(from.inverseSurface, to.inverseSurface),
        inverseOnSurface = blend(from.inverseOnSurface, to.inverseOnSurface),
        error = blend(from.error, to.error),
        onError = blend(from.onError, to.onError),
        errorContainer = blend(from.errorContainer, to.errorContainer),
        onErrorContainer = blend(from.onErrorContainer, to.onErrorContainer),
        outline = blend(from.outline, to.outline),
        outlineVariant = blend(from.outlineVariant, to.outlineVariant),
        scrim = blend(from.scrim, to.scrim),
        surfaceBright = blend(from.surfaceBright, to.surfaceBright),
        surfaceDim = blend(from.surfaceDim, to.surfaceDim),
        surfaceContainer = blend(from.surfaceContainer, to.surfaceContainer),
        surfaceContainerHigh = blend(from.surfaceContainerHigh, to.surfaceContainerHigh),
        surfaceContainerHighest = blend(from.surfaceContainerHighest, to.surfaceContainerHighest),
        surfaceContainerLow = blend(from.surfaceContainerLow, to.surfaceContainerLow),
        surfaceContainerLowest = blend(from.surfaceContainerLowest, to.surfaceContainerLowest),
        primaryFixed = blend(from.primaryFixed, to.primaryFixed),
        primaryFixedDim = blend(from.primaryFixedDim, to.primaryFixedDim),
        onPrimaryFixed = blend(from.onPrimaryFixed, to.onPrimaryFixed),
        onPrimaryFixedVariant = blend(from.onPrimaryFixedVariant, to.onPrimaryFixedVariant),
        secondaryFixed = blend(from.secondaryFixed, to.secondaryFixed),
        secondaryFixedDim = blend(from.secondaryFixedDim, to.secondaryFixedDim),
        onSecondaryFixed = blend(from.onSecondaryFixed, to.onSecondaryFixed),
        onSecondaryFixedVariant = blend(from.onSecondaryFixedVariant, to.onSecondaryFixedVariant),
        tertiaryFixed = blend(from.tertiaryFixed, to.tertiaryFixed),
        tertiaryFixedDim = blend(from.tertiaryFixedDim, to.tertiaryFixedDim),
        onTertiaryFixed = blend(from.onTertiaryFixed, to.onTertiaryFixed),
        onTertiaryFixedVariant = blend(from.onTertiaryFixedVariant, to.onTertiaryFixedVariant),
    )
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
