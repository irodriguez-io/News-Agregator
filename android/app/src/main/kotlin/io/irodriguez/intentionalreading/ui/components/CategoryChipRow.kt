package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun CategoryChipRow(
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val selectedId = selectedCategory?.id ?: "all"
    val selectedState = stringResource(R.string.discover_category_group)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Labels.categoryOptions, key = { it.id }) { option ->
            val selected = option.id == selectedId
            FilterChip(
                selected = selected,
                onClick = {
                    onCategorySelected(if (option.id == "all") null else Category.fromId(option.id))
                },
                label = { Text(option.label) },
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .semantics { stateDescription = if (selected) "$selectedState: ${option.label}" else option.label },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = tokens.surface,
                    labelColor = tokens.fg,
                    selectedContainerColor = tokens.fg,
                    selectedLabelColor = tokens.surface,
                    disabledContainerColor = tokens.surface,
                    disabledLabelColor = tokens.muted,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = tokens.border,
                    selectedBorderColor = tokens.fg,
                ),
            )
        }
    }
}
