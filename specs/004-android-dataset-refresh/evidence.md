# 004 — Android Dataset Refresh — evidence

**Item branch:** `feat/004-android-dataset-refresh`, cut from `main` at `8f5706a`\
**Slices:** 3, each gated by an independent non-author review before the next was dispatched\
**Suite:** 99 JVM tests at item start (`18f4ce3`) → **131** at `8c052b6`, zero deleted

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

Every slice closed as a failing-first test commit followed by an implementation commit. No test was
edited to pass. No existing assertion was removed in this item — the only change to an existing test
file was giving a `dataset(…)` fixture helper a defaulted `generatedAt` parameter, which left every
prior call site's behaviour unchanged.

Each RED was verified by the orchestrator in a throwaway worktree at the test commit, not accepted from
a report. Slices 1 and 3 failed to compile against absent production contracts; the slice 1 follow-up
failed on exactly one assertion (`an unreadable cached payload does not prevent adopting a valid
replacement`) out of 118.

## Gates

Verified by the orchestrator with `--rerun-tasks`, not read from the implementer's report:

- `./gradlew :app:testDebugUnitTest` — **131 tests, 0 failures, 0 errors, 0 skipped** (42 tasks executed).
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

**Not yet performed.** Requires a Pixel API 37 emulator and reader-level interaction (triaging
articles, airplane-mode relaunch, `run-as` inspection). Outstanding before merge.

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

- **The owner walkthrough (`spec.md` §5) has not been run.** See above.
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
