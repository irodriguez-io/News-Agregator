package io.irodriguez.intentionalreading.ui.screens.discover

import io.irodriguez.intentionalreading.domain.model.Article

sealed interface DiscoverUiState {
    data class Loading(
        val copy: String,
    ) : DiscoverUiState

    data class Error(
        val title: String,
        val copy: String,
        val actionLabel: String,
    ) : DiscoverUiState

    data class Empty(
        val title: String,
        val copy: String,
        val actionLabel: String,
    ) : DiscoverUiState

    data class Card(
        val article: Article,
        val publicationAge: String,
        val availableCount: Int,
        val remainingCount: Int,
        val isOpened: Boolean,
    ) : DiscoverUiState
}
