package io.irodriguez.intentionalreading.ui.gesture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwipeGestureTest {
    @Test
    fun `a touch that has barely moved locks no intent`() {
        // Given
        val gesture = gesture()
        assertTrue(gesture.down(x = 100f, y = 200f))

        // When
        val shouldConsume = gesture.move(x = 107f, y = 207f)
        val action = gesture.release()

        // Then
        assertFalse(shouldConsume)
        assertEquals(SwipeGesture.Intent.PENDING, gesture.intent)
        assertEquals(0f, gesture.translationX)
        assertNull(action)
    }

    @Test
    fun `a mostly-vertical drag never becomes a swipe`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)

        // When
        val shouldConsume = gesture.move(x = 9f, y = 10f)

        // Then
        assertFalse(shouldConsume)
        assertEquals(SwipeGesture.Intent.VERTICAL, gesture.intent)
        assertEquals(0f, gesture.translationX)
    }

    @Test
    fun `a decisively horizontal drag locks horizontal`() {
        // Given
        val horizontal = gesture().also { it.down(x = 0f, y = 0f) }
        val vertical = gesture().also { it.down(x = 0f, y = 0f) }

        // When
        val horizontalConsumes = horizontal.move(x = 10f, y = 8.5f)
        val verticalConsumes = vertical.move(x = 10f, y = 9f)

        // Then
        assertEquals(1.15f, SwipeGesture.HORIZONTAL_BIAS)
        assertTrue(horizontalConsumes)
        assertEquals(SwipeGesture.Intent.HORIZONTAL, horizontal.intent)
        assertEquals(10f, horizontal.translationX)
        assertFalse(verticalConsumes)
        assertEquals(SwipeGesture.Intent.VERTICAL, vertical.intent)
        assertEquals(0f, vertical.translationX)
    }

    @Test
    fun `intent is locked once and does not change mid-gesture`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        assertFalse(gesture.move(x = 1f, y = 10f))

        // When
        val shouldConsume = gesture.move(x = 100f, y = 1f)

        // Then
        assertFalse(shouldConsume)
        assertEquals(SwipeGesture.Intent.VERTICAL, gesture.intent)
        assertEquals(0f, gesture.translationX)
    }

    @Test
    fun `rotation follows travel and is clamped`() {
        // Given
        val proportional = gesture().also { it.down(x = 0f, y = 0f) }
        val positive = gesture().also { it.down(x = 0f, y = 0f) }
        val negative = gesture().also { it.down(x = 0f, y = 0f) }

        // When
        proportional.move(x = 34f, y = 0f)
        positive.move(x = 200f, y = 0f)
        negative.move(x = -200f, y = 0f)

        // Then
        assertEquals(34f, SwipeGesture.ROTATION_DIVISOR)
        assertEquals(4.5f, SwipeGesture.MAX_ROTATION_DEGREES)
        assertEquals(1f, proportional.rotationDegrees)
        assertEquals(4.5f, positive.rotationDegrees)
        assertEquals(-4.5f, negative.rotationDegrees)
    }

    @Test
    fun `releasing short of the threshold changes nothing`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP - 1f, y = 0f)

        // When
        val action = gesture.release()

        // Then
        assertEquals(90f, SwipeGesture.THRESHOLD_DP)
        assertNull(action)
        assertEquals(SwipeGesture.Intent.PENDING, gesture.intent)
        assertEquals(0f, gesture.translationX)
        assertEquals(0f, gesture.rotationDegrees)
        assertEquals(0f, gesture.exitTranslationX)
        assertFalse(gesture.commitInFlight)
    }

    @Test
    fun `releasing at or past the threshold emits the direction's action`() {
        // Given
        val left = gesture(viewportWidthPx = 1_000f).also {
            it.down(x = 0f, y = 0f)
            it.move(x = -SwipeGesture.THRESHOLD_DP, y = 0f)
        }
        val right = gesture(viewportWidthPx = 500f).also {
            it.down(x = 0f, y = 0f)
            it.move(x = SwipeGesture.THRESHOLD_DP + 1f, y = 0f)
        }

        // When
        val leftAction = left.release()
        val rightAction = right.release()

        // Then
        assertEquals(SwipeGesture.Action.DISMISS, leftAction)
        assertEquals(SwipeGesture.Action.SAVE, rightAction)
        assertEquals(0.82f, SwipeGesture.EXIT_FRACTION)
        assertEquals(620f, SwipeGesture.EXIT_MINIMUM_DP)
        assertEquals(280, SwipeGesture.EXIT_DURATION_MS)
        assertEquals(-820f, left.exitTranslationX)
        assertEquals(620f, right.exitTranslationX)
    }

    @Test
    fun `a cancelled gesture restores whatever its travel was`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP + 1f, y = 0f)

        // When
        val action = gesture.cancel()

        // Then
        assertNull(action)
        assertEquals(SwipeGesture.Intent.PENDING, gesture.intent)
        assertEquals(0f, gesture.translationX)
        assertEquals(0f, gesture.rotationDegrees)
        assertEquals(0f, gesture.exitTranslationX)
        assertFalse(gesture.commitInFlight)
    }

    @Test
    fun `a second gesture is refused while a commit is in flight`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP, y = 0f)
        assertEquals(SwipeGesture.Action.SAVE, gesture.release())

        // When
        val accepted = gesture.down(x = 10f, y = 10f)
        val shouldConsume = gesture.move(x = -200f, y = 0f)
        val secondAction = gesture.release()

        // Then
        assertFalse(accepted)
        assertFalse(shouldConsume)
        assertNull(secondAction)
        assertTrue(gesture.commitInFlight)
        assertEquals(SwipeGesture.Action.SAVE, gesture.committedAction)
    }

    @Test
    fun `reduced motion removes the rotation and the exit travel`() {
        // Given
        val gesture = gesture(reducedMotion = true)
        gesture.down(x = 0f, y = 0f)

        // When
        gesture.move(x = SwipeGesture.THRESHOLD_DP, y = 0f)
        val rotationDuringDrag = gesture.rotationDegrees
        val action = gesture.release()

        // Then
        assertEquals(0f, rotationDuringDrag)
        assertEquals(0f, gesture.rotationDegrees)
        assertEquals(0f, gesture.exitTranslationX)
        assertEquals(SwipeGesture.Action.SAVE, action)
    }

    @Test
    fun `a commit resolved as persisted accepts and commits a new horizontal gesture`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP, y = 0f)
        assertEquals(SwipeGesture.Action.SAVE, gesture.release())

        // When
        gesture.releaseCommitLock()
        val accepted = gesture.down(x = 0f, y = 0f)
        val shouldConsume = gesture.move(x = -SwipeGesture.THRESHOLD_DP, y = 0f)
        val action = gesture.release()

        // Then
        assertTrue(accepted)
        assertTrue(shouldConsume)
        assertEquals(SwipeGesture.Intent.HORIZONTAL, gesture.intent)
        assertEquals(SwipeGesture.Action.DISMISS, action)
    }

    @Test
    fun `a commit resolved as not persisted releases the lock and restores the travel`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP, y = 0f)
        assertEquals(SwipeGesture.Action.SAVE, gesture.release())

        // When
        gesture.releaseCommitLock()
        val lockReleased = !gesture.commitInFlight
        gesture.restore()

        // Then
        assertTrue(lockReleased)
        assertEquals(SwipeGesture.Intent.PENDING, gesture.intent)
        assertEquals(0f, gesture.translationX)
        assertEquals(0f, gesture.exitTranslationX)
    }

    @Test
    fun `a commit resolved as persisted releases only the lock and leaves travel untouched`() {
        // Given
        val gesture = gesture(viewportWidthPx = 1_000f)
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP, y = 0f)
        assertEquals(SwipeGesture.Action.SAVE, gesture.release())
        val translationX = gesture.translationX
        val exitTranslationX = gesture.exitTranslationX

        // When
        gesture.releaseCommitLock()

        // Then
        assertFalse(gesture.commitInFlight)
        assertEquals(translationX, gesture.translationX)
        assertEquals(exitTranslationX, gesture.exitTranslationX)
    }

    @Test
    fun `a commit resolved as persisted retains its action and a stationary release emits nothing`() {
        // Given
        val gesture = gesture()
        gesture.down(x = 0f, y = 0f)
        gesture.move(x = SwipeGesture.THRESHOLD_DP, y = 0f)
        assertEquals(SwipeGesture.Action.SAVE, gesture.release())

        // When
        gesture.releaseCommitLock()
        val committedAction = gesture.committedAction
        assertTrue(gesture.down(x = 0f, y = 0f))
        val stationaryAction = gesture.release()

        // Then
        assertEquals(SwipeGesture.Action.SAVE, committedAction)
        assertNull(stationaryAction)
    }

    private fun gesture(
        viewportWidthPx: Float = 1_000f,
        reducedMotion: Boolean = false,
    ) = SwipeGesture.State(
        thresholdPx = SwipeGesture.THRESHOLD_DP,
        intentSlopPx = SwipeGesture.INTENT_SLOP_DP,
        viewportWidthPx = viewportWidthPx,
        exitMinimumPx = SwipeGesture.EXIT_MINIMUM_DP,
        reducedMotion = reducedMotion,
    )
}
