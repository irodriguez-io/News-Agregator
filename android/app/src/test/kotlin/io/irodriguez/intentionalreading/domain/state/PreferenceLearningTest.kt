package io.irodriguez.intentionalreading.domain.state

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

@RunWith(Parameterized::class)
class PreferenceLearningDeltaTest(
    private val event: PreferenceEvent,
    private val sourceDelta: Double,
    private val topicDelta: Double,
) {
    @Test
    fun `given empty preferences when an event is applied then its exact source and topic deltas are stored`() {
        val result = PreferenceLearning.apply(emptyPreferences(), article(), event)

        assertEquals(PreferenceEntry(sourceDelta, 1), result.sources["example"])
        assertEquals(PreferenceEntry(topicDelta, 1), result.topics["oauth"])
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun events(): List<Array<Any>> = listOf(
            arrayOf(PreferenceEvent.NOT_INTERESTED, -0.35, -0.20),
            arrayOf(PreferenceEvent.SAVE_FOR_LATER, 0.45, 0.30),
            arrayOf(PreferenceEvent.FIRST_OPEN, 0.10, 0.05),
            arrayOf(PreferenceEvent.MARK_READ, 0.25, 0.20),
        )
    }
}

class PreferenceLearningTest {
    @Test
    fun `given prior entries when each event is applied then reversed then exact prior values return`() {
        val prior = LocalState.Preferences(
            sources = mapOf(
                "example" to PreferenceEntry(weight = 1.2, interactions = 3),
                "other-source" to PreferenceEntry(weight = -0.7, interactions = 2),
            ),
            topics = mapOf(
                "oauth" to PreferenceEntry(weight = 0.8, interactions = 2),
                "other-topic" to PreferenceEntry(weight = -0.4, interactions = 1),
            ),
        )

        PreferenceEvent.entries.forEach { event ->
            val applied = PreferenceLearning.apply(prior, article(), event)
            val reversed = PreferenceLearning.reverse(applied, article(), event)

            assertEquals(prior, reversed, event.name)
        }
    }

    @Test
    fun `given accumulated deltas when weights are stored then they equal browser literals to the last bit`() {
        val accumulated = listOf(
            PreferenceEvent.FIRST_OPEN,
            PreferenceEvent.SAVE_FOR_LATER,
            PreferenceEvent.MARK_READ,
            PreferenceEvent.NOT_INTERESTED,
        ).fold(emptyPreferences()) { preferences, event ->
            PreferenceLearning.apply(preferences, article(), event)
        }

        assertEquals(0.45.toBits(), accumulated.sources.getValue("example").weight.toBits())
        assertEquals(0.35.toBits(), accumulated.topics.getValue("oauth").weight.toBits())
    }

    @Test
    fun `given a source at positive five when save is applied then weight stays clamped and count increments`() {
        val prior = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 5.0, interactions = 7)),
            topics = emptyMap(),
        )

        val result = PreferenceLearning.apply(
            prior,
            article(tags = emptyList()),
            PreferenceEvent.SAVE_FOR_LATER,
        )

        assertEquals(PreferenceEntry(weight = 5.0, interactions = 8), result.sources["example"])
    }

    @Test
    fun `given a source at negative five when not interested is applied then weight stays clamped and count increments`() {
        val prior = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = -5.0, interactions = 4)),
            topics = emptyMap(),
        )

        val result = PreferenceLearning.apply(
            prior,
            article(tags = emptyList()),
            PreferenceEvent.NOT_INTERESTED,
        )

        assertEquals(PreferenceEntry(weight = -5.0, interactions = 5), result.sources["example"])
    }

    @Test
    fun `given a duplicated tag ID when an event is applied then its topic moves exactly once`() {
        val duplicatedTags = listOf(
            ArticleTag(id = "oauth", label = "OAuth"),
            ArticleTag(id = "oauth", label = "OAuth duplicate"),
        )

        val result = PreferenceLearning.apply(
            emptyPreferences(),
            article(tags = duplicatedTags),
            PreferenceEvent.MARK_READ,
        )

        assertEquals(PreferenceEntry(weight = 0.20, interactions = 1), result.topics["oauth"])
        assertEquals(1, result.topics.size)
    }

    @Test
    fun `given an absent entry and a zero-count entry when reversed then both are no-ops`() {
        val prior = LocalState.Preferences(
            sources = emptyMap(),
            topics = mapOf("oauth" to PreferenceEntry(weight = 2.5, interactions = 0)),
        )

        val result = PreferenceLearning.reverse(prior, article(), PreferenceEvent.SAVE_FOR_LATER)

        assertEquals(prior, result)
    }

    @Test
    fun `given one exact interaction when reversed to zero then its entries are removed`() {
        val prior = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 0.45, interactions = 1)),
            topics = mapOf("oauth" to PreferenceEntry(weight = 0.30, interactions = 1)),
        )

        val result = PreferenceLearning.reverse(prior, article(), PreferenceEvent.SAVE_FOR_LATER)

        assertEquals(emptyMap(), result.sources)
        assertEquals(emptyMap(), result.topics)
    }

    @Test
    fun `given an approved forced tag when an event is applied then it trains as a canonical topic`() {
        val forcedTag = ArticleTag(id = "forced-topic", label = "Forced Topic")

        val result = PreferenceLearning.apply(
            emptyPreferences(),
            article(tags = listOf(forcedTag)),
            PreferenceEvent.FIRST_OPEN,
        )

        assertEquals(PreferenceEntry(weight = 0.05, interactions = 1), result.topics["forced-topic"])
    }

    @Test
    fun `given empty preferences when only one article is trained then only reached entries are created`() {
        val prior = emptyPreferences()

        val result = PreferenceLearning.apply(prior, article(), PreferenceEvent.NOT_INTERESTED)

        assertEquals(emptyPreferences(), prior)
        assertEquals(setOf("example"), result.sources.keys)
        assertEquals(setOf("oauth"), result.topics.keys)
        assertEquals(null, result.sources["unreached-source"])
        assertEquals(null, result.topics["unreached-topic"])
    }
}

private fun emptyPreferences(): LocalState.Preferences = LocalState.Preferences(
    sources = emptyMap(),
    topics = emptyMap(),
)

private fun article(
    tags: List<ArticleTag> = listOf(ArticleTag(id = "oauth", label = "OAuth")),
): Article = Article(
    id = "00000000000000000001",
    title = "A deliberate article",
    url = "https://example.com/article",
    source = ArticleSource(id = "example", name = "Example Source"),
    category = Category.IAM,
    publishedAt = Instant.parse("2026-08-20T12:00:00Z"),
    author = "Author",
    excerpt = "A useful excerpt.",
    readingTimeMinutes = 7,
    tags = tags,
    contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
    score = ArticleScore(91, 50, 20, 15, 5, 1),
)
