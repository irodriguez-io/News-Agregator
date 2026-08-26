package io.irodriguez.intentionalreading.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.data.LocalStateRepository
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.state.ArticleStateMachine
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.domain.state.UndoRecord
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.state.UiStateMapper
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class Destination {
    READ_LATER,
    DISCOVER,
    HISTORY,
}

data class ArticleActionResult(
    val transition: ArticleTransition,
    val persisted: Boolean,
    val allowNavigation: Boolean,
    val failure: LocalStateResult.Failure? = null,
)

enum class AppAnnouncementKind {
    PERSISTENCE_FAILED,
    OPEN_NOT_PERSISTED,
    OPEN_NAVIGATION_FAILED,
    RESET_FAILED,
    RESET_COMPLETE,
    REFRESH_UPDATED,
    REFRESH_CURRENT,
    REFRESH_FAILED,
    UNDO_COMPLETED,
    UNDO_FAILED,
}

data class AppAnnouncement(
    val id: Long,
    val kind: AppAnnouncementKind,
)

class AppViewModel(
    private val readCachedDataset: suspend () -> DatasetCacheRead,
    private val refreshDataset: suspend () -> DatasetRefreshResult,
    private val loadLocalState: suspend () -> LocalStateResult,
    private val saveLocalState: suspend (LocalState) -> LocalStateResult,
    private val resetLocalState: suspend () -> LocalStateResult,
    private val nowProvider: () -> Instant,
    private val zoneProvider: () -> ZoneId,
    private val localeProvider: () -> Locale,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val applyNightMode: (Appearance) -> Unit = {},
) : ViewModel() {
    private var phase: DatasetPhase = DatasetPhase.Loading
    private var refreshPhase: DatasetRefreshPhase = DatasetRefreshPhase.Idle
    private var localState: LocalState = LocalState.default()
    private var lastAppliedAppearance: Appearance? = null
    private var undoRecord: UndoRecord? = null
    private var pendingUndoOffer: PendingUndoOffer? = null
    private var nextUndoOfferId = 0L
    private val stateMutex = Mutex()

    private val _destination = MutableStateFlow(Destination.DISCOVER)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    private val _settingsOpen = MutableStateFlow(false)
    val settingsOpen: StateFlow<Boolean> = _settingsOpen.asStateFlow()

    private val _appearance = MutableStateFlow(Appearance.SYSTEM)
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _heldArticleId = MutableStateFlow<String?>(null)
    val heldArticleId: StateFlow<String?> = _heldArticleId.asStateFlow()

    private val _localStateReady = MutableStateFlow(false)
    val localStateReady: StateFlow<Boolean> = _localStateReady.asStateFlow()

    private val _localStateError = MutableStateFlow<LocalStateResult.Failure?>(null)
    val localStateError: StateFlow<LocalStateResult.Failure?> = _localStateError.asStateFlow()

    private val _recoveryNoticeVisible = MutableStateFlow(false)
    val recoveryNoticeVisible: StateFlow<Boolean> = _recoveryNoticeVisible.asStateFlow()

    private val _announcement = MutableStateFlow<AppAnnouncement?>(null)
    val announcement: StateFlow<AppAnnouncement?> = _announcement.asStateFlow()
    private var nextAnnouncementId = 0L

    private val _resetInProgress = MutableStateFlow(false)
    val resetInProgress: StateFlow<Boolean> = _resetInProgress.asStateFlow()

    private val _uiState = MutableStateFlow(mapUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(loadDispatcher) {
            restoreLocalState()
            loadCachedDatasetNow()
            refreshDatasetNow()
        }
    }

    fun selectDestination(destination: Destination) {
        _destination.value = destination
    }

    fun toggleSettings() {
        _settingsOpen.value = !_settingsOpen.value
    }

    fun openSettings() {
        _settingsOpen.value = true
    }

    fun closeSettings() {
        _settingsOpen.value = false
    }

    fun launchAppearanceChange(appearance: Appearance) {
        viewModelScope.launch(loadDispatcher) {
            setAppearance(appearance)
        }
    }

    fun launchCategorySelection(category: Category?) {
        viewModelScope.launch(loadDispatcher) {
            selectCategory(category)
        }
    }

    fun launchArticleAction(
        article: Article,
        action: ArticleAction,
        undoable: Boolean = false,
        onComplete: (ArticleActionResult) -> Unit = {},
    ) {
        viewModelScope.launch(loadDispatcher) {
            onComplete(onArticleAction(article, action, undoable))
        }
    }

    fun launchResetLocalData(onComplete: (LocalStateResult) -> Unit = {}) {
        viewModelScope.launch(loadDispatcher) {
            onComplete(resetLocalData())
        }
    }

    fun dismissRecoveryNotice() {
        _recoveryNoticeVisible.value = false
    }

    fun acknowledgeAnnouncement(id: Long) {
        if (_announcement.value?.id == id) _announcement.value = null
    }

    fun acknowledgeUndoOffer(id: Long) {
        viewModelScope.launch(loadDispatcher) {
            stateMutex.withLock {
                if (pendingUndoOffer?.id == id) {
                    pendingUndoOffer = null
                    publish()
                }
            }
        }
    }

    fun reportOpenResult(result: ArticleActionResult, navigationOpened: Boolean) {
        when {
            !navigationOpened -> announce(AppAnnouncementKind.OPEN_NAVIGATION_FAILED)
            !result.persisted -> announce(AppAnnouncementKind.OPEN_NOT_PERSISTED)
        }
    }

    suspend fun setAppearance(appearance: Appearance) {
        stateMutex.withLock {
            if (localState.settings.appearance == appearance) return@withLock
            val candidate = localState.copy(settings = LocalState.Settings(appearance))
            when (val result = saveLocalState(candidate)) {
                is LocalStateResult.Success -> {
                    adoptPersistedState(result.state)
                    _localStateError.value = null
                }
                is LocalStateResult.Failure -> recordPersistenceFailure(result)
            }
        }
    }

    suspend fun selectCategory(category: Category?) {
        stateMutex.withLock {
            if (localState.session.lastCategory == category) return@withLock
            val candidate = localState.copy(session = LocalState.Session(category))
            when (val result = saveLocalState(candidate)) {
                is LocalStateResult.Success -> {
                    adoptPersistedState(result.state)
                    _localStateError.value = null
                    _heldArticleId.value = null
                    publish()
                }
                is LocalStateResult.Failure -> recordPersistenceFailure(result)
            }
        }
    }

    suspend fun resetLocalData(): LocalStateResult = stateMutex.withLock {
        _resetInProgress.value = true
        try {
            when (val result = resetLocalState()) {
                is LocalStateResult.Success -> {
                    adoptPersistedState(result.state)
                    undoRecord = null
                    pendingUndoOffer = null
                    _heldArticleId.value = null
                    _localStateError.value = null
                    _recoveryNoticeVisible.value = false
                    publish()
                    announce(AppAnnouncementKind.RESET_COMPLETE)
                    result
                }
                is LocalStateResult.Failure -> {
                    _localStateError.value = result
                    announce(AppAnnouncementKind.RESET_FAILED)
                    result
                }
            }
        } finally {
            _resetInProgress.value = false
        }
    }

    fun reload() {
        viewModelScope.launch(loadDispatcher) {
            refreshDatasetNow(announceOutcome = true)
        }
    }

    suspend fun onArticleAction(
        article: Article,
        action: ArticleAction,
        undoable: Boolean = false,
    ): ArticleActionResult =
        stateMutex.withLock {
            val transition = ArticleStateMachine.transition(
                records = localState.articles,
                article = article,
                action = action,
                now = nowProvider(),
                undoable = undoable,
            )
            when (transition) {
                is ArticleTransition.Invalid -> ArticleActionResult(
                    transition = transition,
                    persisted = false,
                    allowNavigation = false,
                )
                is ArticleTransition.Unchanged -> persistUnchangedTransition(article, action, transition)
                is ArticleTransition.Applied -> persistArticleTransition(
                    article = article,
                    action = action,
                    transition = transition,
                    undoable = undoable,
                )
                is ArticleTransition.Reverted -> error("A forward article action cannot return Reverted")
            }
        }

    suspend fun performUndo(): ArticleActionResult = stateMutex.withLock {
        val pendingUndo = undoRecord
        when (val transition = ArticleStateMachine.reverse(localState.articles, pendingUndo)) {
            is ArticleTransition.Invalid -> {
                if (pendingUndo != null) announce(AppAnnouncementKind.UNDO_FAILED)
                ArticleActionResult(
                    transition = transition,
                    persisted = false,
                    allowNavigation = false,
                )
            }
            is ArticleTransition.Reverted -> persistUndoTransition(
                undoRecord = requireNotNull(pendingUndo),
                transition = transition,
            )
            is ArticleTransition.Applied -> error("Undo cannot return an applied forward transition")
            is ArticleTransition.Unchanged -> error("Undo cannot return an unchanged transition")
        }
    }

    private suspend fun persistUnchangedTransition(
        article: Article,
        action: ArticleAction,
        transition: ArticleTransition.Unchanged,
    ): ArticleActionResult = when (val result = saveLocalState(localState)) {
        is LocalStateResult.Failure -> {
            _localStateError.value = result
            if (action != ArticleAction.OPEN) announce(AppAnnouncementKind.PERSISTENCE_FAILED)
            ArticleActionResult(
                transition = transition,
                persisted = false,
                allowNavigation = action == ArticleAction.OPEN,
                failure = result,
            )
        }
        is LocalStateResult.Success -> {
            adoptPersistedState(result.state)
            _localStateError.value = null
            val persistedTransition = ArticleTransition.Unchanged(localState.articles)
            if (
                action == ArticleAction.OPEN &&
                localState.articles[article.id]?.status == ArticleStatus.OPENED
            ) {
                _heldArticleId.value = article.id
                publish()
            }
            ArticleActionResult(
                transition = persistedTransition,
                persisted = true,
                allowNavigation = action == ArticleAction.OPEN,
            )
        }
    }

    private suspend fun persistArticleTransition(
        article: Article,
        action: ArticleAction,
        transition: ArticleTransition.Applied,
        undoable: Boolean,
    ): ArticleActionResult {
        val candidate = localState.copy(articles = transition.records)
        return when (val result = saveLocalState(candidate)) {
            is LocalStateResult.Failure -> {
                _localStateError.value = result
                if (action != ArticleAction.OPEN) announce(AppAnnouncementKind.PERSISTENCE_FAILED)
                ArticleActionResult(
                    transition = transition,
                    persisted = false,
                    allowNavigation = action == ArticleAction.OPEN,
                    failure = result,
                )
            }
            is LocalStateResult.Success -> {
                adoptPersistedState(result.state)
                _localStateError.value = null
                val persistedRecord = localState.articles.getValue(article.id)
                val persistedTransition = ArticleTransition.Applied(
                    records = localState.articles,
                    record = persistedRecord,
                    undoRecord = transition.undoRecord,
                )
                if (undoable) {
                    transition.undoRecord?.let { record ->
                        undoRecord = record
                        raiseUndoOffer(record.action)
                    }
                }
                if (action == ArticleAction.OPEN && persistedRecord.status == ArticleStatus.OPENED) {
                    _heldArticleId.value = article.id
                }
                clearHeldArticleIfNeeded()
                publish()
                ArticleActionResult(
                    transition = persistedTransition,
                    persisted = true,
                    allowNavigation = action == ArticleAction.OPEN,
                )
            }
        }
    }

    private suspend fun persistUndoTransition(
        undoRecord: UndoRecord,
        transition: ArticleTransition.Reverted,
    ): ArticleActionResult {
        val candidate = localState.copy(articles = transition.records)
        return when (val result = saveLocalState(candidate)) {
            is LocalStateResult.Failure -> {
                recordPersistenceFailure(result)
                ArticleActionResult(
                    transition = transition,
                    persisted = false,
                    allowNavigation = false,
                    failure = result,
                )
            }
            is LocalStateResult.Success -> {
                adoptPersistedState(result.state)
                _localStateError.value = null
                val persistedTransition = ArticleTransition.Reverted(
                    records = localState.articles,
                    record = localState.articles[undoRecord.articleId],
                )
                this.undoRecord = null
                pendingUndoOffer = null
                clearHeldArticleIfNeeded()
                publish()
                announce(AppAnnouncementKind.UNDO_COMPLETED)
                ArticleActionResult(
                    transition = persistedTransition,
                    persisted = true,
                    allowNavigation = false,
                )
            }
        }
    }

    private suspend fun restoreLocalState() {
        stateMutex.withLock {
            when (val result = loadLocalState()) {
                is LocalStateResult.Success -> adoptPersistedState(result.state)
                is LocalStateResult.Failure -> {
                    adoptPersistedState(result.state ?: LocalState.default())
                    _localStateError.value = result
                    _recoveryNoticeVisible.value = true
                }
            }
            _heldArticleId.value = null
            publish()
            _localStateReady.value = true
        }
    }

    private suspend fun loadCachedDatasetNow() {
        val cached = readCachedDataset()
        stateMutex.withLock {
            if (cached is DatasetCacheRead.Present) {
                phase = DatasetPhase.Ready(cached.dataset)
            }
            publish()
        }
    }

    private suspend fun refreshDatasetNow(announceOutcome: Boolean = false) {
        val started = stateMutex.withLock {
            if (refreshPhase == DatasetRefreshPhase.Refreshing) {
                false
            } else {
                refreshPhase = DatasetRefreshPhase.Refreshing
                publish()
                true
            }
        }
        if (!started) return

        val result = refreshDataset()
        stateMutex.withLock {
            val announcementKind = when (result) {
                is DatasetRefreshResult.Updated -> {
                    adoptDataset(result.dataset)
                    refreshPhase = DatasetRefreshPhase.Updated
                    AppAnnouncementKind.REFRESH_UPDATED
                }
                is DatasetRefreshResult.Current -> {
                    if (phase !is DatasetPhase.Ready) {
                        phase = DatasetPhase.Ready(result.dataset)
                    }
                    refreshPhase = DatasetRefreshPhase.Current
                    AppAnnouncementKind.REFRESH_CURRENT
                }
                is DatasetRefreshResult.Failed -> {
                    if (phase !is DatasetPhase.Ready) phase = DatasetPhase.Error
                    refreshPhase = DatasetRefreshPhase.Failed
                    AppAnnouncementKind.REFRESH_FAILED
                }
            }
            publish()
            if (announceOutcome) announce(announcementKind)
        }
    }

    private fun adoptDataset(dataset: ArticleDataset) {
        val displayedArticleId = (uiState.value.discover as? DiscoverUiState.Card)?.article?.id
        _heldArticleId.value = displayedArticleId?.takeIf { candidate ->
            dataset.articles.any { article -> article.id == candidate }
        }
        phase = DatasetPhase.Ready(dataset)
    }

    private fun adoptPersistedState(state: LocalState) {
        localState = state
        val appearance = state.settings.appearance
        if (lastAppliedAppearance != appearance) {
            applyNightMode(appearance)
            lastAppliedAppearance = appearance
        }
        _appearance.value = appearance
        _selectedCategory.value = state.session.lastCategory
    }

    private fun clearHeldArticleIfNeeded() {
        val heldId = _heldArticleId.value
        if (heldId != null && localState.articles[heldId]?.status != ArticleStatus.OPENED) {
            _heldArticleId.value = null
        }
    }

    private fun recordPersistenceFailure(result: LocalStateResult.Failure) {
        _localStateError.value = result
        announce(AppAnnouncementKind.PERSISTENCE_FAILED)
    }

    private fun announce(kind: AppAnnouncementKind) {
        nextAnnouncementId += 1
        _announcement.value = AppAnnouncement(nextAnnouncementId, kind)
    }

    private fun raiseUndoOffer(action: ArticleAction) {
        val message = when (action) {
            ArticleAction.SAVE -> PendingUndoMessage.SAVED
            ArticleAction.DISMISS -> PendingUndoMessage.DISMISSED
            else -> error("Only Save and Dismiss can raise an Undo offer")
        }
        nextUndoOfferId += 1
        pendingUndoOffer = PendingUndoOffer(nextUndoOfferId, message)
    }

    private fun publish() {
        _uiState.value = mapUiState()
    }

    private fun mapUiState(): AppUiState = UiStateMapper.map(
        phase = phase,
        records = localState.articles,
        selectedCategory = _selectedCategory.value,
        heldArticleId = _heldArticleId.value,
        now = nowProvider(),
        zone = zoneProvider(),
        locale = localeProvider(),
        refresh = refreshPhase,
        undoAction = undoRecord?.action,
        pendingUndoOffer = pendingUndoOffer,
    )

    class Factory(
        datasetRepository: DatasetRepository,
        localStateRepository: LocalStateRepository,
        private val nowProvider: () -> Instant = Instant::now,
        private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
        private val localeProvider: () -> Locale = Locale::getDefault,
        private val applyNightMode: (Appearance) -> Unit = {},
    ) : ViewModelProvider.Factory {
        private val readCachedDataset: suspend () -> DatasetCacheRead = datasetRepository::readCache
        private val refreshDataset: suspend () -> DatasetRefreshResult = datasetRepository::refresh
        private val loadLocalState: suspend () -> LocalStateResult = localStateRepository::load
        private val saveLocalState: suspend (LocalState) -> LocalStateResult = localStateRepository::save
        private val resetLocalState: suspend () -> LocalStateResult = localStateRepository::reset

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(
                readCachedDataset = readCachedDataset,
                refreshDataset = refreshDataset,
                loadLocalState = loadLocalState,
                saveLocalState = saveLocalState,
                resetLocalState = resetLocalState,
                nowProvider = nowProvider,
                zoneProvider = zoneProvider,
                localeProvider = localeProvider,
                applyNightMode = applyNightMode,
            ) as T
        }
    }
}
