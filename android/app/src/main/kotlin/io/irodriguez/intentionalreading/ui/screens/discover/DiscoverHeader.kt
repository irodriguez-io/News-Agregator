package io.irodriguez.intentionalreading.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.ui.components.CategoryChipRow
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

@Composable
fun DiscoverMasthead(modifier: Modifier = Modifier) {
    val tokens = LocalIntentionalReadingTokens.current
    val spacing = LocalIntentionalReadingSpacing.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.baseUnit),
    ) {
        Text(
            text = stringResource(R.string.discover_eyebrow).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.muted,
        )
        Text(
            text = stringResource(R.string.discover),
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.fg,
        )
    }
}

@Composable
fun DiscoverOperationalBar(
    availableCount: Int?,
    contentFreshness: String?,
    failedRefreshDisclosure: String?,
    degraded: Boolean,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    val selectedLabel = selectedCategory?.let { category ->
        Labels.categoryLabel(category.id)
    } ?: Labels.categoryLabel("all")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
    ) {
        Text(
            text = stringResource(R.string.discover_description),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.muted,
        )
        if (actionLabel != null) {
            OutlinedButton(
                onClick = { onAction?.invoke() },
                enabled = onAction != null,
                shape = shapes.filledPrimaryButton,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.fg),
            ) {
                Text(actionLabel)
            }
        }
        if (contentFreshness != null) {
            Text(
                text = contentFreshness,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.muted,
            )
        }
        if (failedRefreshDisclosure != null) {
            Text(
                text = failedRefreshDisclosure,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.muted,
            )
        }
        if (degraded) {
            Text(
                text = Labels.DEGRADED_NOTICE,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.muted,
            )
        }
        if (availableCount != null) {
            Text(
                text = stringResource(R.string.discover_context, availableCount, selectedLabel),
                style = MaterialTheme.typography.labelMedium,
                color = tokens.quietInk,
            )
        }
        CategoryChipRow(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
