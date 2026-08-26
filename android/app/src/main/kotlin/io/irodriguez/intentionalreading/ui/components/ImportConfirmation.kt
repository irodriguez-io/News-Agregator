package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens

@Composable
fun ImportConfirmation(
    fileName: String,
    importInProgress: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val cancelFocusRequester = remember { FocusRequester() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tokens.surfaceHover,
        contentColor = tokens.fg,
        border = BorderStroke(1.dp, tokens.border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.import_confirmation, fileName),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.fg,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onCancel,
                    enabled = !importInProgress,
                    modifier = Modifier.focusRequester(cancelFocusRequester),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = tokens.quietInk,
                        disabledContentColor = tokens.muted,
                    ),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                OutlinedButton(
                    onClick = onConfirm,
                    enabled = !importInProgress,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (importInProgress) tokens.border else tokens.strongBorder,
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = tokens.surface.copy(alpha = 0f),
                        contentColor = tokens.fg,
                        disabledContainerColor = tokens.surface.copy(alpha = 0f),
                        disabledContentColor = tokens.muted,
                    ),
                ) {
                    Text(stringResource(R.string.replace_local_data))
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }
}
