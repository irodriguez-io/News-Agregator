"""RSS and Atom normalization into raw source records."""

from __future__ import annotations

from typing import Any
from urllib.parse import urljoin

import feedparser


def _entry_link(entry: Any) -> str | None:
    link = entry.get("link")
    if isinstance(link, str) and link.strip():
        return link.strip()
    for candidate in entry.get("links", []):
        href = candidate.get("href")
        relation = candidate.get("rel", "alternate")
        if relation == "alternate" and isinstance(href, str) and href.strip():
            return href.strip()
    return None


def _content_values(entry: Any) -> list[str]:
    values: list[str] = []
    for content in entry.get("content", []):
        value = content.get("value") if isinstance(content, dict) else None
        if isinstance(value, str) and value.strip():
            values.append(value)
    return values


def parse_feed(payload: bytes, base_url: str | None = None) -> list[dict[str, Any]]:
    parsed = feedparser.parse(payload, resolve_relative_uris=False, sanitize_html=False)
    entries: list[dict[str, Any]] = []
    for entry in parsed.entries:
        link = _entry_link(entry)
        entries.append(
            {
                "title": entry.get("title"),
                "url": urljoin(base_url, link) if base_url and link else link,
                "author": entry.get("author"),
                "published": entry.get("published") or entry.get("updated"),
                "summary": entry.get("summary") or entry.get("description"),
                "content": _content_values(entry),
            }
        )
    if getattr(parsed, "bozo", False) and not entries:
        return []
    return entries
