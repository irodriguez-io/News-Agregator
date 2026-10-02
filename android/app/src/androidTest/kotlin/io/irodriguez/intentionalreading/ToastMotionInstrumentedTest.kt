package io.irodriguez.intentionalreading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.ui.components.LiveStatusMessage
import io.irodriguez.intentionalreading.ui.components.ToastRegion
import io.irodriguez.intentionalreading.ui.components.UndoToast
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToastMotionInstrumentedTest {
    @get:Rule val rule = createComposeRule()

    private val undo = mutableStateOf<String?>(null)
    private val status = mutableStateOf<String?>(null)
    private val reference = mutableStateOf(true)
    private var calls = 0
    private var density = 1f
    private var background = Color.Unspecified
    private var surface = Color.Unspecified
    private lateinit var resting: Rect
    private lateinit var referenceStatusBounds: Rect

    @Test
    fun toastEasesIn_GivenMotion_WhenUndoAppears_ThenTransparentBelowRestHalfwayAndOpaqueAt200ms() {
        entry(statusMessage = false)
    }

    @Test
    fun toastEasesIn_GivenMotion_WhenUndoAppears_ThenUndoWorksAtZeroMs() {
        host(statusMessage = false)
        show(statusMessage = false)
        rootClick(rule.onNodeWithText("Undo").fetchSemanticsNode().boundsInRoot.center)
        assertEquals("Undo must work on the first transparent frame", 1, calls)
    }

    @Test
    fun toastEasesOut_GivenMotion_WhenCleared_ThenOldPixelsRemainButUndoIsInertUntil200ms() {
        exit(statusMessage = false)
    }

    @Test
    fun toastEasesOut_GivenMotion_WhenReplaced_ThenOldCopyIsInertWhileNewCopyEnters() {
        host(statusMessage = false)
        show(statusMessage = false)
        rule.mainClock.advanceTimeBy(208)
        val oldButton = rule.onNodeWithText("Undo").fetchSemanticsNode().boundsInRoot.center
        rule.runOnUiThread { undo.value = "Saved" }
        startAnimation()
        rule.mainClock.advanceTimeBy(96)
        assertBlended(pixel(resting), "old replacement still draws")
        rule.onNodeWithText(MESSAGE).assertDoesNotExist()
        val newBounds = surfaceBounds("Saved")
        assertTrue("Old Undo click must be outside the new toast", oldButton.x > newBounds.right)
        rootClick(oldButton + Offset(0f, 8f * density))
        assertEquals("Leaving replacement cannot invoke Undo", 0, calls)
        rule.mainClock.advanceTimeBy(112)
        assertColor(background, pixel(resting), "old replacement is gone")
        assertColor(surface, pixel(surfaceBounds("Saved")), "new replacement is opaque")
        rootClick(rule.onNodeWithText("Undo").fetchSemanticsNode().boundsInRoot.center)
        assertEquals(1, calls)
    }

    @Test
    fun statusMessageMovesTheSameWay_GivenMotion_WhenShown_ThenItEntersOver200ms() {
        entry(statusMessage = true)
    }

    @Test
    fun statusMessageMovesTheSameWay_GivenMotion_WhenCleared_ThenItLeavesOver200ms() {
        exit(statusMessage = true)
    }

    @Test
    fun reducedMotionShowsAndHidesAtOnce_GivenReducedMotion_WhenUndoChanges_ThenFirstFrameIsFinal() {
        immediate(statusMessage = false)
    }

    @Test
    fun reducedMotionShowsAndHidesAtOnce_GivenReducedMotion_WhenStatusChanges_ThenFirstFrameIsFinal() {
        immediate(statusMessage = true)
    }

    @Test
    fun toastTimingAndSemanticsUnchanged_GivenBothResting_WhenInspected_ThenPoliteUnfocusedAndAtOriginalPositions() {
        host(statusMessage = false, both = true, reduced = true)
        rule.runOnUiThread {
            undo.value = MESSAGE
            status.value = STATUS
        }
        rule.mainClock.advanceTimeByFrame()
        assertBounds(resting, surfaceBounds(MESSAGE))
        assertBounds(referenceStatusBounds, surfaceBounds(STATUS))
        for (message in listOf(MESSAGE, STATUS)) {
            val node = surfaceNode(message).fetchSemanticsNode()
            assertEquals(LiveRegionMode.Polite, node.config[SemanticsProperties.LiveRegion])
            assertFalse(node.config.contains(SemanticsProperties.Focused))
        }
        val focused = rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Focused, true))
        assertTrue("Showing messages must not take focus", focused.fetchSemanticsNodes().isEmpty())
    }

    private fun host(statusMessage: Boolean, both: Boolean = false, reduced: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                density = LocalDensity.current.density
                background = LocalIntentionalReadingTokens.current.bg
                surface = LocalIntentionalReadingTokens.current.toastSurface
                Box(Modifier.fillMaxSize().background(background).testTag(HOST)) {
                    if (reference.value) {
                        // The original app's toast Column provides an independent position oracle.
                        Column(
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = 96.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!statusMessage || both) UndoToast(MESSAGE, onUndo = {})
                            if (statusMessage || both) LiveStatusMessage(if (both) STATUS else MESSAGE)
                        }
                    } else {
                        ToastRegion(
                            undoToastMessage = undo.value,
                            announcementText = status.value,
                            onUndo = { calls++ },
                            reducedMotion = { reduced },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
        resting = surfaceBounds(MESSAGE)
        if (both) referenceStatusBounds = surfaceBounds(STATUS)
        rule.runOnUiThread { reference.value = false }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
    }

    private fun show(statusMessage: Boolean, reduced: Boolean = false) {
        rule.runOnUiThread { if (statusMessage) status.value = MESSAGE else undo.value = MESSAGE }
        if (reduced) rule.mainClock.advanceTimeByFrame() else startAnimation()
        rule.waitForIdle()
    }

    private fun startAnimation() {
        // Recompose the target, then establish time zero on the next animation frame.
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
    }

    private fun entry(statusMessage: Boolean) {
        host(statusMessage)
        show(statusMessage)
        val initial = surfaceBounds(MESSAGE)
        assertEquals("Entry starts 8 dp below rest", resting.top + 8f * density, initial.top, 1f)
        assertColor(background, pixel(initial), "entry at 0 ms")
        rule.mainClock.advanceTimeBy(96)
        val middle = surfaceBounds(MESSAGE)
        assertTrue("Entry top lies between start and rest: $middle vs $resting", middle.top > resting.top && middle.top < initial.top)
        assertBlended(pixel(middle), "entry at 96 ms")
        rule.mainClock.advanceTimeBy(112)
        assertBounds(resting, surfaceBounds(MESSAGE))
        assertColor(surface, pixel(resting), "entry after 200 ms")
    }

    private fun exit(statusMessage: Boolean) {
        host(statusMessage)
        show(statusMessage)
        rule.mainClock.advanceTimeBy(208)
        val button = if (!statusMessage) rule.onNodeWithText("Undo").fetchSemanticsNode().boundsInRoot.center else null
        rule.runOnUiThread { if (statusMessage) status.value = null else undo.value = null }
        startAnimation()
        rule.mainClock.advanceTimeBy(96)
        assertBlended(pixel(resting), "exit at 96 ms must retain the old drawing")
        rule.onNodeWithText(MESSAGE).assertDoesNotExist()
        if (button != null) {
            rootClick(button + Offset(0f, 8f * density))
            assertEquals("Leaving Undo must be inert", 0, calls)
        }
        rule.mainClock.advanceTimeBy(112)
        assertColor(background, pixel(resting), "exit after 200 ms")
        rule.onNodeWithText(MESSAGE).assertDoesNotExist()
    }

    private fun immediate(statusMessage: Boolean) {
        host(statusMessage, reduced = true)
        show(statusMessage, reduced = true)
        assertBounds(resting, surfaceBounds(MESSAGE))
        assertColor(surface, pixel(resting), "reduced entry first frame")
        rule.runOnUiThread { if (statusMessage) status.value = null else undo.value = null }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText(MESSAGE).assertDoesNotExist()
        assertColor(background, pixel(resting), "reduced exit first frame")
    }

    private fun surfaceNode(message: String) = rule.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and hasAnyDescendant(hasText(message)),
        useUnmergedTree = true,
    )

    private fun surfaceBounds(message: String) = surfaceNode(message).fetchSemanticsNode().boundsInRoot

    private fun pixel(bounds: Rect): Color {
        val pixels = rule.onNodeWithTag(HOST).captureToImage().toPixelMap()
        // Inside the straight left edge, away from text, rounded corners and the 1 dp border.
        return pixels[(bounds.left + 4f * density).toInt(), bounds.center.y.toInt()]
    }

    private fun rootClick(point: Offset) {
        rule.onNodeWithTag(HOST).performTouchInput { click(point) }
    }

    private fun assertBounds(expected: Rect, actual: Rect) {
        assertEquals("Resting left", expected.left, actual.left, 1f)
        assertEquals("Resting top", expected.top, actual.top, 1f)
        assertEquals("Resting right", expected.right, actual.right, 1f)
        assertEquals("Resting bottom", expected.bottom, actual.bottom, 1f)
    }

    private fun assertColor(expected: Color, actual: Color, phase: String) {
        assertTrue("$phase: expected=$expected actual=$actual", distance(expected, actual) < 0.015f)
    }

    private fun assertBlended(actual: Color, phase: String) {
        for ((bg, fg, value) in listOf(
            Triple(background.red, surface.red, actual.red),
            Triple(background.green, surface.green, actual.green),
            Triple(background.blue, surface.blue, actual.blue),
        )) {
            val fraction = (value - bg) / (fg - bg)
            assertTrue("$phase: expected a blended surface, actual=$actual fraction=$fraction", fraction > 0.01f && fraction < 0.99f)
        }
    }

    private fun distance(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)

    private companion object {
        const val HOST = "toast-motion-host"
        const val MESSAGE = "Saved this article to Read Later"
        const val STATUS = "Export completed."
    }
}
