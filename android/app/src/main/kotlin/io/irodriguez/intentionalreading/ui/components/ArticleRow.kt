package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = tokens.border)
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = position,
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.fg,
                )
                if (!positionDetail.isNullOrEmpty()) {
                    Text(
                        text = positionDetail,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = tokens.muted,
                    )
                }
            }
            if (kicker.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    kicker.forEach { part ->
                        Text(
                            text = part.text.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (part.emphasized) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (part.emphasized) tokens.fg else tokens.muted,
                        )
                    }
                }
            }
            Text(
                text = articleTitle,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, lineHeight = 27.sp),
                color = tokens.fg,
            )
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.take(3).forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = tokens.surface,
                            contentColor = tokens.muted,
                            border = BorderStroke(1.dp, tokens.border),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                actions.forEach { action ->
                    TextButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .semantics {
                                contentDescription = "${action.label} for $articleTitle"
                            },
                        colors = ButtonDefaults.textButtonColors(contentColor = tokens.fg),
                    ) {
                        Text(action.label)
                    }
                }
            }
        }
    }
}

internal fun queuePositionLabel(position: Int): String = "Queue ${position.toString().padStart(2, '0')}"

internal fun historyGroupCount(count: Int): String = "$count ${if (count == 1) "article" else "articles"}"
