"""Atomic, human-readable UTF-8 ArticleDataset output."""

from __future__ import annotations

import json
import os
from pathlib import Path
import tempfile
from typing import Any, Callable


def write_dataset(
    dataset: dict[str, Any],
    destination: Path,
    *,
    replace: Callable[[str, str], None] = os.replace,
) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary_path: str | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            newline="\n",
            dir=destination.parent,
            prefix=f".{destination.name}.",
            suffix=".tmp",
            delete=False,
        ) as temporary:
            temporary_path = temporary.name
            json.dump(dataset, temporary, ensure_ascii=False, indent=2)
            temporary.write("\n")
            temporary.flush()
            os.fsync(temporary.fileno())
        replace(temporary_path, str(destination))
        temporary_path = None
    finally:
        if temporary_path is not None:
            try:
                os.unlink(temporary_path)
            except FileNotFoundError:
                pass
