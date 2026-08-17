"""Conservative deterministic exact and title/date deduplication."""

from __future__ import annotations

from datetime import datetime
from difflib import SequenceMatcher
from typing import Any

from .normalize import parse_utc
from .taxonomy import matching_text


def _seconds_between(first: str | None, second: str | None) -> float | None:
    left = parse_utc(first)
    right = parse_utc(second)
    if left is None or right is None:
        return None
    return abs((left - right).total_seconds())


def are_duplicates(first: dict[str, Any], second: dict[str, Any]) -> bool:
    if first["url"] == second["url"]:
        return True
    first_title = matching_text(first["title"])
    second_title = matching_text(second["title"])
    if first["source"]["id"] == second["source"]["id"]:
        distance = _seconds_between(first["publishedAt"], second["publishedAt"])
        ratio = SequenceMatcher(None, first_title, second_title, autojunk=False).ratio()
        if distance is None:
            return ratio >= 0.97
        return ratio >= 0.92 and distance <= 14 * 86400
    distance = _seconds_between(first["publishedAt"], second["publishedAt"])
    return first_title == second_title and distance is not None and distance <= 72 * 60 * 60


def _winner_key(article: dict[str, Any]) -> tuple[Any, ...]:
    published = parse_utc(article["publishedAt"])
    timestamp = published.timestamp() if isinstance(published, datetime) else float("-inf")
    return (
        -article["_sourceQuality"],
        -article["_metadata"],
        -timestamp,
        -len(article["excerpt"]) if article["excerpt"] else 0,
        article["source"]["id"],
        article["id"],
    )


def deduplicate(articles: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], int]:
    count = len(articles)
    parents = list(range(count))

    def find(index: int) -> int:
        while parents[index] != index:
            parents[index] = parents[parents[index]]
            index = parents[index]
        return index

    def union(left: int, right: int) -> None:
        left_root, right_root = find(left), find(right)
        if left_root != right_root:
            parents[max(left_root, right_root)] = min(left_root, right_root)

    for left in range(count):
        for right in range(left + 1, count):
            if are_duplicates(articles[left], articles[right]):
                union(left, right)

    groups: dict[int, list[dict[str, Any]]] = {}
    for index, article in enumerate(articles):
        groups.setdefault(find(index), []).append(article)
    winners = [min(group, key=_winner_key) for _, group in sorted(groups.items())]
    return winners, count - len(winners)
