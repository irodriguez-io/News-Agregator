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

Last reviewed: 2026-08-31, at item 006 closed — wave C, and the wave programme, complete.

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
| 013 | Undo gesture reset — a card accepts a swipe as soon as it is on screen | Android | 2026-08-28 |
| 006 | Deck diversity sequencing | Android | 2026-08-31 |

Each has `spec.md`, `design.md`, `slices.md`, and `evidence.md` under `specs/<n>-<slug>/`.

---

## Queued

Four items, all unscheduled and all parked on purpose. **012** came from the owner testing wave B's
build. **014**, **015** and **016** were all raised by item 013: two defects it proved but deliberately
did not fix, and one amendment it owes from its first design pass.

**Nothing is scheduled.** Each needs its own design pass; 012 and 016 additionally need a `docs/v1/**`
amendment, which no feature workstream may make silently. **014 and 015 must not inherit 013's
diagnosis** — assuming an inherited cause is what cost 013 two of its four passes.

**With 006 shipped, every Android parity gap the shipped items deferred is closed.**

**Waves A, B and C are done — the wave programme is complete.** `waves/wave-b-note.md` records what
wave B cost and `waves/wave-c-note.md` what wave C cost. Its headline lesson is
that the two most valuable defects of the wave were found by the owner using the app, and a third by
re-gating a rebased head — none of them by reading diffs, and none of them by any gate.

| Wave | Items | Runs after | Brief |
|---|---|---|---|
| ~~A~~ | ~~007 Undo · 010 Launch theme · 011 Validator parity~~ | **merged 2026-08-25** | `waves/wave-a.md`, `waves/wave-a-note.md` |
| ~~B~~ | ~~008 Swipe · 009 Import/export~~ | **merged 2026-08-26** | `waves/wave-b.md`, `waves/wave-b-note.md` |
| ~~C~~ | ~~005 Learning · 006 Diversity~~ | **merged 2026-08-31** | `waves/wave-c.md`, `waves/wave-c-note.md` |

**Item 013 ran outside the waves**, as an unplanned defect item cut from `main` at `2613959` while wave C
was open. It touched **no file item 006 touches** — confirmed at close: 013's surface is
`ui/components/ArticleCard.kt`, `ui/gesture/SwipeGesture.kt` and the instrumented gesture tests; 006's is
`domain/**` and its JVM tests. The one real overlap is documentary — both edit `specs/backlog.md`, and
013 also edits `specs/005-android-preference-learning/evidence.md`, which 006's branch has already
touched. **006 should expect a conflict in those two files and keep both sides**, as `execution-model.md`
prescribes for rolling documents.

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

### ~~006 — Deck diversity sequencing~~  ·  **Shipped**

The −8 same-source and −5 third-consecutive-category penalties (`js/ranking/deck.js:26-38`), ported as a
greedy per-step algorithm rather than a sort key. One slice, 76 lines of production code, 258 → 275 tests.

**It has no reader-visible effect at today's surface, and its walkthrough says so at every step.** The
claim that the Android head article differed from the browser's until this landed was **wrong** — item
005 closed that gap by itself, because `penaltiesFor` reads `selected.at(-1)` and the first selection
step carries no penalty. Recorded in 005's `design.md` D12, this item's `spec.md` §1.1, and
`waves/wave-c-note.md` §4 so it stops being re-inherited. It shipped anyway, on the owner's decision of
2026-08-26, as a genuine spec-parity gap.

Its real cost was three plan defects — two of them supervisor arithmetic and scope errors caught by the
implementer refusing to build a contradiction, one a vacuous assertion caught at review. None reached
shipped code. The transferable lesson is in `waves/wave-c-note.md` §2: **assert a comparator against the
comparator, not through the algorithm that consumes it.**

*Evidence:* `specs/006-android-deck-diversity/evidence.md`. *Deferred by 002 §3, restated by 004 §3.*

### 014 — The Discover card's buttons in the Undo window  ·  *unscheduled*

**Read Later / Save for later and the other card buttons fail in the same window item 013 fixed for
swipes, and they never consult the gesture state** — so nothing 013 did addresses them, and no fix there
would have. Split out deliberately as a scope decision rather than a deferral of difficulty
(`specs/013-android-undo-gesture-reset/design.md` D7).

**It inherits no cause.** 013's mechanism is an ancestor scroll consuming the pointer DOWN before
`ArticleCard`'s gesture handler can adopt it. A `Button` is not a `pointerInput` gesture and does not use
`awaitFirstDown`, so whatever is wrong with the buttons is its own mechanism and needs its own
reproduction. **Do not start from 013's diagnosis** — that assumption is what cost 013 two of its four
passes.

Worth knowing before designing it: `DiscoverScreen.kt:74-87`'s article-change `animateScrollTo` runs for
roughly 380 ms after the head article changes, and it is what claims the pointer in 013's case. Whether a
`Button` inside a scrolling ancestor loses its click the same way is the first thing to measure.

