package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ThemeContrastTest {
    @Test
    fun `a control boundary outline clears three to one in both schemes`() {
        listOf("light" to lightTokens(), "dark" to darkTokens()).forEach { (scheme, tokens) ->
            val outlineRatio = contrastRatio(tokens.outlineControl, tokens.card)
            val triageRatio = contrastRatio(tokens.secondary, tokens.card)

            assertTrue(outlineRatio >= 3.0, "$scheme outlineControl contrast was $outlineRatio")
            assertTrue(triageRatio >= 3.0, "$scheme secondary contrast was $triageRatio")
        }
    }

    @Test
    fun `the decorative hairline is visible without being held to the control floor`() {
        listOf("light" to lightTokens(), "dark" to darkTokens()).forEach { (scheme, tokens) ->
            val ratio = contrastRatio(tokens.outlineVariant, tokens.card)

            assertTrue(ratio > 1.0, "$scheme outlineVariant was not visible against the card")
        }
    }

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
}
