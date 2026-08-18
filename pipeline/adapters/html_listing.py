"""Narrow parsers for the two approved HTML listing sources."""

from __future__ import annotations

from collections.abc import Iterable
from typing import Any
from urllib.parse import urljoin, urlsplit

from bs4 import BeautifulSoup, Tag


APPROVED_HTML_SOURCES = {
    "anthropic_engineering",
    "barbell_medicine",
}


def _text(node: Tag | None) -> str | None:
    if node is None:
        return None
    value = node.get_text(" ", strip=True)
    return value or None


def _nearby_metadata(anchor: Tag) -> tuple[str | None, str | None, str | None]:
    container = anchor.find_parent(["article", "li", "section", "div"]) or anchor
    date_node = container.find("time")
    published = None
    if date_node is not None:
        published = date_node.get("datetime") or _text(date_node)
    author_node = container.select_one("[rel='author'], .author, [class*='author']")
    excerpt_node = container.select_one("p, .excerpt, .summary, [class*='description']")
    return _text(author_node), published, _text(excerpt_node)


def _record(anchor: Tag, page_url: str, title: str | None = None) -> dict[str, Any]:
    author, published, summary = _nearby_metadata(anchor)
    return {
        "title": title or _text(anchor),
        "url": urljoin(page_url, str(anchor.get("href") or "")),
        "author": author,
        "published": published,
        "summary": summary,
        "content": [],
    }


def _unique(records: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    output: list[dict[str, Any]] = []
    seen: set[str] = set()
    for record in records:
        url = str(record.get("url") or "")
        if not url or url in seen:
            continue
        seen.add(url)
        output.append(record)
    return output


def _anthropic(soup: BeautifulSoup, page_url: str) -> list[dict[str, Any]]:
    records = []
    page_host = urlsplit(page_url).hostname
    for anchor in soup.select("main a[href], article a[href]"):
        url = urljoin(page_url, str(anchor.get("href")))
        parsed = urlsplit(url)
        path = parsed.path.rstrip("/")
        if parsed.hostname != page_host or not path.startswith("/engineering/") or path == "/engineering":
            continue
        heading = anchor.find(["h1", "h2", "h3", "h4"])
        title = _text(heading) or _text(anchor)
        if title:
            records.append(_record(anchor, page_url, title))
    return _unique(records)


def _barbell(soup: BeautifulSoup, page_url: str) -> list[dict[str, Any]]:
    records = []
    page_host = urlsplit(page_url).hostname
    for heading in soup.select("main h1, main h2, main h3, article h1, article h2, article h3"):
        anchor = heading.find("a", href=True) or heading.find_parent("a", href=True)
        if anchor is None:
            continue
        url = urljoin(page_url, str(anchor.get("href")))
        parsed = urlsplit(url)
        if parsed.hostname != page_host or parsed.path.rstrip("/") == urlsplit(page_url).path.rstrip("/"):
            continue
        title = _text(heading)
        if title:
            records.append(_record(anchor, page_url, title))
    return _unique(records)


def parse_html_listing(source_id: str, payload: bytes, page_url: str) -> list[dict[str, Any]]:
    if source_id not in APPROVED_HTML_SOURCES:
        raise ValueError("unapproved_html_source")
    soup = BeautifulSoup(payload, "html.parser")
    if source_id == "anthropic_engineering":
        return _anthropic(soup, page_url)
    return _barbell(soup, page_url)
