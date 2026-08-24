package io.irodriguez.intentionalreading.data.local.state

import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidator
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest.Companion.bytes
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidatorTest.Companion.with
import java.io.File
import java.nio.file.Files
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

    private fun validState(
        document: kotlinx.serialization.json.JsonObject = LocalStateValidatorTest.validDocument,
    ): LocalState = assertIs<LocalStateResult.Success>(
        LocalStateValidator().validate(document.bytes()),
    ).state
}
