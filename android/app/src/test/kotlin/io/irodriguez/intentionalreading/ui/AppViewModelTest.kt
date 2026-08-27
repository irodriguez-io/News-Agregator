package io.irodriguez.intentionalreading.ui

import io.irodriguez.intentionalreading.data.DatasetRefreshErrorCode
import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheMetadata
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.domain.state.ArticleTransitionErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateExport
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverRefreshAffordance
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppViewModelTest {
    @Test
    fun `destination switching preserves the fixed declaration order`() {
        assertEquals(
            listOf(Destination.READ_LATER, Destination.DISCOVER, Destination.HISTORY),
            Destination.entries,
        )
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        assertFalse(viewModel.settingsOpen.value)
        viewModel.toggleSettings()
        assertTrue(viewModel.settingsOpen.value)
        viewModel.toggleSettings()
        assertFalse(viewModel.settingsOpen.value)
    }

    @Test
    fun `state restoration gates dataset loading and applies appearance category and records`() = runBlocking {
        val storedArticle = article(1, Category.TECHNOLOGY)
        val stored = localState(
            record(storedArticle, ArticleStatus.OPENED),
            appearance = Appearance.DARK,
            category = Category.TECHNOLOGY,
        )
        val stateResult = CompletableDeferred<LocalStateResult>()
        var datasetLoadStarted = false
        val store = FakeLocalStateStore()
        val viewModel = viewModel(
            store = store,
            loadLocalState = { stateResult.await() },
            refreshDataset = {
                datasetLoadStarted = true
                updated(dataset(listOf(storedArticle, article(2))))
            },
        )

        assertFalse(viewModel.localStateReady.value)
        assertFalse(datasetLoadStarted)
        assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
        stateResult.complete(LocalStateResult.Success(stored, LocalStateSource.STORAGE))
        yield()

        assertTrue(viewModel.localStateReady.value)
        assertTrue(datasetLoadStarted)
        assertEquals(Appearance.DARK, viewModel.appearance.value)
        assertEquals(Category.TECHNOLOGY, viewModel.selectedCategory.value)
        assertNull(viewModel.heldArticleId.value)
        val card = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(storedArticle.id, card.article.id)
        assertTrue(card.isOpened)
    }

    @Test
    fun `default launch and browsing create no writes or article records`() = runBlocking {
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)

        viewModel.selectDestination(Destination.READ_LATER)
        viewModel.selectDestination(Destination.HISTORY)
        viewModel.selectDestination(Destination.DISCOVER)
        viewModel.selectCategory(null)
        viewModel.setAppearance(Appearance.SYSTEM)

        assertTrue(viewModel.localStateReady.value)
        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(0, viewModel.uiState.value.navigationCounts.history)
        assertTrue(store.saveRequests.isEmpty())
    }

    @Test
    fun `appearance and category persist and restore in a new view model`() = runBlocking {
        val store = FakeLocalStateStore()
        val first = viewModel(store = store)

        first.setAppearance(Appearance.DARK)
        first.selectCategory(Category.TECHNOLOGY)

        assertEquals(2, store.saveRequests.size)
        assertEquals(Appearance.DARK, store.saveRequests.last().settings.appearance)
        assertEquals(Category.TECHNOLOGY, store.saveRequests.last().session.lastCategory)

        val restored = viewModel(store = store)
        assertEquals(Appearance.DARK, restored.appearance.value)
        assertEquals(Category.TECHNOLOGY, restored.selectedCategory.value)
    }

    @Test
    fun `restored Read Later and History use snapshots absent from the dataset`() {
        val saved = article(41)
        val read = article(42)
        val stored = localState(
            record(saved, ArticleStatus.SAVED),
            record(read, ArticleStatus.READ),
        )
        val store = FakeLocalStateStore(success(stored))
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(article(1)))),
            store = store,
        )

        assertEquals(listOf(saved.id), viewModel.uiState.value.readLater.rows.map { it.article.id })
        assertEquals(
            listOf(read.id),
            viewModel.uiState.value.history.groups.flatMap { group -> group.rows }.map { it.article.id },
        )
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
    }

    @Test
    fun `load success maps loading to ready content`() = runBlocking {
        val result = CompletableDeferred<DatasetRefreshResult>()
        val viewModel = viewModel(refreshDataset = { result.await() })

        assertIs<DiscoverUiState.Loading>(viewModel.uiState.value.discover)
        result.complete(updated(dataset()))
        yield()

        assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        Unit
    }

    @Test
    fun `load failure maps to the error phase without claiming an article`() {
        val viewModel = viewModel(refreshResult = failedRefresh())

        assertIs<DiscoverUiState.Error>(viewModel.uiState.value.discover)
    }

    @Test
    fun `cold start publishes the cache before a reachable refresh adopts its replacement`() = runBlocking {
        val cachedDataset = dataset(listOf(article(2), article(3)))
        val replacement = dataset(listOf(article(1), article(2), article(3)))
        val refreshEntered = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(cachedDataset),
            initialRefreshResult = failedRefresh(),
        ).apply {
            refreshBehavior = {
                refreshEntered.complete(Unit)
                releaseRefresh.await()
                updated(replacement)
            }
        }

        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )
        refreshEntered.await()

        assertEquals(1, datasets.cacheReadRequests)
        assertEquals(article(2).id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
        assertEquals(DatasetRefreshPhase.Refreshing, viewModel.uiState.value.refresh)

        releaseRefresh.complete(Unit)
        yield()

        assertEquals(
            Triple(article(2).id, 3, 2),
            assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).let { card ->
                Triple(card.article.id, card.availableCount, card.remainingCount)
            },
        )
        assertEquals(DatasetRefreshPhase.Updated, viewModel.uiState.value.refresh)
    }

    @Test
    fun `first launch offline is recoverable and retry adopts the reachable dataset`() = runBlocking {
        val datasets = FakeDatasetRepository(initialRefreshResult = failedRefresh())
        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )

        assertIs<DiscoverUiState.Error>(viewModel.uiState.value.discover)
        assertEquals(DatasetRefreshPhase.Failed, viewModel.uiState.value.refresh)

        datasets.refreshBehavior = { updated(dataset(listOf(article(7)))) }
        viewModel.reload()
        yield()

        assertEquals(article(7).id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
        assertEquals(DatasetRefreshPhase.Updated, viewModel.uiState.value.refresh)
        assertEquals(2, datasets.refreshRequests)
    }

    @Test
    fun `triaged articles stay excluded when a newer dataset is adopted`() {
        val saved = article(1)
        val dismissed = article(2)
        val read = article(3)
        val newArticle = article(4)
        val stored = localState(
            record(saved, ArticleStatus.SAVED),
            record(dismissed, ArticleStatus.DISMISSED),
            record(read, ArticleStatus.READ),
        )
        val datasets = FakeDatasetRepository(
            initialRefreshResult = updated(dataset(listOf(saved, dismissed, read, newArticle))),
        )

        val viewModel = viewModel(
            store = FakeLocalStateStore(success(stored)),
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )

        val card = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(newArticle.id, card.article.id)
        assertEquals(1, card.availableCount)
        assertEquals(0, card.remainingCount)
        assertEquals(listOf(saved.id), viewModel.uiState.value.readLater.rows.map { it.article.id })
        assertEquals(
            listOf(read.id),
            viewModel.uiState.value.history.groups.flatMap { it.rows }.map { it.article.id },
        )
    }

    @Test
    fun `failed refresh leaves Read Later and History populated from local state`() {
        val saved = article(1)
        val read = article(2)
        val cachedArticle = article(3)
        val stored = localState(
            record(saved, ArticleStatus.SAVED),
            record(read, ArticleStatus.READ),
        )
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(dataset(listOf(cachedArticle))),
            initialRefreshResult = failedRefresh(),
        )

        val viewModel = viewModel(
            store = FakeLocalStateStore(success(stored)),
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )

        assertEquals(cachedArticle.id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
        assertEquals(listOf(saved.id), viewModel.uiState.value.readLater.rows.map { it.article.id })
        assertEquals(
            listOf(read.id),
            viewModel.uiState.value.history.groups.flatMap { it.rows }.map { it.article.id },
        )
        assertEquals(DatasetRefreshPhase.Failed, viewModel.uiState.value.refresh)
    }

    @Test
    fun `displayed article survives adoption when present and deck advances when absent`() = runBlocking {
        val displayed = article(1)
        val oldFollower = article(2)
        val newLeader = article(3)
        val refreshEntered = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(dataset(listOf(displayed, oldFollower))),
            initialRefreshResult = failedRefresh(),
        ).apply {
            refreshBehavior = {
                refreshEntered.complete(Unit)
                releaseRefresh.await()
                updated(dataset(listOf(newLeader, displayed, oldFollower)))
            }
        }
        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )
        refreshEntered.await()
        assertEquals(displayed.id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)

        releaseRefresh.complete(Unit)
        yield()

        val held = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(displayed.id, held.article.id)
        assertEquals(3, held.availableCount)
        assertEquals(2, held.remainingCount)

        val replacementLeader = article(4)
        datasets.refreshBehavior = { updated(dataset(listOf(replacementLeader, oldFollower))) }
        viewModel.reload()
        yield()

        val advanced = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(replacementLeader.id, advanced.article.id)
        assertEquals(2, advanced.availableCount)
        assertEquals(1, advanced.remainingCount)
        assertNull(viewModel.heldArticleId.value)
    }

    @Test
    fun `a second refresh is not started while one is in flight`() = runBlocking {
        val cachedDataset = dataset(listOf(article(1)))
        val refreshEntered = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(cachedDataset),
            initialRefreshResult = failedRefresh(),
        ).apply {
            refreshBehavior = {
                refreshEntered.complete(Unit)
                releaseRefresh.await()
                DatasetRefreshResult.Current(cachedDataset, metadata())
            }
        }
        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )
        refreshEntered.await()

        viewModel.reload()
        viewModel.reload()
        yield()

        assertEquals(1, datasets.refreshRequests)
        assertEquals(DatasetRefreshPhase.Refreshing, viewModel.uiState.value.refresh)

        releaseRefresh.complete(Unit)
        yield()
        assertEquals(DatasetRefreshPhase.Current, viewModel.uiState.value.refresh)
    }

    @Test
    fun `content freshness uses generatedAt even when the dataset was fetched moments ago`() {
        val staleDataset = dataset(
            articles = listOf(article(1)),
            generatedAt = "2026-08-20T12:00:00Z",
        )
        val fetchedMomentsAgo = now.minusSeconds(5)
        val viewModel = viewModel(
            refreshResult = DatasetRefreshResult.Current(
                dataset = staleDataset,
                metadata = metadata(fetchedAt = fetchedMomentsAgo),
            ),
        )

        val card = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals("Content age · 2d", card.contentFreshness)
        assertEquals("Content generated · Aug 20, 2026, 6:00 AM", viewModel.uiState.value.generatedAtLabel)
    }

    @Test
    fun `explicit refresh exposes an in progress affordance and cannot offer a second request`() = runBlocking {
        val cachedDataset = dataset(listOf(article(1)))
        val refreshEntered = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(cachedDataset),
            initialRefreshResult = DatasetRefreshResult.Current(cachedDataset, metadata()),
        ).apply {
            refreshBehavior = {
                if (refreshRequests == 1) {
                    DatasetRefreshResult.Current(cachedDataset, metadata())
                } else {
                    refreshEntered.complete(Unit)
                    releaseRefresh.await()
                    DatasetRefreshResult.Current(cachedDataset, metadata())
                }
            }
        }
        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )

        viewModel.reload()
        refreshEntered.await()
        viewModel.reload()
        yield()

        val card = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(DiscoverRefreshAffordance.IN_PROGRESS, card.refreshAffordance)
        assertEquals(2, datasets.refreshRequests)

        releaseRefresh.complete(Unit)
        yield()
    }

    @Test
    fun `explicit refresh announces updated content`() = runBlocking {
        val initial = dataset(listOf(article(1)))
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(initial),
            initialRefreshResult = DatasetRefreshResult.Current(initial, metadata()),
        )
        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )
        assertNull(viewModel.announcement.value)

        datasets.refreshBehavior = { updated(dataset(listOf(article(2)))) }
        viewModel.reload()
        yield()

        assertEquals(AppAnnouncementKind.REFRESH_UPDATED, viewModel.announcement.value?.kind)
    }

    @Test
    fun `explicit refresh announces already current`() = runBlocking {
        val current = dataset(listOf(article(1)))
        val datasets = FakeDatasetRepository(
            initialCacheRead = cached(current),
            initialRefreshResult = DatasetRefreshResult.Current(current, metadata()),
        )
        val viewModel = viewModel(
            readCachedDataset = datasets::readCache,
            refreshDataset = datasets::refresh,
        )
        assertNull(viewModel.announcement.value)

        datasets.refreshBehavior = { DatasetRefreshResult.Current(current, metadata()) }
        viewModel.reload()
        yield()

        assertEquals(AppAnnouncementKind.REFRESH_CURRENT, viewModel.announcement.value?.kind)
    }

    @Test
    fun `failed explicit refresh announces only content failure while local destinations and Settings remain available`() =
        runBlocking {
            val saved = article(1)
            val read = article(2)
            val cachedArticle = article(3)
            val stored = localState(
                record(saved, ArticleStatus.SAVED),
                record(read, ArticleStatus.READ),
            )
            val cachedDataset = dataset(listOf(cachedArticle))
            val datasets = FakeDatasetRepository(
                initialCacheRead = cached(cachedDataset),
                initialRefreshResult = DatasetRefreshResult.Current(cachedDataset, metadata()),
            )
            val viewModel = viewModel(
                store = FakeLocalStateStore(success(stored)),
                readCachedDataset = datasets::readCache,
                refreshDataset = datasets::refresh,
            )

            datasets.refreshBehavior = { failedRefresh() }
            viewModel.reload()
            yield()

            assertEquals(AppAnnouncementKind.REFRESH_FAILED, viewModel.announcement.value?.kind)
            assertEquals(
                "Last refresh · Failed. Saved reading and History are unchanged.",
                viewModel.uiState.value.lastRefreshOutcome,
            )
            viewModel.selectDestination(Destination.READ_LATER)
            assertEquals(listOf(saved.id), viewModel.uiState.value.readLater.rows.map { it.article.id })
            viewModel.selectDestination(Destination.HISTORY)
            assertEquals(
                listOf(read.id),
                viewModel.uiState.value.history.groups.flatMap { it.rows }.map { it.article.id },
            )
            viewModel.openSettings()
            assertTrue(viewModel.settingsOpen.value)
            assertEquals("Content generated · Aug 22, 2026, 6:00 AM", viewModel.uiState.value.generatedAtLabel)
        }

    @Test
    fun `opened card is held after persistence and released when its record leaves opened`() = runBlocking {
        val articles = listOf(article(1), article(2))
        val viewModel = viewModel(refreshResult = updated(dataset(articles)))

        val opened = applied(viewModel.onArticleAction(articles.first(), ArticleAction.OPEN))
        assertEquals(ArticleStatus.OPENED, opened.record.status)
        assertEquals(articles.first().id, viewModel.heldArticleId.value)
        val heldCard = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(articles.first(), heldCard.article)
        assertTrue(heldCard.isOpened)

        val saved = applied(viewModel.onArticleAction(articles.first(), ArticleAction.SAVE))
        assertEquals(ArticleStatus.SAVED, saved.record.status)
        assertNull(viewModel.heldArticleId.value)
        assertEquals(articles[1], assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article)
    }

    @Test
    fun `changing category releases an opened held card after persistence`() = runBlocking {
        val technology = article(1, Category.TECHNOLOGY)
        val iam = article(2, Category.IAM)
        val viewModel = viewModel(refreshResult = updated(dataset(listOf(technology, iam))))
        viewModel.selectCategory(Category.IAM)

        viewModel.onArticleAction(iam, ArticleAction.OPEN)
        assertEquals(iam.id, viewModel.heldArticleId.value)
        viewModel.selectCategory(null)

        assertNull(viewModel.heldArticleId.value)
        assertEquals(technology, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article)
    }

    @Test
    fun `every article action persists its state-machine transition`() = runBlocking {
        val articles = listOf(article(1), article(2), article(3))
        val store = FakeLocalStateStore()
        val viewModel = viewModel(
            refreshResult = updated(dataset(articles)),
            store = store,
        )

        assertStatus(viewModel, articles[0], ArticleAction.OPEN, ArticleStatus.OPENED)
        assertStatus(viewModel, articles[0], ArticleAction.SAVE, ArticleStatus.SAVED)
        assertStatus(viewModel, articles[0], ArticleAction.REMOVE, ArticleStatus.DISMISSED)
        assertStatus(viewModel, articles[1], ArticleAction.DISMISS, ArticleStatus.DISMISSED)
        assertStatus(viewModel, articles[2], ArticleAction.MARK_READ, ArticleStatus.READ)
        assertStatus(viewModel, articles[2], ArticleAction.MARK_UNREAD, ArticleStatus.SAVED)

        assertEquals(ArticleAction.entries.size, store.saveRequests.size)
        assertEquals(ArticleStatus.DISMISSED, store.saveRequests.last().articles.getValue(articles[0].id).status)
        assertEquals(ArticleStatus.DISMISSED, store.saveRequests.last().articles.getValue(articles[1].id).status)
        assertEquals(ArticleStatus.SAVED, store.saveRequests.last().articles.getValue(articles[2].id).status)
    }

    @Test
    fun `records publish only after a successful write`() = runBlocking {
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        val store = FakeLocalStateStore().apply {
            saveBehavior = { state ->
                enteredSave.complete(Unit)
                releaseSave.await()
                success(state)
            }
        }
        val target = article(1)
        val viewModel = viewModel(store = store)

        val pending = async { viewModel.onArticleAction(target, ArticleAction.SAVE) }
        enteredSave.await()

        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(target.id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
        releaseSave.complete(Unit)
        val result = pending.await()

        assertTrue(result.persisted)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
    }

    @Test
    fun `article write outlives the launching scope and adopts persisted state`() = runBlocking {
        val enteredSave = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()
        val completed = CompletableDeferred<ArticleActionResult>()
        val store = FakeLocalStateStore().apply {
            saveBehavior = { state ->
                enteredSave.complete(Unit)
                releaseSave.await()
                success(state)
            }
        }
        val target = article(1)
        val viewModel = viewModel(store = store)
        val launchingJob = Job()
        val launchingScope = CoroutineScope(Dispatchers.Unconfined + launchingJob)

        val caller = launchingScope.launch {
            viewModel.launchArticleAction(
                target,
                ArticleAction.SAVE,
                undoable = false,
                onComplete = completed::complete,
            )
            enteredSave.await()
        }
        enteredSave.await()
        launchingJob.cancel()
        caller.join()
        releaseSave.complete(Unit)
        val result = completed.await()

        assertTrue(result.persisted)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(target.id, viewModel.uiState.value.readLater.rows.single().article.id)
    }

    @Test
    fun `failed queue write leaves records counts and card unchanged and exposes state`() = runBlocking {
        val store = FakeLocalStateStore().apply {
            saveBehavior = {
                LocalStateResult.Failure(
                    code = LocalStateErrorCode.WRITE_FAILED,
                    message = "disk full",
                )
            }
        }
        val target = article(1)
        val viewModel = viewModel(store = store)

        val result = viewModel.onArticleAction(target, ArticleAction.SAVE)

        assertFalse(result.persisted)
        assertFalse(result.allowNavigation)
        assertEquals(LocalStateErrorCode.WRITE_FAILED, result.failure?.code)
        assertEquals(LocalStateErrorCode.WRITE_FAILED, viewModel.localStateError.value?.code)
        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(target.id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
    }

    @Test
    fun `failed Open permits publisher navigation without claiming opened state`() = runBlocking {
        val store = FakeLocalStateStore().apply {
            saveBehavior = {
                LocalStateResult.Failure(
                    code = LocalStateErrorCode.WRITE_FAILED,
                    message = "disk full",
                )
            }
        }
        val target = article(1)
        val viewModel = viewModel(store = store)

        val result = viewModel.onArticleAction(target, ArticleAction.OPEN)

        assertFalse(result.persisted)
        assertTrue(result.allowNavigation)
        assertEquals(LocalStateErrorCode.WRITE_FAILED, result.failure?.code)
        assertNull(viewModel.heldArticleId.value)
        assertFalse(assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).isOpened)
    }

    @Test
    fun `recovery locked store refuses actions without changing visible state`() = runBlocking {
        val store = FakeLocalStateStore(
            LocalStateResult.Failure(
                code = LocalStateErrorCode.MALFORMED_JSON,
                message = "stored bytes are malformed",
                state = LocalState.default(),
            ),
        ).apply {
            saveBehavior = {
                LocalStateResult.Failure(
                    code = LocalStateErrorCode.RECOVERY_REQUIRED,
                    message = "reset required",
                )
            }
        }
        val viewModel = viewModel(store = store)

        assertTrue(viewModel.localStateReady.value)
        assertEquals(LocalStateErrorCode.MALFORMED_JSON, viewModel.localStateError.value?.code)
        val result = viewModel.onArticleAction(article(1), ArticleAction.SAVE)

        assertEquals(LocalStateErrorCode.RECOVERY_REQUIRED, result.failure?.code)
        assertEquals(LocalStateErrorCode.RECOVERY_REQUIRED, viewModel.localStateError.value?.code)
        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
    }

    @Test
    fun `load failure remains a dismissible recovery notice without a transient announcement`() {
        val store = FakeLocalStateStore(
            LocalStateResult.Failure(
                code = LocalStateErrorCode.MALFORMED_JSON,
                message = "stored bytes are malformed",
                state = LocalState.default(),
            ),
        )
        val viewModel = viewModel(store = store)

        assertTrue(viewModel.recoveryNoticeVisible.value)
        assertNull(viewModel.announcement.value)
        viewModel.dismissRecoveryNotice()

        assertFalse(viewModel.recoveryNoticeVisible.value)
        assertEquals(LocalStateErrorCode.MALFORMED_JSON, viewModel.localStateError.value?.code)
    }

    @Test
    fun `write failure emits a focus neutral announcement that is acknowledged by id`() = runBlocking {
        val store = FakeLocalStateStore().apply {
            saveBehavior = {
                LocalStateResult.Failure(
                    code = LocalStateErrorCode.WRITE_FAILED,
                    message = "disk full",
                )
            }
        }
        val viewModel = viewModel(store = store)

        viewModel.onArticleAction(article(1), ArticleAction.SAVE)

        val announcement = requireNotNull(viewModel.announcement.value)
        assertEquals(AppAnnouncementKind.PERSISTENCE_FAILED, announcement.kind)
        viewModel.acknowledgeAnnouncement(announcement.id + 1)
        assertEquals(announcement, viewModel.announcement.value)
        viewModel.acknowledgeAnnouncement(announcement.id)
        assertNull(viewModel.announcement.value)
    }

    @Test
    fun `Open persistence warning is distinct from navigation failure`() {
        val viewModel = viewModel()
        val noPreferences = LocalState.Preferences(sources = emptyMap(), topics = emptyMap())
        val transition = ArticleTransition.Unchanged(emptyMap(), noPreferences)
        val failedPersistence = ArticleActionResult(
            transition = transition,
            persisted = false,
            allowNavigation = true,
            failure = LocalStateResult.Failure(
                code = LocalStateErrorCode.WRITE_FAILED,
                message = "disk full",
            ),
        )

        viewModel.reportOpenResult(failedPersistence, navigationOpened = true)
        val persistenceWarning = requireNotNull(viewModel.announcement.value)
        assertEquals(AppAnnouncementKind.OPEN_NOT_PERSISTED, persistenceWarning.kind)

        viewModel.reportOpenResult(failedPersistence, navigationOpened = false)
        val navigationFailure = requireNotNull(viewModel.announcement.value)
        assertEquals(AppAnnouncementKind.OPEN_NAVIGATION_FAILED, navigationFailure.kind)
        assertTrue(navigationFailure.id > persistenceWarning.id)
    }

    @Test
    fun `confirmed reset clears state projections defaults settings and dismisses recovery notice`() = runBlocking {
        val saved = article(1)
        val read = article(2)
        val dismissed = article(3)
        val stored = localState(
            record(saved, ArticleStatus.SAVED),
            record(read, ArticleStatus.READ),
            record(dismissed, ArticleStatus.DISMISSED),
            appearance = Appearance.DARK,
            category = Category.TECHNOLOGY,
        )
        val store = FakeLocalStateStore(
            LocalStateResult.Failure(
                code = LocalStateErrorCode.MALFORMED_JSON,
                message = "reset required",
                state = stored,
            ),
        )
        val viewModel = viewModel(store = store)

        val result = viewModel.resetLocalData()

        assertIs<LocalStateResult.Success>(result)
        assertEquals(1, store.resetRequests)
        assertEquals(Appearance.SYSTEM, viewModel.appearance.value)
        assertNull(viewModel.selectedCategory.value)
        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(0, viewModel.uiState.value.navigationCounts.history)
        assertNull(viewModel.localStateError.value)
        assertFalse(viewModel.recoveryNoticeVisible.value)
        assertEquals(AppAnnouncementKind.RESET_COMPLETE, viewModel.announcement.value?.kind)
    }

    @Test
    fun `failed reset preserves every visible state and keeps confirmation recoverable`() = runBlocking {
        val saved = article(1)
        val read = article(2)
        val stored = localState(
            record(saved, ArticleStatus.SAVED),
            record(read, ArticleStatus.READ),
            appearance = Appearance.DARK,
            category = Category.TECHNOLOGY,
        )
        val store = FakeLocalStateStore(success(stored)).apply {
            resetBehavior = {
                LocalStateResult.Failure(
                    code = LocalStateErrorCode.WRITE_FAILED,
                    message = "reset failed",
                )
            }
        }
        val viewModel = viewModel(store = store)

        val result = viewModel.resetLocalData()

        assertIs<LocalStateResult.Failure>(result)
        assertEquals(1, store.resetRequests)
        assertEquals(Appearance.DARK, viewModel.appearance.value)
        assertEquals(Category.TECHNOLOGY, viewModel.selectedCategory.value)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
        assertEquals(LocalStateErrorCode.WRITE_FAILED, viewModel.localStateError.value?.code)
        assertEquals(AppAnnouncementKind.RESET_FAILED, viewModel.announcement.value?.kind)
    }

    @Test
    fun `concurrent actions are serialized and the second write includes the first`() = runBlocking {
        val firstSaveEntered = CompletableDeferred<Unit>()
        val releaseFirstSave = CompletableDeferred<Unit>()
        val store = FakeLocalStateStore().apply {
            saveBehavior = { state ->
                if (saveRequests.size == 1) {
                    firstSaveEntered.complete(Unit)
                    releaseFirstSave.await()
                }
                success(state)
            }
        }
        val firstArticle = article(1)
        val secondArticle = article(2)
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(firstArticle, secondArticle))),
            store = store,
        )

        val first = async { viewModel.onArticleAction(firstArticle, ArticleAction.SAVE) }
        firstSaveEntered.await()
        val second = async { viewModel.onArticleAction(secondArticle, ArticleAction.SAVE) }
        yield()

        assertEquals(1, store.saveRequests.size)
        releaseFirstSave.complete(Unit)
        assertTrue(first.await().persisted)
        assertTrue(second.await().persisted)

        assertEquals(2, store.saveRequests.size)
        assertEquals(setOf(firstArticle.id, secondArticle.id), store.saveRequests.last().articles.keys)
        assertEquals(2, viewModel.uiState.value.navigationCounts.readLater)
    }

    @Test
    fun `the validated state returned by save is adopted`() = runBlocking {
        val preciseNow = Instant.parse("2026-08-22T12:00:00.123456Z")
        val target = article(1)
        val store = FakeLocalStateStore().apply {
            saveBehavior = { state ->
                val entry = state.articles.getValue(target.id)
                val normalized = entry.copy(
                    firstSeenAt = entry.firstSeenAt.truncatedTo(ChronoUnit.MILLIS),
                    savedAt = entry.savedAt?.truncatedTo(ChronoUnit.MILLIS),
                )
                success(state.copy(articles = mapOf(entry.article.id to normalized)))
            }
        }
        val viewModel = viewModel(store = store, nowProvider = { preciseNow })

        val first = applied(viewModel.onArticleAction(target, ArticleAction.SAVE))
        assertEquals(preciseNow.truncatedTo(ChronoUnit.MILLIS), first.record.savedAt)

        val opened = applied(viewModel.onArticleAction(target, ArticleAction.OPEN))
        assertEquals(preciseNow.truncatedTo(ChronoUnit.MILLIS), opened.record.firstSeenAt)
    }

    @Test
    fun `Read Later row actions update queue and History projections immediately after writes`() = runBlocking {
        val savedArticle = article(1)
        val removedArticle = article(2)
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(savedArticle, removedArticle))),
        )

        viewModel.onArticleAction(savedArticle, ArticleAction.SAVE)
        viewModel.onArticleAction(removedArticle, ArticleAction.SAVE)
        viewModel.onArticleAction(savedArticle, ArticleAction.OPEN)

        assertEquals(2, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(0, viewModel.uiState.value.navigationCounts.history)
        val unchanged = viewModel.onArticleAction(savedArticle, ArticleAction.SAVE)
        val unchangedTransition = assertIs<ArticleTransition.Unchanged>(unchanged.transition)
        assertTrue(unchanged.persisted)
        assertEquals(ArticleStatus.SAVED, unchangedTransition.records.getValue(savedArticle.id).status)

        viewModel.onArticleAction(savedArticle, ArticleAction.MARK_READ)
        viewModel.onArticleAction(removedArticle, ArticleAction.REMOVE)

        assertTrue(viewModel.uiState.value.readLater.rows.isEmpty())
        assertEquals(listOf(savedArticle.id), viewModel.uiState.value.history.groups.single().rows.map { it.article.id })
        assertEquals(0, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
    }

    @Test
    fun `History row actions reopen in place then mark unread back to Read Later`() = runBlocking {
        val readArticle = article(1)
        val viewModel = viewModel()

        viewModel.onArticleAction(readArticle, ArticleAction.MARK_READ)
        val reopened = applied(viewModel.onArticleAction(readArticle, ArticleAction.OPEN))

        assertEquals(ArticleStatus.READ, reopened.record.status)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
        assertEquals(readArticle.id, viewModel.uiState.value.history.groups.single().rows.single().article.id)

        val unread = applied(viewModel.onArticleAction(readArticle, ArticleAction.MARK_UNREAD))

        assertEquals(ArticleStatus.SAVED, unread.record.status)
        assertEquals(readArticle.id, viewModel.uiState.value.readLater.rows.single().article.id)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(0, viewModel.uiState.value.navigationCounts.history)
    }

    @Test
    fun `a committed swipe is undo-eligible`() = runBlocking {
        // Given an article on the Discover card
        val target = article(1)
        val viewModel = viewModel()

        // When a swipe commits Save
        val result = viewModel.onArticleAction(target, ArticleAction.SAVE, undoable = true)

        // Then the state changes, the slot holds the action, and Undo is available
        assertTrue(result.persisted)
        assertEquals(ArticleStatus.SAVED, applied(result).record.status)
        assertTrue(viewModel.uiState.value.undoAvailable)
        assertEquals(PendingUndoMessage.SAVED, viewModel.uiState.value.pendingUndoOffer?.message)
    }

    @Test
    fun `a labeled button press is still not undo-eligible`() = runBlocking {
        // Given the four labeled article actions
        val actions = listOf(
            ArticleAction.DISMISS,
            ArticleAction.SAVE,
            ArticleAction.OPEN,
            ArticleAction.MARK_READ,
        )

        // When each action is committed through the non-undoable button path
        actions.forEachIndexed { index, action ->
            val viewModel = viewModel()
            val result = viewModel.onArticleAction(article(index + 1), action, undoable = false)

            // Then the slot is unchanged and no offer is raised
            assertTrue(result.persisted)
            assertFalse(viewModel.uiState.value.undoAvailable)
            assertNull(viewModel.uiState.value.pendingUndoOffer)
        }
    }

    @Test
    fun `the slot holds one action, and the newest wins`() = runBlocking {
        // Given an undo-eligible dismiss followed by an undo-eligible save of a different article
        val dismissedArticle = article(1)
        val savedArticle = article(2)
        val store = FakeLocalStateStore()
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(dismissedArticle, savedArticle))),
            store = store,
        )
        viewModel.onArticleAction(
            dismissedArticle,
            ArticleAction.DISMISS,
            undoable = true,
        )
        assertEquals(PendingUndoMessage.DISMISSED, viewModel.uiState.value.pendingUndoOffer?.message)
        viewModel.onArticleAction(
            savedArticle,
            ArticleAction.SAVE,
            undoable = true,
        )

        // When Undo is performed
        val undone = viewModel.performUndo()

        // Then only the save is reversed, the dismiss stays applied, and no older Undo remains
        assertTrue(undone.persisted)
        assertIs<ArticleTransition.Reverted>(undone.transition)
        val stored = assertIs<LocalStateResult.Success>(store.loadResult).state
        assertEquals(ArticleStatus.DISMISSED, stored.articles.getValue(dismissedArticle.id).status)
        assertFalse(savedArticle.id in stored.articles)
        assertFalse(viewModel.uiState.value.undoAvailable)
        assertNull(viewModel.uiState.value.pendingUndoOffer)
        val writesAfterUndo = store.saveRequests.size
        val refused = viewModel.performUndo()
        val invalid = assertIs<ArticleTransition.Invalid>(refused.transition)
        assertEquals(ArticleTransitionErrorCode.UNDO_UNAVAILABLE, invalid.code)
        assertEquals(writesAfterUndo, store.saveRequests.size)
    }

    @Test
    fun `each committed swipe raises its own offer`() = runBlocking {
        // Given a swipe that has raised an Undo offer
        val dismissedArticle = article(1)
        val savedArticle = article(2)
        val store = FakeLocalStateStore()
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(dismissedArticle, savedArticle))),
            store = store,
        )
        viewModel.onArticleAction(
            dismissedArticle,
            ArticleAction.DISMISS,
            undoable = true,
        )
        val firstOffer = requireNotNull(viewModel.uiState.value.pendingUndoOffer)

        // When a second swipe commits on the next article
        viewModel.onArticleAction(
            savedArticle,
            ArticleAction.SAVE,
            undoable = true,
        )
        val secondOffer = requireNotNull(viewModel.uiState.value.pendingUndoOffer)

        // Then a distinct offer is raised and the slot holds only the newer action
        assertTrue(secondOffer.id > firstOffer.id)
        assertEquals(PendingUndoMessage.DISMISSED, firstOffer.message)
        assertEquals(PendingUndoMessage.SAVED, secondOffer.message)
        viewModel.acknowledgeUndoOffer(firstOffer.id)
        assertEquals(secondOffer, viewModel.uiState.value.pendingUndoOffer)
    }

    @Test
    fun `launchArticleAction threads undo eligibility to the slot`() = runBlocking {
        // Given a launch entry point and an eligible Save action
        val target = article(1)
        val completed = CompletableDeferred<ArticleActionResult>()
        val viewModel = viewModel()

        // When the launched commit is explicitly marked undo-eligible
        viewModel.launchArticleAction(
            article = target,
            action = ArticleAction.SAVE,
            undoable = true,
            onComplete = completed::complete,
        )

        // Then the successful commit publishes an Undo offer
        assertTrue(completed.await().persisted)
        assertTrue(viewModel.uiState.value.undoAvailable)
        assertEquals(PendingUndoMessage.SAVED, viewModel.uiState.value.pendingUndoOffer?.message)
    }

    @Test
    fun `Undo through the launcher completes on the view model scope`() = runBlocking {
        // Given an undo-eligible commit and an Undo write that is still in progress
        val target = article(1)
        val enteredUndoWrite = CompletableDeferred<Unit>()
        val releaseUndoWrite = CompletableDeferred<Unit>()
        val completed = CompletableDeferred<ArticleActionResult>()
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)
        assertTrue(viewModel.onArticleAction(target, ArticleAction.SAVE, undoable = true).persisted)
        store.saveBehavior = { state ->
            enteredUndoWrite.complete(Unit)
            releaseUndoWrite.await()
            success(state)
        }
        val launchingJob = Job()
        val launchingScope = CoroutineScope(Dispatchers.Unconfined + launchingJob)

        // When the calling scope leaves while Undo is persisting
        val caller = launchingScope.launch {
            viewModel.launchUndo(onComplete = completed::complete)
            awaitCancellation()
        }
        enteredUndoWrite.await()
        launchingJob.cancel()
        caller.join()
        releaseUndoWrite.complete(Unit)
        val result = completed.await()

        // Then the ViewModel-owned work adopts the persisted result and completes its callback
        assertTrue(result.persisted)
        assertFalse(target.id in assertIs<LocalStateResult.Success>(store.loadResult).state.articles)
        assertFalse(viewModel.uiState.value.undoAvailable)
    }

    @Test
    fun `the offer's message names the action`() = runBlocking {
        // Given articles that can be committed by swipe
        val savedArticle = article(1)
        val dismissedArticle = article(2)
        val viewModel = viewModel()

        // When Save and Dismiss are committed as undoable actions
        viewModel.onArticleAction(savedArticle, ArticleAction.SAVE, undoable = true)
        val savedOffer = requireNotNull(viewModel.uiState.value.pendingUndoOffer)
        viewModel.onArticleAction(dismissedArticle, ArticleAction.DISMISS, undoable = true)
        val dismissedOffer = requireNotNull(viewModel.uiState.value.pendingUndoOffer)

        // Then each offer names its committed action
        assertEquals(PendingUndoMessage.SAVED, savedOffer.message)
        assertEquals(PendingUndoMessage.DISMISSED, dismissedOffer.message)
    }

    @Test
    fun `the offer expires without withdrawing Undo`() = runBlocking {
        // Given a raised Undo offer
        val target = article(1)
        val viewModel = viewModel()
        viewModel.onArticleAction(target, ArticleAction.DISMISS, undoable = true)
        val offer = requireNotNull(viewModel.uiState.value.pendingUndoOffer)

        // When the offer is acknowledged by id
        viewModel.acknowledgeUndoOffer(offer.id)

        // Then the offer is withdrawn while the slot and Undo remain available
        assertNull(viewModel.uiState.value.pendingUndoOffer)
        assertTrue(viewModel.uiState.value.undoAvailable)
        assertTrue(viewModel.performUndo().persisted)
    }

    @Test
    fun `Undo from the offer restores the article and announces`() = runBlocking {
        // Given a raised offer for a dismiss and pre-existing preferences
        val target = article(1)
        val previousRecord = record(target, ArticleStatus.OPENED)
        val preferences = LocalState.Preferences(
            sources = mapOf(target.source.id to PreferenceEntry(weight = 1.5, interactions = 3)),
            topics = mapOf("oauth" to PreferenceEntry(weight = -0.5, interactions = 2)),
        )
        val initial = LocalState.default().copy(
            articles = mapOf(target.id to previousRecord),
            preferences = preferences,
        )
        val store = FakeLocalStateStore(success(initial))
        val viewModel = viewModel(store = store)
        viewModel.onArticleAction(target, ArticleAction.DISMISS, undoable = true)
        assertTrue(viewModel.uiState.value.undoAvailable)
        val preferencesAfterDismiss = assertIs<LocalStateResult.Success>(store.loadResult).state.preferences

        // When Undo is taken from the offer
        val result = viewModel.performUndo()

        // Then the exact record and preferences return, completion is announced, and offer and slot withdraw
        assertTrue(result.persisted)
        val persisted = assertIs<LocalStateResult.Success>(store.loadResult).state
        assertEquals(previousRecord, persisted.articles[target.id])
        // Scenario: Undo Dismiss reverses the signal applied by the forward transition.
        assertNotEquals(preferences, preferencesAfterDismiss)
        assertEquals(preferences, persisted.preferences)
        assertEquals(AppAnnouncementKind.UNDO_COMPLETED, viewModel.announcement.value?.kind)
        assertNull(viewModel.uiState.value.pendingUndoOffer)
        assertFalse(viewModel.uiState.value.undoAvailable)
    }

    @Test
    fun `a refused Undo announces its failure and keeps the offer`() = runBlocking {
        // Given a raised offer whose article record is later absent from the adopted state
        val target = article(1)
        val other = article(2)
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)
        viewModel.onArticleAction(target, ArticleAction.DISMISS, undoable = true)
        val offer = requireNotNull(viewModel.uiState.value.pendingUndoOffer)
        store.saveBehavior = { candidate -> success(candidate.copy(articles = candidate.articles - target.id)) }
        viewModel.onArticleAction(other, ArticleAction.SAVE, undoable = false)
        val writesBeforeUndo = store.saveRequests.size

        // When Undo is refused as stale
        val result = viewModel.performUndo()

        // Then nothing is written, failure is announced, and the same offer remains
        val invalid = assertIs<ArticleTransition.Invalid>(result.transition)
        assertEquals(ArticleTransitionErrorCode.UNDO_STALE, invalid.code)
        assertEquals(writesBeforeUndo, store.saveRequests.size)
        assertEquals(AppAnnouncementKind.UNDO_FAILED, viewModel.announcement.value?.kind)
        assertEquals(offer, viewModel.uiState.value.pendingUndoOffer)
        assertTrue(viewModel.uiState.value.undoAvailable)
    }

    @Test
    fun `a swipe whose write fails is not visually finalized`() = runBlocking {
        // Given an article and a local-state store that refuses the swipe write
        val store = FakeLocalStateStore().apply {
            saveBehavior = {
                LocalStateResult.Failure(
                    code = LocalStateErrorCode.WRITE_FAILED,
                    message = "disk full",
                )
            }
        }
        val viewModel = viewModel(store = store)

        // When a swipe commits an undoable Save
        val result = viewModel.onArticleAction(article(1), ArticleAction.SAVE, undoable = true)

        // Then persistence fails and neither an offer nor an Undo slot is published
        assertFalse(result.persisted)
        assertNull(viewModel.uiState.value.pendingUndoOffer)
        assertFalse(viewModel.uiState.value.undoAvailable)
        assertEquals(AppAnnouncementKind.PERSISTENCE_FAILED, viewModel.announcement.value?.kind)
    }

    @Test
    fun `Undo is refused when there is nothing to undo`() = runBlocking {
        // Given an empty undo slot
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)

        // When Undo is requested
        val result = viewModel.performUndo()

        // Then it fails as unavailable without a write or announcement
        val invalid = assertIs<ArticleTransition.Invalid>(result.transition)
        assertEquals(ArticleTransitionErrorCode.UNDO_UNAVAILABLE, invalid.code)
        assertFalse(result.persisted)
        assertFalse(result.allowNavigation)
        assertTrue(store.saveRequests.isEmpty())
        assertNull(viewModel.announcement.value)
        assertFalse(viewModel.uiState.value.undoAvailable)
    }

    @Test
    fun `a failed write leaves both the state and the offer intact`() = runBlocking {
        // Given a populated undo slot
        val target = article(1)
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)
        viewModel.onArticleAction(target, ArticleAction.SAVE, undoable = true)
        val committed = assertIs<LocalStateResult.Success>(store.loadResult).state
        store.saveBehavior = {
            LocalStateResult.Failure(
                code = LocalStateErrorCode.WRITE_FAILED,
                message = "disk full",
            )
        }

        // When Undo is performed and the local state write fails
        val failed = viewModel.performUndo()

        // Then stored state is unchanged, the same offer remains, and persistence failure is announced
        assertFalse(failed.persisted)
        assertEquals(committed, assertIs<LocalStateResult.Success>(store.loadResult).state)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertTrue(viewModel.uiState.value.undoAvailable)
        assertEquals(PendingUndoMessage.SAVED, viewModel.uiState.value.pendingUndoOffer?.message)
        assertEquals(AppAnnouncementKind.PERSISTENCE_FAILED, viewModel.announcement.value?.kind)

        store.saveBehavior = { success(it) }
        val retried = viewModel.performUndo()
        assertTrue(retried.persisted)
        assertFalse(viewModel.uiState.value.undoAvailable)
        assertFalse(target.id in assertIs<LocalStateResult.Success>(store.loadResult).state.articles)
    }

    @Test
    fun `resetting local data withdraws the offer`() = runBlocking {
        // Given a populated undo slot
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)
        viewModel.onArticleAction(article(1), ArticleAction.DISMISS, undoable = true)
        assertTrue(viewModel.uiState.value.undoAvailable)

        // When local data is reset
        assertIs<LocalStateResult.Success>(viewModel.resetLocalData())

        // Then the slot is empty and Undo is unavailable
        assertFalse(viewModel.uiState.value.undoAvailable)
        assertNull(viewModel.uiState.value.pendingUndoOffer)
        val writesAfterReset = store.saveRequests.size
        val invalid = assertIs<ArticleTransition.Invalid>(viewModel.performUndo().transition)
        assertEquals(ArticleTransitionErrorCode.UNDO_UNAVAILABLE, invalid.code)
        assertEquals(writesAfterReset, store.saveRequests.size)
    }

    @Test
    fun `an undo offer does not survive the process`() = runBlocking {
        // Given a populated undo slot whose committed article state is on disk
        val target = article(1)
        val store = FakeLocalStateStore()
        val firstProcess = viewModel(store = store)
        firstProcess.onArticleAction(target, ArticleAction.SAVE, undoable = true)
        assertTrue(firstProcess.uiState.value.undoAvailable)
        assertEquals(ArticleStatus.SAVED, assertIs<LocalStateResult.Success>(store.loadResult).state.articles[target.id]?.status)

        // When the application process is recreated
        val freshProcess = viewModel(store = store)

        // Then the offer is absent regardless of the state on disk
        assertFalse(freshProcess.uiState.value.undoAvailable)
        assertNull(freshProcess.uiState.value.pendingUndoOffer)
        val writesBeforeRefusal = store.saveRequests.size
        val invalid = assertIs<ArticleTransition.Invalid>(freshProcess.performUndo().transition)
        assertEquals(ArticleTransitionErrorCode.UNDO_UNAVAILABLE, invalid.code)
        assertEquals(writesBeforeRefusal, store.saveRequests.size)
    }

    @Test
    fun `an undone article returns to the head of Discover on its own`() = runBlocking {
        // Given the reader opened and then undo-eligibly dismissed the article Discover was showing
        val target = article(1)
        val next = article(2)
        val preferences = LocalState.Preferences(
            sources = mapOf(target.source.id to PreferenceEntry(weight = 1.5, interactions = 3)),
            topics = mapOf("oauth" to PreferenceEntry(weight = -0.5, interactions = 2)),
        )
        val store = FakeLocalStateStore(success(LocalState.default().copy(preferences = preferences)))
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(target, next))),
            store = store,
        )
        viewModel.onArticleAction(target, ArticleAction.OPEN)
        assertEquals(target.id, viewModel.heldArticleId.value)
        val preferencesAfterOpen = assertIs<LocalStateResult.Success>(store.loadResult).state.preferences
        viewModel.onArticleAction(target, ArticleAction.DISMISS, undoable = true)
        assertNull(viewModel.heldArticleId.value)
        assertEquals(next.id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
        val preferencesBeforeUndo = assertIs<LocalStateResult.Success>(store.loadResult).state.preferences

        // When Undo is performed
        val result = viewModel.performUndo()

        // Then dataset order restores the article to the head without re-establishing the held pin
        assertTrue(result.persisted)
        val card = assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        assertEquals(target.id, card.article.id)
        assertTrue(card.isOpened)
        assertNull(viewModel.heldArticleId.value)
        val persisted = assertIs<LocalStateResult.Success>(store.loadResult).state
        assertEquals(ArticleStatus.OPENED, persisted.articles.getValue(target.id).status)
        // Scenario: Undo Dismiss removes only Dismiss learning and preserves earlier Open learning.
        assertNotEquals(preferencesBeforeUndo, persisted.preferences)
        assertEquals(preferencesAfterOpen, persisted.preferences)
    }

    @Test
    fun `a valid backup replaces local state wholesale`() = runBlocking {
        // Given current state and a valid backup with disjoint records and preferences
        val currentSaved = article(21)
        val currentRead = article(22)
        val importedSaved = article(31)
        val importedRead = article(32)
        val current = localState(
            record(currentSaved, ArticleStatus.SAVED),
            record(currentRead, ArticleStatus.READ),
        ).copy(
            preferences = LocalState.Preferences(
                sources = mapOf("current-only" to PreferenceEntry(1.0, 1)),
                topics = emptyMap(),
            ),
        )
        val imported = localState(
            record(importedSaved, ArticleStatus.SAVED),
            record(importedRead, ArticleStatus.READ),
        ).copy(
            preferences = LocalState.Preferences(
                sources = mapOf("import-only" to PreferenceEntry(-1.0, 2)),
                topics = emptyMap(),
            ),
        )
        val candidateBytes = "valid backup".encodeToByteArray()
        val store = FakeLocalStateStore(success(current)).apply {
            importBehavior = { success(imported) }
        }
        val viewModel = viewModel(store = store)

        // When the backup is imported
        assertTrue(viewModel.importLocalData(candidateBytes))

        // Then it replaces the current state instead of merging either population
        assertTrue(store.importRequests.single().contentEquals(candidateBytes))
        assertEquals(imported, assertIs<LocalStateResult.Success>(store.loadResult).state)
        assertEquals(listOf(importedSaved.id), viewModel.uiState.value.readLater.rows.map { it.article.id })
        assertEquals(
            listOf(importedRead.id),
            viewModel.uiState.value.history.groups.flatMap { it.rows }.map { it.article.id },
        )
        assertFalse(currentSaved.id in assertIs<LocalStateResult.Success>(store.loadResult).state.articles)
        assertFalse(currentRead.id in assertIs<LocalStateResult.Success>(store.loadResult).state.articles)
        assertEquals(AppAnnouncementKind.IMPORT_COMPLETE, viewModel.announcement.value?.kind)
    }

    @Test
    fun `an import rebuilds appearance, category, and counts`() = runBlocking {
        // Given a backup with a different appearance, category, queue, and History
        val saved = article(41, Category.TECHNOLOGY)
        val read = article(42, Category.TECHNOLOGY)
        val eligible = article(43, Category.TECHNOLOGY)
        val imported = localState(
            record(saved, ArticleStatus.SAVED),
            record(read, ArticleStatus.READ),
            appearance = Appearance.DARK,
            category = Category.TECHNOLOGY,
        )
        val appliedAppearances = mutableListOf<Appearance>()
        val store = FakeLocalStateStore().apply {
            importBehavior = { success(imported) }
        }
        val viewModel = viewModel(
            refreshResult = updated(dataset(listOf(saved, read, eligible))),
            store = store,
            applyNightMode = appliedAppearances::add,
        )

        // When the backup is imported
        assertTrue(viewModel.importLocalData("backup".encodeToByteArray()))

        // Then the existing adoption and publish paths rebuild every projection
        assertEquals(Appearance.DARK, viewModel.appearance.value)
        assertEquals(Appearance.DARK, appliedAppearances.last())
        assertEquals(Category.TECHNOLOGY, viewModel.selectedCategory.value)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertEquals(1, viewModel.uiState.value.navigationCounts.history)
        assertEquals(eligible.id, assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover).article.id)
    }

    @Test
    fun `an import clears the undo slot`() = runBlocking {
        // Given a populated undo slot and a separately held opened article
        val store = FakeLocalStateStore()
        val viewModel = viewModel(store = store)
        val held = article(50)
        viewModel.onArticleAction(held, ArticleAction.OPEN)
        viewModel.onArticleAction(article(51), ArticleAction.SAVE, undoable = true)
        assertEquals(held.id, viewModel.heldArticleId.value)
        assertTrue(viewModel.uiState.value.undoAvailable)
        store.importBehavior = { success(LocalState.default()) }

        // When a valid backup is imported
        assertTrue(viewModel.importLocalData("backup".encodeToByteArray()))

        // Then Undo is unavailable and the old slot cannot be performed
        assertNull(viewModel.heldArticleId.value)
        assertFalse(viewModel.uiState.value.undoAvailable)
        assertNull(viewModel.uiState.value.pendingUndoOffer)
        val invalid = assertIs<ArticleTransition.Invalid>(viewModel.performUndo().transition)
        assertEquals(ArticleTransitionErrorCode.UNDO_UNAVAILABLE, invalid.code)
    }

    @Test
    fun `an import recovers a store that was locked for recovery`() = runBlocking {
        // Given a load failure whose recovery notice is visible
        val loadFailure = LocalStateResult.Failure(
            code = LocalStateErrorCode.INVALID_STATE,
            message = "candidate detail",
            state = LocalState.default(),
            path = "articles.secret.status",
        )
        val store = FakeLocalStateStore(loadFailure).apply {
            importBehavior = { success(LocalState.default()) }
        }
        val viewModel = viewModel(store = store)
        assertTrue(viewModel.recoveryNoticeVisible.value)
        assertEquals(loadFailure, viewModel.localStateError.value)

        // When a valid backup is imported
        assertTrue(viewModel.importLocalData("backup".encodeToByteArray()))

        // Then the resolved cause and its notice are withdrawn
        assertFalse(viewModel.recoveryNoticeVisible.value)
        assertNull(viewModel.localStateError.value)
    }

    @Test
    fun `a refusal does not disclose the candidate's contents`() = runBlocking {
        // Given current local state and a refused candidate carrying sensitive validator detail
        val current = localState(record(article(61), ArticleStatus.SAVED))
        val failure = LocalStateResult.Failure(
            code = LocalStateErrorCode.INVALID_STATE,
            message = "secret candidate title",
            path = "articles.secret-candidate.article.title",
        )
        val store = FakeLocalStateStore(success(current)).apply {
            importBehavior = { failure }
        }
        val viewModel = viewModel(store = store)

        // When the import is refused
        assertFalse(viewModel.importLocalData("untrusted candidate".encodeToByteArray()))

        // Then state is unchanged and only the generic announcement kind reaches the UI
        assertEquals(current, assertIs<LocalStateResult.Success>(store.loadResult).state)
        assertEquals(1, viewModel.uiState.value.navigationCounts.readLater)
        assertNull(viewModel.localStateError.value)
        val announcement = requireNotNull(viewModel.announcement.value)
        assertEquals(AppAnnouncementKind.IMPORT_FAILED, announcement.kind)
        assertFalse(announcement.toString().contains(failure.message))
        assertFalse(announcement.toString().contains(requireNotNull(failure.path)))
    }

    @Test
    fun `cold load writes one changed reconciliation and writes no unchanged reconciliation`() {
        val storedArticle = article(71).copy(
            source = ArticleSource("shared-source", "Shared Source"),
        )
        val preLearning = localState(
            record(storedArticle, ArticleStatus.READ).copy(
                openedAt = now.minusSeconds(120),
                signalsApplied = SignalsApplied(
                    opened = true,
                    saved = false,
                    dismissed = false,
                    read = true,
                ),
            ),
        )
        val changingStore = FakeLocalStateStore(success(preLearning))

        viewModel(store = changingStore)

        assertEquals(1, changingStore.saveRequests.size)
        val reconciled = assertIs<LocalStateResult.Success>(changingStore.loadResult).state
        assertEquals(
            PreferenceEntry(weight = 0.35, interactions = 2),
            reconciled.preferences.sources["shared-source"],
        )

        val unchangedStore = FakeLocalStateStore(success(reconciled))

        viewModel(store = unchangedStore)

        assertEquals(0, unchangedStore.saveRequests.size)
    }

    @Test
    fun `pre-learning unread after post-learning open and save preserves exactly the remaining signals`() = runBlocking {
        val preLearningArticle = article(72).copy(
            source = ArticleSource("shared-source", "Shared Source"),
        )
        val postLearningArticle = article(73).copy(
            source = preLearningArticle.source,
            tags = preLearningArticle.tags,
        )
        val preLearning = localState(
            record(preLearningArticle, ArticleStatus.READ).copy(
                openedAt = now.minusSeconds(120),
                signalsApplied = SignalsApplied(
                    opened = true,
                    saved = false,
                    dismissed = false,
                    read = true,
                ),
            ),
        )
        val store = FakeLocalStateStore(success(preLearning))
        val viewModel = viewModel(store = store)

        assertStatus(viewModel, postLearningArticle, ArticleAction.OPEN, ArticleStatus.OPENED)
        assertStatus(viewModel, postLearningArticle, ArticleAction.SAVE, ArticleStatus.SAVED)
        assertStatus(viewModel, preLearningArticle, ArticleAction.MARK_UNREAD, ArticleStatus.SAVED)

        val persisted = assertIs<LocalStateResult.Success>(store.loadResult).state
        assertEquals(
            PreferenceEntry(weight = 0.65, interactions = 3),
            persisted.preferences.sources["shared-source"],
        )
    }

    @Test
    fun `import reconciles its replacement and retains no pre-import record`() = runBlocking {
        val currentOnly = article(74)
        val importedArticle = article(75).copy(
            source = ArticleSource("imported-source", "Imported Source"),
        )
        val current = localState(record(currentOnly, ArticleStatus.SAVED))
        val imported = localState(
            record(importedArticle, ArticleStatus.READ).copy(
                openedAt = now.minusSeconds(120),
                signalsApplied = SignalsApplied(
                    opened = true,
                    saved = false,
                    dismissed = false,
                    read = true,
                ),
            ),
        )
        val store = FakeLocalStateStore(success(current)).apply {
            importBehavior = { success(imported) }
        }
        val viewModel = viewModel(store = store)

        assertTrue(viewModel.importLocalData("backup".encodeToByteArray()))

        assertEquals(1, store.saveRequests.size)
        val persisted = assertIs<LocalStateResult.Success>(store.loadResult).state
        assertEquals(setOf(importedArticle.id), persisted.articles.keys)
        assertFalse(currentOnly.id in persisted.articles)
        assertEquals(
            PreferenceEntry(weight = 0.35, interactions = 2),
            persisted.preferences.sources["imported-source"],
        )
    }

    @Test
    fun `failed cold load raises recovery notice and attempts no reconciliation write`() {
        val store = FakeLocalStateStore(
            LocalStateResult.Failure(
                code = LocalStateErrorCode.INVALID_STATE,
                message = "stored state is invalid",
                state = LocalState.default(),
            ),
        )
        val viewModel = viewModel(store = store)

        assertTrue(viewModel.recoveryNoticeVisible.value)
        assertEquals(LocalStateErrorCode.INVALID_STATE, viewModel.localStateError.value?.code)
        assertEquals(0, store.saveRequests.size)
    }

    @Test
    fun `an ordinary article save result is adopted without reconciliation`() = runBlocking {
        val store = FakeLocalStateStore().apply {
            saveBehavior = { candidate ->
                success(
                    candidate.copy(
                        preferences = LocalState.Preferences(
                            sources = emptyMap(),
                            topics = emptyMap(),
                        ),
                    ),
                )
            }
        }
        val viewModel = viewModel(store = store)

        val result = viewModel.onArticleAction(article(76), ArticleAction.SAVE)

        assertTrue(result.persisted)
        assertEquals(1, store.saveRequests.size)
        assertEquals(emptyMap(), applied(result).preferences.sources)
        assertEquals(emptyMap(), applied(result).preferences.topics)
        val persisted = assertIs<LocalStateResult.Success>(store.loadResult).state
        assertEquals(emptyMap(), persisted.preferences.sources)
        assertEquals(emptyMap(), persisted.preferences.topics)
    }

    @Test
    fun `export writes nothing when the destination cannot be written`() = runBlocking {
        // Given valid current state and a destination that cannot be opened
        val current = localState(record(article(71), ArticleStatus.READ))
        val exportedBytes = "exported state".encodeToByteArray()
        val store = FakeLocalStateStore(success(current)).apply {
            exportBehavior = { LocalStateExport.Success(exportedBytes) }
        }
        val viewModel = viewModel(store = store)

        // When export encounters the unwritable destination
        val exported = viewModel.exportLocalData {
            throw IOException("destination cannot be opened")
        }

        // Then nothing is reported as written, local state is unchanged, and failure is announced
        assertFalse(exported)
        assertEquals(listOf(current), store.exportRequests)
        assertTrue(store.saveRequests.isEmpty())
        assertEquals(current, assertIs<LocalStateResult.Success>(store.loadResult).state)
        assertEquals(AppAnnouncementKind.EXPORT_FAILED, viewModel.announcement.value?.kind)
    }

    @Test
    fun `export does not hold the state lock while the destination is written`() = runBlocking {
        // Given valid export bytes and a destination write that performs an ordinary state mutation
        val store = FakeLocalStateStore().apply {
            exportBehavior = { LocalStateExport.Success("exported state".encodeToByteArray()) }
        }
        val viewModel = viewModel(store = store)

        // When the destination writes, then the mutation completes before the write returns
        val exported = withTimeout(1_000) {
            viewModel.exportLocalData {
                viewModel.selectCategory(Category.TECHNOLOGY)
                assertEquals(Category.TECHNOLOGY, viewModel.selectedCategory.value)
                true
            }
        }

        assertTrue(exported)
        assertEquals(Category.TECHNOLOGY, viewModel.selectedCategory.value)
        assertEquals(Category.TECHNOLOGY, store.saveRequests.single().session.lastCategory)
    }

    @Test
    fun `the suggested filename carries a UTC timestamp`() {
        // Given the same fixed instant through UTC and a non-UTC device zone
        val fixedNow = Instant.parse("2026-08-27T00:12:34Z")
        val utcViewModel = viewModel(
            nowProvider = { fixedNow },
            zoneProvider = { ZoneId.of("UTC") },
        )
        val managuaViewModel = viewModel(
            nowProvider = { fixedNow },
            zoneProvider = { ZoneId.of("America/Managua") },
        )

        // When the suggested names are generated, then both carry the UTC timestamp
        val expected = "intentional-reading-backup-20260827-001234Z.json"
        assertEquals(expected, utcViewModel.backupFilename())
        assertEquals(expected, managuaViewModel.backupFilename())
    }

    private suspend fun assertStatus(
        viewModel: AppViewModel,
        article: Article,
        action: ArticleAction,
        expected: ArticleStatus,
    ) {
        val result = viewModel.onArticleAction(article, action)
        assertTrue(result.persisted)
        assertEquals(expected, applied(result).record.status)
    }

    private fun applied(result: ArticleActionResult): ArticleTransition.Applied =
        assertIs<ArticleTransition.Applied>(result.transition)

    private fun viewModel(
        refreshResult: DatasetRefreshResult = updated(dataset()),
        store: FakeLocalStateStore = FakeLocalStateStore(),
        readCachedDataset: suspend () -> DatasetCacheRead = { DatasetCacheRead.Absent },
        refreshDataset: suspend () -> DatasetRefreshResult = { refreshResult },
        loadLocalState: suspend () -> LocalStateResult = store::load,
        resetLocalState: suspend () -> LocalStateResult = store::reset,
        nowProvider: () -> Instant = { now },
        zoneProvider: () -> ZoneId = { zone },
        applyNightMode: (Appearance) -> Unit = {},
    ): AppViewModel = AppViewModel(
        readCachedDataset = readCachedDataset,
        refreshDataset = refreshDataset,
        loadLocalState = loadLocalState,
        saveLocalState = store::save,
        resetLocalState = resetLocalState,
        exportLocalState = store::exportState,
        importLocalState = store::importState,
        nowProvider = nowProvider,
        zoneProvider = zoneProvider,
        localeProvider = { Locale.US },
        loadDispatcher = Dispatchers.Unconfined,
        applyNightMode = applyNightMode,
    )

    private fun localState(
        vararg records: ArticleRecord,
        appearance: Appearance = Appearance.SYSTEM,
        category: Category? = null,
    ): LocalState = LocalState.default().copy(
        articles = records.associateBy { it.article.id },
        settings = LocalState.Settings(appearance),
        session = LocalState.Session(category),
    )

    private fun record(article: Article, status: ArticleStatus): ArticleRecord {
        val actionAt = now.minusSeconds(article.id.takeLast(2).toLong(16).coerceAtLeast(1L) * 60L)
        return ArticleRecord(
            article = article,
            status = status,
            firstSeenAt = actionAt.minusSeconds(60),
            openedAt = if (status == ArticleStatus.OPENED) actionAt else null,
            savedAt = if (status == ArticleStatus.SAVED) actionAt else null,
            dismissedAt = if (status == ArticleStatus.DISMISSED) actionAt else null,
            readAt = if (status == ArticleStatus.READ) actionAt else null,
            signalsApplied = SignalsApplied(
                opened = status == ArticleStatus.OPENED,
                saved = false,
                dismissed = false,
                read = status == ArticleStatus.READ,
            ),
        )
    }

    private fun success(state: LocalState): LocalStateResult.Success =
        LocalStateResult.Success(state, LocalStateSource.STORAGE)

    private fun updated(dataset: ArticleDataset): DatasetRefreshResult.Updated =
        DatasetRefreshResult.Updated(
            dataset = dataset,
            metadata = metadata(),
        )

    private fun cached(dataset: ArticleDataset): DatasetCacheRead.Present = DatasetCacheRead.Present(
        bytes = byteArrayOf(),
        dataset = dataset,
        metadata = metadata(),
    )

    private fun metadata(fetchedAt: Instant = now): DatasetCacheMetadata =
        DatasetCacheMetadata(etag = "\"fixture\"", fetchedAt = fetchedAt)

    private fun failedRefresh(): DatasetRefreshResult.Failed = DatasetRefreshResult.Failed(
        code = DatasetRefreshErrorCode.FETCH,
    )

    private fun dataset(
        articles: List<Article> = listOf(article(1)),
        generatedAt: String = "2026-08-22T12:00:00Z",
    ): ArticleDataset = ArticleDataset(
        schemaVersion = 1,
        generatedAt = generatedAt,
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
