package io.irodriguez.intentionalreading.ui.state

import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.state.DiscoverDeck
import io.irodriguez.intentionalreading.ui.AggregateUiState
import io.irodriguez.intentionalreading.ui.AppUiState
import io.irodriguez.intentionalreading.ui.DatasetPhase
import io.irodriguez.intentionalreading.ui.NavigationCounts
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.format.RelativeTime
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryGroupUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryPeriod
import io.irodriguez.intentionalreading.ui.screens.history.HistoryRowUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterRowUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterUiState
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object UiStateMapper {
    fun map(
        phase: DatasetPhase,
        records: Map<String, ArticleRecord>,
        selectedCategory: Category?,
        heldArticleId: String?,
        now: Instant,
        zone: ZoneId,
        locale: Locale,
    ): AppUiState = AppUiState(
        discover = discover(phase, records, selectedCategory, heldArticleId, now, zone, locale),
        readLater = readLater(records, now, zone, locale),
        history = history(records, now, zone, locale),
        navigationCounts = navigationCounts(records),
        degraded = phase is DatasetPhase.Ready && phase.dataset.pipeline.failedSourceCount > 0,
    )

    fun readLater(
        records: Map<String, ArticleRecord>,
        now: Instant,
        zone: ZoneId,
        locale: Locale,
    ): ReadLaterUiState {
        val selected = records.values
            .filter { it.status == ArticleStatus.SAVED }
            .sortedWith(timestampDescending { it.savedAt })
        return ReadLaterUiState(
            rows = selected.map { record ->
                ReadLaterRowUiState(
                    article = record.article,
                    savedAt = record.savedAt,
                    savedAge = RelativeTime.relativeDate(record.savedAt, now, zone, locale),
                )
            },
            aggregate = aggregate(selected),
        )
    }

    fun history(
        records: Map<String, ArticleRecord>,
        now: Instant,
        zone: ZoneId,
        locale: Locale,
    ): HistoryUiState {
        val selected = records.values
            .filter { it.status == ArticleStatus.READ }
            .sortedWith(timestampDescending { it.readAt })
        val rowsByPeriod = selected
            .map { record ->
                RelativeTime.historyGroup(record.readAt, now, zone) to HistoryRowUiState(
                    article = record.article,
                    readAt = record.readAt,
                    readAge = RelativeTime.relativeDate(record.readAt, now, zone, locale),
                )
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        return HistoryUiState(
            groups = HistoryPeriod.entries.mapNotNull { period ->
                rowsByPeriod[period]?.let { HistoryGroupUiState(period, it) }
            },
            aggregate = aggregate(selected),
        )
    }

    fun navigationCounts(records: Map<String, ArticleRecord>): NavigationCounts = NavigationCounts(
        readLater = records.values.count { it.status == ArticleStatus.SAVED },
        history = records.values.count { it.status == ArticleStatus.READ },
    )

    private fun discover(
        phase: DatasetPhase,
        records: Map<String, ArticleRecord>,
        selectedCategory: Category?,
        heldArticleId: String?,
        now: Instant,
        zone: ZoneId,
        locale: Locale,
    ): DiscoverUiState = when (phase) {
        DatasetPhase.Loading -> DiscoverUiState.Loading(Labels.DISCOVER_LOADING_COPY)
        DatasetPhase.Error -> DiscoverUiState.Error(
            title = Labels.DISCOVER_ERROR_TITLE,
            copy = Labels.DISCOVER_ERROR_COPY,
            actionLabel = Labels.DISCOVER_ERROR_ACTION,
        )
        is DatasetPhase.Ready -> {
            val deck = DiscoverDeck.build(
                articles = phase.dataset.articles,
                records = records,
                selectedCategory = selectedCategory,
                heldArticleId = heldArticleId,
            )
            val article = deck.article
            if (article == null) {
                DiscoverUiState.Empty(
                    title = Labels.DISCOVER_EMPTY_TITLE,
                    copy = Labels.DISCOVER_EMPTY_COPY,
                    actionLabel = Labels.DISCOVER_EMPTY_ACTION,
                )
            } else {
                DiscoverUiState.Card(
                    article = article,
                    publicationAge = RelativeTime.relativeDate(article.publishedAt, now, zone, locale),
                    availableCount = deck.availableCount,
                    remainingCount = deck.remainingCount,
                    isOpened = records[article.id]?.status == ArticleStatus.OPENED,
                )
            }
        }
    }

    private fun aggregate(records: List<ArticleRecord>): AggregateUiState {
        var knownReadingTimeMinutes = 0
        var unknownReadingTimeCount = 0
        records.forEach { record ->
            val readingTime = record.article.readingTimeMinutes
            if (readingTime == null) {
                unknownReadingTimeCount += 1
            } else {
                knownReadingTimeMinutes += readingTime
            }
        }
        return AggregateUiState(
            count = records.size,
            knownReadingTimeMinutes = knownReadingTimeMinutes,
            unknownReadingTimeCount = unknownReadingTimeCount,
            firstTagId = records.firstNotNullOfOrNull { it.article.tags.firstOrNull()?.id },
        )
    }

    private fun timestampDescending(
        selector: (ArticleRecord) -> Instant?,
    ): Comparator<ArticleRecord> = Comparator { left, right ->
        val leftTimestamp = selector(left)
        val rightTimestamp = selector(right)
        when {
            leftTimestamp == null && rightTimestamp == null -> 0
            leftTimestamp == null -> 1
            rightTimestamp == null -> -1
            else -> rightTimestamp.compareTo(leftTimestamp)
        }
    }
}
