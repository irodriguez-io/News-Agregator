package io.irodriguez.intentionalreading

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverLayoutTags
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoverScreenLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mastheadCardAndOperationalBlockKeepAmendmentSevenOrder() {
        val viewport = setDiscoverContent(
            width = 360.dp,
            height = ORDERING_VIEWPORT_HEIGHT,
        )

        val masthead = fullBounds(
            composeTestRule.onNodeWithTag(DiscoverLayoutTags.MASTHEAD),
        )
        val card = fullBounds(
            composeTestRule.onNodeWithTag(DiscoverLayoutTags.CARD),
        )
        val operationalBlock = fullBounds(
            composeTestRule.onNodeWithTag(DiscoverLayoutTags.OPERATIONAL_BLOCK),
        )

        assertEquals(viewport.widthPx, rootBounds().width, PIXEL_TOLERANCE)
        assertTrue("Expected the masthead above the card: $masthead then $card", masthead.top < card.top)
        assertTrue(
            "Expected the card above the operational block: $card then $operationalBlock",
            card.top < operationalBlock.top,
        )
    }

    @Test
    fun longDatasetCardFitsAboveTheFoldAt360Dp() {
        assertLongDatasetCardFits(
            width = 360.dp,
            height = HANDSET_360_CONTENT_HEIGHT,
        )
    }

    @Test
    fun longDatasetCardFitsAboveTheFoldAt411Dp() {
        assertLongDatasetCardFits(
            width = 411.dp,
            height = HANDSET_411_CONTENT_HEIGHT,
        )
    }

    private fun assertLongDatasetCardFits(width: Dp, height: Dp) {
        val viewport = setDiscoverContent(width, height)
        val elements = listOf(
            "headline" to fullBounds(composeTestRule.onNodeWithText(LONG_DATASET_TITLE)),
            "excerpt" to fullBounds(composeTestRule.onNodeWithText(LONG_DATASET_EXCERPT)),
            "tags" to fullBounds(
                composeTestRule.onNodeWithContentDescription(TOPICS_DESCRIPTION),
            ),
            "Not interested" to fullBounds(
                composeTestRule.onNodeWithContentDescription("Not interested"),
            ),
            "Read article" to fullBounds(
                composeTestRule.onNodeWithContentDescription("Read article in the system browser"),
            ),
            "Save for later" to fullBounds(
                composeTestRule.onNodeWithContentDescription("Save for later"),
            ),
        )

        assertEquals(viewport.widthPx, rootBounds().width, PIXEL_TOLERANCE)
        elements.forEach { (name, bounds) ->
            assertWithinViewport(name, bounds, viewport.bottomPx)
        }
    }

    private fun setDiscoverContent(width: Dp, height: Dp): Viewport {
        var widthPx = 0f
        var bottomPx = 0f
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(
                    DpSize(width = width, height = height),
                ),
            ) {
                widthPx = with(LocalDensity.current) { width.toPx() }
                bottomPx = with(LocalDensity.current) { height.toPx() }
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    DiscoverScreen(
                        state = longDatasetCardState(),
                        degraded = false,
                        selectedCategory = null,
                        onCategorySelected = {},
                        onRetry = {},
                        onViewReadLater = {},
                        onDismiss = {},
                        onReadArticle = {},
                        onSave = {},
                        onMarkRead = {},
                        onSwipeCommit = { _: Article, _: ArticleAction, complete -> complete(true) },
                        modifier = Modifier.testTag(ROOT_TAG),
                    )
                }
            }
        }
        return Viewport(widthPx = widthPx, bottomPx = bottomPx)
    }

    private fun rootBounds(): Rect = composeTestRule.onNodeWithTag(ROOT_TAG)
        .fetchSemanticsNode().boundsInRoot

    private fun fullBounds(interaction: SemanticsNodeInteraction): Rect {
        val node = interaction.fetchSemanticsNode()
        val position = node.positionInRoot
        return Rect(
            left = position.x,
            top = position.y,
            right = position.x + node.size.width,
            bottom = position.y + node.size.height,
        )
    }

    private fun assertWithinViewport(name: String, bounds: Rect, viewportBottomPx: Float) {
        assertTrue("Expected $name to start within the viewport, but was $bounds", bounds.top >= 0f)
        assertTrue(
            "Expected $name to clear the fold at $viewportBottomPx px, but was $bounds",
            bounds.bottom <= viewportBottomPx + PIXEL_TOLERANCE,
        )
    }

    private fun longDatasetCardState() = DiscoverUiState.Card(
        article = Article(
            id = "science-long-title",
            title = LONG_DATASET_TITLE,
            url = "https://www.science.org/doi/10.1126/science.example",
            source = ArticleSource(id = "science", name = "Science / AAAS"),
            category = Category.SCIENCE,
            publishedAt = Instant.parse("2026-08-27T12:00:00Z"),
            author = null,
            excerpt = LONG_DATASET_EXCERPT,
            readingTimeMinutes = null,
            tags = listOf(
                ArticleTag(id = "cystic-fibrosis", label = "Cystic fibrosis"),
                ArticleTag(id = "gene-therapy", label = "Gene therapy"),
            ),
            contentType = ArticleContentType(
                id = ContentTypeId.RESEARCH_REPORTING,
                label = "Research & Science",
            ),
            score = ArticleScore(
                base = 90,
                sourceQuality = 50,
                contentType = 20,
                freshness = 15,
                topicSignal = 5,
                metadata = 0,
            ),
        ),
        publicationAge = "4d",
        availableCount = 181,
        remainingCount = 180,
        isOpened = false,
        contentFreshness = null,
        failedRefreshDisclosure = null,
        refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
    )

    private data class Viewport(
        val widthPx: Float,
        val bottomPx: Float,
    )

    private companion object {
        val ORDERING_VIEWPORT_HEIGHT = 640.dp
        val HANDSET_360_CONTENT_HEIGHT = 444.dp
        val HANDSET_411_CONTENT_HEIGHT = 693.dp
        const val PIXEL_TOLERANCE = 0.5f
        const val ROOT_TAG = "discover-screen-width-root"
        const val LONG_DATASET_TITLE =
            "Nonviral delivery of chemically modified tRNA rescues nonsense mutations in cystic fibrosis | Science"
        const val LONG_DATASET_EXCERPT =
            "Suppressor transfer RNAs can rescue disease-causing nonsense mutations by promoting readthrough."
        const val TOPICS_DESCRIPTION = "Article topics: Cystic fibrosis, Gene therapy"
    }
}
