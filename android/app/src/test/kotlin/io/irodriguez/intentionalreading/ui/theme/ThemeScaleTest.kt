package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeScaleTest {
    @Test
    fun `shape scale carries every authored semantic shape`() {
        assertEquals(
            listOf(
                RoundedCornerShape(24.dp),
                RoundedCornerShape(16.dp),
                RoundedCornerShape(16.dp),
                RoundedCornerShape(16.dp),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                RoundedCornerShape(16.dp),
                CircleShape,
                CircleShape,
                CircleShape,
                CircleShape,
                RoundedCornerShape(20.dp),
            ),
            IntentionalReadingShapes.run {
                listOf(
                    primaryCard,
                    queueRow,
                    statBand,
                    smallContainer,
                    modalSheet,
                    bottomBar,
                    filledPrimaryButton,
                    chip,
                    badge,
                    pill,
                    iconButton,
                    mediaSlot,
                )
            },
        )
    }

    @Test
    fun `Material shape slots are explicitly mapped from the semantic scale`() {
        assertEquals(CircleShape, IntentionalReadingMaterialShapes.extraSmall)
        assertEquals(IntentionalReadingShapes.smallContainer, IntentionalReadingMaterialShapes.small)
        assertEquals(IntentionalReadingShapes.filledPrimaryButton, IntentionalReadingMaterialShapes.medium)
        assertEquals(IntentionalReadingShapes.primaryCard, IntentionalReadingMaterialShapes.large)
        assertEquals(IntentionalReadingShapes.modalSheet, IntentionalReadingMaterialShapes.extraLarge)
    }

    @Test
    fun `spacing rhythm carries every authored value`() {
        assertEquals(
            listOf(4.dp, 12.dp, 16.dp, 18.dp, 24.dp, 32.dp, 680.dp),
            IntentionalReadingSpacing.run {
                listOf(
                    baseUnit,
                    stackGap,
                    gutter,
                    mobileMargin,
                    tabletMargin,
                    sectionGap,
                    contentMaxWidth,
                )
            },
        )
    }
}
