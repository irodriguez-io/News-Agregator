package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import io.irodriguez.intentionalreading.domain.ranking.PersonalizedScore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoverDeckTest {
    @Test
    fun `candidate order follows all five keys with a deliberate collision at each key`() {
        val totalWinner = article("total", "total-source", base = 96)
        val baseWinner = article("base", "base-source", base = 95)
        val publicationNewest = article(
            "publication-newest",
            "publication-newest-source",
            base = 94,
            publishedAt = Instant.parse("2026-08-22T12:00:00Z"),
        )
        val publicationOlder = article(
            "publication-older",
            "publication-older-source",
            base = 94,
            publishedAt = Instant.parse("2026-08-21T12:00:00Z"),
        )
        val publicationUnknown = article(
            "publication-unknown",
            "publication-unknown-source",
            base = 94,
            publishedAt = null,
        )
        val sourceAFirst = article("source-id-002", "source-a", base = 93)
        val sourceASecond = article("source-id-003", "source-a", base = 93)
        val sourceB = article("source-id-001", "source-b", base = 93)
        val inputs = listOf(
            sourceB,
            publicationUnknown,
            sourceASecond,
            publicationOlder,
            baseWinner,
            sourceAFirst,
            totalWinner,
            publicationNewest,
        )
        val preferences = preferences(
            "total-source" to PreferenceEntry(0.0, 3),
            "base-source" to PreferenceEntry(0.0, 3),
            "publication-newest-source" to PreferenceEntry(1.0, 3),
            "publication-older-source" to PreferenceEntry(1.0, 3),
            "publication-unknown-source" to PreferenceEntry(1.0, 3),
            "source-a" to PreferenceEntry(2.0, 3),
            "source-b" to PreferenceEntry(2.0, 3),
        )

        val candidates = inputs
            .map { article ->
                DeckCandidate(article, PersonalizedScore.calculate(article, preferences))
            }
            .sortedWith(DiscoverDeck.candidateComparator)

        assertEquals(
            listOf(
                totalWinner.id,
                baseWinner.id,
                publicationNewest.id,
                publicationOlder.id,
                publicationUnknown.id,
                sourceAFirst.id,
                sourceASecond.id,
                sourceB.id,
            ),
            candidates.map { it.article.id },
        )
    }

    @Test
    fun `two builds of identical inputs produce identical ordered values`() {
        val articles = listOf(
            article("third", "source-c", base = 90),
            article("first", "source-a", base = 90),
            article("second", "source-b", base = 90),
        )
        val preferences = preferences(
            "source-a" to PreferenceEntry(1.0, 3),
            "source-b" to PreferenceEntry(1.0, 3),
            "source-c" to PreferenceEntry(1.0, 3),
        )

        val first = DiscoverDeck.build(articles, emptyMap(), preferences, null, null)
        val second = DiscoverDeck.build(articles, emptyMap(), preferences, null, null)

        assertEquals(first, second)
    }

    @Test
    fun `a held article at personalized position five remains shown without changing counts`() {
        val articles = (1..5).map { index ->
            article("article-$index", "source-$index", base = 90)
        }
        val held = articles.last()
        val preferences = preferences(
            *articles.mapIndexed { index, article ->
                article.source.id to PreferenceEntry(weight = (5 - index).toDouble(), interactions = 3)
            }.toTypedArray(),
        )
        val records = mapOf(held.id to record(held, ArticleStatus.OPENED))

        val unpinned = DiscoverDeck.build(articles, records, preferences, null, null)
        val pinned = DiscoverDeck.build(articles, records, preferences, null, held.id)

        assertEquals(held.id, pinned.candidates[4].article.id)
        assertEquals(held, pinned.article)
        assertEquals(unpinned.availableCount, pinned.availableCount)
        assertEquals(unpinned.remainingCount, pinned.remainingCount)
        assertEquals(5, pinned.availableCount)
        assertEquals(4, pinned.remainingCount)
    }

    @Test
    fun `a build leaves preferences records and article base scores equal by value`() {
        val unseen = article("unseen", "preferred", base = 83)
        val opened = article("opened", "other", base = 91)
        val articles = listOf(unseen, opened)
        val records = mapOf(opened.id to record(opened, ArticleStatus.OPENED))
        val preferences = preferences(
            "preferred" to PreferenceEntry(4.0, 1),
            "other" to PreferenceEntry(-2.0, 2),
        )
        val preferencesBefore = preferences.copy(
            sources = preferences.sources.toMap(),
            topics = preferences.topics.toMap(),
        )
        val recordsBefore = records.mapValues { (_, value) ->
            value.copy(article = value.article.copy(score = value.article.score.copy()))
        }
        val baseScoresBefore = articles.map { it.score.base }

        DiscoverDeck.build(articles, records, preferences, null, null)

        assertEquals(preferencesBefore, preferences)
        assertEquals(recordsBefore, records)
        assertEquals(baseScoresBefore, articles.map { it.score.base })
    }

    @Test
    fun `eligibility category filtering and available count remain unchanged`() {
        val unseen = article("unseen", "source-unseen")
        val opened = article("opened", "source-opened")
        val saved = article("saved", "source-saved")
        val dismissed = article("dismissed", "source-dismissed")
        val read = article("read", "source-read")
        val otherCategory = article("technology", "source-technology", category = Category.TECHNOLOGY)
        val records = listOf(
            record(opened, ArticleStatus.OPENED),
            record(saved, ArticleStatus.SAVED),
            record(dismissed, ArticleStatus.DISMISSED),
            record(read, ArticleStatus.READ),
        ).associateBy { it.article.id }

        val deck = DiscoverDeck.build(
            articles = listOf(saved, otherCategory, read, opened, dismissed, unseen),
            records = records,
            preferences = emptyPreferences,
            selectedCategory = Category.IAM,
            heldArticleId = null,
        )

        assertEquals(setOf(unseen.id, opened.id), deck.candidates.map { it.article.id }.toSet())
        assertEquals(2, deck.availableCount)
        assertEquals(1, deck.remainingCount)
    }

    private fun preferences(
        vararg entries: Pair<String, PreferenceEntry>,
    ): LocalState.Preferences = LocalState.Preferences(
        sources = mapOf(*entries),
        topics = emptyMap(),
    )

    private fun record(article: Article, status: ArticleStatus): ArticleRecord = ArticleRecord(
        article = article,
        status = status,
        firstSeenAt = Instant.parse("2026-08-20T12:00:00Z"),
        openedAt = Instant.parse("2026-08-20T12:00:00Z").takeIf { status == ArticleStatus.OPENED },
        savedAt = Instant.parse("2026-08-20T12:00:00Z").takeIf { status == ArticleStatus.SAVED },
        dismissedAt = Instant.parse("2026-08-20T12:00:00Z").takeIf { status == ArticleStatus.DISMISSED },
        readAt = Instant.parse("2026-08-20T12:00:00Z").takeIf { status == ArticleStatus.READ },
        signalsApplied = SignalsApplied(false, false, false, false),
    )

    private fun article(
        id: String,
        sourceId: String,
        base: Int = 91,
        publishedAt: Instant? = Instant.parse("2026-08-20T12:00:00Z"),
        category: Category = Category.IAM,
    ): Article = Article(
        id = id,
        title = "Article $id",
        url = "https://example.com/$id",
        source = ArticleSource(sourceId, "Source $sourceId"),
        category = category,
        publishedAt = publishedAt,
        author = null,
        excerpt = "Excerpt $id",
        readingTimeMinutes = 7,
        tags = emptyList(),
        contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
        score = ArticleScore(base, 50, 20, 15, 5, 1),
    )

    private companion object {
        val emptyPreferences = LocalState.Preferences(emptyMap(), emptyMap())
    }
}
