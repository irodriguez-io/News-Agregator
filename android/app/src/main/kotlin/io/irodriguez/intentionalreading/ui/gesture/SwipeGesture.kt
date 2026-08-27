package io.irodriguez.intentionalreading.ui.gesture

import kotlin.math.abs

object SwipeGesture {
    const val THRESHOLD_DP = 90f
    const val INTENT_SLOP_DP = 8f
    const val HORIZONTAL_BIAS = 1.15f
    const val ROTATION_DIVISOR = 34f
    const val MAX_ROTATION_DEGREES = 4.5f
    const val EXIT_FRACTION = 0.82f
    const val EXIT_MINIMUM_DP = 620f
    const val EXIT_DURATION_MS = 280

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
