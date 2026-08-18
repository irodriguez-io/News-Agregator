"""Strict ArticleDataset v1 validation and catastrophic generation gates."""

from __future__ import annotations

from datetime import datetime
import math
import re
from typing import Any

from .constants import CATEGORIES
from .normalize import article_id, canonicalize_url, parse_utc, publisher_url_allowed
from .retention import article_order_key
from .scoring import freshness_score, metadata_score
from .taxonomy import apply_forced_tags, match_topics, passes_admission


class DatasetValidationError(ValueError):
    """The generated object is not an exact usable ArticleDataset v1."""


ARTICLE_FIELDS = {
    "id", "title", "url", "source", "category", "publishedAt", "author",
    "excerpt", "readingTimeMinutes", "tags", "contentType", "score",
}
SCORE_FIELDS = {"base", "sourceQuality", "contentType", "freshness", "topicSignal", "metadata"}


def _integer(value: Any, minimum: int, maximum: int, label: str) -> None:
    if type(value) is not int or not minimum <= value <= maximum:
        raise DatasetValidationError(f"invalid {label}")


def _timestamp(value: Any, *, nullable: bool, label: str) -> None:
    if value is None and nullable:
        return
    if not isinstance(value, str) or not value.endswith("Z") or parse_utc(value) is None:
        raise DatasetValidationError(f"invalid {label}")


def validate_article(
    article: Any,
    sources_by_id: dict[str, dict[str, Any]],
    topics_by_id: dict[str, dict[str, Any]],
    topics: list[dict[str, Any]],
    generated_at: datetime,
) -> None:
    if not isinstance(article, dict) or set(article) != ARTICLE_FIELDS:
        raise DatasetValidationError("invalid Article fields")
    if not isinstance(article["id"], str) or re.fullmatch(r"[0-9a-f]{20}", article["id"]) is None:
        raise DatasetValidationError("invalid Article ID")
    canonical = canonicalize_url(article["url"])
    if canonical is None or canonical != article["url"] or article_id(canonical) != article["id"]:
        raise DatasetValidationError("invalid canonical Article URL or identity")
    if (
        not isinstance(article["title"], str)
        or not article["title"].strip()
        or len(article["title"]) > 500
        or not any(char.isalnum() for char in article["title"])
    ):
        raise DatasetValidationError("invalid title")
    source = article["source"]
    if not isinstance(source, dict) or set(source) != {"id", "name"} or source.get("id") not in sources_by_id:
        raise DatasetValidationError("invalid source")
    configured_source = sources_by_id[source["id"]]
    if source["name"] != configured_source["name"] or article["category"] != configured_source["category"]:
        raise DatasetValidationError("source metadata mismatch")
    if not publisher_url_allowed(article["url"], configured_source["url"]):
        raise DatasetValidationError("Article URL outside approved publisher")
    if article["category"] not in CATEGORIES:
        raise DatasetValidationError("invalid category")
    _timestamp(article["publishedAt"], nullable=True, label="publishedAt")
    published_value = parse_utc(article["publishedAt"])
    if published_value is not None and (published_value - generated_at).total_seconds() > 6 * 60 * 60:
        raise DatasetValidationError("implausible future publishedAt")
    if article["author"] is not None and (
        not isinstance(article["author"], str) or not article["author"] or len(article["author"]) > 200
    ):
        raise DatasetValidationError("invalid author")
    if not isinstance(article["excerpt"], str) or len(article["excerpt"]) > 800:
        raise DatasetValidationError("invalid excerpt")
    reading_time = article["readingTimeMinutes"]
    if reading_time is not None and (type(reading_time) is not int or reading_time < 2):
        raise DatasetValidationError("invalid readingTimeMinutes")
    if not isinstance(article["tags"], list):
        raise DatasetValidationError("invalid tags")
    tag_ids: list[str] = []
    for tag in article["tags"]:
        if not isinstance(tag, dict) or set(tag) != {"id", "label"} or tag.get("id") not in topics_by_id:
            raise DatasetValidationError("invalid tag")
        topic = topics_by_id[tag["id"]]
        if tag["label"] != topic["label"] or article["category"] not in topic["categories"]:
            raise DatasetValidationError("tag metadata mismatch")
        tag_ids.append(tag["id"])
    if len(tag_ids) != len(set(tag_ids)) or len(tag_ids) > 6:
        raise DatasetValidationError("duplicate or excessive tags")
    content_type = article["contentType"]
    configured_type = configured_source["contentType"]
    if (
        not isinstance(content_type, dict)
        or set(content_type) != {"id", "label"}
        or content_type != {"id": configured_type["id"], "label": configured_type["label"]}
    ):
        raise DatasetValidationError("invalid content type")
    score = article["score"]
    if not isinstance(score, dict) or set(score) != SCORE_FIELDS:
        raise DatasetValidationError("invalid score")
    for field, maximum in {
        "base": 100, "sourceQuality": 50, "contentType": 20,
        "freshness": 15, "topicSignal": 10, "metadata": 5,
    }.items():
        _integer(score[field], 0, maximum, f"score.{field}")
    if score["sourceQuality"] != configured_source["quality"] or score["contentType"] != configured_type["score"]:
        raise DatasetValidationError("configured score mismatch")
    if score["freshness"] != freshness_score(article["publishedAt"], generated_at):
        raise DatasetValidationError("freshness score mismatch")
    if score["metadata"] != metadata_score(article):
        raise DatasetValidationError("metadata score mismatch")
    recomputed = {
        "title": article["title"],
        "excerpt": article["excerpt"],
        "category": article["category"],
        "tags": [],
    }
    match_topics(recomputed, topics)
    if not passes_admission(recomputed, configured_source):
        raise DatasetValidationError("source admission mismatch")
    apply_forced_tags(recomputed, configured_source, topics_by_id)
    if score["topicSignal"] != recomputed["_topicSignal"] or article["tags"] != recomputed["tags"]:
        raise DatasetValidationError("taxonomy or topic-signal mismatch")
    if score["base"] != sum(score[field] for field in SCORE_FIELDS if field != "base"):
        raise DatasetValidationError("base score invariant failure")


