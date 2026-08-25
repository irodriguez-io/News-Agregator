package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.DatasetSource
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCache
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheMetadata
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.data.remote.DatasetFetchResult
import io.irodriguez.intentionalreading.data.remote.DatasetFetcher
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.validation.DatasetErrorCode
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator
import java.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DatasetRefreshErrorCode {
    FETCH,
    VALIDATION,
    CACHE_WRITE,
    NO_CACHED_DATASET,
    NOT_CONFIGURED,
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
    private val source: DatasetSource? = null,
    private val validator: DatasetValidator = DatasetValidator(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fetcher: DatasetFetcher? = null,
    private val cache: DatasetCache? = null,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun load(): DatasetResult = withContext(ioDispatcher) {
        try {
            val configuredSource = source ?: return@withContext DatasetResult.Failure(
                code = DatasetErrorCode.MALFORMED_DATASET,
                message = "No bundled dataset source is configured",
            )
            validator.validate(configuredSource.read())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            DatasetResult.Failure(
                code = DatasetErrorCode.MALFORMED_DATASET,
                message = "The bundled dataset could not be read",
            )
        }
    }

    suspend fun readCache(): DatasetCacheRead = withContext(ioDispatcher) {
        cache?.read() ?: DatasetCacheRead.Absent
    }

    suspend fun refresh(): DatasetRefreshResult = withContext(ioDispatcher) {
        val configuredFetcher = fetcher ?: return@withContext DatasetRefreshResult.Failed(
            code = DatasetRefreshErrorCode.NOT_CONFIGURED,
        )
        val configuredCache = cache ?: return@withContext DatasetRefreshResult.Failed(
            code = DatasetRefreshErrorCode.NOT_CONFIGURED,
        )
        val cached = when (val read = configuredCache.read()) {
            DatasetCacheRead.Absent -> null
            is DatasetCacheRead.Present -> read
        }

        when (val fetched = configuredFetcher.fetch(cached?.metadata?.etag)) {
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
                    if (!configuredCache.write(fetched.bytes, metadata)) {
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
