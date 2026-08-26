package io.irodriguez.intentionalreading.data

import java.io.InputStream

internal fun readBounded(input: InputStream, byteLimit: Int): ByteArray? {
    require(byteLimit >= 0) { "byteLimit must not be negative" }
    require(byteLimit < Int.MAX_VALUE) { "byteLimit is too large" }

    val bytes = ByteArray(byteLimit + 1)
    var bytesRead = 0
    while (bytesRead < bytes.size) {
        val read = input.read(bytes, bytesRead, bytes.size - bytesRead)
        when {
            read == -1 -> return bytes.copyOf(bytesRead)
            read > 0 -> bytesRead += read
            else -> {
                val next = input.read()
                if (next == -1) return bytes.copyOf(bytesRead)
                bytes[bytesRead] = next.toByte()
                bytesRead += 1
            }
        }
    }
    return null
}
