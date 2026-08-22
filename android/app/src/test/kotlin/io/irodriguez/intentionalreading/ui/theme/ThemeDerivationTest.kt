package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeDerivationTest {
    @Test
    fun `light authored and OKLCH-derived values match the authoritative table`() {
        assertTokenHexes(
            tokens = lightTokens(),
            expected = listOf(
                "#F2F6FB",
                "#FCFEFF",
                "#0F1725",
                "#58616F",
                "#CFD6E2",
                "#0B2D72",
                "#DBE3EF",
                "#EEF0F3",
                "#8F97A4",
                "#1E2634",
                "#1A2230",
                "#F1F3F6",
            ),
        )
        assertEquals("#6B0F1725", argbHex(lightTokens().backdrop))
    }

    @Test
    fun `dark authored and OKLCH-derived values match the authoritative table`() {
        assertTokenHexes(
            tokens = darkTokens(),
            expected = listOf(
                "#050A15",
                "#0D1522",
                "#E6ECF3",
                "#9AA6B4",
                "#2F3848",
                "#7FAAFA",
                "#182438",
                "#151E2B",
                "#606A77",
                "#D5DCE5",
                "#D7DDE5",
                "#141C29",
            ),
        )
        assertEquals("#6BE6ECF3", argbHex(darkTokens().backdrop))
    }

    private fun assertTokenHexes(tokens: IntentionalReadingTokens, expected: List<String>) {
        assertEquals(
            expected,
            listOf(
                tokens.bg,
                tokens.surface,
                tokens.fg,
                tokens.muted,
                tokens.border,
                tokens.accent,
                tokens.accentSoft,
                tokens.surfaceHover,
                tokens.strongBorder,
                tokens.quietInk,
                tokens.toastSurface,
                tokens.toastInk,
            ).map(::rgbHex),
        )
    }

    private fun rgbHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

    private fun argbHex(color: Color): String = "#%08X".format(color.toArgb().toLong() and 0xFFFFFFFFL)
}
