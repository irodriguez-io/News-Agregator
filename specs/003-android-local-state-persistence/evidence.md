# 003 — Android Local State Persistence — evidence

**Item branch:** `feat/003-android-local-state-persistence`, cut from `main` at `99016e6`\
**Slices:** 3, all done, each gated by an independent non-author review before the next was dispatched\
**Suite:** 62 JVM tests at item start → **99** at `3fe6435`, zero deleted

---

## Commit chain

| Commit | Kind |
|---|---|
| `f442c38` | design artifacts (spec, design note, slice plan) |
| `cee8af3` | spec correction — `signalsApplied` derived, not all-false |
| `3d64746` | spec correction — the reversal hazard the forced read signal carries |
| `bea0017` | carry-forward notes for later items |
| `6ca86cd` → `a85bcc3` | slice 1 red → green |
| `c8d8caf` → `cefe5b6` | slice 1 follow-up: typed test harness, calendar-timestamp message |
| `3afaf3c` → `d5efa5b` | slice 1 review findings |
| `fd2da56` | slice 1 marked done |
| `d2361a4` → `b29d119` | slice 2 red → green |
| `13fdb3a` → `4b87aed` | slice 2 review findings |
| `643ddc5` | slice 2 marked done |
| `13ecc7e` → `3fe6435` | slice 3 red → green |
| `31dcb13` | slice 3 marked done |

Every slice closed as a failing-first test commit followed by an implementation commit. No test was
edited to pass, and no existing assertion was removed except one, which review caught and had restored
(see slice 2 findings).

## Gates

Verified by the orchestrator with `--rerun-tasks`, not read from the implementer's report:

- `./gradlew :app:testDebugUnitTest` — **99 tests, 0 failures, 0 errors** (42 tasks executed).
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- `./gradlew :app:connectedDebugAndroidTest` — 1/1 launch smoke passing on a Pixel_10 API 37 emulator.
  Not a CI gate; the Android workflow stays emulator-free by the decision recorded in item 002 slice 4.

Untouched and unaffected: `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`,
`tests/**`, `docs/v1/**`. Every code change in this item is under `android/`.

## Owner walkthrough — `spec.md` §5

Walked on a Pixel_10 API 37 emulator with `run-as` byte inspection of the stored document.

| Check | Result |
|---|---|
| Clean cold start, browse Discover | No state file created — displaying is not a persistent interaction |
| Save / dismiss / mark read, `am force-stop`, relaunch | All three restored; Read Later 1, History 1; JSON matched `contracts.md:657-692` field for field |
| Dark appearance + Technology category | Both restored |
| Record whose article ID is absent from the snapshot | Rendered in full from its stored snapshot |
| `echo not-json > files/…json` | Persistent recovery notice; Save refused; badge unmoved; file still exactly `not-json` |
| Reset after corruption | Document removed, next Save wrote a fresh valid V1 document — the lock was lifted |
| `schemaVersion: 2` | Recovery notice; document unchanged on disk |
| Backup exclusion | Merged manifest carries `android:allowBackup="false"`; `bmgr backupnow` returned *Backup is not allowed* |

## What review caught that the gates did not

Three of the four slice 1 findings and both slice 2 findings were invisible to a green suite. Recorded
because the pattern is the lesson, not the individual bugs:

- **Foreign learning signals were zeroed on every transition** (`ArticleStateMachine`). The test that
  covered signal derivation used only records whose signals were already false, so it asserted the bug.
- **`Instant.toString()` emits six fractional digits** where the contract's pattern allows three, so the
  first save against a real clock would have returned `INVALID_STATE` and — correctly, under
  persist-before-publish — the UI would have refused to show any triage action as done. Slice 1 never
  used a real clock; this would have surfaced in slice 2 as "saving does nothing", three layers from
  its cause.
- **A transient read failure permanently locked writes**, diverging from the ported behaviour and
  leaving Reset — which destroys the reader's data — as the only exit.
- **An unsupported schema was reported as `INVALID_STATE`** for exactly the documents the rule exists
  for, because strict decoding ran before the version comparison.
- **Persistence was launched from a composition-scoped coroutine**, so a rotation timed mid-write left
  the ViewModel holding stale state against a written document, for the rest of the process.
- **A frozen-contract assertion was lost in a rename** — item 002's `Appearance.entries` check. Restored
  along with the `wireValue` mapping, which nothing had ever pinned despite it being what the stored
  document actually contains.

The first slice 1 attempt also routed every test through a Java reflection harness so the tests would
compile before the production classes existed. It was rejected and rewritten against real types; the
replacement was required to prove itself by three mutations of the production code, each of which named
the test that caught it.

## Deviations from the plan, and why

- **`signalsApplied` is derived, not all-`false`.** The design's first draft was wrong: the frozen
  validator asserts `opened === (openedAt !== null)` and `read === (status === "read")`, so an all-false
  document is invalid the moment a record is opened. Escalated by the implementer rather than resolved
  silently, decided by the owner, recorded as `design.md` D9. `dismissed` and `saved` stay `false`,
  which the validator permits and which protects the reversal math.
- **No live-region mechanism existed to reuse.** `slices.md` told slice 3 to reuse item 002's; item 002
  had none — that was a web-side concept. Slice 3 introduced one using `LiveRegionMode.Polite`.

## Outstanding

- **The pre-Compose system splash can still show a light frame.** D8's gate holds for every *composed*
  frame — the app renders no content until local state resolves — and `spec.md` §4 asks for exactly
  that. But Android paints the launch theme's window before Compose starts, and no static
  `windowBackground` can be correct when the stored appearance may be Dark while the system is Light.
  Closing it needs a launch-theme or splash-screen design of its own. Recorded, not fixed.
- **No import or export.** Deferred to its own item along with the Storage Access Framework surface, the
  5 MiB cap, and atomic replacement. `specs/future-items.md` records what that item inherits.
- **Preference learning is still absent**, so `preferences` is always empty. `specs/future-items.md`
  records the one-time reconciliation that item will need, because records written here carry derived
  signal flags with no deltas behind them.
- **Instrumented tests remain outside CI**, by the decision recorded in item 002 slice 4.

## Reviewer independence

All product and test code in this item was written by the implementer agent (Codex) across six fresh
sessions. The reviewer authored the specification, design note, slice plan, and this evidence file, and
wrote no product or test code. Every gate result quoted here was reproduced by the reviewer with
`--rerun-tasks` rather than accepted from an implementer's report.
