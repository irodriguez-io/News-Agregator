package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.ranking.PersonalizedScore

data class DeckCandidate(
    val article: Article,
    val score: PersonalizedScore,
)

data class DiscoverDeckState(
    val article: Article?,
    val candidates: List<DeckCandidate>,
    val availableCount: Int,
    val remainingCount: Int,
)

object DiscoverDeck {
    fun build(
        articles: List<Article>,
        records: Map<String, ArticleRecord>,
        preferences: LocalState.Preferences,
        selectedCategory: Category?,
        heldArticleId: String?,
    ): DiscoverDeckState {
        val eligible = articles.filter { article ->
            isEligible(records[article.id]) &&
                (selectedCategory == null || article.category == selectedCategory)
        }
        val candidates = eligible
            .map { article ->
                DeckCandidate(
                    article = article,
                    score = PersonalizedScore.calculate(article, preferences),
                )
            }
            .sortedWith(candidateComparator)
        val article = candidates
            .firstOrNull { candidate -> candidate.article.id == heldArticleId }
            ?.article
            ?: candidates.firstOrNull()?.article
        return DiscoverDeckState(
            article = article,
            candidates = candidates,
            availableCount = candidates.size,
            remainingCount = if (article == null) 0 else candidates.size - 1,
        )
    }

    fun isEligible(record: ArticleRecord?): Boolean =
        record == null || record.status == ArticleStatus.OPENED

    private val candidateComparator = Comparator<DeckCandidate> { left, right ->
        val totalDifference = right.score.total.compareTo(left.score.total)
        if (totalDifference != 0) return@Comparator totalDifference

        val baseDifference = right.score.base.compareTo(left.score.base)
        if (baseDifference != 0) return@Comparator baseDifference

        val publicationDifference = comparePublicationDescending(left.article, right.article)
        if (publicationDifference != 0) return@Comparator publicationDifference

        val sourceDifference = left.article.source.id.compareTo(right.article.source.id)
        if (sourceDifference != 0) return@Comparator sourceDifference

        left.article.id.compareTo(right.article.id)
    }

    private fun comparePublicationDescending(left: Article, right: Article): Int {
        val leftPublication = left.publishedAt
        val rightPublication = right.publishedAt
        return when {
            leftPublication == null && rightPublication == null -> 0
            leftPublication == null -> 1
            rightPublication == null -> -1
            else -> rightPublication.compareTo(leftPublication)
        }
    }
}
