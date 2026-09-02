package io.irodriguez.intentionalreading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.ui.AggregateUiState
import io.irodriguez.intentionalreading.ui.Destination
import io.irodriguez.intentionalreading.ui.NavigationCounts
import io.irodriguez.intentionalreading.ui.components.BottomNavigationBar
import io.irodriguez.intentionalreading.ui.components.UndoToast
import io.irodriguez.intentionalreading.ui.screens.history.HistoryGroupUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryPeriod
import io.irodriguez.intentionalreading.ui.screens.history.HistoryRowUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryScreen
import io.irodriguez.intentionalreading.ui.screens.history.HistoryUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterRowUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterScreen
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterUiState
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ReadingListLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun a360DpReadLaterLastRowActionsClearTheShowingUndoToast() {
        setReadingListContent(Destination.READ_LATER)

        assertLastActionClearsToast(
            lastItemIndex = READ_LATER_LAST_ITEM_INDEX,
            actionDescription = "Remove for $LAST_ARTICLE_TITLE",
        )
    }

    @Test
    fun a360DpHistoryLastRowActionsClearTheShowingUndoToast() {
        setReadingListContent(Destination.HISTORY)

        assertLastActionClearsToast(
            lastItemIndex = HISTORY_LAST_ITEM_INDEX,
            actionDescription = "Mark unread for $LAST_ARTICLE_TITLE",
        )
    }

    @Test
    fun a360DpHistoryEmptyStateKeepsTheExistingCopyAndDiscoverRoute() {
        var widthPx = 0f
        var discoverClicks = 0
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(TEST_SIZE),
            ) {
                widthPx = with(LocalDensity.current) { TEST_SIZE.width.toPx() }
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    HistoryScreen(
                        state = HistoryUiState(groups = emptyList(), aggregate = EMPTY_AGGREGATE),
                        onReadLater = {},
                        onDiscover = { discoverClicks += 1 },
                        onReopen = {},
                        onMarkUnread = {},
                        modifier = Modifier.testTag(EMPTY_SCREEN_TAG),
                    )
                }
            }
        }

        assertEquals(
            widthPx,
            composeTestRule.onNodeWithTag(EMPTY_SCREEN_TAG).fetchSemanticsNode().boundsInRoot.width,
            PIXEL_TOLERANCE,
        )
        composeTestRule.onNodeWithText("No reading history yet").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Articles appear here after you explicitly mark them read. There is no target to keep up with.",
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Go to Discover").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, discoverClicks)
    }

    private fun setReadingListContent(destination: Destination) {
        var widthPx = 0f
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(TEST_SIZE),
            ) {
                widthPx = with(LocalDensity.current) { TEST_SIZE.width.toPx() }
                IntentionalReadingTheme(appearance = Appearance.LIGHT) {
                    ReadingListTestShell(destination)
                }
            }
        }

        assertEquals(
            widthPx,
            composeTestRule.onNodeWithTag(ROOT_TAG).fetchSemanticsNode().boundsInRoot.width,
            PIXEL_TOLERANCE,
        )
    }

    private fun assertLastActionClearsToast(lastItemIndex: Int, actionDescription: String) {
        composeTestRule.onNodeWithTag(LIST_TAG).performScrollToIndex(lastItemIndex)
        composeTestRule.waitForIdle()
        val action = composeTestRule.onNodeWithContentDescription(actionDescription)

        val actionBounds = action.fetchSemanticsNode().boundsInRoot
        val toastBounds = composeTestRule.onNodeWithTag(TOAST_TAG).fetchSemanticsNode().boundsInRoot
        assertFalse(
            "Last-row action $actionBounds overlapped the showing Undo toast $toastBounds",
            actionBounds.overlaps(toastBounds),
        )
        assertTrue(
            "Last-row action ended at ${actionBounds.bottom}px; Undo toast began at ${toastBounds.top}px",
            actionBounds.bottom <= toastBounds.top,
        )
    }

    @Composable
    private fun ReadingListTestShell(destination: Destination) {
        val spacing = LocalIntentionalReadingSpacing.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ROOT_TAG),
        ) {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        destination = destination,
                        counts = NavigationCounts(readLater = ARTICLE_COUNT, history = ARTICLE_COUNT),
                        onDestinationSelected = {},
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when (destination) {
                        Destination.READ_LATER -> ReadLaterScreen(
                            state = readLaterState(),
                            onDiscover = {},
                            onReadArticle = {},
                            onMarkRead = {},
                            onRemove = {},
                            modifier = Modifier.testTag(LIST_TAG),
                        )

                        Destination.HISTORY -> HistoryScreen(
                            state = historyState(),
                            onReadLater = {},
                            onDiscover = {},
                            onReopen = {},
                            onMarkUnread = {},
                            modifier = Modifier.testTag(LIST_TAG),
                        )

                        Destination.DISCOVER -> error("Reading-list tests do not compose Discover")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        horizontal = spacing.gutter,
                        vertical = spacing.sectionGap * 3,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.baseUnit * 2),
            ) {
                UndoToast(
                    message = "Saved to Read Later",
                    onUndo = {},
                    modifier = Modifier.testTag(TOAST_TAG),
                )
            }
        }
    }

    private fun readLaterState(): ReadLaterUiState = ReadLaterUiState(
        rows = articles().map { article ->
            ReadLaterRowUiState(
                article = article,
                savedAt = TEST_INSTANT,
                savedAge = "today",
            )
        },
        aggregate = POPULATED_AGGREGATE,
    )

    private fun historyState(): HistoryUiState = HistoryUiState(
        groups = listOf(
            HistoryGroupUiState(
                period = HistoryPeriod.TODAY,
                rows = articles().map { article ->
                    HistoryRowUiState(
                        article = article,
                        readAt = TEST_INSTANT,
                        readAge = "today",
                        readDateTime = "Today",
                    )
                },
            ),
        ),
        aggregate = POPULATED_AGGREGATE,
    )

    private fun articles(): List<Article> = (1..ARTICLE_COUNT).map { index ->
        Article(
            id = "article-$index",
            title = if (index == ARTICLE_COUNT) LAST_ARTICLE_TITLE else "Reading list article $index",
            url = "https://example.com/article-$index",
            source = ArticleSource(id = "source", name = "Example Source"),
            category = Category.IAM,
            publishedAt = TEST_INSTANT,
            author = null,
            excerpt = "",
            readingTimeMinutes = 8,
            tags = listOf(ArticleTag(id = "oauth", label = "OAuth")),
            contentType = ArticleContentType(
                id = ContentTypeId.STANDARDS_UPDATE,
                label = "Standards Update",
            ),
            score = ArticleScore(
                base = 0,
                sourceQuality = 0,
                contentType = 0,
                freshness = 0,
                topicSignal = 0,
                metadata = 0,
            ),
        )
    }

    private companion object {
        val TEST_SIZE = DpSize(width = 360.dp, height = 800.dp)
        val TEST_INSTANT: Instant = Instant.parse("2026-09-01T12:00:00Z")
        val EMPTY_AGGREGATE = AggregateUiState(
            count = 0,
            knownReadingTimeMinutes = 0,
            unknownReadingTimeCount = 0,
            firstTagId = null,
            firstTagLabel = null,
        )
        val POPULATED_AGGREGATE = AggregateUiState(
            count = ARTICLE_COUNT,
            knownReadingTimeMinutes = ARTICLE_COUNT * 8,
            unknownReadingTimeCount = 0,
            firstTagId = "oauth",
            firstTagLabel = "OAuth",
        )
        const val ARTICLE_COUNT = 8
        const val LAST_ARTICLE_TITLE = "Final reading list article"
        const val ROOT_TAG = "reading-list-root"
        const val LIST_TAG = "reading-list"
        const val TOAST_TAG = "showing-undo-toast"
        const val EMPTY_SCREEN_TAG = "history-empty-screen"
        const val PIXEL_TOLERANCE = 0.5f
        const val READ_LATER_LAST_ITEM_INDEX = ARTICLE_COUNT + 1
        const val HISTORY_LAST_ITEM_INDEX = ARTICLE_COUNT + 2
    }
}
