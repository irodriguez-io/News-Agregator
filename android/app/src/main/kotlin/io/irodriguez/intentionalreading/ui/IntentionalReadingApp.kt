package io.irodriguez.intentionalreading.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.ui.components.BottomNavigationBar
import io.irodriguez.intentionalreading.ui.screens.discover.DiscoverScreen
import io.irodriguez.intentionalreading.ui.screens.history.HistoryScreen
import io.irodriguez.intentionalreading.ui.screens.readlater.ReadLaterScreen
import io.irodriguez.intentionalreading.ui.screens.settings.SettingsSheet
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTheme
import io.irodriguez.intentionalreading.ui.theme.LocalIntentionalReadingTokens
import java.util.Locale

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
    val applicationContext = LocalContext.current.applicationContext

    IntentionalReadingTheme(appearance = appearance) {
        val tokens = LocalIntentionalReadingTokens.current
        BackHandler(enabled = !settingsOpen && destination != Destination.DISCOVER) {
            viewModel.selectDestination(Destination.DISCOVER)
        }

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
            when (destination) {
                Destination.READ_LATER -> ReadLaterScreen(
                    state = uiState.readLater,
                    onDiscover = { viewModel.selectDestination(Destination.DISCOVER) },
                    onReadArticle = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.OPEN) { result ->
                            if (result.allowNavigation) openPublisher(applicationContext, article.url)
                        }
                    },
                    onMarkRead = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.MARK_READ)
                    },
                    onRemove = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.REMOVE)
                    },
                    modifier = Modifier.padding(innerPadding),
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
                        viewModel.launchArticleAction(article, ArticleAction.DISMISS)
                    },
                    onReadArticle = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.OPEN) { result ->
                            if (result.allowNavigation) openPublisher(applicationContext, article.url)
                        }
                    },
                    onSave = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.SAVE)
                    },
                    onMarkRead = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.MARK_READ)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
                Destination.HISTORY -> HistoryScreen(
                    state = uiState.history,
                    onReadLater = { viewModel.selectDestination(Destination.READ_LATER) },
                    onDiscover = { viewModel.selectDestination(Destination.DISCOVER) },
                    onReopen = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.OPEN) { result ->
                            if (result.allowNavigation) openPublisher(applicationContext, article.url)
                        }
                    },
                    onMarkUnread = { article ->
                        viewModel.launchArticleAction(article, ArticleAction.MARK_UNREAD)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        if (settingsOpen) {
            SettingsSheet(
                appearance = appearance,
                onAppearanceSelected = { selectedAppearance ->
                    viewModel.launchAppearanceChange(selectedAppearance)
                },
                onDismiss = viewModel::closeSettings,
            )
        }
    }
}

private fun openPublisher(context: Context, rawUrl: String) {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return

    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // The Open transition is retained even when no system browser is available.
    } catch (_: SecurityException) {
        // Fail quietly; the dataset URL already passed the frozen HTTP(S) validator.
    }
}
