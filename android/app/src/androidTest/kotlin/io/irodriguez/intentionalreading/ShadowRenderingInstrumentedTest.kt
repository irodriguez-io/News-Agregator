package io.irodriguez.intentionalreading

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
import io.irodriguez.intentionalreading.ui.components.ArticleCard
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverLayoutTags
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.settings.SettingsSheet
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTokens
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShadowRenderingInstrumentedTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var tokens: IntentionalReadingTokens
    private var density = 1f

    @Test
    fun cardShadowIsVisibleAndNavy_GivenLightDiscover_WhenCaptured_ThenPeakIsEightToTwelvePercentTertiary() {
        val peak = captureCardShadow(Appearance.LIGHT)
        val percentages = channels(tokens.bg).zip(channels(peak)).zip(channels(tokens.tertiary)) {
            (background, pixel), tertiary -> (background - pixel) / (background - tertiary)
        }
        val measurement = "light card bg=${hex(tokens.bg)} peak=${hex(peak)} " +
            "tertiary=${hex(tokens.tertiary)} RGB percentages=${percentages.map { it * 100f }}"
        println(measurement)
        assertTrue(measurement, percentages.all { it in 0.08f..0.12f })
        assertTrue("Navy must darken red more than blue: $measurement",
            tokens.bg.red - peak.red > tokens.bg.blue - peak.blue)
    }

    @Test
    fun cardShadowIsVisibleAndNavy_GivenDarkDiscover_WhenCaptured_ThenNoChannelIsLiftedTowardTertiary() {
        val peak = captureCardShadow(Appearance.DARK)
        val measurement = "dark card bg=${hex(tokens.bg)} peak=${hex(peak)}"
        println(measurement)
        assertTrue(measurement, channels(peak).zip(channels(tokens.bg)).all { (pixel, bg) -> pixel <= bg })
    }

    @Test
    @Ignore("Owner's pre-approved drop: the sheet shadow does not render — 030 evidence")
    fun settingsSheetCastsAShadow_GivenOpenSheet_WhenWindowCaptured_ThenNearScrimIsDarkerThanFarScrim() {
        // Given an open sheet over a uniform light background, with motion settled.
        rule.setContent {
            IntentionalReadingTheme(appearance = Appearance.LIGHT, reducedMotion = { true }) {
                tokens = LocalIntentionalReadingTokens.current
                density = LocalDensity.current.density
                Surface(modifier = Modifier.fillMaxSize(), color = tokens.bg) {
                    SettingsSheet(
                        appearance = Appearance.LIGHT,
                        resetInProgress = false,
                        statusMessage = null,
                        generatedAtLabel = "Updated just now",
                        lastRefreshOutcome = "Refresh succeeded",
                        importFileName = null,
                        importInProgress = false,
                        importTooLarge = false,
                        importUnreadable = false,
                        reducedMotion = { true },
                        onAppearanceSelected = {},
                        onExport = {},
                        onSelectImport = {},
                        onCancelImport = {},
                        onConfirmImport = {},
                        onReset = { it(true) },
                        onDismiss = {},
                    )
                }
            }
        }
        rule.waitForIdle()
        val sheet = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle))
            .fetchSemanticsNode().boundsInRoot
        // When capturing the dialog's root, include the scrim outside the sheet's bounds.
        // Select the full-window dialog node: a semantics root above IsDialog falls back
        // to the activity window in captureToImage(), silently omitting the sheet and scrim.
        val dialog = rule.onNode(isDialog())
        val dialogBounds = dialog.fetchSemanticsNode().boundsInRoot
        val pixels = dialog.captureToImage().toPixelMap()
        val x = (sheet.center.x - dialogBounds.left).roundToInt()
        val nearY = (sheet.top - dialogBounds.top - 4.dp.value * density).roundToInt()
        val farY = (sheet.top - dialogBounds.top - 48.dp.value * density).roundToInt()
        assertTrue("Both scrim samples must lie above the laid-out sheet: $sheet", farY >= 0 && nearY < pixels.height)
        val rawNear = pixels[x, nearY]
        val rawFar = pixels[x, farY]
        assertTrue("Capture must include the translucent scrim", rawFar.alpha > 0f && rawFar.alpha < 1f)
        val insideY = (sheet.top - dialogBounds.top + 4.dp.value * density).roundToInt()
        assertTrue("Capture must include the sheet surface", channels(pixels[x, insideY])
            .zip(channels(tokens.surface)).all { (pixel, surface) -> abs(pixel - surface) <= 1f / 255f })
        // PixelCopy preserves the dialog's alpha; composite onto the actual host background.
        val near = rawNear.compositeOver(tokens.bg)
        val far = rawFar.compositeOver(tokens.bg)
        val measurement = "sheet top=${sheet.top} x=$x near4dp=${hex(near)} far48dp=${hex(far)} " +
            "rawNear=${hex(rawNear)} rawFar=${hex(rawFar)}"
        println(measurement)
        // Then the shadow darkens each RGB channel immediately above the sheet.
        assertTrue(measurement, channels(near).zip(channels(far)).all { (close, distant) -> close < distant })
    }

    @Test
    fun shadowKeepsRoundedCornersWhileCardFades_GivenRestingCard_WhenHalfwayThroughEntrance_ThenShadowRemainsPresentAndRounded() {
        assertFadingShadow(entering = true)
    }

    private fun assertFadingShadow(entering: Boolean) {
        val currentArticle = mutableStateOf(article())
        // Given enough horizontal room to keep the exiting card's trailing corner and
        // bottom centre visible at 150 ms, without changing any production motion values.
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1200.dp, 1000.dp))) {
                val configuration = Configuration(LocalConfiguration.current).apply { screenWidthDp = 1200 }
                CompositionLocalProvider(LocalConfiguration provides configuration) {
                    IntentionalReadingTheme(appearance = Appearance.LIGHT, reducedMotion = { false }) {
                        tokens = LocalIntentionalReadingTokens.current
                        density = LocalDensity.current.density
                        Surface(Modifier.fillMaxSize(), color = tokens.bg) {
                            Box(Modifier.padding(start = 24.dp, top = 80.dp)) {
                                ArticleCard(
                                    state = DiscoverUiState.Card(
                                        article = currentArticle.value, publicationAge = "4d", availableCount = 1,
                                        remainingCount = 0, isOpened = false, contentFreshness = null,
                                        failedRefreshDisclosure = null,
                                        refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
                                    ),
                                    onDismiss = {}, onReadArticle = {}, onSave = {}, onMarkRead = {},
                                    onSwipeCommit = { _, _, _ -> }, reducedMotion = { false },
                                    modifier = Modifier.width(240.dp).testTag("fading-shadow-card"),
                                )
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        val card = rule.onNodeWithTag("fading-shadow-card").fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText(article().title, useUnmergedTree = true)
        val restingAnchor = title.fetchSemanticsNode().layoutInfo.coordinates.localToRoot(Offset.Zero)
        // Coordinates are relative to a child inside the card's transformed layer. This
        // follows both the entrance translation and the exit translation/rotation, so
        // each comparison addresses the same point of the card rather than empty space.
        val cornerOffsets = buildList {
            for (x in listOf(1f, 3f, 5f)) {
                for (y in listOf(1f, 3f, 5f)) {
                    add(Offset(card.left + x * density, card.bottom - y * density) - restingAnchor)
                }
            }
            for (y in listOf(1f, 3f, 5f)) {
                add(Offset(card.left - density, card.bottom - y * density) - restingAnchor)
            }
        }
        val peakOffsets = (1..24).map { below ->
            Offset(card.center.x, card.bottom + below * density) - restingAnchor
        }
        fun capture(): List<Color> {
            rule.waitForIdle()
            val coordinates = title.fetchSemanticsNode().layoutInfo.coordinates
            val points = (cornerOffsets + peakOffsets).map { coordinates.localToRoot(it) }
            val pixels = rule.onRoot().captureToImage().toPixelMap()
            return points.map { point ->
                val x = point.x.roundToInt()
                val y = point.y.roundToInt()
                assertTrue("Shadow sample must stay on screen: $point in ${pixels.width}x${pixels.height}",
                    x in 0 until pixels.width && y in 0 until pixels.height)
                pixels[x, y]
            }
        }
        val rest = capture()
        val restPeak = rest.drop(cornerOffsets.size).minBy { channels(it).sum() }
        assertTrue("Resting reference must contain a visible shadow: ${hex(restPeak)}", restPeak.red < tokens.bg.red)

        // When paused at roughly half of the unchanged 300 ms entrance or exit.
        if (entering) {
            rule.runOnUiThread { currentArticle.value = article().copy(id = "arriving-shadow-article") }
            rule.mainClock.advanceTimeByFrame()
        } else {
            rule.onNodeWithTag("fading-shadow-card").performTouchInput {
                down(center)
                moveBy(Offset(100f * density, 0f))
                up()
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(150, ignoreFrameDuration = true)
        val moving = capture()
        val repeated = capture()
        assertTrue("Paused-clock shadow samples must be stable", moving == repeated)
        val movingPeak = moving.drop(cornerOffsets.size).minBy { channels(it).sum() }
        val phase = if (entering) "entrance" else "exit"
        val measurement = "$phase at 150ms: restPeak=${hex(restPeak)} movingPeak=${hex(movingPeak)} " +
            "corners(rest/moving)=" + rest.take(cornerOffsets.size).zip(moving).joinToString { (a, b) ->
                "${hex(a)}/${hex(b)}"
            }
        println(measurement)
        // Then the shadow outside the bounding square retains at least a third of
        // its resting darkening on every channel, rather than disappearing into bg.
        assertTrue("Shadow below the card must remain present: $measurement",
            channels(tokens.bg).zip(channels(restPeak)).zip(channels(movingPeak)).all { (reference, fading) ->
                val (background, resting) = reference
                background - fading >= (background - resting) / 3f
            })
        // Then neither the pixels outside the rounded outline but inside the square,
        // nor those just outside that square, darken beyond the 2/255 tolerance.
        assertTrue(measurement, rest.take(cornerOffsets.size).zip(moving).all { (a, b) ->
            channels(a).zip(channels(b)).all { (resting, fading) -> resting - fading <= 2f / 255f }
        })
        assertTrue("Shadow peak must fade with the card: $measurement",
            channels(restPeak).zip(channels(movingPeak)).all { (resting, fading) -> fading >= resting })
    }

    private fun captureCardShadow(appearance: Appearance): Color {
        // Given Discover with exactly one card, so the tagged card body has the card's bounds.
        rule.setContent {
            IntentionalReadingTheme(appearance = appearance, reducedMotion = { true }) {
                tokens = LocalIntentionalReadingTokens.current
                density = LocalDensity.current.density
                Surface(modifier = Modifier.fillMaxSize(), color = tokens.bg) {
                    DiscoverScreen(
                        state = DiscoverUiState.Card(
                            article = article(), publicationAge = "4d", availableCount = 1,
                            remainingCount = 0, isOpened = false, contentFreshness = null,
                            failedRefreshDisclosure = null,
                            refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
                        ),
                        degraded = false, selectedCategory = null, onCategorySelected = {},
                        onRetry = {}, onViewReadLater = {}, onDismiss = {}, onReadArticle = {},
                        onSave = {}, onMarkRead = {}, onSwipeCommit = { _, _, complete -> complete(true) },
                        reducedMotion = { true },
                    )
                }
            }
        }
        rule.waitForIdle()
        val card = rule.onNodeWithTag(DiscoverLayoutTags.CARD).fetchSemanticsNode().boundsInRoot
        val operational = rule.onNodeWithTag(DiscoverLayoutTags.OPERATIONAL_BLOCK).fetchSemanticsNode().boundsInRoot
        // When capturing the root, search only the empty gap below the laid-out card, never its content.
        val pixels = rule.onRoot().captureToImage().toPixelMap()
        val x = card.center.x.roundToInt()
        val start = ceil(card.bottom).toInt()
        val end = minOf(ceil(operational.top).toInt(), pixels.height)
        assertTrue("A visible gap must separate card $card from operational block $operational", end > start)
        val y = (start until end).minBy { y -> channels(pixels[x, y]).sum() }
        println("$appearance card bounds=$card shadow sample=($x,$y) gap=[$start,$end)")
        return pixels[x, y]
    }

    private fun channels(color: Color) = listOf(color.red, color.green, color.blue)
    private fun hex(color: Color) = "#%08X".format(color.toArgb())

    private fun article() = Article(
        id = "shadow-article", title = "A quiet reading moment", url = "https://example.com/shadow",
        source = ArticleSource(id = "source", name = "Example Source"), category = Category.IAM,
        publishedAt = Instant.parse("2026-09-03T12:00:00Z"), author = null,
        excerpt = "An article about identity standards.", readingTimeMinutes = 8, tags = emptyList(),
        contentType = ArticleContentType(id = ContentTypeId.STANDARDS_UPDATE, label = "Standards Update"),
        score = ArticleScore(base = 0, sourceQuality = 0, contentType = 0, freshness = 0, topicSignal = 0, metadata = 0),
    )
}
