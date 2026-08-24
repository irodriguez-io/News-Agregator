package io.irodriguez.intentionalreading.ui

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.domain.validation.DatasetErrorCode
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppViewModelTest {
    @Test
    fun `destination switching preserves the fixed declaration order`() {
        assertEquals(
            listOf(Destination.READ_LATER, Destination.DISCOVER, Destination.HISTORY),
            Destination.entries,
        )
        val viewModel = viewModel(DatasetResult.Success(dataset()))

        assertEquals(Destination.DISCOVER, viewModel.destination.value)
        viewModel.selectDestination(Destination.READ_LATER)
        assertEquals(Destination.READ_LATER, viewModel.destination.value)
        viewModel.selectDestination(Destination.HISTORY)
        assertEquals(Destination.HISTORY, viewModel.destination.value)
        viewModel.selectDestination(Destination.DISCOVER)
        assertEquals(Destination.DISCOVER, viewModel.destination.value)
    }

    @Test
    fun `settings entry point toggles open and closed`() {
        val viewModel = viewModel(DatasetResult.Success(dataset()))

        assertFalse(viewModel.settingsOpen.value)
        viewModel.toggleSettings()
        assertTrue(viewModel.settingsOpen.value)
        viewModel.toggleSettings()
        assertFalse(viewModel.settingsOpen.value)
    }

    @Test
    fun `appearance changes between the three frozen values`() {
        assertEquals(listOf(Appearance.LIGHT, Appearance.DARK, Appearance.SYSTEM), Appearance.entries)
        val viewModel = viewModel(DatasetResult.Success(dataset()))

        assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
        viewModel.setAppearance(Appearance.LIGHT)
        assertEquals(Appearance.LIGHT, viewModel.appearance.value)
        viewModel.setAppearance(Appearance.DARK)
        assertEquals(Appearance.DARK, viewModel.appearance.value)
        viewModel.setAppearance(Appearance.SYSTEM)
        assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
    }

    @Test
    fun `load success maps loading to ready content`() = runBlocking {
        val result = CompletableDeferred<DatasetResult>()
        val viewModel = viewModel { result.await() }

        assertIs<DiscoverUiState.Loading>(viewModel.uiState.value.discover)
        result.complete(DatasetResult.Success(dataset()))
        yield()

        assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        Unit
    }

    @Test
    fun `load failure maps to the error phase without claiming an article`() {
        val viewModel = viewModel(
            DatasetResult.Failure(
                code = DatasetErrorCode.MALFORMED_DATASET,
                message = "broken fixture",
            ),
        )

        assertIs<DiscoverUiState.Error>(viewModel.uiState.value.discover)
    }

    @Test
    fun `opened card is held and released when its record leaves opened`() {
        val articles = listOf(article(1), article(2))
        val viewModel = viewModel(DatasetResult.Success(dataset(articles)))

        val opened = assertIs<ArticleTransition.Applied>(
            viewModel.onArticleAction(articles.first(), ArticleAction.OPEN),
        )
        assertEquals(ArticleStatus.OPENED, opened.record.status)
        assertEquals(articles.first().id, viewModel.heldArticleId.value)
        val heldCard = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(articles.first(), heldCard.article)
        assertTrue(heldCard.isOpened)

        val saved = assertIs<ArticleTransition.Applied>(
            viewModel.onArticleAction(articles.first(), ArticleAction.SAVE),
        )
        assertEquals(ArticleStatus.SAVED, saved.record.status)
        assertNull(viewModel.heldArticleId.value)
        assertEquals(articles[1], assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article)
    }

    @Test
    fun `changing category releases an opened held card`() {
        val technology = article(1, Category.TECHNOLOGY)
        val iam = article(2, Category.IAM)
        val viewModel = viewModel(DatasetResult.Success(dataset(listOf(technology, iam))))
        viewModel.selectCategory(Category.IAM)

        viewModel.onArticleAction(iam, ArticleAction.OPEN)
        assertEquals(iam.id, viewModel.heldArticleId.value)
        viewModel.selectCategory(null)

        assertNull(viewModel.heldArticleId.value)
        assertEquals(technology, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article)
    }

    @Test
    fun `every article action is delegated to the state machine`() {
        val articles = listOf(article(1), article(2), article(3))
        val viewModel = viewModel(DatasetResult.Success(dataset(articles)))

        assertStatus(viewModel, articles[0], ArticleAction.OPEN, ArticleStatus.OPENED)
        assertStatus(viewModel, articles[0], ArticleAction.SAVE, ArticleStatus.SAVED)
        assertStatus(viewModel, articles[0], ArticleAction.REMOVE, ArticleStatus.DISMISSED)
        assertStatus(viewModel, articles[1], ArticleAction.DISMISS, ArticleStatus.DISMISSED)
        assertStatus(viewModel, articles[2], ArticleAction.MARK_READ, ArticleStatus.READ)
        assertStatus(viewModel, articles[2], ArticleAction.MARK_UNREAD, ArticleStatus.SAVED)
    }

    @Test
    fun `Read Later row actions update queue and History projections immediately`() {
        val savedArticle = article(1)
        val removedArticle = article(2)
        val viewModel = viewModel(DatasetResult.Success(dataset(listOf(savedArticle, removedArticle))))

        viewModel.onArticleAction(savedArticle, ArticleAction.SAVE)
        viewModel.onArticleAction(removedArticle, ArticleAction.SAVE)
        viewModel.onArticleAction(savedArticle, ArticleAction.OPEN)

        assertEquals(2, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(0, viewModel.uiState.value.navigationCounts.history)
        val unchanged = assertIs<ArticleTransition.Unchanged>(
            viewModel.onArticleAction(savedArticle, ArticleAction.SAVE),
        )
        assertEquals(ArticleStatus.SAVED, unchanged.records.getValue(savedArticle.id).status)

        viewModel.onArticleAction(savedArticle, ArticleAction.MARK_READ)
        viewModel.onArticleAction(removedArticle, ArticleAction.REMOVE)

        assertTrue(viewModel.uiState.value.readLater.rows.isEmpty())
        assertEquals(listOf(savedArticle.id), viewModel.uiState.value.history.groups.single().rows.map { it.article.id })
        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
    }

    @Test
    fun `History row actions reopen in place then mark unread back to Read Later`() {
        val readArticle = article(1)
        val viewModel = viewModel(DatasetResult.Success(dataset(listOf(readArticle))))

        viewModel.onArticleAction(readArticle, ArticleAction.MARK_READ)
        val reopened = assertIs<ArticleTransition.Applied>(
            viewModel.onArticleAction(readArticle, ArticleAction.OPEN),
        )

        assertEquals(ArticleStatus.READ, reopened.record.status)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
        assertEquals(readArticle.id, viewModel.uiState.value.history.groups.single().rows.single().article.id)

        val unread = assertIs<ArticleTransition.Applied>(
            viewModel.onArticleAction(readArticle, ArticleAction.MARK_UNREAD),
        )

        assertEquals(ArticleStatus.SAVED, unread.record.status)
        assertEquals(readArticle.id, viewModel.uiState.value.readLater.rows.single().article.id)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(0, viewModel.uiState.value.navigationCounts.history)
    }

    private fun assertStatus(
        viewModel: AppViewModel,
        article: Article,
        action: ArticleAction,
        expected: ArticleStatus,
    ) {
        val transition = assertIs<ArticleTransition.Applied>(viewModel.onArticleAction(article, action))
        assertEquals(expected, transition.record.status)
    }

    private fun viewModel(result: DatasetResult): AppViewModel = viewModel { result }

    private fun viewModel(loadDataset: suspend () -> DatasetResult): AppViewModel = AppViewModel(
        loadDataset = loadDataset,
        nowProvider = { now },
        zoneProvider = { zone },
        localeProvider = { Locale.US },
        loadDispatcher = Dispatchers.Unconfined,
    )

    private fun dataset(articles: List<Article> = listOf(article(1))): ArticleDataset = ArticleDataset(
        schemaVersion = 1,
        generatedAt = "2026-08-22T12:00:00Z",
        pipeline = PipelineMetadata(
            enabledSourceCount = 1,
            successfulSourceCount = 1,
            failedSourceCount = 0,
            articleCount = articles.size,
        ),
        articles = articles,
    )

    private fun article(index: Int, category: Category = Category.IAM): Article = Article(
        id = index.toString(16).padStart(20, '0'),
        title = "Article $index",
        url = "https://example.com/$index",
        source = ArticleSource("source_$index", "Source $index"),
        category = category,
        publishedAt = now.minusSeconds(index * 3_600L),
        author = null,
        excerpt = "Excerpt $index",
        readingTimeMinutes = 7,
        tags = listOf(ArticleTag("oauth", "OAuth")),
        contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
        score = ArticleScore(91, 50, 20, 15, 5, 1),
    )

    private companion object {
        val now: Instant = Instant.parse("2026-08-22T12:00:00Z")
        val zone: ZoneId = ZoneId.of("America/Managua")
    }
}
