package io.irodriguez.intentionalreading.domain.validation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DatasetValidatorTest {
    private val validator = DatasetValidator()

    @Test
    fun `the bundled dataset contract accepts one valid document and preserves file order`() {
        val second = article(2).with("title", JsonPrimitive("Second article"))
        val document = validDocument.withArticles(listOf(article(1), second))

        val result = assertIs<DatasetResult.Success>(validator.validate(document.bytes()))

        assertEquals(listOf(articleId(1), articleId(2)), result.dataset.articles.map { it.id })
    }

    @Test
    fun `an explicit null author is accepted while a missing author is rejected`() {
        assertIs<DatasetResult.Success>(validator.validate(validDocument.bytes()))

        val missingAuthor = validDocument.withArticle { it.without("author") }
        val failure = assertIs<DatasetResult.Failure>(validator.validate(missingAuthor.bytes()))

        assertEquals(DatasetErrorCode.MALFORMED_DATASET, failure.code)
    }

    @Test
    fun `a contract violating dataset claims nothing`() {
        val duplicateTag = tag("oauth")
        val sevenTags = (1..7).map { tag("topic_$it") }
        val fiveHundredOneArticles = (1..501).map(::article)
        val cases = listOf(
            InvalidCase("unknown top-level key") { it.with("unexpected", JsonPrimitive(true)) },
            InvalidCase("missing author key") { it.withArticle { article -> article.without("author") } },
            InvalidCase("19-character article ID") {
                it.withArticle { article -> article.with("id", JsonPrimitive("0123456789abcdef012")) }
            },
            InvalidCase("uppercase hexadecimal article ID") {
                it.withArticle { article -> article.with("id", JsonPrimitive("ABCDEF0123456789ABCD")) }
            },
            InvalidCase("empty title") { it.withArticle { article -> article.with("title", JsonPrimitive("")) } },
            InvalidCase("501-character title") {
                it.withArticle { article -> article.with("title", JsonPrimitive("a".repeat(501))) }
            },
            InvalidCase("punctuation-only title") {
                it.withArticle { article -> article.with("title", JsonPrimitive("—?! …")) }
            },
            InvalidCase("non-HTTP URL") {
                it.withArticle { article -> article.with("url", JsonPrimitive("ftp://example.com/article")) }
            },
            InvalidCase("unknown category") {
                it.withArticle { article -> article.with("category", JsonPrimitive("unknown")) }
            },
            InvalidCase("unknown content-type ID") {
                it.withArticle { article ->
                    article.withObject("contentType") { contentType ->
                        contentType.with("id", JsonPrimitive("unknown_type"))
                    }
                }
            },
            InvalidCase("malformed source ID") {
                it.withArticle { article ->
                    article.withObject("source") { source -> source.with("id", JsonPrimitive("bad-id")) }
                }
            },
            InvalidCase("duplicate tag ID") {
                it.withArticle { article -> article.with("tags", JsonArray(listOf(duplicateTag, duplicateTag))) }
            },
            InvalidCase("seven tags") {
                it.withArticle { article -> article.with("tags", JsonArray(sevenTags)) }
            },
            InvalidCase("impossible calendar date") {
                it.withArticle { article ->
                    article.with("publishedAt", JsonPrimitive("2026-02-30T12:00:00Z"))
                }
            },
            InvalidCase("timestamp without Z") {
                it.withArticle { article ->
                    article.with("publishedAt", JsonPrimitive("2026-02-28T12:00:00+00:00"))
                }
            },
            InvalidCase("broken score base sum") {
                it.withArticle { article ->
                    article.withObject("score") { score -> score.with("base", JsonPrimitive(90)) }
                }
            },
            InvalidCase("out-of-range score component") {
                it.withArticle { article ->
                    article.withObject("score") { score ->
                        score.with("sourceQuality", JsonPrimitive(51)).with("base", JsonPrimitive(92))
                    }
                }
            },
            InvalidCase("articleCount disagrees with articles") {
                it.withPipeline { pipeline -> pipeline.with("articleCount", JsonPrimitive(2)) }
            },
            InvalidCase("enabled source count disagrees with successful plus failed") {
                it.withPipeline { pipeline -> pipeline.with("enabledSourceCount", JsonPrimitive(2)) }
            },
            InvalidCase("duplicate article ID") {
                it.withArticles(listOf(article(1), article(1)))
            },
            InvalidCase("501 articles") { it.withArticles(fiveHundredOneArticles) },
        )

        cases.forEach { case ->
            val result = validator.validate(case.mutate(validDocument).bytes())
            val failure = assertIs<DatasetResult.Failure>(result, case.name)
            assertEquals(DatasetErrorCode.MALFORMED_DATASET, failure.code, case.name)
        }
    }

    @Test
    fun `an unsupported schema version is distinguished from a malformed dataset`() {
        val unsupported = validDocument.with("schemaVersion", JsonPrimitive(2))

        val failure = assertIs<DatasetResult.Failure>(validator.validate(unsupported.bytes()))

        assertEquals(DatasetErrorCode.UNSUPPORTED_SCHEMA, failure.code)
    }

    @Test
    fun `malformed JSON never throws across the data boundary`() {
        val result = validator.validate("{not json".encodeToByteArray())

        assertEquals(
            DatasetErrorCode.MALFORMED_DATASET,
            assertIs<DatasetResult.Failure>(result).code,
        )
    }

    @Test
    fun `readable title regex has no options because Android rejects the Unicode character class flag`() {
        val field = DatasetValidator::class.java.getDeclaredField("READABLE_TEXT_PATTERN")
        field.isAccessible = true

        // Android's java.util.regex.Pattern rejects UNICODE_CHARACTER_CLASS during class initialization.
        val pattern = assertIs<Regex>(field.get(null))
        assertTrue(pattern.options.isEmpty())
    }

    @Test
    fun `Unicode separators and punctuation stay unreadable while non ASCII letters are accepted`() {
        val unreadable = validDocument.withArticle { article ->
            article.with("title", JsonPrimitive("\u00A0\u2003\u202F—。"))
        }
        assertIs<DatasetResult.Failure>(validator.validate(unreadable.bytes()))

        val readable = validDocument.withArticle { article ->
            article.with("title", JsonPrimitive("Identité numérique — São Paulo"))
        }
        assertIs<DatasetResult.Success>(validator.validate(readable.bytes()))
    }

    private data class InvalidCase(
        val name: String,
        val mutate: (JsonObject) -> JsonObject,
    )

    private companion object {
        private val strictJson = Json

        private val validDocument = Json.parseToJsonElement(
            """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-02-28T12:00:00Z",
              "pipeline": {
                "enabledSourceCount": 1,
                "successfulSourceCount": 1,
                "failedSourceCount": 0,
                "articleCount": 1
              },
              "articles": []
            }
            """.trimIndent(),
        ).jsonObject.withArticles(listOf(article(1)))

        private fun article(index: Int): JsonObject = JsonObject(
            mapOf(
                "id" to JsonPrimitive(articleId(index)),
                "title" to JsonPrimitive("A readable article title $index"),
                "url" to JsonPrimitive("https://example.com/articles/$index"),
                "source" to JsonObject(
                    mapOf(
                        "id" to JsonPrimitive("ietf_oauth"),
                        "name" to JsonPrimitive("IETF OAuth WG"),
                    ),
                ),
                "category" to JsonPrimitive("iam"),
                "publishedAt" to JsonPrimitive("2026-02-28T11:00:00Z"),
                "author" to JsonNull,
                "excerpt" to JsonPrimitive("A plain-text excerpt."),
                "readingTimeMinutes" to JsonNull,
                "tags" to JsonArray(listOf(tag("oauth"))),
                "contentType" to JsonObject(
                    mapOf(
                        "id" to JsonPrimitive("standards_update"),
                        "label" to JsonPrimitive("Standards Update"),
                    ),
                ),
                "score" to JsonObject(
                    mapOf(
                        "base" to JsonPrimitive(91),
                        "sourceQuality" to JsonPrimitive(50),
                        "contentType" to JsonPrimitive(20),
                        "freshness" to JsonPrimitive(15),
                        "topicSignal" to JsonPrimitive(5),
                        "metadata" to JsonPrimitive(1),
                    ),
                ),
            ),
        )

        private fun tag(id: String): JsonObject = JsonObject(
            mapOf(
                "id" to JsonPrimitive(id),
                "label" to JsonPrimitive("Topic $id"),
            ),
        )

        private fun articleId(index: Int): String = index.toString(16).padStart(20, '0')

        private fun JsonObject.bytes(): ByteArray = strictJson.encodeToString(JsonElement.serializer(), this)
            .encodeToByteArray()

        private fun JsonObject.with(key: String, value: JsonElement): JsonObject =
            JsonObject(toMutableMap().apply { put(key, value) })

        private fun JsonObject.without(key: String): JsonObject =
            JsonObject(toMutableMap().apply { remove(key) })

        private fun JsonObject.withObject(
            key: String,
            transform: (JsonObject) -> JsonObject,
        ): JsonObject = with(key, transform(getValue(key).jsonObject))

        private fun JsonObject.withArticle(transform: (JsonObject) -> JsonObject): JsonObject {
            val articles = getValue("articles").jsonArray.toMutableList()
            articles[0] = transform(articles[0].jsonObject)
            return with("articles", JsonArray(articles))
        }

        private fun JsonObject.withPipeline(transform: (JsonObject) -> JsonObject): JsonObject =
            withObject("pipeline", transform)

        private fun JsonObject.withArticles(articles: List<JsonObject>): JsonObject =
            with("articles", JsonArray(articles)).withPipeline { pipeline ->
                pipeline.with("articleCount", JsonPrimitive(articles.size))
            }
    }
}
