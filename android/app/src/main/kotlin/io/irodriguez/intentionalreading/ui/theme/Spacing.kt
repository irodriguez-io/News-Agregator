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

val IntentionalReadingSpacing = IntentionalReadingSpacingScale(
    baseUnit = 4.dp,
    stackGap = 12.dp,
    gutter = 16.dp,
    mobileMargin = 18.dp,
    tabletMargin = 24.dp,
    sectionGap = 32.dp,
    contentMaxWidth = 680.dp,
)

val LocalIntentionalReadingSpacing = staticCompositionLocalOf<IntentionalReadingSpacingScale> {
    error("Intentional Reading spacing was not provided")
}
