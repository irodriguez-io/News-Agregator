package io.irodriguez.intentionalreading.data.local.dataset

import io.irodriguez.intentionalreading.data.DatasetTestFixtures
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatasetCacheTest {
    private lateinit var directory: File
    private lateinit var payloadFile: File
    private lateinit var metadataFile: File

    @BeforeTest
    fun createTemporaryDirectory() {
        directory = Files.createTempDirectory("intentional-reading-dataset-").toFile()
        payloadFile = File(directory, DatasetCache.PAYLOAD_FILE_NAME)
        metadataFile = File(directory, DatasetCache.METADATA_FILE_NAME)
    }

    @AfterTest
    fun removeTemporaryDirectory() {
        directory.deleteRecursively()
    }

    @Test
    fun `validated response bytes and metadata round trip without rewriting the payload`() {
        val bytes = DatasetTestFixtures.validDatasetBytes()
        val metadata = DatasetCacheMetadata(
            etag = "\"dataset-v1\"",
            fetchedAt = Instant.parse("2026-08-25T12:00:00Z"),
        )
        val cache = DatasetCache(directory)

        assertTrue(cache.write(bytes, metadata))
        val read = assertIs<DatasetCacheRead.Present>(cache.read())

        assertContentEquals(bytes, payloadFile.readBytes())
        assertContentEquals(bytes, read.bytes)
        assertEquals(metadata, read.metadata)
        assertEquals("A readable article title", read.dataset.articles.single().title)
    }

    @Test
    fun `a payload without a readable sidecar remains usable with no validator`() {
        val bytes = DatasetTestFixtures.validDatasetBytes()
        payloadFile.writeBytes(bytes)
        metadataFile.writeText("{not-json")

        val read = assertIs<DatasetCacheRead.Present>(DatasetCache(directory).read())

        assertContentEquals(bytes, read.bytes)
        assertNull(read.metadata)
    }

    @Test
    fun `a sidecar without a payload is ignored as no cache`() {
        metadataFile.writeText("irrelevant")

        assertIs<DatasetCacheRead.Absent>(DatasetCache(directory).read())
    }

    @Test
    fun `a payload that no longer validates is treated as no cache`() {
        payloadFile.writeText("{not-json")

        assertIs<DatasetCacheRead.Absent>(DatasetCache(directory).read())
    }

    @Test
    fun `a cache write is atomic by rename when replacement fails after flush`() {
        val original = DatasetTestFixtures.validDatasetBytes(title = "Original cached article")
        val replacement = DatasetTestFixtures.validDatasetBytes(title = "Replacement article")
        val cache = DatasetCache(directory)
        assertTrue(
            cache.write(
                original,
                DatasetCacheMetadata(null, Instant.parse("2026-08-25T11:00:00Z")),
            ),
        )
        val failingPayload = DatasetCacheFile(
            directory = directory,
            fileName = DatasetCache.PAYLOAD_FILE_NAME,
            beforeRename = { error("injected failure before rename") },
        )
        val failingCache = DatasetCache(
            payloadFile = failingPayload,
            metadataFile = DatasetCacheFile(directory, DatasetCache.METADATA_FILE_NAME),
        )

        assertFalse(
            failingCache.write(
                replacement,
                DatasetCacheMetadata("\"replacement\"", Instant.parse("2026-08-25T12:00:00Z")),
            ),
        )

        assertContentEquals(original, payloadFile.readBytes())
        assertFalse(File(directory, "${DatasetCache.PAYLOAD_FILE_NAME}.tmp").exists())
    }
}
