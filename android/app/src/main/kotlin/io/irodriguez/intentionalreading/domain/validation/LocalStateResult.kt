package io.irodriguez.intentionalreading.domain.validation

import io.irodriguez.intentionalreading.domain.model.LocalState

enum class LocalStateErrorCode {
    MALFORMED_JSON,
    INVALID_STATE,
    UNSUPPORTED_SCHEMA,
    READ_FAILED,
    WRITE_FAILED,
    RECOVERY_REQUIRED,
    IMPORT_TOO_LARGE,
}

enum class LocalStateSource {
    DEFAULT,
    STORAGE,
}

sealed interface LocalStateExport {
    data class Success(val bytes: ByteArray) : LocalStateExport
    data class Failure(val code: LocalStateErrorCode, val message: String) : LocalStateExport
}

sealed interface LocalStateResult {
    data class Success(
        val state: LocalState,
        val source: LocalStateSource,
    ) : LocalStateResult

    data class Failure(
        val code: LocalStateErrorCode,
        val message: String,
        val state: LocalState? = null,
        val path: String? = null,
    ) : LocalStateResult
}
