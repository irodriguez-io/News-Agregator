"""Load and validate the frozen source and taxonomy configuration."""

from __future__ import annotations

import json
import hashlib
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

from .constants import (
    ADAPTERS,
    APPROVED_SOURCE_IDS,
    CATEGORIES,
    CONTENT_TYPES,
    FILTERED_SOURCE_IDS,
    FORCED_TAGS,
    HTML_SOURCE_IDS,
    SOURCE_CATALOG_SHA256,
    TOPIC_TAXONOMY_SHA256,
)


class ConfigurationError(ValueError):
    """Raised when repository-controlled configuration violates V1."""


SOURCE_FIELDS = {
    "id", "name", "category", "adapter", "url", "quality", "contentType",
    "enabled", "minTopicMatches", "admissionTopicIds", "forcedTags",
}
TOPIC_FIELDS = {"id", "label", "categories", "aliases"}


def _read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ConfigurationError(f"cannot load {path}: {type(exc).__name__}") from exc


def _configuration_digest(items: list[dict[str, Any]]) -> str:
    serialized = json.dumps(
        items, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(serialized).hexdigest()


def _identifier(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value or len(value) > 100:
        raise ConfigurationError(f"{field} must be a bounded non-empty string")
    if not all(char.islower() or char.isdigit() or char == "_" for char in value):
        raise ConfigurationError(f"{field} is not a canonical identifier")
    return value


def _configured_url(value: Any, source_id: str) -> None:
    if not isinstance(value, str):
        raise ConfigurationError(f"{source_id}: url must be a string")
    try:
        parsed = urlsplit(value)
        _ = parsed.port
    except ValueError as exc:
        raise ConfigurationError(f"{source_id}: invalid URL") from exc
    if (
        parsed.scheme.lower() not in {"http", "https"}
        or not parsed.hostname
        or parsed.username
        or parsed.password
        or any(character.isspace() for character in parsed.hostname)
    ):
        raise ConfigurationError(f"{source_id}: invalid HTTP/HTTPS URL")


def validate_topics(topics: Any) -> dict[str, dict[str, Any]]:
    if not isinstance(topics, list) or not topics:
        raise ConfigurationError("topics must be a non-empty array")
    by_id: dict[str, dict[str, Any]] = {}
    aliases_by_category: dict[str, dict[str, str]] = {category: {} for category in CATEGORIES}
    from .taxonomy import matching_text

    for topic in topics:
        if not isinstance(topic, dict) or set(topic) != TOPIC_FIELDS:
            raise ConfigurationError("topic fields do not match the V1 contract")
        topic_id = _identifier(topic["id"], "topic.id")
        if topic_id in by_id:
            raise ConfigurationError(f"duplicate topic ID: {topic_id}")
        label = topic["label"]
        if not isinstance(label, str) or not label.strip() or len(label) > 100:
            raise ConfigurationError(f"{topic_id}: invalid label")
        categories = topic["categories"]
        if (not isinstance(categories, list) or not categories
                or any(category not in CATEGORIES for category in categories)
                or len(set(categories)) != len(categories)):
            raise ConfigurationError(f"{topic_id}: invalid categories")
        aliases = topic["aliases"]
        if not isinstance(aliases, list) or not aliases:
            raise ConfigurationError(f"{topic_id}: aliases must be non-empty")
        normalized: list[str] = []
        for alias in aliases:
            if not isinstance(alias, str) or not (value := matching_text(alias)):
                raise ConfigurationError(f"{topic_id}: empty normalized alias")
            if value in normalized:
                raise ConfigurationError(f"{topic_id}: duplicate normalized alias {value!r}")
            normalized.append(value)
            for category in categories:
                owner = aliases_by_category[category].get(value)
                if owner and owner != topic_id:
                    raise ConfigurationError(
                        f"ambiguous alias {value!r} in {category}: {owner}, {topic_id}"
                    )
                aliases_by_category[category][value] = topic_id
        by_id[topic_id] = topic
    return by_id


def validate_sources(
    sources: Any,
    topics_by_id: dict[str, dict[str, Any]],
    *,
    require_approved_catalog: bool = True,
) -> dict[str, dict[str, Any]]:
    if not isinstance(sources, list) or not sources:
        raise ConfigurationError("sources must be a non-empty array")
    by_id: dict[str, dict[str, Any]] = {}
    for source in sources:
        if not isinstance(source, dict) or set(source) != SOURCE_FIELDS:
            raise ConfigurationError("source fields do not match the V1 contract")
        source_id = _identifier(source["id"], "source.id")
        if source_id in by_id:
            raise ConfigurationError(f"duplicate source ID: {source_id}")
        if require_approved_catalog and source_id not in APPROVED_SOURCE_IDS:
            raise ConfigurationError(f"unapproved source ID: {source_id}")
        if not isinstance(source["name"], str) or not source["name"].strip() or len(source["name"]) > 200:
            raise ConfigurationError(f"{source_id}: invalid name")
        if source["category"] not in CATEGORIES:
            raise ConfigurationError(f"{source_id}: invalid category")
        if source["adapter"] not in ADAPTERS:
            raise ConfigurationError(f"{source_id}: invalid adapter")
        _configured_url(source["url"], source_id)
        if type(source["quality"]) is not int or not 0 <= source["quality"] <= 50:
            raise ConfigurationError(f"{source_id}: quality outside 0..50")
        content_type = source["contentType"]
        if not isinstance(content_type, dict) or set(content_type) != {"id", "label", "score"}:
            raise ConfigurationError(f"{source_id}: invalid content type")
        expected = CONTENT_TYPES.get(content_type["id"])
        if expected is None or (content_type["label"], content_type["score"]) != expected:
            raise ConfigurationError(f"{source_id}: unknown or inconsistent content type")
        if type(source["enabled"]) is not bool:
            raise ConfigurationError(f"{source_id}: enabled must be boolean")
        if type(source["minTopicMatches"]) is not int or source["minTopicMatches"] < 0:
            raise ConfigurationError(f"{source_id}: invalid minTopicMatches")
        for field in ("admissionTopicIds", "forcedTags"):
            values = source[field]
            if not isinstance(values, list) or len(values) != len(set(values)):
                raise ConfigurationError(f"{source_id}: invalid {field}")
            for topic_id in values:
                if topic_id not in topics_by_id:
                    raise ConfigurationError(f"{source_id}: unknown topic {topic_id}")
                if source["category"] not in topics_by_id[topic_id]["categories"]:
                    raise ConfigurationError(f"{source_id}: topic {topic_id} outside source category")
        if source["minTopicMatches"] == 0 and source["admissionTopicIds"]:
            raise ConfigurationError(f"{source_id}: contradictory admission settings")
        eligible_count = sum(
            source["category"] in topic["categories"] for topic in topics_by_id.values()
        )
        admission_ceiling = len(source["admissionTopicIds"]) or eligible_count
        if source["minTopicMatches"] > admission_ceiling:
            raise ConfigurationError(f"{source_id}: contradictory admission settings")
        by_id[source_id] = source

    if require_approved_catalog:
        if set(by_id) != APPROVED_SOURCE_IDS or len(by_id) != 20:
            raise ConfigurationError("source catalog must contain exactly the 20 approved IDs")
        if any(not source["enabled"] for source in sources):
            raise ConfigurationError("all approved V1 sources must be enabled")
        html_ids = {source["id"] for source in sources if source["adapter"] == "html_listing"}
        if html_ids != HTML_SOURCE_IDS:
            raise ConfigurationError("HTML adapters are limited to the two approved sources")
        filtered = {source["id"] for source in sources if source["minTopicMatches"] > 0}
        if filtered != FILTERED_SOURCE_IDS:
            raise ConfigurationError("admission filtering must apply to exactly two sources")
        forced = {source["id"]: source["forcedTags"] for source in sources if source["forcedTags"]}
        if forced != FORCED_TAGS:
            raise ConfigurationError("forced tags do not match the frozen V1 catalog")
    return by_id


def load_configuration(
    sources_path: Path,
    topics_path: Path,
    *,
    require_approved_catalog: bool = True,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    source_root = _read_json(sources_path)
    topic_root = _read_json(topics_path)
    if not isinstance(source_root, dict) or set(source_root) != {"sources"}:
        raise ConfigurationError("sources.json must contain only a sources array")
    if not isinstance(topic_root, dict) or set(topic_root) != {"topics"}:
        raise ConfigurationError("topics.json must contain only a topics array")
    topics = topic_root["topics"]
    sources = source_root["sources"]
    topics_by_id = validate_topics(topics)
    validate_sources(sources, topics_by_id, require_approved_catalog=require_approved_catalog)
    if require_approved_catalog:
        if _configuration_digest(sources) != SOURCE_CATALOG_SHA256:
            raise ConfigurationError("source catalog differs from the frozen V1 catalog")
        if _configuration_digest(topics) != TOPIC_TAXONOMY_SHA256:
            raise ConfigurationError("topic taxonomy differs from the frozen V1 taxonomy")
    return sources, topics
