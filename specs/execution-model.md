# Execution model — how the backlog gets run

Companion to `backlog.md`. The backlog says *what* is left; this says *how* items 005–011 are executed
without running them one at a time from end to end, and without pretending they are independent when
they are not.

Written 2026-08-25, after 004 merged. Amend it when a wave finishes and it turns out to have been wrong.

**Reviewed 2026-08-26, after wave B.** Two waves have now run under it and neither contradicted it. §2's
hub-file matrix predicted wave B's only two contact points exactly, and §4's requirement to re-run both
gates on the rebased head — with pre-rebase numbers discarded — caught a cross-item defect that both
branches' green gates and both PRs' green hosted CI had missed
(`waves/wave-b-note.md` §3). One thing wave B showed the model does **not** say, added below as §9.

---

## 1. The shape, in one paragraph

Work is organised into three **waves**. Waves run **sequentially**, one Claude session each. Items
**inside** a wave run **concurrently**, one implementer session and one branch each. That is the whole
model. The concurrency lives inside a wave, never across waves, and the reason is in §2.

---

## 2. Why waves cannot overlap

They share hub files, and two implementers rewriting the same function on two branches produce a merge
no reviewer should be asked to sign.

| Hub file | 005 | 006 | 007 | 008 | 009 | 010 | 011 |
|---|---|---|---|---|---|---|---|
| `ui/AppViewModel.kt` | ● | | ● | | ● | | |
| `domain/state/ArticleStateMachine.kt` | ● | | ● | | | | |
| `domain/state/DiscoverDeck.kt` | ● | ● | | | | | |
| `ui/screens/discover/**`, `ui/components/ArticleCard.kt` | | | ● | ● | | | |
| `ui/screens/settings/SettingsSheet.kt`, `data/local/state/**` | | | | | ● | | |
| `res/values/themes.xml`, manifest, `MainActivity.kt` | | | | | | ● | |
| `js/**`, `tests/js/**` | | | | | | | ● |

Read down the columns and the waves fall out on their own:

- **Wave A — 011, 010, 007.** Three disjoint columns. Nothing to coordinate.
- **Wave B — 008, 009.** Disjoint from each other. Both need 007 merged: 008 because a swipe without
  Undo is worse than no swipe, 009 because it edits `AppViewModel` after 007 has.
- **Wave C — 005, then 006.** 005 collides with every other Android item, so it runs alone. 006 shares
  `DiscoverDeck.kt` with it and follows immediately, or lands as 005's last slice.

**007 leads the whole programme deliberately.** `contracts.md` §23 ties Undo Not Interested and Undo
Save to the `signalsApplied` reversal guard that 005 introduces. While `preferences` is still always
empty, Undo is pure state-machine inversion with no weight arithmetic to get wrong. After 005 it is the
hardest item in the backlog. Order is not a preference here.

**Corollary: three concurrent wave managers would not work.** Two of the three would sit blocked on a
merge. The dependency graph is what it is.

### 2.1 The matrix orders files by who writes them, and that is not enough

**Amended at wave D's close, 2026-08-31, after the same gap failed three items across three waves.**

The matrix above answers one question: *which items write this file.* That is sufficient for deciding
which **waves** can run concurrently, which is what it was built for. It is **not** sufficient for drawing
a **slice plan**, and slice plans have been borrowing its shape and inheriting its blind spot.

A file has three kinds of dependent, and the matrix models only the first:

| Edge | The question | What breaks when it is missed |
|---|---|---|
| **Writes** | Who edits this file? | Two implementers produce an unmergeable diff. **Modelled.** |
| **Asserts** | Which test files make claims about this file's behaviour? | A plan freezes an assertion its predecessor made unfreezable, or splits a signature change away from the 28 call sites that pass it. |
| **Receives** | What consumes this file's output at runtime? | Widening what a producer emits sends values into a consumer that rejects them — in another file, in another slice. |

**The three failures, one root each costume:**

1. **Item 006 — an assertion edge.** Item 005 asserted `contracts.md` §58's ordering *through*
   `DiscoverDeck.build()`. Item 006 exists to reorder that list, and its plan then froze every existing
   assertion. Unsatisfiable, and it surfaced at dispatch — weeks after plan-mode approval.
   (`waves/wave-c-note.md` §2, §7.)
