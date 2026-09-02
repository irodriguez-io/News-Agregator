package io.irodriguez.intentionalreading

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.ui.components.ArticleKickerPart
import io.irodriguez.intentionalreading.ui.components.ArticleRow
import io.irodriguez.intentionalreading.ui.components.ArticleRowAction
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleRowLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun a360DpQueueRowClampsItsTitleAndKeepsEveryActionReachable() {
        var minimumTargetPx = 0f
        var maximumTitleHeightPx = 0f
        var absentMediaSlotPx = 0f
        var leadingClicks = 0
        var outlinedClicks = 0

        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width = 360.dp, height = 800.dp)),
            ) {
                minimumTargetPx = with(LocalDensity.current) { 48.dp.toPx() }
                absentMediaSlotPx = with(LocalDensity.current) { 80.dp.toPx() }
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    maximumTitleHeightPx = with(LocalDensity.current) {
                        MaterialTheme.typography.headlineMedium.lineHeight.toPx() * 2
                    }
                    ArticleRow(
                        articleTitle = LONG_TITLE,
                        position = "Queue 01",
                        positionDetail = "Saved today",
                        kicker = listOf(ArticleKickerPart("Source", emphasized = true)),
                        tags = listOf("OAuth"),
                        actions = listOf(
                            ArticleRowAction("Read") { leadingClicks += 1 },
                            ArticleRowAction("Remove") { outlinedClicks += 1 },
                        ),
                        modifier = Modifier
                            .testTag(ROW_TAG)
                            .width(360.dp),
                    )
                }
            }
        }

        val rowBounds = composeTestRule.onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithText(LONG_TITLE).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Title height was ${titleBounds.height}px; two authored lines allow $maximumTitleHeightPx px",
            titleBounds.height <= maximumTitleHeightPx,
        )
        assertTrue(
            "Title began ${titleBounds.left - rowBounds.left}px from the row edge; no 80 dp media slot may be reserved",
            titleBounds.left - rowBounds.left < absentMediaSlotPx,
        )
        assertTrue(
            "Title width was ${titleBounds.width}px inside a ${rowBounds.width}px row; no 80 dp media slot may be reserved",
            titleBounds.width > rowBounds.width - absentMediaSlotPx,
        )

        listOf("Read" to 1, "Remove" to 1).forEach { (label, expectedClicks) ->
            val action = composeTestRule.onNodeWithContentDescription("$label for $LONG_TITLE")
            val bounds = action.fetchSemanticsNode().boundsInRoot
            assertTrue("$label width was ${bounds.width}px", bounds.width >= minimumTargetPx)
            assertTrue("$label height was ${bounds.height}px", bounds.height >= minimumTargetPx)
            action.performClick()
            composeTestRule.waitForIdle()
            assertEquals(
                expectedClicks,
                if (label == "Read") leadingClicks else outlinedClicks,
            )
        }
    }

    private companion object {
        const val ROW_TAG = "queue-row-360"
        const val LONG_TITLE =
            "A deliberately long article title that must use the available type width and still clamp after exactly two lines"
    }
}
