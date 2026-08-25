package io.irodriguez.intentionalreading.di

import android.content.Context
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.data.LocalStateRepository
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCache
import io.irodriguez.intentionalreading.data.local.state.LocalStateStore
import io.irodriguez.intentionalreading.data.remote.HttpDatasetFetcher

class AppContainer(context: Context) {
    val datasetRepository = DatasetRepository(
        fetcher = HttpDatasetFetcher(),
        cache = DatasetCache(context.filesDir),
    )
    val localStateRepository = LocalStateRepository(
        store = LocalStateStore(context.filesDir),
    )
}
