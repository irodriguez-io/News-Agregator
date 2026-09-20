package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppearanceTransitionTest {
    @Test
    fun `the end states are exactly the authored palette in both directions`() {
        // Given the two existing authored palettes and their complete Material role maps.
        val light = lightTokens()
        val dark = darkTokens()
        val lightScheme = intentionalReadingColorScheme(light, darkTheme = false).namedRoles()
        val darkScheme = intentionalReadingColorScheme(dark, darkTheme = true).namedRoles()

        listOf(light to dark, dark to light).forEach { (from, to) ->
            // When the blend is at either endpoint.
            val start = blendTokens(from, to, 0f)
            val end = blendTokens(from, to, 1f)

            // Then all 26 packed Color values are untouched, with no round-trip conversion.
            assertSame(from, start)
            assertSame(to, end)
            assertEquals(from.namedColors(), start.namedColors())
            assertEquals(to.namedColors(), end.namedColors())
            // And all 48 scheme roles retain their exact original values, including surfaceTint.
            assertEquals(
                if (from == light) lightScheme else darkScheme,
                intentionalReadingColorScheme(start, darkFraction = if (from == light) 0f else 1f).namedRoles(),
            )
            assertEquals(
                if (to == dark) darkScheme else lightScheme,
                intentionalReadingColorScheme(end, darkFraction = if (to == dark) 1f else 0f).namedRoles(),
            )
        }
    }

    @Test
    fun `the colours cross-fade rather than snap at an interior point in both directions`() {
        // Given either direction between the existing palettes.
        listOf(lightTokens() to darkTokens(), darkTokens() to lightTokens()).forEach { (from, to) ->
            // When the single progress value is halfway through the blend.
            val middle = blendTokens(from, to, 0.5f)
            val startColors = from.namedColors()
            val endColors = to.namedColors()

            // Then every token is an interior perceptual colour, including the nearly equal tertiary.
            middle.namedColors().forEachIndexed { index, (name, color) ->
                val start = startColors[index].second.convert(ColorSpaces.Oklab)
                val end = endColors[index].second.convert(ColorSpaces.Oklab)
                assertEquals(lerp(start, end, 0.5f), color, name)
                assertTrue(color.red > minOf(start.red, end.red), "$name lightness at start")
                assertTrue(color.red < maxOf(start.red, end.red), "$name lightness at end")
            }
            // And every derived role travels too, rather than snapping to either authored scheme.
            val startScheme = intentionalReadingColorScheme(from, from == darkTokens()).namedRoles()
            val endScheme = intentionalReadingColorScheme(to, to == darkTokens()).namedRoles()
            intentionalReadingColorScheme(middle, darkFraction = 0.5f).namedRoles()
                .forEachIndexed { index, (name, color) ->
                    val start = startScheme[index].second.convert(ColorSpaces.Oklab)
                    val end = endScheme[index].second.convert(ColorSpaces.Oklab)
                    val actual = color.convert(ColorSpaces.Oklab)
                    assertTrue(actual.red > minOf(start.red, end.red), "$name lightness at start")
                    assertTrue(actual.red < maxOf(start.red, end.red), "$name lightness at end")
                }
        }
    }

    @Test
    fun `surface tint cross-fades both variants instead of flipping a boolean`() {
        // Given blended tokens independent of the surface-tint fraction.
        val tokens = blendTokens(lightTokens(), darkTokens(), 0.5f)
        // When the dark fraction is interior.
        val tint = intentionalReadingColorScheme(tokens, darkFraction = 0.5f).surfaceTint
        // Then both variants contribute, while each endpoint keeps its original tint.
        assertEquals(lerp(tokens.tertiary, tokens.bg, 0.5f).copy(alpha = 0.10f), tint)
        assertNotEquals(tokens.tertiary.copy(alpha = 0.10f), tint)
        assertNotEquals(tokens.bg.copy(alpha = 0.10f), tint)
        assertEquals(tokens.tertiary.copy(alpha = 0.10f), intentionalReadingColorScheme(tokens, 0f).surfaceTint)
        assertEquals(tokens.bg.copy(alpha = 0.10f), intentionalReadingColorScheme(tokens, 1f).surfaceTint)
    }

    @Test
    fun `a reduced-motion preference makes the change immediate`() {
        // Given reduced motion is enabled.
        val selected = appearanceTransitionSpec(reducedMotion = true)
        // When the selected spec drives either direction.
        val spec = selected.vectorize(Float.VectorConverter)
        listOf(0f to 1f, 1f to 0f).forEach { (from, to) ->
            val start = AnimationVector1D(from)
            val end = AnimationVector1D(to)
            val velocity = AnimationVector1D(0f)
            // Then no time or intermediate blend is needed.
            assertTrue(selected is SnapSpec<Float>)
            assertEquals(0L, spec.getDurationNanos(start, end, velocity))
            assertEquals(to, spec.getValueFromNanos(0L, start, end, velocity).value)
        }
    }

    @Test
    fun `the colours cross-fade over 300ms on Material 3 Standard easing`() {
        // Given reduced motion is not set.
        val selected = appearanceTransitionSpec(reducedMotion = false)
        val spec = selected.vectorize(Float.VectorConverter)
        val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        // When the selected spec drives either direction.
        listOf(0f to 1f, 1f to 0f).forEach { (from, to) ->
            val start = AnimationVector1D(from)
            val end = AnimationVector1D(to)
            val velocity = AnimationVector1D(0f)
            // Then actual sampled values follow Standard and complete at 300ms.
            assertFalse(selected is SnapSpec<Float>)
            assertEquals(300_000_000L, spec.getDurationNanos(start, end, velocity))
            listOf(0L, 75L, 150L, 225L, 300L).forEach { millis ->
                val expected = from + (to - from) * standard.transform(millis / 300f)
                assertEquals(expected, spec.getValueFromNanos(millis * 1_000_000, start, end, velocity).value, 0.00001f)
            }
        }
    }

    @Test
    fun `nothing bounces pulses or celebrates during the colour transition`() {
        // Given the normal spec and either direction of the colour-only progress.
        val spec = appearanceTransitionSpec(reducedMotion = false).vectorize(Float.VectorConverter)
        listOf(0f to 1f, 1f to 0f).forEach { (from, to) ->
            var previous = from
            // When every millisecond is sampled, including time after completion.
            for (millis in 0L..400L) {
                val value = spec.getValueFromNanos(
                    millis * 1_000_000, AnimationVector1D(from), AnimationVector1D(to), AnimationVector1D(0f),
                ).value
                // Then progress never overshoots, reverses, or repeats.
                assertTrue(value in 0f..1f)
                assertTrue(if (to > from) value >= previous else value <= previous)
                if (millis >= 300L) assertEquals(to, value)
                previous = value
            }
        }
    }

    private fun IntentionalReadingTokens.namedColors(): List<Pair<String, Color>> = listOf(
        "bg" to bg,
        "surface" to surface,
        "fg" to fg,
        "muted" to muted,
        "border" to border,
        "accent" to accent,
        "accentSoft" to accentSoft,
        "surfaceHover" to surfaceHover,
        "strongBorder" to strongBorder,
        "quietInk" to quietInk,
        "toastSurface" to toastSurface,
        "toastInk" to toastInk,
        "backdrop" to backdrop,
        "primary" to primary,
        "secondary" to secondary,
        "tonal" to tonal,
        "tertiary" to tertiary,
        "error" to error,
        "card" to card,
        "container" to container,
        "primarySoft" to primarySoft,
        "outlineVariant" to outlineVariant,
        "outlineControl" to outlineControl,
        "quiet" to quiet,
        "onPrimary" to onPrimary,
        "onTonal" to onTonal,
    )

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
