package io.irodriguez.intentionalreading.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.irodriguez.intentionalreading.ui.components.historyGroupCount
import io.irodriguez.intentionalreading.ui.components.knownReadingTimeValue
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.format.RelativeTime
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onReadLater: () -> Unit,
    onDiscover: () -> Unit,
    onReopen: (Article) -> Unit,
    onMarkUnread: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
    ) {
        item(key = "header") {
            EditorialHeader(
                eyebrow = stringResource(R.string.history_eyebrow),
                title = stringResource(R.string.history),
                description = stringResource(R.string.history_description),
                actionLabel = stringResource(R.string.return_to_read_later),
                onAction = onReadLater,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }

        if (state.groups.isNotEmpty()) {
            item(key = "overview") {
                StatBand(
                    stats = listOf(
                        StatItem(stringResource(R.string.articles_read), state.aggregate.count.toString()),
                        StatItem(
                            stringResource(R.string.known_reading_time),
                            knownReadingTimeValue(state.aggregate.knownReadingTimeMinutes),
                        ),
                        StatItem(
                            stringResource(R.string.latest_topic),
                            availableStatValue(state.aggregate.firstTagLabel),
                        ),
                    ),
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }
            state.groups.forEach { group ->
                item(key = "heading-${group.period.name}") {
                    HistoryGroupHeading(
                        period = group.period,
                        count = group.rows.size,
                    )
                }
                group.rows.forEach { row ->
                    item(key = row.article.id) {
                        val article = row.article
                        ArticleRow(
                            articleTitle = article.title,
                            position = row.readDateTime.ifEmpty {
                                stringResource(R.string.read_date_unavailable)
                            },
                            positionDetail = null,
                            kicker = buildList {
                                Labels.categoryLabel(article.category.id).takeIf { it.isNotEmpty() }?.let {
                                    add(ArticleKickerPart(it))
                                }
                                article.source.name.takeIf { it.isNotEmpty() }?.let {
                                    add(ArticleKickerPart(it, emphasized = true))
                                }
                                article.contentType.label.takeIf { it.isNotEmpty() }?.let {
                                    add(ArticleKickerPart(it))
                                }
                                RelativeTime.readingTime(article.readingTimeMinutes).takeIf { it.isNotEmpty() }?.let {
                                    add(ArticleKickerPart(it))
                                }
                            },
                            tags = emptyList(),
                            actions = listOf(
                                ArticleRowAction(stringResource(R.string.reopen_external)) { onReopen(article) },
                                ArticleRowAction(stringResource(R.string.mark_unread)) { onMarkUnread(article) },
                            ),
                        )
                    }
                }
            }
        } else {
            item(key = "empty") {
                EmptyStatePanel(
                    title = stringResource(R.string.history_empty_title),
                    copy = stringResource(R.string.history_empty_copy),
                    actionLabel = stringResource(R.string.go_to_discover),
                    onAction = onDiscover,
                )
            }
        }
    }
}

@Composable
private fun HistoryGroupHeading(period: HistoryPeriod, count: Int) {
    val tokens = LocalIntentionalReadingTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = period.label,
            style = MaterialTheme.typography.headlineLarge,
            color = tokens.fg,
        )
        Text(
            text = historyGroupCount(count),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.muted,
        )
    }
}
