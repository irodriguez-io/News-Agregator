from __future__ import annotations

from copy import deepcopy
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit

import pytest

from pipeline.configuration import load_configuration
from pipeline.normalize import normalize_entry
from pipeline.scoring import compute_metadata, score_article
from pipeline.taxonomy import match_topics


ROOT = Path(__file__).resolve().parents[2]
FIXTURES = Path(__file__).parent / "fixtures"
NOW = datetime(2026, 8, 16, 23, 0, 0, tzinfo=timezone.utc)


@pytest.fixture(scope="session")
def configuration():
    return load_configuration(ROOT / "config/sources.json", ROOT / "config/topics.json")


@pytest.fixture
def sources(configuration):
    return {source["id"]: deepcopy(source) for source in configuration[0]}


@pytest.fixture
def topics(configuration):
    return deepcopy(configuration[1])


def make_article(
    source,
    *,
    title="Software architecture report",
    url=None,
    published="2026-08-15T12:00:00Z",
    author="Author",
    summary="A detailed software architecture excerpt that is deliberately more than eighty characters for metadata scoring.",
    generated_at=NOW,
):
    source_for_normalization = deepcopy(source)
    configured = urlsplit(source["url"])
    if url is None:
        url = urlunsplit((configured.scheme, configured.netloc, "/fixture/article", "", ""))
    else:
        article_origin = urlsplit(url)
        source_for_normalization["url"] = urlunsplit(
            (article_origin.scheme, article_origin.netloc, "/", "", "")
        )
    raw = {
        "title": title,
        "url": url,
        "published": published,
        "author": author,
        "summary": summary,
        "content": [],
    }
    article = normalize_entry(raw, source_for_normalization, generated_at)
    assert article is not None
    compute_metadata(article)
    return article


def scored_article(source, topics, **kwargs):
    article = make_article(source, **kwargs)
    match_topics(article, topics)
    compute_metadata(article)
    return score_article(article, kwargs.get("generated_at", NOW))
