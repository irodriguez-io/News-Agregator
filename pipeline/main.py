"""Intentional Reading V1 content-pipeline command-line orchestrator."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
import sys
from typing import Any, Callable

from .adapters import AdapterError, fetch_entries
from .configuration import ConfigurationError, load_configuration
from .deduplicate import deduplicate
from .fetch import FetchError, HttpClient
from .normalize import iso_utc, normalize_entry
from .output import write_dataset
from .retention import retain_articles
from .scoring import compute_metadata, public_article, score_article
from .taxonomy import apply_forced_tags, match_topics, passes_admission
from .validation import DatasetValidationError, validate_catastrophic_gates, validate_dataset


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCES = PROJECT_ROOT / "config" / "sources.json"
DEFAULT_TOPICS = PROJECT_ROOT / "config" / "topics.json"
DEFAULT_OUTPUT = PROJECT_ROOT / "data" / "articles.json"


@dataclass
class SourceRecord:
    source_id: str
    status: str = "failed"
    raw: int = 0
    normalized: int = 0
    accepted: int = 0
    rejected: int = 0
    error: str | None = None
    warning_count: int = 0


def _bounded_error(exc: Exception) -> str:
    code = getattr(exc, "code", str(exc))
    detail = getattr(exc, "detail", "")
    if detail and str(detail).isdigit():
        return f"{code}_{detail}"
    return str(code)


def capture_generated_at() -> datetime:
    return datetime.now(timezone.utc).replace(microsecond=0)


def process_source(
    source: dict[str, Any],
    topics: list[dict[str, Any]],
    topics_by_id: dict[str, dict[str, Any]],
    generated_at: datetime,
    client: HttpClient,
) -> tuple[SourceRecord, list[dict[str, Any]]]:
    record = SourceRecord(source_id=source["id"])
    try:
        raw_entries = fetch_entries(source, client)
    except (FetchError, AdapterError) as exc:
        record.error = _bounded_error(exc)
        return record, []
    record.status = "successful"
    record.raw = len(raw_entries)
    accepted: list[dict[str, Any]] = []

    def warning(_: str) -> None:
        record.warning_count += 1

    for raw in raw_entries:
        article = normalize_entry(raw, source, generated_at, warning=warning)
        if article is None:
            record.rejected += 1
            continue
        record.normalized += 1
        match_topics(article, topics)
        if not passes_admission(article, source):
            record.rejected += 1
            continue
        apply_forced_tags(article, source, topics_by_id)
        compute_metadata(article)
        accepted.append(article)
        record.accepted += 1
    return record, accepted


def _print_record(record: SourceRecord, *, validation_only: bool = False) -> None:
    if validation_only:
        if record.status == "successful":
            print(f"PASS {record.source_id} raw={record.raw}")
        else:
            print(f"FAIL {record.source_id} reason={record.error}")
        return
    if record.status == "failed":
        print(f"[{record.source_id}] FAILED reason={record.error}")
        return
    warning = f" warnings={record.warning_count}" if record.warning_count else ""
    print(
        f"[{record.source_id}] raw={record.raw} normalized={record.normalized} "
        f"accepted={record.accepted} rejected={record.rejected}{warning}"
    )


def validate_live_sources(sources: list[dict[str, Any]], client: HttpClient) -> int:
    failures = 0
    for source in sources:
        if not source["enabled"]:
            continue
        record = SourceRecord(source["id"])
        try:
            entries = fetch_entries(source, client)
            record.status = "successful"
            record.raw = len(entries)
        except (FetchError, AdapterError) as exc:
            record.error = _bounded_error(exc)
            failures += 1
        _print_record(record, validation_only=True)
    enabled = sum(1 for source in sources if source["enabled"])
    print(f"Source validation: enabled={enabled} passed={enabled - failures} failed={failures}")
    return 0 if failures == 0 else 1


def generate_dataset(
    sources: list[dict[str, Any]],
    topics: list[dict[str, Any]],
    *,
    generated_at: datetime,
    client: HttpClient,
    destination: Path,
) -> dict[str, Any]:
    topics_by_id = {topic["id"]: topic for topic in topics}
    enabled = [source for source in sources if source["enabled"]]
    records: list[SourceRecord] = []
    candidates: list[dict[str, Any]] = []
    for source in enabled:
        record, source_candidates = process_source(
            source, topics, topics_by_id, generated_at, client
        )
        records.append(record)
        candidates.extend(source_candidates)
        _print_record(record)

    unique, duplicate_count = deduplicate(candidates)
    scored = [score_article(article, generated_at) for article in unique]
    retained, expired, source_capped, globally_capped = retain_articles(scored, generated_at)
    articles = [public_article(article) for article in retained]
    successful = sum(record.status == "successful" for record in records)
    dataset = {
        "schemaVersion": 1,
        "generatedAt": iso_utc(generated_at),
        "pipeline": {
            "enabledSourceCount": len(enabled),
            "successfulSourceCount": successful,
            "failedSourceCount": len(enabled) - successful,
            "articleCount": len(articles),
        },
        "articles": articles,
    }
    validate_dataset(dataset, sources, topics)
    validate_catastrophic_gates(dataset)
    write_dataset(dataset, destination)
    print(
        "Pipeline summary: "
        f"enabled={len(enabled)} successful={successful} failed={len(enabled) - successful} "
        f"raw={sum(record.raw for record in records)} "
        f"accepted={sum(record.accepted for record in records)} duplicates={duplicate_count} "
        f"expired={expired} source_capped={source_capped} global_capped={globally_capped} "
        f"retained={len(articles)}"
    )
    return dataset


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Generate the Intentional Reading V1 dataset")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--validate-config", action="store_true")
    mode.add_argument("--validate-sources", action="store_true")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    return parser


def main(
    argv: list[str] | None = None,
    *,
    now: Callable[[], datetime] = capture_generated_at,
    client_factory: Callable[[], HttpClient] = HttpClient,
) -> int:
    args = build_parser().parse_args(argv)
    try:
        sources, topics = load_configuration(DEFAULT_SOURCES, DEFAULT_TOPICS)
        if args.validate_config:
            print(f"Configuration valid: sources={len(sources)} topics={len(topics)}")
            return 0
        client = client_factory()
        if args.validate_sources:
            return validate_live_sources(sources, client)
        generated_at = now()
        if generated_at.tzinfo is None or generated_at.utcoffset() is None:
            raise ValueError("generatedAt clock must be timezone-aware")
        generated_at = generated_at.astimezone(timezone.utc).replace(microsecond=0)
        generate_dataset(
            sources,
            topics,
            generated_at=generated_at,
            client=client,
            destination=args.output,
        )
        return 0
    except (ConfigurationError, DatasetValidationError, OSError, ValueError) as exc:
        print(f"Pipeline failed: {exc}", file=sys.stderr)
        return 1
    except Exception:
        print("Pipeline failed: unexpected_error", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
