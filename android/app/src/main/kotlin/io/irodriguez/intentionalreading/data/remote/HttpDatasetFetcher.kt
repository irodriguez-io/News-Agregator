package io.irodriguez.intentionalreading.data.remote

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpDatasetFetcher internal constructor(
    private val endpoint: String,
    private val connectionFactory: (URL) -> HttpURLConnection,
) : DatasetFetcher {
    constructor() : this(
        endpoint = DATASET_URL,
        connectionFactory = { url -> url.openConnection() as HttpURLConnection },
    )

    override fun fetch(etag: String?): DatasetFetchResult {
        val url = try {
            URL(endpoint)
        } catch (failure: Exception) {
            return failure(
                code = DatasetFetchErrorCode.INSECURE_URL,
                message = "The dataset URL is not a valid HTTPS URL",
                cause = failure,
            )
        }
        if (!url.protocol.equals("https", ignoreCase = true)) {
            return failure(
                code = DatasetFetchErrorCode.INSECURE_URL,
                message = "The dataset URL must use HTTPS",
            )
        }

        var connection: HttpURLConnection? = null
        return try {
            connection = connectionFactory(url).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                instanceFollowRedirects = false
                useCaches = false
                if (etag != null) setRequestProperty("If-None-Match", etag)
            }
            when (val statusCode = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> DatasetFetchResult.NotModified
                HttpURLConnection.HTTP_OK -> readBody(connection)
                in 300..399 -> failure(
                    code = DatasetFetchErrorCode.REDIRECT,
                    message = "The dataset endpoint returned a redirect",
                    statusCode = statusCode,
                )
                else -> failure(
                    code = DatasetFetchErrorCode.HTTP_STATUS,
                    message = "The dataset endpoint returned HTTP $statusCode",
                    statusCode = statusCode,
                )
            }
        } catch (failure: Exception) {
            failure(
                code = DatasetFetchErrorCode.TRANSPORT,
                message = "The dataset could not be fetched",
                cause = failure,
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection): DatasetFetchResult =
        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(READ_BUFFER_BYTES)
            var remaining = MAX_RESPONSE_BYTES
            while (true) {
                val read = input.read(buffer, 0, minOf(buffer.size, remaining + 1))
                if (read == -1) break
                if (read > remaining) {
                    return failure(
                        code = DatasetFetchErrorCode.RESPONSE_TOO_LARGE,
                        message = "The dataset response exceeded the 10 MiB limit",
                    )
                }
                output.write(buffer, 0, read)
                remaining -= read
            }
            DatasetFetchResult.Body(
                bytes = output.toByteArray(),
                etag = connection.getHeaderField("ETag"),
            )
        }

    private fun failure(
        code: DatasetFetchErrorCode,
        message: String,
        statusCode: Int? = null,
        cause: Throwable? = null,
    ): DatasetFetchResult.Failure = DatasetFetchResult.Failure(
        code = code,
        message = message,
        statusCode = statusCode,
        cause = cause,
    )

    internal companion object {
        const val DATASET_URL = "https://irodriguez.io/News-Agregator/data/articles.json"
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 20_000
        const val MAX_RESPONSE_BYTES = 10 * 1024 * 1024
        private const val READ_BUFFER_BYTES = 8 * 1024
    }
}
