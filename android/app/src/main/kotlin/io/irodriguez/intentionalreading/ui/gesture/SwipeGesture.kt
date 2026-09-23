package io.irodriguez.intentionalreading.ui.gesture

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.PathEasing
import androidx.compose.ui.graphics.Path
import kotlin.math.abs

object SwipeGesture {
    const val THRESHOLD_DP = 90f
    const val INTENT_SLOP_DP = 8f
    const val HORIZONTAL_BIAS = 1.15f
    const val ROTATION_DIVISOR = 34f
    const val MAX_ROTATION_DEGREES = 4.5f
    const val EXIT_FRACTION = 0.82f
    const val EXIT_MINIMUM_DP = 620f
    // docs/v1/06-ui-ux.md §44.2 — 023 D1 uses item 021's 300ms Emphasized motion.
    const val EXIT_DURATION_MS = 300

    // docs/v1/06-ui-ux.md §79.5 — the replacement rises and fades into place over 300ms.
    const val ENTRANCE_DURATION_MS = 300
    // docs/v1/06-ui-ux.md §79.5 — 023 D3 starts the replacement 12dp below its resting position.
    const val ENTRANCE_RISE_DP = 12f

    // docs/v1/06-ui-ux.md §44.2 — Material 3 Emphasized, copied from item 021's destination transition.
    internal object ExitEmphasizedEasing : Easing {
        // Defer the Android Path until animation; selecting a spec needs no Android runtime.
        private val pathEasing by lazy {
            PathEasing(
                Path().apply {
                    moveTo(0f, 0f)
                    cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
                    cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
                },
            )
        }

        override fun transform(fraction: Float): Float = pathEasing.transform(fraction)
    }

    enum class Intent {
        PENDING,
        HORIZONTAL,
        VERTICAL,
    }

    enum class Action {
        DISMISS,
        SAVE,
    }

    class State(
        private val thresholdPx: Float,
        private val intentSlopPx: Float,
        private val viewportWidthPx: Float,
        private val exitMinimumPx: Float,
        private val reducedMotion: Boolean,
    ) {
        var intent: Intent = Intent.PENDING
            private set

        var translationX: Float = 0f
            private set

        val rotationDegrees: Float
            get() = if (reducedMotion) {
                0f
            } else {
                (translationX / ROTATION_DIVISOR).coerceIn(
                    minimumValue = -MAX_ROTATION_DEGREES,
                    maximumValue = MAX_ROTATION_DEGREES,
                )
            }

        var exitTranslationX: Float = 0f
            private set

        // docs/v1/06-ui-ux.md §79.5 / §48 — fade only a departing card with motion enabled.
        val alpha: Float
            get() = if (reducedMotion || exitTranslationX == 0f) 1f else 0f

        var commitInFlight: Boolean = false
            private set

        var committedAction: Action? = null
            private set

        private var tracking = false
        private var startX = 0f
        private var startY = 0f

        fun down(x: Float, y: Float): Boolean {
            if (commitInFlight) return false

            reset()
            tracking = true
            startX = x
            startY = y
            return true
        }

        fun move(x: Float, y: Float): Boolean {
            if (!tracking || commitInFlight) return false

            val horizontalTravel = x - startX
            val verticalTravel = y - startY

            if (
                intent == Intent.PENDING &&
                (abs(horizontalTravel) > intentSlopPx || abs(verticalTravel) > intentSlopPx)
            ) {
                intent = if (abs(horizontalTravel) > abs(verticalTravel) * HORIZONTAL_BIAS) {
                    Intent.HORIZONTAL
                } else {
                    Intent.VERTICAL
                }
            }

            if (intent != Intent.HORIZONTAL) return false

            translationX = horizontalTravel
            return true
        }

        fun release(): Action? {
            if (!tracking || commitInFlight) return null

            tracking = false
            if (intent != Intent.HORIZONTAL || abs(translationX) < thresholdPx) {
                restore()
                return null
            }

            val action = if (translationX < 0f) Action.DISMISS else Action.SAVE
            val direction = if (action == Action.DISMISS) -1f else 1f
            commitInFlight = true
            committedAction = action
            exitTranslationX = if (reducedMotion) {
                0f
            } else {
                maxOf(viewportWidthPx * EXIT_FRACTION, exitMinimumPx) * direction
            }
            translationX = exitTranslationX
            return action
        }

        fun cancel(): Action? {
            if (tracking && !commitInFlight) restore()
            return null
        }

        fun releaseCommitLock() {
            commitInFlight = false
        }

        fun restore() {
            reset()
            commitInFlight = false
            committedAction = null
        }

        private fun reset() {
            tracking = false
            intent = Intent.PENDING
            translationX = 0f
            exitTranslationX = 0f
        }
    }
}
