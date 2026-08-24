package io.irodriguez.intentionalreading.ui

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
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.domain.validation.DatasetErrorCode
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
            loadDataset = {
                datasetLoadStarted = true
                DatasetResult.Success(dataset(listOf(storedArticle, article(2))))
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
            datasetResult = DatasetResult.Success(dataset(listOf(article(1)))),
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
        val result = CompletableDeferred<DatasetResult>()
        val viewModel = viewModel(loadDataset = { result.await() })

        assertIs<DiscoverUiState.Loading>(viewModel.uiState.value.discover)
        result.complete(DatasetResult.Success(dataset()))
        yield()

        assertIs<DiscoverUiState.Card>(viewModel.uiState.value.discover)
        Unit
    }

    @Test
    fun `load failure maps to the error phase without claiming an article`() {
        val viewModel = viewModel(
            datasetResult = DatasetResult.Failure(
                code = DatasetErrorCode.MALFORMED_DATASET,
                message = "broken fixture",
            ),
        )

        assertIs<DiscoverUiState.Error>(viewModel.uiState.value.discover)
    }

    @Test
    fun `opened card is held after persistence and released when its record leaves opened`() = runBlocking {
        val articles = listOf(article(1), article(2))
        val viewModel = viewModel(datasetResult = DatasetResult.Success(dataset(articles)))

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
        val viewModel = viewModel(datasetResult = DatasetResult.Success(dataset(listOf(technology, iam))))
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
            datasetResult = DatasetResult.Success(dataset(articles)),
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
            viewModel.launchArticleAction(target, ArticleAction.SAVE, completed::complete)
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
            datasetResult = DatasetResult.Success(dataset(listOf(firstArticle, secondArticle))),
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
            datasetResult = DatasetResult.Success(dataset(listOf(savedArticle, removedArticle))),
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
        datasetResult: DatasetResult = DatasetResult.Success(dataset()),
        store: FakeLocalStateStore = FakeLocalStateStore(),
        loadDataset: suspend () -> DatasetResult = { datasetResult },
        loadLocalState: suspend () -> LocalStateResult = store::load,
        nowProvider: () -> Instant = { now },
    ): AppViewModel = AppViewModel(
        loadDataset = loadDataset,
        loadLocalState = loadLocalState,
        saveLocalState = store::save,
        nowProvider = nowProvider,
        zoneProvider = { zone },
        localeProvider = { Locale.US },
        loadDispatcher = Dispatchers.Unconfined,
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
        )
    }

    private fun success(state: LocalState): LocalStateResult.Success =
        LocalStateResult.Success(state, LocalStateSource.STORAGE)

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
