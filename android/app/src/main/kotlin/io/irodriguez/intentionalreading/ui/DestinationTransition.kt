package io.irodriguez.intentionalreading.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

internal enum class DestinationSlideDirection {
    FROM_LEFT,
    FROM_RIGHT,
}

internal fun destinationSlideDirection(
    current: Destination,
    target: Destination,
): DestinationSlideDirection {
    require(current != target) { "A destination transition requires two different destinations" }
    return if (target.ordinal < current.ordinal) {
        DestinationSlideDirection.FROM_LEFT
    } else {
        DestinationSlideDirection.FROM_RIGHT
    }
}

@Composable
internal fun DestinationTransition(
    destination: Destination,
    reducedMotion: () -> Boolean,
    modifier: Modifier,
    content: @Composable (Destination) -> Unit,
) {
    val emphasizedEasing = remember {
        PathEasing(
            Path().apply {
                moveTo(0f, 0f)
                cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
                cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
            },
        )
    }
    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            if (initialState == targetState || reducedMotion()) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val offsetMultiplier = when (
                    destinationSlideDirection(initialState, targetState)
                ) {
                    DestinationSlideDirection.FROM_LEFT -> -1
                    DestinationSlideDirection.FROM_RIGHT -> 1
                }
                slideInHorizontally(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = emphasizedEasing,
                    ),
                    initialOffsetX = { fullWidth -> offsetMultiplier * fullWidth },
                ) togetherWith (
                    scaleOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = emphasizedEasing,
                        ),
                        targetScale = 0.95f,
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = emphasizedEasing,
                        ),
                        targetAlpha = 0.8f,
                    )
                )
            }
        },
        modifier = modifier,
        label = "Destination transition",
    ) { targetDestination ->
        Box(Modifier.fillMaxSize().background(LocalIntentionalReadingTokens.current.bg)) {
            content(targetDestination)
        }
    }
}
