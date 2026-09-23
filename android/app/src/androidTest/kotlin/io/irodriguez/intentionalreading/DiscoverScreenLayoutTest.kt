package io.irodriguez.intentionalreading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
import io.irodriguez.intentionalreading.ui.components.ArticleCard
import io.irodriguez.intentionalreading.ui.gesture.SwipeGesture
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
    fun theArrivingCardAcceptsAndTracksASwipeWhileItsEntranceIsRunning() {
        // Given a committed departure and an arriving article still rising and fading.
        val host = startReplacementEntrance()
        val arriving = composeTestRule.onNodeWithText(host.arriving.title)
        val before = arriving.fetchSemanticsNode().positionInRoot
        val firstTravel = host.intentSlopPx + 1f
        val secondTravel = host.density * 4f

        // When the reader starts moving immediately, without advancing the entrance clock.
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(firstTravel, 0f))
        }
        composeTestRule.waitForIdle()

        // Then the arriving article follows the first pointer movement, and the next one too.
        val firstPosition = arriving.fetchSemanticsNode().positionInRoot
        assertTrue("The arriving article must track the first movement", firstPosition.x > before.x + firstTravel / 2f)
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            moveBy(Offset(secondTravel, 0f))
        }
        composeTestRule.waitForIdle()
        val secondPosition = arriving.fetchSemanticsNode().positionInRoot
        assertTrue("The arriving article must keep following the pointer", secondPosition.x > firstPosition.x + secondTravel / 2f)
        assertTrue(
            "the swipe must land while the entrance is still running",
            composeTestRule.mainClock.currentTime - host.entranceObservedAt < SwipeGesture.ENTRANCE_DURATION_MS,
        )
        assertEquals(listOf(host.leaving.id to ArticleAction.SAVE), host.commits)
        composeTestRule.onNodeWithText(host.leaving.title).assertDoesNotExist()

        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput { cancel() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }

    @Test
    fun aSwipeCommittedDuringAnEntranceIsAttributedToTheArrivingArticle() {
        // Given the old article has committed and the replacement entrance is still running.
        val host = startReplacementEntrance()

        // When the reader crosses the threshold and releases before that entrance finishes.
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(-(host.thresholdPx + host.intentSlopPx + 1f), 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "the swipe must land while the entrance is still running",
            composeTestRule.mainClock.currentTime - host.entranceObservedAt < SwipeGesture.ENTRANCE_DURATION_MS,
        )
        assertEquals("The arriving action must still wait for its exit", 1, host.commits.size)
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        // Then the action belongs to the arriving article, exactly once, after the departure's action.
        assertEquals(
            listOf(host.leaving.id to ArticleAction.SAVE, host.arriving.id to ArticleAction.DISMISS),
            host.commits,
        )
        composeTestRule.mainClock.autoAdvance = true
    }

    private fun startReplacementEntrance(): EntranceHost {
        val template = longDatasetCardState().article.copy(excerpt = "", tags = emptyList())
        val leaving = template.copy(id = "leaving", title = "Leaving article")
        val arriving = template.copy(id = "arriving", title = "Arriving article")
        val host = EntranceHost(leaving, arriving, mutableStateOf(leaving))
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 640.dp)),
            ) {
                host.density = LocalDensity.current.density
                host.intentSlopPx = with(LocalDensity.current) { SwipeGesture.INTENT_SLOP_DP.dp.toPx() }
                host.thresholdPx = with(LocalDensity.current) { SwipeGesture.THRESHOLD_DP.dp.toPx() }
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Magenta).testTag(ENTRANCE_ROOT_TAG),
                    ) {
                        Box(Modifier.padding(24.dp)) {
                            ArticleCard(
                                state = longDatasetCardState().copy(article = host.current.value),
                                onDismiss = {},
                                onReadArticle = {},
                                onSave = {},
                                onMarkRead = {},
                                onSwipeCommit = { article, action, complete ->
                                    host.commits += article.id to action
                                    complete(true)
                                    if (article.id == leaving.id) host.current.value = arriving
                                },
                                reducedMotion = { false },
                                modifier = Modifier.testTag(ENTRANCE_CARD_TAG),
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val restingTop = composeTestRule.onNodeWithText(leaving.title).fetchSemanticsNode().positionInRoot.y
        val opaquePixel = entranceFillPixel(host)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(host.thresholdPx + host.intentSlopPx + 1f, 0f))
            up()
        }
        // Stop on the frame that processes the state action; never sleep through the arrival.
        var frames = 0
        while (host.current.value.id != arriving.id && frames < 25) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.waitForIdle()
            frames++
        }
        assertEquals("The departure must process the old article", arriving.id, host.current.value.id)
        composeTestRule.mainClock.advanceTimeBy(64)
        composeTestRule.waitForIdle()
        host.entranceObservedAt = composeTestRule.mainClock.currentTime

        // Establish the actual entrance, so the gesture assertions cannot pass with no animation.
        val arrivingTop = composeTestRule.onNodeWithText(arriving.title).fetchSemanticsNode().positionInRoot.y
        assertTrue(
            "The arriving article must still be below rest during its entrance: rest=$restingTop, arriving=$arrivingTop",
            arrivingTop > restingTop + 0.5f && arrivingTop < restingTop + 12f * host.density,
        )
        val entrancePixel = entranceFillPixel(host)
        assertTrue("The arriving card must still be fading", colorDistance(opaquePixel, entrancePixel) > 0.01f)
        assertTrue("The arriving card must already be partly visible", colorDistance(Color.Magenta, entrancePixel) > 0.01f)
        return host
    }

    private fun entranceFillPixel(host: EntranceHost): Color {
        // The fixed host owns this geometry: centre width, below the card edge and above its content.
        val pixels = composeTestRule.onNodeWithTag(ENTRANCE_ROOT_TAG).captureToImage().toPixelMap()
        return pixels[(180f * host.density).toInt(), (48f * host.density).toInt()]
    }

    private fun colorDistance(first: Color, second: Color): Float =
        kotlin.math.abs(first.red - second.red) + kotlin.math.abs(first.green - second.green) +
            kotlin.math.abs(first.blue - second.blue)

    private data class EntranceHost(
        val leaving: Article,
        val arriving: Article,
        val current: MutableState<Article>,
        val commits: MutableList<Pair<String, ArticleAction>> = mutableListOf(),
        var density: Float = 0f,
        var intentSlopPx: Float = 0f,
        var thresholdPx: Float = 0f,
        var entranceObservedAt: Long = 0L,
    )

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
        const val ENTRANCE_ROOT_TAG = "card-entrance-root"
        const val ENTRANCE_CARD_TAG = "card-entrance"
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
