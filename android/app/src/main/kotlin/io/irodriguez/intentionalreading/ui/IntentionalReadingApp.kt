package io.irodriguez.intentionalreading.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.irodriguez.intentionalreading.IntentionalReadingApplication
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.data.readBounded
import io.irodriguez.intentionalreading.data.local.state.LocalStateStore
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.ui.components.BottomNavigationBar
import io.irodriguez.intentionalreading.ui.components.LiveStatusMessage
import io.irodriguez.intentionalreading.ui.components.LocalStateRecoveryNotice
import io.irodriguez.intentionalreading.ui.components.UndoToast
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.history.HistoryScreen
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterScreen
import io.irodriguez.intentionalreading.ui.screens.settings.SettingsSheet
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionalReadingApp(viewModel: AppViewModel) {
    val localStateReady by viewModel.localStateReady.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    if (!localStateReady) return

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val settingsOpen by viewModel.settingsOpen.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val localStateError by viewModel.localStateError.collectAsStateWithLifecycle()
    val recoveryNoticeVisible by viewModel.recoveryNoticeVisible.collectAsStateWithLifecycle()
    val announcement by viewModel.announcement.collectAsStateWithLifecycle()
    val resetInProgress by viewModel.resetInProgress.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val applicationContext = LocalContext.current.applicationContext
    val reducedMotion = (applicationContext as? IntentionalReadingApplication)
        ?.container
        ?.reducedMotion
        ?: { false }
    val pendingUndoOffer = uiState.pendingUndoOffer
    val importScope = rememberCoroutineScope()
    var selectedImport by remember { mutableStateOf<SelectedImport?>(null) }
    var importNotice by remember { mutableStateOf<ImportNotice?>(null) }
    var importInProgress by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.launchExportLocalData(
                writeBytes = { bytes ->
                    withContext(Dispatchers.IO) {
                        applicationContext.contentResolver
                            .openOutputStream(uri, "wt")
                            ?.use { output ->
                                output.write(bytes)
                                output.flush()
                                true
                            }
                            ?: false
                    }
                },
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importScope.launch {
                val document = withContext(Dispatchers.IO) {
                    describeDocument(applicationContext, uri)
                }
                importNotice = null
                if (
                    document.reportedSize != null &&
                    document.reportedSize > LocalStateStore.MAX_IMPORT_BYTES
                ) {
                    selectedImport = null
                    importNotice = ImportNotice.TOO_LARGE
                } else {
                    selectedImport = SelectedImport(uri, document.fileName)
                }
            }
        }
    }
    val onOpenArticle: (Article) -> Unit = { article ->
        viewModel.launchArticleAction(article, ArticleAction.OPEN) { result ->
            if (result.allowNavigation) {
                val navigationOpened = openPublisher(applicationContext, article.url)
                viewModel.reportOpenResult(result, navigationOpened)
            }
        }
    }

    LaunchedEffect(announcement?.id, lifecycleOwner) {
        val current = announcement ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(6_000)
            viewModel.acknowledgeAnnouncement(current.id)
        }
    }

    LaunchedEffect(pendingUndoOffer?.id, lifecycleOwner) {
        val current = pendingUndoOffer ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(4_500)
            viewModel.acknowledgeUndoOffer(current.id)
        }
    }

    IntentionalReadingTheme(appearance = appearance) {
        val tokens = LocalIntentionalReadingTokens.current
        val announcementText = announcement?.let { event ->
            stringResource(
                when (event.kind) {
                    AppAnnouncementKind.PERSISTENCE_FAILED -> R.string.local_state_write_failure
                    AppAnnouncementKind.OPEN_NOT_PERSISTED -> R.string.open_not_persisted
                    AppAnnouncementKind.OPEN_NAVIGATION_FAILED -> R.string.open_navigation_failed
                    AppAnnouncementKind.RESET_FAILED -> R.string.reset_failed
                    AppAnnouncementKind.RESET_COMPLETE -> R.string.reset_complete
                    AppAnnouncementKind.REFRESH_UPDATED -> R.string.refresh_updated
                    AppAnnouncementKind.REFRESH_CURRENT -> R.string.refresh_current
                    AppAnnouncementKind.REFRESH_FAILED -> R.string.refresh_failed
                    AppAnnouncementKind.UNDO_COMPLETED -> R.string.undo_completed
                    AppAnnouncementKind.UNDO_FAILED -> R.string.undo_failed
                    AppAnnouncementKind.EXPORT_COMPLETE -> R.string.local_data_export_prepared
                    AppAnnouncementKind.EXPORT_FAILED -> R.string.local_data_export_failed
                    AppAnnouncementKind.IMPORT_COMPLETE -> R.string.local_data_imported
                    AppAnnouncementKind.IMPORT_FAILED -> R.string.local_data_import_failed
                },
            )
        }
        val undoToastMessage = pendingUndoOffer?.let { offer ->
            stringResource(
                when (offer.message) {
                    PendingUndoMessage.SAVED -> R.string.undo_toast_saved
                    PendingUndoMessage.DISMISSED -> R.string.undo_toast_dismissed
                },
            )
        }
        BackHandler(enabled = !settingsOpen && destination != Destination.DISCOVER) {
            viewModel.selectDestination(Destination.DISCOVER)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = tokens.bg,
                contentColor = tokens.fg,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = viewModel::toggleSettings,
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(BorderStroke(1.dp, tokens.border), CircleShape),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings),
                                    contentDescription = stringResource(R.string.settings),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = tokens.bg,
                            titleContentColor = tokens.fg,
                            actionIconContentColor = tokens.fg,
                        ),
                    )
                },
                bottomBar = {
                    BottomNavigationBar(
                        destination = destination,
                        counts = uiState.navigationCounts,
                        onDestinationSelected = viewModel::selectDestination,
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (recoveryNoticeVisible && localStateError != null) {
                        LocalStateRecoveryNotice(
                            onOpenSettings = viewModel::openSettings,
                            onDismiss = viewModel::dismissRecoveryNotice,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        when (destination) {
                            Destination.READ_LATER -> ReadLaterScreen(
                                state = uiState.readLater,
                                onDiscover = { viewModel.selectDestination(Destination.DISCOVER) },
                                onReadArticle = onOpenArticle,
                                onMarkRead = { article ->
                                    viewModel.launchArticleAction(article, ArticleAction.MARK_READ)
                                },
                                onRemove = { article ->
                                    viewModel.launchArticleAction(article, ArticleAction.REMOVE)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            Destination.DISCOVER -> DiscoverScreen(
                                state = uiState.discover,
                                degraded = uiState.degraded,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { category ->
                                    viewModel.launchCategorySelection(category)
                                },
                                onRetry = viewModel::reload,
                                onViewReadLater = { viewModel.selectDestination(Destination.READ_LATER) },
                                onDismiss = { article ->
                                    viewModel.launchArticleAction(
                                        article,
                                        ArticleAction.DISMISS,
                                        expectDiscoverHead = true,
                                    )
                                },
                                onReadArticle = onOpenArticle,
                                onSave = { article ->
                                    viewModel.launchArticleAction(
                                        article,
                                        ArticleAction.SAVE,
                                        expectDiscoverHead = true,
                                    )
                                },
                                onMarkRead = { article ->
                                    viewModel.launchArticleAction(
                                        article,
                                        ArticleAction.MARK_READ,
                                        expectDiscoverHead = true,
                                    )
                                },
                                onSwipeCommit = { article, action, onComplete ->
                                    viewModel.launchArticleAction(
                                        article = article,
                                        action = action,
                                        undoable = true,
                                        expectDiscoverHead = true,
                                    ) { result ->
                                        onComplete(result.persisted)
                                    }
                                },
                                reducedMotion = reducedMotion,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Destination.HISTORY -> HistoryScreen(
                                state = uiState.history,
                                onReadLater = { viewModel.selectDestination(Destination.READ_LATER) },
                                onDiscover = { viewModel.selectDestination(Destination.DISCOVER) },
                                onReopen = onOpenArticle,
                                onMarkUnread = { article ->
                                    viewModel.launchArticleAction(article, ArticleAction.MARK_UNREAD)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            if (!settingsOpen) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (undoToastMessage != null) {
                        UndoToast(
                            message = undoToastMessage,
                            onUndo = viewModel::launchUndo,
                        )
                    }
                    if (announcementText != null) {
                        LiveStatusMessage(message = announcementText)
                    }
                }
            }
        }

        if (settingsOpen) {
            SettingsSheet(
                appearance = appearance,
                resetInProgress = resetInProgress,
                statusMessage = announcementText,
                generatedAtLabel = uiState.generatedAtLabel,
                lastRefreshOutcome = uiState.lastRefreshOutcome,
                importFileName = selectedImport?.fileName,
                importInProgress = importInProgress,
                importTooLarge = importNotice == ImportNotice.TOO_LARGE,
                importUnreadable = importNotice == ImportNotice.UNREADABLE,
                onAppearanceSelected = { selectedAppearance ->
                    viewModel.launchAppearanceChange(selectedAppearance)
                },
                onExport = {
                    exportLauncher.launch(viewModel.backupFilename())
                },
                onSelectImport = {
                    importLauncher.launch(arrayOf("application/json"))
                },
                onCancelImport = {
                    selectedImport = null
                    importNotice = null
                },
                onConfirmImport = {
                    val selected = selectedImport
                    if (selected != null) {
                        importInProgress = true
                        importNotice = null
                        importScope.launch {
                            val readResult = try {
                                withContext(Dispatchers.IO) {
                                    readImportCandidate(applicationContext, selected.uri)
                                }
                            } catch (cancellation: CancellationException) {
                                importInProgress = false
                                throw cancellation
                            } catch (_: Exception) {
                                ImportReadResult.Unreadable
                            }
                            when (readResult) {
                                is ImportReadResult.Success -> {
                                    viewModel.launchImportLocalData(readResult.bytes) {
                                        importInProgress = false
                                        selectedImport = null
                                    }
                                }
                                ImportReadResult.TooLarge -> {
                                    importInProgress = false
                                    selectedImport = null
                                    importNotice = ImportNotice.TOO_LARGE
                                }
                                ImportReadResult.Unreadable -> {
                                    importInProgress = false
                                    selectedImport = null
                                    importNotice = ImportNotice.UNREADABLE
                                }
                            }
                        }
                    }
                },
                onReset = { onComplete ->
                    viewModel.launchResetLocalData { result ->
                        onComplete(result is LocalStateResult.Success)
                    }
                },
                onDismiss = viewModel::closeSettings,
            )
        }
    }
}

private data class SelectedImport(
    val uri: Uri,
    val fileName: String,
)

private data class SelectedDocument(
    val fileName: String,
    val reportedSize: Long?,
)

private enum class ImportNotice {
    TOO_LARGE,
    UNREADABLE,
}

private sealed interface ImportReadResult {
    data class Success(val bytes: ByteArray) : ImportReadResult
    data object TooLarge : ImportReadResult
    data object Unreadable : ImportReadResult
}

private fun describeDocument(context: Context, uri: Uri): SelectedDocument {
    var displayName: String? = null
    var reportedSize: Long? = null
    try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) {
                    displayName = cursor.getString(nameColumn)
                }
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    reportedSize = cursor.getLong(sizeColumn).takeIf { it >= 0L }
                }
            }
        }
    } catch (_: Exception) {
        // Provider metadata is optional; the bounded stream read remains authoritative.
    }
    val fallbackName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }
        ?: uri.toString()
    return SelectedDocument(
        fileName = displayName?.takeIf { it.isNotBlank() } ?: fallbackName,
        reportedSize = reportedSize,
    )
}

private fun readImportCandidate(context: Context, uri: Uri): ImportReadResult {
    val input = context.contentResolver.openInputStream(uri)
        ?: return ImportReadResult.Unreadable
    val bytes = input.use {
        readBounded(it, LocalStateStore.MAX_IMPORT_BYTES)
    }
    return if (bytes == null) {
        ImportReadResult.TooLarge
    } else {
        ImportReadResult.Success(bytes)
    }
}

private fun openPublisher(context: Context, rawUrl: String): Boolean {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return false

    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
