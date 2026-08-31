package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import io.irodriguez.intentionalreading.domain.ranking.PersonalizedScore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoverDeckSequencingTest {
    @Test
    fun `the second card avoids the head card's source`() {
        // Given
        val sourceAFirst = article("a-first", "source-a", base = 100, category = Category.TECHNOLOGY)
        val sourceASecond = article("a-second", "source-a", base = 99, category = Category.TECHNOLOGY)
        val sourceB = article("b", "source-b", base = 95, category = Category.TECHNOLOGY)
        val articles = listOf(sourceAFirst, sourceASecond, sourceB)

        // When
        val deck = build(articles, selectedCategory = Category.TECHNOLOGY)

        // Then
        assertEquals(listOf("a-first", "b", "a-second"), deck.ids())
        assertEquals(0.0, deck.candidates[2].sequencing.sameSourcePenalty)
    }

    @Test
    fun `a sufficiently stronger candidate keeps its place despite the penalty`() {
        // Given
        val sourceAFirst = article("a-first", "source-a", base = 100, category = Category.TECHNOLOGY)
        val sourceASecond = article("a-second", "source-a", base = 99, category = Category.TECHNOLOGY)
        val sourceB = article("b", "source-b", base = 90, category = Category.TECHNOLOGY)

        // When
        val deck = build(listOf(sourceAFirst, sourceASecond, sourceB), Category.TECHNOLOGY)

        // Then
        assertEquals("a-second", deck.candidates[1].article.id)
        assertEquals(-8.0, deck.candidates[1].sequencing.sameSourcePenalty)
    }

    @Test
    fun `the penalty compares against the previous card only`() {
        // Given
        val articles = listOf(
            article("a-first", "source-a", base = 100),
            article("b", "source-b", base = 99),
            article("c", "source-c", base = 98),
            article("a-later", "source-a", base = 90),
        )

        // When
        val deck = build(articles, selectedCategory = Category.IAM)

        // Then
        assertEquals(listOf("a-first", "b", "c", "a-later"), deck.ids())
        assertEquals(0.0, deck.candidates[3].sequencing.sameSourcePenalty)
    }

    @Test
    fun `the all view breaks a third consecutive category`() {
        // Given
        val articles = categoryFixture()

        // When
        val deck = build(articles, selectedCategory = null)

        // Then
        assertEquals(listOf("1", "2", "4", "3"), deck.ids())
    }

    @Test
    fun `a category view disables the penalty entirely`() {
        // Given
        val articles = categoryFixture()

        // When
        val deck = build(articles, selectedCategory = Category.TECHNOLOGY)

        // Then
        assertEquals(listOf("1", "2", "3"), deck.ids())
        assertTrue(deck.candidates.all { candidate -> candidate.sequencing.categoryPenalty == 0.0 })
    }

    @Test
    fun `a sufficiently stronger third candidate keeps its place`() {
        // Given
        val articles = listOf(
            article("1", "source-a", base = 100, category = Category.TECHNOLOGY),
            article("2", "source-b", base = 99, category = Category.TECHNOLOGY),
            article("3", "source-c", base = 98, category = Category.TECHNOLOGY),
            article("4", "source-d", base = 90, category = Category.SCIENCE),
        )

        // When
        val deck = build(articles, selectedCategory = null)

        // Then
        assertEquals("3", deck.candidates[2].article.id)
        assertEquals(-5.0, deck.candidates[2].sequencing.categoryPenalty)
    }

    @Test
    fun `two consecutive cards from one category are not penalized`() {
        // Given
        val articles = listOf(
            article("1", "source-a", base = 100, category = Category.TECHNOLOGY),
            article("2", "source-b", base = 99, category = Category.TECHNOLOGY),
            article("3", "source-c", base = 90, category = Category.SCIENCE),
        )

        // When
        val deck = build(articles, selectedCategory = null)

        // Then
        assertEquals("2", deck.candidates[1].article.id)
        assertEquals(0.0, deck.candidates[1].sequencing.categoryPenalty)
    }

    @Test
    fun `the penalty needs two previously selected cards`() {
        // Given
        val articles = listOf(
            article("1", "source-a", base = 100, category = Category.TECHNOLOGY),
            article("2", "source-b", base = 99, category = Category.TECHNOLOGY),
        )

        // When
        val deck = build(articles, selectedCategory = null)

        // Then
        assertEquals(listOf(0.0, 0.0), deck.candidates.map { it.sequencing.categoryPenalty })
    }

    @Test
    fun `both penalties are additive`() {
        // Given
        val articles = listOf(
            article("1", "source-a", base = 100, category = Category.TECHNOLOGY),
            article("2", "source-b", base = 99, category = Category.TECHNOLOGY),
            article("3", "source-b", base = 98, category = Category.TECHNOLOGY),
            article("4", "source-c", base = 80, category = Category.SCIENCE),
        )

        // When
        val deck = build(articles, selectedCategory = null)

        // Then
        val candidate = deck.candidates[2]
        assertEquals("3", candidate.article.id)
        assertEquals(-8.0, candidate.sequencing.sameSourcePenalty)
        assertEquals(-5.0, candidate.sequencing.categoryPenalty)
        assertEquals(-13.0, candidate.sequencing.sameSourcePenalty + candidate.sequencing.categoryPenalty)
    }

    @Test
    fun `the first card carries no penalty`() {
        // Given
        val articles = listOf(
            article("a-first", "source-a", base = 100),
            article("a-second", "source-a", base = 99),
            article("b", "source-b", base = 95),
        )
        val preferences = preferencesFor(articles)
        val unsequenced = articles
            .map { candidate ->
                DeckCandidate(candidate, PersonalizedScore.calculate(candidate, preferences))
            }
            .sortedWith(DiscoverDeck.candidateComparator)

        // When
        val deck = build(articles, selectedCategory = Category.IAM, preferences = preferences)

        // Then
        assertEquals(unsequenced.first().article, deck.candidates.first().article)
        assertEquals(0.0, deck.candidates.first().sequencing.sameSourcePenalty)
        assertEquals(0.0, deck.candidates.first().sequencing.categoryPenalty)
        assertEquals(listOf("a-first", "b", "a-second"), deck.ids())
    }

    @Test
    fun `the penalty is applied to the personalized total not to the base score`() {
        // Given
        val first = article("first", "preferred", base = 100)
        val penalized = article(
            "penalized",
            "preferred",
            base = 91,
            tags = listOf(ArticleTag("oauth", "OAuth")),
        )
        val alternative = article("alternative", "other", base = 80)
        val articles = listOf(first, penalized, alternative)
        val preferences = preferencesFor(
            articles,
            sourceWeights = mapOf("preferred" to 4.0),
            topicEntries = mapOf("oauth" to PreferenceEntry(2.0, 2)),
        )
        val personalizedBefore = PersonalizedScore.calculate(penalized, preferences)
        val articleScoreBefore = penalized.score.copy()

        // When
        val deck = build(articles, selectedCategory = Category.IAM, preferences = preferences)

        // Then
        val selected = deck.candidates.single { it.article.id == penalized.id }
        assertEquals(-8.0, selected.sequencing.sameSourcePenalty)
        assertEquals(personalizedBefore.total - 8.0, selected.sequencing.score)
        assertEquals(personalizedBefore, selected.score)
        assertEquals(articleScoreBefore, selected.article.score)
        assertEquals(91, selected.score.base)
        assertEquals(4.0, selected.score.sourcePreference)
        assertEquals(2.0, selected.score.topicPreference)
        assertEquals(0.5, selected.score.exploration)
    }

    @Test
    fun `ties after penalties fall through to section 58 order`() {
        // Given
        val articles = listOf(
            article("head", "source-a", base = 100),
            article("penalized", "source-a", base = 99),
            article("unpenalized", "source-b", base = 91),
        )

        // When
        val deck = build(articles, selectedCategory = Category.IAM)

        // Then
        assertEquals(listOf("head", "penalized", "unpenalized"), deck.ids())
        assertEquals(91.0, deck.candidates[1].sequencing.score)
        assertEquals(91.0, deck.candidates[2].sequencing.score)
        assertEquals(-8.0, deck.candidates[1].sequencing.sameSourcePenalty)
    }

    @Test
    fun `sequencing modifies nothing`() {
        // Given
        val unseen = article("unseen", "source-a", base = 100)
        val opened = article("opened", "source-a", base = 99)
        val articles = listOf(unseen, opened)
        val records = mapOf(opened.id to record(opened, ArticleStatus.OPENED))
        val preferences = preferencesFor(articles, sourceWeights = mapOf("source-a" to 2.0))
        val articlesBefore = articles.map { it.copy(score = it.score.copy(), tags = it.tags.toList()) }
        val recordsBefore = records.mapValues { (_, value) ->
            value.copy(article = value.article.copy(score = value.article.score.copy()))
        }
        val preferencesBefore = preferences.copy(
            sources = preferences.sources.toMap(),
            topics = preferences.topics.toMap(),
        )

        // When
        build(articles, Category.IAM, records, preferences)

        // Then
        assertEquals(articlesBefore, articles)
        assertEquals(recordsBefore, records)
        assertEquals(preferencesBefore, preferences)
    }

    @Test
    fun `identical inputs produce identical decks`() {
        // Given
        val articles = categoryFixture()
        val preferences = preferencesFor(articles)

        // When
        val first = build(articles, selectedCategory = null, preferences = preferences)
        val second = build(articles, selectedCategory = null, preferences = preferences)

        // Then
        assertEquals(first, second)
        assertEquals(first.candidates.map { it.sequencing }, second.candidates.map { it.sequencing })
    }

    @Test
    fun `the deck is a permutation of the candidate set`() {
        // Given
        val eligible = categoryFixture()
        val saved = article("saved", "source-saved", base = 101)
        val articles = listOf(saved) + eligible
        val records = mapOf(saved.id to record(saved, ArticleStatus.SAVED))

        // When
        val deck = build(articles, selectedCategory = null, records = records)

        // Then
        assertEquals(eligible.map { it.id }.sorted(), deck.ids().sorted())
        assertEquals(deck.candidates.size, deck.candidates.map { it.article.id }.toSet().size)
        assertEquals(4, deck.availableCount)
        assertEquals(3, deck.remainingCount)
    }

    @Test
    fun `the held card still wins`() {
        // Given
        val first = article("a-first", "source-a", base = 100)
        val held = article("a-second", "source-a", base = 99)
        val other = article("b", "source-b", base = 95)
        val articles = listOf(first, held, other)
        val records = mapOf(held.id to record(held, ArticleStatus.OPENED))

        // When
        val deck = DiscoverDeck.build(
            articles = articles,
            records = records,
            preferences = preferencesFor(articles),
            selectedCategory = Category.IAM,
            heldArticleId = held.id,
        )

        // Then
        assertTrue(deck.candidates.indexOfFirst { it.article.id == held.id } > 0)
        assertEquals(held, deck.article)
        assertEquals(3, deck.availableCount)
        assertEquals(2, deck.remainingCount)
    }

    @Test
    fun `the five-key fixture has the sequenced order`() {
        // Given
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
        val preferences = preferencesFor(
            inputs,
            sourceWeights = mapOf(
                "publication-newest-source" to 1.0,
                "publication-older-source" to 1.0,
                "publication-unknown-source" to 1.0,
                "source-a" to 2.0,
                "source-b" to 2.0,
            ),
        )

        // When
        val deck = build(inputs, selectedCategory = null, preferences = preferences)

        // Then
        assertEquals(
            listOf(
                totalWinner.id,
                baseWinner.id,
                publicationNewest.id,
                publicationOlder.id,
                publicationUnknown.id,
                sourceAFirst.id,
                sourceB.id,
                sourceASecond.id,
            ),
            deck.ids(),
        )
        assertEquals(0.0, deck.candidates.last().sequencing.sameSourcePenalty)
    }

    private fun build(
        articles: List<Article>,
        selectedCategory: Category?,
        records: Map<String, ArticleRecord> = emptyMap(),
        preferences: LocalState.Preferences = preferencesFor(articles),
    ): DiscoverDeckState = DiscoverDeck.build(
        articles = articles,
        records = records,
        preferences = preferences,
        selectedCategory = selectedCategory,
        heldArticleId = null,
    )

    private fun preferencesFor(
        articles: List<Article>,
        sourceWeights: Map<String, Double> = emptyMap(),
        topicEntries: Map<String, PreferenceEntry> = emptyMap(),
    ): LocalState.Preferences = LocalState.Preferences(
        sources = articles.associate { candidate ->
            candidate.source.id to PreferenceEntry(
                weight = sourceWeights[candidate.source.id] ?: 0.0,
                interactions = 3,
            )
        },
        topics = topicEntries,
    )

    private fun categoryFixture(): List<Article> = listOf(
        article("1", "source-a", base = 100, category = Category.TECHNOLOGY),
        article("2", "source-b", base = 99, category = Category.TECHNOLOGY),
        article("3", "source-c", base = 96, category = Category.TECHNOLOGY),
        article("4", "source-d", base = 94, category = Category.SCIENCE),
    )

    private fun DiscoverDeckState.ids(): List<String> = candidates.map { it.article.id }

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
        base: Int,
        publishedAt: Instant? = Instant.parse("2026-08-20T12:00:00Z"),
        category: Category = Category.IAM,
        tags: List<ArticleTag> = emptyList(),
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
        tags = tags,
        contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
        score = ArticleScore(base, 50, 20, 15, 5, 1),
    )
}
