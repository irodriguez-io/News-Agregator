"""Approved source-adapter dispatch."""

from __future__ import annotations

from typing import Any

from ..fetch import FetchError, HttpClient
from .feed import parse_feed
from .html_listing import parse_html_listing
from .rss_autodiscovery import fetch_autodiscovered_feed


class AdapterError(RuntimeError):
    """A source response could not produce a valid raw-entry collection."""


def fetch_entries(source: dict[str, Any], client: HttpClient) -> list[dict[str, Any]]:
    adapter = source["adapter"]
    try:
        if adapter == "rss_autodiscovery":
            entries = fetch_autodiscovered_feed(source, client)
        else:
            response = client.get(source["url"])
            if adapter in {"rss", "atom"}:
                entries = parse_feed(response.body, response.url)
            elif adapter == "html_listing":
                entries = parse_html_listing(source["id"], response.body, response.url)
            else:  # Configuration validation normally makes this unreachable.
                raise AdapterError("unknown_adapter")
    except FetchError:
        raise
    except AdapterError:
        raise
    except Exception as exc:
        raise AdapterError("parser_exception") from exc
    if not entries:
        raise AdapterError("empty_parse")
    return entries


__all__ = ["AdapterError", "fetch_entries"]
