package io.irodriguez.intentionalreading.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ArticleDatasetDto(
    val schemaVersion: Int,
    val generatedAt: String,
    val pipeline: PipelineDto,
    val articles: List<ArticleDto>,
)

@Serializable
internal data class PipelineDto(
    val enabledSourceCount: Int,
    val successfulSourceCount: Int,
    val failedSourceCount: Int,
    val articleCount: Int,
)

@Serializable
internal data class ArticleDto(
    val id: String,
    val title: String,
    val url: String,
    val source: ArticleSourceDto,
    val category: String,
    val publishedAt: String?,
    val author: String?,
    val excerpt: String,
    val readingTimeMinutes: Int?,
    val tags: List<ArticleTagDto>,
    val contentType: ArticleContentTypeDto,
    val score: ArticleScoreDto,
)

@Serializable
internal data class ArticleSourceDto(
    val id: String,
    val name: String,
)

@Serializable
internal data class ArticleTagDto(
    val id: String,
    val label: String,
)

@Serializable
internal data class ArticleContentTypeDto(
    val id: String,
    val label: String,
)

@Serializable
internal data class ArticleScoreDto(
    val base: Int,
    val sourceQuality: Int,
    val contentType: Int,
    val freshness: Int,
    val topicSignal: Int,
    val metadata: Int,
)
