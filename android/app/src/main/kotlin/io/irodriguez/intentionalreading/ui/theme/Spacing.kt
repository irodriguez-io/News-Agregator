package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class IntentionalReadingSpacingScale(
    val baseUnit: Dp,
    val stackGap: Dp,
    val gutter: Dp,
    val mobileMargin: Dp,
    val tabletMargin: Dp,
    val sectionGap: Dp,
    val contentMaxWidth: Dp,
)

// Deliberately wrong RED values. The failing test commit proves the complete rhythm by value.
val IntentionalReadingSpacing = IntentionalReadingSpacingScale(
    baseUnit = 2.dp,
    stackGap = 8.dp,
    gutter = 12.dp,
    mobileMargin = 16.dp,
    tabletMargin = 20.dp,
    sectionGap = 24.dp,
    contentMaxWidth = 640.dp,
)

val LocalIntentionalReadingSpacing = staticCompositionLocalOf<IntentionalReadingSpacingScale> {
    error("Intentional Reading spacing was not provided")
}
