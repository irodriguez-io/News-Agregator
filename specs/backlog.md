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

Last reviewed: 2026-08-26, at wave B merged (`008`, `009`).

---

## Shipped

| # | Item | Surface | Merged |
|---|---|---|---|
| 001 | Opened article return state | Browser | PR #3, 2026-08-22 |
| 002 | Android client foundation | Android | PR #4, 2026-08-22 |
| 003 | Android local state persistence | Android | PR #5, 2026-08-24 |
| 004 | Android dataset refresh | Android | PR #6, 2026-08-25 |
| 007 | Undo | Android | PR #10, 2026-08-25 |
| 010 | Launch theme | Android | PR #9, 2026-08-25 |
| 011 | Web validator parity and shared copy | Browser + Android | PR #8, 2026-08-25 |
| 008 | Swipe gestures | Android | PR #12, 2026-08-26 |
| 009 | Import and export | Android | PR #13, 2026-08-26 |

Each has `spec.md`, `design.md`, `slices.md`, and `evidence.md` under `specs/<n>-<slug>/`.

---

## Queued

Three items. Two are Android deferrals closing the client's remaining distance from the browser's V1
behaviour, each recorded in writing by a shipped item — the citation is given so the next designer starts
from the reasoning rather than from the summary. The third came from the owner testing wave B's build.

**Waves A and B are done.** `waves/wave-b-note.md` records what wave B cost. Its headline lesson is
that the two most valuable defects of the wave were found by the owner using the app, and a third by
re-gating a rebased head — none of them by reading diffs, and none of them by any gate.

| Wave | Items | Runs after | Brief |
|---|---|---|---|
| ~~A~~ | ~~007 Undo · 010 Launch theme · 011 Validator parity~~ | **merged 2026-08-25** | `waves/wave-a.md`, `waves/wave-a-note.md` |
| ~~B~~ | ~~008 Swipe · 009 Import/export~~ | **merged 2026-08-26** | `waves/wave-b.md`, `waves/wave-b-note.md` |
| C | ~~005 Learning~~, then 006 Diversity | **005 merged; 006 now** | `waves/wave-c.md` |

Waves are ordered by file collisions, not by value — see `execution-model.md` §2 for the hub-file matrix
that produced them. 007 led the programme deliberately, because `contracts.md` §23 ties Undo to the
`signalsApplied` reversal guard 005 introduces; that ordering has now been banked.

### ~~005 — Preference learning and personalized ranking~~  ·  **Shipped**

Ported `js/ranking/personalize.js` and the `INTERACTION_DELTAS` table. Four slices: the arithmetic, the
state machine and undo path, the reconciliation fold, and personalized scoring with deck order.
198 → 254 tests.

The inherited decision was taken by invariant rather than by marker (`design.md` D1): `interactions` is
exactly recomputable from the record set, weight is not, so the fold applies only the missing signals'
deltas and never audits a weight. It runs at two call sites — cold load and import — and deliberately
not at the single choke point every path funnels through, because that one also runs on every ordinary
save and would silently repair arithmetic bugs in the state machine.

*Evidence:* `specs/005-android-preference-learning/evidence.md`.

### 006 — Deck diversity sequencing  ·  *wave C*

The −8 same-source and −5 third-consecutive-category penalties (`js/ranking/deck.js:26-38`).

**Correction, carried from 005 `design.md` D12: the claim that the Android head article differs from the
browser's until 006 lands is wrong.** `penaltiesFor` reads `selected.at(-1)`, so the first selection step
carries no penalty; both clients render exactly one card; and the browser rebuilds the deck on every
render. The head card is chosen by personalized order alone in both clients, and **005 closed that gap by
itself**. 006 is a genuine spec-parity gap with no reader-visible effect at today's surface, and the owner
confirmed on 2026-08-26 that it ships anyway as its own item. Size it against that, not against the head
article.

005 leaves `DiscoverDeck` holding the ordered candidate list these penalties sequence, so the expected
surface is `DiscoverDeck.kt` and its tests only.

*Deferred by 002 §3, restated by 004 §3.*

### 012 — Discover header below the card  ·  *unscheduled*

