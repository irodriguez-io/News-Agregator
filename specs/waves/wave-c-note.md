# Wave C — what it actually cost

Written at 006's close, per `execution-model.md` §4.6. Wave C was two items: **005 preference learning**,
then **006 deck diversity sequencing**. This note scores the wave's predictions against what happened.

**Scope of this note.** 005 was designed, dispatched and closed in earlier sessions; its own numbers are
in `specs/005-android-preference-learning/evidence.md` and are not restated or re-scored here. What
follows is written from 006's run and from what the repository records. Where a judgement needs data only
005's sessions had, it says so instead of inventing it.

## 1. The wave's central prediction about 006 was wrong, and wrong in a useful way

`wave-c.md` says of 006: *"Small, well-specified, and fully JVM-testable — the browser's numbers are the
specification"*, and *"006 is small enough that the sequencing costs almost nothing."*

The first half held exactly. The production diff is **76 lines in one file**, the browser's tests were
the fixtures, and every expected order was ported as a literal rather than re-derived.

The second half did not. **006 took four implementer dispatches to produce two commits of code**, and
three of those dispatches ended in a plan defect rather than an implementation. None of the three had
anything to do with the penalties, the greedy loop, or the browser. **All three came from the seam
between 006 and 005** — the thing the wave ordered these items to manage, and then assumed was free.

The sequencing cost was real. It just did not land where the brief expected it: not in 005's dispatch,
and not in the code, but in **006's plan colliding with 005's tests**.

## 2. The seam that cost the wave: an assertion routed through the wrong thing

This is the transferable lesson of wave C and the reason to read this note.

Item 005 asserted §58's five-key comparator by calling `DiscoverDeck.build(...)` and checking the order
of `deck.candidates`. Entirely reasonable when nothing reordered that list. Item 006 exists to reorder
that list. Its slice plan then froze every existing assertion — also reasonable in isolation — and the
two rules together were unsatisfiable.

**The browser never had this problem**: `tests/js/ranking.test.js:60-74` asserts `compareCandidates`
directly on a sorted array, and `:92-143` asserts `buildDeck` separately. The Android port collapsed two
concerns into one assertion, and the next item paid for it.

> **Assert a comparator against the comparator, not through the algorithm that consumes it.** An
> assertion routed through an algorithm that a later item reorders will be frozen by that item's plan and
> block it — and the block surfaces at dispatch, after the design pass is already approved.

Resolved by `006/design.md` D8: `candidateComparator` became `internal`, 005's assertion now sorts with
it directly — same fixture, same eight IDs, same order, same name — and 006 added a test for that
fixture's sequenced order so nothing was traded away. Net assertions went up.

## 3. Three plan defects, none of which reached shipped code

| # | Defect | Caught by | Cost |
| --- | --- | --- | --- |
| 1 | The freeze rule and `spec.md` §4.1 were unsatisfiable together | implementer, dispatch 1 | one dispatch, no code written |
| 2 | The amendment asserted `sameSourcePenalty == -8.0` where the algorithm records `0.0` | implementer, dispatch 2 | one dispatch, no code written |
| 3 | A variable declared `0`, never touched, asserted equal to `0`, standing in for a DoD bullet | slice review | one follow-up dispatch |

**Defects 1 and 2 were the supervisor's, not the implementer's**, and both were caught because the
implementer refused to build a contradiction and reported instead. `wave-b-note.md` §4 said the
escalations were the right ones; wave C is the second consecutive wave where that held, and the first
where **the escalation was against the plan the supervisor had just written and had approved in plan
mode**.

Defect 2 is worth naming precisely because it was cheap and stupid: the corrected value was **already
written down** in `spec.md` §4.1 — *"the third card's same-source penalty is 0"* — two files from the
line that contradicted it. `wave-b-note.md` §4's finding, that the cheapest fixes of the wave were
arithmetic errors in the reviewer's plan caught before code, repeats here verbatim.

Defect 3 is a different animal and the one worth carrying furthest. A vacuous assertion is not a weak
check — **it is zero check wearing the costume of one**, and it survives review by looking like coverage.
013's close put it as *"a check one notch too weak is indistinguishable from a pass"*; this is the
degenerate case. The DoD bullet it pretended to satisfy turned out to be **unassertable at that layer**,
which is the real defect: `DiscoverDeck.build` is a pure function with no writer to observe, so
`spec.md` §4.4's "no local-state write occurs" is enforced by the signature and the no-`android.*`
invariant, not by any test. A DoD that demands the unassertable invites exactly this.

