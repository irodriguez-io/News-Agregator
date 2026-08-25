package io.irodriguez.intentionalreading.ui.screens.discover

import io.irodriguez.intentionalreading.domain.model.Article

enum class DiscoverRefreshAffordance {
    HIDDEN,
    AVAILABLE,
    IN_PROGRESS,
}

sealed interface DiscoverUiState {
    val contentFreshness: String?
    val failedRefreshDisclosure: String?
    val refreshAffordance: DiscoverRefreshAffordance

    data class Loading(
        val copy: String,
        override val contentFreshness: String?,
        override val failedRefreshDisclosure: String?,
        override val refreshAffordance: DiscoverRefreshAffordance,
    ) : DiscoverUiState

    data class Error(
        val title: String,
        val copy: String,
        val actionLabel: String,
        override val contentFreshness: String?,
        override val failedRefreshDisclosure: String?,
        override val refreshAffordance: DiscoverRefreshAffordance,
    ) : DiscoverUiState

    data class Empty(
        val title: String,
        val copy: String,
        val actionLabel: String,
        override val contentFreshness: String?,
        override val failedRefreshDisclosure: String?,
        override val refreshAffordance: DiscoverRefreshAffordance,
    ) : DiscoverUiState

    data class Card(
        val article: Article,
        val publicationAge: String,
        val availableCount: Int,
        val remainingCount: Int,
        val isOpened: Boolean,
        override val contentFreshness: String?,
        override val failedRefreshDisclosure: String?,
        override val refreshAffordance: DiscoverRefreshAffordance,
    ) : DiscoverUiState
}
