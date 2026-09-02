package io.irodriguez.intentionalreading.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.format.RelativeTime
import io.irodriguez.intentionalreading.ui.gesture.SwipeGesture
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverUiState
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun ArticleCard(
    state: DiscoverUiState.Card,
    onDismiss: (Article) -> Unit,
    onReadArticle: (Article) -> Unit,
    onSave: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
    onSwipeCommit: (Article, ArticleAction, (Boolean) -> Unit) -> Unit,
    reducedMotion: () -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    val article = state.article
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val thresholdPx = with(density) { SwipeGesture.THRESHOLD_DP.dp.toPx() }
    val intentSlopPx = with(density) { SwipeGesture.INTENT_SLOP_DP.dp.toPx() }
    val viewportWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val exitMinimumPx = with(density) { SwipeGesture.EXIT_MINIMUM_DP.dp.toPx() }
    val reducedMotionEnabled = reducedMotion()
    val gestureValues = remember(
        article.id,
        thresholdPx,
        intentSlopPx,
        viewportWidthPx,
        exitMinimumPx,
        reducedMotionEnabled,
    ) {
        ArticleGestureValues(
            article = article,
            gestureState = SwipeGesture.State(
                thresholdPx = thresholdPx,
                intentSlopPx = intentSlopPx,
                viewportWidthPx = viewportWidthPx,
                exitMinimumPx = exitMinimumPx,
                reducedMotion = reducedMotionEnabled,
            ),
            translationX = Animatable(0f),
            rotationDegrees = Animatable(0f),
            swipeCue = mutableStateOf<SwipeGesture.Action?>(null),
            motionSpec = tween(
                durationMillis = if (reducedMotionEnabled) {
                    0
                } else {
                    SwipeGesture.EXIT_DURATION_MS
                },
                easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f),
            ),
        )
    }
    val restoreScope = rememberCoroutineScope()
    val currentOnSwipeCommit by rememberUpdatedState(onSwipeCommit)
    val currentGestureValues by rememberUpdatedState(gestureValues)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gesture = currentGestureValues
                    if (!gesture.gestureState.down(down.position.x, down.position.y)) {
                        return@awaitEachGesture
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            restoreScope.launch { gesture.restoreCard() }
                            break
                        }

                        if (!change.pressed) {
                            val action = gesture.gestureState.release()
                            gesture.swipeCue.value = null
                            if (action == null) {
                                restoreScope.launch { gesture.animateToGestureState() }
                            } else {
                                val articleAction = when (action) {
                                    SwipeGesture.Action.DISMISS -> ArticleAction.DISMISS
                                    SwipeGesture.Action.SAVE -> ArticleAction.SAVE
                                }
                                restoreScope.launch {
                                    gesture.animateToGestureState()
                                    currentOnSwipeCommit(gesture.article, articleAction) { persisted ->
                                        gesture.gestureState.releaseCommitLock()
                                        if (!persisted) {
                                            restoreScope.launch { gesture.restoreCard() }
                                        }
                                    }
                                }
                            }
                            break
                        }

                        val shouldConsume = gesture.gestureState.move(
                            x = change.position.x,
                            y = change.position.y,
                        )
                        if (
                            shouldConsume &&
                            gesture.gestureState.intent == SwipeGesture.Intent.HORIZONTAL
                        ) {
                            change.consume()
                            gesture.swipeCue.value = if (gesture.gestureState.translationX < 0f) {
                                SwipeGesture.Action.DISMISS
                            } else {
                                SwipeGesture.Action.SAVE
                            }
                            restoreScope.launch { gesture.snapToGestureState() }
                        }
                    }
                }
            }
            .graphicsLayer {
                this.translationX = gestureValues.translationX.value
                rotationZ = gestureValues.rotationDegrees.value
            }
            .shadow(
                elevation = DeckCardShadowElevation,
                shape = shapes.primaryCard,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.surfaceTint,
                spotColor = MaterialTheme.colorScheme.surfaceTint,
            ),
        shape = shapes.primaryCard,
        color = tokens.card,
        contentColor = tokens.fg,
    ) {
        Box {
            Column(
                modifier = Modifier.padding(
                    horizontal = spacing.mobileMargin,
                    vertical = spacing.tabletMargin,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
            ) {
                ArticleMetadata(article = article, publicationAge = state.publicationAge)

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.fg,
                    maxLines = DiscoverHeadlineMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )

                if (article.excerpt.isNotEmpty()) {
                    Text(
                        text = article.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.quiet,
                        maxLines = DiscoverDescriptionMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (article.tags.isNotEmpty()) {
                    TopicTags(article)
                }

                if (state.isOpened) {
                    OpenedAcknowledgment()
                }

                ArticleActions(
                    article = article,
                    isOpened = state.isOpened,
                    onDismiss = onDismiss,
                    onReadArticle = onReadArticle,
                    onSave = onSave,
                    onMarkRead = onMarkRead,
                )
            }

            gestureValues.swipeCue.value?.let { action ->
                SwipeCue(
                    action = action,
                    modifier = Modifier
                        .align(
                            if (action == SwipeGesture.Action.DISMISS) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            },
                        )
                        .padding(16.dp)
                        .clearAndSetSemantics {},
                )
            }
        }
    }
}

