package io.irodriguez.intentionalreading

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.ui.components.CategoryChipRow
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryChipRowLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun a360DpRowCarries48DpTargetsWithoutGrowingPastThatTarget() {
        var targetPx = 0f
        var visibleHeightPx = 0f
        var insetTapPx = 0f
        var widthPx = 0f
        var selectionCount = 0
        composeTestRule.setContent {
            targetPx = with(LocalDensity.current) { 48.dp.toPx() }
            visibleHeightPx = with(LocalDensity.current) { 40.dp.toPx() }
            insetTapPx = with(LocalDensity.current) { 1.dp.toPx() }
            widthPx = with(LocalDensity.current) { 360.dp.toPx() }
            IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                CategoryChipRow(
                    selectedCategory = null,
                    onCategorySelected = { selectionCount += 1 },
                    modifier = Modifier
                        .testTag(ROW_TAG)
                        .width(360.dp),
                )
            }
        }

        val rowNode = composeTestRule.onNodeWithTag(ROW_TAG).fetchSemanticsNode()
        val rowBounds = rowNode.boundsInRoot
        val firstChipBounds = composeTestRule.onNodeWithText("All").fetchSemanticsNode().boundsInRoot
        val horizontalScrollRange = rowNode.config[SemanticsProperties.HorizontalScrollAxisRange]

        assertEquals(widthPx, rowBounds.width, 0.5f)
        assertEquals(targetPx, rowBounds.height, 0.5f)
        assertTrue("Expected the 360 dp row to remain horizontally scrollable", horizontalScrollRange.maxValue() > 0f)
        assertEquals(visibleHeightPx, firstChipBounds.height, 0.5f)
        assertTrue("Expected the first chip to meet the horizontal target floor", firstChipBounds.width >= targetPx)

        composeTestRule.onNodeWithTag(ROW_TAG).performTouchInput {
            down(
                Offset(
                    x = firstChipBounds.center.x - rowBounds.left,
                    y = insetTapPx,
                ),
            )
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals("The expanded vertical target should accept a tap above the 40 dp pill", 1, selectionCount)
    }

    private companion object {
        const val ROW_TAG = "category-chip-row-360"
    }
}
