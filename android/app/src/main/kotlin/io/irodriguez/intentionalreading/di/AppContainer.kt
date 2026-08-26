package io.irodriguez.intentionalreading.di

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.data.LocalStateRepository
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCache
import io.irodriguez.intentionalreading.data.local.state.LocalStateStore
import io.irodriguez.intentionalreading.data.remote.HttpDatasetFetcher
import io.irodriguez.intentionalreading.domain.model.Appearance

class AppContainer(context: Context) {
    val datasetRepository = DatasetRepository(
        fetcher = HttpDatasetFetcher(),
        cache = DatasetCache(context.filesDir),
    )
    val localStateRepository = LocalStateRepository(
        store = LocalStateStore(context.filesDir),
    )
    private val uiModeManager = context.getSystemService(UiModeManager::class.java)
    val applyNightMode: (Appearance) -> Unit =
        if (Build.VERSION.SDK_INT >= 31) {
            { appearance -> uiModeManager.setApplicationNightMode(modeFor(appearance)) }
        } else {
            { _ -> }
        }
}

internal fun modeFor(appearance: Appearance): Int = when (appearance) {
    Appearance.LIGHT -> UiModeManager.MODE_NIGHT_NO
    Appearance.DARK -> UiModeManager.MODE_NIGHT_YES
    Appearance.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
}
