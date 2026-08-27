package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleRecord

enum class PreferenceReversal(
    val event: PreferenceEvent,
) {
    NOT_INTERESTED(PreferenceEvent.NOT_INTERESTED),
    SAVE_FOR_LATER(PreferenceEvent.SAVE_FOR_LATER),
    FIRST_OPEN(PreferenceEvent.FIRST_OPEN),
    MARK_READ(PreferenceEvent.MARK_READ),
}

data class UndoRecord(
    val articleId: String,
    val action: ArticleAction,
    val previousRecord: ArticleRecord?,
    val preferenceReversal: PreferenceReversal? = null,
)
