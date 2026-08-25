# 004 — Android Dataset Refresh — evidence

**Item branch:** `feat/004-android-dataset-refresh`, cut from `main` at `8f5706a`\
**Slices:** 3, each gated by an independent non-author review before the next was dispatched\
**Suite:** 99 JVM tests at item start (`18f4ce3`) → **135** at `bcc18f1`, zero deleted

---

## Commit chain

| Commit | Kind |
|---|---|
| `18f4ce3` | design artifacts (spec, design note, slice plan) |
| `e2d5b48` → `7616ecb` | slice 1 red → green |
| `fe6382b` → `6a39837` | slice 1 review findings 1 and 2 |
| `a98bce9` | slice 1 review finding 3 — unreachable state removed |
| `af83f70` | slice 1 marked done |
| `44b0863` | slice 2 red |
| `11f1aa0` | slice 2 — supervisor-authorized correction of a self-contradictory test |
| `4735adb` | slice 2 green |
| `059d531` | slice 2 marked done |
| `e9a40ca` → `8c052b6` | slice 3 red → green |
| `de60f88` | first evidence draft, PR #6 opened |
| `8ae661c` | spec corrections — D3's ETag form, D12 amended after the walkthrough |
| `c4ef24c` → `bcc18f1` | slice 3 walkthrough finding: failed-refresh disclosure on Discover |

Every slice closed as a failing-first test commit followed by an implementation commit. No test was
edited to pass. Exactly one existing assertion changed in this item, in `bcc18f1`: the frozen-copy
assertion pinning `Labels.DEGRADED_NOTICE`, updated because the owner amended that copy. It remains an
exact-string assertion. The only other change to an existing test file was giving a `dataset(…)`
fixture helper a defaulted `generatedAt` parameter, which left every prior call site's behaviour
unchanged.

Each RED was verified by the orchestrator in a throwaway worktree at the test commit, not accepted from
a report. Slices 1 and 3 failed to compile against absent production contracts; the slice 1 follow-up
failed on exactly one assertion (`an unreadable cached payload does not prevent adopting a valid
replacement`) out of 118.

## Gates

Verified by the orchestrator with `--rerun-tasks`, not read from the implementer's report:

- `./gradlew :app:testDebugUnitTest` — **135 tests, 0 failures, 0 errors, 0 skipped** (42 tasks executed).
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- Merged manifest (`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`)
  — exactly **one** `uses-permission` line, `android.permission.INTERNET`, and
  `android:usesCleartextTraffic="false"`. No `ACCESS_NETWORK_STATE`, per D11.
- Built APK — contains no `assets/` entries and no JSON. The client no longer ships articles.
- No unit test touches the network (D10); the transport sits behind `DatasetFetcher` with a fake, and
  the cache is exercised against temp directories.

Untouched and unaffected: `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`,
`tests/**`, `docs/v1/**`. Every change in this item is under `android/`.

## Owner walkthrough — `spec.md` §5

Walked on a Pixel_10 API 37 emulator on 2026-08-25, against the debug APK built from the PR head.

| Check | Result |
|---|---|
| Fresh install, online | **214** articles offered — not the 166 the old APK shipped — and the header stated `Content age · 1h`, derived from `generatedAt` |
| Airplane mode, force-stop, relaunch | Cached articles still offered; Read Later 1 / History 1 intact; 211 available after three triages; cache byte-unchanged. **Disclosure gap found — see below** |
| Clear data, offline launch | `Discover is unavailable right now` with **Try again**; copy stated saved reading remains on the device; Read Later, History, Settings all reachable; `files/` empty, nothing written |
| Leave airplane mode, retry | Queue arrived, `Content updated.` announced through the live region, cache written |
| Triage then refresh | Saved, dismissed, and read articles all stayed out of Discover across both a refresh and an offline relaunch; Read Later and History still listed them |
| `run-as` inspection of `filesDir` | Payload (220,122 B) and sidecar (73 B) both present alongside item 003's local-state document |
| Stored `ETag` vs served header | Matched — see the D3 correction below; `304` confirmed, and the payload's mtime did not move across a refresh, proving the `304` rewrote nothing |
| `aapt dump permissions` | `uses-permission: name='android.permission.INTERNET'` and nothing else |

