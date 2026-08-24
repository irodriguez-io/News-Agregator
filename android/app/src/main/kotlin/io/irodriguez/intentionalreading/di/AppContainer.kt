package io.irodriguez.intentionalreading.di

import android.content.Context
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.data.LocalStateRepository
import io.irodriguez.intentionalreading.data.local.AssetDatasetSource
import io.irodriguez.intentionalreading.data.local.state.LocalStateStore

class AppContainer(context: Context) {
    val datasetRepository = DatasetRepository(
        source = AssetDatasetSource(context.assets),
    )
    val localStateRepository = LocalStateRepository(
        store = LocalStateStore(context.filesDir),
    )
}
