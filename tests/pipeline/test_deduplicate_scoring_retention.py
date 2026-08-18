from __future__ import annotations

from copy import deepcopy
from datetime import timedelta

import pytest

from pipeline.deduplicate import are_duplicates, deduplicate
from pipeline.normalize import iso_utc
from pipeline.retention import retain_articles
from pipeline.scoring import compute_metadata, freshness_score, metadata_score, score_article

from conftest import NOW, make_article


def test_exact_canonical_url_duplicates_collapse(sources):
    first = make_article(sources["quanta"], url="https://example.com/same")
    second = make_article(sources["science_aaas"], url="https://example.com/same")
    unique, count = deduplicate([first, second])
    assert count == 1
    assert unique == [second]  # Science / AAAS has higher configured quality.


def test_same_source_near_duplicate_with_dates_inside_window(sources):
    title = "A detailed OAuth authorization protocol revision for implementers"
    first = make_article(sources["quanta"], title=title, url="https://example.com/one", published="2026-08-01T00:00:00Z")
    second = make_article(sources["quanta"], title=title + " update", url="https://example.com/two", published="2026-08-14T00:00:00Z")
    assert are_duplicates(first, second)


def test_same_source_near_duplicate_outside_date_window_is_retained(sources):
    title = "A detailed OAuth authorization protocol revision for implementers"
    first = make_article(sources["quanta"], title=title, url="https://example.com/one", published="2026-07-01T00:00:00Z")
    second = make_article(sources["quanta"], title=title, url="https://example.com/two", published="2026-08-01T00:00:01Z")
    assert not are_duplicates(first, second)


def test_same_source_unknown_date_uses_stricter_similarity(sources):
    title = "A very long normalized title about deterministic distributed architecture"
    first = make_article(sources["quanta"], title=title, url="https://example.com/one", published=None)
    second = make_article(sources["quanta"], title=title + "s", url="https://example.com/two", published=None)
    assert are_duplicates(first, second)
    second["title"] = "A substantially different title"
    assert not are_duplicates(first, second)


def test_cross_source_exact_normalized_title_inside_72_hours_collapses(sources):
    first = make_article(sources["quanta"], title="A Shared Event!", url="https://a.example/event", published="2026-08-10T00:00:00Z")
    second = make_article(sources["science_aaas"], title="a shared event", url="https://b.example/event", published="2026-08-12T23:59:59Z")
    assert are_duplicates(first, second)


@pytest.mark.parametrize(
    ("second_title", "second_date"),
    [
        ("A Shared Event", "2026-08-13T00:00:01Z"),
        ("A Shared Event revised", "2026-08-11T00:00:00Z"),
        ("A Shared Event", None),
    ],
)
def test_cross_source_nonqualifying_cases_are_retained(sources, second_title, second_date):
    first = make_article(sources["quanta"], title="A Shared Event", url="https://a.example/event", published="2026-08-10T00:00:00Z")
    second = make_article(sources["science_aaas"], title=second_title, url="https://b.example/event", published=second_date)
    assert not are_duplicates(first, second)


def test_duplicate_winner_metadata_then_date_then_excerpt(sources):
    source = sources["quanta"]
    low_metadata = make_article(source, title="Same", url="https://example.com/same", published=None, author=None, summary="")
    high_metadata = make_article(source, title="Same", url="https://example.com/same", published="2026-08-01T00:00:00Z", author="A", summary="x" * 80)
    winner, _ = deduplicate([low_metadata, high_metadata])
    assert winner == [high_metadata]

    older = make_article(source, title="Same", url="https://example.com/same2", published="2026-08-01T00:00:00Z", author="A", summary="x" * 80)
    newer = make_article(source, title="Same", url="https://example.com/same2", published="2026-08-02T00:00:00Z", author="A", summary="x" * 80)
    assert deduplicate([older, newer])[0] == [newer]

    shorter = make_article(source, title="Same", url="https://example.com/same3", published=None, author=None, summary="short")
    richer = make_article(source, title="Same", url="https://example.com/same3", published=None, author=None, summary="a richer excerpt")
    assert deduplicate([shorter, richer])[0] == [richer]


