from __future__ import annotations

from datetime import timedelta

import pytest

from pipeline.normalize import (
    article_id,
    canonicalize_url,
    normalize_entry,
    normalize_publication_date,
    plain_text,
    publisher_url_allowed,
    reading_time,
)

from conftest import NOW


@pytest.mark.parametrize(
    ("given", "expected"),
    [
        (
            "https://example.com/article?utm_source=rss&utm_campaign=test",
            "https://example.com/article",
        ),
        ("https://example.com/article#comments", "https://example.com/article"),
        (
            "https://example.com/article?id=123&lang=en",
            "https://example.com/article?id=123&lang=en",
        ),
        (
            "https://EXAMPLE.com:443/article/?lang=en&id=123&fbclid=x",
            "https://example.com/article?id=123&lang=en",
        ),
        ("http://EXAMPLE.com:80/", "http://example.com/"),
        ("https://example.com:8443/path/", "https://example.com:8443/path"),
    ],
)
def test_canonicalize_url(given, expected):
    assert canonicalize_url(given) == expected


@pytest.mark.parametrize("url", ["javascript:alert(1)", "data:text/plain,x", "file:///tmp/x", "ftp://example.com/x", "not a url", "https://exa mple.com/x", "https://.example.com/x", "https://localhost/x", "https://127.0.0.1/x", "https://192.168.1.1/x"])
def test_unsafe_or_unusable_urls_are_rejected(url):
    assert canonicalize_url(url) is None


def test_article_id_known_value_and_stability():
    assert article_id("https://example.com/article") == "632538290468e7a39c06"
    assert article_id("https://example.com/article") == article_id("https://example.com/article")
    assert article_id("https://example.com/other") != "632538290468e7a39c06"


def test_publisher_boundary_allows_subdomains_but_rejects_third_parties():
    assert publisher_url_allowed(
        "https://www.science.org/article", "https://feeds.science.org/rss/science.xml"
    )
    assert not publisher_url_allowed(
        "https://content.third-party.example/article", "https://spectrum.ieee.org/"
    )


def test_normalization_rejects_third_party_links_from_approved_feed(sources):
    raw = {
        "title": "Syndicated link",
        "url": "https://third-party.example/article",
        "summary": "External material",
        "content": [],
    }
    assert normalize_entry(raw, sources["quanta"], NOW) is None


def test_plain_text_removes_markup_scripts_styles_and_normalizes_unicode():
    raw = "<script>alert(1)</script><style>x{}</style><strong>ＯAuth&nbsp;Update</strong>  now"
    assert plain_text(raw) == "OAuth Update now"


def test_normalize_entry_bounds_fields_and_rejects_bad_title(sources):
    raw = {
        "title": "A" * 600,
        "url": "https://www.quantamagazine.org/long",
        "author": "B" * 300,
        "published": "2026-08-15T10:00:00-06:00",
        "summary": ("Sentence one. " * 100),
        "content": [],
    }
    article = normalize_entry(raw, sources["quanta"], NOW)
    assert article is not None
    assert len(article["title"]) <= 500 and article["title"].endswith("…")
    assert article["author"] is not None and len(article["author"]) <= 200
    assert len(article["excerpt"]) <= 800 and article["excerpt"].endswith("…")
    assert article["publishedAt"] == "2026-08-15T16:00:00Z"
    raw["title"] = "--- !!!"
    assert normalize_entry(raw, sources["quanta"], NOW) is None


def test_normalize_entry_rejects_missing_url_without_guid_fallback(sources):
    raw = {"title": "Valid title", "url": None, "guid": "fallback", "summary": "x", "content": []}
    assert normalize_entry(raw, sources["quanta"], NOW) is None


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        ("2026-08-16T17:00:00-06:00", "2026-08-16T23:00:00Z"),
        ("2026-08-16T23:00:00Z", "2026-08-16T23:00:00Z"),
        (None, None),
        ("invalid", None),
        ("2026-08-16 23:00:00", None),
    ],
)
def test_publication_date_normalization(value, expected):
    assert normalize_publication_date(value, NOW) == expected


def test_future_date_within_six_hours_is_retained_and_beyond_is_unknown():
    within = (NOW + timedelta(hours=6)).isoformat()
    beyond = (NOW + timedelta(hours=6, seconds=1)).isoformat()
    warnings = []
    assert normalize_publication_date(within, NOW) == "2026-08-17T05:00:00Z"
    assert normalize_publication_date(beyond, NOW, warning=warnings.append) is None
    assert warnings == ["future_publication_date"]


def test_reading_time_requires_400_source_supplied_words():
    assert reading_time(["word " * 399]) is None
    assert reading_time(["word " * 400]) == 2
    assert reading_time(["word " * 451]) == 3
    assert reading_time(["word " * 250, "word " * 250]) is None
    assert reading_time(None) is None


def test_short_excerpt_does_not_create_reading_time(sources):
    article = normalize_entry(
        {
            "title": "A useful report",
            "url": "https://www.quantamagazine.org/short",
            "summary": "word " * 399,
            "content": [],
        },
        sources["quanta"],
        NOW,
    )
    assert article is not None
    assert article["readingTimeMinutes"] is None
