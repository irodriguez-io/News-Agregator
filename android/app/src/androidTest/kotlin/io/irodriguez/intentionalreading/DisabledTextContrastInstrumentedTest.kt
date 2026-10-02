package io.irodriguez.intentionalreading

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTokens
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DisabledTextContrastInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun refreshingIsLegible_givenLightCard_whenRefreshRuns_thenContrastMeetsFourPointFive() {
        assertLegible(Appearance.LIGHT, errorPanel = false)
    }

    @Test
    fun refreshingIsLegible_givenDarkCard_whenRefreshRuns_thenContrastMeetsFourPointFive() {
        assertLegible(Appearance.DARK, errorPanel = false)
    }

    @Test
    fun tryAgainIsLegible_givenLightError_whenRetryRuns_thenContrastMeetsFourPointFive() {
        assertLegible(Appearance.LIGHT, errorPanel = true)
    }

    @Test
    fun tryAgainIsLegible_givenDarkError_whenRetryRuns_thenContrastMeetsFourPointFive() {
        assertLegible(Appearance.DARK, errorPanel = true)
    }

    @Test
    fun disabledStillLooksDisabled_givenLightCard_whenRefreshBecomesAvailable_thenEnabledControlIsUnchanged() {
        assertDisabledAndEnabled(Appearance.LIGHT, errorPanel = false)
    }

    @Test
    fun disabledStillLooksDisabled_givenDarkCard_whenRefreshBecomesAvailable_thenEnabledControlIsUnchanged() {
        assertDisabledAndEnabled(Appearance.DARK, errorPanel = false)
    }

    @Test
    fun disabledStillLooksDisabled_givenLightError_whenRetryBecomesAvailable_thenEnabledControlIsUnchanged() {
        assertDisabledAndEnabled(Appearance.LIGHT, errorPanel = true)
    }

    @Test
    fun disabledStillLooksDisabled_givenDarkError_whenRetryBecomesAvailable_thenEnabledControlIsUnchanged() {
        assertDisabledAndEnabled(Appearance.DARK, errorPanel = true)
    }

    private fun assertLegible(appearance: Appearance, errorPanel: Boolean) {
        // Given Discover has a card or an error, with its refresh or retry in progress.
        val host = setDiscover(appearance, errorPanel)
        val label = if (errorPanel) "Try again" else "Refreshing…"
        // When the busy control is drawn, read the resolved colour from its unmerged Text.
        composeTestRule.onNodeWithText(label).performScrollTo().assertIsNotEnabled()
        val color = drawnTextColor(label)
        val background = if (errorPanel) host.tokens.container else host.tokens.bg
        // Alpha is part of the drawn colour: composite it onto the actual background first.
        val ratio = contrastRatio(color.compositeOver(background), background)
        val measurement = "$appearance $label contrast=$ratio:1 (required >= 4.5:1)"
        println(measurement)
        // Then the label remains readable in this scheme.
        assertTrue(measurement, ratio >= 4.5)
    }

    private fun assertDisabledAndEnabled(appearance: Appearance, errorPanel: Boolean) {
        // Given the same card or error state, initially busy.
        val host = setDiscover(appearance, errorPanel)
        val disabledLabel = if (errorPanel) "Try again" else "Refreshing…"
        val disabledButton = composeTestRule.onNodeWithText(disabledLabel)
            .performScrollTo().assertIsNotEnabled()
        val disabledColor = drawnTextColor(disabledLabel)
        // When the reader taps the disabled control, Then onRetry is not invoked.
        disabledButton.performTouchInput { click() }
        composeTestRule.runOnIdle { assertEquals(0, host.retryCalls) }

        // When the same state's affordance becomes AVAILABLE.
        composeTestRule.runOnIdle { host.affordance.value = DiscoverRefreshAffordance.AVAILABLE }
        val enabledLabel = if (errorPanel) "Try again" else "Refresh"
        val enabledButton = composeTestRule.onNodeWithText(enabledLabel)
            .performScrollTo().assertIsEnabled()
        val enabledColor = drawnTextColor(enabledLabel)
        // Then disabled text differs, and the enabled colour and callback remain unchanged.
        assertNotEquals("$appearance $disabledLabel must look disabled", enabledColor, disabledColor)
        assertEquals("$appearance $enabledLabel keeps tokens.fg", host.tokens.fg, enabledColor)
        enabledButton.performTouchInput { click() }
        composeTestRule.runOnIdle { assertEquals(1, host.retryCalls) }
    }

    private fun drawnTextColor(label: String): Color {
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResults ->
                assertTrue("$label must expose its text layout", getResults(results))
            }
        assertEquals("One resolved Text layout for $label", 1, results.size)
        return results.single().layoutInput.style.color.also {
            assertTrue("$label must expose a specified drawn colour", it.isSpecified)
        }
    }

    private class DiscoverHost {
        val affordance = mutableStateOf(DiscoverRefreshAffordance.IN_PROGRESS)
        lateinit var tokens: IntentionalReadingTokens
        var retryCalls = 0
    }

    private fun setDiscover(appearance: Appearance, errorPanel: Boolean): DiscoverHost {
        val host = DiscoverHost()
        composeTestRule.setContent {
            IntentionalReadingTheme(appearance = appearance, reducedMotion = { true }) {
                host.tokens = LocalIntentionalReadingTokens.current
                Surface(modifier = Modifier.fillMaxSize(), color = host.tokens.bg) {
                    val state = if (errorPanel) {
                        DiscoverUiState.Error(
                            title = "Content could not be loaded",
                            copy = "Try again when you have a connection.",
                            actionLabel = "Try again",
                            contentFreshness = null,
                            failedRefreshDisclosure = null,
                            refreshAffordance = host.affordance.value,
                        )
                    } else {
                        DiscoverUiState.Card(
                            article = article(),
                            publicationAge = "4d",
                            availableCount = 3,
                            remainingCount = 2,
                            isOpened = false,
                            contentFreshness = "Updated just now",
                            failedRefreshDisclosure = null,
                            refreshAffordance = host.affordance.value,
                        )
                    }
                    DiscoverScreen(
                        state = state,
                        degraded = false,
                        selectedCategory = null,
                        onCategorySelected = {},
                        onRetry = { host.retryCalls++ },
                        onViewReadLater = {},
                        onDismiss = {},
                        onReadArticle = {},
                        onSave = {},
                        onMarkRead = {},
                        onSwipeCommit = { _, _, complete -> complete(true) },
                        reducedMotion = { true },
                    )
                }
            }
        }
        return host
    }

    private fun article() = Article(
        id = "article-1",
        title = "Reading list article 1",
        url = "https://example.com/article-1",
        source = ArticleSource(id = "source", name = "Example Source"),
        category = Category.IAM,
        publishedAt = Instant.parse("2026-09-03T12:00:00Z"),
        author = null,
        excerpt = "An article about identity standards.",
        readingTimeMinutes = 8,
        tags = listOf(ArticleTag(id = "oauth", label = "OAuth")),
        contentType = ArticleContentType(id = ContentTypeId.STANDARDS_UPDATE, label = "Standards Update"),
        score = ArticleScore(base = 0, sourceQuality = 0, contentType = 0, freshness = 0, topicSignal = 0, metadata = 0),
    )

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        val red = linearChannel((argb ushr 16 and 0xFF) / 255.0)
        val green = linearChannel((argb ushr 8 and 0xFF) / 255.0)
        val blue = linearChannel((argb and 0xFF) / 255.0)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun linearChannel(value: Double): Double = if (value <= 0.04045) {
        value / 12.92
    } else {
        ((value + 0.055) / 1.055).pow(2.4)
    }
}