Beyond the checklist, three things were verified that no unit test covers: the conditional GET was
exercised against the live endpoint with both the weak and strong ETag forms (both `304`, zero bytes);
the cached payload's mtime was compared before and after a refresh to prove `304` is not a rewrite; and
Settings was read on device to confirm the `304` surfaces as `Last refresh · Already current` rather
than as a failure.

### What the walkthrough caught that no test could

On an offline relaunch with a cached dataset, the Discover header rendered **pixel-identical to the
online header**:

> Refresh · Content age · 1h · *Some sources were unavailable during the latest refresh.* · 214 available in All

Nothing said the app could not reach the server — that fact lived only in Settings. Worse, the one line
that *sounded* like it described the failed fetch described something else entirely:
`DEGRADED_NOTICE` fires on `failedSourceCount` from the displayed dataset's pipeline metadata, i.e. the
sources unavailable when the **pipeline generated the content**, hours earlier. An offline reader would
read it as the story of their failed refresh, conclude that was what went wrong, and never learn that
no refresh happened at all.

Every unit test passed throughout. The defect was in what two independently-correct lines meant when
placed next to each other, which is only visible on a screen.

The owner amended `design.md` D12 (`8ae661c`) and the fix landed in `c4ef24c` → `bcc18f1`. The offline
header now reads three distinct facts:

> Content age · 2h · **Refresh failed. Showing the last available content.** · Some sources were unavailable **when this content was gathered.** · 214 available in All

Both surfaces were re-verified on the emulator after the fix: the failure line appears offline and is
absent online.

## What review caught that the gates did not

All three slice 1 findings and the slice 2 contradiction were invisible to a green suite:

- **An unreadable cached payload permanently blocked every refresh.** `refresh()` returned
  `Failed(CACHE_READ)` before calling the fetcher, while an *invalid* payload was already mapped to
  "no cache" so a refresh could replace it. The two cases have the same remedy under D9 — the cache is
  regenerable — but the code treated them oppositely, leaving a state whose only exit was Reset, which
  destroys Read Later and History. That is the same dead end `spec.md` §1 exists to remove, reintroduced
  one layer down. Fixed at the cache boundary so the policy lives in one place.
- **The production endpoint was unpinned.** Every fetcher test built the class through its `internal`
  constructor with a hardcoded string, so `DATASET_URL` and the no-arg constructor were never asserted.
  Changing the host — or the scheme — would have broken no test, despite D2 being a security control.
- **Fixing the first finding orphaned `DatasetCacheRead.Failed`.** With no producer left, the variant,
  `DatasetRefreshErrorCode.CACHE_READ`, and the `refresh()` branch consuming it were dead — and that
  dead branch still contained the defect just removed. Because `DatasetCacheRead` is public and slice 2
  wires it into the view model, Kotlin exhaustiveness would have forced a UI branch for an impossible
  state. Deleted so the compiler enforces D9 rather than convention.
