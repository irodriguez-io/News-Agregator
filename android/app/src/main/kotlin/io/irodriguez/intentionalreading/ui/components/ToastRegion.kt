package io.irodriguez.intentionalreading.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToastRegion(
    undoToastMessage: String?,
    announcementText: String?,
    onUndo: () -> Unit,
    reducedMotion: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (undoToastMessage != null) {
            UndoToast(message = undoToastMessage, onUndo = onUndo)
        }
        if (announcementText != null) {
            LiveStatusMessage(message = announcementText)
        }
    }
}
