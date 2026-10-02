package io.irodriguez.intentionalreading.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

private val ToastStandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun ToastRegion(
    undoToastMessage: String?,
    announcementText: String?,
    onUndo: () -> Unit,
    reducedMotion: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToastSlot(message = undoToastMessage, reducedMotion = reducedMotion) { message ->
            UndoToast(message = message, onUndo = onUndo)
        }
        ToastSlot(message = announcementText, reducedMotion = reducedMotion) { message ->
            LiveStatusMessage(message = message)
        }
    }
}

@Composable
private fun ToastSlot(
    message: String?,
    reducedMotion: () -> Boolean,
    content: @Composable (String) -> Unit,
) {
    val immediate = reducedMotion()
    val travel = with(LocalDensity.current) { 8.dp.roundToPx() }
    val transition = updateTransition(targetState = message, label = "Toast message")
    // Empty slots must not add Column spacing; keep a leaving slot until its exit finishes.
    if (message != null || (!immediate && (transition.currentState != null || transition.isRunning))) {
        transition.AnimatedContent(
            transitionSpec = {
                if (immediate) {
                    (EnterTransition.None togetherWith ExitTransition.None).using(null)
                } else {
                    (fadeIn(tween(200, easing = ToastStandardEasing)) +
                        slideInVertically(tween(200, easing = ToastStandardEasing)) { travel } togetherWith
                        fadeOut(tween(200, easing = ToastStandardEasing)) +
                        slideOutVertically(tween(200, easing = ToastStandardEasing)) { travel }).using(null)
                }
            },
            contentAlignment = Alignment.BottomCenter,
        ) { currentMessage ->
            if (currentMessage != null) {
                val leaving = currentMessage != message
                Box(
                    modifier = if (leaving) {
                        Modifier.clearAndSetSemantics {}.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ) {
                    content(currentMessage)
                }
            }
        }
    }
}
