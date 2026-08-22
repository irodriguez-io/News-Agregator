package io.irodriguez.intentionalreading.ui.screens.readlater

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.ui.AggregateUiState
import java.time.Instant

data class ReadLaterUiState(
    val rows: List<ReadLaterRowUiState>,
    val aggregate: AggregateUiState,
)

data class ReadLaterRowUiState(
    val article: Article,
    val savedAt: Instant?,
    val savedAge: String,
)
