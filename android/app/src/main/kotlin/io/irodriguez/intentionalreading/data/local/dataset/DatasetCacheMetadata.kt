package io.irodriguez.intentionalreading.data.local.dataset

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class DatasetCacheMetadata(
    val etag: String?,
    val fetchedAt: Instant,
) {
    internal fun encode(): ByteArray = MetadataJson.encodeToString(
        DatasetCacheMetadataDocument(
            etag = etag,
            fetchedAt = fetchedAt.toString(),
        ),
    ).encodeToByteArray()

    internal companion object {
        fun decode(bytes: ByteArray): DatasetCacheMetadata? = try {
            val document = MetadataJson.decodeFromString<DatasetCacheMetadataDocument>(
                bytes.toString(Charsets.UTF_8),
            )
            DatasetCacheMetadata(
                etag = document.etag,
                fetchedAt = Instant.parse(document.fetchedAt),
            )
        } catch (failure: Exception) {
            null
        }

        private val MetadataJson = Json {
            ignoreUnknownKeys = false
            explicitNulls = true
            isLenient = false
            coerceInputValues = false
            allowTrailingComma = false
            allowComments = false
        }
    }
}

@Serializable
private data class DatasetCacheMetadataDocument(
    val etag: String?,
    val fetchedAt: String,
)
