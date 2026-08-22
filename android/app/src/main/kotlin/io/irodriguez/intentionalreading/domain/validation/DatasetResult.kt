package io.irodriguez.intentionalreading.domain.validation

import io.irodriguez.intentionalreading.domain.model.ArticleDataset

enum class DatasetErrorCode {
    UNSUPPORTED_SCHEMA,
    MALFORMED_DATASET,
}

sealed interface DatasetResult {
    data class Success(val dataset: ArticleDataset) : DatasetResult

    data class Failure(
        val code: DatasetErrorCode,
        val message: String,
        val path: String? = null,
    ) : DatasetResult
}
