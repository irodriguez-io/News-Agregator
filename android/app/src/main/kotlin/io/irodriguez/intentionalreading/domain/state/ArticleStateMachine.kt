package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.Article
import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import io.irodriguez.intentionalreading.domain.model.SignalsApplied
import java.time.Instant

enum class ArticleTransitionErrorCode {
    ACTION_NOT_ALLOWED,
    UNDO_UNAVAILABLE,
    UNDO_STALE,
}

sealed interface ArticleTransition {
    val records: Map<String, ArticleRecord>

    data class Applied(
        override val records: Map<String, ArticleRecord>,
        val record: ArticleRecord?,
        val undoRecord: UndoRecord? = null,
    ) : ArticleTransition

    data class Unchanged(
        override val records: Map<String, ArticleRecord>,
    ) : ArticleTransition

    data class Invalid(
        override val records: Map<String, ArticleRecord>,
        val action: ArticleAction?,
        val fromStatus: ArticleStatus?,
        val code: ArticleTransitionErrorCode = ArticleTransitionErrorCode.ACTION_NOT_ALLOWED,
    ) : ArticleTransition
}

object ArticleStateMachine {
    fun transition(
        records: Map<String, ArticleRecord>,
        article: Article,
        action: ArticleAction,
        now: Instant,
        undoable: Boolean = false,
    ): ArticleTransition {
        val existing = records[article.id]
        val status = existing?.status ?: ArticleStatus.UNSEEN
        if (status !in allowedFrom.getValue(action)) {
            return ArticleTransition.Invalid(records, action, status)
        }

        if (isIdempotentNoOp(existing, status, action)) {
            return ArticleTransition.Unchanged(records)
        }

        val current = existing ?: ArticleRecord(
            article = article,
            status = ArticleStatus.OPENED,
            firstSeenAt = now,
            openedAt = null,
            savedAt = null,
            dismissedAt = null,
            readAt = null,
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
        val next = transitioned.copy(
            signalsApplied = SignalsApplied(
                opened = transitioned.openedAt != null,
                saved = existing?.signalsApplied?.saved ?: false,
                dismissed = existing?.signalsApplied?.dismissed == true &&
                    transitioned.status == ArticleStatus.DISMISSED,
                read = transitioned.status == ArticleStatus.READ,
            ),
        )
        val nextRecords = buildMap {
            putAll(records)
            put(article.id, next)
        }
        val undoRecord = if (undoable && action in reversibleActions) {
            UndoRecord(
                articleId = article.id,
                action = action,
                previousRecord = existing,
            )
        } else {
            null
        }
        return ArticleTransition.Applied(nextRecords, next, undoRecord)
    }

    fun reverse(
        records: Map<String, ArticleRecord>,
        undoRecord: UndoRecord?,
    ): ArticleTransition {
        if (undoRecord == null || undoRecord.action !in reversibleActions) {
            return ArticleTransition.Invalid(
                records = records,
                action = undoRecord?.action,
                fromStatus = undoRecord?.let { records[it.articleId]?.status },
                code = ArticleTransitionErrorCode.UNDO_UNAVAILABLE,
            )
        }

        if (records[undoRecord.articleId] == null) {
            return ArticleTransition.Invalid(
                records = records,
                action = undoRecord.action,
                fromStatus = null,
                code = ArticleTransitionErrorCode.UNDO_STALE,
            )
        }
        val nextRecords = buildMap {
            putAll(records)
            if (undoRecord.previousRecord == null) {
                remove(undoRecord.articleId)
            } else {
                put(undoRecord.articleId, undoRecord.previousRecord)
            }
        }
        return ArticleTransition.Applied(
            records = nextRecords,
            record = undoRecord.previousRecord,
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
        // The browser checks signalsApplied.opened. Preference signals are deferred here, and storage's
        // invariant makes that flag exactly equivalent to openedAt != null.
        ArticleAction.OPEN -> existing?.openedAt != null
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

    private val reversibleActions = setOf(ArticleAction.SAVE, ArticleAction.DISMISS)
}
