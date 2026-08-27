package io.irodriguez.intentionalreading.domain.ranking

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import java.time.Instant
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalizedScoreTest {
    @Test
    fun `a weighted source and three topics expose all five exact score components`() {
        val preferences = preferences(
            source = PreferenceEntry(weight = 2.4, interactions = 2),
            topics = mapOf(
                "oauth" to PreferenceEntry(weight = 1.5, interactions = 3),
                "scim" to PreferenceEntry(weight = 2.0, interactions = 1),
                "zero-trust" to PreferenceEntry(weight = -0.5, interactions = 4),
            ),
        )
        val article = article(
            base = 87,
            tags = listOf(
                ArticleTag("oauth", "OAuth"),
                ArticleTag("scim", "SCIM"),
                ArticleTag("zero-trust", "Zero Trust"),
            ),
        )

        val score = PersonalizedScore.calculate(article, preferences)

        assertEquals(94.4, score.total)
        assertEquals(87, score.base)
        assertEquals(2.4, score.sourcePreference)
        assertEquals(3.0, score.topicPreference)
        assertEquals(2.0, score.exploration)
    }

    @Test
    fun `topic sums clamp at both six boundaries without changing stored weights`() {
        val article = article(
            tags = listOf(
                ArticleTag("one", "One"),
                ArticleTag("two", "Two"),
                ArticleTag("three", "Three"),
            ),
        )
        val positive = preferences(
            topics = mapOf(
                "one" to PreferenceEntry(5.0, 3),
                "two" to PreferenceEntry(4.0, 3),
                "three" to PreferenceEntry(3.0, 3),
            ),
        )
        val negative = preferences(
            topics = mapOf(
                "one" to PreferenceEntry(-5.0, 3),
                "two" to PreferenceEntry(-4.0, 3),
                "three" to PreferenceEntry(-3.0, 3),
            ),
        )
        val positiveBefore = positive.copy(topics = positive.topics.toMap())
        val negativeBefore = negative.copy(topics = negative.topics.toMap())

        val positiveScore = PersonalizedScore.calculate(article, positive)
        val negativeScore = PersonalizedScore.calculate(article, negative)

        assertEquals(6.0, positiveScore.topicPreference)
        assertEquals(-6.0, negativeScore.topicPreference)
        assertEquals(positiveBefore, positive)
        assertEquals(negativeBefore, negative)
    }

    @Test
    fun `an article without topics has no topic preference or topic exploration`() {
        val score = PersonalizedScore.calculate(
            article(tags = emptyList()),
            preferences(source = PreferenceEntry(weight = 0.0, interactions = 3)),
        )

        assertEquals(0.0, score.topicPreference)
        assertEquals(0.0, score.exploration)
    }

    @Test
    fun `source and topic exploration are capped at three`() {
        val score = PersonalizedScore.calculate(
            article(tags = listOf(ArticleTag("new-topic", "New Topic"))),
            preferences(
                source = PreferenceEntry(weight = 0.0, interactions = 0),
                topics = mapOf("new-topic" to PreferenceEntry(weight = 0.0, interactions = 0)),
            ),
        )

        assertEquals(3.0, score.exploration)
    }

    @Test
    fun `duplicate topic ids contribute once to both topic scoring inputs`() {
        val score = PersonalizedScore.calculate(
            article(
                tags = listOf(
                    ArticleTag("oauth", "OAuth"),
                    ArticleTag("oauth", "OAuth duplicate"),
                ),
            ),
            preferences(
                source = PreferenceEntry(weight = 0.0, interactions = 3),
                topics = mapOf("oauth" to PreferenceEntry(weight = 4.0, interactions = 2)),
            ),
        )

        assertEquals(4.0, score.topicPreference)
        assertEquals(0.5, score.exploration)
    }
}

@RunWith(Parameterized::class)
class PersonalizedScoreSourceExplorationTest(
    private val interactions: Int,
    private val expected: Double,
) {
    @Test
    fun `source exploration follows every row`() {
        val score = PersonalizedScore.calculate(
            article(tags = emptyList()),
            preferences(source = PreferenceEntry(weight = 0.0, interactions = interactions)),
        )

        assertEquals(expected, score.exploration)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} interactions -> {1}")
        fun rows(): List<Array<Any>> = listOf(
            arrayOf(0, 3.0),
            arrayOf(1, 2.0),
            arrayOf(2, 1.0),
            arrayOf(3, 0.0),
        )
    }
}

@RunWith(Parameterized::class)
class PersonalizedScoreTopicExplorationTest(
    private val interactions: Int,
    private val expected: Double,
) {
    @Test
    fun `topic exploration follows every row`() {
        val score = PersonalizedScore.calculate(
            article(tags = listOf(ArticleTag("topic", "Topic"))),
            preferences(
                source = PreferenceEntry(weight = 0.0, interactions = 3),
                topics = mapOf("topic" to PreferenceEntry(weight = 0.0, interactions = interactions)),
            ),
        )

        assertEquals(expected, score.exploration)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} interactions -> {1}")
        fun rows(): List<Array<Any>> = listOf(
            arrayOf(0, 2.0),
            arrayOf(1, 1.0),
            arrayOf(2, 0.5),
            arrayOf(3, 0.0),
        )
    }
}

private fun preferences(
    source: PreferenceEntry = PreferenceEntry(weight = 0.0, interactions = 3),
    topics: Map<String, PreferenceEntry> = emptyMap(),
): LocalState.Preferences = LocalState.Preferences(
    sources = mapOf("example" to source),
    topics = topics,
)

private fun article(
    base: Int = 91,
    tags: List<ArticleTag> = listOf(ArticleTag("oauth", "OAuth")),
): Article = Article(
    id = "00000000000000000001",
    title = "A deliberate article",
    url = "https://example.com/article",
    source = ArticleSource("example", "Example Source"),
    category = Category.IAM,
    publishedAt = Instant.parse("2026-08-20T12:00:00Z"),
    author = null,
    excerpt = "A useful excerpt.",
    readingTimeMinutes = 7,
    tags = tags,
    contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
    score = ArticleScore(
        base = base,
        sourceQuality = 50,
        contentType = 20,
        freshness = 15,
        topicSignal = 5,
        metadata = 1,
    ),
)
