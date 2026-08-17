"""Bounded HTTP retrieval with redirect and derived-target SSRF protection."""

from __future__ import annotations

from dataclasses import dataclass
import ipaddress
import socket
import time
from typing import Callable, Iterable
from urllib.parse import urljoin, urlsplit

import requests

from .constants import (
    CONNECT_TIMEOUT_SECONDS,
    MAX_REDIRECTS,
    MAX_RESPONSE_BYTES,
    READ_TIMEOUT_SECONDS,
    RETRY_DELAY_SECONDS,
    TRANSIENT_STATUS_CODES,
    USER_AGENT,
)


class FetchError(RuntimeError):
    """A bounded, content-free source retrieval failure."""

    def __init__(self, code: str, detail: str = "") -> None:
        self.code = code
        self.detail = detail
        super().__init__(f"{code}{': ' + detail if detail else ''}")


@dataclass(frozen=True)
class FetchResponse:
    body: bytes
    url: str
    content_type: str


def _addresses_for_host(hostname: str) -> Iterable[str]:
    try:
        records = socket.getaddrinfo(hostname, None, type=socket.SOCK_STREAM)
    except socket.gaierror as exc:
        raise FetchError("dns_failure") from exc
    addresses = {record[4][0].split("%", 1)[0] for record in records}
    if not addresses:
        raise FetchError("dns_failure")
    return addresses


def validate_public_url(
    url: str,
    *,
    resolver: Callable[[str], Iterable[str]] = _addresses_for_host,
) -> str:
    """Validate a remote-derived URL and resolve every address as public."""

    try:
        parsed = urlsplit(url)
        _ = parsed.port
    except (TypeError, ValueError) as exc:
        raise FetchError("invalid_derived_url") from exc
    if (
        parsed.scheme.lower() not in {"http", "https"}
        or not parsed.hostname
        or parsed.username
        or parsed.password
    ):
        raise FetchError("invalid_derived_url")
    hostname = parsed.hostname.rstrip(".").lower()
    if hostname == "localhost" or hostname.endswith(".localhost"):
        raise FetchError("non_public_target")
    try:
        literal = ipaddress.ip_address(hostname.split("%", 1)[0])
        addresses = [str(literal)]
    except ValueError:
        addresses = resolver(hostname)
    try:
        parsed_addresses = [ipaddress.ip_address(address) for address in addresses]
    except ValueError as exc:
        raise FetchError("dns_failure") from exc
    if not parsed_addresses or any(not address.is_global for address in parsed_addresses):
        raise FetchError("non_public_target")
    return url


class HttpClient:
    """One reusable requests session with a single bounded retry."""

    def __init__(
        self,
        session: requests.Session | None = None,
        *,
        sleeper: Callable[[float], None] = time.sleep,
        target_validator: Callable[[str], str] = validate_public_url,
    ) -> None:
        self.session = session or requests.Session()
        self.session.headers.update({"User-Agent": USER_AGENT, "Accept-Encoding": "gzip, deflate"})
        self.sleeper = sleeper
        self.target_validator = target_validator

    def get(self, url: str, *, derived: bool = False) -> FetchResponse:
        if derived:
            self.target_validator(url)
        last_error: FetchError | None = None
        for attempt in range(2):
            try:
                return self._get_once(url)
            except FetchError as exc:
                last_error = exc
                if attempt or exc.code not in {"transient_http", "network_interruption", "timeout"}:
                    raise
                self.sleeper(RETRY_DELAY_SECONDS)
        assert last_error is not None
        raise last_error

    def _get_once(self, url: str) -> FetchResponse:
        current = url
        origin_host = (urlsplit(url).hostname or "").lower()
        for redirect_count in range(MAX_REDIRECTS + 1):
            try:
                response = self.session.get(
                    current,
                    allow_redirects=False,
                    stream=True,
                    timeout=(CONNECT_TIMEOUT_SECONDS, READ_TIMEOUT_SECONDS),
                )
            except requests.Timeout as exc:
                raise FetchError("timeout") from exc
            except requests.ConnectionError as exc:
                raise FetchError("network_interruption") from exc
            except requests.RequestException as exc:
                raise FetchError("request_failure") from exc

            try:
                if response.is_redirect or response.is_permanent_redirect:
                    if redirect_count >= MAX_REDIRECTS:
                        raise FetchError("redirect_limit")
                    location = response.headers.get("Location")
                    if not location:
                        raise FetchError("invalid_redirect")
                    target = urljoin(current, location)
                    try:
                        parsed_target = urlsplit(target)
                        _ = parsed_target.port
                    except ValueError as exc:
                        raise FetchError("invalid_redirect") from exc
                    if (
                        parsed_target.scheme.lower() not in {"http", "https"}
                        or not parsed_target.hostname
                        or parsed_target.username
                        or parsed_target.password
                    ):
                        raise FetchError("invalid_redirect")
                    if parsed_target.hostname.lower() != origin_host:
                        self.target_validator(target)
                    current = target
                    continue

                if response.status_code in TRANSIENT_STATUS_CODES:
                    raise FetchError("transient_http", str(response.status_code))
                if not 200 <= response.status_code < 300:
                    raise FetchError("http_error", str(response.status_code))

                content_length = response.headers.get("Content-Length")
                if content_length:
                    try:
                        if int(content_length) > MAX_RESPONSE_BYTES:
                            raise FetchError("response_too_large")
                    except ValueError:
                        pass
                chunks: list[bytes] = []
                size = 0
                try:
                    for chunk in response.iter_content(chunk_size=64 * 1024):
                        if not chunk:
                            continue
                        size += len(chunk)
                        if size > MAX_RESPONSE_BYTES:
                            raise FetchError("response_too_large")
                        chunks.append(chunk)
                except requests.Timeout as exc:
                    raise FetchError("timeout") from exc
                except requests.ConnectionError as exc:
                    raise FetchError("network_interruption") from exc
                except requests.RequestException as exc:
                    raise FetchError("network_interruption") from exc
                return FetchResponse(
                    body=b"".join(chunks),
                    url=current,
                    content_type=response.headers.get("Content-Type", ""),
                )
            finally:
                response.close()
        raise FetchError("redirect_limit")
