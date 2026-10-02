package io.irodriguez.intentionalreading.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingSpacing

private val TabletReadingWidthThreshold = 600.dp

fun readingHorizontalPadding(available: Dp): Dp {
    val spacing = IntentionalReadingSpacing
    val margin = if (available >= TabletReadingWidthThreshold) spacing.tabletMargin else spacing.mobileMargin
    return max(margin, (available - spacing.contentMaxWidth) / 2)
}
