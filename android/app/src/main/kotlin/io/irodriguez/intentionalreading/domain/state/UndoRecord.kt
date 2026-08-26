package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.ArticleAction
import io.irodriguez.intentionalreading.domain.model.ArticleRecord

typealias PreferenceReversal = Nothing

data class UndoRecord(
    val articleId: String,
    val action: ArticleAction,
    val previousRecord: ArticleRecord?,
    val preferenceReversal: PreferenceReversal? = null,
)