2. **Item 014 — a compile edge.** Slice 1 removed a parameter from `AppViewModel` while excluding
   `AppViewModelTest.kt` and its 28 calls. The slice could not have built its own test sources.
   (`014/evidence.md` §7.)
3. **Item 016 — a runtime edge.** Slice 1 widened `reversibleActions`, so the domain began producing undo
   records for three more actions. Those records reached `AppViewModel.raiseUndoOffer`'s
   `error("Only Save and Dismiss can raise an Undo offer")` — a different file, owned by slice 2. Measured:
   **292 tests, 7 failures**, every one that exception. The same item also carried a slice 2 → slice 3
   compile edge that the plan **documented and mislabelled as slice 3's failing-first evidence**.
   (`016/evidence.md` §5.1.)

**None of the three was caught by a gate or by reading a diff. All three were caught by an implementer
refusing to build a contradiction**, two of them only after the work was done and measured.

#### The rules that follow

1. **Every hub-file row in a slice plan carries three lists, not one:** who writes it, which test files
   assert against it, and what receives its output at runtime. A signature or type change must be assumed
   to reach every file on lists two and three.
2. **A slice boundary is valid only if the slice can reach a green gate on its own.** State how, in one
   sentence, when the plan is written. If you cannot state it, the boundary is wrong — that single check
   would have caught items 014 and 016 at design time rather than at dispatch.
3. **A slice's failing-first evidence must be its own tests failing for the intended reason — never the
   next slice's compile error.** "This slice breaks the build until the next one lands" is a report that
   the cut is wrong, not a description of RED.
4. **When a change widens what a producer emits, land the consumers first.** Extending an enum, a set, or a
   returned type is safe while nothing produces the new values, so the consumer slice is green on the old
   tree and the producer slice turns the whole thing on. This is what unblocked item 016, and it is the
   general shape.
5. **An assertion enumeration expires when the item beneath it merges.** `design.md` D5 lists are accurate
   when written and stale when used; no gate distinguishes the two. Name cases with reasons, never freeze
   them, and require the implementer to report an unlisted failure before editing it. That protocol caught
   item 016's stale row at preflight, before a single file was edited.

---

## 3. Numbers, branches, directories

Allocated here, once, so two concurrent `/feature-design` sessions cannot both reach for 005. This
supersedes `future-items.md`'s "numbers are allocated at design time" for these seven only.

| # | Slug | Branch | Wave |
|---|---|---|---|
| 005 | `005-android-preference-learning` | `feat/005-android-preference-learning` | C |
| 006 | `006-android-deck-diversity` | `feat/006-android-deck-diversity` | C |
| 007 | `007-android-undo` | `feat/007-android-undo` | A |
| 008 | `008-android-swipe-gestures` | `feat/008-android-swipe-gestures` | B |
| 009 | `009-android-import-export` | `feat/009-android-import-export` | B |
| 010 | `010-android-launch-theme` | `feat/010-android-launch-theme` | A |
| 011 | `011-web-validator-parity` | `feat/011-web-validator-parity` | A |

Every branch is cut from `main` and its PR targets `main`. `integration/v1` is stale; it is not the
target.

---

## 4. What a wave session does, in order

1. **Design all of the wave's items first, in one session, before any implementation starts.** Run
   `/feature-design` per item back to back. It is cheap relative to implementation, and doing it in one
   head is what keeps two items from quietly claiming the same file. If a design reveals an overlap this
   document missed, fix the wave here — that is far cheaper than discovering it at merge.
2. **Dispatch implementation concurrently.** One item, one branch, one git worktree, one herdr/Codex
   session. Worktrees are not optional with concurrent items: two sessions in one checkout will corrupt
   each other's build outputs.
3. **Review slice gates as they land**, in arrival order, not in item order. `/feature-review` in slice
   mode. Reproduce each RED and each gate with `--rerun-tasks` in a throwaway worktree; do not accept an
   implementer's report. This is the established standard from 002–004 and concurrency does not relax it.
4. **Merge in the wave's stated order** as each item's final review passes, then tell the other live
   branches to rebase and re-run their gates. Expect this; it is the price of the wave.
5. **Batch the walkthroughs at the end of the wave**, once, against merged `main`. See §6.
6. **Write `evidence.md` per item, and a wave note** recording what the concurrency actually cost.

