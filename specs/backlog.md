# Backlog

The visibility surface for this project: what has shipped, what is queued, and what has been parked on
purpose. It is a tracking document, not a specification. Nothing here is approved scope, nothing here
binds `docs/v1/**`, and nothing here substitutes for the `spec.md` that `/feature-design` writes.

**On numbers.** Shipped items own their numbers. Numbers 005–011 were allocated by
`execution-model.md` §3 so that concurrent design sessions cannot both claim one; for these seven that
supersedes `future-items.md`'s "allocated at design time". Anything added below 011 follows the old rule.

**On execution.** `execution-model.md` says how these run — three waves, sequential, with concurrency
inside each wave. Per-wave briefs are in `specs/waves/`, each self-contained enough to hand to a fresh
session.

Last reviewed: 2026-08-25, at `004` merged.

---

## Shipped

| # | Item | Surface | Merged |
|---|---|---|---|
| 001 | Opened article return state | Browser | PR #3, 2026-08-22 |
| 002 | Android client foundation | Android | PR #4, 2026-08-22 |
| 003 | Android local state persistence | Android | PR #5, 2026-08-24 |
| 004 | Android dataset refresh | Android | PR #6, 2026-08-25 |

Each has `spec.md`, `design.md`, `slices.md`, and `evidence.md` under `specs/<n>-<slug>/`.

---

## Queued

Seven items. Six close the Android client's remaining distance from the browser's V1 behaviour;
one is web-side. Every one of them is a deferral some shipped item recorded in writing — the citation is
given so the next designer starts from the reasoning rather than from the summary.

| Wave | Items | Runs after | Brief |
|---|---|---|---|
| A | 007 Undo · 010 Launch theme · 011 Validator parity | now | `waves/wave-a.md` |
| B | 008 Swipe · 009 Import/export | A | `waves/wave-b.md` |
| C | 005 Learning, then 006 Diversity | B | `waves/wave-c.md` |

Waves are ordered by file collisions, not by value — see `execution-model.md` §2 for the hub-file matrix
that produced them. **007 leads the programme deliberately**: `contracts.md` §23 ties Undo to the
`signalsApplied` reversal guard 005 introduces, so Undo is cheap before learning and expensive after.

### 005 — Preference learning and personalized ranking  ·  *wave C*

Port `js/ranking/personalize.js` and the `INTERACTION_DELTAS` table (`js/state/preferences.js:1-6`) to
Android. `preferences.sources` and `preferences.topics` already persist, validate against
`contracts.md:632-655`, and round-trip; nothing writes a non-empty entry yet.

Carries the largest inherited decision in the project: every record already on disk claims
`signalsApplied` flags with no deltas behind them, so a `Mark Unread` after learning ships would subtract
weight that was never added. `future-items.md` §"The item that ports preference learning" sets out the
one-time reconciliation and why the choice should be made deliberately.

*Deferred by 002 §3, 003 §3, 004 §3.*

### 006 — Deck diversity sequencing  ·  *wave C*

The −8 same-source and −5 third-consecutive-category penalties (`js/ranking/deck.js:26-38`). Until this
lands the Android head article can differ from the browser's for the same dataset. Independent of 005 in
principle; naturally sequenced with it, since both change what Discover presents first.

*Deferred by 002 §3, restated by 004 §3.*

### 007 — Undo  ·  *wave A*

The single-slot undo manager and its toast (`js/state/article-state.js:154-231`). No Discover action on
Android currently offers Undo. `contracts.md` §23 ties Undo Not Interested and Undo Save to the same
`signalsApplied` reversal guard as 005, so shipping Undo before learning keeps the reversal math trivial
and shipping it after does not.

*Deferred by 002 §3, restated by 004 §3.*

### 008 — Swipe gestures  ·  *wave B*

`js/ui/swipe.js` — the 90px threshold, the intent lock, the rotation. The labeled buttons are the only
triage affordance on Android today, which is the compliant direction: `DESIGN.md:8` requires a labeled
equivalent for every gesture, never the reverse. So this is enrichment, not a gap in obligation. Pairs
with 007 — a mis-swipe with no Undo is worse than no swipe.

*Deferred by 002 §3, restated by 004 §3.*

### 009 — Import and export  ·  *wave B*

