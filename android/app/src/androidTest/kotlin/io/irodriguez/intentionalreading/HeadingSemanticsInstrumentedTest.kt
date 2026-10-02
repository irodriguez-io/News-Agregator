package io.irodriguez.intentionalreading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryGroupUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryPeriod
import io.irodriguez.intentionalreading.ui.screens.history.HistoryRowUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryScreen
import io.irodriguez.intentionalreading.ui.screens.history.HistoryUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterRowUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterScreen
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterUiState
import io.irodriguez.intentionalreading.ui.screens.settings.SettingsSheet
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeadingSemanticsInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun everyScreensSectionTitlesAreHeadings_givenDiscoverCard_thenOnlyItsTitlesAreHeadings() {
        val article = article(1)
        setScreen {
            DiscoverHost(
                DiscoverUiState.Card(
                    article = article,
                    publicationAge = "4d",
                    availableCount = 3,
                    remainingCount = 2,
                    isOpened = false,
                    contentFreshness = "Updated just now",
                    failedRefreshDisclosure = null,
                    refreshAffordance = DiscoverRefreshAffordance.AVAILABLE,
                ),
            )
        }
        assertOnlyHeadings("Discover", article.title)
    }

    @Test
    fun statePanelsTitleThemselves_givenDiscoverError_thenOnlyItsTitlesAreHeadings() {
        setScreen {
            DiscoverHost(
                DiscoverUiState.Error(
                    title = "Content could not be loaded",
                    copy = "Try again when you have a connection.",
                    actionLabel = "Try again",
                    contentFreshness = null,
                    failedRefreshDisclosure = null,
                    refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
                ),
            )
        }
        assertOnlyHeadings("Discover", "Content could not be loaded")
    }

    @Test
    fun statePanelsTitleThemselves_givenDiscoverEmpty_thenOnlyItsTitlesAreHeadings() {
        setScreen {
            DiscoverHost(
                DiscoverUiState.Empty(
                    title = "No articles in this category",
                    copy = "Choose another category or visit Read Later.",
                    actionLabel = "View Read Later",
                    contentFreshness = null,
                    failedRefreshDisclosure = null,
                    refreshAffordance = DiscoverRefreshAffordance.HIDDEN,
                ),
                selectedCategory = Category.IAM,
            )
        }
        assertOnlyHeadings("Discover", "No articles in this category")
    }

    @Test
    fun everyScreensSectionTitlesAreHeadings_givenSavedArticles_thenOnlyItsTitlesAreHeadings() {
        val articles = listOf(article(1), article(2))
        setScreen(tall = true) {
            ReadLaterHost(articles)
        }
        assertOnlyHeadings("Read Later", *articles.map { it.title }.toTypedArray())
    }

    @Test
    fun statePanelsTitleThemselves_givenReadLaterEmpty_thenOnlyItsTitlesAreHeadings() {
        setScreen { ReadLaterHost(emptyList()) }
        assertOnlyHeadings("Read Later", "Your reading queue is open")
    }

    @Test
    fun everyScreensSectionTitlesAreHeadings_givenAllHistoryPeriods_thenOnlyItsTitlesAreHeadings() {
        val groups = HistoryPeriod.entries.mapIndexed { index, period ->
            HistoryGroupUiState(
                period = period,
                rows = listOf(
                    HistoryRowUiState(
                        article = article(index + 1),
                        readAt = TEST_INSTANT.minusSeconds(index * 86_400L),
                        readAge = "${index}d",
                        readDateTime = "2026-09-0${3 - index} 12:00",
                    ),
                ),
            )
        }
        // Given all three groups, use the layout tests' forced-size host with enough height
        // to compose every lazy item together, so the exact-set check covers the whole list.
        setScreen(tall = true) { HistoryHost(groups) }
        assertOnlyHeadings(
            "History", "Today", "Yesterday", "Earlier",
            *groups.flatMap { it.rows }.map { it.article.title }.toTypedArray(),
        )
    }

    @Test
    fun statePanelsTitleThemselves_givenHistoryEmpty_thenOnlyItsTitlesAreHeadings() {
        setScreen { HistoryHost(emptyList()) }
        assertOnlyHeadings("History", "No reading history yet")
    }

    @Test
    fun everyScreensSectionTitlesAreHeadings_givenSettingsOpen_thenOnlyItsTitlesAreHeadings() {
        // Given the same button-triggered sheet fixture as SettingsSheetInstrumentedTest.
        setScreen {
            val open = remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize()) {
                Button(onClick = { open.value = true }) { Text("Open settings") }
                if (open.value) {
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
                        onAppearanceSelected = {},
                        onExport = {},
                        onSelectImport = {},
                        onCancelImport = {},
                        onConfirmImport = {},
                        onReset = { complete -> complete(true) },
                        onDismiss = { open.value = false },
                    )
                }
            }
        }
        // When the sheet opens.
        composeTestRule.onNodeWithText("Open settings").performClick()
        composeTestRule.waitUntil(timeoutMillis = 30_000L) {
            composeTestRule.onAllNodes(hasText("Settings")).fetchSemanticsNodes().size == 1
        }
        assertOnlyHeadings("Settings", "Content", "Appearance", "Local data")
    }

    private fun assertOnlyHeadings(vararg titles: String) {
        // Given every expected title is rendered as one Text, with no merging ancestor.
        // A heading still reads as itself: its exact text is its accessible name.
        titles.forEach { title ->
            val node = composeTestRule.onNodeWithText(title, useUnmergedTree = true)
                .assertExists().fetchSemanticsNode()
            assertEquals(listOf(title), node.config[SemanticsProperties.Text].map { it.text })
            assertFalse("$title must keep its text as its accessible name", node.config.contains(SemanticsProperties.ContentDescription))
            generateSequence(node.parent) { it.parent }.forEach { parent ->
                assertFalse("$title must not have a merging ancestor", parent.config.isMergingSemanticsOfDescendants)
            }
        }
        // Then every expected title is a heading in the rendered accessibility tree.
        titles.forEach { title ->
            composeTestRule.onNode(hasText(title) and isHeading()).assertExists()
        }
        // And nothing else is a heading, including controls, metadata, tags and body copy.
        val headings = composeTestRule.onAllNodes(isHeading(), useUnmergedTree = true)
            .fetchSemanticsNodes()
        val texts = headings.map { node ->
            node.config[SemanticsProperties.Text].single().text
        }
        assertEquals("Exactly the expected heading texts", titles.toSet(), texts.toSet())
        assertEquals("No duplicate or extra heading nodes", titles.size, headings.size)
    }

    private fun setScreen(tall: Boolean = false, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, if (tall) 2400.dp else 800.dp)),
            ) {
                IntentionalReadingTheme(appearance = Appearance.LIGHT, content = content)
            }
        }
    }

    @Composable
    private fun DiscoverHost(state: DiscoverUiState, selectedCategory: Category? = null) {
        DiscoverScreen(
            state = state,
            degraded = false,
            selectedCategory = selectedCategory,
            onCategorySelected = {},
            onRetry = {},
            onViewReadLater = {},
            onDismiss = {},
            onReadArticle = {},
            onSave = {},
            onMarkRead = {},
            onSwipeCommit = { _, _, complete -> complete(true) },
        )
    }

    @Composable
    private fun ReadLaterHost(articles: List<Article>) {
        ReadLaterScreen(
            state = ReadLaterUiState(
                rows = articles.map { ReadLaterRowUiState(it, TEST_INSTANT, "today") },
                aggregate = aggregate(articles.size),
            ),
            onDiscover = {},
            onReadArticle = {},
            onMarkRead = {},
            onRemove = {},
        )
    }

    @Composable
    private fun HistoryHost(groups: List<HistoryGroupUiState>) {
        HistoryScreen(
            state = HistoryUiState(groups, aggregate(groups.sumOf { it.rows.size })),
            onReadLater = {},
            onDiscover = {},
            onReopen = {},
            onMarkUnread = {},
        )
    }

    // Article and aggregate fixture patterns from ReadingListLayoutTest.
    private fun article(index: Int) = Article(
        id = "article-$index",
        title = "Reading list article $index",
        url = "https://example.com/article-$index",
        source = ArticleSource(id = "source", name = "Example Source"),
        category = Category.IAM,
        publishedAt = TEST_INSTANT,
        author = null,
        excerpt = "An article about identity standards.",
        readingTimeMinutes = 8,
        tags = listOf(ArticleTag(id = "oauth", label = "OAuth")),
        contentType = ArticleContentType(id = ContentTypeId.STANDARDS_UPDATE, label = "Standards Update"),
        score = ArticleScore(base = 0, sourceQuality = 0, contentType = 0, freshness = 0, topicSignal = 0, metadata = 0),
    )

    private fun aggregate(count: Int) = AggregateUiState(
        count = count,
        knownReadingTimeMinutes = count * 8,
        unknownReadingTimeCount = 0,
        firstTagId = "oauth".takeIf { count > 0 },
        firstTagLabel = "OAuth".takeIf { count > 0 },
    )

    private companion object {
        val TEST_INSTANT: Instant = Instant.parse("2026-09-03T12:00:00Z")
    }
}
