package io.irodriguez.intentionalreading.data.local.state

import io.irodriguez.intentionalreading.data.dto.ArticleContentTypeDto
import io.irodriguez.intentionalreading.data.dto.ArticleDto
import io.irodriguez.intentionalreading.data.dto.ArticleScoreDto
import io.irodriguez.intentionalreading.data.dto.ArticleSourceDto
import io.irodriguez.intentionalreading.data.dto.ArticleTagDto
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.validation.DatasetValidator
import io.irodriguez.intentionalreading.domain.validation.LocalStateErrorCode
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateSource
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidator
import io.irodriguez.intentionalreading.domain.validation.LocalStateViolation
import kotlinx.serialization.encodeToString

internal object LocalStateMapper {
    fun migrate(
        dto: LocalStateDto,
        fromVersion: Int,
        toVersion: Int = LocalState.SCHEMA_VERSION,
    ): LocalStateResult {
        if (fromVersion != LocalState.SCHEMA_VERSION || toVersion != LocalState.SCHEMA_VERSION) {
            return LocalStateResult.Failure(
                code = LocalStateErrorCode.UNSUPPORTED_SCHEMA,
                message = "No Local State migration exists from version $fromVersion to $toVersion",
                state = LocalState.default(),
                path = "state.schemaVersion",
            )
        }
        return try {
            LocalStateResult.Success(
                state = LocalStateValidator().validateVersionOne(dto),
                source = LocalStateSource.STORAGE,
            )
        } catch (failure: LocalStateViolation) {
            LocalStateResult.Failure(
                code = LocalStateErrorCode.INVALID_STATE,
                message = failure.message ?: "Stored local state is structurally invalid",
                state = LocalState.default(),
                path = failure.path,
            )
        } catch (failure: DatasetValidator.ContractViolation) {
            LocalStateResult.Failure(
                code = LocalStateErrorCode.INVALID_STATE,
                message = failure.message ?: "Stored Article snapshot is structurally invalid",
                state = LocalState.default(),
                path = failure.path,
            )
        } catch (failure: Exception) {
            LocalStateResult.Failure(
                code = LocalStateErrorCode.INVALID_STATE,
                message = "Stored local state is structurally invalid",
                state = LocalState.default(),
            )
        }
    }

    fun validate(state: LocalState): LocalStateResult = try {
        val dto = toDto(state)
        migrate(dto, fromVersion = state.schemaVersion)
    } catch (failure: Exception) {
        LocalStateResult.Failure(
            code = LocalStateErrorCode.INVALID_STATE,
            message = "Local state is structurally invalid",
        )
    }

    fun encode(state: LocalState): ByteArray =
        LocalStateJson.encodeToString(toDto(state)).encodeToByteArray()

    private fun toDto(state: LocalState): LocalStateDto = LocalStateDto(
        schemaVersion = state.schemaVersion,
        preferences = PreferencesDto(
            sources = state.preferences.sources.mapValues { (_, entry) -> entry.toDto() },
            topics = state.preferences.topics.mapValues { (_, entry) -> entry.toDto() },
        ),
        articles = state.articles.mapValues { (_, record) -> record.toDto() },
        settings = SettingsDto(appearance = state.settings.appearance.wireValue),
        session = SessionDto(lastCategory = state.session.lastCategory?.id ?: "all"),
    )

    private fun PreferenceEntry.toDto(): PreferenceEntryDto = PreferenceEntryDto(
        weight = weight,
        interactions = interactions,
    )

    private fun ArticleRecord.toDto(): ArticleRecordDto = ArticleRecordDto(
        article = article.toDto(),
        status = status.wireValue ?: error("Unseen articles cannot be persisted"),
        firstSeenAt = firstSeenAt.toString(),
        openedAt = openedAt?.toString(),
        savedAt = savedAt?.toString(),
        dismissedAt = dismissedAt?.toString(),
        readAt = readAt?.toString(),
        signalsApplied = SignalsAppliedDto(
            opened = signalsApplied.opened,
            saved = signalsApplied.saved,
            dismissed = signalsApplied.dismissed,
            read = signalsApplied.read,
        ),
    )

    private fun Article.toDto(): ArticleDto = ArticleDto(
        id = id,
        title = title,
        url = url,
        source = ArticleSourceDto(id = source.id, name = source.name),
        category = category.id,
        publishedAt = publishedAt?.toString(),
        author = author,
        excerpt = excerpt,
        readingTimeMinutes = readingTimeMinutes,
        tags = tags.map { ArticleTagDto(id = it.id, label = it.label) },
        contentType = ArticleContentTypeDto(id = contentType.id.id, label = contentType.label),
        score = ArticleScoreDto(
            base = score.base,
            sourceQuality = score.sourceQuality,
            contentType = score.contentType,
            freshness = score.freshness,
            topicSignal = score.topicSignal,
            metadata = score.metadata,
        ),
    )
}
