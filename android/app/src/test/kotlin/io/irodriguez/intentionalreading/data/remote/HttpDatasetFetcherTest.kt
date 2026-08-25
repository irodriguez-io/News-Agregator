package io.irodriguez.intentionalreading.data.remote

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class HttpDatasetFetcherTest {
    @Test
    fun `only the client HTTPS origin is contacted because HTTP is refused before connecting`() {
        var connectionAttempts = 0
        val fetcher = HttpDatasetFetcher(
            endpoint = "http://irodriguez.io/News-Agregator/data/articles.json",
            connectionFactory = {
                connectionAttempts += 1
                error("a cleartext URL must be refused before opening a connection")
            },
        )

        val failure = assertIs<DatasetFetchResult.Failure>(fetcher.fetch(etag = null))

        assertEquals(DatasetFetchErrorCode.INSECURE_URL, failure.code)
        assertEquals(0, connectionAttempts)
    }

    @Test
    fun `an unchanged dataset maps 304 to success and sends its ETag`() {
        val connection = FakeHttpURLConnection(statusCode = 304)
        val fetcher = fetcher(connection)

        val result = fetcher.fetch(etag = "\"dataset-v1\"")

        assertIs<DatasetFetchResult.NotModified>(result)
        assertEquals("\"dataset-v1\"", connection.getRequestProperty("If-None-Match"))
        assertEquals("GET", connection.requestMethod)
        assertEquals(HttpDatasetFetcher.CONNECT_TIMEOUT_MILLIS, connection.connectTimeout)
        assertEquals(HttpDatasetFetcher.READ_TIMEOUT_MILLIS, connection.readTimeout)
        assertFalse(connection.instanceFollowRedirects)
        assertEquals(0, connection.inputStreamRequests)
    }

    @Test
    fun `a redirect is refused instead of opening or reading its target`() {
        val connection = FakeHttpURLConnection(statusCode = 302)

        val failure = assertIs<DatasetFetchResult.Failure>(fetcher(connection).fetch(etag = null))

        assertEquals(DatasetFetchErrorCode.REDIRECT, failure.code)
        assertEquals(302, failure.statusCode)
        assertFalse(connection.instanceFollowRedirects)
        assertEquals(0, connection.inputStreamRequests)
    }

    @Test
    fun `an oversized response is abandoned as soon as the body ceiling is crossed`() {
        val body = CountingInputStream(HttpDatasetFetcher.MAX_RESPONSE_BYTES + 8_192)
        val connection = FakeHttpURLConnection(statusCode = 200, body = body)

        val failure = assertIs<DatasetFetchResult.Failure>(fetcher(connection).fetch(etag = null))

        assertEquals(DatasetFetchErrorCode.RESPONSE_TOO_LARGE, failure.code)
        assertEquals(HttpDatasetFetcher.MAX_RESPONSE_BYTES + 1, body.bytesRead)
        assertEquals(1, connection.inputStreamRequests)
    }

    @Test
    fun `a successful response preserves its body and optional ETag`() {
        val body = "response bytes exactly".encodeToByteArray()
        val connection = FakeHttpURLConnection(
            statusCode = 200,
            body = body.inputStream(),
            etag = "\"replacement\"",
        )

        val result = assertIs<DatasetFetchResult.Body>(fetcher(connection).fetch(etag = null))

        assertContentEquals(body, result.bytes)
        assertEquals("\"replacement\"", result.etag)
    }

    @Test
    fun `a non-success status is a typed failure without reading a body`() {
        val connection = FakeHttpURLConnection(statusCode = 503)

        val failure = assertIs<DatasetFetchResult.Failure>(fetcher(connection).fetch(etag = null))

        assertEquals(DatasetFetchErrorCode.HTTP_STATUS, failure.code)
        assertEquals(503, failure.statusCode)
        assertNull(failure.cause)
        assertEquals(0, connection.inputStreamRequests)
    }

    private fun fetcher(connection: FakeHttpURLConnection): HttpDatasetFetcher = HttpDatasetFetcher(
        endpoint = "https://irodriguez.io/News-Agregator/data/articles.json",
        connectionFactory = { connection },
    )

    private class FakeHttpURLConnection(
        private val statusCode: Int,
        private val body: InputStream = byteArrayOf().inputStream(),
        private val etag: String? = null,
    ) : HttpURLConnection(URL("https://irodriguez.io/News-Agregator/data/articles.json")) {
        var inputStreamRequests = 0
            private set

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int = statusCode

        override fun getInputStream(): InputStream {
            inputStreamRequests += 1
            return body
        }

        override fun getHeaderField(name: String?): String? =
            if (name.equals("ETag", ignoreCase = true)) etag else null
    }

    private class CountingInputStream(
        private val totalBytes: Int,
    ) : InputStream() {
        var bytesRead: Int = 0
            private set

        override fun read(): Int = if (bytesRead >= totalBytes) {
            -1
        } else {
            bytesRead += 1
            0
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (bytesRead >= totalBytes) return -1
            val count = minOf(length, totalBytes - bytesRead)
            buffer.fill(0, offset, offset + count)
            bytesRead += count
            return count
        }
    }
}
