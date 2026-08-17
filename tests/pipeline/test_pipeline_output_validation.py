from __future__ import annotations

from copy import deepcopy
import json
from urllib.parse import urlsplit

import pytest

from pipeline.adapters import AdapterError
from pipeline.fetch import FetchError
from pipeline.main import (
    SourceRecord,
    generate_dataset,
    main,
    process_source,
    validate_live_sources,
)
from pipeline.output import write_dataset
from pipeline.retention import article_order_key
from pipeline.scoring import public_article
from pipeline.taxonomy import match_topics
from pipeline.validation import (
    DatasetValidationError,
    validate_catastrophic_gates,
    validate_dataset,
)

from conftest import NOW, make_article


class UnusedClient:
    pass


def _public_valid_article(source, topics, index=0):
    article = make_article(
        source,
        title=f"Useful report number {index}",
        url=f"https://www.quantamagazine.org/fixture/article-{index}",
    )
    match_topics(article, topics)
    from pipeline.scoring import compute_metadata, score_article

    compute_metadata(article)
    score_article(article, NOW)
    return public_article(article)


def _dataset(sources, topics, article_count=20, successful=11):
    articles = sorted(
        [_public_valid_article(sources[0], topics, index) for index in range(article_count)],
        key=article_order_key,
    )
    return {
        "schemaVersion": 1,
        "generatedAt": "2026-08-16T23:00:00Z",
        "pipeline": {
            "enabledSourceCount": 22,
            "successfulSourceCount": successful,
            "failedSourceCount": 22 - successful,
            "articleCount": article_count,
        },
        "articles": articles,
    }


def test_dataset_contract_accepts_valid_structure(configuration):
    sources, topics = configuration
    dataset = _dataset(sources, topics)
    validate_dataset(dataset, sources, topics)
    validate_catastrophic_gates(dataset)


def test_dataset_rejects_score_sum_mismatch(configuration):
    sources, topics = configuration
    dataset = _dataset(sources, topics)
    dataset["articles"][0]["score"]["base"] -= 1
    with pytest.raises(DatasetValidationError, match="base score invariant"):
        validate_dataset(dataset, sources, topics)


@pytest.mark.parametrize(
    ("field", "delta", "message"),
    [
        ("freshness", -1, "freshness score mismatch"),
        ("metadata", -1, "metadata score mismatch"),
        ("topicSignal", 1, "taxonomy or topic-signal mismatch"),
    ],
)
def test_dataset_recomputes_deterministic_score_components(
    configuration, field, delta, message
):
    sources, topics = configuration
    dataset = _dataset(sources, topics)
    dataset["articles"][0]["score"][field] += delta
    dataset["articles"][0]["score"]["base"] += delta
    with pytest.raises(DatasetValidationError, match=message):
        validate_dataset(dataset, sources, topics)


def test_dataset_rejects_duplicate_ids_and_noncanonical_url(configuration):
    sources, topics = configuration
    dataset = _dataset(sources, topics)
    dataset["articles"][1] = deepcopy(dataset["articles"][0])
    with pytest.raises(DatasetValidationError, match="duplicate Article ID"):
        validate_dataset(dataset, sources, topics)
    dataset = _dataset(sources, topics)
    dataset["articles"][0]["url"] += "?utm_source=test"
    with pytest.raises(DatasetValidationError, match="canonical Article URL"):
        validate_dataset(dataset, sources, topics)


@pytest.mark.parametrize(
    ("articles", "successful", "passes"),
    [(19, 11, False), (20, 11, True), (20, 10, False)],
)
def test_catastrophic_exact_boundaries(configuration, articles, successful, passes):
    sources, topics = configuration
    dataset = _dataset(sources, topics, articles, successful)
    if passes:
        validate_catastrophic_gates(dataset)
    else:
        with pytest.raises(DatasetValidationError, match="catastrophic"):
            validate_catastrophic_gates(dataset)


def test_atomic_output_writes_utf8_human_readable_json(tmp_path, configuration):
    sources, topics = configuration
    dataset = _dataset(sources, topics)
    destination = tmp_path / "articles.json"
    write_dataset(dataset, destination)
    raw = destination.read_bytes()
    assert not raw.startswith(b"\xef\xbb\xbf")
    assert b'\n  "schemaVersion"' in raw
    assert json.loads(raw) == dataset


