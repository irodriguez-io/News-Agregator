package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import kotlin.math.roundToLong

enum class PreferenceEvent {
    NOT_INTERESTED,
    SAVE_FOR_LATER,
    FIRST_OPEN,
    MARK_READ,
}

object PreferenceLearning {
    private data class Delta(
        val source: Double,
        val topic: Double,
    )

    private val interactionDeltas = mapOf(
        PreferenceEvent.NOT_INTERESTED to Delta(source = -0.35, topic = -0.20),
        PreferenceEvent.SAVE_FOR_LATER to Delta(source = 0.45, topic = 0.30),
        PreferenceEvent.FIRST_OPEN to Delta(source = 0.10, topic = 0.05),
        PreferenceEvent.MARK_READ to Delta(source = 0.25, topic = 0.20),
    )

    fun apply(
        preferences: LocalState.Preferences,
        article: Article,
        event: PreferenceEvent,
    ): LocalState.Preferences {
        val delta = interactionDeltas.getValue(event)
        val sources = preferences.sources.applyToEntry(article.source.id, delta.source)
        val topics = uniqueTopicIds(article).fold(preferences.topics) { entries, topicId ->
            entries.applyToEntry(topicId, delta.topic)
        }
        return LocalState.Preferences(sources = sources, topics = topics)
    }

    fun reverse(
        preferences: LocalState.Preferences,
        article: Article,
        event: PreferenceEvent,
    ): LocalState.Preferences {
        val delta = interactionDeltas.getValue(event)
        val sources = preferences.sources.reverseFromEntry(article.source.id, delta.source)
        val topics = uniqueTopicIds(article).fold(preferences.topics) { entries, topicId ->
            entries.reverseFromEntry(topicId, delta.topic)
        }
        return LocalState.Preferences(sources = sources, topics = topics)
    }

    private fun Map<String, PreferenceEntry>.applyToEntry(
        key: String,
        delta: Double,
    ): Map<String, PreferenceEntry> {
        val current = this[key] ?: PreferenceEntry(weight = 0.0, interactions = 0)
        return this + (
            key to PreferenceEntry(
                weight = clampWeight(current.weight + delta),
                interactions = current.interactions + 1,
            )
        )
    }

    private fun Map<String, PreferenceEntry>.reverseFromEntry(
        key: String,
        delta: Double,
    ): Map<String, PreferenceEntry> {
        val current = this[key] ?: return this
        if (current.interactions <= 0) return this

        val next = PreferenceEntry(
            weight = clampWeight(current.weight - delta),
            interactions = maxOf(0, current.interactions - 1),
        )
        return if (next.interactions == 0 && next.weight == 0.0) {
            this - key
        } else {
            this + (key to next)
        }
    }

    private fun clampWeight(value: Double): Double =
        ((value * 1e10).roundToLong() / 1e10).coerceIn(-5.0, 5.0)

    private fun uniqueTopicIds(article: Article): List<String> =
        article.tags.map { tag -> tag.id }.distinct()
}