## 4. The correction this wave owes the next planner

**`wave-c.md` says of 006: *"Until this lands, the Android head article can differ from the browser's for
the same dataset."* That is wrong, and it was already known to be wrong before 006 was implemented.**

`penaltiesFor` reads `selected.at(-1)`, so the first selection step carries no penalty at all; both
clients render exactly one card. The head card is chosen by personalized order alone in both clients, and
**item 005 closed that gap by itself**. The correction is recorded in 005's `design.md` D12, in 006's
`spec.md` §1.1, and now here so it stops being re-inherited.

006 shipped anyway, on the owner's decision of 2026-08-26, as a genuine spec-parity gap **with no
reader-visible effect at today's surface**. Its walkthrough says so at every step, and no step
demonstrates a penalty because none can.

## 5. What the walkthrough found that no gate could

Both findings are pre-existing on `main` and neither is 006's to fix. Both were invisible to 275 green
unit tests.

**The Save for later button commits the save but raises no Undo offer.** Screencaps at 0.2 s, 1.0 s and
2.0 s show no toast; the swipe path raises one at 0.35 s. Reproduces identically on the pre-006 build.
This is **adjacent to item 014 but not what 014 currently describes** — 014 says the card buttons *fail*
inside the Undo window, whereas here the button *succeeds* and simply never offers the reversal that
`ArticleStateMachine.reversibleActions` makes available for `SAVE`. **Item 014's designer should start
from this observation and should not assume the two share a mechanism** — assuming an inherited cause is
what cost item 013 two of its four passes.

**`uiautomator dump` cannot see the Undo toast.** `013/investigation/step0-undo-window.md` §158 already
recorded that tapping Undo from the host after a dump misses the window; wave C confirms the stronger
form — the dump does not observe the toast at all, while a `screencap` at 0.35 s shows it plainly. Any
future item working this ground should drive the whole sequence inside one on-device `adb shell` and
score from `screencap` and the pulled state document, never from a dump.

`wave-b-note.md`'s headline — that the wave's most valuable defects were found by using the app, not by
reading diffs and not by any gate — holds for a third consecutive wave.

## 6. Tooling notes for whoever runs the next wave

- **Codex auto-updates on launch and then exits.** `herdr agent start` reports the agent ready before it
  actually is, the first prompt is swallowed, and the pane is left at a shell prompt with a
  bracketed-paste artifact. Restart the agent and re-send. Budget one wasted dispatch per Codex version
  bump.
- **Card action buttons sit above their text labels.** Tapping the centre of the `Save for later` *text*
  in a `uiautomator` dump misses the button; the clickable node is ~150 px higher. Read the clickable
  node's bounds, not the label's.
- **The category chip row scrolls.** Chip coordinates from one dump go stale after any interaction that
  moves the page. Re-locate before every tap.
- **The item worktree had drifted 46 commits behind `main`** before dispatch — 005's merge and the whole
  of item 013. `/feature-implementation` Step 0 caught it. The merge was clean and **none** of the
  documentary conflicts the wave brief predicted in `backlog.md` or `005/evidence.md` materialised, which
  is worth knowing before someone budgets time for them again.

## 7. Was 005's sequential-alone dispatch worth what it was predicted to be?

`wave-c.md` justified running 005 alone because it touches `AppViewModel.kt`,
`ArticleStateMachine.kt` and `DiscoverDeck.kt` — the three hub files (`execution-model.md` §2). **On the
file-collision criterion the decision was clearly right**, and item 013, which ran outside the waves
during wave C, is the proof: it touched `ArticleCard.kt` and `SwipeGesture.kt` and collided with neither
item, exactly as the collision matrix predicted.

**On cost, this note declines to score it.** 005's dispatch numbers belong to sessions that are not this
one, and inventing a verdict from the repository's shape would be the kind of confident-sounding
inference wave B spent a pass paying for. `005/evidence.md` holds its own record.

What wave C *can* say is that **the collision matrix ordered the code correctly and said nothing about
the tests**, and the tests are where the entire cost of this wave landed. That is the gap in
`execution-model.md` §2 worth closing before wave D: hub files are ordered by who writes them, and
nobody asks who *asserts* against them.

## 8. State at wave close

- **Waves A, B and C are complete.** Every Android parity gap the shipped items deferred is closed.
- The queue holds **012, 014, 015 and 016** only — all four parked on purpose, none blocking.
- 012 and 016 each need a `docs/v1/**` amendment before any design pass; 014 and 015 need their own
  reproductions and must not inherit 013's diagnosis.
