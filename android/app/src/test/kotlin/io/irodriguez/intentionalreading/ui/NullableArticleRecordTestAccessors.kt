package io.irodriguez.intentionalreading.ui

import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import java.time.Instant

private fun ArticleRecord?.requireForwardRecord(): ArticleRecord =
    requireNotNull(this) { "A forward applied transition must contain its resulting record" }

internal val ArticleRecord?.status: ArticleStatus
    get() = requireForwardRecord().status

internal val ArticleRecord?.firstSeenAt: Instant
    get() = requireForwardRecord().firstSeenAt

internal val ArticleRecord?.savedAt: Instant?
    get() = requireForwardRecord().savedAt
