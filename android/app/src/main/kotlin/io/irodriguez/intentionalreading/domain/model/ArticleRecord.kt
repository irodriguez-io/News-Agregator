package io.irodriguez.intentionalreading.domain.model

import java.time.Instant

data class ArticleRecord(
    val article: Article,
    val status: ArticleStatus,
    val firstSeenAt: Instant,
    val openedAt: Instant?,
    val savedAt: Instant?,
    val dismissedAt: Instant?,
    val readAt: Instant?,
)