def validate_dataset(
    dataset: Any,
    sources: list[dict[str, Any]],
    topics: list[dict[str, Any]],
) -> None:
    if not isinstance(dataset, dict) or set(dataset) != {"schemaVersion", "generatedAt", "pipeline", "articles"}:
        raise DatasetValidationError("invalid dataset fields")
    if dataset["schemaVersion"] != 1:
        raise DatasetValidationError("unsupported schemaVersion")
    _timestamp(dataset["generatedAt"], nullable=False, label="generatedAt")
    pipeline = dataset["pipeline"]
    required_pipeline = {"enabledSourceCount", "successfulSourceCount", "failedSourceCount", "articleCount"}
    if not isinstance(pipeline, dict) or set(pipeline) != required_pipeline:
        raise DatasetValidationError("invalid pipeline metadata")
    for field in required_pipeline:
        if type(pipeline[field]) is not int or pipeline[field] < 0:
            raise DatasetValidationError(f"invalid pipeline.{field}")
    if pipeline["successfulSourceCount"] + pipeline["failedSourceCount"] != pipeline["enabledSourceCount"]:
        raise DatasetValidationError("source count invariant failure")
    if pipeline["enabledSourceCount"] != sum(1 for source in sources if source["enabled"]):
        raise DatasetValidationError("enabled source count mismatch")
    articles = dataset["articles"]
    if not isinstance(articles, list) or pipeline["articleCount"] != len(articles):
        raise DatasetValidationError("article count mismatch")
    sources_by_id = {source["id"]: source for source in sources}
    topics_by_id = {topic["id"]: topic for topic in topics}
    generated_at = parse_utc(dataset["generatedAt"])
    assert generated_at is not None
    ids: set[str] = set()
    for article in articles:
        validate_article(article, sources_by_id, topics_by_id, topics, generated_at)
        if article["id"] in ids:
            raise DatasetValidationError("duplicate Article ID")
        ids.add(article["id"])
    if articles != sorted(articles, key=article_order_key):
        raise DatasetValidationError("invalid final Article ordering")


def validate_catastrophic_gates(dataset: dict[str, Any]) -> None:
    pipeline = dataset["pipeline"]
    if pipeline["articleCount"] < 20:
        raise DatasetValidationError("catastrophic article-count gate failed")
    minimum_sources = math.ceil(pipeline["enabledSourceCount"] * 0.50)
    if pipeline["successfulSourceCount"] < minimum_sources:
        raise DatasetValidationError("catastrophic source-success gate failed")
