package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry

object PreferenceReconciliation {
    fun reconcile(state: LocalState): LocalState {
        val records = state.articles.values.sortedBy { record -> record.article.id }
        val claimedSources = mutableMapOf<String, Int>()
        val claimedTopics = mutableMapOf<String, Int>()

        records.forEach { record ->
            val signalCount = appliedEvents(record).size
            if (signalCount > 0) {
                claimedSources.add(record.article.source.id, signalCount)
                record.article.tags.map { tag -> tag.id }.distinct().forEach { topicId ->
                    claimedTopics.add(topicId, signalCount)
                }
            }
        }

        var preferences = correctOverCounts(
            preferences = state.preferences,
            claimedSources = claimedSources,
            claimedTopics = claimedTopics,
        )
        val missingSources = claimedSources.mapValuesTo(mutableMapOf()) { (sourceId, claimed) ->
            (claimed - (preferences.sources[sourceId]?.interactions ?: 0)).coerceAtLeast(0)
        }
        val missingTopics = claimedTopics.mapValuesTo(mutableMapOf()) { (topicId, claimed) ->
            (claimed - (preferences.topics[topicId]?.interactions ?: 0)).coerceAtLeast(0)
        }

        records.forEach { record ->
            appliedEvents(record).forEach { event ->
                val sourceId = record.article.source.id
                val applySource = missingSources.getOrDefault(sourceId, 0) > 0
                val topicIds = record.article.tags
                    .map { tag -> tag.id }
                    .distinct()
                    .filter { topicId -> missingTopics.getOrDefault(topicId, 0) > 0 }

                if (applySource || topicIds.isNotEmpty()) {
                    val applied = PreferenceLearning.apply(preferences, record.article, event)
                    val sources = if (applySource) {
                        preferences.sources + (sourceId to applied.sources.getValue(sourceId))
                    } else {
                        preferences.sources
                    }
                    val topics = topicIds.fold(preferences.topics) { entries, topicId ->
                        entries + (topicId to applied.topics.getValue(topicId))
                    }
                    preferences = LocalState.Preferences(sources = sources, topics = topics)

                    if (applySource) missingSources[sourceId] = missingSources.getValue(sourceId) - 1
                    topicIds.forEach { topicId ->
                        missingTopics[topicId] = missingTopics.getValue(topicId) - 1
                    }
                }
            }
        }

        return if (preferences == state.preferences) state else state.copy(preferences = preferences)
    }

    private fun appliedEvents(record: ArticleRecord): List<PreferenceEvent> = buildList {
        if (record.signalsApplied.opened) add(PreferenceEvent.FIRST_OPEN)
        if (record.signalsApplied.saved) add(PreferenceEvent.SAVE_FOR_LATER)
        if (record.signalsApplied.dismissed) add(PreferenceEvent.NOT_INTERESTED)
        if (record.signalsApplied.read) add(PreferenceEvent.MARK_READ)
    }

    private fun correctOverCounts(
        preferences: LocalState.Preferences,
        claimedSources: Map<String, Int>,
        claimedTopics: Map<String, Int>,
    ): LocalState.Preferences = LocalState.Preferences(
        sources = preferences.sources.correctOverCounts(claimedSources),
        topics = preferences.topics.correctOverCounts(claimedTopics),
    )

    private fun Map<String, PreferenceEntry>.correctOverCounts(
        claimed: Map<String, Int>,
    ): Map<String, PreferenceEntry> = mapValues { (id, entry) ->
        val claimedCount = claimed[id] ?: 0
        if (entry.interactions > claimedCount) entry.copy(interactions = claimedCount) else entry
    }

    private fun MutableMap<String, Int>.add(key: String, count: Int) {
        this[key] = getOrDefault(key, 0) + count
    }
}
