package io.irodriguez.intentionalreading.data.local.state

import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateExport
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidator
import java.io.File

class LocalStateStore internal constructor(
    private val file: LocalStateFile,
) {
    constructor(directory: File) : this(LocalStateFile(directory))

    private val validator = LocalStateValidator()
    private var recoveryLocked = false

    fun load(): LocalStateResult = boundary(
        fallbackCode = LocalStateErrorCode.READ_FAILED,
        fallbackMessage = "Local state could not be read",
        fallbackState = LocalState.default(),
    ) {
        when (val read = file.read()) {
            LocalStateFileRead.Absent -> LocalStateResult.Success(
                state = LocalState.default(),
                source = LocalStateSource.DEFAULT,
            )
            is LocalStateFileRead.Present -> when (val result = validator.validate(read.bytes)) {
                is LocalStateResult.Success -> result.copy(source = LocalStateSource.STORAGE)
                is LocalStateResult.Failure -> {
                    recoveryLocked = true
                    result.copy(state = LocalState.default())
                }
            }
            LocalStateFileRead.Failed -> {
                failure(
                    code = LocalStateErrorCode.READ_FAILED,
                    message = "Local state could not be read",
                    state = LocalState.default(),
                )
            }
        }
    }

    fun save(state: LocalState): LocalStateResult = boundary(
        fallbackCode = LocalStateErrorCode.WRITE_FAILED,
        fallbackMessage = "Local state could not be persisted",
    ) {
        val validated = when (val result = LocalStateMapper.validate(state)) {
            is LocalStateResult.Success -> result.state
            is LocalStateResult.Failure -> return@boundary result
        }
        if (recoveryLocked) {
            return@boundary failure(
                code = LocalStateErrorCode.RECOVERY_REQUIRED,
                message = "Stored local state must be reset before changes can be persisted",
            )
        }
        if (!file.write(LocalStateMapper.encode(validated))) {
            return@boundary failure(
                code = LocalStateErrorCode.WRITE_FAILED,
                message = "Local state could not be persisted",
            )
        }
        LocalStateResult.Success(validated, LocalStateSource.STORAGE)
    }

    fun exportState(state: LocalState): LocalStateExport = boundary(
        fallback = {
            LocalStateExport.Failure(
                code = LocalStateErrorCode.WRITE_FAILED,
                message = "Local state could not be exported",
            )
        },
    ) {
        val validated = when (val result = LocalStateMapper.validate(state)) {
            is LocalStateResult.Success -> result.state
            is LocalStateResult.Failure -> return@boundary LocalStateExport.Failure(
                code = result.code,
                message = result.message,
            )
        }
        LocalStateExport.Success(LocalStateMapper.encode(validated))
    }

    fun importState(candidateBytes: ByteArray): LocalStateResult = boundary(
        fallbackCode = LocalStateErrorCode.WRITE_FAILED,
        fallbackMessage = "Local state could not be imported",
    ) {
        if (candidateBytes.size > MAX_IMPORT_BYTES) {
            return@boundary failure(
                code = LocalStateErrorCode.IMPORT_TOO_LARGE,
                message = "Imported local state exceeds 5 MiB",
            )
        }
        val validated = when (val result = validator.validate(candidateBytes)) {
            is LocalStateResult.Success -> result.state
            is LocalStateResult.Failure -> return@boundary result
        }
        if (!file.write(LocalStateMapper.encode(validated))) {
            return@boundary failure(
                code = LocalStateErrorCode.WRITE_FAILED,
                message = "Local state could not be imported",
            )
        }
        recoveryLocked = false
        LocalStateResult.Success(validated, LocalStateSource.STORAGE)
    }

    fun reset(): LocalStateResult = boundary(
        fallbackCode = LocalStateErrorCode.WRITE_FAILED,
        fallbackMessage = "Local state could not be reset",
    ) {
        if (!file.reset()) {
            return@boundary failure(
                code = LocalStateErrorCode.WRITE_FAILED,
                message = "Local state could not be reset",
            )
        }
        recoveryLocked = false
        LocalStateResult.Success(LocalState.default(), LocalStateSource.DEFAULT)
    }

    private inline fun boundary(
        fallbackCode: LocalStateErrorCode,
        fallbackMessage: String,
        fallbackState: LocalState? = null,
        operation: () -> LocalStateResult,
    ): LocalStateResult = boundary(
        fallback = {
            failure(
                code = fallbackCode,
                message = fallbackMessage,
                state = fallbackState,
            )
        },
        operation = operation,
    )

    private inline fun <T> boundary(
        fallback: () -> T,
        operation: () -> T,
    ): T = try {
        operation()
    } catch (failure: Exception) {
        fallback()
    }

    private fun failure(
        code: LocalStateErrorCode,
        message: String,
        state: LocalState? = null,
    ): LocalStateResult.Failure = LocalStateResult.Failure(
        code = code,
        message = message,
        state = state,
    )

    internal companion object {
        const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
    }
}
