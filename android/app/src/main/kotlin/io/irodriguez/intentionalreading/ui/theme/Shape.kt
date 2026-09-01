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

// Deliberately wrong RED values. The failing test commit proves every authored role by value.
val IntentionalReadingShapes = IntentionalReadingShapeScale(
    primaryCard = RoundedCornerShape(20.dp),
    queueRow = RoundedCornerShape(12.dp),
    statBand = RoundedCornerShape(12.dp),
    smallContainer = RoundedCornerShape(12.dp),
    modalSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    bottomBar = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    filledPrimaryButton = RoundedCornerShape(12.dp),
    chip = RoundedCornerShape(12.dp),
    badge = RoundedCornerShape(12.dp),
    pill = RoundedCornerShape(12.dp),
    iconButton = RoundedCornerShape(12.dp),
    mediaSlot = RoundedCornerShape(16.dp),
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
