"""Category-scoped whole-token taxonomy, admission, and forced tags."""

from __future__ import annotations

import unicodedata
from typing import Any


def matching_text(value: str) -> str:
    normalized = unicodedata.normalize("NFKC", value).casefold()
    characters = [char if char.isalnum() else " " for char in normalized]
    return " ".join("".join(characters).split())


def _contains_phrase(text_tokens: tuple[str, ...], alias_tokens: tuple[str, ...]) -> bool:
    width = len(alias_tokens)
    if not width or width > len(text_tokens):
        return False
    return any(text_tokens[index : index + width] == alias_tokens for index in range(len(text_tokens) - width + 1))


def match_topics(article: dict[str, Any], topics: list[dict[str, Any]]) -> dict[str, int]:
    title_tokens = tuple(matching_text(article["title"]).split())
    excerpt_tokens = tuple(matching_text(article["excerpt"]).split())
    matches: list[tuple[dict[str, Any], int, bool]] = []
    all_evidence: dict[str, int] = {}
    for topic in topics:
        if article["category"] not in topic["categories"]:
            continue
        aliases = [tuple(matching_text(alias).split()) for alias in topic["aliases"]]
        title_match = any(_contains_phrase(title_tokens, alias) for alias in aliases)
        excerpt_match = any(_contains_phrase(excerpt_tokens, alias) for alias in aliases)
        evidence = (3 if title_match else 0) + (1 if excerpt_match else 0)
        if evidence:
            all_evidence[topic["id"]] = evidence
            matches.append((topic, evidence, title_match))
    matches.sort(key=lambda item: (-item[1], not item[2], item[0]["label"].casefold(), item[0]["id"]))
    article["tags"] = [
        {"id": topic["id"], "label": topic["label"]} for topic, _, _ in matches[:5]
    ]
    article["_organicTopicIds"] = [topic["id"] for topic, _, _ in matches]
    article["_organicEvidence"] = all_evidence
    article["_topicSignal"] = min(10, sum(all_evidence.values()))
    return all_evidence


def passes_admission(article: dict[str, Any], source: dict[str, Any]) -> bool:
    minimum = source["minTopicMatches"]
    if minimum == 0:
        return True
    organic = set(article["_organicTopicIds"])
    allowed = set(source["admissionTopicIds"])
    if allowed:
        organic &= allowed
    return len(organic) >= minimum


def apply_forced_tags(
    article: dict[str, Any],
    source: dict[str, Any],
    topics_by_id: dict[str, dict[str, Any]],
) -> None:
    present = {tag["id"] for tag in article["tags"]}
    for topic_id in source["forcedTags"]:
        if topic_id in present:
            continue
        topic = topics_by_id[topic_id]
        article["tags"].append({"id": topic_id, "label": topic["label"]})
        present.add(topic_id)
