package io.irodriguez.intentionalreading.domain.validation

import io.irodriguez.intentionalreading.data.DatasetJson
import io.irodriguez.intentionalreading.data.dto.ArticleDatasetDto
import io.irodriguez.intentionalreading.data.dto.ArticleDto
import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleDataset
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.PipelineMetadata
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.util.regex.Pattern
import kotlinx.serialization.decodeFromString

class DatasetValidator {
    fun validate(bytes: ByteArray): DatasetResult = try {
        val json = decodeUtf8(bytes)
        validate(DatasetJson.decodeFromString<ArticleDatasetDto>(json))
    } catch (failure: ContractViolation) {
        DatasetResult.Failure(
            code = failure.code,
            message = failure.message ?: "Dataset validation failed",
            path = failure.path,
        )
    } catch (failure: Exception) {
        DatasetResult.Failure(
            code = DatasetErrorCode.MALFORMED_DATASET,
            message = "The dataset is not valid ArticleDataset v1 JSON",
        )
    }

    private fun validate(dto: ArticleDatasetDto): DatasetResult {
        if (dto.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            invalid(
                path = "dataset.schemaVersion",
                message = "Unsupported ArticleDataset schema version",
                code = DatasetErrorCode.UNSUPPORTED_SCHEMA,
            )
        }
        expectTimestamp(dto.generatedAt, "dataset.generatedAt")
        expectNonNegative(dto.pipeline.enabledSourceCount, "dataset.pipeline.enabledSourceCount")
        expectNonNegative(dto.pipeline.successfulSourceCount, "dataset.pipeline.successfulSourceCount")
        expectNonNegative(dto.pipeline.failedSourceCount, "dataset.pipeline.failedSourceCount")
        expectNonNegative(dto.pipeline.articleCount, "dataset.pipeline.articleCount")
        if (dto.pipeline.successfulSourceCount + dto.pipeline.failedSourceCount !=
            dto.pipeline.enabledSourceCount
        ) {
            invalid("dataset.pipeline", "source counts are inconsistent")
        }
        if (dto.pipeline.articleCount != dto.articles.size) {
            invalid("dataset.articles", "does not match pipeline.articleCount")
        }
        if (dto.articles.size > MAX_ARTICLES) {
            invalid("dataset.articles", "must contain at most $MAX_ARTICLES articles")
        }

        val seenArticleIds = mutableSetOf<String>()
        val articles = dto.articles.mapIndexed { index, article ->
            val path = "dataset.articles[$index]"
            val validated = validateArticle(article, path)
            if (!seenArticleIds.add(validated.id)) {
                invalid("$path.id", "must be unique")
            }
            validated
        }

        return DatasetResult.Success(
            ArticleDataset(
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                generatedAt = dto.generatedAt,
                pipeline = PipelineMetadata(
                    enabledSourceCount = dto.pipeline.enabledSourceCount,
                    successfulSourceCount = dto.pipeline.successfulSourceCount,
                    failedSourceCount = dto.pipeline.failedSourceCount,
                    articleCount = dto.pipeline.articleCount,
                ),
                articles = articles.toList(),
            ),
        )
    }