Move Discover's **operational** header below the article card: Refresh, content age, the failed-refresh
disclosure, the available count, and the category selector. The masthead — eyebrow, title, purpose copy —
stays on top.

Requested by the owner on 2026-08-26 after testing wave B's build, and framed precisely: the card should
be the first thing in the viewport, not the thing you scroll to.

**This needs a specification amendment and therefore its own design pass.** `06-ui-ux.md:570` says
*"Discover begins with an editorial header area"*, which is an ordering rule. §21 then lists the category
selector and the available-article context among things the header *may* include, so moving those is a
narrow change rather than a rewrite — but it is still a change to `docs/v1/**`, which no feature
workstream may make silently (`AGENTS.md`; `docs/v1/README.md` §14).

**It largely subsumes item 008's D12.** That fix scrolls the incoming card into view after a swipe, and
currently clamps at the content maximum rather than placing the card at the top, because Discover's
content is shorter than the scroll that would require. With the operational block below the card there is
little left for it to correct.

*Raised by the owner, 2026-08-26. Not a deferral of any shipped item.*



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

- **`DiscoverScreen` now carries three scroll effects** — the reset on category and state-class change,
  D11's scroll-to-end when an article becomes opened, and D12's scroll-the-card-into-view when the deck
  advances. Each is individually clear and they do not currently fight. If a fourth appears, or when item
  012 moves the header, reconcile them. (*008 D11/D12*)
- **The recovery notice still says *"Reset local data in Settings to recover."*** Import is now also a
  recovery path and the copy does not say so. Left alone deliberately: changing it means authoring copy
  the specification does not provide. (*009 §Outstanding*)
- **`SettingsSheet`'s body sits one indent level shallower than its nesting** after the status message was
  pinned outside the scrollable column. Cosmetic, and no formatter gate exists to catch it; fix it when
  something next edits that file. (*009 s3 walkthrough fix*)

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

Owner walkthroughs — `spec.md` §5 in each item — performed for **003**, **004**, wave A's **010**, and
wave B's **008** and **009**, all driven over `adb` by the orchestrator and recorded in each item's
`evidence.md`; open for **001** and **002**. Item **007** has no walkthrough of its own by design — its
surface was unreachable until 008 landed the trigger, so **008's walkthrough is Undo's**, which closes
that gap. **011** has none by design; its validator scenarios cannot occur in a dataset the pipeline
emits.

**002's debt is partly retired.** Its four unobserved checks were Discover's Loading and Error states,
History's Yesterday and Earlier groups, three-tag row truncation, and a positive known-reading-time
aggregate. During wave B's close the emulator lost network mid-session, which surfaced **Discover's
Loading state** (*"Gathering a thoughtful queue…"*) and **its Error state** (*"Discover is unavailable
right now"*, with the retry control correctly disabled while a refresh was in flight) on the device for
the first time. The other three remain open.

001's tooling blocker is **partly cleared**: a Python 3.14.5 venv built from `requirements-dev.txt` runs
`python -m pytest` (144 passed) and `python -m pipeline.main --validate-config` cleanly, so the
pinned-3.13 concern turns out not to block the suite. What remains missing for 001 is
`data/articles.json` locally.

**Open from wave B, recorded rather than written off:**

- **A real Storage Access Framework round trip on hardware for 009** — export to Drive or Files, reboot,
  import back. Emulator provider behaviour differs from a real one, and that difference is the whole
  point of the check. It is an owner checkpoint in `waves/wave-b.md`.
- **A TalkBack gesture pass on both items.** The accessibility *tree* was inspected and is correct on both
  surfaces; TalkBack navigation itself was not driven.
- **008's mid-drag cue frame** was never photographed. The cue is verified by code and by the
  committed-swipe path, not mid-gesture.
- **009's walkthrough was performed on the pre-rebase build.** The merged build was not re-walked — the
  emulator had lost network. The rebase added one production line, covered by a unit test.
- **The owner's judgement on whether the swipe motion feels right** (`06-ui-ux.md` §44: tactile, quiet,
  controlled). Reported as "smooth and nice" during testing, but that was before the landing defects were
  fixed, so it is worth one more pass.
