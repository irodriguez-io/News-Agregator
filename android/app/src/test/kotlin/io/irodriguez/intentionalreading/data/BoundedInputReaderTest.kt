package io.irodriguez.intentionalreading.data

import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BoundedInputReaderTest {
    @Test
    fun `a candidate that lies about its size is still refused`() {
        // Given
        val byteLimit = 5 * 1024 * 1024
        val candidate = SmallReportedSizeInputStream(totalBytes = byteLimit + 8_192)
        assertEquals(1, candidate.available())

        // When
        val result = readBounded(candidate, byteLimit)

        // Then
        assertNull(result)
        assertEquals(byteLimit + 1, candidate.bytesRead)
    }

    private class SmallReportedSizeInputStream(
        private val totalBytes: Int,
    ) : InputStream() {
        var bytesRead: Int = 0
            private set

        override fun available(): Int = 1

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
