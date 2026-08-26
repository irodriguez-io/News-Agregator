package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.data.local.state.LocalStateMapper
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
import io.irodriguez.intentionalreading.domain.validation.LocalStateResult
import io.irodriguez.intentionalreading.domain.validation.LocalStateValidator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class ArticleStateMachineUndoTest {
    @Test
    fun `an undo-eligible save records what it replaced`() {
        // Given an article with no stored record
        val records = emptyMap<String, ArticleRecord>()

        // When it is saved through a commit marked undo-eligible
        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = records,
                article = article(),
                action = ArticleAction.SAVE,
                now = actionTime,
                undoable = true,
            ),
        )

        // Then the saved result carries the absence of the prior record
        assertEquals(ArticleStatus.SAVED, result.record.status)
        assertEquals(
            UndoRecord(
                articleId = article().id,
                action = ArticleAction.SAVE,
                previousRecord = null,
            ),
            result.undoRecord,
        )
    }

    @Test
    fun `an undo-eligible dismiss of an opened article records the opened record`() {
        // Given an article whose stored record is opened
        val opened = openedRecord()

        // When it is dismissed through a commit marked undo-eligible
        val result = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(article().id to opened),
                article = article(),
                action = ArticleAction.DISMISS,
                now = actionTime,
                undoable = true,
            ),
        )

        // Then the undo record holds the exact opened record
        assertEquals(ArticleStatus.DISMISSED, result.record.status)
        assertEquals(article().id, result.undoRecord?.articleId)
        assertEquals(ArticleAction.DISMISS, result.undoRecord?.action)
        assertSame(opened, result.undoRecord?.previousRecord)
    }

    @Test
    fun `a commit that is not marked undo-eligible offers nothing`() {
        // Given an article with no stored record
        listOf(ArticleAction.SAVE, ArticleAction.DISMISS).forEach { action ->
            // When it is committed without undo eligibility
            val result = assertIs<ArticleTransition.Applied>(
                ArticleStateMachine.transition(emptyMap(), article(), action, actionTime),
            )

            // Then no undo record is offered
            assertNull(result.undoRecord, action.name)
        }
    }

    @Test
    fun `only save and dismiss are reversible`() {
        // Given each non-reversible action in an allowed starting state
        val cases = listOf(
            ArticleAction.OPEN to emptyMap(),
            ArticleAction.MARK_READ to emptyMap(),
            ArticleAction.MARK_UNREAD to mapOf(article().id to readRecord()),
            ArticleAction.REMOVE to mapOf(article().id to savedRecord()),
        )

        cases.forEach { (action, records) ->
            // When the commit is marked undo-eligible
            val result = assertIs<ArticleTransition.Applied>(
                ArticleStateMachine.transition(records, article(), action, actionTime, undoable = true),
            )

            // Then the transition carries no undo record
            assertNull(result.undoRecord, action.name)
        }
    }

    @Test
    fun `an idempotent no-op produces no undo record`() {
        // Given an article that is already saved
        val records = mapOf(article().id to savedRecord())

        // When Save is marked undo-eligible but changes nothing
        val result = ArticleStateMachine.transition(
            records,
            article(),
            ArticleAction.SAVE,
            actionTime,
            undoable = true,
        )

        // Then the unchanged transition has no undo record
        assertIs<ArticleTransition.Unchanged>(result)
        assertSame(records, result.records)
    }

    @Test
    fun `undoing a save returns the article to having no record`() {
        // Given an undo-eligible save of an unseen article
        val save = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                emptyMap(),
                article(),
                ArticleAction.SAVE,
                actionTime,
                undoable = true,
            ),
        )

        // When Undo is performed
        val reversed = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.reverse(save.records, save.undoRecord),
        )

        // Then the key is deleted, Discover offers it, and the resulting LocalState validates
        assertFalse(reversed.records.containsKey(article().id))
        assertEquals(
            article(),
            DiscoverDeck.build(
                articles = listOf(article()),
                records = reversed.records,
                selectedCategory = null,
                heldArticleId = null,
            ).article,
        )
        val state = LocalState.default().copy(articles = reversed.records)
        assertIs<LocalStateResult.Success>(
            LocalStateValidator().validate(LocalStateMapper.encode(state)),
        )
    }

    @Test
    fun `undoing a dismiss restores the exact record it replaced`() {
        // Given an undo-eligible dismiss of a fully populated opened record
        val beforeDismiss = openedRecord()
        val dismiss = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to beforeDismiss),
                article(),
                ArticleAction.DISMISS,
                actionTime,
                undoable = true,
            ),
        )

        // When Undo is performed
        val reversed = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.reverse(dismiss.records, dismiss.undoRecord),
        )

        // Then the complete record is restored without rewriting any field
        val restored = reversed.records.getValue(article().id)
        assertEquals(beforeDismiss, restored)
        assertEquals(firstSeenAt, restored.firstSeenAt)
        assertEquals(openedAt, restored.openedAt)
        assertEquals(beforeDismiss.signalsApplied, restored.signalsApplied)
    }

    @Test
    fun `Undo is refused when there is nothing to undo`() {
        // Given no undo record
        val records = mapOf(article().id to savedRecord())

        // When Undo is requested
        val result = assertIs<ArticleTransition.Invalid>(
            ArticleStateMachine.reverse(records, undoRecord = null),
        )

        // Then it fails as unavailable without changing the records
        assertEquals(ArticleTransitionErrorCode.UNDO_UNAVAILABLE, result.code)
        assertSame(records, result.records)
    }

    @Test
    fun `Undo is refused when the article it names is gone`() {
        // Given a populated undo record whose named article is absent
        val undoRecord = UndoRecord(
            articleId = article().id,
            action = ArticleAction.SAVE,
            previousRecord = null,
        )
        val records = emptyMap<String, ArticleRecord>()

        // When Undo is requested
        val result = assertIs<ArticleTransition.Invalid>(
            ArticleStateMachine.reverse(records, undoRecord),
        )

        // Then it fails as stale without changing the records
        assertEquals(ArticleTransitionErrorCode.UNDO_STALE, result.code)
        assertSame(records, result.records)
    }

    @Test
    fun `the undo record carries a reversal field that is not yet used`() {
        // Given preferences and an undo-eligible dismissal
        val preferences = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 1.25, interactions = 3)),
            topics = mapOf("oauth" to PreferenceEntry(weight = -0.5, interactions = 2)),
        )
        val before = LocalState.default().copy(
            preferences = preferences,
            articles = mapOf(article().id to openedRecord()),
        )
        val dismiss = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                before.articles,
                article(),
                ArticleAction.DISMISS,
                actionTime,
                undoable = true,
            ),
        )

        // When the record is inspected and Undo is performed
        assertNull(dismiss.undoRecord?.preferenceReversal)
        val reversed = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.reverse(dismiss.records, dismiss.undoRecord),
        )
        val after = before.copy(articles = reversed.records)

        // Then no preference entry changed
        assertSame(preferences, after.preferences)
        assertEquals(before.preferences, after.preferences)
    }

    private fun openedRecord(): ArticleRecord = ArticleRecord(
        article = article(),
        status = ArticleStatus.OPENED,
        firstSeenAt = firstSeenAt,
        openedAt = openedAt,
        savedAt = null,
        dismissedAt = null,
        readAt = null,
        signalsApplied = SignalsApplied(
            opened = true,
            saved = true,
            dismissed = false,
            read = false,
        ),
    )

    private fun savedRecord(): ArticleRecord = ArticleRecord(
        article = article(),
        status = ArticleStatus.SAVED,
        firstSeenAt = firstSeenAt,
        openedAt = null,
        savedAt = oldActionTime,
        dismissedAt = null,
        readAt = null,
    )

    private fun readRecord(): ArticleRecord = ArticleRecord(
        article = article(),
        status = ArticleStatus.READ,
        firstSeenAt = firstSeenAt,
        openedAt = openedAt,
        savedAt = null,
        dismissedAt = null,
        readAt = oldActionTime,
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
