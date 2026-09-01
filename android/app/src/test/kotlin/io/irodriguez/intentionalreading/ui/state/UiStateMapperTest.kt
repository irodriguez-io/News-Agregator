package io.irodriguez.intentionalreading.ui.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import io.irodriguez.intentionalreading.domain.state.ArticleStateMachine
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.ui.AppUiState
import io.irodriguez.intentionalreading.ui.DatasetPhase
import io.irodriguez.intentionalreading.ui.DatasetRefreshPhase
import io.irodriguez.intentionalreading.ui.PendingUndoOffer
import io.irodriguez.intentionalreading.ui.PendingUndoMessage
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryPeriod
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiStateMapperTest {
    @Test
    fun `Discover body state maps loading then error then empty then card`() {
        val loading = UiStateMapper.map(
            phase = DatasetPhase.Loading,
            records = emptyMap(),
            preferences = noPreferences,
            selectedCategory = null,
            heldArticleId = null,
            now = now,
            zone = zone,
            locale = Locale.US,
        )
        val error = UiStateMapper.map(
            phase = DatasetPhase.Error,
            records = emptyMap(),
            preferences = noPreferences,
            selectedCategory = null,
            heldArticleId = null,
            now = now,
            zone = zone,
            locale = Locale.US,
        )
        val empty = map(dataset = dataset(emptyList()))
        val card = map(dataset = dataset(listOf(article(1))))

        assertEquals(Labels.DISCOVER_LOADING_COPY, assertIs<DiscoverUiState.Loading>(loading.discover).copy)
        assertEquals(Labels.DISCOVER_ERROR_TITLE, assertIs<DiscoverUiState.Error>(error.discover).title)
        assertEquals(Labels.DISCOVER_EMPTY_TITLE, assertIs<DiscoverUiState.Empty>(empty.discover).title)
        assertIs<DiscoverUiState.Card>(card.discover)
    }

    @Test
    fun `a category with no articles uses exact permission-to-leave copy`() {
        val state = map(
            dataset = dataset(listOf(article(1, Category.IAM))),
            selectedCategory = Category.WEIGHTLIFTING,
        )

        val empty = assertIs<DiscoverUiState.Empty>(state.discover)
        assertEquals("Nothing needs your attention right now", empty.title)
        assertEquals(
            "You are caught up for this category. Leave without missing anything, or return to your saved reading.",
            empty.copy,
        )
        assertEquals("View Read Later", empty.actionLabel)
    }

    @Test
    fun `Discover offers one dataset-ordered card with available and remaining counts`() {
        val articles = listOf(article(1), article(2), article(3))

        val card = assertIs<DiscoverUiState.Card>(map(dataset = dataset(articles)).discover)

        assertEquals(articles.first(), card.article)
        assertEquals(3, card.availableCount)
        assertEquals(2, card.remainingCount)
        assertEquals("2 more choices wait quietly behind this one.", Labels.remainingChoices(card.remainingCount))
    }

    @Test
    fun `Scenario - the Android client says exactly what the browser says`() {
        // Given
        val singularRemainingCount = 1
        val pluralRemainingCount = 2

        // When
        val singularLabel = Labels.remainingChoices(singularRemainingCount)
        val pluralLabel = Labels.remainingChoices(pluralRemainingCount)

        // Then
        assertEquals("1 more choice waits quietly behind this one.", singularLabel)
        assertEquals("2 more choices wait quietly behind this one.", pluralLabel)
    }

    @Test
    fun `category filtering happens before head selection without sorting dataset order`() {
        val technology = article(2, Category.TECHNOLOGY)
        val laterIam = article(3, Category.IAM)
        val articles = listOf(article(1, Category.IAM), technology, laterIam)

        val card = assertIs<DiscoverUiState.Card>(
            map(dataset = dataset(articles), selectedCategory = Category.TECHNOLOGY).discover,
        )

        assertEquals(technology, card.article)
        assertEquals(1, card.availableCount)
        assertEquals(0, card.remainingCount)
        assertNull(Labels.remainingChoices(card.remainingCount))
    }

    @Test
    fun `an eligible held article remains presented and visibly acknowledged when opened`() {
        val articles = listOf(article(1), article(2), article(3))
        val held = articles[1]
        val records = mapOf(held.id to record(held, ArticleStatus.OPENED, openedAt = now.minusSeconds(60)))

        val card = assertIs<DiscoverUiState.Card>(
            map(dataset = dataset(articles), records = records, heldArticleId = held.id).discover,
        )

        assertEquals(held, card.article)
        assertTrue(card.isOpened)
        assertEquals(3, card.availableCount)
        assertEquals(2, card.remainingCount)
    }

    @Test
    fun `Discover head reflects source preferences despite the exploration disadvantage`() {
        val first = article(1)
        val preferred = article(2)
        val preferences = LocalState.Preferences(
            sources = mapOf(preferred.source.id to PreferenceEntry(weight = 5.0, interactions = 3)),
            topics = emptyMap(),
        )

        val card = assertIs<DiscoverUiState.Card>(
            map(dataset = dataset(listOf(first, preferred)), preferences = preferences).discover,
        )

        assertEquals(preferred, card.article)
    }

    @Test
    fun `an ineligible or category-mismatched held article does not replace the first eligible head`() {
        val first = article(1, Category.IAM)
        val held = article(2, Category.TECHNOLOGY)
        val records = mapOf(held.id to record(held, ArticleStatus.SAVED, savedAt = now))

        val card = assertIs<DiscoverUiState.Card>(
            map(
                dataset = dataset(listOf(first, held)),
                records = records,
                selectedCategory = Category.IAM,
                heldArticleId = held.id,
            ).discover,
        )

        assertEquals(first, card.article)
        assertFalse(card.isOpened)
    }

    @Test
    fun `dismissing advances the deck and immediately decreases both counts`() {
        val articles = listOf(article(1), article(2))
        val initial = assertIs<DiscoverUiState.Card>(map(dataset = dataset(articles)).discover)
        val transition = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                emptyMap(),
                noPreferences,
                articles.first(),
                ArticleAction.DISMISS,
                now,
            ),
        )

        val next = assertIs<DiscoverUiState.Card>(
            map(dataset = dataset(articles), records = transition.records).discover,
        )

        assertEquals(2, initial.availableCount)
        assertEquals(1, initial.remainingCount)
        assertEquals(articles[1], next.article)
        assertEquals(1, next.availableCount)
        assertEquals(0, next.remainingCount)
    }

    @Test
    fun `saving removes Discover head reaches top of Read Later and increments navigation`() {
        val first = article(1)
        val olderSaved = record(article(3), ArticleStatus.SAVED, savedAt = now.minusSeconds(60))
        val transition = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(olderSaved.article.id to olderSaved),
                noPreferences,
                first,
                ArticleAction.SAVE,
                now,
            ),
        )

        val state = map(dataset = dataset(listOf(first, article(2))), records = transition.records)

        assertEquals(article(2), assertIs<DiscoverUiState.Card>(state.discover).article)
        assertEquals(listOf(first.id, olderSaved.article.id), state.readLater.rows.map { it.article.id })
        assertEquals(2, state.navigationCounts.readLater)
        assertEquals(0, state.navigationCounts.history)
    }

    @Test
    fun `marking read removes Discover head and groups it under Today without undo state`() {
        val first = article(1)
        val opened = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                emptyMap(),
                noPreferences,
                first,
                ArticleAction.OPEN,
                now.minusSeconds(120),
            ),
        )
        val read = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                opened.records,
                opened.preferences,
                first,
                ArticleAction.MARK_READ,
                now,
            ),
        )

        val state = map(dataset = dataset(listOf(first, article(2))), records = read.records)

        assertEquals(article(2), assertIs<DiscoverUiState.Card>(state.discover).article)
        assertEquals(listOf(HistoryPeriod.TODAY), state.history.groups.map { it.period })
        assertEquals(first.id, state.history.groups.single().rows.single().article.id)
        assertEquals(0, state.navigationCounts.readLater)
        assertEquals(1, state.navigationCounts.history)
    }

    @Test
    fun `Read Later orders savedAt descending and computes every aggregate field`() {
        val oldest = record(
            article = article(1, readingTimeMinutes = 5, tags = emptyList()),
            status = ArticleStatus.SAVED,
            savedAt = now.minusSeconds(300),
        )
        val newest = record(
            article = article(2, readingTimeMinutes = null, tags = listOf(ArticleTag("oauth", "OAuth"))),
            status = ArticleStatus.SAVED,
            savedAt = now.minusSeconds(60),
        )
        val middle = record(
            article = article(3, readingTimeMinutes = 7, tags = listOf(ArticleTag("oidc", "OIDC"))),
            status = ArticleStatus.SAVED,
            savedAt = now.minusSeconds(120),
        )
        val records = listOf(oldest, newest, middle).associateBy { it.article.id }

        val state = map(dataset = dataset(emptyList()), records = records).readLater

        assertEquals(listOf(newest.article.id, middle.article.id, oldest.article.id), state.rows.map { it.article.id })
        assertEquals(3, state.aggregate.count)
        assertEquals(12, state.aggregate.knownReadingTimeMinutes)
        assertEquals(1, state.aggregate.unknownReadingTimeCount)
        assertEquals("oauth", state.aggregate.firstTagId)
        assertEquals("OAuth", state.aggregate.firstTagLabel)
    }

    @Test
    fun `Read Later stays ordered by savedAt descending regardless of weights`() {
        val newest = record(article(1), ArticleStatus.SAVED, savedAt = now)
        val older = record(article(2), ArticleStatus.SAVED, savedAt = now.minusSeconds(60))
        val preferences = LocalState.Preferences(
            sources = mapOf(
                newest.article.source.id to PreferenceEntry(-5.0, 3),
                older.article.source.id to PreferenceEntry(5.0, 3),
            ),
            topics = emptyMap(),
        )

        val readLater = map(
            dataset = dataset(emptyList()),
            records = listOf(older, newest).associateBy { it.article.id },
            preferences = preferences,
        ).readLater

        assertEquals(listOf(newest.article.id, older.article.id), readLater.rows.map { it.article.id })
    }

    @Test
    fun `aggregate topic id and label are null when no record has tags`() {
        val saved = record(
            article = article(1, tags = emptyList()),
            status = ArticleStatus.SAVED,
            savedAt = now,
        )

        val aggregate = map(
            dataset = dataset(emptyList()),
            records = mapOf(saved.article.id to saved),
        ).readLater.aggregate

        assertNull(aggregate.firstTagId)
        assertNull(aggregate.firstTagLabel)
    }

    @Test
    fun `History orders readAt descending groups local calendar days and omits empty groups`() {
        val localZone = ZoneId.of("America/Los_Angeles")
        val localNow = Instant.parse("2026-08-22T07:30:00Z")
        val todayEarly = record(article(1), ArticleStatus.READ, readAt = Instant.parse("2026-08-22T07:05:00Z"))
        val yesterdayLate = record(article(2), ArticleStatus.READ, readAt = Instant.parse("2026-08-22T06:55:00Z"))
        val earlier = record(article(3), ArticleStatus.READ, readAt = Instant.parse("2026-08-20T18:00:00Z"))
        val records = listOf(earlier, yesterdayLate, todayEarly).associateBy { it.article.id }

        val history = UiStateMapper.history(records, localNow, localZone, Locale.US)

        assertEquals(
            listOf(HistoryPeriod.TODAY, HistoryPeriod.YESTERDAY, HistoryPeriod.EARLIER),
            history.groups.map { it.period },
        )
        assertEquals(todayEarly.article.id, history.groups[0].rows.single().article.id)
        assertEquals("Aug 22, 2026, 12:05 AM", history.groups[0].rows.single().readDateTime)
        assertEquals(yesterdayLate.article.id, history.groups[1].rows.single().article.id)
        assertEquals(earlier.article.id, history.groups[2].rows.single().article.id)

        val onlyToday = UiStateMapper.history(mapOf(todayEarly.article.id to todayEarly), localNow, localZone, Locale.US)
        assertEquals(listOf(HistoryPeriod.TODAY), onlyToday.groups.map { it.period })
    }

    @Test
    fun `History stays ordered by readAt descending regardless of weights`() {
        val newest = record(article(1), ArticleStatus.READ, readAt = now)
        val older = record(article(2), ArticleStatus.READ, readAt = now.minusSeconds(60))
        val preferences = LocalState.Preferences(
            sources = mapOf(
                newest.article.source.id to PreferenceEntry(-5.0, 3),
                older.article.source.id to PreferenceEntry(5.0, 3),
            ),
            topics = emptyMap(),
        )

        val history = map(
            dataset = dataset(emptyList()),
            records = listOf(older, newest).associateBy { it.article.id },
            preferences = preferences,
        ).history

        assertEquals(listOf(newest.article.id, older.article.id), history.groups.flatMap { it.rows }.map { it.article.id })
    }

    @Test
    fun `History aggregate uses newest tagged record and counts unknown reading times`() {
        val newest = record(
            article(1, readingTimeMinutes = null, tags = emptyList()),
            ArticleStatus.READ,
            readAt = now,
        )
        val tagged = record(
            article(2, readingTimeMinutes = 8, tags = listOf(ArticleTag("scim", "SCIM"))),
            ArticleStatus.READ,
            readAt = now.minusSeconds(60),
        )

        val history = map(
            dataset = dataset(emptyList()),
            records = listOf(tagged, newest).associateBy { it.article.id },
        ).history

        assertEquals(2, history.aggregate.count)
        assertEquals(8, history.aggregate.knownReadingTimeMinutes)
        assertEquals(1, history.aggregate.unknownReadingTimeCount)
        assertEquals("scim", history.aggregate.firstTagId)
    }

    @Test
    fun `navigation counts ignore opened and dismissed records and degraded follows pipeline failures`() {
        val records = listOf(
            record(article(1), ArticleStatus.OPENED, openedAt = now),
            record(article(2), ArticleStatus.DISMISSED, dismissedAt = now),
            record(article(3), ArticleStatus.SAVED, savedAt = now),
            record(article(4), ArticleStatus.READ, readAt = now),
        ).associateBy { it.article.id }

        val state = map(dataset = dataset(emptyList(), failedSourceCount = 2), records = records)

        assertEquals(1, state.navigationCounts.readLater)
        assertEquals(1, state.navigationCounts.history)
        assertTrue(state.degraded)
        assertEquals("Some sources were unavailable when this content was gathered.", Labels.DEGRADED_NOTICE)
        assertFalse(map(dataset = dataset(emptyList(), failedSourceCount = 0)).degraded)
    }

    @Test
    fun `failed refresh with cached content discloses failure beside distinct content freshness`() {
        val card = assertIs<DiscoverUiState.Card>(
            map(
                dataset = dataset(
                    articles = listOf(article(1)),
                    generatedAt = "2026-08-20T12:00:00Z",
                ),
                refresh = DatasetRefreshPhase.Failed,
            ).discover,
        )

        assertEquals("Content age · 2d", card.contentFreshness)
        assertEquals(
            "Refresh failed. Showing the last available content.",
            card.failedRefreshDisclosure,
        )
        assertTrue(card.contentFreshness != card.failedRefreshDisclosure)
    }

    @Test
    fun `successful and in progress outcomes do not persist a Discover failure disclosure`() {
        listOf(
            DatasetRefreshPhase.Idle,
            DatasetRefreshPhase.Refreshing,
            DatasetRefreshPhase.Updated,
            DatasetRefreshPhase.Current,
        ).forEach { refresh ->
            val card = assertIs<DiscoverUiState.Card>(
                map(dataset = dataset(listOf(article(1))), refresh = refresh).discover,
            )

            assertNull(card.failedRefreshDisclosure, "Unexpected disclosure for $refresh")
        }
    }

    @Test
    fun `failed refresh without cached content relies on the existing Discover error panel`() {
        val state = UiStateMapper.map(
            phase = DatasetPhase.Error,
            records = emptyMap(),
            preferences = noPreferences,
            selectedCategory = null,
            heldArticleId = null,
            now = now,
            zone = zone,
            locale = Locale.US,
            refresh = DatasetRefreshPhase.Failed,
        )

        assertNull(assertIs<DiscoverUiState.Error>(state.discover).failedRefreshDisclosure)
    }

    @Test
    fun `pipeline degradation follows failed source count independently of client refresh failure`() {
        val degradedPipeline = map(
            dataset = dataset(listOf(article(1)), failedSourceCount = 1),
            refresh = DatasetRefreshPhase.Current,
        )
        val failedClientRefresh = map(
            dataset = dataset(listOf(article(1)), failedSourceCount = 0),
            refresh = DatasetRefreshPhase.Failed,
        )

        assertTrue(degradedPipeline.degraded)
        assertFalse(failedClientRefresh.degraded)
    }

    @Test
    fun `known generatedAt maps relative Discover age and exact local Settings time beside the refresh outcome`() {
        val state = map(
            dataset = dataset(
                articles = listOf(article(1)),
                generatedAt = "2026-08-20T12:00:00Z",
            ),
            refresh = DatasetRefreshPhase.Current,
        )

        val card = assertIs<DiscoverUiState.Card>(state.discover)
        assertEquals("Content age · 2d", card.contentFreshness)
        assertEquals("Content generated · Aug 20, 2026, 6:00 AM", state.generatedAtLabel)
        assertEquals("Last refresh · Already current", state.lastRefreshOutcome)
    }

    @Test
    fun `ready Discover exposes Refresh until the in flight state replaces it with a disabled status`() {
        val available = assertIs<DiscoverUiState.Card>(
            map(dataset = dataset(listOf(article(1)))).discover,
        )
        val refreshing = assertIs<DiscoverUiState.Card>(
            map(
                dataset = dataset(listOf(article(1))),
                refresh = DatasetRefreshPhase.Refreshing,
            ).discover,
        )

        assertEquals(DiscoverRefreshAffordance.AVAILABLE, available.refreshAffordance)
        assertEquals(DiscoverRefreshAffordance.IN_PROGRESS, refreshing.refreshAffordance)
    }

    @Test
    fun `the eight category options retain exact order ids and labels`() {
        assertEquals(
            listOf(
                "all" to "All",
                "science" to "Science",
                "technology" to "Technology",
                "literature" to "Literature",
                "history" to "History",
                "weightlifting" to "Weightlifting",
                "iam" to "IAM",
                "identity_automation" to "Identity Automation",
            ),
            Labels.categoryOptions.map { it.id to it.label },
        )
    }

    @Test
    fun `undo availability and pending offer reflect separate in-memory state`() {
        // Given no pending undo action or offer
        val unavailable = mapWithUndoState(null, null)

        // When the mapper receives eligible slot actions and identified offers
        val savedOffer = PendingUndoOffer(7L, PendingUndoMessage.SAVED)
        val dismissedOffer = PendingUndoOffer(8L, PendingUndoMessage.DISMISSED)
        val saved = mapWithUndoState(ArticleAction.SAVE, savedOffer)
        val dismissed = mapWithUndoState(ArticleAction.DISMISS, dismissedOffer)
        val acknowledged = mapWithUndoState(ArticleAction.SAVE, null)

        // Then slot availability and offer presentation remain independently observable
        assertFalse(unavailable.undoAvailable)
        assertNull(unavailable.pendingUndoOffer)
        assertTrue(saved.undoAvailable)
        assertEquals(savedOffer, saved.pendingUndoOffer)
        assertTrue(dismissed.undoAvailable)
        assertEquals(dismissedOffer, dismissed.pendingUndoOffer)
        assertTrue(acknowledged.undoAvailable)
        assertNull(acknowledged.pendingUndoOffer)
    }

    @Test
    fun `each new action's undo record makes Undo available for Mark Read`() {
        // Given a Mark Read undo record exists
        val action = ArticleAction.MARK_READ

        // When the mapper receives the record's action
        val mapped = mapWithUndoState(action, null)

        // Then Undo is available
        assertTrue(mapped.undoAvailable)
    }

    @Test
    fun `each new action's undo record makes Undo available for Mark Unread`() {
        // Given a Mark Unread undo record exists
        val action = ArticleAction.MARK_UNREAD

        // When the mapper receives the record's action
        val mapped = mapWithUndoState(action, null)

        // Then Undo is available
        assertTrue(mapped.undoAvailable)
    }

    @Test
    fun `each new action's undo record makes Undo available for Remove`() {
        // Given a Remove undo record exists
        val action = ArticleAction.REMOVE

        // When the mapper receives the record's action
        val mapped = mapWithUndoState(action, null)

        // Then Undo is available
        assertTrue(mapped.undoAvailable)
    }

    private fun mapWithUndoState(
        action: ArticleAction?,
        offer: PendingUndoOffer?,
    ): AppUiState = UiStateMapper.map(
        phase = DatasetPhase.Ready(dataset(listOf(article(1)))),
        records = emptyMap(),
        preferences = noPreferences,
        selectedCategory = null,
        heldArticleId = null,
        now = now,
        zone = zone,
        locale = Locale.US,
        refresh = DatasetRefreshPhase.Idle,
        undoAction = action,
        pendingUndoOffer = offer,
    )

    private fun map(
        dataset: ArticleDataset,
        records: Map<String, ArticleRecord> = emptyMap(),
        preferences: LocalState.Preferences = noPreferences,
        selectedCategory: Category? = null,
        heldArticleId: String? = null,
        refresh: DatasetRefreshPhase = DatasetRefreshPhase.Idle,
    ) = UiStateMapper.map(
        phase = DatasetPhase.Ready(dataset),
        records = records,
        preferences = preferences,
        selectedCategory = selectedCategory,
        heldArticleId = heldArticleId,
        now = now,
        zone = zone,
        locale = Locale.US,
        refresh = refresh,
    )

    private fun dataset(
        articles: List<Article>,
        failedSourceCount: Int = 0,
        generatedAt: String = "2026-08-22T12:00:00Z",
    ): ArticleDataset = ArticleDataset(
        schemaVersion = 1,
        generatedAt = generatedAt,
        pipeline = PipelineMetadata(
            enabledSourceCount = 2,
            successfulSourceCount = 2 - failedSourceCount,
            failedSourceCount = failedSourceCount,
            articleCount = articles.size,
        ),
        articles = articles,
    )

    private fun record(
        article: Article,
        status: ArticleStatus,
        openedAt: Instant? = null,
        savedAt: Instant? = null,
        dismissedAt: Instant? = null,
        readAt: Instant? = null,
    ) = ArticleRecord(
        article = article,
        status = status,
        firstSeenAt = now.minusSeconds(600),
        openedAt = openedAt,
        savedAt = savedAt,
        dismissedAt = dismissedAt,
        readAt = readAt,
        signalsApplied = SignalsApplied(
            opened = openedAt != null,
            saved = false,
            dismissed = false,
            read = status == ArticleStatus.READ,
        ),
    )

    private fun article(
        index: Int,
        category: Category = Category.IAM,
        readingTimeMinutes: Int? = 7,
        tags: List<ArticleTag> = listOf(ArticleTag("oauth", "OAuth")),
    ): Article = Article(
        id = index.toString(16).padStart(20, '0'),
        title = "Article $index",
        url = "https://example.com/$index",
        source = ArticleSource("source_$index", "Source $index"),
        category = category,
        publishedAt = now.minusSeconds(index * 3_600L),
        author = null,
        excerpt = "Excerpt $index",
        readingTimeMinutes = readingTimeMinutes,
        tags = tags,
        contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
        score = ArticleScore(91, 50, 20, 15, 5, 1),
    )

    private companion object {
        val noPreferences = LocalState.Preferences(sources = emptyMap(), topics = emptyMap())
        val now: Instant = Instant.parse("2026-08-22T12:00:00Z")
        val zone: ZoneId = ZoneId.of("America/Managua")
    }
}
