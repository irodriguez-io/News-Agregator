package io.irodriguez.intentionalreading.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.state.ArticleStateMachine
import io.irodriguez.intentionalreading.domain.state.ArticleTransition
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
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

enum class Destination {
    READ_LATER,
    DISCOVER,
    HISTORY,
}

enum class Appearance {
    LIGHT,
    DARK,
    SYSTEM,
}

class AppViewModel(
    private val loadDataset: suspend () -> DatasetResult,
    private val nowProvider: () -> Instant,
    private val zoneProvider: () -> ZoneId,
    private val localeProvider: () -> Locale,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private var phase: DatasetPhase = DatasetPhase.Loading
    private var records: Map<String, ArticleRecord> = emptyMap()

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

    private val _uiState = MutableStateFlow(mapUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        reload()
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

    fun setAppearance(appearance: Appearance) {
        _appearance.value = appearance
    }

    fun selectCategory(category: Category?) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        _heldArticleId.value = null
        publish()
    }

    fun reload() {
        phase = DatasetPhase.Loading
        publish()
        viewModelScope.launch(loadDispatcher) {
            phase = when (val result = loadDataset()) {
                is DatasetResult.Success -> DatasetPhase.Ready(result.dataset)
                is DatasetResult.Failure -> DatasetPhase.Error
            }
            publish()
        }
    }

    fun onArticleAction(article: Article, action: ArticleAction): ArticleTransition {
        val transition = ArticleStateMachine.transition(
            records = records,
            article = article,
            action = action,
            now = nowProvider(),
        )
        records = transition.records

        if (action == ArticleAction.OPEN && records[article.id]?.status == ArticleStatus.OPENED) {
            _heldArticleId.value = article.id
        }
        val heldId = _heldArticleId.value
        if (heldId != null && records[heldId]?.status != ArticleStatus.OPENED) {
            _heldArticleId.value = null
        }
        publish()
        return transition
    }

    private fun publish() {
        _uiState.value = mapUiState()
    }

    private fun mapUiState(): AppUiState = UiStateMapper.map(
        phase = phase,
        records = records,
        selectedCategory = _selectedCategory.value,
        heldArticleId = _heldArticleId.value,
        now = nowProvider(),
        zone = zoneProvider(),
        locale = localeProvider(),
    )

    class Factory(
        repository: DatasetRepository,
        private val nowProvider: () -> Instant = Instant::now,
        private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
        private val localeProvider: () -> Locale = Locale::getDefault,
    ) : ViewModelProvider.Factory {
        private val loadDataset: suspend () -> DatasetResult = repository::load

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(
                loadDataset = loadDataset,
                nowProvider = nowProvider,
                zoneProvider = zoneProvider,
                localeProvider = localeProvider,
            ) as T
        }
    }
}
