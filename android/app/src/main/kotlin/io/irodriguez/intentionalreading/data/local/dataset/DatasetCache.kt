package io.irodriguez.intentionalreading.data.local.dataset

import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.validation.DatasetResult
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator
import java.io.File

sealed interface DatasetCacheRead {
    data object Absent : DatasetCacheRead

    data class Present(
        val bytes: ByteArray,
        val dataset: ArticleDataset,
        val metadata: DatasetCacheMetadata?,
    ) : DatasetCacheRead

    data object Failed : DatasetCacheRead
}

class DatasetCache internal constructor(
    private val payloadFile: DatasetCacheFile,
    private val metadataFile: DatasetCacheFile,
    private val validator: DatasetValidator = DatasetValidator(),
) {
    constructor(directory: File) : this(
        payloadFile = DatasetCacheFile(directory, PAYLOAD_FILE_NAME),
        metadataFile = DatasetCacheFile(directory, METADATA_FILE_NAME),
    )

    fun read(): DatasetCacheRead = when (val payload = payloadFile.read()) {
        DatasetCacheFileRead.Absent -> DatasetCacheRead.Absent
        DatasetCacheFileRead.Failed -> DatasetCacheRead.Absent
        is DatasetCacheFileRead.Present -> when (val validated = validator.validate(payload.bytes)) {
            is DatasetResult.Failure -> DatasetCacheRead.Absent
            is DatasetResult.Success -> DatasetCacheRead.Present(
                bytes = payload.bytes,
                dataset = validated.dataset,
                metadata = readMetadata(),
            )
        }
    }

    fun write(bytes: ByteArray, metadata: DatasetCacheMetadata): Boolean {
        if (validator.validate(bytes) !is DatasetResult.Success) return false
        if (!metadataFile.remove()) return false
        if (!payloadFile.write(bytes)) return false
        return metadataFile.write(metadata.encode())
    }

    private fun readMetadata(): DatasetCacheMetadata? = when (val metadata = metadataFile.read()) {
        DatasetCacheFileRead.Absent,
        DatasetCacheFileRead.Failed,
        -> null
        is DatasetCacheFileRead.Present -> DatasetCacheMetadata.decode(metadata.bytes)
    }

    internal companion object {
        const val PAYLOAD_FILE_NAME = "intentional-reading-dataset-v1.json"
        const val METADATA_FILE_NAME = "intentional-reading-dataset-v1.metadata.json"
    }
}