**Slices inside an item stay strictly sequential.** `/feature-implementation` verifies a failing-first
commit per slice; that is not parallelisable and must not be attempted.

---

## 5. Who reviews

Unchanged from 002–004: **the implementer (Codex) writes all product and test code; Claude writes the
specification, design note, slice plan, and evidence, and reviews.** Claude never writes product code
here. The reviewer authoring the spec is the established pattern and is not an independence problem —
the reviewer not having written the code is.

The consequence for concurrency is a hard one: **the review gate is single-threaded through the wave
session.** Three concurrent items means three interleaved gate streams in one context. That is the real
ceiling, and it is why waves cap at three items rather than at however many columns happen to be
disjoint.

**Fallback.** If a wave session's context gets heavy, or two items start colliding in review, drop that
wave to sequential dispatch. Each wave brief states its sequential order for exactly this case. Losing
the concurrency is a cost; losing review quality is a defect.

### 5.1 Reviewing concurrent items without corrupting the review

Concurrent implementation is approved. The risk it introduces is **not** the number of items; it is the
reviewer working from memory of a tree it read on another branch. That produces a false PASS, which is
the only review error that costs anything — a false finding merely wastes a round trip.

Two named failure modes:

- **Cross-branch recall.** "This matches the browser at `js/state/article-state.js:154`" written from an
  hour-old read on a different worktree.
- **Gate-number crossing.** Three `--rerun-tasks` runs produce three test counts; the wrong one lands in
  the wrong `evidence.md`.

Four controls, which are the 002–004 standard made explicit rather than anything new:

1. **One git worktree per item, and review only from that worktree.** Not optional for the implementer
   either — two Codex sessions in one checkout will trash each other's Gradle outputs.
2. **Never review two items in one reasoning pass.** Finish one gate, write its evidence, then pick up
   the next. Slice gates are handled in arrival order, never batched.
3. **Every `file:line` in a review comes from a fresh read**, never from recall. This is already the
   house style; under concurrency it is also the anti-contamination control.
4. **Gate numbers are recorded into `evidence.md` at the moment of the run**, not reconstructed later.

**Reviewer attention is not distributed evenly across a wave, and the waves were built that way.** Wave A
is one hard review (007) plus two mechanical ones (010 is emulator-visual; 011 is a JS diff plus
`npm test`). Wave C is one review larger than all of wave A combined, which is the second reason 005 runs
alone.

**Per-item review subagents were considered and declined.** They would solve contamination by reading
from scratch, but 002–004's reviews were sharp because the reviewer had authored the spec and knew which
browser lines the port had to match — a fresh reviewer checks the diff against the spec text, not against
the reasoning behind it. It also reintroduces exactly the trust problem this standard exists to kill:
accepting a subagent's report of a gate run instead of reproducing it.

---

## 6. Walkthroughs

**The orchestrator drives the emulator over `adb`**, and the owner is asked only for what `adb` cannot
settle — a visual judgment, a device the emulator does not model, or an authored copy decision.

This reverses the earlier assumption that walkthroughs are handed to the owner as a checklist. Item 004
settled it: the orchestrator drove the walkthrough directly and it caught a disclosure defect that all
135 JVM tests missed. A checklist handed over would have found it later or not at all.

