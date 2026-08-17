"""Narrow parsers for the three approved HTML listing sources."""

from __future__ import annotations

from collections.abc import Iterable
import re
from typing import Any
from urllib.parse import urljoin, urlsplit

from bs4 import BeautifulSoup, Tag


APPROVED_HTML_SOURCES = {
    "anthropic_engineering",
    "barbell_medicine",
    "okta_workflows",
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


def _unique(
    records: Iterable[dict[str, Any]], *, include_title: bool = False
) -> list[dict[str, Any]]:
    output: list[dict[str, Any]] = []
    seen: set[str | tuple[str, str]] = set()
    for record in records:
        url = str(record.get("url") or "")
        key: str | tuple[str, str] = (url, str(record.get("title") or "")) if include_title else url
        if not url or key in seen:
            continue
        seen.add(key)
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


def _okta(soup: BeautifulSoup, page_url: str) -> list[dict[str, Any]]:
    version_headings = [
        node
        for node in soup.select("main .heading-level-h3, article .heading-level-h3")
        if re.fullmatch(r"20\d{2}\.\d{2}\.\d+", _text(node) or "")
    ]
    if version_headings:
        records = []
        for heading in version_headings:
            version = _text(heading)
            summary_parts: list[str] = []
            for sibling in heading.next_siblings:
                if not isinstance(sibling, Tag):
                    continue
                sibling_text = _text(sibling)
                if "heading-level-h3" in (sibling.get("class") or []) and re.fullmatch(
                    r"20\d{2}\.\d{2}\.\d+", sibling_text or ""
                ):
                    break
                if sibling_text:
                    summary_parts.append(sibling_text)
            records.append(
                {
                    "title": f"Okta Workflows {version} production release",
                    "url": page_url,
                    "author": None,
                    "published": None,
                    "summary": " ".join(summary_parts),
                    "content": [],
                }
            )
        return _unique(records, include_title=True)

    records = []
    for container in soup.select("main article, main .release, main [class*='release'], article"):
        heading = container.find(["h2", "h3", "h4"])
        anchor = heading.find("a", href=True) if heading else None
        if anchor is None:
            anchor = container.find("a", href=True)
        if heading is None or anchor is None:
            continue
        records.append(_record(anchor, page_url, _text(heading)))
    if records:
        return _unique(records)

    # Current documentation layouts commonly expose release entries as linked headings.
    for heading in soup.select("main h2, main h3, main h4"):
        anchor = heading.find("a", href=True) or heading.find_next("a", href=True)
        if anchor is not None:
            records.append(_record(anchor, page_url, _text(heading)))
    return _unique(records)


def parse_html_listing(source_id: str, payload: bytes, page_url: str) -> list[dict[str, Any]]:
    if source_id not in APPROVED_HTML_SOURCES:
        raise ValueError("unapproved_html_source")
    soup = BeautifulSoup(payload, "html.parser")
    if source_id == "anthropic_engineering":
        return _anthropic(soup, page_url)
    if source_id == "barbell_medicine":
        return _barbell(soup, page_url)
    return _okta(soup, page_url)
