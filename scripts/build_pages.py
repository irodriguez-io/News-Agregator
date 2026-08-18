"""Assemble the deterministic, allowlist-only GitHub Pages artifact."""

from __future__ import annotations

import json
from pathlib import Path
import shutil


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DESTINATION = PROJECT_ROOT / ".build" / "pages"
ALLOWED_PATHS = (
    Path("index.html"),
    Path("css"),
    Path("js"),
    Path("data/articles.json"),
)


def _copy_allowlisted(source: Path, destination: Path) -> None:
    if source.is_symlink():
        raise RuntimeError(f"Refusing symlink in Pages input: {source.relative_to(PROJECT_ROOT)}")
    if source.is_file():
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        return
    if not source.is_dir():
        raise FileNotFoundError(f"Required Pages input is missing: {source.relative_to(PROJECT_ROOT)}")
    destination.mkdir(parents=True, exist_ok=True)
    for child in sorted(source.iterdir(), key=lambda path: path.name):
        _copy_allowlisted(child, destination / child.name)


def build_pages() -> list[str]:
    dataset_path = PROJECT_ROOT / "data" / "articles.json"
    with dataset_path.open(encoding="utf-8") as handle:
        dataset = json.load(handle)
    if dataset.get("schemaVersion") != 1 or not isinstance(dataset.get("articles"), list):
        raise RuntimeError("data/articles.json is not an ArticleDataset v1 artifact")

    if DESTINATION.exists():
        shutil.rmtree(DESTINATION)
    DESTINATION.mkdir(parents=True)
    for relative_path in ALLOWED_PATHS:
        _copy_allowlisted(PROJECT_ROOT / relative_path, DESTINATION / relative_path)

    manifest = [
        path.relative_to(DESTINATION).as_posix()
        for path in sorted(DESTINATION.rglob("*"))
        if path.is_file()
    ]
    return manifest


def main() -> int:
    manifest = build_pages()
    print(f"Pages artifact assembled: files={len(manifest)} destination={DESTINATION}")
    for path in manifest:
        print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
