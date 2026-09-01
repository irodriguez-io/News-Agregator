package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.LocalState
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import java.time.Instant

enum class ArticleTransitionErrorCode {
    ACTION_NOT_ALLOWED,
    UNDO_UNAVAILABLE,
    UNDO_STALE,
}

sealed interface ArticleTransition {
    val records: Map<String, ArticleRecord>
    val preferences: LocalState.Preferences

    data class Applied(
        override val records: Map<String, ArticleRecord>,
        val record: ArticleRecord,
        val undoRecord: UndoRecord? = null,
        override val preferences: LocalState.Preferences,
    ) : ArticleTransition

    data class Reverted(
        override val records: Map<String, ArticleRecord>,
        val record: ArticleRecord?,
        override val preferences: LocalState.Preferences,
    ) : ArticleTransition

    data class Unchanged(
        override val records: Map<String, ArticleRecord>,
        override val preferences: LocalState.Preferences,
    ) : ArticleTransition

    data class Invalid(
        override val records: Map<String, ArticleRecord>,
        val action: ArticleAction?,
        val fromStatus: ArticleStatus?,
        val code: ArticleTransitionErrorCode = ArticleTransitionErrorCode.ACTION_NOT_ALLOWED,
        override val preferences: LocalState.Preferences,
    ) : ArticleTransition
}

object ArticleStateMachine {
    fun transition(
        records: Map<String, ArticleRecord>,
        preferences: LocalState.Preferences,
        article: Article,
        action: ArticleAction,
        now: Instant,
    ): ArticleTransition {
        val existing = records[article.id]
        val status = existing?.status ?: ArticleStatus.UNSEEN
        if (status !in allowedFrom.getValue(action)) {
            return ArticleTransition.Invalid(records, action, status, preferences = preferences)
        }

        if (isIdempotentNoOp(existing, status, action)) {
            return ArticleTransition.Unchanged(records, preferences)
        }

        val current = existing ?: ArticleRecord(
            article = article,
            status = ArticleStatus.OPENED,
            firstSeenAt = now,
            openedAt = null,
            savedAt = null,
            dismissedAt = null,
            readAt = null,
            signalsApplied = SignalsApplied(
                opened = false,
                saved = false,
                dismissed = false,
                read = false,
            ),
        )
        val transitioned = when (action) {
            ArticleAction.OPEN -> current.copy(
                status = if (existing == null) ArticleStatus.OPENED else current.status,
                openedAt = current.openedAt ?: now,
            )
            ArticleAction.SAVE -> current.copy(
                status = ArticleStatus.SAVED,
                savedAt = now,
                dismissedAt = null,
                readAt = null,
            )
            ArticleAction.DISMISS -> current.copy(
                status = ArticleStatus.DISMISSED,
                dismissedAt = now,
                savedAt = null,
                readAt = null,
            )
            ArticleAction.MARK_READ -> current.copy(
                status = ArticleStatus.READ,
                readAt = now,
                savedAt = null,
                dismissedAt = null,
            )
            ArticleAction.MARK_UNREAD -> current.copy(
                status = ArticleStatus.SAVED,
                savedAt = now,
                readAt = null,
                dismissedAt = null,
            )
            ArticleAction.REMOVE -> current.copy(
                status = ArticleStatus.DISMISSED,
                dismissedAt = now,
                savedAt = null,
                readAt = null,
            )
        }
        var next = transitioned
        var nextPreferences = preferences
        var preferenceSignalApplied = false
        var preferenceSignalReversed = false
        if (action == ArticleAction.MARK_UNREAD && current.signalsApplied.read) {
            nextPreferences = PreferenceLearning.reverse(
                preferences = preferences,
                article = current.article,
                event = PreferenceEvent.MARK_READ,
            )
            next = transitioned.copy(signalsApplied = current.signalsApplied.copy(read = false))
            preferenceSignalReversed = true
        } else {
            val preferenceEvent = preferenceEvents[action]
            if (preferenceEvent != null && !current.signalsApplied.isAppliedFor(action)) {
                nextPreferences = PreferenceLearning.apply(
                    preferences = preferences,
                    article = current.article,
                    event = preferenceEvent,
                )
                next = transitioned.copy(signalsApplied = current.signalsApplied.withApplied(action))
                preferenceSignalApplied = true
            }
        }
        val nextRecords = buildMap {
            putAll(records)
            put(article.id, next)
        }
        val undoRecord = if (action in reversibleActions) {
            UndoRecord(
                articleId = article.id,
                action = action,
                previousRecord = existing,
                preferenceReversal = if (preferenceSignalApplied) {
                    preferenceReversals[action]
                } else {
                    null
                },
                preferenceReapplication = if (preferenceSignalReversed) {
                    PreferenceEvent.MARK_READ
                } else {
                    null
                },
            )
        } else {
            null
        }
        return ArticleTransition.Applied(
            records = nextRecords,
            record = next,
            undoRecord = undoRecord,
            preferences = nextPreferences,
        )
    }