- **Slice 2's RED commit specified two opposite outcomes for the same situation.** `cold start
  publishes the cache before a reachable refresh adopts its replacement` expected the newer dataset's
  head to win, while `displayed article survives adoption when present and deck advances when absent`
  expected the displayed card to survive — both with a cached head article present in the replacement.
  The implementer stopped and escalated rather than editing either test, which is the prescribed
  behaviour. Adjudicated below.

The pattern across slices 1 and 3: the implementer began leaving no unreachable code behind once the
first two findings landed. Slice 2 pre-emptively made `DatasetRepository`'s `fetcher` and `cache`
required and deleted `NOT_CONFIGURED`; slice 3 removed the `AppUiState.refresh` default and the
`.copy(refresh = …)` shim slice 2 had used to avoid touching the mapper.

## Decisions taken during implementation

- **D8 applies uniformly, including cold start.** Ruled by the orchestrator when slice 2's tests
  contradicted each other. The authoritative scenario *a refresh does not move the article being read*
  is unconditional, and D8 protects "the displayed card" across a dataset swap without qualification.
  The cold-start test was the faulty one: it was bound to no authoritative Then-clause, and the
  scenario it gestured at — *a reachable dataset replaces the queue with what the pipeline published* —
  begins "Given the app starts with **no cached dataset**", which was not its setup. Correcting it
  weakened nothing authoritative.

  **Consequence, accepted:** on a cold start with a cached dataset, the previously-cached head article
  is pinned above the new dataset's leader until the reader triages it. Both articles are still
  offered; the effect is a one-position reordering, and the pinned article is the prior ranking's
  top-scored piece. The alternative required inventing a "was the reader actually looking?"
  distinction the spec does not make and the code cannot express.
- **The cold-start refresh does not announce its outcome; only an explicit refresh does.** The
  scenario is scoped "Given the reader asks for a refresh", and announcing on every launch would make
  the polite live region noisy for screen-reader users at exactly the moment it carries no decision.
- **The degraded notice moved from the card body into the editorial header**, beside the freshness
  line, as D12 directs. Its trigger (`failedSourceCount > 0`) is unchanged. It now also renders in the
  Empty state, which is correct — the notice describes the dataset, not the card.
- **Only the failed refresh outcome is disclosed persistently on Discover.** `Updated` and
  `Already current` keep the transient announcement plus the Settings line. A permanent "we succeeded"
  is noise; a permanent "this is not current" changes what the reader should believe about what is on
  screen. The disclosure is also suppressed when there is no cached dataset, because the existing
  `DiscoverUiState.Error` panel already covers that case and a second failure line would be redundant.
- **`design.md` D3 was corrected, not the code.** The note claimed the endpoint "serves a strong ETag".
  It serves strong uncompressed and **weak** (`W/"…"`) over gzip, which `HttpURLConnection` requests on
  its own — so the client stores the weak form, which is correct, because it stores what it was served.
  `If-None-Match` uses weak comparison and the `304` still arrives. Verified both directly and on
  device.

## Deviations from the plan, and why

- **`slices.md` scoped slice 3 to the view layer, but the DoD required the refresh outcome to be
  announced**, and announcements originate in `AppViewModel`. Slice 3 was authorized to touch
  `AppViewModel.kt`, `AppUiState.kt`, and `IntentionalReadingApp.kt` for the announcement kinds and
  plumbing only, with slice 2's refresh ordering, in-flight guard, adoption path, and D8 preservation
  explicitly fenced off.
- **Slice 2 removed the bundled-asset path entirely** — `AssetDatasetSource`, `DatasetSource`, and
  `DatasetRepository.load()` — rather than leaving it orphaned once the asset moved to test resources.
  Not in the slice plan's file list, but forced by the slice 1 findings about unreachable code.
- **`android/README.md`'s "Bundled dataset" section became "Test fixture provenance"**, dropping the
  now-false claim that the unit suite reads the bytes shipping in the APK. The fixture moved as a
  100%-similarity rename; its SHA-256 is unchanged at
  `235e4df614b66108d1a471dddfa0b3ce06d838ac058d8570d440a5d7ac93f27f`.

## Known behaviour this item introduces

- **The degraded notice will now actually appear in the app**, because the live dataset currently
  reports three failed sources. That is correct behaviour finally receiving real data, not a
  regression — recorded because it will look like one.
- **A fresh install now requires a network.** Removing the bundled snapshot means an offline first
  launch shows the dataset failure state with a retry action rather than months-old articles. That is
  the intended trade — known provenance over an offline seed — but it is a real regression for that one
  case, as `design.md` anticipated.

## Outstanding

- **`AppViewModel.adoptDataset()` derives the displayed article by reading back its own published UI
  state** (`uiState.value.discover as? DiscoverUiState.Card`) to apply D8. It is correct and covered —
  the D8 test fails if the cast stops matching — but it is inverted data flow, and it couples a domain
  decision to a screen-level type. Raised at the slice 2 review, judged not worth a round trip, carried
  into slice 3's brief as a warning. Worth restructuring if a fourth writer of that state appears.
- **The pre-Compose launch frame** recorded as outstanding by item 003 is untouched here.
- **Background and periodic refresh** remain out of scope: the app fetches on cold start and on
  request, never otherwise.

## Reviewer independence

All product and test code in this item was written by the implementer agent (Codex) across five fresh
sessions — one per slice plus two findings follow-ups. The orchestrator authored the specification,
design note, slice plan, and this evidence file, and wrote no product or test code. Every gate result
quoted here was reproduced by the orchestrator with `--rerun-tasks`, every RED was reproduced in a
throwaway worktree, and the merged manifest and APK contents were inspected directly rather than
accepted from a report.
