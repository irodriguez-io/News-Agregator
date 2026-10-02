package io.irodriguez.intentionalreading

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverLayoutTags
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import java.time.Instant
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoverAutoScrollReducedMotionTest {
    @get:Rule
    val rule = createComposeRule(
        // Keep animation observable even when the runner disables device animations. The
        // production reducedMotion callback must choose the immediate branch itself.
        effectContext = object : MotionDurationScale {
            override val scaleFactor = 1f
        },
    )

    private val state = mutableStateOf(cardState())

    @Test
    fun automaticScrollsHonourReducedMotion_GivenReducedMotion_WhenNextCardArrives_ThenTargetOnFirstFrame() {
        checkScroll(reduced = true, nextCard = true)
    }

    @Test
    fun automaticScrollsHonourReducedMotion_GivenReducedMotion_WhenArticleOpens_ThenTargetOnFirstFrame() {
        checkScroll(reduced = true, nextCard = false)
    }

    @Test
    fun automaticScrollsHonourReducedMotion_GivenMotion_WhenNextCardArrives_ThenBetweenStartAndTargetPartwayThrough() {
        checkScroll(reduced = false, nextCard = true)
    }

    @Test
    fun automaticScrollsHonourReducedMotion_GivenMotion_WhenArticleOpens_ThenBetweenStartAndTargetPartwayThrough() {
        checkScroll(reduced = false, nextCard = false)
    }

    private fun checkScroll(reduced: Boolean, nextCard: Boolean) {
        // Given a 360 dp host short enough to require both automatic scrolls.
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 240.dp))) {
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    DiscoverScreen(
                        state = state.value,
                        degraded = false,
                        selectedCategory = null,
                        onCategorySelected = {},
                        onRetry = {},
                        onViewReadLater = {},
                        onDismiss = {},
                        onReadArticle = {},
                        onSave = {},
                        onMarkRead = {},
                        onSwipeCommit = { _, _, complete -> complete(true) },
                        reducedMotion = { reduced },
                        modifier = Modifier.testTag(SCROLL_HOST),
                    )
                }
            }
        }
        rule.waitForIdle()
        val start = scrollPosition()
        assertEquals("Fixture starts at the top", 0, start)
        assertTrue("Card top must require scrolling", target(nextCard = true) > start)
        assertTrue("Action reveal must require scrolling", target(nextCard = false) > start)
        rule.mainClock.autoAdvance = false

        // When a replacement article arrives, or the current article becomes opened.
        rule.runOnUiThread {
            state.value = if (nextCard) {
                state.value.copy(article = state.value.article.copy(id = "arriving-article"))
            } else {
                state.value.copy(isOpened = true)
            }
        }
        // Apply the trigger's composition/layout. Its effect awaits the next frame so that
        // the new card's measured bounds are available before choosing the scroll target.
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val target = target(nextCard)
        assertTrue("Triggered target must be reachable and nonzero: $target", target > start)
        assertEquals("Scroll waits for the first frame after the trigger's layout", start, scrollPosition())

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        if (reduced) {
            // Then that first frame lands exactly on the measured target.
            val actual = scrollPosition()
            Log.i(LOG_TAG, "reduced nextCard=$nextCard start=$start target=$target firstFrame=$actual")
            assertEquals("Reduced motion nextCard=$nextCard must land on the first frame", target, actual)
        } else {
            // Then around half the usual 300 ms scroll interval, motion is still in flight.
            rule.mainClock.advanceTimeBy(144)
            rule.waitForIdle()
            val middle = scrollPosition()
            Log.i(LOG_TAG, "motion nextCard=$nextCard start=$start target=$target at144ms=$middle")
            assertTrue(
                "Motion nextCard=$nextCard must be strictly between start=$start and target=$target, actual=$middle",
                middle > start && middle < target,
            )
            rule.mainClock.advanceTimeBy(1_000)
            rule.waitForIdle()
            assertEquals("Normal motion settles on the same measured target", target, scrollPosition())
        }
    }

    private fun scrollPosition(): Int = rule.onNodeWithTag(SCROLL_HOST)
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value().roundToInt()

    private fun target(nextCard: Boolean): Int {
        val root = rule.onNodeWithTag(SCROLL_HOST).fetchSemanticsNode()
        val card = rule.onNodeWithTag(DiscoverLayoutTags.CARD).fetchSemanticsNode()
        // Use the laid-out content coordinates, which remain un-clipped in this short host.
        val top = card.layoutInfo.coordinates.positionInParent().y
        val requested = if (nextCard) top.roundToInt() else {
            (top + card.size.height).roundToInt() - root.size.height
        }
        val maximum = root.config[SemanticsProperties.VerticalScrollAxisRange].maxValue().roundToInt()
        return requested.coerceIn(0, maximum)
    }

    private fun cardState() = DiscoverUiState.Card(
        article = Article(
            id = "initial-article",
            title = "A measured article for automatic Discover scrolling",
            url = "https://example.com/discover-scroll",
            source = ArticleSource(id = "science", name = "Science / AAAS"),
            category = Category.SCIENCE,
            publishedAt = Instant.parse("2026-08-27T12:00:00Z"),
            author = null,
            excerpt = "Opening this article reveals its actions in a short viewport.",
            readingTimeMinutes = null,
            tags = emptyList(),
            contentType = ArticleContentType(ContentTypeId.RESEARCH_REPORTING, "Research & Science"),
            score = ArticleScore(base = 90, sourceQuality = 50, contentType = 20, freshness = 15, topicSignal = 5, metadata = 0),
        ),
        publicationAge = "4d",
        availableCount = 3,
        remainingCount = 2,
        isOpened = false,
        contentFreshness = null,
        failedRefreshDisclosure = null,
        refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
    )

    private companion object {
        const val SCROLL_HOST = "discover-auto-scroll-host"
        const val LOG_TAG = "DiscoverAutoScrollTest"
    }
}
