package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingShapes
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun EmptyStatePanel(
    title: String,
    copy: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val shapes = LocalIntentionalReadingShapes.current
    val spacing = LocalIntentionalReadingSpacing.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.primaryCard,
        color = tokens.container,
        contentColor = tokens.fg,
    ) {
        Column(
            modifier = Modifier.padding(spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Text(text = copy, style = MaterialTheme.typography.bodyLarge, color = tokens.muted)
            FilledPrimaryControl(
                onClick = onAction,
            ) {
                Text(actionLabel)
            }
        }
    }
}
