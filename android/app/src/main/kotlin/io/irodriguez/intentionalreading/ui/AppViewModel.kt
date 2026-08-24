package io.irodriguez.intentionalreading.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.data.LocalStateRepository
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.state.ArticleStateMachine
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
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

class AppViewModel(
    private val loadDataset: suspend () -> DatasetResult,
    private val loadLocalState: suspend () -> LocalStateResult,
    private val saveLocalState: suspend (LocalState) -> LocalStateResult,
    private val nowProvider: () -> Instant,
    private val zoneProvider: () -> ZoneId,
    private val localeProvider: () -> Locale,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private var phase: DatasetPhase = DatasetPhase.Loading
    private var localState: LocalState = LocalState.default()
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

    private val _uiState = MutableStateFlow(mapUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(loadDispatcher) {
            restoreLocalState()
            loadDatasetNow()
        }
    }

    fun selectDestination(destination: Destination) {
        _destination.value = destination
    }

    fun toggleSettings() {
        _settingsOpen.value = !_settingsOpen.value
    }

    fun closeSettings() {
        _settingsOpen.value = false
    }

    suspend fun setAppearance(appearance: Appearance) {
        stateMutex.withLock {
            if (localState.settings.appearance == appearance) return@withLock
            val candidate = localState.copy(settings = LocalState.Settings(appearance))
            when (val result = saveLocalState(candidate)) {
                is LocalStateResult.Success -> adoptPersistedState(result.state)
                is LocalStateResult.Failure -> _localStateError.value = result
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
                    _heldArticleId.value = null
                    publish()
                }
                is LocalStateResult.Failure -> _localStateError.value = result
            }
        }
    }

    fun reload() {
        phase = DatasetPhase.Loading
        publish()
        viewModelScope.launch(loadDispatcher) {
            loadDatasetNow()
        }
    }

    suspend fun onArticleAction(article: Article, action: ArticleAction): ArticleActionResult =
        stateMutex.withLock {
            val transition = ArticleStateMachine.transition(
                records = localState.articles,
                article = article,
                action = action,
                now = nowProvider(),
            )
            when (transition) {
                is ArticleTransition.Invalid -> ArticleActionResult(
                    transition = transition,
                    persisted = false,
                    allowNavigation = false,
                )
                is ArticleTransition.Unchanged -> persistUnchangedTransition(article, action, transition)
                is ArticleTransition.Applied -> persistArticleTransition(article, action, transition)
            }
        }

    private suspend fun persistUnchangedTransition(
        article: Article,
        action: ArticleAction,
        transition: ArticleTransition.Unchanged,
    ): ArticleActionResult = when (val result = saveLocalState(localState)) {
        is LocalStateResult.Failure -> {
            _localStateError.value = result
            ArticleActionResult(
                transition = transition,
                persisted = false,
                allowNavigation = action == ArticleAction.OPEN,
                failure = result,
            )
        }
        is LocalStateResult.Success -> {
            adoptPersistedState(result.state)
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
    ): ArticleActionResult {
        val candidate = localState.copy(articles = transition.records)
        return when (val result = saveLocalState(candidate)) {
            is LocalStateResult.Failure -> {
                _localStateError.value = result
                ArticleActionResult(
                    transition = transition,
                    persisted = false,
                    allowNavigation = action == ArticleAction.OPEN,
                    failure = result,
                )
            }
            is LocalStateResult.Success -> {
                adoptPersistedState(result.state)
                val persistedRecord = localState.articles.getValue(article.id)
                val persistedTransition = ArticleTransition.Applied(
                    records = localState.articles,
                    record = persistedRecord,
                )
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

    private suspend fun restoreLocalState() {
        stateMutex.withLock {
            when (val result = loadLocalState()) {
                is LocalStateResult.Success -> adoptPersistedState(result.state)
                is LocalStateResult.Failure -> {
                    adoptPersistedState(result.state ?: LocalState.default())
                    _localStateError.value = result
                }
            }
            _heldArticleId.value = null
            publish()
            _localStateReady.value = true
        }
    }

    private suspend fun loadDatasetNow() {
        phase = when (val result = loadDataset()) {
            is DatasetResult.Success -> DatasetPhase.Ready(result.dataset)
            is DatasetResult.Failure -> DatasetPhase.Error
        }
        publish()
    }

    private fun adoptPersistedState(state: LocalState) {
        localState = state
        _appearance.value = state.settings.appearance
        _selectedCategory.value = state.session.lastCategory
    }

    private fun clearHeldArticleIfNeeded() {
        val heldId = _heldArticleId.value
        if (heldId != null && localState.articles[heldId]?.status != ArticleStatus.OPENED) {
            _heldArticleId.value = null
        }
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
    )

    class Factory(
        datasetRepository: DatasetRepository,
        localStateRepository: LocalStateRepository,
        private val nowProvider: () -> Instant = Instant::now,
        private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
        private val localeProvider: () -> Locale = Locale::getDefault,
    ) : ViewModelProvider.Factory {
        private val loadDataset: suspend () -> DatasetResult = datasetRepository::load
        private val loadLocalState: suspend () -> LocalStateResult = localStateRepository::load
        private val saveLocalState: suspend (LocalState) -> LocalStateResult = localStateRepository::save

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(
                loadDataset = loadDataset,
                loadLocalState = loadLocalState,
                saveLocalState = saveLocalState,
                nowProvider = nowProvider,
                zoneProvider = zoneProvider,
                localeProvider = localeProvider,
            ) as T
        }
    }
}
