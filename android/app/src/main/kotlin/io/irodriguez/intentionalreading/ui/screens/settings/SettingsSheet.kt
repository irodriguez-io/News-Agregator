package io.irodriguez.intentionalreading.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.ui.components.ImportConfirmation
import io.irodriguez.intentionalreading.ui.components.LiveStatusMessage
import io.irodriguez.intentionalreading.ui.components.ResetConfirmation
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    appearance: Appearance,
    resetInProgress: Boolean,
    statusMessage: String?,
    generatedAtLabel: String,
    lastRefreshOutcome: String,
    importFileName: String?,
    importInProgress: Boolean,
    importTooLarge: Boolean,
    importUnreadable: Boolean,
    onAppearanceSelected: (Appearance) -> Unit,
    onExport: () -> Unit,
    onSelectImport: () -> Unit,
    onCancelImport: () -> Unit,
    onConfirmImport: () -> Unit,
    onReset: (onComplete: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalIntentionalReadingTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    var resetConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = tokens.surface,
        contentColor = tokens.fg,
        scrimColor = tokens.backdrop,
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .verticalScroll(rememberScrollState())
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    }
                    .padding(
                        start = 20.dp,
                        top = 24.dp,
                        end = 20.dp,
                        bottom = if (statusMessage != null) 112.dp else 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_eyebrow).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.muted,
                    )
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.displayLarge,
                        color = tokens.fg,
                    )
                }
                val closeDescription = stringResource(R.string.close_settings)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = closeDescription },
                ) {
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.content_status),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.fg,
                )
                Text(
                    text = generatedAtLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.muted,
                )
                Text(
                    text = lastRefreshOutcome,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.muted,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.fg,
                )
                Text(
                    text = stringResource(R.string.appearance_copy),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.muted,
                )
                Column(modifier = Modifier.selectableGroup()) {
                    Appearance.entries.forEach { option ->
                        val label = when (option) {
                            Appearance.LIGHT -> stringResource(R.string.appearance_light)
                            Appearance.DARK -> stringResource(R.string.appearance_dark)
                            Appearance.SYSTEM -> stringResource(R.string.appearance_system)
                        }
                        val selected = option == appearance
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onAppearanceSelected(option) },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.fg,
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.local_data),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.fg,
                )
                Text(
                    text = stringResource(R.string.local_data_copy),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.muted,
                )
                if (resetConfirmationVisible) {
                    ResetConfirmation(
                        resetInProgress = resetInProgress,
                        onCancel = { resetConfirmationVisible = false },
                        onConfirm = {
                            onReset { succeeded ->
                                if (succeeded) resetConfirmationVisible = false
                            }
                        },
                    )
                } else {
                    OutlinedButton(
                        onClick = { resetConfirmationVisible = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = tokens.surface.copy(alpha = 0f),
                            contentColor = tokens.fg,
                        ),
                        border = BorderStroke(1.dp, tokens.strongBorder),
                    ) {
                        Text(stringResource(R.string.reset_all_data))
                    }
                }
                Text(
                    text = stringResource(R.string.local_data_import_copy),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.muted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onExport,
                        enabled = !importInProgress,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = tokens.surface.copy(alpha = 0f),
                            contentColor = tokens.fg,
                        ),
                        border = BorderStroke(1.dp, tokens.strongBorder),
                    ) {
                        Text(stringResource(R.string.export_local_data))
                    }
                    OutlinedButton(
                        onClick = onSelectImport,
                        enabled = !importInProgress,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = tokens.surface.copy(alpha = 0f),
                            contentColor = tokens.fg,
                        ),
                        border = BorderStroke(1.dp, tokens.strongBorder),
                    ) {
                        Text(stringResource(R.string.import_local_data))
                    }
                }
                if (importTooLarge) {
                    LiveStatusMessage(
                        message = stringResource(R.string.local_data_import_too_large),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (importUnreadable) {
                    LiveStatusMessage(
                        message = stringResource(R.string.local_data_import_unreadable),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (importFileName != null) {
                    ImportConfirmation(
                        fileName = importFileName,
                        importInProgress = importInProgress,
                        onCancel = onCancelImport,
                        onConfirm = onConfirmImport,
                    )
                }
            }

            }

            if (statusMessage != null) {
                LiveStatusMessage(
                    message = statusMessage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                )
            }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
