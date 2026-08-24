package io.irodriguez.intentionalreading.domain.validation

import io.irodriguez.intentionalreading.data.local.state.ArticleRecordDto
import io.irodriguez.intentionalreading.data.local.state.LocalStateDto
import io.irodriguez.intentionalreading.data.local.state.LocalStateJson
import io.irodriguez.intentionalreading.data.local.state.LocalStateMapper
import io.irodriguez.intentionalreading.data.local.state.PreferenceEntryDto
import io.irodriguez.intentionalreading.domain.model.Appearance
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

class LocalStateValidator {
    fun validate(bytes: ByteArray): LocalStateResult {
        val decoded = try {
            decodeUtf8(bytes)
        } catch (failure: Exception) {
            return malformedJson()
        }
        val element = try {
            LocalStateJson.parseToJsonElement(decoded)
        } catch (failure: Exception) {
            return malformedJson()
        }
        if (element !is JsonObject || element.containsDangerousKey()) {
            return invalidState()
        }
        val schemaVersion = element["schemaVersion"] as? JsonPrimitive
        if (schemaVersion == null || schemaVersion.isString || schemaVersion.intOrNull == null) {
            return invalidState()
        }
        val dto = try {
            LocalStateJson.decodeFromJsonElement(LocalStateDto.serializer(), element)
        } catch (failure: Exception) {
            return invalidState()
        }
        return LocalStateMapper.migrate(
            dto = dto,
            fromVersion = dto.schemaVersion,
            toVersion = LocalState.SCHEMA_VERSION,
        )
    }

    internal fun validateVersionOne(dto: LocalStateDto): LocalState {
        val preferences = LocalState.Preferences(
            sources = validatePreferenceMap(dto.preferences.sources, "state.preferences.sources"),
            topics = validatePreferenceMap(dto.preferences.topics, "state.preferences.topics"),
        )
        val articleValidator = DatasetValidator()
        val articles = dto.articles.map { (articleId, record) ->
            if (!DatasetValidator.ARTICLE_ID_PATTERN.matches(articleId)) {
                invalid("state.articles", "contains an invalid Article ID")
            }
            articleId to validateRecord(record, articleId, articleValidator)
        }.toMap()
        val appearance = Appearance.fromWireValue(dto.settings.appearance)
            ?: invalid("state.settings.appearance", "is invalid")
        val lastCategory = when (dto.session.lastCategory) {
            "all" -> null
            else -> Category.fromId(dto.session.lastCategory)
                ?: invalid("state.session.lastCategory", "is invalid")
        }
        return LocalState(
            schemaVersion = LocalState.SCHEMA_VERSION,
            preferences = preferences,
            articles = articles,
            settings = LocalState.Settings(appearance),
            session = LocalState.Session(lastCategory),
        )
    }

    private fun validatePreferenceMap(
        entries: Map<String, PreferenceEntryDto>,
        path: String,
    ): Map<String, PreferenceEntry> = entries.map { (key, entry) ->
        if (!DatasetValidator.IDENTIFIER_PATTERN.matches(key)) {
            invalid(path, "contains an invalid key")
        }
        if (!entry.weight.isFinite() || entry.weight < -5.0 || entry.weight > 5.0) {
            invalid("$path.$key.weight", "is outside V1 bounds")
        }
        if (entry.interactions < 0) {
            invalid("$path.$key.interactions", "is invalid")
        }
        key to PreferenceEntry(weight = entry.weight, interactions = entry.interactions)
    }.toMap()

