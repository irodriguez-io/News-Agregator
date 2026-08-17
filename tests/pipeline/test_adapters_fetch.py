from __future__ import annotations

from pathlib import Path

import pytest
import requests

from pipeline.adapters import AdapterError, fetch_entries
from pipeline.adapters.feed import parse_feed
from pipeline.adapters.html_listing import parse_html_listing
from pipeline.adapters.rss_autodiscovery import discover_feed_url
from pipeline.constants import MAX_RESPONSE_BYTES, USER_AGENT
from pipeline.fetch import FetchError, FetchResponse, HttpClient, validate_public_url

from conftest import FIXTURES


class FakeResponse:
    def __init__(self, status=200, body=b"ok", headers=None):
        self.status_code = status
        self._body = body
        self.headers = headers or {}
        self.is_redirect = status in {301, 302, 303, 307, 308}
        self.is_permanent_redirect = status in {301, 308}
        self.closed = False

    def iter_content(self, chunk_size):
        yield self._body

    def close(self):
        self.closed = True


class FakeSession:
    def __init__(self, outcomes):
        self.outcomes = list(outcomes)
        self.headers = {}
        self.calls = []

    def get(self, url, **kwargs):
        self.calls.append((url, kwargs))
        outcome = self.outcomes.pop(0)
        if isinstance(outcome, Exception):
            raise outcome
        return outcome


@pytest.mark.parametrize(
    "address",
    [
        "127.0.0.1",
        "10.0.0.1",
        "172.16.0.1",
        "192.168.1.1",
        "169.254.1.1",
        "0.0.0.0",
        "192.0.2.1",
        "::1",
        "fe80::1",
    ],
)
def test_ssrf_validation_rejects_non_public_resolutions(address):
    with pytest.raises(FetchError, match="non_public_target"):
        validate_public_url("https://publisher.example/feed", resolver=lambda _: [address])


def test_ssrf_validation_accepts_only_public_http_targets():
    assert validate_public_url(
        "https://publisher.example/feed", resolver=lambda _: ["93.184.216.34"]
    ) == "https://publisher.example/feed"
    with pytest.raises(FetchError, match="invalid_derived_url"):
        validate_public_url("file:///tmp/feed", resolver=lambda _: ["93.184.216.34"])
    with pytest.raises(FetchError, match="non_public_target"):
        validate_public_url("http://localhost/feed")


def test_http_client_uses_bounded_request_options_and_user_agent():
    session = FakeSession([FakeResponse(body=b"feed")])
    client = HttpClient(session=session)
    result = client.get("https://example.com/feed")
    assert result.body == b"feed"
    assert session.headers["User-Agent"] == USER_AGENT
    assert session.calls[0][1] == {
        "allow_redirects": False,
        "stream": True,
        "timeout": (10, 20),
    }


@pytest.mark.parametrize(
    "failure",
    [requests.Timeout(), requests.ConnectionError(), FakeResponse(status=503)],
)
def test_http_client_retries_transient_failure_exactly_once(failure):
    session = FakeSession([failure, FakeResponse(body=b"recovered")])
    sleeps = []
    client = HttpClient(session=session, sleeper=sleeps.append)
    assert client.get("https://example.com/feed").body == b"recovered"
    assert len(session.calls) == 2
    assert sleeps == [2]


def test_http_client_does_not_retry_permanent_http_error():
    session = FakeSession([FakeResponse(status=404)])
    client = HttpClient(session=session, sleeper=lambda _: None)
    with pytest.raises(FetchError, match="http_error: 404"):
        client.get("https://example.com/feed")
    assert len(session.calls) == 1


def test_http_client_rejects_oversized_content_length_and_stream():
    length_response = FakeResponse(headers={"Content-Length": str(MAX_RESPONSE_BYTES + 1)})
    with pytest.raises(FetchError, match="response_too_large"):
        HttpClient(FakeSession([length_response])).get("https://example.com/feed")
    body_response = FakeResponse(body=b"x" * (MAX_RESPONSE_BYTES + 1))
    with pytest.raises(FetchError, match="response_too_large"):
        HttpClient(FakeSession([body_response])).get("https://example.com/feed")


def test_cross_host_redirect_is_validated_before_request():
    session = FakeSession(
        [FakeResponse(status=302, headers={"Location": "https://other.example/feed"}), FakeResponse()]
    )
    validated = []
    client = HttpClient(session=session, target_validator=lambda url: validated.append(url) or url)
    client.get("https://example.com/feed")
    assert validated == ["https://other.example/feed"]
    assert [call[0] for call in session.calls] == ["https://example.com/feed", "https://other.example/feed"]


