package io.irodriguez.intentionalreading

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.ui.components.ArticleCard
import io.irodriguez.intentionalreading.ui.gesture.SwipeGesture
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleCardScrollGestureTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aCardAcceptsAHorizontalSwipeWhileItsAncestorScrollIsAnimating() {
        // Given
        val visibleArticle = article(id = "visible", title = "Visible article")
        val committedArticles = mutableListOf<Article>()
        lateinit var scrollState: ScrollState
        lateinit var scrollScope: CoroutineScope
        var intentSlopPx = 0f
        var thresholdPx = 0f

        composeTestRule.setContent {
            scrollState = rememberScrollState()
            scrollScope = rememberCoroutineScope()
            intentSlopPx = with(LocalDensity.current) { SwipeGesture.INTENT_SLOP_DP.dp.toPx() }
            thresholdPx = with(LocalDensity.current) { SwipeGesture.THRESHOLD_DP.dp.toPx() }
            IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                Column(
                    modifier = Modifier
                        .height(700.dp)
                        .verticalScroll(scrollState),
                ) {
                    ArticleCard(
                        state = card(visibleArticle),
                        onDismiss = {},
                        onReadArticle = {},
                        onSave = {},
                        onMarkRead = {},
                        onSwipeCommit = { committedArticle, _: ArticleAction, complete ->
                            committedArticles += committedArticle
                            complete(true)
                        },
                        reducedMotion = { true },
                        modifier = Modifier.testTag(CARD_TAG),
                    )
                    Spacer(modifier = Modifier.height(700.dp))
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // When the visible card is swiped while its ancestor's programmatic scroll is mid-flight
        composeTestRule.runOnUiThread {
            scrollScope.launch {
                scrollState.animateScrollTo(
                    value = scrollState.maxValue,
                    animationSpec = tween(durationMillis = SCROLL_DURATION_MS),
                )
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.runOnIdle {
            assertTrue("Expected the ancestor scroll to be in flight.", scrollState.isScrollInProgress)
        }
        composeTestRule.onNodeWithTag(CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(intentSlopPx + thresholdPx + 1f, 0f))
            up()
        }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Then the card receives the gesture and commits its action
        assertEquals(listOf(visibleArticle), committedArticles)
    }

    private fun card(article: Article) = DiscoverUiState.Card(
        article = article,
        publicationAge = "Today",
        availableCount = 1,
        remainingCount = 1,
        isOpened = false,
        contentFreshness = null,
        failedRefreshDisclosure = null,
        refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
    )

    private fun article(id: String, title: String) = Article(
        id = id,
        title = title,
        url = "https://example.com/$id",
        source = ArticleSource(id = "source-$id", name = "Source $id"),
        category = Category.IAM,
        publishedAt = Instant.parse("2026-08-28T12:00:00Z"),
        author = null,
        excerpt = "Excerpt for $title",
        readingTimeMinutes = null,
        tags = emptyList(),
        contentType = ArticleContentType(
            id = ContentTypeId.STANDARDS_UPDATE,
            label = "Standards Update",
        ),
        score = ArticleScore(
            base = 90,
            sourceQuality = 50,
            contentType = 20,
            freshness = 15,
            topicSignal = 5,
            metadata = 0,
        ),
    )

    private companion object {
        const val CARD_TAG = "article-card-during-scroll"
        const val SCROLL_DURATION_MS = 10_000
    }
}
