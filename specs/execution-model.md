# Execution model — how the backlog gets run

Companion to `backlog.md`. The backlog says *what* is left; this says *how* items 005–011 are executed
without running them one at a time from end to end, and without pretending they are independent when
they are not.

Written 2026-08-25, after 004 merged. Amend it when a wave finishes and it turns out to have been wrong.

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
| Android | `./gradlew :app:testDebugUnitTest` | from `android/`; 135 tests at 004 |
| Android | `./gradlew :app:assembleDebug` | |
| Web | `npm test` | 105/105 at 004 |
| Web | `python -m pytest`, `python -m pipeline.main --validate-config` | web/pipeline items only |

CI is path-filtered: `android.yml` fires on `android/**`, `test.yml` on the web and pipeline paths. Item
011 touches both trees and will fire both. Hosted CI must be green on the exact final head before a final
review merges — that requirement is not waived by any wave arrangement.
