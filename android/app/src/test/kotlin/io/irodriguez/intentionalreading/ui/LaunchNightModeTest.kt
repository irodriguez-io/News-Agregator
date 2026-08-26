package io.irodriguez.intentionalreading.ui

import android.app.UiModeManager
import io.irodriguez.intentionalreading.data.DatasetRefreshErrorCode
import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.di.modeFor
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LaunchNightModeTest {
    @Test
    fun `the stored appearance is pushed to the platform when it changes`() = runBlocking {
        val appliedModes = mutableListOf<Int>()
        val viewModel = viewModel { appearance -> appliedModes += modeFor(appearance) }
        appliedModes.clear()

        viewModel.setAppearance(Appearance.LIGHT)
        viewModel.setAppearance(Appearance.DARK)
        viewModel.setAppearance(Appearance.SYSTEM)

        assertEquals(
            listOf(
                UiModeManager.MODE_NIGHT_NO,
                UiModeManager.MODE_NIGHT_YES,
                UiModeManager.MODE_NIGHT_AUTO,
            ),
            appliedModes,
        )
    }

    @Test
    fun `the platform is told once per process, not once per action`() = runBlocking {
        val stored = LocalState.default().copy(settings = LocalState.Settings(Appearance.DARK))
        val appliedModes = mutableListOf<Int>()
        val viewModel = viewModel(
            store = FakeLocalStateStore(success(stored)),
            applyNightMode = { appearance -> appliedModes += modeFor(appearance) },
        )
        val article = article()

        assertTrue(viewModel.onArticleAction(article, ArticleAction.SAVE).persisted)
        assertTrue(viewModel.onArticleAction(article, ArticleAction.OPEN).persisted)
        assertTrue(viewModel.onArticleAction(article, ArticleAction.MARK_READ).persisted)

        assertEquals(listOf(UiModeManager.MODE_NIGHT_YES), appliedModes)
    }

    @Test
    fun `resetting local data returns the launch frame to following the system`() = runBlocking {
        val stored = LocalState.default().copy(settings = LocalState.Settings(Appearance.DARK))
        val appliedModes = mutableListOf<Int>()
        val viewModel = viewModel(
            store = FakeLocalStateStore(success(stored)),
            applyNightMode = { appearance -> appliedModes += modeFor(appearance) },
        )

        assertIs<LocalStateResult.Success>(viewModel.resetLocalData())

        assertEquals(
            listOf(UiModeManager.MODE_NIGHT_YES, UiModeManager.MODE_NIGHT_AUTO),
            appliedModes,
        )
    }

    @Test
    fun `a fresh AppViewModel built without an applier still works`() = runBlocking {
        val viewModel = viewModelWithoutApplier()

        assertTrue(viewModel.localStateReady.value)
        assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
        assertTrue(viewModel.onArticleAction(article(), ArticleAction.SAVE).persisted)
    }

    private fun viewModel(
        store: FakeLocalStateStore = FakeLocalStateStore(),
        applyNightMode: (Appearance) -> Unit,
    ): AppViewModel = AppViewModel(
        readCachedDataset = { DatasetCacheRead.Absent },
        refreshDataset = { failedRefresh() },
        loadLocalState = store::load,
        saveLocalState = store::save,
        resetLocalState = store::reset,
        nowProvider = { now },
        zoneProvider = { zone },
        localeProvider = { Locale.US },
        loadDispatcher = Dispatchers.Unconfined,
        applyNightMode = applyNightMode,
    )

    private fun viewModelWithoutApplier(
        store: FakeLocalStateStore = FakeLocalStateStore(),
    ): AppViewModel = AppViewModel(
        readCachedDataset = { DatasetCacheRead.Absent },
        refreshDataset = { failedRefresh() },
        loadLocalState = store::load,
        saveLocalState = store::save,
        resetLocalState = store::reset,
        nowProvider = { now },
        zoneProvider = { zone },
        localeProvider = { Locale.US },
        loadDispatcher = Dispatchers.Unconfined,
    )

    private fun failedRefresh(): DatasetRefreshResult.Failed = DatasetRefreshResult.Failed(
        code = DatasetRefreshErrorCode.FETCH,
    )

    private fun success(state: LocalState): LocalStateResult.Success =
        LocalStateResult.Success(state, LocalStateSource.STORAGE)

    private fun article(): Article = Article(
        id = "00000000000000000001",
        title = "Launch theme article",
        url = "https://example.com/launch-theme",
        source = ArticleSource("source", "Source"),
        category = Category.IAM,
        publishedAt = now.minusSeconds(3_600L),
        author = null,
        excerpt = "Excerpt",
        readingTimeMinutes = 7,
        tags = listOf(ArticleTag("oauth", "OAuth")),
        contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
        score = ArticleScore(91, 50, 20, 15, 5, 1),
    )

    private companion object {
        val now: Instant = Instant.parse("2026-08-25T12:00:00Z")
        val zone: ZoneId = ZoneId.of("America/Managua")
    }
}
