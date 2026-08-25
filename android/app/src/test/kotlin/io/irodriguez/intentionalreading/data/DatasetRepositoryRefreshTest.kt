package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.dataset.DatasetCache
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheMetadata
import io.irodriguez.intentionalreading.data.local.dataset.DatasetCacheRead
import io.irodriguez.intentionalreading.data.remote.DatasetFetchErrorCode
import io.irodriguez.intentionalreading.data.remote.DatasetFetchResult
import io.irodriguez.intentionalreading.data.remote.DatasetFetcher
import io.irodriguez.intentionalreading.domain.validation.DatasetErrorCode
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatasetRepositoryRefreshTest {
    private lateinit var directory: File
    private lateinit var cache: DatasetCache
    private val fetchedAt = Instant.parse("2026-08-25T12:00:00Z")
    private val clock = Clock.fixed(fetchedAt, ZoneOffset.UTC)

    @BeforeTest
    fun createTemporaryCache() {
        directory = Files.createTempDirectory("intentional-reading-repository-").toFile()
        cache = DatasetCache(directory)
    }

    @AfterTest
    fun removeTemporaryCache() {
        directory.deleteRecursively()
    }

    @Test
    fun `an unchanged dataset is current and 304 rewrites nothing`() = runBlocking {
        val original = DatasetTestFixtures.validDatasetBytes(title = "Cached article")
        seedCache(original, etag = "\"dataset-v1\"")
        val payloadBefore = payloadFile().readBytes()
        val metadataBefore = metadataFile().readBytes()
        val fetcher = FakeDatasetFetcher(DatasetFetchResult.NotModified)

        val result = assertIs<DatasetRefreshResult.Current>(repository(fetcher).refresh())

        assertEquals("Cached article", result.dataset.articles.single().title)
        assertEquals("\"dataset-v1\"", fetcher.requestedEtags.single())
        assertContentEquals(payloadBefore, payloadFile().readBytes())
        assertContentEquals(metadataBefore, metadataFile().readBytes())
    }

    @Test
    fun `a structurally invalid response cannot replace good content byte for byte`() = runBlocking {
        val original = DatasetTestFixtures.validDatasetBytes(title = "Last good article")
        seedCache(original)
        val repository = repository(
            FakeDatasetFetcher(
                DatasetFetchResult.Body(
                    bytes = "{\"schemaVersion\":1}".encodeToByteArray(),
                    etag = "\"invalid\"",
                ),
            ),
        )

        val failure = assertIs<DatasetRefreshResult.Failed>(repository.refresh())

        assertEquals(DatasetRefreshErrorCode.VALIDATION, failure.code)
        assertEquals(DatasetErrorCode.MALFORMED_DATASET, failure.validationFailure?.code)
        assertContentEquals(original, payloadFile().readBytes())
        val cached = assertIs<DatasetCacheRead.Present>(repository.readCache())
        assertEquals("Last good article", cached.dataset.articles.single().title)
    }

    @Test
    fun `an unsupported schema is refused without reading it as version one`() = runBlocking {
        val original = DatasetTestFixtures.validDatasetBytes(title = "Supported cached article")
        seedCache(original)
        val unsupported = DatasetTestFixtures.validDatasetBytes(
            schemaVersion = 2,
            title = "Must never be adopted",
        )

        val failure = assertIs<DatasetRefreshResult.Failed>(
            repository(FakeDatasetFetcher(DatasetFetchResult.Body(unsupported, "\"v2\""))).refresh(),
        )

        assertEquals(DatasetRefreshErrorCode.VALIDATION, failure.code)
        assertEquals(DatasetErrorCode.UNSUPPORTED_SCHEMA, failure.validationFailure?.code)
        assertContentEquals(original, payloadFile().readBytes())
        assertEquals("Supported cached article", failure.cachedDataset?.articles?.single()?.title)
    }

    @Test
    fun `no network keeps the last good dataset readable and does not modify its cache`() = runBlocking {
        val original = DatasetTestFixtures.validDatasetBytes(title = "Offline cached article")
        seedCache(original)
        val metadataBefore = metadataFile().readBytes()
        val transportFailure = DatasetFetchResult.Failure(
            code = DatasetFetchErrorCode.TRANSPORT,
            message = "offline",
        )
        val repository = repository(FakeDatasetFetcher(transportFailure))

        val failure = assertIs<DatasetRefreshResult.Failed>(repository.refresh())

        assertEquals(DatasetRefreshErrorCode.FETCH, failure.code)
        assertEquals(transportFailure, failure.fetchFailure)
        assertEquals("Offline cached article", failure.cachedDataset?.articles?.single()?.title)
        assertContentEquals(original, payloadFile().readBytes())
        assertContentEquals(metadataBefore, metadataFile().readBytes())
    }

    @Test
    fun `a validated replacement is adopted verbatim with its ETag`() = runBlocking {
        val replacement = DatasetTestFixtures.validDatasetBytes(title = "Newly published article")
        val repository = repository(
            FakeDatasetFetcher(DatasetFetchResult.Body(replacement, "\"new-dataset\"")),
        )

        val updated = assertIs<DatasetRefreshResult.Updated>(repository.refresh())

        assertEquals("Newly published article", updated.dataset.articles.single().title)
        assertEquals("\"new-dataset\"", updated.metadata.etag)
        assertEquals(fetchedAt, updated.metadata.fetchedAt)
        assertContentEquals(replacement, payloadFile().readBytes())
        val cached = assertIs<DatasetCacheRead.Present>(repository.readCache())
        assertEquals(updated.metadata, cached.metadata)
    }

    @Test
    fun `an unreadable cached payload does not prevent adopting a valid replacement`() = runBlocking {
        val unreadablePayload = payloadFile()
        assertTrue(unreadablePayload.mkdir())
        val replacement = DatasetTestFixtures.validDatasetBytes(title = "Recovered article")
        val requestedEtags = mutableListOf<String?>()
        val fetcher = DatasetFetcher { etag ->
            requestedEtags += etag
            assertTrue(unreadablePayload.delete())
            DatasetFetchResult.Body(replacement, "\"recovered-dataset\"")
        }

        val updated = assertIs<DatasetRefreshResult.Updated>(repository(fetcher).refresh())

        assertEquals(listOf<String?>(null), requestedEtags)
        assertEquals("Recovered article", updated.dataset.articles.single().title)
        assertEquals("\"recovered-dataset\"", updated.metadata.etag)
        assertContentEquals(replacement, payloadFile().readBytes())
    }

    @Test
    fun `304 without a cached payload is a typed failure rather than false success`() = runBlocking {
        val result = assertIs<DatasetRefreshResult.Failed>(
            repository(FakeDatasetFetcher(DatasetFetchResult.NotModified)).refresh(),
        )

        assertEquals(DatasetRefreshErrorCode.NO_CACHED_DATASET, result.code)
        assertNull(result.cachedDataset)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    private fun repository(fetcher: DatasetFetcher): DatasetRepository = DatasetRepository(
        fetcher = fetcher,
        cache = cache,
        clock = clock,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun seedCache(bytes: ByteArray, etag: String? = "\"old-dataset\"") {
        assertTrue(
            cache.write(
                bytes,
                DatasetCacheMetadata(etag, Instant.parse("2026-08-25T11:00:00Z")),
            ),
        )
    }

    private fun payloadFile(): File = File(directory, DatasetCache.PAYLOAD_FILE_NAME)

    private fun metadataFile(): File = File(directory, DatasetCache.METADATA_FILE_NAME)

    private class FakeDatasetFetcher(
        private val result: DatasetFetchResult,
    ) : DatasetFetcher {
        val requestedEtags = mutableListOf<String?>()

        override fun fetch(etag: String?): DatasetFetchResult {
            requestedEtags += etag
            return result
        }
    }
}
