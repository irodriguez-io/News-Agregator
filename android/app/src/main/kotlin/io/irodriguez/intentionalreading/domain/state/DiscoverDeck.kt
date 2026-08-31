package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.ranking.PersonalizedScore

data class DeckSequencing(
    val score: Double,
    val sameSourcePenalty: Double,
    val categoryPenalty: Double,
)

data class DeckCandidate(
    val article: Article,
    val score: PersonalizedScore,
    val sequencing: DeckSequencing = DeckSequencing(
        score = score.total,
        sameSourcePenalty = 0.0,
        categoryPenalty = 0.0,
    ),
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
        val initialCandidates = eligible
            .map { article ->
                DeckCandidate(
                    article = article,
                    score = PersonalizedScore.calculate(article, preferences),
                )
            }
            .sortedWith(candidateComparator)
        val candidates = sequenceCandidates(initialCandidates, selectedCategory)
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

    internal val candidateComparator = Comparator<DeckCandidate> { left, right ->
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

    private val sequencedComparator = Comparator<DeckCandidate> { left, right ->
        val sequencingDifference = right.sequencing.score.compareTo(left.sequencing.score)
        if (sequencingDifference != 0) return@Comparator sequencingDifference

        candidateComparator.compare(left, right)
    }

    private fun sequenceCandidates(
        initialCandidates: List<DeckCandidate>,
        selectedCategory: Category?,
    ): List<DeckCandidate> {
        val remaining = initialCandidates.toMutableList()
        val selected = mutableListOf<DeckCandidate>()

        while (remaining.isNotEmpty()) {
            val winner = remaining
                .map { candidate ->
                    candidate.copy(
                        sequencing = sequencingFor(candidate, selected, selectedCategory),
                    )
                }
                .minWithOrNull(sequencedComparator)
                ?: break
            selected += winner
            remaining.removeAt(
                remaining.indexOfFirst { candidate -> candidate.article.id == winner.article.id },
            )
        }

        return selected
    }

    private fun sequencingFor(
        candidate: DeckCandidate,
        selected: List<DeckCandidate>,
        selectedCategory: Category?,
    ): DeckSequencing {
        val previous = selected.lastOrNull()
        val sameSourcePenalty = if (previous?.article?.source?.id == candidate.article.source.id) {
            -8.0
        } else {
            0.0
        }
        val categoryPenalty = if (
            selectedCategory == null &&
            selected.size >= 2 &&
            previous?.article?.category == candidate.article.category &&
            selected[selected.lastIndex - 1].article.category == candidate.article.category
        ) {
            -5.0
        } else {
            0.0
        }
        return DeckSequencing(
            score = candidate.score.total + sameSourcePenalty + categoryPenalty,
            sameSourcePenalty = sameSourcePenalty,
            categoryPenalty = categoryPenalty,
        )
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