private class ArticleGestureValues(
    val article: Article,
    val gestureState: SwipeGesture.State,
    val translationX: Animatable<Float, AnimationVector1D>,
    val rotationDegrees: Animatable<Float, AnimationVector1D>,
    val swipeCue: MutableState<SwipeGesture.Action?>,
    val motionSpec: AnimationSpec<Float>,
)

private suspend fun ArticleGestureValues.snapToGestureState() {
    translationX.snapTo(gestureState.translationX)
    rotationDegrees.snapTo(gestureState.rotationDegrees)
}

private suspend fun ArticleGestureValues.animateToGestureState() {
    coroutineScope {
        launch { translationX.animateTo(gestureState.translationX, motionSpec) }
        launch { rotationDegrees.animateTo(gestureState.rotationDegrees, motionSpec) }
    }
}

private suspend fun ArticleGestureValues.restoreCard() {
    gestureState.restore()
    swipeCue.value = null
    animateToGestureState()
}

@Composable
private fun SwipeCue(
    action: SwipeGesture.Action,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tokens.surfaceHover,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.strongBorder),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (action == SwipeGesture.Action.DISMISS) Text("←")
            Text(
                stringResource(
                    if (action == SwipeGesture.Action.DISMISS) {
                        R.string.not_interested
                    } else {
                        R.string.save_for_later
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
            )
            if (action == SwipeGesture.Action.SAVE) Text("→")
        }
    }
}

@Composable
private fun ArticleMetadata(article: Article, publicationAge: String) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    val readingTime = RelativeTime.readingTime(article.readingTimeMinutes)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.baseUnit),
        verticalArrangement = Arrangement.spacedBy(spacing.baseUnit),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataText(article.source.name)
        if (publicationAge.isNotEmpty()) MetadataText(publicationAge)
        Surface(
            shape = shapes.badge,
            color = tokens.primarySoft,
            contentColor = tokens.primary,
        ) {
            Text(
                text = article.contentType.label.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(
                    horizontal = spacing.stackGap,
                    vertical = spacing.baseUnit,
                ),
            )
        }
        MetadataText(Labels.categoryLabel(article.category.id))
        if (readingTime.isNotEmpty()) MetadataText(readingTime)
    }
}

@Composable
private fun MetadataText(text: String) {
    val tokens = LocalIntentionalReadingTokens.current
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium,
        color = tokens.muted,
    )
}

@Composable
private fun TopicTags(article: Article) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    val topicsDescription = stringResource(R.string.article_topics)
    FlowRow(
        modifier = Modifier.semantics {
            contentDescription = "$topicsDescription: ${article.tags.take(5).joinToString { it.label }}"
        },
        horizontalArrangement = Arrangement.spacedBy(spacing.baseUnit),
        verticalArrangement = Arrangement.spacedBy(spacing.baseUnit),
    ) {
        article.tags.take(5).forEach { tag ->
            CompositionLocalProvider(LocalContentColor provides tokens.muted) {
                Text(
                    text = tag.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .border(
                            TopicTagOutlineWidth,
                            tokens.outlineVariant,
                            shapes.pill,
                        )
                        .padding(
                            horizontal = spacing.stackGap,
                            vertical = spacing.baseUnit,
                        ),
                )
            }
        }
    }
}

// docs/v1/06-ui-ux.md §13.2 — the deck card is bounded independently of its content.
private const val DiscoverHeadlineMaxLines = 3
private const val DiscoverDescriptionMaxLines = 2

// docs/v1/06-ui-ux.md §16.2 — preserve the existing elevation while tinting its ambient shadow.
private val DeckCardShadowElevation = Dp(8f)

// docs/v1/06-ui-ux.md §27.2 — a tag carries a decorative hairline, not a control boundary.
private val TopicTagOutlineWidth = Dp(1f)

@Composable
private fun OpenedAcknowledgment() {
    val tokens = LocalIntentionalReadingTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = tokens.surfaceHover,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.opened).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium,
                color = tokens.fg,
            )
            Text(
                text = stringResource(R.string.opened_acknowledgment),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.muted,
            )
        }
    }
}

@Composable
private fun ArticleActions(
    article: Article,
    isOpened: Boolean,
    onDismiss: (Article) -> Unit,
    onReadArticle: (Article) -> Unit,
    onSave: (Article) -> Unit,
    onMarkRead: (Article) -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val externalDescription = stringResource(R.string.read_article_external)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RoundTriageAction(
                arrow = "←",
                label = stringResource(R.string.not_interested),
                onClick = { onDismiss(article) },
            )
            Button(
                onClick = { onReadArticle(article) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = externalDescription },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.surface,
                ),
            ) {
                Text(stringResource(R.string.read_article))
                Spacer(Modifier.width(6.dp))
                Text("↗")
            }
            RoundTriageAction(
                arrow = "→",
                label = stringResource(R.string.save_for_later),
                onClick = { onSave(article) },
            )
        }
        if (isOpened) {
            OutlinedButton(
                onClick = { onMarkRead(article) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, tokens.strongBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.fg),
            ) {
                Text(stringResource(R.string.mark_read))
            }
        }
    }
}

@Composable
private fun RoundTriageAction(
    arrow: String,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .semantics { contentDescription = label },
            border = BorderStroke(1.dp, tokens.strongBorder),
        ) {
            Text(
                text = arrow,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.fg,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = tokens.fg,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
