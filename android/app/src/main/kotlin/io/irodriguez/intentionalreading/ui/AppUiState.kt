package io.irodriguez.intentionalreading.ui

import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.screens.history.HistoryUiState
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterUiState

sealed interface DatasetPhase {
    data object Loading : DatasetPhase
    data object Error : DatasetPhase
    data class Ready(val dataset: ArticleDataset) : DatasetPhase
}

data class AppUiState(
    val discover: DiscoverUiState,
    val readLater: ReadLaterUiState,
    val history: HistoryUiState,
    val navigationCounts: NavigationCounts,
    val degraded: Boolean,
)

data class NavigationCounts(
    val readLater: Int,
    val history: Int,
)

data class AggregateUiState(
    val count: Int,
    val knownReadingTimeMinutes: Int,
    val unknownReadingTimeCount: Int,
    val firstTagId: String?,
)
