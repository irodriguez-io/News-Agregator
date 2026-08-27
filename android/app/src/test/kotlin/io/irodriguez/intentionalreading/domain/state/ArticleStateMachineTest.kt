package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleContentType
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleScore
import io.irodriguez.intentionalreading.domain.model.ArticleSource
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.ArticleTag
import io.irodriguez.intentionalreading.domain.model.Category
import io.irodriguez.intentionalreading.domain.model.ContentTypeId
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.PreferenceEntry
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ArticleStateMachineTest {
    @Test
    fun `the allowed-from table accepts and rejects every action-status cell`() {
        val allowedFrom = mapOf(
            ArticleAction.OPEN to setOf(
                ArticleStatus.UNSEEN,
                ArticleStatus.OPENED,
                ArticleStatus.SAVED,
                ArticleStatus.READ,
            ),
            ArticleAction.SAVE to setOf(ArticleStatus.UNSEEN, ArticleStatus.OPENED, ArticleStatus.SAVED),
            ArticleAction.DISMISS to setOf(
                ArticleStatus.UNSEEN,
                ArticleStatus.OPENED,
                ArticleStatus.DISMISSED,
            ),
            ArticleAction.MARK_READ to setOf(
                ArticleStatus.UNSEEN,
                ArticleStatus.OPENED,
                ArticleStatus.SAVED,
                ArticleStatus.READ,
            ),
            ArticleAction.MARK_UNREAD to setOf(ArticleStatus.READ),
            ArticleAction.REMOVE to setOf(ArticleStatus.SAVED),
        )

        ArticleAction.entries.forEach { action ->
            ArticleStatus.entries.forEach { status ->
                val records = recordsFor(status)
                val result = ArticleStateMachine.transition(records, article(), action, actionTime)
                val message = "$action from $status"

                if (status in allowedFrom.getValue(action)) {
                    check(result !is ArticleTransition.Invalid) { "$message should be allowed" }
                } else {
                    val invalid = assertIs<ArticleTransition.Invalid>(result, message)
                    assertEquals(action, invalid.action, message)
                    assertEquals(status, invalid.fromStatus, message)
                    assertSame(records, invalid.records, message)
                }
            }
        }
    }

    @Test
    fun `open from unseen creates an opened record with write-once metadata`() {
        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(emptyMap(), article(), ArticleAction.OPEN, actionTime),
        )

        assertEquals(
            ArticleRecord(
                article = article(),
                status = ArticleStatus.OPENED,
                firstSeenAt = actionTime,
                openedAt = actionTime,
                savedAt = null,
                dismissedAt = null,
                readAt = null,
            ),
            result.record,
        )
    }

    @Test
    fun `open from saved or read preserves status and sets a missing opened timestamp`() {
        listOf(ArticleStatus.SAVED, ArticleStatus.READ).forEach { status ->
            val existing = record(status = status, openedAt = null)
            val result = assertIs<ArticleTransition.Applied>(
                ArticleStateMachine.transition(
                    mapOf(article().id to existing),
                    article(),
                    ArticleAction.OPEN,
                    actionTime,
                ),
            )

            assertEquals(status, result.record.status)
            assertEquals(actionTime, result.record.openedAt)
            assertEquals(firstSeenAt, result.record.firstSeenAt)
        }
    }

    @Test
    fun `a second open is unchanged whenever openedAt is already present`() {
        listOf(ArticleStatus.OPENED, ArticleStatus.SAVED, ArticleStatus.READ).forEach { status ->
            val existing = record(status = status, openedAt = openedAt)
            val records = mapOf(article().id to existing)

            val result = assertIs<ArticleTransition.Unchanged>(
                ArticleStateMachine.transition(records, article(), ArticleAction.OPEN, actionTime),
            )

            assertSame(records, result.records)
            assertSame(existing, result.records.getValue(article().id))
        }
    }

    @Test
    fun `save sets savedAt and clears dismissedAt and readAt while preserving openedAt`() {
        val existing = record(
            status = ArticleStatus.OPENED,
            openedAt = openedAt,
            dismissedAt = oldActionTime,
            readAt = oldActionTime,
        )

        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to existing),
                article(),
                ArticleAction.SAVE,
                actionTime,
            ),
        )

        assertEquals(ArticleStatus.SAVED, result.record.status)
        assertEquals(actionTime, result.record.savedAt)
        assertEquals(openedAt, result.record.openedAt)
        assertEquals(null, result.record.dismissedAt)
        assertEquals(null, result.record.readAt)
    }

    @Test
    fun `save from saved is unchanged and preserves every timestamp`() {
        assertIdempotentNoOp(ArticleStatus.SAVED, ArticleAction.SAVE)
    }

    @Test
    fun `dismiss sets dismissedAt and clears savedAt and readAt while preserving openedAt`() {
        val existing = record(
            status = ArticleStatus.OPENED,
            openedAt = openedAt,
            savedAt = oldActionTime,
            readAt = oldActionTime,
        )

        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to existing),
                article(),
                ArticleAction.DISMISS,
                actionTime,
            ),
        )

        assertEquals(ArticleStatus.DISMISSED, result.record.status)
        assertEquals(actionTime, result.record.dismissedAt)
        assertEquals(openedAt, result.record.openedAt)
        assertEquals(null, result.record.savedAt)
        assertEquals(null, result.record.readAt)
    }

    @Test
    fun `dismiss from dismissed is unchanged and preserves every timestamp`() {
        assertIdempotentNoOp(ArticleStatus.DISMISSED, ArticleAction.DISMISS)
    }

    @Test
    fun `mark read sets readAt clears queue timestamps and preserves openedAt`() {
        val existing = record(
            status = ArticleStatus.SAVED,
            openedAt = openedAt,
            savedAt = oldActionTime,
            dismissedAt = oldActionTime,
        )

        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to existing),
                article(),
                ArticleAction.MARK_READ,
                actionTime,
            ),
        )

        assertEquals(ArticleStatus.READ, result.record.status)
        assertEquals(actionTime, result.record.readAt)
        assertEquals(openedAt, result.record.openedAt)
        assertEquals(null, result.record.savedAt)
        assertEquals(null, result.record.dismissedAt)
    }

    @Test
    fun `mark read from read is unchanged and preserves every timestamp`() {
        assertIdempotentNoOp(ArticleStatus.READ, ArticleAction.MARK_READ)
    }

    @Test
    fun `mark unread moves read to saved with a new savedAt and preserved openedAt`() {
        val existing = record(status = ArticleStatus.READ, openedAt = openedAt, readAt = oldActionTime)

        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to existing),
                article(),
                ArticleAction.MARK_UNREAD,
                actionTime,
            ),
        )

        assertEquals(ArticleStatus.SAVED, result.record.status)
        assertEquals(actionTime, result.record.savedAt)
        assertEquals(openedAt, result.record.openedAt)
        assertEquals(null, result.record.readAt)
        assertEquals(null, result.record.dismissedAt)
    }

    @Test
    fun `remove moves saved to dismissed without changing openedAt`() {
        val existing = record(status = ArticleStatus.SAVED, openedAt = openedAt, savedAt = oldActionTime)

        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to existing),
                article(),
                ArticleAction.REMOVE,
                actionTime,
            ),
        )

        assertEquals(ArticleStatus.DISMISSED, result.record.status)
        assertEquals(actionTime, result.record.dismissedAt)
        assertEquals(openedAt, result.record.openedAt)
        assertEquals(null, result.record.savedAt)
        assertEquals(null, result.record.readAt)
    }

    @Test
    fun `applied transitions return a new map without mutating the input or its record`() {
        val existing = record(status = ArticleStatus.OPENED, openedAt = openedAt)
        val records = mapOf(article().id to existing)

        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(records, article(), ArticleAction.SAVE, actionTime),
        )

        assertNotSame(records, result.records)
        assertSame(existing, records.getValue(article().id))
        assertEquals(ArticleStatus.OPENED, records.getValue(article().id).status)
        assertEquals(ArticleStatus.SAVED, result.records.getValue(article().id).status)
    }

    @Test
    fun `Android transitions derive only the signals structurally forced by the record`() {
        val opened = applied(emptyMap(), ArticleAction.OPEN)
        assertSignals(opened, opened = true, saved = false, dismissed = false, read = false)

        val read = applied(emptyMap(), ArticleAction.MARK_READ)
        assertSignals(read, opened = false, saved = false, dismissed = false, read = true)

        val savedRecord = record(status = ArticleStatus.SAVED, savedAt = oldActionTime)
        val removed = applied(mapOf(article().id to savedRecord), ArticleAction.REMOVE)
        assertSignals(removed, opened = false, saved = false, dismissed = false, read = false)
    }

    @Test
    fun `Open applies its signal exactly once`() {
        assertSignalAppliedExactlyOnce(
            action = ArticleAction.OPEN,
            expectedSourceWeight = 0.10,
            expectedTopicWeight = 0.05,
        )
    }

    @Test
    fun `Save applies its signal exactly once`() {
        assertSignalAppliedExactlyOnce(
            action = ArticleAction.SAVE,
            expectedSourceWeight = 0.45,
            expectedTopicWeight = 0.30,
        )
    }

    @Test
    fun `Dismiss applies its signal exactly once`() {
        assertSignalAppliedExactlyOnce(
            action = ArticleAction.DISMISS,
            expectedSourceWeight = -0.35,
            expectedTopicWeight = -0.20,
        )
    }

    @Test
    fun `Mark Read applies its signal exactly once`() {
        assertSignalAppliedExactlyOnce(
            action = ArticleAction.MARK_READ,
            expectedSourceWeight = 0.25,
            expectedTopicWeight = 0.20,
        )
    }

    @Test
    fun `Remove changes no preference and leaves Save latched`() {
        val preferences = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 0.45, interactions = 1)),
            topics = mapOf("oauth" to PreferenceEntry(weight = 0.30, interactions = 1)),
        )
        val saved = record(
            status = ArticleStatus.SAVED,
            savedAt = oldActionTime,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = true,
                dismissed = false,
                read = false,
            ),
        )

        val removed = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(article().id to saved),
                preferences = preferences,
                article = article(),
                action = ArticleAction.REMOVE,
                now = actionTime,
            ),
        )

        assertSame(preferences, removed.preferences)
        assertEquals(ArticleStatus.DISMISSED, removed.record.status)
        assertSignals(removed.record, opened = false, saved = true, dismissed = false, read = false)
    }

    @Test
    fun `Mark Unread reverses a latched Read signal without applying Save`() {
        val storedArticle = article().copy(
            tags = listOf(ArticleTag("oauth", "OAuth"), ArticleTag("scim", "SCIM")),
        )
        val preferences = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 1.0, interactions = 2)),
            topics = mapOf(
                "oauth" to PreferenceEntry(weight = 0.6, interactions = 2),
                "scim" to PreferenceEntry(weight = 0.6, interactions = 2),
            ),
        )
        val read = ArticleRecord(
            article = storedArticle,
            status = ArticleStatus.READ,
            firstSeenAt = firstSeenAt,
            openedAt = null,
            savedAt = null,
            dismissedAt = null,
            readAt = oldActionTime,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = false,
                dismissed = false,
                read = true,
            ),
        )

        val unread = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(storedArticle.id to read),
                preferences = preferences,
                article = storedArticle,
                action = ArticleAction.MARK_UNREAD,
                now = actionTime,
            ),
        )

        assertEquals(PreferenceEntry(weight = 0.75, interactions = 1), unread.preferences.sources["example"])
        assertEquals(PreferenceEntry(weight = 0.4, interactions = 1), unread.preferences.topics["oauth"])
        assertEquals(PreferenceEntry(weight = 0.4, interactions = 1), unread.preferences.topics["scim"])
        assertEquals(ArticleStatus.SAVED, unread.record.status)
        assertSignals(unread.record, opened = false, saved = false, dismissed = false, read = false)
    }

    @Test
    fun `Mark Unread without a latched Read signal changes no preference`() {
        val preferences = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 1.0, interactions = 2)),
            topics = mapOf("oauth" to PreferenceEntry(weight = 0.6, interactions = 2)),
        )
        val readWithoutSignal = ArticleRecord(
            article = article(),
            status = ArticleStatus.READ,
            firstSeenAt = firstSeenAt,
            openedAt = null,
            savedAt = null,
            dismissedAt = null,
            readAt = oldActionTime,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = false,
                dismissed = false,
                read = false,
            ),
        )

        val unread = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(article().id to readWithoutSignal),
                preferences = preferences,
                article = article(),
                action = ArticleAction.MARK_UNREAD,
                now = actionTime,
            ),
        )

        assertSame(preferences, unread.preferences)
        assertEquals(ArticleStatus.SAVED, unread.record.status)
        assertSignals(unread.record, opened = false, saved = false, dismissed = false, read = false)
    }

    @Test
    fun `Mark Unread reverses against the stored snapshot when the dataset article differs`() {
        val storedArticle = article().copy(
            source = ArticleSource("stored-source", "Stored Source"),
            tags = listOf(ArticleTag("stored-topic", "Stored Topic")),
        )
        val incomingArticle = article().copy(
            source = ArticleSource("incoming-source", "Incoming Source"),
            tags = listOf(ArticleTag("incoming-topic", "Incoming Topic")),
        )
        val preferences = LocalState.Preferences(
            sources = mapOf(
                "stored-source" to PreferenceEntry(weight = 0.25, interactions = 1),
                "incoming-source" to PreferenceEntry(weight = 2.0, interactions = 4),
            ),
            topics = mapOf(
                "stored-topic" to PreferenceEntry(weight = 0.20, interactions = 1),
                "incoming-topic" to PreferenceEntry(weight = -1.0, interactions = 3),
            ),
        )
        val read = ArticleRecord(
            article = storedArticle,
            status = ArticleStatus.READ,
            firstSeenAt = firstSeenAt,
            openedAt = null,
            savedAt = null,
            dismissedAt = null,
            readAt = oldActionTime,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = false,
                dismissed = false,
                read = true,
            ),
        )

        val unread = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(storedArticle.id to read),
                preferences = preferences,
                article = incomingArticle,
                action = ArticleAction.MARK_UNREAD,
                now = actionTime,
            ),
        )

        assertEquals(null, unread.preferences.sources["stored-source"])
        assertEquals(null, unread.preferences.topics["stored-topic"])
        assertEquals(preferences.sources["incoming-source"], unread.preferences.sources["incoming-source"])
        assertEquals(preferences.topics["incoming-topic"], unread.preferences.topics["incoming-topic"])
    }

    @Test
    fun `Android actions preserve foreign learning signal latches`() {
        val savedSignal = record(
            status = ArticleStatus.SAVED,
            savedAt = oldActionTime,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = true,
                dismissed = false,
                read = false,
            ),
        )
        val read = applied(mapOf(article().id to savedSignal), ArticleAction.MARK_READ)
        assertSignals(read, opened = false, saved = true, dismissed = false, read = true)

        val dismissedSignal = record(
            status = ArticleStatus.DISMISSED,
            dismissedAt = oldActionTime,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = false,
                dismissed = true,
                read = false,
            ),
        )
        val records = mapOf(article().id to dismissedSignal)
        val unchanged = assertIs<ArticleTransition.Unchanged>(
            ArticleStateMachine.transition(records, article(), ArticleAction.DISMISS, actionTime),
        )
        assertSignals(
            unchanged.records.getValue(article().id),
            opened = false,
            saved = false,
            dismissed = true,
            read = false,
        )

        val inconsistentDismissSignal = record(
            status = ArticleStatus.OPENED,
            openedAt = openedAt,
            signalsApplied = SignalsApplied(
                opened = true,
                saved = false,
                dismissed = true,
                read = false,
            ),
        )
        val saved = applied(mapOf(article().id to inconsistentDismissSignal), ArticleAction.SAVE)
        // Scenario: stored signals remain authoritative latches across allowed transitions.
        assertSignals(saved, opened = true, saved = false, dismissed = true, read = false)
    }

    private fun assertSignalAppliedExactlyOnce(
        action: ArticleAction,
        expectedSourceWeight: Double,
        expectedTopicWeight: Double,
    ) {
        val preferences = LocalState.Preferences(sources = emptyMap(), topics = emptyMap())
        val first = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = emptyMap(),
                preferences = preferences,
                article = article(),
                action = action,
                now = actionTime,
            ),
        )
        val second = assertIs<ArticleTransition.Unchanged>(
            ArticleStateMachine.transition(
                records = first.records,
                preferences = first.preferences,
                article = article(),
                action = action,
                now = actionTime.plusSeconds(60),
            ),
        )

        assertEquals(
            PreferenceEntry(weight = expectedSourceWeight, interactions = 1),
            first.preferences.sources["example"],
        )
        assertEquals(
            PreferenceEntry(weight = expectedTopicWeight, interactions = 1),
            first.preferences.topics["oauth"],
        )
        assertSame(first.preferences, second.preferences)
    }

    private fun assertIdempotentNoOp(status: ArticleStatus, action: ArticleAction) {
        val existing = record(
            status = status,
            openedAt = openedAt,
            savedAt = oldActionTime,
            dismissedAt = oldActionTime,
            readAt = oldActionTime,
        )
        val records = mapOf(article().id to existing)

        val result = assertIs<ArticleTransition.Unchanged>(
            ArticleStateMachine.transition(records, article(), action, actionTime),
        )

        assertSame(records, result.records)
        assertEquals(existing, result.records.getValue(article().id))
    }

    private fun applied(
        records: Map<String, ArticleRecord>,
        action: ArticleAction,
    ): ArticleRecord = assertIs<ArticleTransition.Applied>(
        ArticleStateMachine.transition(records, article(), action, actionTime),
    ).record

    private fun assertSignals(
        record: ArticleRecord,
        opened: Boolean,
        saved: Boolean,
        dismissed: Boolean,
        read: Boolean,
    ) {
        assertEquals(opened, record.signalsApplied.opened)
        assertEquals(saved, record.signalsApplied.saved)
        assertEquals(dismissed, record.signalsApplied.dismissed)
        assertEquals(read, record.signalsApplied.read)
    }

    private fun recordsFor(status: ArticleStatus): Map<String, ArticleRecord> = when (status) {
        ArticleStatus.UNSEEN -> emptyMap()
        ArticleStatus.OPENED -> mapOf(article().id to record(status, openedAt = openedAt))
        ArticleStatus.SAVED -> mapOf(article().id to record(status, savedAt = oldActionTime))
        ArticleStatus.DISMISSED -> mapOf(article().id to record(status, dismissedAt = oldActionTime))
        ArticleStatus.READ -> mapOf(article().id to record(status, readAt = oldActionTime))
    }

    private fun record(
        status: ArticleStatus,
        openedAt: Instant? = null,
        savedAt: Instant? = null,
        dismissedAt: Instant? = null,
        readAt: Instant? = null,
        signalsApplied: SignalsApplied = SignalsApplied.derivedForAndroid(
            status = status,
            openedAtPresent = openedAt != null,
        ),
    ): ArticleRecord = ArticleRecord(
        article = article(),
        status = status,
        firstSeenAt = firstSeenAt,
        openedAt = openedAt,
        savedAt = savedAt,
        dismissedAt = dismissedAt,
        readAt = readAt,
        signalsApplied = signalsApplied,
    )

    private fun article(): Article = Article(
        id = "00000000000000000001",
        title = "A deliberate article",
        url = "https://example.com/article",
        source = ArticleSource("example", "Example Source"),
        category = Category.IAM,
        publishedAt = Instant.parse("2026-08-20T12:00:00Z"),
        author = "Author",
        excerpt = "A useful excerpt.",
        readingTimeMinutes = 7,
        tags = listOf(ArticleTag("oauth", "OAuth")),
        contentType = ArticleContentType(ContentTypeId.STANDARDS_UPDATE, "Standards Update"),
        score = ArticleScore(91, 50, 20, 15, 5, 1),
    )

    private companion object {
        val firstSeenAt: Instant = Instant.parse("2026-08-20T10:00:00Z")
        val openedAt: Instant = Instant.parse("2026-08-20T11:00:00Z")
        val oldActionTime: Instant = Instant.parse("2026-08-20T12:00:00Z")
        val actionTime: Instant = Instant.parse("2026-08-22T12:00:00Z")
    }
}
