package io.irodriguez.intentionalreading

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
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

    @Test
    fun anUndoRestoredCardRisesAndFadesAndImmediatelyTracksASwipe() {
        // Given a committed swipe and its replacement settled at rest.
        val host = startReplacementEntrance()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals(listOf(host.leaving.id to ArticleAction.SAVE), host.commits)
        val restingTop = composeTestRule.onNodeWithText(host.arriving.title).fetchSemanticsNode().positionInRoot.y
        val opaquePixel = entranceFillPixel(host)

        // When undo makes the committed article the head again, through the card's normal input.
        composeTestRule.runOnIdle { host.current.value = host.leaving }
        composeTestRule.mainClock.advanceTimeBy(64)
        composeTestRule.waitForIdle()
        host.entranceObservedAt = composeTestRule.mainClock.currentTime

        // Then the restored head has the same bounded rise and partial fade as a replacement.
        assertEquals(host.leaving.id, host.current.value.id)
        composeTestRule.onNodeWithText(host.arriving.title).assertDoesNotExist()
        val restored = composeTestRule.onNodeWithText(host.leaving.title)
        val before = restored.fetchSemanticsNode().positionInRoot
        assertTrue(
            "The restored article must still be below rest during its entrance: rest=$restingTop, restored=${before.y}",
            before.y > restingTop + 0.5f && before.y < restingTop + SwipeGesture.ENTRANCE_RISE_DP * host.density,
        )
        val entrancePixel = entranceFillPixel(host)
        assertTrue("The restored card must still be fading", colorDistance(opaquePixel, entrancePixel) > 0.01f)
        assertTrue("The restored card must already be partly visible", colorDistance(Color.Magenta, entrancePixel) > 0.01f)

        // And it tracks the first pointer movement and the next without waiting for its entrance.
        val firstTravel = host.intentSlopPx + 1f
        val secondTravel = host.density * 4f
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(firstTravel, 0f))
        }
        composeTestRule.waitForIdle()
        val firstPosition = restored.fetchSemanticsNode().positionInRoot
        assertTrue("The restored article must track the first movement", firstPosition.x > before.x + firstTravel / 2f)
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            moveBy(Offset(secondTravel, 0f))
        }
        composeTestRule.waitForIdle()
        val secondPosition = restored.fetchSemanticsNode().positionInRoot
        assertTrue("The restored article must keep following the pointer", secondPosition.x > firstPosition.x + secondTravel / 2f)
        assertTrue(
            "the swipe must land while the entrance is still running",
            composeTestRule.mainClock.currentTime - host.entranceObservedAt < SwipeGesture.ENTRANCE_DURATION_MS,
        )
        assertEquals(listOf(host.leaving.id to ArticleAction.SAVE), host.commits)

        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput { cancel() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
    }

    @Test
    fun theCommitStartsWhenTheCardHasLeftTheViewportExactlyOnce() {
        val host = departureHost()
        val releasedAt = releaseDepartureSwipe(host)
        assertEquals("A visible card must not commit at release", 0, host.commits.size)

        awaitEarlyDeparture(host, releasedAt)
        assertEquals(listOf(host.leaving.id to ArticleAction.SAVE), host.commits)
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals("Curve completion must not request a second commit", 1, host.commits.size)
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun aSwipeOnTheArrivingCardWhileTheOldExitIsStillFinishing() {
        val host = departureHost { current, article, complete ->
            complete(true)
            if (article.id == current.leaving.id) current.current.value = current.arriving
        }
        val releasedAt = releaseDepartureSwipe(host)
        awaitEarlyDeparture(host, releasedAt)
        composeTestRule.mainClock.advanceTimeBy(32)
        composeTestRule.waitForIdle()
        val arriving = composeTestRule.onNodeWithText(host.arriving.title)
        val before = arriving.fetchSemanticsNode().positionInRoot
        val firstTravel = host.intentSlopPx + 1f

        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(-firstTravel, 0f))
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "The replacement must track its very first movement during the old exit",
            arriving.fetchSemanticsNode().positionInRoot.x < before.x - firstTravel / 2f,
        )
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            moveBy(Offset(-host.thresholdPx, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertTrue(
            "The replacement swipe must finish before the old exit curve would finish",
            composeTestRule.mainClock.currentTime - releasedAt < SwipeGesture.EXIT_DURATION_MS,
        )
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals(
            listOf(host.leaving.id to ArticleAction.SAVE, host.arriving.id to ArticleAction.DISMISS),
            host.commits,
        )
        composeTestRule.onNodeWithText(host.leaving.title).assertDoesNotExist()
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun aFailedSaveReturnsTheCardWhereverTheExitHadReached() {
        var finishSave: ((Boolean) -> Unit)? = null
        val host = departureHost { _, _, complete -> finishSave = complete }
        val restingPosition = composeTestRule.onNodeWithText(host.leaving.title).fetchSemanticsNode().positionInRoot
        val opaquePixel = entranceFillPixel(host)
        val releasedAt = releaseDepartureSwipe(host)
        awaitEarlyDeparture(host, releasedAt)
        composeTestRule.mainClock.advanceTimeByFrame()
        assertTrue(composeTestRule.mainClock.currentTime - releasedAt < SwipeGesture.EXIT_DURATION_MS)
        composeTestRule.runOnIdle { checkNotNull(finishSave)(false) }

        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals(host.leaving.id, host.current.value.id)
        composeTestRule.onNodeWithText(host.arriving.title).assertDoesNotExist()
        val restoredPosition = composeTestRule.onNodeWithText(host.leaving.title).fetchSemanticsNode().positionInRoot
        assertEquals(restingPosition.x, restoredPosition.x, 0.5f)
        assertEquals(restingPosition.y, restoredPosition.y, 0.5f)
        assertTrue("A failed save restores full opacity", colorDistance(opaquePixel, entranceFillPixel(host)) < 0.01f)
        assertEquals("The cancelled exit must not commit again", 1, host.commits.size)

        releaseDepartureSwipe(host, direction = -1f)
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals(
            listOf(host.leaving.id to ArticleAction.SAVE, host.leaving.id to ArticleAction.DISMISS),
            host.commits,
        )
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun whereTheCardCannotLeaveTheCurvesCompletionStartsTheCommit() {
        val host = departureHost(width = 840.dp)
        releaseDepartureSwipe(host)
        composeTestRule.mainClock.advanceTimeBy(200)
        composeTestRule.waitForIdle()
        assertEquals("The wide card is still partly in the viewport", 0, host.commits.size)
        composeTestRule.mainClock.advanceTimeBy(160)
        composeTestRule.waitForIdle()
        assertEquals(listOf(host.leaving.id to ArticleAction.SAVE), host.commits)
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals(1, host.commits.size)
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun reducedMotionIsUnchangedForDepartureAndArrival() {
        val host = departureHost(reducedMotion = true) { current, _, complete ->
            complete(true)
            current.current.value = current.arriving
        }
        val restingTop = composeTestRule.onNodeWithText(host.leaving.title).fetchSemanticsNode().positionInRoot.y
        val opaquePixel = entranceFillPixel(host)
        val releasedAt = releaseDepartureSwipe(host)
        // Touch injection itself advances the clock; allow only frame dispatch, not an exit curve.
        composeTestRule.mainClock.advanceTimeBy(32)
        composeTestRule.waitForIdle()
        assertTrue(composeTestRule.mainClock.currentTime - releasedAt < 100)
        assertEquals(listOf(host.leaving.id to ArticleAction.SAVE), host.commits)
        assertEquals(host.arriving.id, host.current.value.id)
        assertEquals(
            restingTop,
            composeTestRule.onNodeWithText(host.arriving.title).fetchSemanticsNode().positionInRoot.y,
            0.5f,
        )
        assertTrue("Reduced motion has no arrival fade", colorDistance(opaquePixel, entranceFillPixel(host)) < 0.01f)
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
        assertEquals(1, host.commits.size)
        composeTestRule.mainClock.autoAdvance = true
    }

    private fun departureHost(
        width: Dp = 360.dp,
        reducedMotion: Boolean = false,
        onCommit: (EntranceHost, Article, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    ): EntranceHost {
        val template = longDatasetCardState().article.copy(excerpt = "", tags = emptyList())
        val leaving = template.copy(id = "leaving", title = "Leaving article")
        val arriving = template.copy(id = "arriving", title = "Arriving article")
        val host = EntranceHost(leaving, arriving, mutableStateOf(leaving))
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, 640.dp))) {
                val configuration = Configuration(LocalConfiguration.current).apply {
                    screenWidthDp = width.value.toInt()
                }
                CompositionLocalProvider(LocalConfiguration provides configuration) {
                    host.density = LocalDensity.current.density
                    host.intentSlopPx = with(LocalDensity.current) { SwipeGesture.INTENT_SLOP_DP.dp.toPx() }
                    host.thresholdPx = with(LocalDensity.current) { SwipeGesture.THRESHOLD_DP.dp.toPx() }
                    IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                        Box(Modifier.fillMaxSize().background(Color.Magenta).testTag(ENTRANCE_ROOT_TAG)) {
                            Box(Modifier.padding(24.dp)) {
                                ArticleCard(
                                    state = longDatasetCardState().copy(article = host.current.value),
                                    onDismiss = {},
                                    onReadArticle = {},
                                    onSave = {},
                                    onMarkRead = {},
                                    onSwipeCommit = { article, action, complete ->
                                        host.commits += article.id to action
                                        onCommit(host, article, complete)
                                    },
                                    reducedMotion = { reducedMotion },
                                    modifier = Modifier.testTag(ENTRANCE_CARD_TAG),
                                )
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        return host
    }

    private fun releaseDepartureSwipe(host: EntranceHost, direction: Float = 1f): Long {
        // Record before injection, conservatively including its 16–32 ms in the exit deadline.
        val releasedAt = composeTestRule.mainClock.currentTime
        composeTestRule.onNodeWithTag(ENTRANCE_CARD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(direction * (host.thresholdPx + host.intentSlopPx + 1f), 0f))
            up()
        }
        composeTestRule.waitForIdle()
        return releasedAt
    }

    private fun awaitEarlyDeparture(host: EntranceHost, releasedAt: Long) {
        while (host.commits.isEmpty() && composeTestRule.mainClock.currentTime - releasedAt < 192) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.waitForIdle()
        }
        assertEquals("Departure must request the commit before the 300 ms exit finishes", 1, host.commits.size)
        assertTrue(composeTestRule.mainClock.currentTime - releasedAt < SwipeGesture.EXIT_DURATION_MS)
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
        // Given the long dataset card at the supported handset width.
        val viewport = setDiscoverContent(width, height)
        val elements = listOf(
            "headline" to fullBounds(composeTestRule.onNodeWithText(LONG_DATASET_TITLE)),
            "excerpt" to fullBounds(composeTestRule.onNodeWithText(LONG_DATASET_EXCERPT)),
            "tags" to fullBounds(
                composeTestRule.onNodeWithContentDescription(TOPICS_DESCRIPTION),
            ),
            "Skip, not interested" to fullBounds(
                composeTestRule.onNodeWithContentDescription("Skip, not interested"),
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
            assertTrue("Expected $name inside the viewport horizontally: $bounds", bounds.left >= 0f)
            assertTrue("Expected $name inside the viewport horizontally: $bounds", bounds.right <= viewport.widthPx + PIXEL_TOLERANCE)
        }

        // Then each visible label stays on one line without visual overflow.
        listOf("Skip", "Save", "Read article").forEach { label ->
            val results = mutableListOf<TextLayoutResult>()
            composeTestRule.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getTextLayoutResult ->
                    assertTrue("Expected a text layout result for $label", getTextLayoutResult(results))
                }
            assertEquals("Expected one text layout result for $label", 1, results.size)
            val result = results.single()
            assertTrue(
                "Expected $label on one line without visual overflow at $width: " +
                    "lineCount=${result.lineCount}, maxIntrinsicWidth=${result.multiParagraph.maxIntrinsicWidth}, " +
                    "size.width=${result.size.width}",
                result.lineCount == 1 && !result.isLineEllipsized(0) &&
                    result.multiParagraph.maxIntrinsicWidth <= result.size.width,
            )
        }

        // Then each triage control announces one name and has a target of at least 48 x 48 dp.
        val minimumTargetPx = viewport.widthPx / width.value * 48f
        listOf("Skip, not interested", "Save for later").forEach { name ->
            val control = composeTestRule.onNodeWithContentDescription(name)
            val semantics = control.fetchSemanticsNode().config
            assertEquals(listOf(name), semantics[SemanticsProperties.ContentDescription])
            assertTrue("Expected no merged Text for $name", !semantics.contains(SemanticsProperties.Text))
            val bounds = fullBounds(control)
            assertTrue("Expected $name at least 48 dp wide: $bounds", bounds.width >= minimumTargetPx)
            assertTrue("Expected $name at least 48 dp high: $bounds", bounds.height >= minimumTargetPx)
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
