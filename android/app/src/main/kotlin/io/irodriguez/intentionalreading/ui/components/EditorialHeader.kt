package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

@Composable
fun EditorialHeader(
    availableCount: Int?,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLabel = selectedCategory?.let { category ->
        io.irodriguez.intentionalreading.ui.format.Labels.categoryLabel(category.id)
    } ?: io.irodriguez.intentionalreading.ui.format.Labels.categoryLabel("all")
    EditorialHeader(
        eyebrow = stringResource(R.string.discover_eyebrow),
        title = stringResource(R.string.discover),
        description = stringResource(R.string.discover_description),
        modifier = modifier,
    ) {
        if (availableCount != null) {
            Text(
                text = stringResource(R.string.discover_context, availableCount, selectedLabel),
                style = MaterialTheme.typography.labelMedium,
                color = LocalIntentionalReadingTokens.current.quietInk,
            )
        }
        CategoryChipRow(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EditorialHeader(
    eyebrow: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
) {
    val tokens = LocalIntentionalReadingTokens.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = eyebrow.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.muted,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = tokens.fg,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.muted,
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(
                onClick = onAction,
                border = BorderStroke(1.dp, tokens.strongBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.fg),
            ) {
                Text(actionLabel)
            }
        }
        supportingContent()
    }
}
