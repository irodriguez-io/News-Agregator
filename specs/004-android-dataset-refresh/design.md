# 004 — Design note

Decisions for `spec.md`. Where the web client already answers a question, the answer is ported and
cited rather than re-derived.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths: `android/**` and
`.github/workflows/android.yml`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`, `docs/v1/**`. The `ArticleDataset v1` contract is consumed as
frozen; nothing in this item widens, reinterprets, or forks it.

## D1 — `HttpURLConnection`, and therefore no new dependency

Android does not ship `java.net.http.HttpClient`, so the JDK 11 client is not an option. OkHttp,
Retrofit, and Ktor are each a new entry in `android/gradle/libs.versions.toml`, which
`08-security-dependencies.md` §40 makes a justified decision rather than a convenience — and the
justification does not exist: this item performs exactly one conditional GET of one static file from
one origin. The platform's `HttpURLConnection` (itself OkHttp inside the platform since API 21) covers
that in a few dozen lines.

Nothing is added to the version catalog in this item. If a slice appears to need a dependency, that is
a report to the supervisor, not a decision to make.

## D2 — One compile-time HTTPS origin, and no redirect is followed

The URL is a constant: `https://irodriguez.io/News-Agregator/data/articles.json`. There is no
user-editable endpoint, no build-variant switch, no discovery, and no mirror. A URL whose scheme is
not `https` is refused before a connection is opened, and the manifest sets
`android:usesCleartextTraffic="false"` so the platform enforces it independently of application code.

Redirects are **not** followed (`setInstanceFollowRedirects(false)`); a 3xx is a failed refresh.
This deliberately diverges from `07-pipeline-deployment.md` §13, which allows the pipeline five
redirects per retrieval, and the reason is that the two situations are not alike. The pipeline
retrieves URLs from a publisher catalog whose hosts legitimately redirect; this client retrieves one
URL it hardcoded from a host it controls. A redirect there is either a deployment change worth
noticing or something worth refusing, and §17–18 of `08-security-dependencies.md` — reject derived
targets, allow only `http`/`https` — points the same way. If GitHub Pages ever starts redirecting this
path, that is a spec change, not a silent follow.

`08-security-dependencies.md` §6 and §52 bound the whole item: the client contacts its own dataset
origin and nothing else. No publisher is fetched at runtime, on any surface, ever.

## D3 — Conditional GET, and `304` is a success

The cached `ETag` is sent as `If-None-Match`. Verified against the live endpoint on 2026-08-24: it
serves a strong ETag and answers a matching `If-None-Match` with `304` and a zero-byte body, so the
common launch — the pipeline refreshes every six hours (§43), the reader opens the app more often than
that — costs one round trip and no payload.

`304` therefore means *the cache is current*. It is reported as a successful refresh, it updates the
"last refreshed" fact, and it must not be routed through the failure path or cause a rewrite of the
cache. `200` carries a replacement. Everything else — any other status, a transport failure, a
timeout, a redirect — is a failed refresh.

`Last-Modified`/`If-Modified-Since` is not used. One validator is enough, and the ETag is the stronger
of the two.

## D4 — Bounded like the pipeline's own retrieval

Connect timeout 10s, read timeout 20s, and a hard ceiling of 10 MiB on the body, taken from
`07-pipeline-deployment.md` §10 and §14 rather than invented. §19 of the security spec says those
limits are security controls, not tuning, and must not be relaxed because a real server behaves well in
testing. The live dataset is ~207 KB, so the ceiling is a defensive bound, exactly as the 5 MiB import
cap is.

The ceiling is enforced **while reading**, not by trusting `Content-Length`: a `Content-Length` is a
claim by the sender, and a chunked response has none.

## D5 — Validate before adopting, and never destroy the last good copy

A response becomes a dataset only after `DatasetValidator` accepts it as `ArticleDataset v1`. Until
then it is bytes. An invalid body, an unsupported `schemaVersion`, an oversized body, or a transport
failure leaves the cache byte-identical and the displayed dataset untouched, and reports a failed
refresh.

This is the same rule in three places already: item 003 preserves malformed local state rather than
overwriting it (`08-security-dependencies.md` §27, `js/state/storage.js:189-211`), and the pipeline
refuses to replace a deployed dataset with a broken one (`07-pipeline-deployment.md` §49). A client
that overwrote a good cache with a bad response would be the only component in the system that loses
data on failure.

## D6 — No client-side catastrophic-reduction gate

A structurally valid v1 dataset is adopted even if it contains fewer articles than the cached one. The
minimum-article and source-success-ratio gates live in the pipeline (`07-pipeline-deployment.md` §32,
§33) and run before anything is published; a second copy of those thresholds on the client would be a
second policy, unsynchronized with the first, that eventually disagrees with it and refuses a dataset
the pipeline deliberately shipped.

Recorded because it is a real choice, and because "the client should sanity-check the size" is the
first thing a reviewer will suggest.

## D7 — Cache first, then refresh

Order on start: local state resolves (item 003 D8 is unchanged and still gates the first composed
frame), the cached dataset is read and published, then the refresh runs and swaps in a newer dataset if
it gets one. Discover blocks on the network only when there is nothing cached to show — which is the
first launch, and is exactly what `06-ui-ux.md` §67's loading state and §69's failure state are for.

A refresh is never started while one is in flight.

## D8 — A refresh does not move the card the reader is looking at

Adopting a dataset recomputes the deck, so the head article can change. If the article currently on
screen is still present in the new dataset it must remain on screen. `DiscoverDeck.build` already has
the mechanism — `heldArticleId` wins over dataset order
(`domain/state/DiscoverDeck.kt:25`) — and item 003 already keeps `heldArticleId` for an opened article
that the reader has not yet resolved. This item extends the same protection to the displayed card
across a dataset swap; if the article is gone from the new dataset, the deck advances normally, which
is the behaviour Read Later and History already rely on for articles that leave retention.

## D9 — Two files in `filesDir`, written with item 003's discipline

The validated response bytes are stored verbatim in one file, and `{etag, fetchedAt}` in a sidecar
written **after** it. Both go through temp file → `flush()` → `fd.sync()` → `renameTo`, the sequence
`LocalStateFile.write` established (`data/local/state/LocalStateFile.kt:34-51`).

Recovery rules follow from the write order:

- payload present, sidecar absent or unreadable → a usable cache with no validator; refetch
  unconditionally;
- payload absent → no cache, whatever the sidecar says;
- payload present but no longer valid on read → treated as no cache (a refresh replaces it), because
  unlike local state these bytes are a copy of something public and losing them costs a fetch, not the
  reader's history.

That last rule is the one real divergence from item 003's handling of a corrupt file, and it is
deliberate: the recovery lock exists to protect data that cannot be regenerated. This cache can.

**The write discipline is duplicated, not extracted.** `LocalStateFile` is `internal` to its package,
carries a `beforeRename` seam that exists for local-state tests, and owns `reset()` semantics that
belong to the reader's data. Generalizing it would put the dataset cache and the reading history on one
code path, so a change made for one would be a change made for both. Thirty lines of duplication is
the cheaper mistake. Revisit if a third writer appears.

## D10 — The network never enters a unit test

`DatasetSource` today is `fun interface DatasetSource { fun read(): ByteArray }`
(`data/local/DatasetSource.kt:5-7`), which cannot express "not modified" or a typed failure. It is
reshaped into a fetch interface returning a result — not-modified, a body with its ETag, or a failure
kind — with the `HttpURLConnection` implementation behind it and a fake in tests.

Unit tests therefore exercise every scenario in `spec.md` §4 except the transport itself, offline and
deterministically, against fakes and a temp directory. The Android CI job stays emulator-free and
network-free (`.github/workflows/android.yml`); the transport is covered by the owner walkthrough, as
the instrumented tests already are by the decision recorded in item 002 slice 4.

## D11 — `INTERNET`, and nothing else

`INTERNET` is a normal permission: declared in the manifest, granted at install, no runtime prompt, no
user-visible consent surface. It is unavoidable for this item and sufficient for it.

Deliberately not declared: `ACCESS_NETWORK_STATE`. Reachability is not asked about — the fetch is
attempted and its failure is the answer. A pre-flight connectivity check would be a second permission
and a second source of truth that can disagree with the request that follows it.

`android:allowBackup="false"` and the data-extraction rules from item 003 stay as they are; a cache of
public data changes nothing about them.

## D12 — Freshness is `generatedAt`, not fetch time

What the reader needs to know is how old the *content* is, so the disclosure is derived from the
dataset's own `generatedAt` (`07-pipeline-deployment.md` §22), formatted through the existing
`ui/format/RelativeTime.kt`. Fetch time answers a different and less useful question — a successful
fetch of a stale artifact would read as fresh.

The last refresh outcome is stated separately, because "content generated 3 hours ago" and "we could
not reach the server just now" are two different facts and collapsing them into one line loses the
one that explains the other. Settings carries the absolute timestamp for when something looks wrong.

The existing degraded notice (`Labels.DEGRADED_NOTICE`, driven by `failedSourceCount > 0`) stays as it
is and sits beside the freshness line; §23 of the deployment spec is what authorizes the frontend to
surface pipeline metadata this way. Note that the notice will now actually appear in the app, because
the live dataset currently reports three failed sources under Amendments 4 and 5 — that is correct
behaviour finally getting real data, not a regression introduced here.

## Divergences from the web client, deliberate

- **The browser holds no copy; this client does.** The web app refetches on every load, so it is
  either current or visibly broken. An installed app with a cache has a third state — working, and
  quietly out of date — which is why D12's disclosure exists at all. The web client has no equivalent
  surface and needs none.
- **Redirects are refused here and followed in the pipeline** (D2).
- **A corrupt cache is discarded; corrupt local state is preserved** (D9).

## Risks

- **A refresh landing mid-triage.** D8 protects the displayed card, but the counts beside it change,
  and an article the reader was about to reach may vanish from the new dataset. That is inherent to a
  feed that updates; the alternative — pinning the deck until the reader leaves — trades a visible
  count change for a silently stale queue, which is the defect this item exists to fix.
- **`HttpURLConnection` is a dated API.** It is easy to use slightly wrong: streams left unclosed,
  `disconnect()` semantics, error streams read from the wrong place, `304` arriving through
  `getErrorStream`. The mitigation is that the whole surface is one class behind one interface, tested
  through a fake, with the real transport walked once on a device.
- **First launch now requires a network.** Removing the bundled snapshot means an offline fresh
  install shows the failure state rather than months-old articles. That is the intended trade — the
  owner chose known provenance over an offline seed — but it is a real regression for one specific
  case and belongs in the item's evidence.
