package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.format.RelativeTime
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

@Composable
fun ArticleCard(
    state: DiscoverUiState.Card,
    onDismiss: (Article) -> Unit,
    onReadArticle: (Article) -> Unit,
    onSave: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val article = state.article
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = tokens.surface,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.border),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ArticleMetadata(article = article, publicationAge = state.publicationAge)

            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineLarge,
                color = tokens.fg,
            )

            if (article.excerpt.isNotEmpty()) {
                Text(
                    text = article.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.quietInk,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (article.tags.isNotEmpty()) {
                TopicTags(article)
            }

            if (state.isOpened) {
                OpenedAcknowledgment()
            }

            ArticleActions(
                article = article,
                isOpened = state.isOpened,
                onDismiss = onDismiss,
                onReadArticle = onReadArticle,
                onSave = onSave,
                onMarkRead = onMarkRead,
            )
        }
    }
}

@Composable
private fun ArticleMetadata(article: Article, publicationAge: String) {
    val tokens = LocalIntentionalReadingTokens.current
    val readingTime = RelativeTime.readingTime(article.readingTimeMinutes)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataText(article.source.name)
        if (publicationAge.isNotEmpty()) MetadataText(publicationAge)
        Surface(
            shape = CircleShape,
            color = tokens.accentSoft,
            contentColor = tokens.accent,
        ) {
            Text(
                text = article.contentType.label.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
        MetadataText(Labels.categoryLabel(article.category.id))
        if (readingTime.isNotEmpty()) MetadataText(readingTime)
    }
}

@Composable
private fun MetadataText(text: String) {
    val tokens = LocalIntentionalReadingTokens.current
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium,
        color = tokens.muted,
    )
}

@Composable
private fun TopicTags(article: Article) {
    val tokens = LocalIntentionalReadingTokens.current
    val topicsDescription = stringResource(R.string.article_topics)
    FlowRow(
        modifier = Modifier.semantics {
            contentDescription = "$topicsDescription: ${article.tags.take(5).joinToString { it.label }}"
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        article.tags.take(5).forEach { tag ->
            Surface(
                shape = CircleShape,
                color = tokens.surface,
                contentColor = tokens.muted,
                border = BorderStroke(1.dp, tokens.border),
            ) {
                Text(
                    text = tag.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun OpenedAcknowledgment() {
    val tokens = LocalIntentionalReadingTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = tokens.surfaceHover,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.opened).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium,
                color = tokens.fg,
            )
            Text(
                text = stringResource(R.string.opened_acknowledgment),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.muted,
            )
        }
    }
}

@Composable
private fun ArticleActions(
    article: Article,
    isOpened: Boolean,
    onDismiss: (Article) -> Unit,
    onReadArticle: (Article) -> Unit,
    onSave: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val externalDescription = stringResource(R.string.read_article_external)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RoundTriageAction(
                arrow = "←",
                label = stringResource(R.string.not_interested),
                onClick = { onDismiss(article) },
            )
            Button(
                onClick = { onReadArticle(article) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = externalDescription },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.surface,
                ),
            ) {
                Text(stringResource(R.string.read_article))
                Spacer(Modifier.width(6.dp))
                Text("↗")
            }
            RoundTriageAction(
                arrow = "→",
                label = stringResource(R.string.save_for_later),
                onClick = { onSave(article) },
            )
        }
        if (isOpened) {
            OutlinedButton(
                onClick = { onMarkRead(article) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, tokens.strongBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.fg),
            ) {
                Text(stringResource(R.string.mark_read))
            }
        }
    }
}

@Composable
private fun RoundTriageAction(
    arrow: String,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .semantics { contentDescription = label },
            border = BorderStroke(1.dp, tokens.strongBorder),
        ) {
            Text(
                text = arrow,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.fg,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = tokens.fg,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
