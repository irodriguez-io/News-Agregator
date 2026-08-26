package io.irodriguez.intentionalreading.data

import io.irodriguez.intentionalreading.data.local.state.LocalStateStore
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateExport
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalStateRepository(
    private val store: LocalStateStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val writeMutex = Mutex()

    suspend fun load(): LocalStateResult = withContext(ioDispatcher) {
        store.load()
    }

    suspend fun save(state: LocalState): LocalStateResult = withContext(ioDispatcher) {
        writeMutex.withLock {
            store.save(state)
        }
    }

    suspend fun reset(): LocalStateResult = withContext(ioDispatcher) {
        writeMutex.withLock {
            store.reset()
        }
    }

    suspend fun exportState(state: LocalState): LocalStateExport = withContext(ioDispatcher) {
        store.exportState(state)
    }

    suspend fun importState(candidateBytes: ByteArray): LocalStateResult = withContext(ioDispatcher) {
        writeMutex.withLock {
            store.importState(candidateBytes)
        }
    }
}