    private fun validateArticle(dto: ArticleDto, path: String): Article {
        expectPattern(dto.id, ARTICLE_ID_PATTERN, "$path.id")
        expectLength(dto.title, minimum = 1, maximum = 500, path = "$path.title")
        if (!READABLE_TEXT_PATTERN.matcher(dto.title).find()) {
            invalid("$path.title", "must contain readable text")
        }
        if (!isSafeHttpUrl(dto.url)) {
            invalid("$path.url", "must be an external HTTP/HTTPS URL")
        }

        expectPattern(dto.source.id, IDENTIFIER_PATTERN, "$path.source.id")
        expectLength(dto.source.name, minimum = 1, maximum = 200, path = "$path.source.name")
        val category = Category.fromId(dto.category)
            ?: invalid("$path.category", "is invalid")
        val publishedAt = dto.publishedAt?.let { parseTimestamp(it, "$path.publishedAt") }
        dto.author?.let { expectLength(it, minimum = 1, maximum = 200, path = "$path.author") }
        expectLength(dto.excerpt, minimum = 0, maximum = 800, path = "$path.excerpt")
        dto.readingTimeMinutes?.let {
            if (it < 1) invalid("$path.readingTimeMinutes", "is invalid")
        }

        if (dto.tags.size > MAX_TAGS) {
            invalid("$path.tags", "must contain at most $MAX_TAGS tags")
        }
        val seenTagIds = mutableSetOf<String>()
        val tags = dto.tags.mapIndexed { index, tag ->
            val tagPath = "$path.tags[$index]"
            expectPattern(tag.id, IDENTIFIER_PATTERN, "$tagPath.id")
            if (!seenTagIds.add(tag.id)) invalid("$tagPath.id", "must be unique")
            expectLength(tag.label, minimum = 1, maximum = 200, path = "$tagPath.label")
            ArticleTag(id = tag.id, label = tag.label)
        }

        val contentTypeId = ContentTypeId.fromId(dto.contentType.id)
            ?: invalid("$path.contentType.id", "is invalid")
        expectLength(dto.contentType.label, minimum = 1, maximum = 200, path = "$path.contentType.label")

        expectRange(dto.score.base, 0, 100, "$path.score.base")
        expectRange(dto.score.sourceQuality, 0, 50, "$path.score.sourceQuality")
        expectRange(dto.score.contentType, 0, 20, "$path.score.contentType")
        expectRange(dto.score.freshness, 0, 15, "$path.score.freshness")
        expectRange(dto.score.topicSignal, 0, 10, "$path.score.topicSignal")
        expectRange(dto.score.metadata, 0, 5, "$path.score.metadata")
        val componentSum = dto.score.sourceQuality + dto.score.contentType + dto.score.freshness +
            dto.score.topicSignal + dto.score.metadata
        if (dto.score.base != componentSum) {
            invalid("$path.score.base", "must equal its component sum")
        }

        return Article(
            id = dto.id,
            title = dto.title,
            url = dto.url,
            source = ArticleSource(id = dto.source.id, name = dto.source.name),
            category = category,
            publishedAt = publishedAt,
            author = dto.author,
            excerpt = dto.excerpt,
            readingTimeMinutes = dto.readingTimeMinutes,
            tags = tags.toList(),
            contentType = ArticleContentType(id = contentTypeId, label = dto.contentType.label),
            score = ArticleScore(
                base = dto.score.base,
                sourceQuality = dto.score.sourceQuality,
                contentType = dto.score.contentType,
                freshness = dto.score.freshness,
                topicSignal = dto.score.topicSignal,
                metadata = dto.score.metadata,
            ),
        )
    }

    private fun decodeUtf8(bytes: ByteArray): String = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes))
        .toString()

    private fun expectTimestamp(value: String, path: String) {
        parseTimestamp(value, path)
    }

    private fun parseTimestamp(value: String, path: String): Instant {
        val match = UTC_TIMESTAMP_PATTERN.matchEntire(value)
            ?: invalid(path, "must be a UTC ISO-8601 timestamp")
        try {
            LocalDateTime.of(
                match.groupValues[1].toInt(),
                match.groupValues[2].toInt(),
                match.groupValues[3].toInt(),
                match.groupValues[4].toInt(),
                match.groupValues[5].toInt(),
                match.groupValues[6].toInt(),
            )
        } catch (failure: DateTimeException) {
            invalid(path, "must be a real UTC calendar timestamp")
        }
        return Instant.parse(value)
    }

    private fun expectPattern(value: String, pattern: Regex, path: String) {
        if (!pattern.matches(value)) invalid(path, "is invalid")
    }

    private fun expectLength(value: String, minimum: Int, maximum: Int, path: String) {
        if (value.length !in minimum..maximum) invalid(path, "is invalid")
    }

    private fun expectRange(value: Int, minimum: Int, maximum: Int, path: String) {
        if (value !in minimum..maximum) invalid(path, "is invalid")
    }

    private fun expectNonNegative(value: Int, path: String) {
        if (value < 0) invalid(path, "is invalid")
    }

    private fun isSafeHttpUrl(value: String): Boolean = try {
        val uri = URI(value)
        (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
    } catch (failure: Exception) {
        false
    }

    private fun invalid(
        path: String,
        message: String,
        code: DatasetErrorCode = DatasetErrorCode.MALFORMED_DATASET,
    ): Nothing = throw ContractViolation(code, path, "$path $message")

    private class ContractViolation(
        val code: DatasetErrorCode,
        val path: String,
        message: String,
    ) : Exception(message)

    private companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val MAX_ARTICLES = 500
        const val MAX_TAGS = 6

        val ARTICLE_ID_PATTERN = Regex("^[0-9a-f]{20}$")
        val IDENTIFIER_PATTERN = Regex("^[a-z0-9][a-z0-9_]{0,99}$")
        val UTC_TIMESTAMP_PATTERN = Regex(
            "^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?Z$",
        )
        val READABLE_TEXT_PATTERN: Pattern = Pattern.compile(
            "[^\\s\\p{P}\\p{S}]",
            Pattern.UNICODE_CHARACTER_CLASS,
        )
    }
}
