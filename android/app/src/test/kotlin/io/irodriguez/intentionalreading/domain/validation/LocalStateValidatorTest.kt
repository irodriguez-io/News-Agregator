package io.irodriguez.intentionalreading.domain.validation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LocalStateValidatorTest {
    private val validator = LocalStateValidator()

    @Test
    fun `one valid state document maps through the migration entry point`() {
        val result = validator.validate(validDocument.bytes())

        assertIs<LocalStateResult.Success>(result)
    }

    @Test
    fun `the validator rejects one-field contract violations without throwing`() {
        val cases = listOf(
            InvalidCase("unknown top-level key") { it.with("unexpected", JsonPrimitive(true)) },
            InvalidCase("missing top-level key") { it.without("session") },
            InvalidCase("schemaVersion 0", LocalStateErrorCode.UNSUPPORTED_SCHEMA) {
                it.with("schemaVersion", JsonPrimitive(0))
            },
            InvalidCase("schemaVersion 2", LocalStateErrorCode.UNSUPPORTED_SCHEMA) {
                it.with("schemaVersion", JsonPrimitive(2))
            },
            InvalidCase("schemaVersion string") { it.with("schemaVersion", JsonPrimitive("1")) },
            InvalidCase("unknown preferences key") {
                it.withObject("preferences") { value -> value.with("unexpected", JsonObject(emptyMap())) }
            },
            InvalidCase("unknown settings key") {
                it.withObject("settings") { value -> value.with("unexpected", JsonPrimitive(true)) }
            },
            InvalidCase("unknown session key") {
                it.withObject("session") { value -> value.with("unexpected", JsonPrimitive(true)) }
            },
            InvalidCase("unknown record key") {
                it.withRecord { value -> value.with("unexpected", JsonPrimitive(true)) }
            },
            InvalidCase("preference weight below the lower bound") {
                it.withSourcePreference { entry -> entry.with("weight", JsonPrimitive(-5.1)) }
            },
            InvalidCase("preference weight above the upper bound") {
                it.withSourcePreference { entry -> entry.with("weight", JsonPrimitive(5.1)) }
            },
            InvalidCase("preference weight is not a number") {
                it.withSourcePreference { entry -> entry.with("weight", JsonPrimitive("heavy")) }
            },
            InvalidCase("negative preference interactions") {
                it.withSourcePreference { entry -> entry.with("interactions", JsonPrimitive(-1)) }
            },
            InvalidCase("non-integer preference interactions") {
                it.withSourcePreference { entry -> entry.with("interactions", JsonPrimitive(1.5)) }
            },
            InvalidCase("preference key is not an identifier") {
                it.withObject("preferences") { preferences ->
                    preferences.withObject("sources") { sources ->
                        JsonObject(mapOf("bad-key" to sources.getValue(SOURCE_ID)))
                    }
                }
            },
            InvalidCase("19-character Article ID") { it.withRecordKey("0123456789abcdef012") },
            InvalidCase("uppercase hexadecimal Article ID") { it.withRecordKey("ABCDEF0123456789ABCD") },
            InvalidCase("record key disagrees with Article snapshot") {
                it.withRecordKey("00000000000000000002")
            },
            InvalidCase("unseen is not a persisted status") {
                it.withRecord { record -> record.with("status", JsonPrimitive("unseen")) }
            },
            InvalidCase("archived is not a persisted status") {
                it.withRecord { record -> record.with("status", JsonPrimitive("archived")) }
            },
            InvalidCase("non-UTC timestamp") {
                it.withRecord { record ->
                    record.with("firstSeenAt", JsonPrimitive("2026-08-20T10:00:00+00:00"))
                }
            },
            InvalidCase("non-ISO-8601 timestamp") {
                it.withRecord { record -> record.with("firstSeenAt", JsonPrimitive("20 August 2026")) }
            },
            InvalidCase("firstSeenAt null") {
                it.withRecord { record -> record.with("firstSeenAt", JsonNull) }
            },
            InvalidCase("signalsApplied missing a key") {
                it.withRecord { record -> record.withSignals { signals -> signals.without("saved") } }
            },
            InvalidCase("signalsApplied carries an extra key") {
                it.withRecord { record ->
                    record.withSignals { signals -> signals.with("shared", JsonPrimitive(false)) }
                }
            },
            InvalidCase("signal is not boolean") {
                it.withRecord { record ->
                    record.withSignals { signals -> signals.with("saved", JsonPrimitive(1)) }
                }
            },
            InvalidCase("opened signal false while openedAt is present") {
                it.withRecord { record ->
                    record.withSignals { signals -> signals.with("opened", JsonPrimitive(false)) }
                }
            },
            InvalidCase("opened signal true while openedAt is absent") {
                it.withRecord { record -> record.with("openedAt", JsonNull) }
            },
            InvalidCase("read signal true while status is saved") {
                it.withRecord { record ->
                    record.withSignals { signals -> signals.with("read", JsonPrimitive(true)) }
                }
            },
            InvalidCase("read status while read signal is false") {
                it.withRecord { record -> record.with("status", JsonPrimitive("read")) }
            },
            InvalidCase("dismissed signal set on a saved record") {
                it.withRecord { record ->
                    record.withSignals { signals -> signals.with("dismissed", JsonPrimitive(true)) }
                }
            },
            InvalidCase("required savedAt is null") {
                it.withRecord { record -> record.with("savedAt", JsonNull) }
            },
            InvalidCase("dismissedAt is not applicable to saved status") {
                it.withRecord { record -> record.with("dismissedAt", JsonPrimitive(ACTION_AT)) }
            },
            InvalidCase("action timestamp predates firstSeenAt") {
                it.withRecord { record -> record.with("savedAt", JsonPrimitive("2026-08-20T09:59:59Z")) }
            },
            InvalidCase("Article snapshot violates item 002 rules") {
                it.withRecord { record ->
                    record.withObject("article") { article -> article.with("title", JsonPrimitive("")) }
                }
            },
            InvalidCase("appearance is case-sensitive") {
                it.withObject("settings") { settings ->
                    settings.with("appearance", JsonPrimitive("Light"))
                }
            },
            InvalidCase("lastCategory everything is not in the frozen enumeration") {
                it.withObject("session") { session ->
                    session.with("lastCategory", JsonPrimitive("everything"))
                }
            },
        )

        cases.forEach { case ->
            val result = validator.validate(case.mutate(validDocument).bytes())
            val failure = assertIs<LocalStateResult.Failure>(result, case.name)
            assertEquals(case.expectedCode, failure.code, case.name)
        }
    }

    @Test
    fun `malformed JSON is distinct from a structurally invalid state`() {
        val result = validator.validate("{not json".encodeToByteArray())

        assertEquals(
            LocalStateErrorCode.MALFORMED_JSON,
            assertIs<LocalStateResult.Failure>(result).code,
        )
    }

    @Test
    fun `unsupported future shape is classified before version one decoding`() {
        val futureDocument = JsonObject(
            mapOf(
                "schemaVersion" to JsonPrimitive(2),
                "settings" to JsonArray(listOf(JsonPrimitive("future-setting"))),
                "futureTopLevelKey" to JsonObject(mapOf("enabled" to JsonPrimitive(true))),
            ),
        )

        val failure = assertIs<LocalStateResult.Failure>(validator.validate(futureDocument.bytes()))

        assertEquals(LocalStateErrorCode.UNSUPPORTED_SCHEMA, failure.code)
    }

    @Test
    fun `an impossible local-state calendar date reports the specific validity error`() {
        val document = validDocument.withRecord { record ->
            record.with("firstSeenAt", JsonPrimitive("2026-02-30T10:00:00Z"))
        }

        val failure = assertIs<LocalStateResult.Failure>(validator.validate(document.bytes()))

        assertEquals(
            "state.articles.$ARTICLE_ID.firstSeenAt must be a real UTC calendar timestamp",
            failure.message,
        )
    }

    @Test
    fun `remove from Read Later is accepted without a dismiss signal`() {
        val removed = validDocument.withRecord { record ->
            record
                .with("status", JsonPrimitive("dismissed"))
                .with("savedAt", JsonNull)
                .with("dismissedAt", JsonPrimitive(ACTION_AT))
                .withSignals { signals ->
                    signals
                        .with("saved", JsonPrimitive(false))
                        .with("dismissed", JsonPrimitive(false))
                }
        }

        assertIs<LocalStateResult.Success>(validator.validate(removed.bytes()))
    }

    private data class InvalidCase(
        val name: String,
        val expectedCode: LocalStateErrorCode = LocalStateErrorCode.INVALID_STATE,
        val mutate: (JsonObject) -> JsonObject,
    )

    companion object {
        const val ARTICLE_ID = "00000000000000000001"
        const val SOURCE_ID = "ietf_oauth"
        const val FIRST_SEEN_AT = "2026-08-20T10:00:00Z"
        const val OPENED_AT = "2026-08-20T11:00:00Z"
        const val ACTION_AT = "2026-08-20T12:00:00Z"

        val strictJson = Json
        val validDocument: JsonObject = JsonObject(
            mapOf(
                "schemaVersion" to JsonPrimitive(1),
                "preferences" to JsonObject(
                    mapOf(
                        "sources" to JsonObject(
                            mapOf(
                                SOURCE_ID to preference(weight = 1.5, interactions = 2),
                            ),
                        ),
                        "topics" to JsonObject(
                            mapOf("oauth" to preference(weight = -0.25, interactions = 1)),
                        ),
                    ),
                ),
                "articles" to JsonObject(mapOf(ARTICLE_ID to savedRecord())),
                "settings" to JsonObject(mapOf("appearance" to JsonPrimitive("dark"))),
                "session" to JsonObject(mapOf("lastCategory" to JsonPrimitive("iam"))),
            ),
        )

        fun fullyPopulatedDocument(): JsonObject {
            val opened = savedRecord(1)
                .with("status", JsonPrimitive("opened"))
                .with("savedAt", JsonNull)
                .withSignals { it.with("saved", JsonPrimitive(false)) }
            val saved = savedRecord(2)
            val dismissed = savedRecord(3)
                .with("status", JsonPrimitive("dismissed"))
                .with("savedAt", JsonNull)
                .with("dismissedAt", JsonPrimitive(ACTION_AT))
                .withSignals {
                    it.with("saved", JsonPrimitive(false)).with("dismissed", JsonPrimitive(true))
                }
            val read = savedRecord(4)
                .with("status", JsonPrimitive("read"))
                .with("savedAt", JsonNull)
                .with("readAt", JsonPrimitive(ACTION_AT))
                .withSignals { it.with("saved", JsonPrimitive(false)).with("read", JsonPrimitive(true)) }
            return validDocument.with(
                "articles",
                JsonObject(
                    mapOf(
                        articleId(1) to opened,
                        articleId(2) to saved,
                        articleId(3) to dismissed,
                        articleId(4) to read,
                    ),
                ),
            )
        }

        fun JsonObject.bytes(): ByteArray =
            strictJson.encodeToString(JsonElement.serializer(), this).encodeToByteArray()

        fun JsonObject.with(key: String, value: JsonElement): JsonObject =
            JsonObject(toMutableMap().apply { put(key, value) })

        fun JsonObject.without(key: String): JsonObject =
            JsonObject(toMutableMap().apply { remove(key) })

        fun JsonObject.withObject(key: String, transform: (JsonObject) -> JsonObject): JsonObject =
            with(key, transform(getValue(key).jsonObject))

        fun JsonObject.withRecord(transform: (JsonObject) -> JsonObject): JsonObject =
            withObject("articles") { articles ->
                articles.with(ARTICLE_ID, transform(articles.getValue(ARTICLE_ID).jsonObject))
            }

        fun JsonObject.withRecordKey(key: String): JsonObject =
            with("articles", JsonObject(mapOf(key to getValue("articles").jsonObject.getValue(ARTICLE_ID))))

        fun JsonObject.withSignals(transform: (JsonObject) -> JsonObject): JsonObject =
            withObject("signalsApplied", transform)

        private fun JsonObject.withSourcePreference(transform: (JsonObject) -> JsonObject): JsonObject =
            withObject("preferences") { preferences ->
                preferences.withObject("sources") { sources ->
                    sources.with(SOURCE_ID, transform(sources.getValue(SOURCE_ID).jsonObject))
                }
            }

        private fun preference(weight: Double, interactions: Int): JsonObject = JsonObject(
            mapOf(
                "weight" to JsonPrimitive(weight),
                "interactions" to JsonPrimitive(interactions),
            ),
        )

        private fun savedRecord(index: Int = 1): JsonObject = JsonObject(
            mapOf(
                "article" to article(index),
                "status" to JsonPrimitive("saved"),
                "firstSeenAt" to JsonPrimitive(FIRST_SEEN_AT),
                "openedAt" to JsonPrimitive(OPENED_AT),
                "savedAt" to JsonPrimitive(ACTION_AT),
                "dismissedAt" to JsonNull,
                "readAt" to JsonNull,
                "signalsApplied" to JsonObject(
                    mapOf(
                        "opened" to JsonPrimitive(true),
                        "saved" to JsonPrimitive(true),
                        "dismissed" to JsonPrimitive(false),
                        "read" to JsonPrimitive(false),
                    ),
                ),
            ),
        )

        private fun article(index: Int): JsonObject = JsonObject(
            mapOf(
                "id" to JsonPrimitive(articleId(index)),
                "title" to JsonPrimitive("A deliberate article $index"),
                "url" to JsonPrimitive("https://example.com/articles/$index"),
                "source" to JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(SOURCE_ID),
                        "name" to JsonPrimitive("IETF OAuth WG"),
                    ),
                ),
                "category" to JsonPrimitive("iam"),
                "publishedAt" to JsonPrimitive("2026-08-20T09:00:00Z"),
                "author" to JsonNull,
                "excerpt" to JsonPrimitive("A plain-text excerpt."),
                "readingTimeMinutes" to JsonPrimitive(7),
                "tags" to JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "id" to JsonPrimitive("oauth"),
                                "label" to JsonPrimitive("OAuth"),
                            ),
                        ),
                    ),
                ),
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

        private fun articleId(index: Int): String = index.toString(16).padStart(20, '0')
    }
}
