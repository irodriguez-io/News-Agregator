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
    fun `the circular triage control is 56 dp with the secondary outline`() {
        val layout = sharedControlLayout(IntentionalReadingSpacing, IntentionalReadingShapes)
        val tokens = lightTokens()
        val colors = sharedControlColors(tokens)

        assertEquals(56.dp, layout.triageSize)
        assertEquals(1.5.dp, layout.triageOutlineWidth)
        assertEquals(IntentionalReadingShapes.iconButton, layout.triageShape)
        assertEquals(tokens.secondary, colors.triageOutline)
    }

    @Test
    fun `a circular triage control carries a non-empty accessible name`() {
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