def test_atomic_output_failure_preserves_previous_file_and_removes_temp(tmp_path):
    destination = tmp_path / "articles.json"
    destination.write_text('{"healthy": true}\n', encoding="utf-8")

    def fail_replace(source, target):
        raise OSError("simulated replace failure")

    with pytest.raises(OSError, match="simulated"):
        write_dataset({"new": True}, destination, replace=fail_replace)
    assert destination.read_text(encoding="utf-8") == '{"healthy": true}\n'
    assert list(tmp_path.glob(".articles.json.*.tmp")) == []


@pytest.mark.parametrize(
    "failure",
    [FetchError("timeout"), FetchError("http_error", "503"), AdapterError("parser_exception")],
)
def test_source_failures_are_isolated(monkeypatch, sources, topics, failure):
    def fail(*args, **kwargs):
        raise failure

    monkeypatch.setattr("pipeline.main.fetch_entries", fail)
    source = sources["quanta"]
    record, articles = process_source(
        source, topics, {topic["id"]: topic for topic in topics}, NOW, UnusedClient()
    )
    assert record.status == "failed"
    assert record.error
    assert articles == []


def test_parsed_but_filtered_to_zero_source_is_successful(monkeypatch, sources, topics):
    monkeypatch.setattr(
        "pipeline.main.fetch_entries",
        lambda source, client: [
            {
                "title": "New sidebar colors",
                "url": "https://openai.com/sidebar",
                "summary": "A generic product interface announcement.",
                "content": [],
            }
        ],
    )
    source = sources["openai_release_notes"]
    record, articles = process_source(
        source, topics, {topic["id"]: topic for topic in topics}, NOW, UnusedClient()
    )
    assert record.status == "successful"
    assert record.raw == 1
    assert record.normalized == 1
    assert record.accepted == 0
    assert record.rejected == 1
    assert articles == []


def test_normal_generation_uses_all_stages_and_output_override(monkeypatch, tmp_path, configuration, capsys):
    sources, topics = configuration

    def fixture_entry(source, client):
        title = {
            "openai_release_notes": "AI software architecture release",
            "barbell_medicine": "Strength training volume",
            "entra_releases": "SCIM provisioning connector release",
        }.get(source["id"], f"Useful release {source['id']}")
        return [
            {
                "title": title,
                "url": f"{urlsplit(source['url']).scheme}://{urlsplit(source['url']).netloc}/fixture/{source['id']}",
                "published": "2026-08-15T12:00:00Z",
                "author": "Fixture Author",
                "summary": "A bounded fixture summary for deterministic generation.",
                "content": [],
            }
        ]

    monkeypatch.setattr("pipeline.main.fetch_entries", fixture_entry)
    destination = tmp_path / "custom.json"
    dataset = generate_dataset(
        sources,
        topics,
        generated_at=NOW,
        client=UnusedClient(),
        destination=destination,
    )
    assert destination.exists()
    assert dataset["pipeline"] == {
        "enabledSourceCount": 22,
        "successfulSourceCount": 22,
        "failedSourceCount": 0,
        "articleCount": 22,
    }
    output = capsys.readouterr().out
    assert "Pipeline summary" in output
    assert "Fixture Author" not in output


def test_validate_sources_exit_code_reports_individual_failure(monkeypatch, configuration, capsys):
    sources, _ = configuration

    def mixed(source, client):
        if source["id"] == "quanta":
            raise FetchError("timeout")
        return [{"title": "entry"}]

    monkeypatch.setattr("pipeline.main.fetch_entries", mixed)
    assert validate_live_sources(sources, UnusedClient()) == 1
    output = capsys.readouterr().out
    assert "FAIL quanta reason=timeout" in output
    assert "passed=21 failed=1" in output


def test_validate_config_mode_does_not_construct_network_client():
    def forbidden():
        raise AssertionError("network client constructed")

    assert main(["--validate-config"], client_factory=forbidden) == 0


def test_output_cli_option_is_forwarded_to_normal_generation(monkeypatch, tmp_path):
    destination = tmp_path / "override.json"
    captured = {}

    def fake_generate(sources, topics, *, generated_at, client, destination):
        captured["destination"] = destination
        return {}

    monkeypatch.setattr("pipeline.main.generate_dataset", fake_generate)
    assert main(
        ["--output", str(destination)],
        now=lambda: NOW,
        client_factory=UnusedClient,
    ) == 0
    assert captured["destination"] == destination
