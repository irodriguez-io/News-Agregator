"""Publisher-declared RSS/Atom discovery from one approved canonical page."""

from __future__ import annotations

from typing import Any
from urllib.parse import urljoin

from bs4 import BeautifulSoup

from ..fetch import HttpClient, validate_public_url
from .feed import parse_feed


FEED_TYPES = {
    "application/rss+xml",
    "application/atom+xml",
    "application/rdf+xml",
}
REJECTED_HINTS = ("comment", "comments", "podcast", "audio")
PRIMARY_HINTS = ("main", "primary", "article", "articles", "posts", "feed", "rss")


def discover_feed_url(payload: bytes, page_url: str) -> str:
    soup = BeautifulSoup(payload, "html.parser")
    candidates: list[tuple[int, int, str]] = []
    for index, link in enumerate(soup.find_all("link", href=True)):
        relations = {str(value).casefold() for value in (link.get("rel") or [])}
        media_type = str(link.get("type") or "").split(";", 1)[0].strip().casefold()
        if "alternate" not in relations or media_type not in FEED_TYPES:
            continue
        href = urljoin(page_url, str(link["href"]).strip())
        descriptor = " ".join(
            [href, str(link.get("title") or ""), str(link.get("aria-label") or "")]
        ).casefold()
        if any(term in descriptor for term in REJECTED_HINTS):
            continue
        score = sum(1 for term in PRIMARY_HINTS if term in descriptor)
        candidates.append((-score, index, href))
    if not candidates:
        raise ValueError("no_publisher_feed")
    candidates.sort()
    selected = candidates[0][2]
    validate_public_url(selected)
    return selected


def fetch_autodiscovered_feed(source: dict[str, Any], client: HttpClient) -> list[dict[str, Any]]:
    page = client.get(source["url"])
    feed_url = discover_feed_url(page.body, page.url)
    feed = client.get(feed_url, derived=True)
    return parse_feed(feed.body, feed.url)
