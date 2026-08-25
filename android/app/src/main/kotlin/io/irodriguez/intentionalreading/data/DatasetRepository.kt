package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.dataset.DatasetCache
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheMetadata
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.data.remote.DatasetFetchResult
import io.irodriguez.intentionalreading.data.remote.DatasetFetcher
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DatasetRefreshErrorCode {
    FETCH,
    VALIDATION,
    CACHE_WRITE,
    NO_CACHED_DATASET,
}

sealed interface DatasetRefreshResult {
    data class Updated(
        val dataset: ArticleDataset,
        val metadata: DatasetCacheMetadata,
    ) : DatasetRefreshResult

    data class Current(
        val dataset: ArticleDataset,
        val metadata: DatasetCacheMetadata?,
    ) : DatasetRefreshResult

    data class Failed(
        val code: DatasetRefreshErrorCode,
        val cachedDataset: ArticleDataset? = null,
        val fetchFailure: DatasetFetchResult.Failure? = null,
        val validationFailure: DatasetResult.Failure? = null,
    ) : DatasetRefreshResult
}

class DatasetRepository(
    private val fetcher: DatasetFetcher,
    private val cache: DatasetCache,
    private val validator: DatasetValidator = DatasetValidator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun readCache(): DatasetCacheRead = withContext(ioDispatcher) {
        cache.read()
    }

    suspend fun refresh(): DatasetRefreshResult = withContext(ioDispatcher) {
        val cached = when (val read = cache.read()) {
            DatasetCacheRead.Absent -> null
            is DatasetCacheRead.Present -> read
        }

        when (val fetched = fetcher.fetch(cached?.metadata?.etag)) {
            DatasetFetchResult.NotModified -> if (cached == null) {
                DatasetRefreshResult.Failed(code = DatasetRefreshErrorCode.NO_CACHED_DATASET)
            } else {
                DatasetRefreshResult.Current(
                    dataset = cached.dataset,
                    metadata = cached.metadata,
                )
            }
            is DatasetFetchResult.Failure -> DatasetRefreshResult.Failed(
                code = DatasetRefreshErrorCode.FETCH,
                cachedDataset = cached?.dataset,
                fetchFailure = fetched,
            )
            is DatasetFetchResult.Body -> when (val validated = validator.validate(fetched.bytes)) {
                is DatasetResult.Failure -> DatasetRefreshResult.Failed(
                    code = DatasetRefreshErrorCode.VALIDATION,
                    cachedDataset = cached?.dataset,
                    validationFailure = validated,
                )
                is DatasetResult.Success -> {
                    val metadata = DatasetCacheMetadata(
                        etag = fetched.etag,
                        fetchedAt = clock.instant(),
                    )
                    if (!cache.write(fetched.bytes, metadata)) {
                        DatasetRefreshResult.Failed(
                            code = DatasetRefreshErrorCode.CACHE_WRITE,
                            cachedDataset = cached?.dataset,
                        )
                    } else {
                        DatasetRefreshResult.Updated(
                            dataset = validated.dataset,
                            metadata = metadata,
                        )
                    }
                }
            }
        }
    }
}
