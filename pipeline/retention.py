"""Exact 45-day, 40-per-source, and 500-total V1 retention sequence."""

from __future__ import annotations

from collections import defaultdict
from datetime import datetime
from typing import Any

from .normalize import parse_utc


def article_order_key(article: dict[str, Any]) -> tuple[Any, ...]:
    published = parse_utc(article["publishedAt"])
    return (
        -article["score"]["base"],
        published is None,
        -(published.timestamp() if published else 0),
        article["source"]["id"],
        article["id"],
    )


def per_source_order_key(article: dict[str, Any]) -> tuple[Any, ...]:
    published = parse_utc(article["publishedAt"])
    return (
        -article["score"]["base"],
        published is None,
        -(published.timestamp() if published else 0),
        article["id"],
    )


def retain_articles(
    articles: list[dict[str, Any]], generated_at: datetime
) -> tuple[list[dict[str, Any]], int, int, int]:
    age_eligible: list[dict[str, Any]] = []
    expired = 0
    for article in articles:
        published = parse_utc(article["publishedAt"])
        if published is not None and (generated_at - published).total_seconds() > 45 * 86400:
            expired += 1
        else:
            age_eligible.append(article)

    by_source: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for article in age_eligible:
        by_source[article["source"]["id"]].append(article)
    source_limited: list[dict[str, Any]] = []
    source_capped = 0
    for source_id in sorted(by_source):
        ranked = sorted(by_source[source_id], key=per_source_order_key)
        source_limited.extend(ranked[:40])
        source_capped += max(0, len(ranked) - 40)

    globally_ranked = sorted(source_limited, key=article_order_key)
    globally_capped = max(0, len(globally_ranked) - 500)
    return globally_ranked[:500], expired, source_capped, globally_capped
