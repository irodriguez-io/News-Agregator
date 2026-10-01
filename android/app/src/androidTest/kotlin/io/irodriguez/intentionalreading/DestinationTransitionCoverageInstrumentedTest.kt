package io.irodriguez.intentionalreading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.ui.Destination
import io.irodriguez.intentionalreading.ui.DestinationTransition
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DestinationTransitionCoverageInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun movingTowardReadLater_coveredAreaShowsOnlyIncomingDestination() {
        assertIncomingCoverage(Destination.READ_LATER, coveredFraction = 0.1f, uncoveredFraction = 0.8f)
    }

    @Test
    fun movingTowardHistory_coveredAreaShowsOnlyIncomingDestination() {
        assertIncomingCoverage(Destination.HISTORY, coveredFraction = 0.9f, uncoveredFraction = 0.2f)
    }

    private fun assertIncomingCoverage(
        target: Destination,
        coveredFraction: Float,
        uncoveredFraction: Float,
    ) {
        val destination = mutableStateOf(Destination.DISCOVER)
        var background = Color.Unspecified
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                background = LocalIntentionalReadingTokens.current.bg
                Box(Modifier.fillMaxSize().background(background).testTag("transition-host")) {
                    DestinationTransition(
                        destination = destination.value,
                        reducedMotion = { false },
                        modifier = Modifier.fillMaxSize(),
                    ) { current ->
                        if (current == Destination.DISCOVER) {
                            Box(Modifier.fillMaxSize().background(Color.Magenta))
                        } else {
                            // Real destinations fill the viewport even where they draw no content.
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { destination.value = target }
        // Two setup frames establish the animation, followed by 48 ms of motion (80 ms total).
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(48)
        composeTestRule.waitForIdle()

        val pixels = composeTestRule.onNodeWithTag("transition-host").captureToImage().toPixelMap()
        val covered = pixels[(pixels.width * coveredFraction).toInt(), pixels.height / 2]
        val uncovered = pixels[(pixels.width * uncoveredFraction).toInt(), pixels.height / 2]
        assertTrue(
            "Covered pixel must equal theme bg: target=$target, expected=$background, actual=$covered",
            colorDistance(background, covered) < 0.01f,
        )
        assertTrue(
            "Uncovered pixel must still show the outgoing destination: target=$target, bg=$background, actual=$uncovered",
            colorDistance(background, uncovered) > 0.01f,
        )
    }

    private fun colorDistance(first: Color, second: Color): Float =
        kotlin.math.abs(first.red - second.red) + kotlin.math.abs(first.green - second.green) +
            kotlin.math.abs(first.blue - second.blue)
}
