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
    ): ArticleRecord = ArticleRecord(
        article = article(),
        status = status,
        firstSeenAt = firstSeenAt,
        openedAt = openedAt,
        savedAt = savedAt,
        dismissedAt = dismissedAt,
        readAt = readAt,
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
