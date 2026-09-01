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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ArticleStateMachineUndoTest {
    @Test
    fun `reversible actions carry undo state without caller eligibility`() {
        // Given an article with an exact prior record
        val previousRecord = openedRecord()

        // When Save is committed without an eligibility argument
        val save = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(article().id to previousRecord),
                preferences = noPreferences,
                article = article(),
                action = ArticleAction.SAVE,
                now = actionTime,
            ),
        )

        // Then Save names the exact state it replaced
        assertEquals(article().id, save.undoRecord?.articleId)
        assertSame(previousRecord, save.undoRecord?.previousRecord)

        // And Open and Mark Read remain outside the reversible action set
        listOf(ArticleAction.OPEN).forEach { action ->
            val result = assertIs<ArticleTransition.Applied>(
                ArticleStateMachine.transition(
                    records = emptyMap(),
                    preferences = noPreferences,
                    article = article(),
                    action = action,
                    now = actionTime,
                ),
            )

            assertNull(result.undoRecord, action.name)
        }
    }

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
            ),
        )

        // Then the undo record holds the exact opened record
        assertEquals(ArticleStatus.DISMISSED, result.record.status)
        assertEquals(article().id, result.undoRecord?.articleId)
        assertEquals(ArticleAction.DISMISS, result.undoRecord?.action)
        assertSame(opened, result.undoRecord?.previousRecord)
    }

    @Test
    fun `only Open is not reversible`() {
        // Given every action in an allowed starting state
        val cases = listOf(
            Triple(ArticleAction.OPEN, emptyMap(), false),
            Triple(ArticleAction.SAVE, emptyMap(), true),
            Triple(ArticleAction.DISMISS, emptyMap(), true),
            Triple(ArticleAction.MARK_READ, emptyMap(), true),
            Triple(ArticleAction.MARK_UNREAD, mapOf(article().id to readRecord()), true),
            Triple(ArticleAction.REMOVE, mapOf(article().id to savedRecord()), true),
        )

        cases.forEach { (action, records, isReversible) ->
            // When the action is committed
            val result = assertIs<ArticleTransition.Applied>(
                ArticleStateMachine.transition(
                    records,
                    noPreferences,
                    article(),
                    action,
                    actionTime,
                ),
            )

            // Then every action except Open carries an undo record
            assertEquals(isReversible, result.undoRecord != null, action.name)
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
        )

        // Then the unchanged transition has no undo record
        assertIs<ArticleTransition.Unchanged>(result)
        assertSame(records, result.records)

        // Given the article is already read
        val readRecords = mapOf(article().id to readRecord())

        // When Mark Read is requested again
        val markRead = ArticleStateMachine.transition(
            readRecords,
            noPreferences,
            article(),
            ArticleAction.MARK_READ,
            actionTime,
        )

        // Then that unchanged transition also has no undo record
        assertIs<ArticleTransition.Unchanged>(markRead)
        assertSame(readRecords, markRead.records)
    }

    @Test
    // Scenario: Remove from Read Later is reversible and moves no weight
    fun `Scenario - Remove from Read Later is reversible and moves no weight`() {
        // Given article A is saved with preferences keyed by its source and topic ids
        val targetArticle = arithmeticArticle()
        val beforeRemove = savedRecord().copy(article = targetArticle)
        val preferencesBeforeRemove = arithmeticPreferences()

        // When Remove is applied
        val remove = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(reversalArticleId to beforeRemove),
                preferences = preferencesBeforeRemove,
                article = targetArticle,
                action = ArticleAction.REMOVE,
                now = actionTime,
            ),
        )

        // Then article A is dismissed by id, carries Undo, and no keyed preference entry moves
        assertEquals(ArticleStatus.DISMISSED, remove.records[reversalArticleId]?.status)
        assertEquals(reversalArticleId, remove.undoRecord?.articleId)
        assertSame(beforeRemove, remove.undoRecord?.previousRecord)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeRemove, remove.preferences)

        // When Undo is applied
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(remove.records, remove.preferences, remove.undoRecord),
        )

        // Then article A is restored exactly as Saved by id and no keyed preference entry moved
        assertSame(beforeRemove, reversed.records[reversalArticleId])
        assertEquals(ArticleStatus.SAVED, reversed.records[reversalArticleId]?.status)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeRemove, reversed.preferences)
    }

    @Test
    // Scenario: Mark read from Read Later is reversible and its signal is reversed
    fun `Scenario - Mark read from Read Later is reversible and its signal is reversed`() {
        // Given article A is saved and its Read signal has never been applied
        val targetArticle = arithmeticArticle()
        val beforeMarkRead = savedRecord().copy(article = targetArticle)
        val preferencesBeforeMarkRead = arithmeticPreferences()

        // When Mark Read is applied
        val markRead = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(reversalArticleId to beforeMarkRead),
                preferences = preferencesBeforeMarkRead,
                article = targetArticle,
                action = ArticleAction.MARK_READ,
                now = actionTime,
            ),
        )

        // Then article A is Read by id and Mark Read moved its exact source and topic entries
        assertEquals(ArticleStatus.READ, markRead.records[reversalArticleId]?.status)
        assertTrue(markRead.records[reversalArticleId]?.signalsApplied?.read == true)
        assertEquals(reversalArticleId, markRead.undoRecord?.articleId)
        assertEquals(
            PreferenceEntry(weight = 1.50, interactions = 4),
            markRead.preferences.sources[reversalSourceId],
            "source id $reversalSourceId",
        )
        assertEquals(
            PreferenceEntry(weight = 0.70, interactions = 5),
            markRead.preferences.topics[reversalTopicId],
            "topic id $reversalTopicId",
        )
        assertEquals(
            PreferenceEntry(weight = -0.20, interactions = 3),
            markRead.preferences.topics[secondReversalTopicId],
            "topic id $secondReversalTopicId",
        )
        assertEquals(
            preferencesBeforeMarkRead.topics[unrelatedTopicId],
            markRead.preferences.topics[unrelatedTopicId],
            "topic id $unrelatedTopicId",
        )

        // When Undo is applied
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(markRead.records, markRead.preferences, markRead.undoRecord),
        )

        // Then article A is restored exactly by id and every source/topic entry is restored
        assertSame(beforeMarkRead, reversed.records[reversalArticleId])
        assertFalse(reversed.records[reversalArticleId]?.signalsApplied?.read ?: true)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeMarkRead, reversed.preferences)
    }

    @Test
    // Scenario: Mark read that applies no signal reverses no weight
    fun `Scenario - Mark read that applies no signal reverses no weight`() {
        // Given article A is Saved while already carrying its applied Read signal
        val targetArticle = arithmeticArticle()
        val beforeMarkRead = savedRecord().copy(
            article = targetArticle,
            signalsApplied = savedRecord().signalsApplied.copy(read = true),
        )
        val preferencesBeforeMarkRead = arithmeticPreferences()

        // When Mark Read is applied
        val markRead = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(reversalArticleId to beforeMarkRead),
                preferences = preferencesBeforeMarkRead,
                article = targetArticle,
                action = ArticleAction.MARK_READ,
                now = actionTime,
            ),
        )

        // Then article A has an Undo record by id, but no signal or keyed weight is applied
        assertEquals(reversalArticleId, markRead.undoRecord?.articleId)
        assertNull(markRead.undoRecord?.preferenceReversal)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeMarkRead, markRead.preferences)

        // When Undo is applied
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(markRead.records, markRead.preferences, markRead.undoRecord),
        )

        // Then article A and every source/topic entry are restored exactly by id
        assertSame(beforeMarkRead, reversed.records[reversalArticleId])
        assertPreferencesEqualEntryForEntryById(preferencesBeforeMarkRead, reversed.preferences)
    }

    @Test
    // Scenario: Mark unread from History is reversible and its signal is re-applied
    fun `Scenario - Mark unread from History is reversible and its signal is re-applied`() {
        // Given article A is marked Read so its Read signal and exact keyed weights are applied
        val targetArticle = arithmeticArticle()
        val saved = savedRecord().copy(article = targetArticle)
        val markRead = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(reversalArticleId to saved),
                preferences = arithmeticPreferences(),
                article = targetArticle,
                action = ArticleAction.MARK_READ,
                now = oldActionTime,
            ),
        )
        val beforeMarkUnreadRecord = assertNotNull(markRead.records[reversalArticleId])
        val preferencesBeforeMarkUnread = markRead.preferences

        // When Mark Unread is applied
        val markUnread = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = markRead.records,
                preferences = preferencesBeforeMarkUnread,
                article = targetArticle,
                action = ArticleAction.MARK_UNREAD,
                now = actionTime,
            ),
        )

        // Then article A is Saved by id, its Read signal is false, and exact keyed weights are reversed
        assertEquals(ArticleStatus.SAVED, markUnread.records[reversalArticleId]?.status)
        assertFalse(markUnread.records[reversalArticleId]?.signalsApplied?.read ?: true)
        assertEquals(reversalArticleId, markUnread.undoRecord?.articleId)
        assertPreferencesEqualEntryForEntryById(arithmeticPreferences(), markUnread.preferences)

        // When Undo is applied
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(markUnread.records, markUnread.preferences, markUnread.undoRecord),
        )

        // Then article A is restored exactly by id with Read true
        assertSame(beforeMarkUnreadRecord, reversed.records[reversalArticleId])
        assertEquals(ArticleStatus.READ, reversed.records[reversalArticleId]?.status)
        assertTrue(reversed.records[reversalArticleId]?.signalsApplied?.read == true)

        // And the preferences match the pre-Mark-Unread maps entry for entry by source and topic id
        assertEquals(preferencesBeforeMarkUnread.sources, reversed.preferences.sources)
        assertEquals(preferencesBeforeMarkUnread.topics, reversed.preferences.topics)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeMarkUnread, reversed.preferences)
    }

    @Test
    // Scenario: Mark unread of an article carrying no Read signal reverses nothing either way
    fun `Scenario - Mark unread of an article carrying no Read signal reverses nothing either way`() {
        // Given article A is Read by id but carries no applied Read signal
        val targetArticle = arithmeticArticle()
        val beforeMarkUnread = readRecord().copy(
            article = targetArticle,
            signalsApplied = readRecord().signalsApplied.copy(read = false),
        )
        val preferencesBeforeMarkUnread = arithmeticPreferences()

        // When Mark Unread is applied
        val markUnread = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(reversalArticleId to beforeMarkUnread),
                preferences = preferencesBeforeMarkUnread,
                article = targetArticle,
                action = ArticleAction.MARK_UNREAD,
                now = actionTime,
            ),
        )

        // Then article A carries Undo by id and no keyed preference entry moves forward
        assertEquals(reversalArticleId, markUnread.undoRecord?.articleId)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeMarkUnread, markUnread.preferences)

        // When Undo is applied
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(markUnread.records, markUnread.preferences, markUnread.undoRecord),
        )

        // Then article A and every source/topic entry are restored exactly by id
        assertSame(beforeMarkUnread, reversed.records[reversalArticleId])
        assertFalse(reversed.records[reversalArticleId]?.signalsApplied?.read ?: true)
        assertPreferencesEqualEntryForEntryById(preferencesBeforeMarkUnread, reversed.preferences)
    }

    @Test
    // Scenario: Discover's Mark read is reversible
    fun `Scenario - Discover's Mark read is reversible`() {
        // Given article A is Opened and its First Open source/topic weights are already applied
        val targetArticle = arithmeticArticle()
        val beforeMarkRead = openedRecord().copy(
            article = targetArticle,
            signalsApplied = openedRecord().signalsApplied.copy(
                opened = true,
                saved = false,
                read = false,
            ),
        )
        val firstOpenPreferences = LocalState.Preferences(
            sources = mapOf(
                reversalSourceId to PreferenceEntry(weight = 0.10, interactions = 1),
                unrelatedSourceId to PreferenceEntry(weight = -0.75, interactions = 2),
            ),
            topics = mapOf(
                reversalTopicId to PreferenceEntry(weight = 0.05, interactions = 1),
                secondReversalTopicId to PreferenceEntry(weight = 0.05, interactions = 1),
                unrelatedTopicId to PreferenceEntry(weight = 0.90, interactions = 5),
            ),
        )

        // When Mark Read is applied and then undone
        val markRead = assertIs<ArticleTransition.Applied>(
            ArticleStateMachine.transition(
                records = mapOf(reversalArticleId to beforeMarkRead),
                preferences = firstOpenPreferences,
                article = targetArticle,
                action = ArticleAction.MARK_READ,
                now = actionTime,
            ),
        )
        assertEquals(reversalArticleId, markRead.undoRecord?.articleId)
        assertEquals(
            PreferenceEntry(weight = 0.35, interactions = 2),
            markRead.preferences.sources[reversalSourceId],
            "source id $reversalSourceId",
        )
        assertEquals(
            PreferenceEntry(weight = 0.25, interactions = 2),
            markRead.preferences.topics[reversalTopicId],
            "topic id $reversalTopicId",
        )
        assertEquals(
            PreferenceEntry(weight = 0.25, interactions = 2),
            markRead.preferences.topics[secondReversalTopicId],
            "topic id $secondReversalTopicId",
        )
        val reversed = assertIs<ArticleTransition.Reverted>(
            ArticleStateMachine.reverse(markRead.records, markRead.preferences, markRead.undoRecord),
        )

        // Then article A's exact Opened record survives by id, including First Open but not Mark Read
        assertSame(beforeMarkRead, reversed.records[reversalArticleId])
        assertTrue(reversed.records[reversalArticleId]?.signalsApplied?.opened == true)
        assertFalse(reversed.records[reversalArticleId]?.signalsApplied?.read ?: true)

        // And only Mark Read was reversed from each exact source/topic entry
        assertPreferencesEqualEntryForEntryById(firstOpenPreferences, reversed.preferences)
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
                preferences = noPreferences,
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

    private fun arithmeticArticle(): Article = article().copy(
        id = reversalArticleId,
        source = ArticleSource(reversalSourceId, "Reversal Source"),
        tags = listOf(
            ArticleTag(reversalTopicId, "Reversal Topic"),
            ArticleTag(secondReversalTopicId, "Second Reversal Topic"),
        ),
    )

    private fun arithmeticPreferences(): LocalState.Preferences = LocalState.Preferences(
        sources = mapOf(
            reversalSourceId to PreferenceEntry(weight = 1.25, interactions = 3),
            unrelatedSourceId to PreferenceEntry(weight = -0.75, interactions = 2),
        ),
        topics = mapOf(
            reversalTopicId to PreferenceEntry(weight = 0.50, interactions = 4),
            secondReversalTopicId to PreferenceEntry(weight = -0.40, interactions = 2),
            unrelatedTopicId to PreferenceEntry(weight = 0.90, interactions = 5),
        ),
    )

    private fun assertPreferencesEqualEntryForEntryById(
        expected: LocalState.Preferences,
        actual: LocalState.Preferences,
    ) {
        assertEquals(expected.sources, actual.sources)
        assertEquals(expected.topics, actual.topics)
        assertEquals(
            expected.sources[reversalSourceId],
            actual.sources[reversalSourceId],
            "source id $reversalSourceId",
        )
        assertEquals(
            expected.sources[unrelatedSourceId],
            actual.sources[unrelatedSourceId],
            "source id $unrelatedSourceId",
        )
        assertEquals(
            expected.topics[reversalTopicId],
            actual.topics[reversalTopicId],
            "topic id $reversalTopicId",
        )
        assertEquals(
            expected.topics[secondReversalTopicId],
            actual.topics[secondReversalTopicId],
            "topic id $secondReversalTopicId",
        )
        assertEquals(
            expected.topics[unrelatedTopicId],
            actual.topics[unrelatedTopicId],
            "topic id $unrelatedTopicId",
        )
    }

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
        const val reversalArticleId = "00000000000000000016"
        const val reversalSourceId = "reversal-source"
        const val unrelatedSourceId = "unrelated-source"
        const val reversalTopicId = "reversal-topic"
        const val secondReversalTopicId = "second-reversal-topic"
        const val unrelatedTopicId = "unrelated-topic"
    }
}
