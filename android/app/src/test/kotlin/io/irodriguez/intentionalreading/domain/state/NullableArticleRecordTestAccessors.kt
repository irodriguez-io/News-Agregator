package io.irodriguez.intentionalreading.domain.state

import io.irodriguez.intentionalreading.domain.model.ArticleRecord
import io.irodriguez.intentionalreading.domain.model.ArticleStatus
import java.time.Instant

internal fun ArticleRecord?.requireForwardRecord(): ArticleRecord =
    requireNotNull(this) { "A forward applied transition must contain its resulting record" }

internal val ArticleRecord?.status: ArticleStatus
    get() = requireForwardRecord().status

internal val ArticleRecord?.firstSeenAt: Instant
    get() = requireForwardRecord().firstSeenAt

internal val ArticleRecord?.openedAt: Instant?
    get() = requireForwardRecord().openedAt

internal val ArticleRecord?.savedAt: Instant?
    get() = requireForwardRecord().savedAt

internal val ArticleRecord?.dismissedAt: Instant?
    get() = requireForwardRecord().dismissedAt

internal val ArticleRecord?.readAt: Instant?
    get() = requireForwardRecord().readAt
