package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

@Composable
fun EditorialHeader(
    availableCount: Int?,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val selectedLabel = selectedCategory?.let { category ->
        io.irodriguez.intentionalreading.ui.format.Labels.categoryLabel(category.id)
    } ?: io.irodriguez.intentionalreading.ui.format.Labels.categoryLabel("all")
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
        Text(
            text = stringResource(R.string.discover_description),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.muted,
        )
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
