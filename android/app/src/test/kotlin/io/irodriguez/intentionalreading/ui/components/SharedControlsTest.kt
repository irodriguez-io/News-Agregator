package io.irodriguez.intentionalreading.ui.components

import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.lightTokens
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedControlsTest {
    @Test
    fun `the filled primary control is 52 dp and uses the primary roles`() {
        val layout = sharedControlLayout(IntentionalReadingSpacing, IntentionalReadingShapes)
        val tokens = lightTokens()
        val colors = sharedControlColors(tokens)

        assertEquals(52.dp, layout.filledPrimaryHeight)
        assertEquals(IntentionalReadingShapes.filledPrimaryButton, layout.filledPrimaryShape)
        assertEquals(tokens.primary, colors.primaryFill)
        assertEquals(tokens.onPrimary, colors.primaryLabel)
    }

    @Test
    fun `the tonal secondary control uses tonal roles and meets the target floor`() {
        val layout = sharedControlLayout(IntentionalReadingSpacing, IntentionalReadingShapes)
        val tokens = lightTokens()
        val colors = sharedControlColors(tokens)

        assertEquals(48.dp, layout.minimumTouchTarget)
        assertEquals(tokens.tonal, colors.tonalFill)
        assertEquals(tokens.onTonal, colors.tonalLabel)
    }

    @Test
    fun `the inline triage control has a 48 dp minimum, 4 dp padding and the secondary label`() {
        // Given the shared control source and the authored spacing and shape scales.
        val source = Path.of(
            "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/SharedControls.kt",
        ).readText()
        val layout = sharedControlLayout(IntentionalReadingSpacing, IntentionalReadingShapes)

        // Then the inline control is borderless, padded by one base unit and uses secondary.
        assertTrue(source.contains("fun InlineTriageControl("), "InlineTriageControl is missing")
        assertEquals(48.dp, layout.minimumTouchTarget)
        assertEquals(4.dp, IntentionalReadingSpacing.baseUnit)
        assertEquals(IntentionalReadingShapes.pill, layout.triageShape)
        assertTrue(source.contains(".padding(horizontal = spacing.baseUnit)"), "4 dp padding is missing")
        assertTrue(source.contains("triageLabel = tokens.secondary"), "secondary triage label is missing")
        assertFalse(source.contains("BorderStroke"), "triage must have no outline")
    }

    @Test
    fun `an inline triage control carries a non-empty accessible name`() {
        assertEquals("Save for later", triageAccessibleName("Save for later"))
        assertFailsWith<IllegalArgumentException> { triageAccessibleName(" ") }
    }

    @Test
    fun `pressed and disabled values are shared by every control`() {
        assertEquals(0.12f, SharedControlState.pressedOverlayAlpha)
        assertEquals(0.95f, SharedControlState.pressedScale)
        assertEquals(0.38f, SharedControlState.disabledOpacity)
    }

    @Test
    fun `a disabled shared control is not interactive`() {
        assertFalse(isSharedControlInteractive(enabled = false))
    }

    @Test
    fun `the shared control source names no colour radius dimension or font literal`() {
        val source = Path.of(
            "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/SharedControls.kt",
        ).readText()

        val forbidden = listOf(
            Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b"""),
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\s*\("""),
        )

        forbidden.forEach { pattern ->
            assertFalse(pattern.containsMatchIn(source), "Forbidden literal matched ${pattern.pattern}")
        }
    }
}
