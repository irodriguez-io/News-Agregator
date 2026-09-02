package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

data class ArticleKickerPart(
    val text: String,
    val emphasized: Boolean = false,
)

data class ArticleRowAction(
    val label: String,
    val onClick: () -> Unit,
)

/** §72.2 — the accessibility floor for every interactive element. Never derived. */
internal val ArticleRowMinimumTarget = Dp(48f)

@Composable
fun ArticleRow(
    articleTitle: String,
    position: String,
    positionDetail: String?,
    kicker: List<ArticleKickerPart>,
    tags: List<String>,
    actions: List<ArticleRowAction>,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.queueRow,
        color = tokens.container,
        contentColor = tokens.fg,
    ) {
        Column(
            modifier = Modifier.padding(spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.baseUnit)) {
                Text(
                    text = position,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.fg,
                )
                if (!positionDetail.isNullOrEmpty()) {
                    Text(
                        text = positionDetail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.muted,
                    )
                }
            }
            if (kicker.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.baseUnit * 2),
                    verticalArrangement = Arrangement.spacedBy(spacing.baseUnit),
                ) {
                    kicker.forEach { part ->
                        Text(
                            text = part.text.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (part.emphasized) tokens.quiet else tokens.muted,
                        )
                    }
                }
            }
            Text(
                text = articleTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = tokens.fg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.baseUnit * 2),
                    verticalArrangement = Arrangement.spacedBy(spacing.baseUnit * 2),
                ) {
                    tags.take(3).forEach { tag ->
                        Surface(
                            shape = shapes.pill,
                            color = tokens.container,
                            contentColor = tokens.muted,
                            border = BorderStroke(Dp.Hairline, tokens.outlineVariant),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = spacing.baseUnit * 2,
                                    vertical = spacing.baseUnit,
                                ),
                            )
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.baseUnit * 2),
                verticalArrangement = Arrangement.spacedBy(spacing.baseUnit),
            ) {
                actions.forEachIndexed { index, action ->
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides ArticleRowMinimumTarget,
                    ) {
                        val actionModifier = Modifier
                            .sizeIn(
                                minWidth = ArticleRowMinimumTarget,
                                minHeight = ArticleRowMinimumTarget,
                            )
                            .semantics {
                                contentDescription = "${action.label} for $articleTitle"
                            }
                        if (index == 0) {
                            TonalSecondaryControl(
                                onClick = action.onClick,
                                modifier = actionModifier,
                            ) {
                                Text(action.label, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = action.onClick,
                                modifier = actionModifier,
                                shape = shapes.pill,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = tokens.quiet,
                                ),
                                border = BorderStroke(Dp.Hairline, tokens.outlineControl),
                            ) {
                                Text(action.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun queuePositionLabel(position: Int): String = "Queue ${position.toString().padStart(2, '0')}"

internal fun historyGroupCount(count: Int): String = "$count ${if (count == 1) "article" else "articles"}"
