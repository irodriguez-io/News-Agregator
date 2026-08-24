package io.irodriguez.intentionalreading.data.local.state

import io.irodriguez.intentionalreading.domain.validation.LocalStateTestApi
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
        val result = LocalStateTestApi.call(LocalStateTestApi.store(directory), "load")

        LocalStateTestApi.successState(result)
        assertEquals("DEFAULT", LocalStateTestApi.source(result))
        assertFalse(stateFile.exists())
    }

    @Test
    fun `triage document round trips every field through a real directory`() {
        val document = LocalStateValidatorTest.fullyPopulatedDocument()
        val state = LocalStateTestApi.successState(LocalStateTestApi.validate(document.bytes()))
        val store = LocalStateTestApi.store(directory)

        val saved = LocalStateTestApi.call(store, "save", state)
        LocalStateTestApi.successState(saved)
        val loaded = LocalStateTestApi.call(store, "load")
        LocalStateTestApi.successState(loaded)

        assertEquals(
            Json.parseToJsonElement(document.toString()),
            Json.parseToJsonElement(stateFile.readText()),
        )
        assertEquals("STORAGE", LocalStateTestApi.source(loaded))
    }

    @Test
    fun `foreign preferences and true signals survive a rewrite unchanged`() {
        val document = LocalStateValidatorTest.fullyPopulatedDocument()
        stateFile.writeBytes(document.bytes())
        val store = LocalStateTestApi.store(directory)
        val state = LocalStateTestApi.successState(LocalStateTestApi.call(store, "load"))

        LocalStateTestApi.successState(LocalStateTestApi.call(store, "save", state))

        val rewritten = Json.parseToJsonElement(stateFile.readText())
        assertEquals(Json.parseToJsonElement(document.toString()), rewritten)
    }

    @Test
    fun `malformed stored state is preserved and locks subsequent writes`() {
        val original = "{not-json".encodeToByteArray()
        stateFile.writeBytes(original)
        val store = LocalStateTestApi.store(directory)

        val loaded = LocalStateTestApi.call(store, "load")
        assertEquals("MALFORMED_JSON", LocalStateTestApi.failureCode(loaded))
        assertContentEquals(original, stateFile.readBytes())

        val validState = validState()
        val saved = LocalStateTestApi.call(store, "save", validState)
        assertEquals("RECOVERY_REQUIRED", LocalStateTestApi.failureCode(saved))
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `unsupported schema is preserved and distinguished from malformed JSON`() {
        val original = LocalStateValidatorTest.validDocument
            .with("schemaVersion", JsonPrimitive(2))
            .bytes()
        stateFile.writeBytes(original)

        val result = LocalStateTestApi.call(LocalStateTestApi.store(directory), "load")

        assertEquals("UNSUPPORTED_SCHEMA", LocalStateTestApi.failureCode(result))
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `structurally invalid state is rejected whole and preserved byte for byte`() {
        val original = LocalStateValidatorTest.validDocument
            .with("unexpected", JsonPrimitive(true))
            .bytes()
        stateFile.writeBytes(original)

        val result = LocalStateTestApi.call(LocalStateTestApi.store(directory), "load")

        assertEquals("INVALID_STATE", LocalStateTestApi.failureCode(result))
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `reset removes the document and lifts the recovery lock`() {
        stateFile.writeText("{not-json")
        val store = LocalStateTestApi.store(directory)
        assertEquals(
            "MALFORMED_JSON",
            LocalStateTestApi.failureCode(LocalStateTestApi.call(store, "load")),
        )

        val reset = LocalStateTestApi.call(store, "reset")
        LocalStateTestApi.successState(reset)
        assertFalse(stateFile.exists())

        LocalStateTestApi.successState(LocalStateTestApi.call(store, "save", validState()))
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
        val failingStore = LocalStateTestApi.storeThatFailsBeforeRename(directory)

        val result = LocalStateTestApi.call(failingStore, "save", replacement)

        assertEquals("WRITE_FAILED", LocalStateTestApi.failureCode(result))
        assertContentEquals(original, stateFile.readBytes())
    }

    @Test
    fun `file failures return results instead of crossing the store boundary`() {
        val notDirectory = File(directory, "not-a-directory").apply { writeText("occupied") }
        val store = LocalStateTestApi.store(notDirectory)

        val result = LocalStateTestApi.call(store, "save", validState())

        assertEquals("WRITE_FAILED", LocalStateTestApi.failureCode(result))
    }

    private fun validState(
        document: kotlinx.serialization.json.JsonObject = LocalStateValidatorTest.validDocument,
    ): Any = LocalStateTestApi.successState(LocalStateTestApi.validate(document.bytes()))
}
