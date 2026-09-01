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

sealed interface DatasetRefreshPhase {
    data object Idle : DatasetRefreshPhase
    data object Refreshing : DatasetRefreshPhase
    data object Updated : DatasetRefreshPhase
    data object Current : DatasetRefreshPhase
    data object Failed : DatasetRefreshPhase
}

data class AppUiState(
    val discover: DiscoverUiState,
    val readLater: ReadLaterUiState,
    val history: HistoryUiState,
    val navigationCounts: NavigationCounts,
    val degraded: Boolean,
    val generatedAtLabel: String,
    val lastRefreshOutcome: String,
    val refresh: DatasetRefreshPhase,
    val undoAvailable: Boolean,
    val pendingUndoOffer: PendingUndoOffer?,
)

data class PendingUndoOffer(
    val id: Long,
    val message: PendingUndoMessage,
)

enum class PendingUndoMessage {
    SAVED,
    DISMISSED,
    MARKED_READ,
    MARKED_UNREAD,
    REMOVED,
}

data class NavigationCounts(
    val readLater: Int,
    val history: Int,
)

data class AggregateUiState(
    val count: Int,
    val knownReadingTimeMinutes: Int,
    val unknownReadingTimeCount: Int,
    val firstTagId: String?,
    val firstTagLabel: String?,
)