def test_duplicate_winner_does_not_use_content_type_score(sources):
    first_source = deepcopy(sources["quanta"])
    second_source = deepcopy(sources["quanta"])
    first_source["id"] = "a_source"
    second_source["id"] = "b_source"
    first_source["contentType"]["score"] = 0
    second_source["contentType"]["score"] = 20
    first = make_article(first_source, title="Same", url="https://example.com/same", published=None, author=None, summary="")
    second = make_article(second_source, title="Same", url="https://example.com/same", published=None, author=None, summary="")
    assert deduplicate([second, first])[0] == [first]


@pytest.mark.parametrize(
    ("published", "excerpt", "author", "expected"),
    [
        (None, "", None, 0),
        (None, "x" * 79, None, 1),
        (None, "x" * 80, None, 2),
        ("2026-08-01T00:00:00Z", "", None, 2),
        ("2026-08-01T00:00:00Z", "x" * 80, "A", 5),
    ],
)
def test_metadata_score_exact_boundaries(sources, published, excerpt, author, expected):
    article = make_article(sources["quanta"], published=published, summary=excerpt, author=author)
    article["readingTimeMinutes"] = 99
    assert metadata_score(article) == expected


@pytest.mark.parametrize(
    ("age", "expected"),
    [
        (timedelta(days=1), 15),
        (timedelta(days=1, seconds=1), 13),
        (timedelta(days=3), 13),
        (timedelta(days=3, seconds=1), 10),
        (timedelta(days=7), 10),
        (timedelta(days=7, seconds=1), 7),
        (timedelta(days=14), 7),
        (timedelta(days=14, seconds=1), 4),
        (timedelta(days=30), 4),
        (timedelta(days=30, seconds=1), 1),
    ],
)
def test_freshness_exact_boundaries(age, expected):
    assert freshness_score(iso_utc(NOW - age), NOW) == expected


def test_freshness_unknown_and_minor_future():
    assert freshness_score(None, NOW) == 5
    assert freshness_score(iso_utc(NOW + timedelta(hours=6)), NOW) == 15


def test_base_score_is_exact_component_sum(sources):
    article = make_article(
        sources["quanta"],
        title="Quantum physics",
        summary="x" * 80,
        published="2026-08-16T12:00:00Z",
    )
    article["_topicSignal"] = 4
    compute_metadata(article)
    score_article(article, NOW)
    assert article["score"] == {
        "base": 90,
        "sourceQuality": 48,
        "contentType": 18,
        "freshness": 15,
        "topicSignal": 4,
        "metadata": 5,
    }


def _retention_article(index, source="source", *, days=1, score=80, published=True):
    return {
        "id": f"{index:020x}",
        "source": {"id": source},
        "publishedAt": iso_utc(NOW - timedelta(days=days)) if published else None,
        "score": {"base": score},
    }


def test_retention_removes_only_known_dates_older_than_45_days():
    articles = [
        _retention_article(1, days=45),
        _retention_article(2, days=45, published=False),
        _retention_article(3, days=46),
    ]
    retained, expired, _, _ = retain_articles(articles, NOW)
    assert {article["id"] for article in retained} == {f"{1:020x}", f"{2:020x}"}
    assert expired == 1


def test_retention_per_source_cap_keeps_best_40():
    articles = [_retention_article(index, score=index) for index in range(45)]
    retained, _, source_capped, _ = retain_articles(articles, NOW)
    assert len(retained) == 40
    assert source_capped == 5
    assert min(article["score"]["base"] for article in retained) == 5


def test_retention_global_cap_is_deterministic():
    articles = [
        _retention_article(index, source=f"source_{index // 25:02}", score=80)
        for index in range(501)
    ]
    retained, _, _, globally_capped = retain_articles(articles, NOW)
    assert len(retained) == 500
    assert globally_capped == 1
    assert [article["id"] for article in retained] == sorted(article["id"] for article in articles)[:500]
