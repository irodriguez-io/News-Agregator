package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category

data class DiscoverDeckState(
    val article: Article?,
    val availableCount: Int,
    val remainingCount: Int,
)

object DiscoverDeck {
    fun build(
        articles: List<Article>,
        records: Map<String, ArticleRecord>,
        selectedCategory: Category?,
        heldArticleId: String?,
    ): DiscoverDeckState {
        val eligible = articles.filter { article ->
            isEligible(records[article.id]) &&
                (selectedCategory == null || article.category == selectedCategory)
        }
        val article = eligible.firstOrNull { it.id == heldArticleId } ?: eligible.firstOrNull()
        return DiscoverDeckState(
            article = article,
            availableCount = eligible.size,
            remainingCount = if (article == null) 0 else eligible.size - 1,
        )
    }

    fun isEligible(record: ArticleRecord?): Boolean =
        record == null || record.status == ArticleStatus.OPENED
}
