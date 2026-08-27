package io.irodriguez.intentionalreading.domain.ranking

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.LocalState

data class PersonalizedScore(
    val total: Double,
    val base: Int,
    val sourcePreference: Double,
    val topicPreference: Double,
    val exploration: Double,
) {
    companion object {
        fun calculate(
            article: Article,
            preferences: LocalState.Preferences,
        ): PersonalizedScore {
            val sourceEntry = preferences.sources[article.source.id]
            val sourcePreference = sourceEntry?.weight ?: 0.0
            val uniqueTopicIds = article.tags.map { tag -> tag.id }.distinct()
            val topicPreference = uniqueTopicIds
                .sumOf { topicId -> preferences.topics[topicId]?.weight ?: 0.0 }
                .coerceIn(-6.0, 6.0)
            val sourceExploration = sourceExploration(sourceEntry?.interactions ?: 0)
            val topicExploration = if (uniqueTopicIds.isEmpty()) {
                0.0
            } else {
                val lowestInteractions = uniqueTopicIds.minOf { topicId ->
                    preferences.topics[topicId]?.interactions ?: 0
                }
                topicExploration(lowestInteractions)
            }
            val exploration = minOf(3.0, sourceExploration + topicExploration)
            val base = article.score.base
            return PersonalizedScore(
                total = base + sourcePreference + topicPreference + exploration,
                base = base,
                sourcePreference = sourcePreference,
                topicPreference = topicPreference,
                exploration = exploration,
            )
        }

        private fun sourceExploration(interactions: Int): Double = when {
            interactions <= 0 -> 3.0
            interactions == 1 -> 2.0
            interactions == 2 -> 1.0
            else -> 0.0
        }

        private fun topicExploration(interactions: Int): Double = when {
            interactions <= 0 -> 2.0
            interactions == 1 -> 1.0
            interactions == 2 -> 0.5
            else -> 0.0
        }
    }
}