    fun reverse(
        records: Map<String, ArticleRecord>,
        preferences: LocalState.Preferences,
        undoRecord: UndoRecord?,
    ): ArticleTransition {
        if (undoRecord == null || undoRecord.action !in reversibleActions) {
            return ArticleTransition.Invalid(
                records = records,
                action = undoRecord?.action,
                fromStatus = undoRecord?.let { records[it.articleId]?.status },
                code = ArticleTransitionErrorCode.UNDO_UNAVAILABLE,
                preferences = preferences,
            )
        }

        val current = records[undoRecord.articleId]
        if (current == null) {
            return ArticleTransition.Invalid(
                records = records,
                action = undoRecord.action,
                fromStatus = null,
                code = ArticleTransitionErrorCode.UNDO_STALE,
                preferences = preferences,
            )
        }
        val nextPreferences = when {
            undoRecord.preferenceReversal != null ->
                PreferenceLearning.reverse(
                    preferences = preferences,
                    article = current.article,
                    event = undoRecord.preferenceReversal.event,
                )
            undoRecord.preferenceReapplication != null ->
                PreferenceLearning.apply(
                    preferences = preferences,
                    article = current.article,
                    event = undoRecord.preferenceReapplication,
                )
            else -> preferences
        }
        val nextRecords = buildMap {
            putAll(records)
            if (undoRecord.previousRecord == null) {
                remove(undoRecord.articleId)
            } else {
                put(undoRecord.articleId, undoRecord.previousRecord)
            }
        }
        return ArticleTransition.Reverted(
            records = nextRecords,
            record = undoRecord.previousRecord,
            preferences = nextPreferences,
        )
    }

    private fun isIdempotentNoOp(
        existing: ArticleRecord?,
        status: ArticleStatus,
        action: ArticleAction,
    ): Boolean = when (action) {
        ArticleAction.SAVE -> status == ArticleStatus.SAVED
        ArticleAction.DISMISS -> status == ArticleStatus.DISMISSED
        ArticleAction.MARK_READ -> status == ArticleStatus.READ
        ArticleAction.OPEN -> existing?.signalsApplied?.opened == true
        ArticleAction.MARK_UNREAD,
        ArticleAction.REMOVE,
        -> false
    }

    private val allowedFrom = mapOf(
        ArticleAction.OPEN to setOf(
            ArticleStatus.UNSEEN,
            ArticleStatus.OPENED,
            ArticleStatus.SAVED,
            ArticleStatus.READ,
        ),
        ArticleAction.SAVE to setOf(
            ArticleStatus.UNSEEN,
            ArticleStatus.OPENED,
            ArticleStatus.SAVED,
        ),
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

    private val reversibleActions = setOf(
        ArticleAction.SAVE,
        ArticleAction.DISMISS,
        ArticleAction.MARK_READ,
        ArticleAction.MARK_UNREAD,
        ArticleAction.REMOVE,
    )

    private val preferenceEvents = mapOf(
        ArticleAction.OPEN to PreferenceEvent.FIRST_OPEN,
        ArticleAction.SAVE to PreferenceEvent.SAVE_FOR_LATER,
        ArticleAction.DISMISS to PreferenceEvent.NOT_INTERESTED,
        ArticleAction.MARK_READ to PreferenceEvent.MARK_READ,
    )

    private val preferenceReversals = mapOf(
        ArticleAction.SAVE to PreferenceReversal.SAVE_FOR_LATER,
        ArticleAction.DISMISS to PreferenceReversal.NOT_INTERESTED,
        ArticleAction.MARK_READ to PreferenceReversal.MARK_READ,
    )
}

private fun SignalsApplied.isAppliedFor(action: ArticleAction): Boolean = when (action) {
    ArticleAction.OPEN -> opened
    ArticleAction.SAVE -> saved
    ArticleAction.DISMISS -> dismissed
    ArticleAction.MARK_READ -> read
    ArticleAction.MARK_UNREAD,
    ArticleAction.REMOVE,
    -> false
}

private fun SignalsApplied.withApplied(action: ArticleAction): SignalsApplied = when (action) {
    ArticleAction.OPEN -> copy(opened = true)
    ArticleAction.SAVE -> copy(saved = true)
    ArticleAction.DISMISS -> copy(dismissed = true)
    ArticleAction.MARK_READ -> copy(read = true)
    ArticleAction.MARK_UNREAD,
    ArticleAction.REMOVE,
    -> this
}
