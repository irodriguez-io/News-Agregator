from __future__ import annotations

from copy import deepcopy
import json

import pytest

from pipeline.configuration import (
    ConfigurationError,
    load_configuration,
    validate_sources,
    validate_topics,
)
from pipeline.constants import APPROVED_SOURCE_IDS, FILTERED_SOURCE_IDS, HTML_SOURCE_IDS


DEFERRED_SOURCE_IDS = {"openai_release_notes", "okta_workflows"}


def test_frozen_configuration_loads_with_exact_counts(configuration):
    sources, topics = configuration
    source_ids = {source["id"] for source in sources}
    assert len(sources) == 20
    assert len(topics) == 72
    assert all(source["enabled"] for source in sources)
    assert source_ids == APPROVED_SOURCE_IDS
    assert source_ids.isdisjoint(DEFERRED_SOURCE_IDS)
    assert {source["id"] for source in sources if source["adapter"] == "html_listing"} == {
        "anthropic_engineering", "barbell_medicine"
    }


def test_frozen_filtered_and_forced_sources_are_exact(configuration):
    sources, _ = configuration
    assert {source["id"] for source in sources if source["minTopicMatches"]} == {
        "barbell_medicine", "entra_releases"
    }
    assert {source["id"]: source["forcedTags"] for source in sources if source["forcedTags"]} == {
        "ietf_oauth": ["oauth"],
        "w3c_webauthn": ["passkeys_webauthn"],
        "ietf_scim": ["scim"],
    }


def test_amendment_5_deferred_sources_are_absent_from_pipeline_boundaries():
    from pipeline.adapters.html_listing import APPROVED_HTML_SOURCES

    assert APPROVED_SOURCE_IDS.isdisjoint(DEFERRED_SOURCE_IDS)
    assert HTML_SOURCE_IDS == {"anthropic_engineering", "barbell_medicine"}
    assert FILTERED_SOURCE_IDS == {"barbell_medicine", "entra_releases"}
    assert APPROVED_HTML_SOURCES == HTML_SOURCE_IDS


def test_default_validation_rejects_catalog_drift(tmp_path, configuration):
    sources, topics = deepcopy(configuration)
    sources[0]["name"] = "Changed"
    source_path = tmp_path / "sources.json"
    topic_path = tmp_path / "topics.json"
    source_path.write_text(json.dumps({"sources": sources}), encoding="utf-8")
    topic_path.write_text(json.dumps({"topics": topics}), encoding="utf-8")
    with pytest.raises(ConfigurationError, match="frozen V1 catalog"):
        load_configuration(source_path, topic_path)


def test_default_validation_rejects_taxonomy_drift(tmp_path, configuration):
    sources, topics = deepcopy(configuration)
    topics[0]["aliases"][0] = "changed alias"
    source_path = tmp_path / "sources.json"
    topic_path = tmp_path / "topics.json"
    source_path.write_text(json.dumps({"sources": sources}), encoding="utf-8")
    topic_path.write_text(json.dumps({"topics": topics}), encoding="utf-8")
    with pytest.raises(ConfigurationError, match="frozen V1 taxonomy"):
        load_configuration(source_path, topic_path)


def test_malformed_json_is_rejected_before_generation(tmp_path):
    source_path = tmp_path / "sources.json"
    topic_path = tmp_path / "topics.json"
    source_path.write_text("{", encoding="utf-8")
    topic_path.write_text('{"topics": []}', encoding="utf-8")
    with pytest.raises(ConfigurationError, match="cannot load"):
        load_configuration(source_path, topic_path)


@pytest.mark.parametrize(
    ("mutation", "message"),
    [
        (lambda topics: topics.append(deepcopy(topics[0])), "duplicate topic ID"),
        (lambda topics: topics[0].update(categories=["invalid"]), "invalid categories"),
        (lambda topics: topics[0].update(aliases=["  "]), "empty normalized alias"),
        (lambda topics: topics[0].update(aliases=["Physics", "physics"]), "duplicate normalized alias"),
    ],
)
def test_invalid_topics_are_rejected(configuration, mutation, message):
    topics = deepcopy(configuration[1])
    mutation(topics)
    with pytest.raises(ConfigurationError, match=message):
        validate_topics(topics)


def test_ambiguous_alias_in_same_category_is_rejected(configuration):
    topics = deepcopy(configuration[1])
    topics[1]["aliases"].append(topics[0]["aliases"][0])
    with pytest.raises(ConfigurationError, match="ambiguous alias"):
        validate_topics(topics)


@pytest.mark.parametrize(
    ("field", "value", "message"),
    [
        ("category", "invalid", "invalid category"),
        ("adapter", "crawler", "invalid adapter"),
        ("quality", 51, "quality outside"),
        ("url", "file:///tmp/feed", "invalid HTTP/HTTPS URL"),
        ("minTopicMatches", -1, "invalid minTopicMatches"),
    ],
)
def test_invalid_source_fields_are_rejected(configuration, field, value, message):
    sources, topics = deepcopy(configuration)
    sources[0][field] = value
    with pytest.raises(ConfigurationError, match=message):
        validate_sources(sources, validate_topics(topics), require_approved_catalog=False)


def test_invalid_content_type_score_is_rejected(configuration):
    sources, topics = deepcopy(configuration)
    sources[0]["contentType"]["score"] = 17
    with pytest.raises(ConfigurationError, match="content type"):
        validate_sources(sources, validate_topics(topics), require_approved_catalog=False)


@pytest.mark.parametrize("field", ["forcedTags", "admissionTopicIds"])
def test_unknown_source_topic_is_rejected(configuration, field):
    sources, topics = deepcopy(configuration)
    sources[0][field] = ["unknown"]
    if field == "admissionTopicIds":
        sources[0]["minTopicMatches"] = 1
    with pytest.raises(ConfigurationError, match="unknown topic"):
        validate_sources(sources, validate_topics(topics), require_approved_catalog=False)


def test_forced_topic_outside_source_category_is_rejected(configuration):
    sources, topics = deepcopy(configuration)
    sources[0]["forcedTags"] = ["oauth"]
    with pytest.raises(ConfigurationError, match="outside source category"):
        validate_sources(sources, validate_topics(topics), require_approved_catalog=False)


def test_impossible_admission_minimum_is_rejected(configuration):
    sources, topics = deepcopy(configuration)
    sources[0]["minTopicMatches"] = 2
    sources[0]["admissionTopicIds"] = ["physics_quantum"]
    with pytest.raises(ConfigurationError, match="contradictory admission"):
        validate_sources(sources, validate_topics(topics), require_approved_catalog=False)


def test_duplicate_source_id_is_rejected(configuration):
    sources, topics = deepcopy(configuration)
    sources.append(deepcopy(sources[0]))
    with pytest.raises(ConfigurationError, match="duplicate source ID"):
        validate_sources(sources, validate_topics(topics), require_approved_catalog=False)
