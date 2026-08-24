package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.state.LocalStateStore
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalStateRepositoryTest {
    private val directory = Files.createTempDirectory("local-state-repository-test").toFile()
    private val stateFile = File(directory, "intentional-reading-v1.json")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `reset is a repository write that lifts the recovery lock`() = runBlocking {
        stateFile.writeText("{not-json")
        val repository = LocalStateRepository(
            store = LocalStateStore(directory),
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertEquals(
            LocalStateErrorCode.MALFORMED_JSON,
            assertIs<LocalStateResult.Failure>(repository.load()).code,
        )
        assertEquals(
            LocalStateErrorCode.RECOVERY_REQUIRED,
            assertIs<LocalStateResult.Failure>(repository.save(LocalState.default())).code,
        )

        assertIs<LocalStateResult.Success>(repository.reset())
        assertTrue(!stateFile.exists())
        assertIs<LocalStateResult.Success>(repository.save(LocalState.default()))
        Unit
    }
}
