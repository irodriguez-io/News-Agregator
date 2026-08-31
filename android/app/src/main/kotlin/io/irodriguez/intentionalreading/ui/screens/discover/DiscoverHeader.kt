package io.irodriguez.intentionalreading.ui.screens.discover

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.ui.components.CategoryChipRow
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

@Composable
fun DiscoverMasthead(modifier: Modifier = Modifier) {
    val tokens = LocalIntentionalReadingTokens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.discover_eyebrow).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.muted,
        )
        Text(
            text = stringResource(R.string.discover),
            style = MaterialTheme.typography.displayLarge,
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
    val selectedLabel = selectedCategory?.let { category ->
        Labels.categoryLabel(category.id)
    } ?: Labels.categoryLabel("all")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                border = BorderStroke(1.dp, tokens.strongBorder),
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
