package io.irodriguez.intentionalreading.data.remote

enum class DatasetFetchErrorCode {
    INSECURE_URL,
    REDIRECT,
    HTTP_STATUS,
    RESPONSE_TOO_LARGE,
    TRANSPORT,
}

sealed interface DatasetFetchResult {
    data object NotModified : DatasetFetchResult

    data class Body(
        val bytes: ByteArray,
        val etag: String?,
    ) : DatasetFetchResult

    data class Failure(
        val code: DatasetFetchErrorCode,
        val message: String,
        val statusCode: Int? = null,
        val cause: Throwable? = null,
    ) : DatasetFetchResult
}