    private fun validateRecord(
        dto: ArticleRecordDto,
        articleId: String,
        articleValidator: DatasetValidator,
    ): ArticleRecord {
        val path = "state.articles.$articleId"
        val article = articleValidator.validateArticle(dto.article, "$path.article")
        if (article.id != articleId) {
            invalid("$path.article.id", "does not match its map key")
        }
        val status = ArticleStatus.fromWireValue(dto.status)
            ?.takeUnless { it == ArticleStatus.UNSEEN }
            ?: invalid("$path.status", "is invalid")
        val firstSeenAt = timestamp(dto.firstSeenAt, "$path.firstSeenAt")
        val openedAt = nullableTimestamp(dto.openedAt, "$path.openedAt")
        val savedAt = nullableTimestamp(dto.savedAt, "$path.savedAt")
        val dismissedAt = nullableTimestamp(dto.dismissedAt, "$path.dismissedAt")
        val readAt = nullableTimestamp(dto.readAt, "$path.readAt")
        val signals = SignalsApplied(
            opened = dto.signalsApplied.opened,
            saved = dto.signalsApplied.saved,
            dismissed = dto.signalsApplied.dismissed,
            read = dto.signalsApplied.read,
        )

        if (signals.opened != (openedAt != null)) {
            invalid(path, "has inconsistent Open signal metadata")
        }
        if (signals.dismissed && status != ArticleStatus.DISMISSED) {
            invalid(path, "has an inconsistent Dismiss signal")
        }
        if (signals.read != (status == ArticleStatus.READ)) {
            invalid(path, "has an inconsistent Read signal")
        }

        val requiredTimestamp = when (status) {
            ArticleStatus.OPENED -> openedAt
            ArticleStatus.SAVED -> savedAt
            ArticleStatus.DISMISSED -> dismissedAt
            ArticleStatus.READ -> readAt
            ArticleStatus.UNSEEN -> null
        }
        if (requiredTimestamp == null) {
            val field = when (status) {
                ArticleStatus.OPENED -> "openedAt"
                ArticleStatus.SAVED -> "savedAt"
                ArticleStatus.DISMISSED -> "dismissedAt"
                ArticleStatus.READ -> "readAt"
                ArticleStatus.UNSEEN -> "status"
            }
            invalid("$path.$field", "is required")
        }
        if (status != ArticleStatus.SAVED && savedAt != null) {
            invalid("$path.savedAt", "is not currently applicable")
        }
        if (status != ArticleStatus.DISMISSED && dismissedAt != null) {
            invalid("$path.dismissedAt", "is not currently applicable")
        }
        if (status != ArticleStatus.READ && readAt != null) {
            invalid("$path.readAt", "is not currently applicable")
        }
        listOf(
            "openedAt" to openedAt,
            "savedAt" to savedAt,
            "dismissedAt" to dismissedAt,
            "readAt" to readAt,
        ).forEach { (field, value) ->
            if (value != null && value < firstSeenAt) {
                invalid("$path.$field", "predates firstSeenAt")
            }
        }

        return ArticleRecord(
            article = article,
            status = status,
            firstSeenAt = firstSeenAt,
            openedAt = openedAt,
            savedAt = savedAt,
            dismissedAt = dismissedAt,
            readAt = readAt,
            signalsApplied = signals,
        )
    }

    private fun timestamp(value: String, path: String): Instant =
        DatasetValidator.parseUtcTimestampOrNull(value)
            ?: invalid(path, "must be a UTC ISO-8601 timestamp")

    private fun nullableTimestamp(value: String?, path: String): Instant? =
        value?.let { timestamp(it, path) }

    private fun decodeUtf8(bytes: ByteArray): String = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

    private fun JsonElement.containsDangerousKey(): Boolean = when (this) {
        is JsonObject -> entries.any { (key, value) ->
            key in DANGEROUS_KEYS || value.containsDangerousKey()
        }
        is JsonArray -> any { it.containsDangerousKey() }
        else -> false
    }

    private fun malformedJson(): LocalStateResult.Failure = LocalStateResult.Failure(
        code = LocalStateErrorCode.MALFORMED_JSON,
        message = "Stored local state is malformed",
        state = LocalState.default(),
    )

    private fun invalidState(): LocalStateResult.Failure = LocalStateResult.Failure(
        code = LocalStateErrorCode.INVALID_STATE,
        message = "Stored local state is structurally invalid",
        state = LocalState.default(),
    )

    private fun invalid(path: String, message: String): Nothing =
        throw LocalStateViolation(path, "$path $message")

    private companion object {
        val DANGEROUS_KEYS = setOf("__proto__", "prototype", "constructor")
    }
}

internal class LocalStateViolation(
    val path: String,
    message: String,
) : Exception(message)
