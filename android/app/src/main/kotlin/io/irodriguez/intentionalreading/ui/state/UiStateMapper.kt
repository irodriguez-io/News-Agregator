package io.irodriguez.intentionalreading.ui.state

import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.state.DiscoverDeck
import io.irodriguez.intentionalreading.ui.AggregateUiState
import io.irodriguez.intentionalreading.ui.AppUiState
import io.irodriguez.intentionalreading.ui.DatasetPhase
import io.irodriguez.intentionalreading.ui.DatasetRefreshPhase
import io.irodriguez.intentionalreading.ui.NavigationCounts
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.format.RelativeTime
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
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
        refresh: DatasetRefreshPhase = DatasetRefreshPhase.Idle,
    ): AppUiState {
        val generatedAt = (phase as? DatasetPhase.Ready)?.dataset?.generatedAt?.let(Instant::parse)
        val contentFreshness = generatedAt?.let { value ->
            Labels.contentFreshness(RelativeTime.relativeDate(value, now, zone, locale))
        }
        return AppUiState(
            discover = discover(
                phase = phase,
                records = records,
                selectedCategory = selectedCategory,
                heldArticleId = heldArticleId,
                now = now,
                zone = zone,
                locale = locale,
                contentFreshness = contentFreshness,
                failedRefreshDisclosure = failedRefreshDisclosure(phase, refresh),
                refreshAffordance = refreshAffordance(phase, refresh),
            ),
            readLater = readLater(records, now, zone, locale),
            history = history(records, now, zone, locale),
            navigationCounts = navigationCounts(records),
            degraded = phase is DatasetPhase.Ready && phase.dataset.pipeline.failedSourceCount > 0,
            generatedAtLabel = generatedAt?.let { value ->
                Labels.generatedAt(RelativeTime.localDateTime(value, zone, locale))
            } ?: Labels.CONTENT_GENERATION_UNAVAILABLE,
            lastRefreshOutcome = refreshOutcome(refresh),
            refresh = refresh,
        )
    }

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
                    readDateTime = RelativeTime.localDateTime(record.readAt, zone, locale),
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
        contentFreshness: String?,
        failedRefreshDisclosure: String?,
        refreshAffordance: DiscoverRefreshAffordance,
    ): DiscoverUiState = when (phase) {
        DatasetPhase.Loading -> DiscoverUiState.Loading(
            copy = Labels.DISCOVER_LOADING_COPY,
            contentFreshness = contentFreshness,
            failedRefreshDisclosure = failedRefreshDisclosure,
            refreshAffordance = refreshAffordance,
        )
        DatasetPhase.Error -> DiscoverUiState.Error(
            title = Labels.DISCOVER_ERROR_TITLE,
            copy = Labels.DISCOVER_ERROR_COPY,
            actionLabel = Labels.DISCOVER_ERROR_ACTION,
            contentFreshness = contentFreshness,
            failedRefreshDisclosure = failedRefreshDisclosure,
            refreshAffordance = refreshAffordance,
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
                    contentFreshness = contentFreshness,
                    failedRefreshDisclosure = failedRefreshDisclosure,
                    refreshAffordance = refreshAffordance,
                )
            } else {
                DiscoverUiState.Card(
                    article = article,
                    publicationAge = RelativeTime.relativeDate(article.publishedAt, now, zone, locale),
                    availableCount = deck.availableCount,
                    remainingCount = deck.remainingCount,
                    isOpened = records[article.id]?.status == ArticleStatus.OPENED,
                    contentFreshness = contentFreshness,
                    failedRefreshDisclosure = failedRefreshDisclosure,
                    refreshAffordance = refreshAffordance,
                )
            }
        }
    }

    private fun refreshAffordance(
        phase: DatasetPhase,
        refresh: DatasetRefreshPhase,
    ): DiscoverRefreshAffordance = when {
        refresh == DatasetRefreshPhase.Refreshing -> DiscoverRefreshAffordance.IN_PROGRESS
        phase is DatasetPhase.Ready -> DiscoverRefreshAffordance.AVAILABLE
        else -> DiscoverRefreshAffordance.HIDDEN
    }

    private fun failedRefreshDisclosure(
        phase: DatasetPhase,
        refresh: DatasetRefreshPhase,
    ): String? = Labels.DISCOVER_REFRESH_FAILED.takeIf {
        phase is DatasetPhase.Ready && refresh == DatasetRefreshPhase.Failed
    }

    private fun refreshOutcome(refresh: DatasetRefreshPhase): String = when (refresh) {
        DatasetRefreshPhase.Idle -> Labels.LAST_REFRESH_IDLE
        DatasetRefreshPhase.Refreshing -> Labels.LAST_REFRESH_REFRESHING
        DatasetRefreshPhase.Updated -> Labels.LAST_REFRESH_UPDATED
        DatasetRefreshPhase.Current -> Labels.LAST_REFRESH_CURRENT
        DatasetRefreshPhase.Failed -> Labels.LAST_REFRESH_FAILED
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
        val firstTagId = records.firstNotNullOfOrNull { it.article.tags.firstOrNull()?.id }
        return AggregateUiState(
            count = records.size,
            knownReadingTimeMinutes = knownReadingTimeMinutes,
            unknownReadingTimeCount = unknownReadingTimeCount,
            firstTagId = firstTagId,
            firstTagLabel = topicLabel(records, firstTagId),
        )
    }

    private fun topicLabel(records: List<ArticleRecord>, topicId: String?): String? {
        if (topicId == null) return null
        return records.firstNotNullOfOrNull { record ->
            record.article.tags.firstOrNull { tag -> tag.id == topicId }?.label
        }
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
