package io.irodriguez.intentionalreading

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheMetadata
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.ui.AppViewModel
import io.irodriguez.intentionalreading.ui.Destination
import io.irodriguez.intentionalreading.ui.IntentionalReadingApp
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DestinationTransitionInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reducedMotionComposesDestinationAndBackResultImmediately() = withReducedMotion {
        composeTestRule.mainClock.autoAdvance = false
        val viewModel = testViewModel()
        val stateBeforeNavigation = viewModel.uiState.value
        composeTestRule.setContent {
            IntentionalReadingApp(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText(DISCOVER_EYEBROW).assertIsDisplayed()
        selectDestination("Read Later")
        settleImmediateChange()

        composeTestRule.onNodeWithText(READ_LATER_EYEBROW).assertIsDisplayed()
        composeTestRule.onNodeWithText(DISCOVER_EYEBROW).assertDoesNotExist()
        assertEquals(Destination.READ_LATER, viewModel.destination.value)
        assertEquals(stateBeforeNavigation, viewModel.uiState.value)

        Espresso.pressBack()
        settleImmediateChange()

        composeTestRule.onNodeWithText(DISCOVER_EYEBROW).assertIsDisplayed()
        composeTestRule.onNodeWithText(READ_LATER_EYEBROW).assertDoesNotExist()
        assertEquals(Destination.DISCOVER, viewModel.destination.value)
        assertEquals(stateBeforeNavigation, viewModel.uiState.value)
    }

    @Test
    fun liveUndoOfferSurvivesDestinationChangeAndRemainsActionable() = withReducedMotion {
        composeTestRule.mainClock.autoAdvance = false
        val viewModel = testViewModel()
        composeTestRule.setContent {
            IntentionalReadingApp(viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("Save for later").performClick()
        settleImmediateChange()
        val offerBeforeNavigation = requireNotNull(viewModel.uiState.value.pendingUndoOffer)
        composeTestRule.onNodeWithText("Saved to Read Later").assertIsDisplayed()

        selectDestination("Read Later")
        settleImmediateChange()

        assertEquals(offerBeforeNavigation, viewModel.uiState.value.pendingUndoOffer)
        assertTrue(viewModel.uiState.value.undoAvailable)
        assertEquals(
            TEST_ARTICLE_ID,
            viewModel.uiState.value.readLater.rows.single().article.id,
        )
        composeTestRule.onNodeWithText("Saved to Read Later").assertIsDisplayed()
        composeTestRule.onNodeWithText("Undo").performClick()
        settleImmediateChange()

        assertTrue(viewModel.uiState.value.readLater.rows.isEmpty())
        assertEquals(
            TEST_ARTICLE_ID,
            (viewModel.uiState.value.discover as DiscoverUiState.Card).article.id,
        )
        composeTestRule.onNodeWithText("Undo completed.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your reading queue is open").assertIsDisplayed()
    }

    private fun settleImmediateChange() {
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
    }

    private fun selectDestination(label: String) {
        composeTestRule.onNode(
            hasClickAction() and hasAnyDescendant(hasText(label)),
            useUnmergedTree = true,
        ).performClick()
    }

    private fun testViewModel(): AppViewModel {
        val article = Article(
            id = TEST_ARTICLE_ID,
            title = "Motion test article",
            url = "https://example.com/motion-test",
            source = ArticleSource("motion-source", "Motion Source"),
            category = Category.IAM,
            publishedAt = NOW,
            author = null,
            excerpt = "A deterministic destination-transition fixture.",
            readingTimeMinutes = 4,
            tags = listOf(ArticleTag("motion-topic", "Motion topic")),
            contentType = ArticleContentType(
                ContentTypeId.STANDARDS_UPDATE,
                "Standards Update",
            ),
            score = ArticleScore(90, 50, 20, 15, 5, 0),
        )
        val dataset = ArticleDataset(
            schemaVersion = 1,
            generatedAt = NOW.toString(),
            pipeline = PipelineMetadata(1, 1, 0, 1),
            articles = listOf(article),
        )
        return AppViewModel(
            readCachedDataset = { DatasetCacheRead.Absent },
            refreshDataset = {
                DatasetRefreshResult.Updated(
                    dataset = dataset,
                    metadata = DatasetCacheMetadata("\"motion-test\"", NOW),
                )
            },
            loadLocalState = {
                LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT)
            },
            saveLocalState = { state ->
                LocalStateResult.Success(state, LocalStateSource.STORAGE)
            },
            resetLocalState = {
                LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT)
            },
            nowProvider = { NOW },
            zoneProvider = { ZoneId.of("America/Managua") },
            localeProvider = { Locale.US },
            loadDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun withReducedMotion(block: () -> Unit) {
        val originalScale = runShellCommand("settings get global animator_duration_scale").trim()
        try {
            runShellCommand("settings put global animator_duration_scale 0")
            block()
        } finally {
            if (originalScale.toFloatOrNull() == null) {
                runShellCommand("settings delete global animator_duration_scale")
            } else {
                runShellCommand("settings put global animator_duration_scale $originalScale")
            }
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    private fun runShellCommand(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-02T12:00:00Z")
        const val TEST_ARTICLE_ID = "00000000000000000021"
        const val DISCOVER_EYEBROW = "A FINITE READING QUEUE"
        const val READ_LATER_EYEBROW = "YOUR DELIBERATE QUEUE"
    }
}
