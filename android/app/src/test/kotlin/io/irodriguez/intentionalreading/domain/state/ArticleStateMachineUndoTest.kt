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
import kotlin.test.assertNotEquals
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
                preferences = noPreferences,
                article = article(),
                action = ArticleAction.SAVE,
                now = actionTime,
                undoable = true,
            ),
        )

        // Then the saved result carries the absence of the prior record
        assertEquals(ArticleStatus.SAVED, result.record.status)
        // Scenario: an undo-eligible first Save records its applied Save reversal.
        assertEquals(
            UndoRecord(
                articleId = article().id,
                action = ArticleAction.SAVE,
                previousRecord = null,
                preferenceReversal = PreferenceReversal.SAVE_FOR_LATER,
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
                preferences = noPreferences,
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
                ArticleStateMachine.transition(emptyMap(), noPreferences, article(), action, actionTime),
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
                ArticleStateMachine.transition(
                    records,
                    noPreferences,
                    article(),
                    action,
                    actionTime,
                    undoable = true,
                ),
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
            noPreferences,
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
                noPreferences,
                article(),
                ArticleAction.SAVE,
                actionTime,
                undoable = true,
            ),
        )

        // When Undo is performed
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(save.records, save.preferences, save.undoRecord),
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
    fun `undoing a save does not claim a resulting record absent from its records`() {
        // Given an undo-eligible save of an article with no prior record
        val save = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                emptyMap(),
                noPreferences,
                article(),
                ArticleAction.SAVE,
                actionTime,
                undoable = true,
            ),
        )

        // When Undo removes the newly created record
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(save.records, save.preferences, save.undoRecord),
        )

        // Then the success result does not claim a record that its own map does not contain
        assertEquals(reversed.records[article().id], reversed.record)
    }

    @Test
    fun `reverse success owns nullable record shape while forward applied stays non-null`() {
        // Given a forward save whose resulting record exists and whose prior record did not
        val save = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                emptyMap(),
                noPreferences,
                article(),
                ArticleAction.SAVE,
                actionTime,
                undoable = true,
            ),
        )

        // When the save is reversed
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(save.records, save.preferences, save.undoRecord),
        )

        // Then forward Applied is non-null by type and reverse has its own nullable-record success type
        assertEquals("non-null", forwardRecordType(save.record))
        assertEquals("Reverted", reversed.javaClass.simpleName)
        assertNull(reversed.record)
    }

    @Test
    fun `undoing a dismiss restores the exact record it replaced`() {
        // Given an undo-eligible dismiss of a fully populated opened record
        val beforeDismiss = openedRecord()
        val dismiss = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                mapOf(article().id to beforeDismiss),
                noPreferences,
                article(),
                ArticleAction.DISMISS,
                actionTime,
                undoable = true,
            ),
        )

        // When Undo is performed
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(dismiss.records, dismiss.preferences, dismiss.undoRecord),
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
            ArticleStateMachine.reverse(records, preferences = noPreferences, undoRecord = null),
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
            ArticleStateMachine.reverse(records, noPreferences, undoRecord),
        )

        // Then it fails as stale without changing the records
        assertEquals(ArticleTransitionErrorCode.UNDO_STALE, result.code)
        assertSame(records, result.records)
    }

    @Test
    fun `a stale undo refusal carries no fabricated source status`() {
        // Given an undo record whose article has no current record
        val undoRecord = UndoRecord(
            articleId = article().id,
            action = ArticleAction.DISMISS,
            previousRecord = openedRecord(),
        )

        // When Undo is refused as stale
        val result = assertIs<ArticleTransition.Invalid>(
            ArticleStateMachine.reverse(emptyMap(), noPreferences, undoRecord),
        )

        // Then the unknown source status remains unknown
        assertEquals(ArticleTransitionErrorCode.UNDO_STALE, result.code)
        assertNull(result.fromStatus)
    }

    @Test
    fun `Undo Dismiss reverses the signal applied by the forward transition`() {
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
                records = before.articles,
                preferences = before.preferences,
                article = article(),
                action = ArticleAction.DISMISS,
                now = actionTime,
                undoable = true,
            ),
        )

        // When the record is inspected and Undo is performed
        // Scenario: Undo Dismiss records and reverses the signal the forward transition applied.
        assertEquals(PreferenceReversal.NOT_INTERESTED, dismiss.undoRecord?.preferenceReversal)
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(dismiss.records, dismiss.preferences, dismiss.undoRecord),
        )
        val after = before.copy(
            articles = reversed.records,
            preferences = reversed.preferences,
        )

        // Then Dismiss moved the preferences and Undo restored them.
        assertNotEquals(before.preferences, dismiss.preferences)
        assertEquals(before.preferences, after.preferences)
    }

    @Test
    fun `Save then Undo then Save leaves one applied signal`() {
        val preferences = LocalState.Preferences(sources = emptyMap(), topics = emptyMap())
        val firstSave = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = emptyMap(),
                preferences = preferences,
                article = article(),
                action = ArticleAction.SAVE,
                now = actionTime,
                undoable = true,
            ),
        )
        val undone = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(firstSave.records, firstSave.preferences, firstSave.undoRecord),
        )

        val secondSave = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = undone.records,
                preferences = undone.preferences,
                article = article(),
                action = ArticleAction.SAVE,
                now = actionTime.plusSeconds(60),
                undoable = true,
            ),
        )

        assertEquals(firstSave.preferences, secondSave.preferences)
        assertEquals(PreferenceEntry(weight = 0.45, interactions = 1), secondSave.preferences.sources["example"])
        assertEquals(PreferenceEntry(weight = 0.30, interactions = 1), secondSave.preferences.topics["oauth"])
    }

    @Test
    fun `Undo of a Save that applied no signal leaves preferences unchanged and restores the record`() {
        val previousRecord = openedRecord()
        val preferences = LocalState.Preferences(
            sources = mapOf("example" to PreferenceEntry(weight = 0.45, interactions = 1)),
            topics = mapOf("oauth" to PreferenceEntry(weight = 0.30, interactions = 1)),
        )
        val save = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(article().id to previousRecord),
                preferences = preferences,
                article = article(),
                action = ArticleAction.SAVE,
                now = actionTime,
                undoable = true,
            ),
        )

        assertNull(save.undoRecord?.preferenceReversal)
        assertSame(preferences, save.preferences)
        val undone = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(save.records, save.preferences, save.undoRecord),
        )

        assertSame(save.preferences, undone.preferences)
        assertSame(previousRecord, undone.records.getValue(article().id))
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
        signalsApplied = SignalsApplied(
            opened = false,
            saved = false,
            dismissed = false,
            read = false,
        ),
    )

    private fun readRecord(): ArticleRecord = ArticleRecord(
        article = article(),
        status = ArticleStatus.READ,
        firstSeenAt = firstSeenAt,
        openedAt = openedAt,
        savedAt = null,
        dismissedAt = null,
        readAt = oldActionTime,
        signalsApplied = SignalsApplied(
            opened = true,
            saved = false,
            dismissed = false,
            read = true,
        ),
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

    @Suppress("UNUSED_PARAMETER")
    private fun forwardRecordType(record: ArticleRecord): String = "non-null"

    @Suppress("UNUSED_PARAMETER")
    private fun forwardRecordType(record: Any?): String = "nullable"

    private companion object {
        val noPreferences = LocalState.Preferences(sources = emptyMap(), topics = emptyMap())
        val firstSeenAt: Instant = Instant.parse("2026-08-20T10:00:00Z")
        val openedAt: Instant = Instant.parse("2026-08-20T11:00:00Z")
        val oldActionTime: Instant = Instant.parse("2026-08-20T12:00:00Z")
        val actionTime: Instant = Instant.parse("2026-08-22T12:00:00Z")
    }
}
