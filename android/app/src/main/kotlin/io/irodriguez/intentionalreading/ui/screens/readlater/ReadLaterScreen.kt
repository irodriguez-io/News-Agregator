package io.irodriguez.intentionalreading.ui.screens.readlater

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.ui.components.ArticleKickerPart
import io.irodriguez.intentionalreading.ui.components.ArticleRow
import io.irodriguez.intentionalreading.ui.components.ArticleRowAction
import io.irodriguez.intentionalreading.ui.components.EditorialHeader
import io.irodriguez.intentionalreading.ui.components.EmptyStatePanel
import io.irodriguez.intentionalreading.ui.components.StatBand
import io.irodriguez.intentionalreading.ui.components.StatItem
import io.irodriguez.intentionalreading.ui.components.availableStatValue
import io.irodriguez.intentionalreading.ui.components.knownReadingTimeValue
import io.irodriguez.intentionalreading.ui.components.queuePositionLabel
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.format.RelativeTime

@Composable
fun ReadLaterScreen(
    state: ReadLaterUiState,
    onDiscover: () -> Unit,
    onReadArticle: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
    onRemove: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
    ) {
        item(key = "header") {
            EditorialHeader(
                eyebrow = stringResource(R.string.read_later_eyebrow),
                title = stringResource(R.string.read_later),
                description = stringResource(R.string.read_later_description),
                actionLabel = stringResource(R.string.discover_something_new),
                onAction = onDiscover,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }

        if (state.rows.isNotEmpty()) {
            item(key = "overview") {
                StatBand(
                    stats = listOf(
                        StatItem(stringResource(R.string.in_queue), state.aggregate.count.toString()),
                        StatItem(
                            stringResource(R.string.known_reading_time),
                            knownReadingTimeValue(state.aggregate.knownReadingTimeMinutes),
                        ),
                        StatItem(
                            stringResource(R.string.next_topic),
                            availableStatValue(state.aggregate.firstTagLabel),
                        ),
                    ),
                    modifier = Modifier.padding(bottom = 32.dp),
                )
            }
            itemsIndexed(
                items = state.rows,
                key = { _, row -> row.article.id },
            ) { index, row ->
                val article = row.article
                ArticleRow(
                    articleTitle = article.title,
                    position = queuePositionLabel(index + 1),
                    positionDetail = row.savedAge.takeIf { it.isNotEmpty() }?.let { "Saved $it" },
                    kicker = buildList {
                        article.source.name.takeIf { it.isNotEmpty() }?.let {
                            add(ArticleKickerPart(it, emphasized = true))
                        }
                        Labels.categoryLabel(article.category.id).takeIf { it.isNotEmpty() }?.let {
                            add(ArticleKickerPart(it))
                        }
                        RelativeTime.readingTime(article.readingTimeMinutes).takeIf { it.isNotEmpty() }?.let {
                            add(ArticleKickerPart(it))
                        }
                        article.contentType.label.takeIf { it.isNotEmpty() }?.let {
                            add(ArticleKickerPart(it))
                        }
                    },
                    tags = article.tags.take(3).map { it.label }.filter { it.isNotEmpty() },
                    actions = listOf(
                        ArticleRowAction(stringResource(R.string.read_external)) { onReadArticle(article) },
                        ArticleRowAction(stringResource(R.string.mark_read)) { onMarkRead(article) },
                        ArticleRowAction(stringResource(R.string.remove)) { onRemove(article) },
                    ),
                )
            }
        } else {
            item(key = "empty") {
                EmptyStatePanel(
                    title = stringResource(R.string.read_later_empty_title),
                    copy = stringResource(R.string.read_later_empty_copy),
                    actionLabel = stringResource(R.string.return_to_discover),
                    onAction = onDiscover,
                )
            }
        }
    }
}
