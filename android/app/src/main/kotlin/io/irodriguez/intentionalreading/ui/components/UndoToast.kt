package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun UndoToast(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        color = tokens.toastSurface,
        contentColor = tokens.toastInk,
        border = BorderStroke(1.dp, tokens.strongBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.toastInk,
            )
            TextButton(
                onClick = onUndo,
                colors = ButtonDefaults.textButtonColors(contentColor = tokens.toastInk),
            ) {
                Text(stringResource(R.string.undo_action))
            }
        }
    }
}
