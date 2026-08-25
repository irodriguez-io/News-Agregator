package io.irodriguez.intentionalreading.ui

import io.irodriguez.intentionalreading.data.DatasetRefreshResult
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead

internal class FakeDatasetRepository(
    initialCacheRead: DatasetCacheRead = DatasetCacheRead.Absent,
    initialRefreshResult: DatasetRefreshResult,
) {
    var cacheRead: DatasetCacheRead = initialCacheRead
    var refreshBehavior: suspend () -> DatasetRefreshResult = { initialRefreshResult }
    var cacheReadRequests = 0
    var refreshRequests = 0

    suspend fun readCache(): DatasetCacheRead {
        cacheReadRequests += 1
        return cacheRead
    }

    suspend fun refresh(): DatasetRefreshResult {
        refreshRequests += 1
        return refreshBehavior()
    }
}
