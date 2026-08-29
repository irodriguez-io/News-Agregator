package io.irodriguez.intentionalreading

import androidx.compose.runtime.mutableStateOf
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleCardGestureTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aGestureCommitsAgainstTheCardItStartedOnWhenTheHeadArticleChanges() {
        // Given
        val firstArticle = article(id = "first", title = "First article")
        val secondArticle = article(id = "second", title = "Second article")
        val currentArticle = mutableStateOf(firstArticle)
        val committedArticles = mutableListOf<Article>()
        var intentSlopPx = 0f
        var thresholdPx = 0f

        composeTestRule.setContent {
            intentSlopPx = with(LocalDensity.current) { SwipeGesture.INTENT_SLOP_DP.dp.toPx() }
            thresholdPx = with(LocalDensity.current) { SwipeGesture.THRESHOLD_DP.dp.toPx() }
            IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                ArticleCard(
                    state = card(currentArticle.value),
                    onDismiss = {},
                    onReadArticle = {},
                    onSave = {},
                    onMarkRead = {},
                    onSwipeCommit = { committedArticle, _: ArticleAction, complete ->
                        committedArticles += committedArticle
                        complete(true)
                    },
                    modifier = Modifier.testTag(CARD_TAG),
                )
            }
        }

        val card = composeTestRule.onNodeWithTag(CARD_TAG)

        // When the gesture becomes live and the head article changes while the pointer stays down
        card.performTouchInput {
            down(center)
            moveBy(Offset(intentSlopPx + 1f, 0f))
        }
        composeTestRule.runOnUiThread {
            currentArticle.value = secondArticle
        }
        composeTestRule.waitForIdle()

        // And the same gesture crosses the commit threshold before lifting
        card.performTouchInput {
            moveBy(Offset(thresholdPx, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        // Then the persistent handler commits the article where the gesture began
        assertEquals(listOf(firstArticle), committedArticles)
    }

    private fun card(article: Article) = DiscoverUiState.Card(
        article = article,
        publicationAge = "Today",
        availableCount = 2,
        remainingCount = 2,
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
        publishedAt = Instant.parse("2026-08-27T12:00:00Z"),
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
        const val CARD_TAG = "article-card"
    }
}