Walkthroughs serialize on one AVD regardless, so they are batched at the end of a wave rather than run
per item. That is a scheduling detail, not a weakening: **a green JVM suite is not evidence that an
Android app runs** (002's strongest recorded lesson, from three emulator-only defects).

What the owner is asked for, per wave, is stated in that wave's brief under *Owner checkpoints*.

---

## 7. Handing a wave to a fresh session

Each wave has a self-contained brief in `specs/waves/`. Open a new session, point it at the brief, and it
should not need this conversation's history. If it does, the brief is incomplete — fix the brief.

Read in this order: `AGENTS.md`, `docs/v1/README.md`, `specs/backlog.md`, this file, then the wave brief.

---

## 8. Gates

| Surface | Command | Notes |
|---|---|---|
| Android | `./gradlew :app:testDebugUnitTest` | from `android/`; 343 tests at 018 |
| Android | `./gradlew :app:assembleDebug` | |
| Android | `./gradlew :app:assembleDebugAndroidTest` | **added at 018** — instrumented sources must compile |
| Android | `./gradlew :app:connectedDebugAndroidTest` | **added at 018** — runs on an emulator in CI |
| Web | `npm test` | 105/105 at 004 |
| Web | `python -m pytest`, `python -m pipeline.main --validate-config` | web/pipeline items only |

Hosted CI must be green on the exact final head before a final review merges — that requirement is not
waived by any wave arrangement.

### 8.1 What is actually path-filtered — this section was wrong until 018

**`android.yml` is path-filtered** on `android/**` and its own workflow file. That much was right.

**`test.yml` is not path-filtered at all.** Its trigger is a bare `pull_request:` with no `paths` key, so it
runs on **every** pull request in this repository regardless of what changed, and on every push to `main`.
This section previously claimed it fired "on the web and pipeline paths", which is false and was believed
through waves A to D.

The practical consequence: **seeing `test.yml` green on a documents-only or Android-only PR means nothing
about whether web paths were touched.** Do not read it as a signal. Item 011 remains the only item that
genuinely exercises both trees.

### 8.2 Instrumented tests are gated as of item 018

Before item 018, `android.yml` ran only `testDebugUnitTest` and `assembleDebug`. **Instrumented tests were
neither run nor even compiled**, so an `androidTest` source could rot without any gate noticing, and a
behavioural claim asserted only there was true on the day it was measured and unprotected afterwards.

Item 018 surfaced this by adding a 360 dp layout test whose evidence had exactly that weakness. Both gaps
are now closed: `assembleDebugAndroidTest` compiles the sources in the main job, and a separate
emulator-backed job runs `connectedDebugAndroidTest`.

**This matters most for width claims.** Item 012's 360 dp finding and item 019's §13.2 clamp that closes it
are the kind of thing a JVM test cannot see, and they now have somewhere durable to live.

### 8.3 A width test must establish its width, not inherit it

**The gate found a defect in merged code on its first run**, and it is the defect every width test in this
programme is liable to.

Item 018's `CategoryChipRowLayoutTest` set `Modifier.width(360.dp)` and asserted the row was 360 dp wide. A
Compose root cannot exceed the device, so on the runner's 320 dp default AVD the row was clamped and the
test failed `expected:<360.0> but was:<320.0>`. **It had been asserting a width it hoped for rather than one
it established**, and passed locally only because the device happened to be wide enough.

The fix is `DeviceConfigurationOverride.ForcedSize`, which scales density so the composition is laid out at a
size the test chooses — and it genuinely can present a 360 dp composition on a 320 dp screen.

**The trap inside the fix, which cost a second round:** `ForcedSize` changes the density, so every
`dp.toPx()` baseline must be computed **inside** the override. Computed outside, the expected and actual
values sit in different density spaces and the test fails with an inscrutable pixel mismatch — `1215.0`
against `1080.0`, where `1215 = 360 × 3.375` is the *device* density.

**Verify a width test at more than one width before trusting it.** The corrected test was confirmed passing
at 320, 411 and 480 dp. A single passing run on the developer's own device proves only that that device is
wide enough.

---

## 9. Verification is wider than review

Added 2026-08-26, from wave B. §5 says the review gate is the ceiling. That is true and it is not enough,
because it describes only the half of verification that reads diffs.

Wave B's four most valuable findings did not come from reading a diff:

| Finding | Found by |
|---|---|
| Mark read hidden after returning from the publisher | the owner, using the app |
| The deck advancing returned the screen to the top | the owner, using the app |
| Import left a live Undo offer whose action could not succeed | re-gating the rebased head |
| A refused import told the reader nothing | driving the emulator, not reading its screenshots |

The first two are the uncomfortable ones: the reviewer had **walked past both** during walkthroughs that
otherwise passed. Every check being run was *did the state change correctly* — records written, counts
updated, announcements raised. Neither defect is a state question; both are *is the right thing in front
of the reader*, and no step asked it.

**So each walkthrough step now carries a second question after its assertion:** *and is what the reader
needs next actually on screen?* It costs nothing to ask and it is the only thing that would have caught
either defect.

**And a walkthrough is not a screenshot audit.** Screenshots were being read to confirm assertions rather
than looked at the way a reader looks at a screen. Where a step's outcome is visual, describe what a
reader would see before checking whether the assertion holds.
