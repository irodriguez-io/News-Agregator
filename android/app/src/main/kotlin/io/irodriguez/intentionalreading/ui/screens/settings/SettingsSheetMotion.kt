@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.irodriguez.intentionalreading.ui.screens.settings

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

/** §79.2 — the complete sheet reveal and reverse tuck last 350 ms. */
internal const val SettingsSheetRevealDurationMillis = 350

internal fun <T> settingsSheetRevealSpec(reducedMotion: Boolean): FiniteAnimationSpec<T> =
    if (reducedMotion) {
        snap()
    } else {
        tween(
            durationMillis = SettingsSheetRevealDurationMillis,
            easing = LinearOutSlowInEasing,
        )
    }

private class SettingsSheetMotionScheme(
    private val base: MotionScheme,
    private val reducedMotion: Boolean,
) : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        settingsSheetRevealSpec(reducedMotion)

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else base.fastSpatialSpec()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else base.slowSpatialSpec()

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else base.defaultEffectsSpec()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else base.fastEffectsSpec()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else base.slowEffectsSpec()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSettingsSheetState(reducedMotion: Boolean): SheetState {
    val density = LocalDensity.current
    return remember(density, reducedMotion) {
        SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = {
                with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
            },
            velocityThreshold = {
                with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
            },
            initialValue = if (reducedMotion) SheetValue.Expanded else SheetValue.Hidden,
            confirmValueChange = { true },
            skipHiddenState = false,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsSheetMotionTheme(reducedMotion: Boolean, content: @Composable () -> Unit) {
    val baseMotionScheme = MaterialTheme.motionScheme
    val sheetMotionScheme = remember(baseMotionScheme, reducedMotion) {
        SettingsSheetMotionScheme(baseMotionScheme, reducedMotion)
    }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = sheetMotionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