def test_redirect_limit_is_five():
    redirects = [FakeResponse(status=302, headers={"Location": f"/r{index}"}) for index in range(6)]
    client = HttpClient(FakeSession(redirects), target_validator=lambda url: url)
    with pytest.raises(FetchError, match="redirect_limit"):
        client.get("https://example.com/start")


def test_redirect_rejects_credentials_even_on_same_host():
    response = FakeResponse(status=302, headers={"Location": "https://user:pass@example.com/feed"})
    with pytest.raises(FetchError, match="invalid_redirect"):
        HttpClient(FakeSession([response])).get("https://example.com/start")


def test_feed_parser_handles_rss_and_atom_fixtures():
    rss = parse_feed((FIXTURES / "quanta.xml").read_bytes())
    atom = parse_feed((FIXTURES / "ietf_oauth.atom").read_bytes())
    assert rss[0]["url"].startswith("https://example.com/quantum-geometry")
    assert atom[0]["title"] == "Document update without an organic OAuth alias"
    assert parse_feed(b"<not-a-feed>") == []


def test_feed_parser_resolves_relative_article_links_against_feed_url():
    payload = b"""<feed xmlns='http://www.w3.org/2005/Atom'><entry>
    <title>Relative</title><link href='/doc/item'/><updated>2026-08-15T00:00:00Z</updated>
    </entry></feed>"""
    entry = parse_feed(payload, "https://publisher.example/group/feed")[0]
    assert entry["url"] == "https://publisher.example/doc/item"


def test_empty_or_malformed_feed_is_a_source_failure(sources):
    class Client:
        def get(self, url, **kwargs):
            return FetchResponse(b"<not-a-feed>", url, "application/rss+xml")

    with pytest.raises(AdapterError, match="empty_parse"):
        fetch_entries(sources["quanta"], Client())


def test_autodiscovery_prefers_primary_and_rejects_comment_and_podcast(monkeypatch):
    monkeypatch.setattr(
        "pipeline.adapters.rss_autodiscovery.validate_public_url", lambda url: url
    )
    selected = discover_feed_url(
        (FIXTURES / "autodiscovery.html").read_bytes(), "https://example.com/articles/"
    )
    assert selected == "https://example.com/primary.xml"


@pytest.mark.parametrize(
    ("source_id", "fixture", "page", "expected_title"),
    [
        (
            "anthropic_engineering",
            "anthropic_engineering.html",
            "https://www.anthropic.com/engineering",
            "Building Reliable AI Agents",
        ),
        (
            "barbell_medicine",
            "barbell_medicine.html",
            "https://www.barbellmedicine.com/articles/articles-training/",
            "Training Volume for Strength",
        ),
        (
            "okta_workflows",
            "okta_workflows.html",
            "https://help.okta.com/wf/en-us/Content/Topics/ReleaseNotes/Workflows/production.htm",
            "Okta Workflows 2026.08.1 production release",
        ),
    ],
)
def test_source_specific_html_fixtures(source_id, fixture, page, expected_title):
    entries = parse_html_listing(source_id, (FIXTURES / fixture).read_bytes(), page)
    assert len(entries) == (2 if source_id == "okta_workflows" else 1)
    assert entries[0]["title"] == expected_title
    assert entries[0]["url"].startswith("http")


def test_html_adapter_rejects_unapproved_source():
    with pytest.raises(ValueError, match="unapproved_html_source"):
        parse_html_listing("arbitrary", b"<html></html>", "https://example.com/")


def test_every_enabled_source_has_deterministic_adapter_fixture_coverage(configuration):
    sources, _ = configuration
    direct_feed_ids = {source["id"] for source in sources if source["adapter"] in {"rss", "atom"}}
    autodiscovery_ids = {source["id"] for source in sources if source["adapter"] == "rss_autodiscovery"}
    html_ids = {source["id"] for source in sources if source["adapter"] == "html_listing"}
    assert direct_feed_ids | autodiscovery_ids | html_ids == {source["id"] for source in sources}
    assert parse_feed((FIXTURES / "quanta.xml").read_bytes())
    assert parse_feed((FIXTURES / "ietf_oauth.atom").read_bytes())
    assert parse_feed((FIXTURES / "ietf_scim.atom").read_bytes())
    assert (FIXTURES / "autodiscovery.html").exists()
    assert html_ids == {"anthropic_engineering", "barbell_medicine", "okta_workflows"}
