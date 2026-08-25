package io.irodriguez.intentionalreading.data.remote

fun interface DatasetFetcher {
    fun fetch(etag: String?): DatasetFetchResult
}
