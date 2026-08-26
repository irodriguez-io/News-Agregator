package io.irodriguez.intentionalreading.ui

import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateExport
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource

internal class FakeLocalStateStore(
    initialLoadResult: LocalStateResult = LocalStateResult.Success(
        state = LocalState.default(),
        source = LocalStateSource.DEFAULT,
    ),
) {
    var loadResult: LocalStateResult = initialLoadResult
    var saveBehavior: suspend (LocalState) -> LocalStateResult = { state ->
        LocalStateResult.Success(state, LocalStateSource.STORAGE)
    }
    var resetBehavior: suspend () -> LocalStateResult = {
        LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT)
    }
    var exportBehavior: suspend (LocalState) -> LocalStateExport = {
        LocalStateExport.Success(byteArrayOf())
    }
    var importBehavior: suspend (ByteArray) -> LocalStateResult = {
        LocalStateResult.Success(LocalState.default(), LocalStateSource.STORAGE)
    }
    val saveRequests = mutableListOf<LocalState>()
    val exportRequests = mutableListOf<LocalState>()
    val importRequests = mutableListOf<ByteArray>()
    var resetRequests = 0

    suspend fun load(): LocalStateResult = loadResult

    suspend fun save(state: LocalState): LocalStateResult {
        saveRequests += state
        return saveBehavior(state).also { result ->
            if (result is LocalStateResult.Success) loadResult = result
        }
    }

    suspend fun reset(): LocalStateResult {
        resetRequests += 1
        return resetBehavior().also { result ->
            if (result is LocalStateResult.Success) loadResult = result
        }
    }

    suspend fun exportState(state: LocalState): LocalStateExport {
        exportRequests += state
        return exportBehavior(state)
    }

    suspend fun importState(candidateBytes: ByteArray): LocalStateResult {
        importRequests += candidateBytes
        return importBehavior(candidateBytes).also { result ->
            if (result is LocalStateResult.Success) loadResult = result
        }
    }
}
