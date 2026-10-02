package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
    val filledPrimaryShape: Shape,
    val triageShape: Shape,
)

@Immutable
internal data class SharedControlColors(
    val primaryFill: Color,
    val primaryLabel: Color,
    val tonalFill: Color,
    val tonalLabel: Color,
    val triageLabel: Color,
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

/** §32.2 — the filled primary control's specified height. */
private val FilledPrimaryHeight = Dp(52f)

/** §72.2 — the accessibility floor for every interactive element. Never derived. */
private val MinimumTouchTarget = Dp(48f)

internal fun sharedControlLayout(
    spacing: IntentionalReadingSpacingScale,
    shapes: IntentionalReadingShapeScale,
): SharedControlLayout = SharedControlLayout(
    filledPrimaryHeight = FilledPrimaryHeight,
    minimumTouchTarget = MinimumTouchTarget,
    filledPrimaryShape = shapes.filledPrimaryButton,
    triageShape = shapes.pill,
)

internal fun sharedControlColors(tokens: IntentionalReadingTokens): SharedControlColors =
    SharedControlColors(
        primaryFill = tokens.primary,
        primaryLabel = tokens.onPrimary,
        tonalFill = tokens.tonal,
        tonalLabel = tokens.onTonal,
        triageLabel = tokens.secondary,
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
fun InlineTriageControl(
    accessibleName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val spacing = LocalIntentionalReadingSpacing.current
    val layout = sharedControlLayout(
        spacing = spacing,
        shapes = LocalIntentionalReadingShapes.current,
    )
    val colors = sharedControlColors(tokens)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val interactive = isSharedControlInteractive(enabled)
    val name = triageAccessibleName(accessibleName)

    CompositionLocalProvider(LocalContentColor provides colors.triageLabel) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier = modifier
                    .sizeIn(minWidth = layout.minimumTouchTarget, minHeight = layout.minimumTouchTarget)
                    .sharedControlState(
                        shape = layout.triageShape,
                        overlayColor = colors.triageLabel,
                        pressed = pressed,
                        enabled = interactive,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = interactive,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .clearAndSetSemantics {
                        contentDescription = name
                        role = Role.Button
                        if (!interactive) disabled()
                        onClick {
                            if (interactive) onClick()
                            interactive
                        }
                    }
                    .padding(horizontal = spacing.baseUnit),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
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
