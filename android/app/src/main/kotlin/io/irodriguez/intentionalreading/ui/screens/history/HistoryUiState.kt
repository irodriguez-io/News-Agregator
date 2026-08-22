package io.irodriguez.intentionalreading.ui.screens.history

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.ui.AggregateUiState
import java.time.Instant

enum class HistoryPeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    EARLIER("Earlier"),
}

data class HistoryUiState(
    val groups: List<HistoryGroupUiState>,
    val aggregate: AggregateUiState,
)

data class HistoryGroupUiState(
    val period: HistoryPeriod,
    val rows: List<HistoryRowUiState>,
)

data class HistoryRowUiState(
    val article: Article,
    val readAt: Instant?,
    val readAge: String,
    val readDateTime: String,
)
