package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class IntentionalReadingShapeScale(
    val primaryCard: RoundedCornerShape,
    val queueRow: RoundedCornerShape,
    val statBand: RoundedCornerShape,
    val smallContainer: RoundedCornerShape,
    val modalSheet: RoundedCornerShape,
    val bottomBar: RoundedCornerShape,
    val filledPrimaryButton: RoundedCornerShape,
    val chip: RoundedCornerShape,
    val badge: RoundedCornerShape,
    val pill: RoundedCornerShape,
    val iconButton: RoundedCornerShape,
    val mediaSlot: RoundedCornerShape,
)

val IntentionalReadingShapes = IntentionalReadingShapeScale(
    primaryCard = RoundedCornerShape(24.dp),
    queueRow = RoundedCornerShape(16.dp),
    statBand = RoundedCornerShape(16.dp),
    smallContainer = RoundedCornerShape(16.dp),
    modalSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    bottomBar = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    filledPrimaryButton = RoundedCornerShape(16.dp),
    chip = CircleShape,
    badge = CircleShape,
    pill = CircleShape,
    iconButton = CircleShape,
    mediaSlot = RoundedCornerShape(20.dp),
)

val LocalIntentionalReadingShapes = staticCompositionLocalOf<IntentionalReadingShapeScale> {
    error("Intentional Reading shapes were not provided")
}

internal val IntentionalReadingMaterialShapes = Shapes(
    extraSmall = CircleShape,
    small = IntentionalReadingShapes.smallContainer,
    medium = IntentionalReadingShapes.filledPrimaryButton,
    large = IntentionalReadingShapes.primaryCard,
    extraLarge = IntentionalReadingShapes.modalSheet,
)
