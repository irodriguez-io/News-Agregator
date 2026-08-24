package io.irodriguez.intentionalreading.data.local.state

import io.irodriguez.intentionalreading.data.dto.ArticleDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class LocalStateDto(
    val schemaVersion: Int,
    val preferences: PreferencesDto,
    val articles: Map<String, ArticleRecordDto>,
    val settings: SettingsDto,
    val session: SessionDto,
)

@Serializable
internal data class PreferencesDto(
    val sources: Map<String, PreferenceEntryDto>,
    val topics: Map<String, PreferenceEntryDto>,
)

@Serializable
internal data class PreferenceEntryDto(
    val weight: Double,
    val interactions: Int,
)

@Serializable
internal data class ArticleRecordDto(
    val article: ArticleDto,
    val status: String,
    val firstSeenAt: String,
    val openedAt: String?,
    val savedAt: String?,
    val dismissedAt: String?,
    val readAt: String?,
    val signalsApplied: SignalsAppliedDto,
)

@Serializable
internal data class SignalsAppliedDto(
    val opened: Boolean,
    val saved: Boolean,
    val dismissed: Boolean,
    val read: Boolean,
)

@Serializable
internal data class SettingsDto(
    val appearance: String,
)

@Serializable
internal data class SessionDto(
    val lastCategory: String,
)

@OptIn(ExperimentalSerializationApi::class)
internal val LocalStateJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = true
    isLenient = false
    coerceInputValues = false
    allowTrailingComma = false
    allowComments = false
}
