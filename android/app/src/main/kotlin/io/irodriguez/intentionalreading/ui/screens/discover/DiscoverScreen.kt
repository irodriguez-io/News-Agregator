package io.irodriguez.intentionalreading.ui.screens.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.ui.components.ArticleCard
import io.irodriguez.intentionalreading.ui.components.EditorialHeader
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    degraded: Boolean,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    onRetry: () -> Unit,
    onViewReadLater: () -> Unit,
    onDismiss: (Article) -> Unit,
    onReadArticle: (Article) -> Unit,
    onSave: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardState = state as? DiscoverUiState.Card
    val refreshActionLabel = when (state.refreshAffordance) {
        DiscoverRefreshAffordance.HIDDEN -> null
        DiscoverRefreshAffordance.AVAILABLE -> stringResource(R.string.refresh_content)
        DiscoverRefreshAffordance.IN_PROGRESS -> stringResource(R.string.refreshing_content)
    }
    val onRefreshAction = onRetry.takeIf {
        state.refreshAffordance == DiscoverRefreshAffordance.AVAILABLE
    }
    val scrollState = rememberScrollState()
    LaunchedEffect(selectedCategory, cardState?.article?.id, state::class) {
        scrollState.scrollTo(0)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        EditorialHeader(
            availableCount = cardState?.availableCount,
            contentFreshness = state.contentFreshness,
            failedRefreshDisclosure = state.failedRefreshDisclosure,
            degraded = degraded,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            actionLabel = refreshActionLabel,
            onAction = onRefreshAction,
        )

        when (state) {
            is DiscoverUiState.Loading -> LoadingPanel(state)
            is DiscoverUiState.Error -> StatePanel(
                title = state.title,
                copy = state.copy,
                actionLabel = state.actionLabel,
                onAction = onRetry.takeUnless {
                    state.refreshAffordance == DiscoverRefreshAffordance.IN_PROGRESS
                },
            )
            is DiscoverUiState.Empty -> StatePanel(
                title = state.title,
                copy = state.copy,
                actionLabel = state.actionLabel,
                onAction = onViewReadLater,
            )
            is DiscoverUiState.Card -> CardBody(
                state = state,
                onDismiss = onDismiss,
                onReadArticle = onReadArticle,
                onSave = onSave,
                onMarkRead = onMarkRead,
            )
        }
    }
}

@Composable
private fun LoadingPanel(state: DiscoverUiState.Loading) {
    val tokens = LocalIntentionalReadingTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = tokens.surface,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.border),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = state.copy,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(20.dp),
            color = tokens.muted,
        )
    }
}

@Composable
private fun StatePanel(
    title: String,
    copy: String,
    actionLabel: String,
    onAction: (() -> Unit)?,
) {
    val tokens = LocalIntentionalReadingTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = tokens.surface,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.border),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Text(text = copy, style = MaterialTheme.typography.bodyLarge, color = tokens.muted)
            OutlinedButton(
                onClick = { onAction?.invoke() },
                enabled = onAction != null,
                border = BorderStroke(1.dp, tokens.strongBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.fg),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun CardBody(
    state: DiscoverUiState.Card,
    onDismiss: (Article) -> Unit,
    onReadArticle: (Article) -> Unit,
    onSave: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ArticleCard(
            state = state,
            onDismiss = onDismiss,
            onReadArticle = onReadArticle,
            onSave = onSave,
            onMarkRead = onMarkRead,
        )
        Labels.remainingChoices(state.remainingCount)?.let { sideNote ->
            Text(
                text = sideNote,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.muted,
            )
        }
    }
}
