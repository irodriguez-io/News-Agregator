package io.irodriguez.intentionalreading.data.local.state

import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidator
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest.Companion.bytes
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest.Companion.with
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest.Companion.withRecord
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalStateStoreTest {
    private lateinit var directory: File
    private lateinit var stateFile: File

    @BeforeTest
    fun createTemporaryDirectory() {
        directory = Files.createTempDirectory("intentional-reading-state-").toFile()
        stateFile = File(directory, "intentional-reading-v1.json")
    }

    @AfterTest
    fun removeTemporaryDirectory() {
        directory.deleteRecursively()
    }

    @Test
    fun `a fresh install returns the default state without creating a document`() {
        val result = assertIs<LocalStateResult.Success>(LocalStateStore(directory).load())

        assertEquals(LocalState.default(), result.state)
        assertEquals(LocalStateSource.DEFAULT, result.source)
        assertFalse(stateFile.exists())
    }

    @Test
    fun `triage document round trips every field through a real directory`() {
        val document = LocalStateValidatorTest.fullyPopulatedDocument()
        val state = validState(document)
        val store = LocalStateStore(directory)

        assertIs<LocalStateResult.Success>(store.save(state))
        val loaded = assertIs<LocalStateResult.Success>(store.load())

        assertEquals(
            Json.parseToJsonElement(document.toString()),
            Json.parseToJsonElement(stateFile.readText()),
        )
        assertEquals(state, loaded.state)
        assertEquals(LocalStateSource.STORAGE, loaded.source)
    }

    @Test
    fun `foreign preferences and true signals survive a rewrite unchanged`() {
        val document = LocalStateValidatorTest.fullyPopulatedDocument()
        stateFile.writeBytes(document.bytes())
        val store = LocalStateStore(directory)
        val state = assertIs<LocalStateResult.Success>(store.load()).state

        assertIs<LocalStateResult.Success>(store.save(state))

        val rewritten = Json.parseToJsonElement(stateFile.readText())
        assertEquals(Json.parseToJsonElement(document.toString()), rewritten)
    }

    @Test
    fun `sub-millisecond timestamps round trip at contract precision`() {
        val preciseSavedAt = Instant.parse("2026-08-20T12:00:00.123456Z")
        val original = validState()
        val preciseRecord = original.articles.getValue(LocalStateValidatorTest.ARTICLE_ID).copy(
            savedAt = preciseSavedAt,
        )
        val preciseState = original.copy(
            articles = mapOf(LocalStateValidatorTest.ARTICLE_ID to preciseRecord),
        )
        val expected = preciseState.copy(
            articles = mapOf(
                LocalStateValidatorTest.ARTICLE_ID to preciseRecord.copy(
                    savedAt = preciseSavedAt.truncatedTo(ChronoUnit.MILLIS),
                ),
            ),
        )
        val store = LocalStateStore(directory)

        val saved = assertIs<LocalStateResult.Success>(store.save(preciseState))
        val loaded = assertIs<LocalStateResult.Success>(store.load())

        assertEquals(expected, saved.state)
        assertEquals(expected, loaded.state)
        assertTrue(stateFile.readText().contains("2026-08-20T12:00:00.123Z"))
    }

    @Test
    fun `malformed stored state is preserved and locks subsequent writes`() {
        val original = "{not-json".encodeToByteArray()
        stateFile.writeBytes(original)
        val store = LocalStateStore(directory)

        val loaded = assertIs<LocalStateResult.Failure>(store.load())
        assertEquals(LocalStateErrorCode.MALFORMED_JSON, loaded.code)
        assertContentEquals(original, stateFile.readBytes())

        val validState = validState()
        val saved = assertIs<LocalStateResult.Failure>(store.save(validState))
        assertEquals(LocalStateErrorCode.RECOVERY_REQUIRED, saved.code)
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `unsupported schema is preserved and distinguished from malformed JSON`() {
        val original = LocalStateValidatorTest.validDocument
            .with("schemaVersion", JsonPrimitive(2))
            .bytes()
        stateFile.writeBytes(original)

        val result = assertIs<LocalStateResult.Failure>(LocalStateStore(directory).load())

        assertEquals(LocalStateErrorCode.UNSUPPORTED_SCHEMA, result.code)
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `structurally invalid state is rejected whole and preserved byte for byte`() {
        val original = LocalStateValidatorTest.validDocument
            .with("unexpected", JsonPrimitive(true))
            .bytes()
        stateFile.writeBytes(original)

        val result = assertIs<LocalStateResult.Failure>(LocalStateStore(directory).load())

        assertEquals(LocalStateErrorCode.INVALID_STATE, result.code)
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `a transient read failure does not lock later writes`() {
        assertTrue(stateFile.mkdir())
        val store = LocalStateStore(directory)

        val loaded = assertIs<LocalStateResult.Failure>(store.load())
        assertEquals(LocalStateErrorCode.READ_FAILED, loaded.code)

        assertTrue(stateFile.delete())
        assertIs<LocalStateResult.Success>(store.save(validState()))
        assertTrue(stateFile.isFile)
    }

    @Test
    fun `reset removes the document and lifts the recovery lock`() {
        stateFile.writeText("{not-json")
        val store = LocalStateStore(directory)
        assertEquals(
            LocalStateErrorCode.MALFORMED_JSON,
            assertIs<LocalStateResult.Failure>(store.load()).code,
        )

        assertIs<LocalStateResult.Success>(store.reset())
        assertFalse(stateFile.exists())

        assertIs<LocalStateResult.Success>(store.save(validState()))
        assertTrue(stateFile.isFile)
    }

    @Test
    fun `the previous document survives a failure after the replacement is fully flushed`() {
        val original = LocalStateValidatorTest.validDocument.bytes()
        stateFile.writeBytes(original)
        val replacement = validState(
            LocalStateValidatorTest.validDocument.with(
                "session",
                kotlinx.serialization.json.JsonObject(
                    mapOf("lastCategory" to JsonPrimitive("technology")),
                ),
            ),
        )
        val failingFile = LocalStateFile(directory) { error("injected failure before rename") }
        val failingStore = LocalStateStore(failingFile)

        val result = assertIs<LocalStateResult.Failure>(failingStore.save(replacement))

        assertEquals(LocalStateErrorCode.WRITE_FAILED, result.code)
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `file failures return results instead of crossing the store boundary`() {
        val notDirectory = File(directory, "not-a-directory").apply { writeText("occupied") }
        val store = LocalStateStore(notDirectory)

        val result = assertIs<LocalStateResult.Failure>(store.save(validState()))

        assertEquals(LocalStateErrorCode.WRITE_FAILED, result.code)
    }

    @Test
    fun `export serializes the current state as the V1 root object`() {
        // Given
        val document = LocalStateValidatorTest.fullyPopulatedDocument()
        val state = validState(document)
        val store = LocalStateStore(directory)

        // When
        val exported = store.exportState(state)

        // Then
        assertEquals(
            document,
            Json.parseToJsonElement(exported.decodeToString()),
        )
        val validated = assertIs<LocalStateResult.Success>(LocalStateValidator().validate(exported))
        assertEquals(state, validated.state)
    }

    @Test
    fun `an exported document round-trips through import unchanged`() {
        // Given
        val original = validState(LocalStateValidatorTest.fullyPopulatedDocument())
        val store = LocalStateStore(directory)

        // When
        val imported = assertIs<LocalStateResult.Success>(
            store.importState(store.exportState(original)),
        )
        val loaded = assertIs<LocalStateResult.Success>(store.load())

        // Then
        assertEquals(original, imported.state)
        assertEquals(original, loaded.state)
    }

    @Test
    fun `an oversized file is refused without being read into state`() {
        // Given
        val validBytes = LocalStateValidatorTest.validDocument.bytes()
        val exactLimitCandidate = validBytes.paddedTo(LocalStateStore.MAX_IMPORT_BYTES)
        val oversizedCandidate = exactLimitCandidate + byteArrayOf(' '.code.toByte())
        val store = LocalStateStore(directory)
        assertEquals(LocalStateStore.MAX_IMPORT_BYTES, exactLimitCandidate.size)
        assertIs<LocalStateResult.Success>(store.importState(exactLimitCandidate))
        val storedAtLimit = stateFile.readBytes()

        // When
        val refused = assertIs<LocalStateResult.Failure>(store.importState(oversizedCandidate))

        // Then
        assertEquals(LocalStateErrorCode.IMPORT_TOO_LARGE, refused.code)
        assertContentEquals(storedAtLimit, stateFile.readBytes())
        assertEquals(
            validState(),
            assertIs<LocalStateResult.Success>(store.load()).state,
        )
    }

    @Test
    fun `malformed, wrong-schema, and structurally invalid candidates are all refused atomically`() {
        // Given
        val store = LocalStateStore(directory)
        assertIs<LocalStateResult.Success>(store.save(validState()))
        val originalBytes = stateFile.readBytes()
        val invalidRecord = LocalStateValidatorTest.validDocument.withRecord { record ->
            record.with("status", JsonPrimitive("archived"))
        }
        val candidates = listOf(
            LocalStateErrorCode.MALFORMED_JSON to "{not-json".encodeToByteArray(),
            LocalStateErrorCode.UNSUPPORTED_SCHEMA to LocalStateValidatorTest.validDocument
                .with("schemaVersion", JsonPrimitive(2))
                .bytes(),
            LocalStateErrorCode.INVALID_STATE to invalidRecord.bytes(),
        )

        // When / Then
        candidates.forEach { (expectedCode, candidate) ->
            val refused = assertIs<LocalStateResult.Failure>(store.importState(candidate))
            assertEquals(expectedCode, refused.code)
            assertContentEquals(originalBytes, stateFile.readBytes())
            assertEquals(validState(), assertIs<LocalStateResult.Success>(store.load()).state)
        }
    }

    @Test
    fun `a half-written import cannot be observed`() {
        // Given
        val originalBytes = LocalStateValidatorTest.validDocument.bytes()
        val originalState = validState()
        stateFile.writeBytes(originalBytes)
        val candidate = LocalStateValidatorTest.validDocument.with(
            "session",
            kotlinx.serialization.json.JsonObject(
                mapOf("lastCategory" to JsonPrimitive("technology")),
            ),
        )
        val failingStore = LocalStateStore(
            LocalStateFile(directory) { error("injected failure before rename") },
        )

        // When
        val result = assertIs<LocalStateResult.Failure>(failingStore.importState(candidate.bytes()))

        // Then
        assertEquals(LocalStateErrorCode.WRITE_FAILED, result.code)
        assertContentEquals(originalBytes, stateFile.readBytes())
        assertEquals(
            originalState,
            assertIs<LocalStateResult.Success>(LocalStateStore(directory).load()).state,
        )
    }

    @Test
    fun `an import recovers a store that was locked for recovery`() {
        // Given
        val corruptBytes = "{not-json".encodeToByteArray()
        stateFile.writeBytes(corruptBytes)
        val store = LocalStateStore(directory)
        assertEquals(
            LocalStateErrorCode.MALFORMED_JSON,
            assertIs<LocalStateResult.Failure>(store.load()).code,
        )

        // When / Then: rejection preserves both the bytes and the lock.
        assertEquals(
            LocalStateErrorCode.MALFORMED_JSON,
            assertIs<LocalStateResult.Failure>(store.importState(corruptBytes)).code,
        )
        assertContentEquals(corruptBytes, stateFile.readBytes())
        assertEquals(
            LocalStateErrorCode.RECOVERY_REQUIRED,
            assertIs<LocalStateResult.Failure>(store.save(validState())).code,
        )

        // When / Then: a successful import writes first, then clears the lock.
        val imported = assertIs<LocalStateResult.Success>(
            store.importState(LocalStateValidatorTest.validDocument.bytes()),
        )
        assertEquals(validState(), imported.state)
        assertIs<LocalStateResult.Success>(
            store.save(
                validState(
                    LocalStateValidatorTest.validDocument.with(
                        "session",
                        kotlinx.serialization.json.JsonObject(
                            mapOf("lastCategory" to JsonPrimitive("technology")),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun validState(
        document: kotlinx.serialization.json.JsonObject = LocalStateValidatorTest.validDocument,
    ): LocalState = assertIs<LocalStateResult.Success>(
        LocalStateValidator().validate(document.bytes()),
    ).state

    private fun ByteArray.paddedTo(size: Int): ByteArray = ByteArray(size) { index ->
        if (index < this.size) this[index] else ' '.code.toByte()
    }
}
