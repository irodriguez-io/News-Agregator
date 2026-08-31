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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferenceReconciliationTest {
    @Test
    fun `a pre-learning opened and read record folds exact source and deduplicated topic deltas`() {
        val trainedArticle = article(
            id = "00000000000000000001",
            tags = listOf(
                ArticleTag("oauth", "OAuth"),
                ArticleTag("zero-trust", "Zero Trust"),
                ArticleTag("oauth", "OAuth duplicate"),
            ),
        )
        val state = state(
            record(
                article = trainedArticle,
                status = ArticleStatus.READ,
                signals = SignalsApplied(opened = true, saved = false, dismissed = false, read = true),
            ),
        )

        val result: LocalState = PreferenceReconciliation.reconcile(state)

        assertEquals(PreferenceEntry(weight = 0.35, interactions = 2), result.preferences.sources["example"])
        assertEquals(PreferenceEntry(weight = 0.25, interactions = 2), result.preferences.topics["oauth"])
        assertEquals(PreferenceEntry(weight = 0.25, interactions = 2), result.preferences.topics["zero-trust"])
        assertEquals(2, result.preferences.topics.size)
    }

    @Test
    fun `a second reconciliation returns the reconciled state unchanged`() {
        val initial = state(
            record(
                article = article("00000000000000000001"),
                status = ArticleStatus.READ,
                signals = SignalsApplied(opened = true, saved = false, dismissed = false, read = true),
            ),
        )

        val reconciled: LocalState = PreferenceReconciliation.reconcile(initial)

        assertEquals(reconciled, PreferenceReconciliation.reconcile(reconciled))
    }

    @Test
    fun `saved and dismissed flags fold their exact positive and negative deltas`() {
        val saved = article(
            id = "00000000000000000001",
            sourceId = "saved-source",
            tags = listOf(ArticleTag("saved-topic", "Saved Topic")),
        )
        val dismissed = article(
            id = "00000000000000000002",
            sourceId = "dismissed-source",
            tags = listOf(ArticleTag("dismissed-topic", "Dismissed Topic")),
        )
        val initial = state(
            record(
                article = saved,
                status = ArticleStatus.SAVED,
                signals = SignalsApplied(opened = false, saved = true, dismissed = false, read = false),
            ),
            record(
                article = dismissed,
                status = ArticleStatus.DISMISSED,
                signals = SignalsApplied(opened = false, saved = false, dismissed = true, read = false),
            ),
        )

        val result: LocalState = PreferenceReconciliation.reconcile(initial)

        assertEquals(PreferenceEntry(weight = 0.45, interactions = 1), result.preferences.sources["saved-source"])
        assertEquals(PreferenceEntry(weight = 0.30, interactions = 1), result.preferences.topics["saved-topic"])
        assertEquals(PreferenceEntry(weight = -0.35, interactions = 1), result.preferences.sources["dismissed-source"])
        assertEquals(PreferenceEntry(weight = -0.20, interactions = 1), result.preferences.topics["dismissed-topic"])
    }

    @Test
    fun `an over-count is lowered to the claimed count without changing weight`() {
        val trainedArticle = article(id = "00000000000000000001", tags = emptyList())
        val initial = state(
            record(
                article = trainedArticle,
                status = ArticleStatus.SAVED,
                signals = SignalsApplied(opened = false, saved = true, dismissed = false, read = false),
            ),
        ).copy(
            preferences = LocalState.Preferences(
                sources = mapOf("example" to PreferenceEntry(weight = 2.75, interactions = 3)),
                topics = emptyMap(),
            ),
        )

        val result: LocalState = PreferenceReconciliation.reconcile(initial)

        assertEquals(PreferenceEntry(weight = 2.75, interactions = 1), result.preferences.sources["example"])
    }

    @Test
    fun `article ID order is deterministic where clamping changes the result`() {
        val dismissedFirst = article(
            id = "00000000000000000001",
            sourceId = "shared-source",
            tags = emptyList(),
        )
        val savedSecond = article(
            id = "00000000000000000002",
            sourceId = "shared-source",
            tags = emptyList(),
        )
        val initial = state(
            record(
                article = savedSecond,
                status = ArticleStatus.SAVED,
                signals = SignalsApplied(opened = false, saved = true, dismissed = false, read = false),
            ),
            record(
                article = dismissedFirst,
                status = ArticleStatus.DISMISSED,
                signals = SignalsApplied(opened = false, saved = false, dismissed = true, read = false),
            ),
        ).copy(
            preferences = LocalState.Preferences(
                sources = mapOf("shared-source" to PreferenceEntry(weight = 4.9, interactions = 0)),
                topics = emptyMap(),
            ),
        )

        val first: LocalState = PreferenceReconciliation.reconcile(initial)
        val second: LocalState = PreferenceReconciliation.reconcile(initial)

        assertEquals(first, second)
        assertEquals(PreferenceEntry(weight = 5.0, interactions = 2), first.preferences.sources["shared-source"])
    }
}

private fun state(vararg records: ArticleRecord): LocalState = LocalState.default().copy(
    articles = records.associateBy { record -> record.article.id },
)

private fun record(
    article: Article,
    status: ArticleStatus,
    signals: SignalsApplied,
): ArticleRecord = ArticleRecord(
    article = article,
    status = status,
    firstSeenAt = now.minusSeconds(120),
    openedAt = now.minusSeconds(90).takeIf { signals.opened },
    savedAt = now.minusSeconds(60).takeIf { status == ArticleStatus.SAVED },
    dismissedAt = now.minusSeconds(60).takeIf { status == ArticleStatus.DISMISSED },
    readAt = now.minusSeconds(30).takeIf { status == ArticleStatus.READ },
    signalsApplied = signals,
)

private fun article(
    id: String,
    sourceId: String = "example",
    tags: List<ArticleTag> = listOf(ArticleTag("oauth", "OAuth")),
): Article = Article(
    id = id,
    title = "A deliberate article",
    url = "https://example.com/$id",
    source = ArticleSource(sourceId, "Source $sourceId"),
    category = Category.IAM,
    publishedAt = now.minusSeconds(3_600),
    author = null,
    excerpt = "A useful excerpt.",
    readingTimeMinutes = 7,
    tags = tags,
    contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
    score = ArticleScore(91, 50, 20, 15, 5, 1),
)

private val now: Instant = Instant.parse("2026-08-22T12:00:00Z")
