package io.irodriguez.intentionalreading.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.ui.components.ArticleCard
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import kotlin.math.roundToInt

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
    onSwipeCommit: (Article, ArticleAction, (Boolean) -> Unit) -> Unit,
    reducedMotion: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val spacing = LocalIntentionalReadingSpacing.current
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
    val articleId = cardState?.article?.id
    var previousArticleId by remember(selectedCategory, state::class) {
        mutableStateOf(articleId)
    }
    var cardTopOffset by remember { mutableIntStateOf(0) }
    var cardBottomOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedCategory, state::class) {
        scrollState.scrollTo(0)
    }
    LaunchedEffect(articleId) {
        val articleChanged = previousArticleId != null &&
            articleId != null &&
            previousArticleId != articleId
        previousArticleId = articleId
        if (articleChanged) {
            withFrameNanos { }
            if (reducedMotion()) {
                scrollState.scrollTo(cardTopOffset)
            } else {
                scrollState.animateScrollTo(cardTopOffset)
            }
        }
    }
    var wasOpened by remember(articleId) {
        mutableStateOf(cardState?.isOpened == true)
    }
    LaunchedEffect(articleId, cardState?.isOpened) {
        val isOpened = cardState?.isOpened == true
        val becameOpened = !wasOpened && isOpened
        wasOpened = isOpened
        if (becameOpened) {
            withFrameNanos { }
            val target = DiscoverScrollTargets.revealCardActions(
                cardBottomOffset = cardBottomOffset,
                viewportHeight = scrollState.viewportSize,
                maxValue = scrollState.maxValue,
            )
            if (reducedMotion()) {
                scrollState.scrollTo(target)
            } else {
                scrollState.animateScrollTo(target)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(spacing.mobileMargin),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        DiscoverMasthead(modifier = Modifier.testTag(DiscoverLayoutTags.MASTHEAD))

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
                onSwipeCommit = onSwipeCommit,
                reducedMotion = reducedMotion,
                modifier = Modifier
                    .testTag(DiscoverLayoutTags.CARD)
                    .onGloballyPositioned { coordinates ->
                        cardTopOffset = coordinates.positionInParent().y.roundToInt()
                        cardBottomOffset =
                            (coordinates.positionInParent().y + coordinates.size.height).roundToInt()
                    },
            )
        }

        DiscoverOperationalBar(
            availableCount = cardState?.availableCount,
            contentFreshness = state.contentFreshness,
            failedRefreshDisclosure = state.failedRefreshDisclosure,
            degraded = degraded,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            actionLabel = refreshActionLabel,
            onAction = onRefreshAction,
            modifier = Modifier.testTag(DiscoverLayoutTags.OPERATIONAL_BLOCK),
        )
    }
}

internal object DiscoverLayoutTags {
    const val MASTHEAD = "discover-masthead"
    const val CARD = "discover-card"
    const val OPERATIONAL_BLOCK = "discover-operational-block"
}

@Composable
private fun LoadingPanel(state: DiscoverUiState.Loading) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.smallContainer,
        color = tokens.container,
        contentColor = tokens.fg,
    ) {
        Text(
            text = state.copy,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(spacing.gutter),
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
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.smallContainer,
        color = tokens.container,
        contentColor = tokens.fg,
    ) {
        Column(
            modifier = Modifier.padding(spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Text(text = copy, style = MaterialTheme.typography.bodyLarge, color = tokens.muted)
            OutlinedButton(
                onClick = { onAction?.invoke() },
                enabled = onAction != null,
                shape = shapes.filledPrimaryButton,
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
    onSwipeCommit: (Article, ArticleAction, (Boolean) -> Unit) -> Unit,
    reducedMotion: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val spacing = LocalIntentionalReadingSpacing.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
    ) {
        ArticleCard(
            state = state,
            onDismiss = onDismiss,
            onReadArticle = onReadArticle,
            onSave = onSave,
            onMarkRead = onMarkRead,
            onSwipeCommit = onSwipeCommit,
            reducedMotion = reducedMotion,
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
