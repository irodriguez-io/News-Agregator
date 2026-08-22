package io.irodriguez.intentionalreading.domain.model

data class Article(
    val id: String,
    val title: String,
    val url: String,
    val source: ArticleSource,
    val category: Category,
    val publishedAt: String?,
    val author: String?,
    val excerpt: String,
    val readingTimeMinutes: Int?,
    val tags: List<ArticleTag>,
    val contentType: ArticleContentType,
    val score: ArticleScore,
)

data class ArticleSource(
    val id: String,
    val name: String,
)

data class ArticleTag(
    val id: String,
    val label: String,
)

data class ArticleContentType(
    val id: ContentTypeId,
    val label: String,
)

data class ArticleScore(
    val base: Int,
    val sourceQuality: Int,
    val contentType: Int,
    val freshness: Int,
    val topicSignal: Int,
    val metadata: Int,
)
