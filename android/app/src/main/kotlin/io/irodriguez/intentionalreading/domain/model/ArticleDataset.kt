package io.irodriguez.intentionalreading.domain.model

data class ArticleDataset(
    val schemaVersion: Int,
    val generatedAt: String,
    val pipeline: PipelineMetadata,
    val articles: List<Article>,
)

data class PipelineMetadata(
    val enabledSourceCount: Int,
    val successfulSourceCount: Int,
    val failedSourceCount: Int,
    val articleCount: Int,
)
