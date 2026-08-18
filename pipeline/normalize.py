"""Plain-text, URL, identity, date, and raw-entry normalization."""

from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import html
import ipaddress
import math
import re
import unicodedata
from typing import Any, Callable
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from bs4 import BeautifulSoup
from dateutil import parser as date_parser

from .constants import (
    AUTHOR_MAX_CHARS,
    EXCERPT_MAX_CHARS,
    MIN_READING_TIME_WORDS,
    READING_WORDS_PER_MINUTE,
    TITLE_MAX_CHARS,
)


TRACKING_PARAMETERS = {
    "fbclid",
    "gclid",
    "dclid",
    "msclkid",
    "mc_cid",
    "mc_eid",
    "igshid",
    "vero_id",
    "oly_anon_id",
    "oly_enc_id",
}


def plain_text(value: Any) -> str:
    if value is None:
        return ""
    soup = BeautifulSoup(str(value), "html.parser")
    for element in soup(["script", "style", "noscript", "template"]):
        element.decompose()
    text = html.unescape(soup.get_text(" "))
    text = unicodedata.normalize("NFKC", text)
    return " ".join(text.split()).strip()


def _hard_bound(value: str, maximum: int) -> str:
    if len(value) <= maximum:
        return value
    target = value[: maximum - 1].rstrip()
    if " " in target:
        word_boundary = target.rfind(" ")
        if word_boundary >= maximum // 2:
            target = target[:word_boundary].rstrip()
    return target + "…"


def bound_excerpt(value: str, maximum: int = EXCERPT_MAX_CHARS) -> str:
    if len(value) <= maximum:
        return value
    window = value[: maximum - 1]
    sentence_ends = [match.end() for match in re.finditer(r"[.!?](?=\s|$)", window)]
    if sentence_ends and sentence_ends[-1] >= maximum // 2:
        return window[: sentence_ends[-1]].rstrip() + "…"
    return _hard_bound(value, maximum)


def canonicalize_url(value: Any) -> str | None:
    if not isinstance(value, str) or not value.strip():
        return None
    try:
        parsed = urlsplit(value.strip())
        port = parsed.port
    except ValueError:
        return None
    scheme = parsed.scheme.lower()
    hostname = parsed.hostname
    if scheme not in {"http", "https"} or not hostname or parsed.username or parsed.password:
        return None
    hostname = hostname.lower()
    if any(character.isspace() or unicodedata.category(character) == "Cc" for character in hostname):
        return None
    if hostname.startswith(".") or hostname.endswith(".") or ".." in hostname:
        return None
    if hostname == "localhost" or hostname.endswith(".localhost"):
        return None
    try:
        if not ipaddress.ip_address(hostname.split("%", 1)[0]).is_global:
            return None
    except ValueError:
        pass
    host_display = f"[{hostname}]" if ":" in hostname else hostname
    if port is not None and not ((scheme == "http" and port == 80) or (scheme == "https" and port == 443)):
        host_display = f"{host_display}:{port}"
    path = parsed.path or "/"
    if path != "/" and path.endswith("/"):
        path = path[:-1]
    remaining = []
    for name, parameter_value in parse_qsl(parsed.query, keep_blank_values=True):
        lowered = name.casefold()
        if lowered.startswith("utm_") or lowered in TRACKING_PARAMETERS:
            continue
        remaining.append((name, parameter_value))
    remaining.sort(key=lambda pair: (pair[0], pair[1]))
    query = urlencode(remaining, doseq=True)
    return urlunsplit((scheme, host_display, path, query, ""))


def article_id(canonical_url: str) -> str:
    return hashlib.sha256(canonical_url.encode("utf-8")).hexdigest()[:20]


def publisher_url_allowed(article_url: str, configured_url: str) -> bool:
    article_host = (urlsplit(article_url).hostname or "").lower()
    configured_host = (urlsplit(configured_url).hostname or "").lower()
    configured_labels = configured_host.split(".")
    if len(configured_labels) < 2:
        return article_host == configured_host
    publisher_domain = ".".join(configured_labels[-2:])
    return article_host == publisher_domain or article_host.endswith(f".{publisher_domain}")


def parse_utc(value: Any) -> datetime | None:
    if not isinstance(value, str) or not value.strip():
        return None
    try:
        parsed = date_parser.parse(value.strip())
    except (ValueError, TypeError, OverflowError):
        return None
    if parsed.tzinfo is None or parsed.utcoffset() is None:
        return None
    return parsed.astimezone(timezone.utc)


def iso_utc(value: datetime) -> str:
    return value.astimezone(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def normalize_publication_date(
    value: Any,
    generated_at: datetime,
    *,
    warning: Callable[[str], None] | None = None,
) -> str | None:
    parsed = parse_utc(value)
    if parsed is None:
        return None
    if (parsed - generated_at).total_seconds() > 6 * 60 * 60:
        if warning:
            warning("future_publication_date")
        return None
    return iso_utc(parsed)


def reading_time(content_values: Any) -> int | None:
    if not isinstance(content_values, list):
        return None
    representations = [plain_text(value) for value in content_values if isinstance(value, str)]
    word_counts = [
        len(re.findall(r"\b[^\W_]+(?:['’\-][^\W_]+)*\b", value, flags=re.UNICODE))
        for value in representations
    ]
    word_count = max(word_counts, default=0)
    if word_count < MIN_READING_TIME_WORDS:
        return None
    return max(2, math.ceil(word_count / READING_WORDS_PER_MINUTE))


def normalize_entry(
    raw: dict[str, Any],
    source: dict[str, Any],
    generated_at: datetime,
    *,
    warning: Callable[[str], None] | None = None,
) -> dict[str, Any] | None:
    title = _hard_bound(plain_text(raw.get("title")), TITLE_MAX_CHARS)
    if not title or not any(character.isalnum() for character in title):
        return None
    url = canonicalize_url(raw.get("url"))
    if url is None or not publisher_url_allowed(url, source["url"]):
        return None
    author_value = plain_text(raw.get("author"))
    author = _hard_bound(author_value, AUTHOR_MAX_CHARS) if author_value else None
    summary = plain_text(raw.get("summary"))
    if not summary:
        content_values = raw.get("content")
        if isinstance(content_values, list) and content_values:
            summary = plain_text(content_values[0])
    excerpt = bound_excerpt(summary)
    published_at = normalize_publication_date(raw.get("published"), generated_at, warning=warning)
    return {
        "id": article_id(url),
        "title": title,
        "url": url,
        "source": {"id": source["id"], "name": source["name"]},
        "category": source["category"],
        "publishedAt": published_at,
        "author": author,
        "excerpt": excerpt,
        "readingTimeMinutes": reading_time(raw.get("content")),
        "tags": [],
        "contentType": {
            "id": source["contentType"]["id"],
            "label": source["contentType"]["label"],
        },
        "_sourceQuality": source["quality"],
        "_contentTypeScore": source["contentType"]["score"],
        "_organicTopicIds": [],
        "_organicEvidence": {},
        "_topicSignal": 0,
        "_metadata": 0,
    }