**New observation from 006's walkthrough, 2026-08-31 — start here, not from 014's wording above.** On
both the 006 build and the pre-006 build, the **Save for later button commits the save but raises no Undo
offer at all**: screencaps at 0.2 s, 1.0 s and 2.0 s show no toast, while the swipe path raises one at
0.35 s. That is not "the button fails" — the record is written and Read Later increments. The button
**succeeds and never offers the reversal** that `ArticleStateMachine.reversibleActions` already makes
available for `SAVE`. Whether that is the same defect as the one described above is an open question this
item must answer, not assume. See `specs/006-android-deck-diversity/evidence.md` §5 step 5.

*Owner decision, 2026-08-27. `specs/013-android-undo-gesture-reset/spec.md` §1.8.*

### 015 — A swipe immediately after Undo is attributed to the outgoing article  ·  *unscheduled*

**Found by 013's slice 1, out of its scope, and left deliberately unfixed.** At a very short delay after
Undo — under roughly one frame — the pointer DOWN is adopted against the article that was *leaving*, not
the one Undo restored. `awaitFirstDown RETURNED article=8c80f6f9…` fires **22 ms before** the restored
card composes, and the state document records the wrong source. Three runs at delay 0 gave `ietf_oauth`,
`ietf_oauth`, `science_aaas` — a race, not a constant.

The reader sees a card, swipes it, and trains a preference for a different article.

**This is a publish-ordering defect, not a consumption one**, so 013's `requireUnconsumed` fix neither
addresses it nor makes it worse — verified unchanged in 013's walkthrough. It is outside 013's `spec.md`
§4 scenarios, and 013's slice-2 brief explicitly barred the implementer from absorbing it.

It is also **why 013 spent a pass chasing a mystery that did not exist**: scored by "did a weight move",
these runs counted as passes and made the failure window look like a band open on both sides. Any item
that touches this ground must score by **which** article moved.

*Raised by `specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md` §2, 2026-08-28.*

### 016 — Undo in Read Later and History  ·  *unscheduled*

Owed since item 013's first design pass and not yet written down anywhere but that pass. Widening
`ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS`, and adding undo affordances to the
Read Later and History panes so an action taken there can be taken back the way a Discover action can.

**Needs its own amendment and design pass, like item 012** — it changes what is reversible, and
`contracts.md` §23 ties Undo to the `signalsApplied` reversal guard that item 005 introduced. That
coupling is the reason 007 led the programme, and it is the reason this cannot be bolted on.

*Owed from `specs/013-android-undo-gesture-reset` design pass 1; recorded at 013's close, 2026-08-28.*

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

- **`DiscoverScreen` carries three scroll effects, and one of them was implicated in a real defect.**
  The reset on category and state-class change, D11's scroll-to-end when an article becomes opened, and
  D12's scroll-the-card-into-view when the deck advances. **This is no longer a tidiness note.** Item 013
  proved that D12's `animateScrollTo` — roughly 380 ms of ancestor scroll after every head-article change,
  including the Undo restore — consumes the pointer DOWN in the Initial pass, which silently ate every
  swipe that landed inside it. 013 fixed it at the card (`requireUnconsumed = false`) rather than by
  touching the scroll, deliberately: the scroll is behaving correctly and it is item 012's ground.

  **Item 012 should know this before it moves the header.** 012 largely subsumes D12 — with the
  operational block below the card there is little left for it to correct — so the reconciliation is
  likelier to be a *deletion* than a merge. If D12 goes, the window 013 fixed stops occurring at all, and
  013's fix becomes belt-and-braces rather than the only thing holding. Do not read that as licence to
  revert it. (*008 D11/D12; `specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md`*)
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
  fixed, so it is worth one more pass. **Still open after 013**, and now more clearly worth doing: 013
  changed when a gesture is *adopted*, not how it animates, but it is the third item in a row to touch
  the swipe surface.

**Added by 013, 2026-08-28:**

- **The instrumented suite is now three classes and four tests, and still out of CI** (see Parked, *002
  slice 4*). `MainActivityLaunchSmokeTest`, `ArticleCardGestureTest` (gesture across a head-article
  change) and `ArticleCardScrollGestureTest` (two tests: a horizontal swipe is heard through a running
  ancestor scroll; a vertical drag still belongs to the scroll). **A local
  `connectedDebugAndroidTest` run is the only thing that exercises any of them** — nothing in CI will
  notice if all four break. That is a deliberate decision, but the exposure grows with each item that
  parks a guard here, and 013's whole history is defects a green JVM gate could not see.

- **A residual Undo-window measurement, recorded as a measurement and not as intended behaviour.** After
  013's fix every delay from 0.05 s to 1.2 s commits against the restored article. At a nominal delay of
  **0** the swipe still lands on the *outgoing* article — that is item **015**, a real defect with its own
  entry, not a tolerance. Nothing here refuses input at any delay (`013 design.md` D9, D11); there is no
  window in which the card declines a touch.
