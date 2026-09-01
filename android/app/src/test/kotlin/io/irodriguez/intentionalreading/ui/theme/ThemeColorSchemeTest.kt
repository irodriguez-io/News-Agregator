package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThemeColorSchemeTest {
    @Test
    fun `control and decorative outlines reach their distinct Material roles`() {
        listOf(lightTokens(), darkTokens()).forEach { tokens ->
            val scheme = intentionalReadingColorScheme(tokens)

            assertEquals(tokens.outlineControl, scheme.outline, "control outline")
            assertEquals(tokens.outlineVariant, scheme.outlineVariant, "decorative outline")
            assertNotEquals(tokens.outlineControl, scheme.outlineVariant, "decorative outline used control role")
        }
    }

    @Test
    fun `surface tint carries the scheme appropriate ambient shadow tint`() {
        assertEquals(
            lightTokens().tertiary.copy(alpha = 0.10f),
            intentionalReadingColorScheme(lightTokens(), darkTheme = false).surfaceTint,
            "light surface tint",
        )
        assertEquals(
            darkTokens().bg.copy(alpha = 0.10f),
            intentionalReadingColorScheme(darkTokens(), darkTheme = true).surfaceTint,
            "dark surface tint",
        )
    }

    @Test
    fun `tonal container reaches the navigation selection roles`() {
        listOf(lightTokens(), darkTokens()).forEach { tokens ->
            val scheme = intentionalReadingColorScheme(tokens)

            assertEquals(tokens.tonal, scheme.secondaryContainer, "navigation indicator")
            assertEquals(tokens.onTonal, scheme.onSecondaryContainer, "navigation indicator ink")
        }
    }

    @Test
    fun `all 48 Material colour roles are explicitly mapped from theme tokens`() {
        assertCompleteRoleMap(lightTokens(), darkTheme = false)
        assertCompleteRoleMap(darkTokens(), darkTheme = true)
    }

    private fun assertCompleteRoleMap(tokens: IntentionalReadingTokens, darkTheme: Boolean) {
        val actual = intentionalReadingColorScheme(tokens, darkTheme).namedRoles()
        val expected = listOf(
            "primary" to tokens.primary,
            "onPrimary" to tokens.onPrimary,
            "primaryContainer" to tokens.primarySoft,
            "onPrimaryContainer" to tokens.primary,
            "inversePrimary" to tokens.primary,
            "secondary" to tokens.secondary,
            "onSecondary" to tokens.surface,
            "secondaryContainer" to tokens.tonal,
            "onSecondaryContainer" to tokens.onTonal,
            "tertiary" to tokens.tertiary,
            "onTertiary" to tokens.surface,
            "tertiaryContainer" to tokens.container,
            "onTertiaryContainer" to tokens.fg,
            "background" to tokens.bg,
            "onBackground" to tokens.fg,
            "surface" to tokens.surface,
            "onSurface" to tokens.fg,
            "surfaceVariant" to tokens.container,
            "onSurfaceVariant" to tokens.muted,
            "surfaceTint" to (if (darkTheme) tokens.bg else tokens.tertiary).copy(alpha = 0.10f),
            "inverseSurface" to tokens.toastSurface,
            "inverseOnSurface" to tokens.toastInk,
            "error" to tokens.error,
            "onError" to tokens.surface,
            "errorContainer" to tokens.container,
            "onErrorContainer" to tokens.fg,
            "outline" to tokens.outlineControl,
            "outlineVariant" to tokens.outlineVariant,
            "scrim" to tokens.backdrop,
            "surfaceBright" to tokens.surface,
            "surfaceDim" to tokens.bg,
            "surfaceContainer" to tokens.container,
            "surfaceContainerHigh" to tokens.surfaceHover,
            "surfaceContainerHighest" to tokens.surfaceHover,
            "surfaceContainerLow" to tokens.card,
            "surfaceContainerLowest" to tokens.bg,
            "primaryFixed" to tokens.primary,
            "primaryFixedDim" to tokens.primarySoft,
            "onPrimaryFixed" to tokens.onPrimary,
            "onPrimaryFixedVariant" to tokens.quiet,
            "secondaryFixed" to tokens.secondary,
            "secondaryFixedDim" to tokens.tonal,
            "onSecondaryFixed" to tokens.surface,
            "onSecondaryFixedVariant" to tokens.onTonal,
            "tertiaryFixed" to tokens.tertiary,
            "tertiaryFixedDim" to tokens.quiet,
            "onTertiaryFixed" to tokens.surface,
            "onTertiaryFixedVariant" to tokens.fg,
        )

        assertEquals(48, actual.size)
        assertEquals(expected, actual)
    }

    private fun ColorScheme.namedRoles(): List<Pair<String, Color>> = listOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "scrim" to scrim,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "primaryFixed" to primaryFixed,
        "primaryFixedDim" to primaryFixedDim,
        "onPrimaryFixed" to onPrimaryFixed,
        "onPrimaryFixedVariant" to onPrimaryFixedVariant,
        "secondaryFixed" to secondaryFixed,
        "secondaryFixedDim" to secondaryFixedDim,
        "onSecondaryFixed" to onSecondaryFixed,
        "onSecondaryFixedVariant" to onSecondaryFixedVariant,
        "tertiaryFixed" to tertiaryFixed,
        "tertiaryFixedDim" to tertiaryFixedDim,
        "onTertiaryFixed" to onTertiaryFixed,
        "onTertiaryFixedVariant" to onTertiaryFixedVariant,
    )
}
