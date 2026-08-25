package io.irodriguez.intentionalreading.data.local

import android.content.res.AssetManager

/** Bundled-asset compatibility boundary retained until Slice 2 replaces its wiring. */
interface DatasetSource {
    fun read(): ByteArray
}

class AssetDatasetSource(
    private val assets: AssetManager,
) : DatasetSource {
    override fun read(): ByteArray = assets.open(ASSET_NAME).use { it.readBytes() }

    private companion object {
        const val ASSET_NAME = "sample_articles.json"
    }
}
