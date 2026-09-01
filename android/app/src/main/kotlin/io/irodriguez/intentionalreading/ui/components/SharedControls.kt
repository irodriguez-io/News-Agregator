package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingShapeScale
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingSpacingScale
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTokens
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Immutable
internal data class SharedControlLayout(
    val filledPrimaryHeight: Dp,
    val minimumTouchTarget: Dp,
    val triageSize: Dp,
    val triageOutlineWidth: Dp,
    val filledPrimaryShape: Shape,
    val triageShape: Shape,
)

@Immutable
internal data class SharedControlColors(
    val primaryFill: Color,
    val primaryLabel: Color,
    val tonalFill: Color,
    val tonalLabel: Color,
    val triageOutline: Color,
)

@Immutable
internal data class SharedControlStateValues(
    val pressedOverlayAlpha: Float,
    val pressedScale: Float,
    val disabledOpacity: Float,
)

internal val SharedControlState = SharedControlStateValues(
    pressedOverlayAlpha = 0.12f,
    pressedScale = 0.95f,
    disabledOpacity = 0.38f,
)

internal fun sharedControlLayout(
    spacing: IntentionalReadingSpacingScale,
    shapes: IntentionalReadingShapeScale,
): SharedControlLayout = SharedControlLayout(
    filledPrimaryHeight = spacing.sectionGap + spacing.gutter + spacing.baseUnit,
    minimumTouchTarget = spacing.sectionGap + spacing.gutter,
    triageSize = spacing.sectionGap + spacing.tabletMargin,
    triageOutlineWidth = spacing.baseUnit * 3f / 8f,
    filledPrimaryShape = shapes.filledPrimaryButton,
    triageShape = shapes.iconButton,
)

internal fun sharedControlColors(tokens: IntentionalReadingTokens): SharedControlColors =
    SharedControlColors(
        primaryFill = tokens.primary,
        primaryLabel = tokens.onPrimary,
        tonalFill = tokens.tonal,
        tonalLabel = tokens.onTonal,
        triageOutline = tokens.secondary,
    )

internal fun triageAccessibleName(accessibleName: String): String {
    require(accessibleName.isNotBlank()) { "A circular triage control requires an accessible name" }
    return accessibleName
}

internal fun isSharedControlInteractive(enabled: Boolean): Boolean = enabled

@Composable
fun FilledPrimaryControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val layout = sharedControlLayout(
        spacing = LocalIntentionalReadingSpacing.current,
        shapes = LocalIntentionalReadingShapes.current,
    )
    val colors = sharedControlColors(tokens)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val interactive = isSharedControlInteractive(enabled)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(layout.filledPrimaryHeight)
            .sharedControlState(
                shape = layout.filledPrimaryShape,
                overlayColor = colors.primaryLabel,
                pressed = pressed,
                enabled = interactive,
            ),
        enabled = interactive,
        shape = layout.filledPrimaryShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primaryFill,
            contentColor = colors.primaryLabel,
            disabledContainerColor = colors.primaryFill,
            disabledContentColor = colors.primaryLabel,
        ),
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun TonalSecondaryControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val layout = sharedControlLayout(
        spacing = LocalIntentionalReadingSpacing.current,
        shapes = LocalIntentionalReadingShapes.current,
    )
    val colors = sharedControlColors(tokens)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val interactive = isSharedControlInteractive(enabled)

    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = layout.minimumTouchTarget)
            .sharedControlState(
                shape = layout.filledPrimaryShape,
                overlayColor = colors.tonalLabel,
                pressed = pressed,
                enabled = interactive,
            ),
        enabled = interactive,
        shape = layout.filledPrimaryShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.tonalFill,
            contentColor = colors.tonalLabel,
            disabledContainerColor = colors.tonalFill,
            disabledContentColor = colors.tonalLabel,
        ),
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun CircularTriageControl(
    accessibleName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val layout = sharedControlLayout(
        spacing = LocalIntentionalReadingSpacing.current,
        shapes = LocalIntentionalReadingShapes.current,
    )
    val colors = sharedControlColors(tokens)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val interactive = isSharedControlInteractive(enabled)
    val contentDescription = triageAccessibleName(accessibleName)

    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier
            .size(layout.triageSize)
            .semantics { this.contentDescription = contentDescription }
            .sharedControlState(
                shape = layout.triageShape,
                overlayColor = colors.triageOutline,
                pressed = pressed,
                enabled = interactive,
            ),
        enabled = interactive,
        shape = layout.triageShape,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = colors.triageOutline,
            disabledContentColor = colors.triageOutline,
        ),
        border = BorderStroke(layout.triageOutlineWidth, colors.triageOutline),
        interactionSource = interactionSource,
        content = content,
    )
}

private fun Modifier.sharedControlState(
    shape: Shape,
    overlayColor: Color,
    pressed: Boolean,
    enabled: Boolean,
): Modifier = graphicsLayer {
    val scale = if (pressed) SharedControlState.pressedScale else 1f
    scaleX = scale
    scaleY = scale
    alpha = if (enabled) 1f else SharedControlState.disabledOpacity
    this.shape = shape
    clip = true
}.drawWithContent {
    drawContent()
    if (pressed) {
        drawRect(overlayColor.copy(alpha = SharedControlState.pressedOverlayAlpha))
    }
}
