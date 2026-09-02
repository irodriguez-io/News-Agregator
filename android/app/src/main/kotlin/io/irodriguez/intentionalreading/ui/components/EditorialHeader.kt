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
import androidx.compose.ui.unit.Dp
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingSpacing
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

/** §73.1 — the existing outlined action boundary width, named instead of inlined. */
internal val EditorialHeaderOutlineWidth = Dp(1f)

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
    val spacing = LocalIntentionalReadingSpacing.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.stackGap),
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
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.muted,
        )
        if (actionLabel != null) {
            OutlinedButton(
                onClick = { onAction?.invoke() },
                enabled = onAction != null,
                border = BorderStroke(EditorialHeaderOutlineWidth, tokens.strongBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.fg),
            ) {
                Text(actionLabel)
            }
        }
        supportingContent()
    }
}
