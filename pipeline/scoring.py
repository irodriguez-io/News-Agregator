"""Exact deterministic V1 metadata, freshness, and base scoring."""

from __future__ import annotations

from datetime import datetime
from typing import Any

from .normalize import parse_utc


def metadata_score(article: dict[str, Any]) -> int:
    score = 2 if article["publishedAt"] is not None else 0
    excerpt_length = len(article["excerpt"])
    if excerpt_length >= 80:
        score += 2
    elif excerpt_length:
        score += 1
    if article["author"]:
        score += 1
    return score


def freshness_score(published_at: str | None, generated_at: datetime) -> int:
    if published_at is None:
        return 5
    parsed = parse_utc(published_at)
    if parsed is None:
        return 5
    age_seconds = max(0.0, (generated_at - parsed).total_seconds())
    age_days = age_seconds / 86400
    if age_days <= 1:
        return 15
    if age_days <= 3:
        return 13
    if age_days <= 7:
        return 10
    if age_days <= 14:
        return 7
    if age_days <= 30:
        return 4
    return 1


def compute_metadata(article: dict[str, Any]) -> int:
    article["_metadata"] = metadata_score(article)
    return article["_metadata"]


def score_article(article: dict[str, Any], generated_at: datetime) -> dict[str, Any]:
    source_quality = article["_sourceQuality"]
    content_type = article["_contentTypeScore"]
    freshness = freshness_score(article["publishedAt"], generated_at)
    topic_signal = article["_topicSignal"]
    metadata = article["_metadata"]
    article["score"] = {
        "base": source_quality + content_type + freshness + topic_signal + metadata,
        "sourceQuality": source_quality,
        "contentType": content_type,
        "freshness": freshness,
        "topicSignal": topic_signal,
        "metadata": metadata,
    }
    return article


def public_article(article: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in article.items() if not key.startswith("_")}
