package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeDerivationTest {
    @Test
    fun `the authored light seeds are the approved values and there is no eleventh seed`() {
        val seeds = lightSeeds().namedColors()

        assertEquals(
            listOf(
                "bg" to "#F7F9FD",
                "surface" to "#FFFFFF",
                "fg" to "#181C1F",
                "muted" to "#454655",
                "border" to "#757686",
                "primary" to "#1B2CC1",
                "secondary" to "#3856BF",
                "tonal" to "#ABD2FA",
                "tertiary" to "#212B56",
                "error" to "#BA1A1A",
            ),
            seeds.map { (name, color) -> name to rgbHex(color) },
        )
        assertEquals(10, seeds.size)
    }

    @Test
    fun `the dark seeds expose the approved derived values`() {
        assertEquals(
            listOf(
                "#060A15",
                "#0E1523",
                "#E4ECF3",
                "#A2A3B4",
                "#353647",
                "#6C9DFF",
                "#7197FF",
                "#2D5072",
                "#202A55",
                "#FF6A5D",
            ),
            darkSeeds().namedColors().map { (_, color) -> rgbHex(color) },
        )
    }

    @Test
    fun `the dark brand seeds hold hue and chroma and change only lightness`() {
        val light = lightSeeds()
        val dark = darkSeeds()
        val brandPairs = listOf(
            light.primary to dark.primary,
            light.secondary to dark.secondary,
            light.tonal to dark.tonal,
            light.tertiary to dark.tertiary,
            light.error to dark.error,
        )

        brandPairs.forEach { (lightSeed, darkSeed) ->
            assertEquals(lightSeed.oklch.hue, darkSeed.oklch.hue, 0.000_001)
            assertEquals(lightSeed.oklch.chroma, darkSeed.oklch.chroma, 0.000_001)
            assertNotEquals(lightSeed.oklch.lightness, darkSeed.oklch.lightness)
        }
    }

    @Test
    fun `the light derived roles match the authoritative table`() {
        assertEquals(
            listOf(
                "#FFFFFF",
                "#EAF0FB",
                "#E0E8FA",
                "#20252A",
                "#CBCCDE",
                "#757686",
                "#FFFFFF",
                "#212B56",
            ),
            lightTokens().derivedRoleColors().map(::rgbHex),
        )
    }

    @Test
    fun `the dark derived roles match the authoritative table`() {
        assertEquals(
            listOf(
                "#18202E",
                "#1F2735",
                "#1A253D",
                "#D4DCE5",
                "#434456",
                "#A2A3B4",
                "#060A15",
                "#E4ECF3",
            ),
            darkTokens().derivedRoleColors().map(::rgbHex),
        )
    }

    @Test
    fun `derived roles come from the stated mixes in both schemes`() {
        assertDerivedRoleRules(tokens = lightTokens(), seeds = lightSeeds(), dark = false)
        assertDerivedRoleRules(tokens = darkTokens(), seeds = darkSeeds(), dark = true)
    }

    @Test
    fun `a near achromatic endpoint adopts the saturated endpoint hue`() {
        val blue = AuthoredColor(Color(0xFF1B2CC1), Oklch(0.40, 0.20, 260.0))
        val white = AuthoredColor(Color.White, Oklch(1.0, 0.0, 90.0))

        val mixed = mixOklch(blue, 0.12, white).toTestOklch()

        assertEquals(blue.oklch.hue, mixed.hue, 1.0)
        assertTrue(mixed.hue !in 70.0..160.0, "The mix shifted toward yellow or green: $mixed")
    }

    @Test
    fun `every legacy token field still exists with its derived value`() {
        assertLegacyMappings(lightTokens(), lightSeeds(), dark = false)
        assertLegacyMappings(darkTokens(), darkSeeds(), dark = true)
    }

    private fun assertDerivedRoleRules(
        tokens: IntentionalReadingTokens,
        seeds: SeedPalette,
        dark: Boolean,
    ) {
        assertEquals(
            if (dark) mixOklch(seeds.fg, 0.06, seeds.surface) else seeds.surface.color,
            tokens.card,
        )
        assertEquals(
            mixOklch(
                if (dark) seeds.fg else seeds.primary,
                if (dark) 0.10 else 0.05,
                if (dark) seeds.surface else seeds.bg,
            ),
            tokens.container,
        )
        assertEquals(mixOklch(seeds.primary, if (dark) 0.14 else 0.12, seeds.surface), tokens.primarySoft)
        assertEquals(mixOklch(seeds.fg, 0.78, seeds.muted), tokens.quiet)
        assertEquals(if (dark) seeds.muted.color else seeds.border.color, tokens.outlineControl)
        assertEquals(if (dark) seeds.bg.color else Color.White, tokens.onPrimary)
        assertEquals(if (dark) seeds.fg.color else lightSeeds().tertiary.color, tokens.onTonal)
    }

    private fun assertLegacyMappings(
        tokens: IntentionalReadingTokens,
        seeds: SeedPalette,
        dark: Boolean,
    ) {
        assertEquals(seeds.bg.color, tokens.bg)
        assertEquals(seeds.surface.color, tokens.surface)
        assertEquals(seeds.fg.color, tokens.fg)
        assertEquals(seeds.muted.color, tokens.muted)
        assertEquals(seeds.border.color, tokens.border)
        assertEquals(tokens.primary, tokens.accent)
        assertEquals(tokens.primarySoft, tokens.accentSoft)
        assertEquals(mixOklch(seeds.fg, 0.05, seeds.surface), tokens.surfaceHover)
        assertEquals(tokens.outlineControl, tokens.strongBorder)
        assertEquals(tokens.quiet, tokens.quietInk)
        assertEquals(mixOklch(seeds.fg, 0.94, seeds.surface), tokens.toastSurface)
        assertEquals(mixOklch(seeds.surface, 0.96, seeds.fg), tokens.toastInk)
        assertEquals(seeds.fg.color.copy(alpha = 0.42f), tokens.backdrop)
        assertEquals(if (dark) "#6BE4ECF3" else "#6B181C1F", argbHex(tokens.backdrop))
    }

    private fun SeedPalette.namedColors(): List<Pair<String, Color>> = listOf(
        "bg" to bg.color,
        "surface" to surface.color,
        "fg" to fg.color,
        "muted" to muted.color,
        "border" to border.color,
        "primary" to primary.color,
        "secondary" to secondary.color,
        "tonal" to tonal.color,
        "tertiary" to tertiary.color,
        "error" to error.color,
    )

    private fun IntentionalReadingTokens.derivedRoleColors(): List<Color> = listOf(
        card,
        container,
        primarySoft,
        quiet,
        outlineVariant,
        outlineControl,
        onPrimary,
        onTonal,
    )

    private fun Color.toTestOklch(): TestOklch {
        val argb = toArgb()
        val red = srgbToLinear((argb ushr 16 and 0xFF) / 255.0)
        val green = srgbToLinear((argb ushr 8 and 0xFF) / 255.0)
        val blue = srgbToLinear((argb and 0xFF) / 255.0)

        val l = cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue)
        val m = cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue)
        val s = cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue)
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        return TestOklch(
            lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            chroma = sqrt(a * a + b * b),
            hue = (atan2(b, a) * 180.0 / PI + 360.0) % 360.0,
        )
    }

    private fun srgbToLinear(value: Double): Double = if (value <= 0.04045) {
        value / 12.92
    } else {
        ((value + 0.055) / 1.055).pow(2.4)
    }

    private fun rgbHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    private fun argbHex(color: Color): String = "#%08X".format(color.toArgb().toLong() and 0xFFFFFFFFL)

    private data class TestOklch(
        val lightness: Double,
        val chroma: Double,
        val hue: Double,
    )
}