`exportState`/`importState` (`js/state/storage.js:303-345`) plus the Storage Access Framework surface: a
document picker and creator, the 5 MiB cap (`05-personalization-state.md:1023-1048`), atomic replacement
(§49), replacement-not-merge (§50), and the import validator's own error surface.

Cheaper than it looks — `LocalStateValidator` is already ported, so this is the wrapper. `future-items.md`
§"The item that ports import and export" records the Android → browser reversal hazard it inherits.

*Deferred by 002 §3 and 003 §3, restated by 004 §3.*

### 010 — Launch theme  ·  *wave A*

Android paints the launch window before Compose starts, so the pre-Compose frame can flash light while
the stored appearance is Dark. D8's no-flash gate holds for every *composed* frame; this is the frame
before them. No static `windowBackground` can be correct when the stored appearance is unknown at that
moment, so closing it needs a launch-theme or splash-screen design of its own.

*Recorded by 003 §Outstanding, untouched by 004.*

### 011 — Web-side validator parity and shared copy  ·  *wave A*

Three defects on the browser side, all found by Android ports reading the source and none of them
Android's to fix:

- `js/data/validation.js:145` accepts `readingTimeMinutes >= 1`; `pipeline/validation.py:81-83` requires
  ≥ 2. The pipeline is stricter than its own client validator.
- `js/data/validation.js:148-163` enforces no `tags` length limit, where the taxonomy limits organically
  detected tags to five (`contracts.md` §7).
- `js/ui/discover.js:330` renders "1 more choice wait quietly behind this one." — singular noun, plural
  verb. Ported verbatim to `Labels.kt:46` for parity, which was the right call. **Fix both clients in the
  same item or neither**; diverging unilaterally would invent a requirement.

*Raised by 002 §Outstanding and slice 2 observation 6.*

---

## Parked

Deliberate non-goals, recorded so they are not rediscovered as oversights.

- **Background and periodic refresh.** No `WorkManager`, `JobScheduler`, alarms, or push. The app fetches
  on cold start and on request, and never while closed. (*004 §3*)
- **Instrumented tests in CI.** They need an emulator; the path-filtered CI job stays emulator-free and
  runs the JVM suite plus `assembleDebug`. `MainActivityLaunchSmokeTest` is the local, on-demand startup
  guard. Decision, not oversight. (*002 slice 4*)
- **A second dataset endpoint.** One compile-time HTTPS URL: no user-editable address, no environment
  switching, no mirror, no publisher fetching (`08-security-dependencies.md` §52). (*004 §3*)
- **Delta or partial dataset updates.** Whole dataset or nothing. (*004 §3*)

---

## Debt

Not items. Things a future item should absorb when it touches the same ground.

- **`AppViewModel.adoptDataset()` reads back its own published UI state** to apply D8
  (`uiState.value.discover as? DiscoverUiState.Card`). Correct and covered — the D8 test fails if the
  cast stops matching — but it is inverted data flow coupling a domain decision to a screen-level type.
  Restructure if a fourth writer of that state appears. (*004 §Outstanding*)
- **`DiscoverUiState.Card.isOpened` tests status only**, where the browser also requires
  `openedAt !== null` (`js/app.js:118-122`). Equivalent today. Tighten it when a record can arrive from a
  path other than a transition. (*002 slice 2, observation 7*)
- **`DatasetPhase.Error` carries no error code**, so the validator's `UNSUPPORTED_SCHEMA`-versus-malformed
  distinction cannot reach the UI. Matches the browser, which renders one panel for all four codes and is
  forbidden from leaking payload text. (*002 slice 2, observation 8*)

---

## Verification debt

Owner walkthroughs — `spec.md` §5 in each item — performed for **003** and **004**, open for **001** and
**002**. 004's walkthrough caught a disclosure defect the whole JVM suite missed, which is the argument
for closing the other two rather than writing them off.

001's is blocked on local tooling: no `.venv`, no `data/articles.json`, and only Python 3.14.5 installed
against the project's pinned 3.13. 002's is superseded in part — 003 and 004 walked the same screens on a
Pixel_10 API 37 emulator — but its four unobserved checks (Discover's Loading and Error states, History's
Yesterday and Earlier groups, three-tag row truncation, a positive known-reading-time aggregate) were
never device-observed.
