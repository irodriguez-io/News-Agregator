package io.irodriguez.intentionalreading.di

import android.content.Context
import io.irodriguez.intentionalreading.data.DatasetRepository
import io.irodriguez.intentionalreading.data.local.AssetDatasetSource

class AppContainer(context: Context) {
    val datasetRepository = DatasetRepository(
        source = AssetDatasetSource(context.assets),
    )
}
