# 006 — slice plan

Sized **S → 1 slice**. One item branch (`feat/006-android-deck-diversity`), one PR targeting `main`. The
slice closes as a failing-first test commit plus an implementation commit.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`;
tests live under `android/app/src/test/kotlin/io/irodriguez/intentionalreading/`.

**Every Gradle invocation needs both of these exported first** — `java` is not on this machine's `PATH`,
and a worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

**Done, on 2026-08-31.** Item 005 is merged. `main` (`f06a32b`) was **merged** into this branch as
`c27d87e` — merged, not rebased, because `AGENTS.md` requires existing history be preserved — and both
gates were re-run on that head. None of the documentary conflicts the wave brief predicted materialised.

**The baseline for this item is that run, and nothing carried over from any 005 slice report**
(`waves/wave-b-note.md` §7):

- `:app:testDebugUnitTest` — **258 tests, 0 failures, 0 errors, 0 skipped**
- `:app:assembleDebug` — BUILD SUCCESSFUL
- `npm test` — **114 pass, 0 fail** (the `spec.md` §5.1 parity cross-check; any movement here means
  `js/**` was touched, which is out of scope)

Fixed for the slice — do not re-decide these mid-implementation:

- **−8 and −5, and nothing else.** No third rule, no tuning, no category penalty in a category view
  (`05-personalization-state.md` §§59–60).
- **The penalties are recomputed at every selection step** from the last two selected cards. Folding them
  into a single sort key produces different output and is a defect (`design.md` D1).
- **Same-source compares against the previously selected card only.** Not any earlier card, not a count
  (`design.md` D5).
- **The all view is `selectedCategory == null`.** There is no `Category.ALL` in the model; inventing one
  is out of scope (`design.md` D5).
- **The category penalty needs two already-selected cards**, and both must match the candidate's
  category.
- **Both penalties are additive** — a candidate can carry −13 — and **neither is a prohibition**.
- **The head card must not move.** Asserted by a test, not assumed (`design.md` D3).
- **Penalties live on the candidate wrapper**, never on `Article` or on any record; nothing is persisted
  (§61).
- **The initial §58 sort stays.** The greedy loop's tie-breaks depend on it.
- **No caching, no memoisation, no background dispatch** for the deck build (`design.md` D4).
- **Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*`.**
- **Exactly one existing test may be modified, and only in one way** (`design.md` D8).
  `DiscoverDeckTest.kt`'s *"candidate order follows all five keys with a deliberate collision at each
  key"* is retargeted from `DiscoverDeck.build(...).candidates` at the `candidateComparator` directly.
  **Its fixture, its eight expected IDs, their order, and its name all stay exactly as 005 left them** —
  only what is sorted changes. This is not licence to adjust an expectation that fails: it is the one
  edit this item's own scope makes unavoidable, and D8 is the reasoning.
- **Every other existing assertion stays frozen** — in that file, in `PersonalizedScoreTest.kt`, in
  `ArticleStateMachineUndoTest.kt`, everywhere. Nothing else may be edited, deleted, weakened, skipped,
  or absorbed. Any further pressure to change one is a report to the supervisor, not a decision
  (`waves/wave-b-note.md` §1).
- **No new dependency**, and no Compose file touched.
- Everything outside `android/` is untouched, except this item's own `specs/006-*/` documents.
- **Escalate rather than infer.** If an expected order in the scenarios below looks wrong against the
  browser's own tests, say so before implementing it — wave B's two cheapest fixes were arithmetic
  errors in the reviewer's plan, caught before any code was written (`waves/wave-b-note.md` §4).

---

## Slice 1: greedy sequencing in `DiscoverDeck`

- **Scenarios:** `spec.md` §4 in full — §4.1 same-source, §4.2 category, §4.3 composition with 005's
  weights, §4.4 purity and determinism.
- **Files:**
  - `«pkg»/domain/state/DiscoverDeck.kt` — the `DeckSequencing` type, the per-step penalty computation,
    the greedy selection loop, and `candidateComparator` changing from `private val` to `internal val`
    (`design.md` D8). `internal` is module-scoped and `:app`'s unit-test source set already has friend
    access, so this exposes the comparator to tests without widening the public API.
  - `domain/state/DiscoverDeckSequencingTest.kt` — a **new** test file, so the bulk of this item's
    coverage is added rather than edited.
  - `domain/state/DiscoverDeckTest.kt` — **one authorized edit only**, the retargeting described in the
    fixed-constraints list above. Every other test in the file is untouched.
- **Must not touch:** `«pkg»/domain/ranking/**`, `«pkg»/domain/state/PreferenceLearning.kt`,
  `«pkg»/domain/state/PreferenceReconciliation.kt`, `«pkg»/domain/state/ArticleStateMachine.kt`,
  `«pkg»/ui/**`, `«pkg»/data/**`, `res/**`, any Gradle file.
- **Reuse:** 005's `PersonalizedScore` and its §58 comparator exactly as they stand — the sequenced
  comparator falls through to it and must not restate its five keys. 005's `DeckCandidate`,
  `DiscoverDeckState`, `isEligible`, and the held-card pin are all unchanged; this slice adds one field
  and one loop.
- **Reference — the browser's tests are the fixtures, and their expected orders are ported as literal
  expectations:**
  - `js/ranking/deck.js:26-38` — `penaltiesFor`, the whole of the penalty logic;
  - `js/ranking/deck.js:40-45` — `compareSequenced`, including the fall-through;
  - `js/ranking/deck.js:47-72` — `buildDeck`, the greedy loop;
  - `tests/js/ranking.test.js:92-107` — the same-source cases, expected order **A, B, A**, and the
    stronger-candidate case with `sameSourcePenalty === -8`;
  - `tests/js/ranking.test.js:109-130` — the all-view case, expected order **1, 2, 4, 3**; the
    category-view case, expected order **1, 2, 3** with every `categoryPenalty === 0`; and the
    stronger-third case with `categoryPenalty === -5`;
  - `tests/js/ranking.test.js:132-143` — determinism and mutation-freedom.
- **Implementation shape — decided, not offered** (`design.md` D2):
  - the initial candidate list is 005's §58-sorted list;
  - each step computes `sameSourcePenalty` and `categoryPenalty` per remaining candidate from
    `selected.lastOrNull()` and the one before it;
  - the winner is `remaining.minWithOrNull(sequencedComparator)`, where the comparator orders by
    sequencing score descending and falls through to 005's §58 comparator — provably the same element the
    browser's per-step sort would put at index 0, since both comparators are total;
  - the winner is removed from `remaining` and appended to `selected`, carrying its `DeckSequencing`.
- **Definition of done:**
  - Both gates green, and `npm test` green and unchanged (`spec.md` §5.1).
  - A test asserting the **A, B, A** order and that the third card's same-source penalty is 0.
  - A test asserting a candidate stronger by more than 8 stays second and records `-8.0`.
  - A test asserting the penalty ignores cards earlier than the previous one.
  - A test asserting the **1, 2, 4, 3** all-view order.
  - A test asserting a category view produces the personalized order with every category penalty 0.
  - A test asserting a third-consecutive candidate stronger by more than 5 stays third and records
    `-5.0`.
  - A test asserting two consecutive same-category cards are not penalized, and one asserting a
    two-candidate deck is not penalized.
  - A test asserting a candidate can carry both penalties, at `-13.0`.
  - **A test asserting the head card is identical to the unsequenced order's first element**, on a deck
    whose later positions demonstrably reorder (`design.md` D3).
  - A test asserting the sequencing score is the personalized total minus the penalties, while the
    candidate's `PersonalizedScore` and its four components are unchanged.
  - A test asserting a tie after penalties resolves by §58's five keys.
  - A test asserting `articles`, `records`, and `preferences` are equal by value before and after a
    build, and that no local-state write occurs.
  - A test asserting two builds of identical inputs are equal candidate for candidate, including
    penalties.
  - A test asserting the deck is a permutation — same articles, each once — and that `availableCount` and
    `remainingCount` are unchanged from 005's values.
  - A test asserting a held card whose sequenced position is not first is still the card returned.
  - **005's five-key assertion retargeted at `candidateComparator` and still green**, asserting the same
    eight IDs in the same order against the same fixture (`design.md` D8).
  - **A test asserting that same fixture's sequenced order**, which ends `sourceAFirst, sourceB,
    sourceASecond` — so the coverage the retargeting moved off `build()` is replaced, not dropped. Net
    assertions go up. **`sourceASecond` records `sameSourcePenalty == 0.0`**: the −8 decides step 7,
    where it loses to `sourceB`; by step 8 the previous card is `sourceB` and D1 recomputes from
    scratch. The order is the proof the penalty fired. Same shape as `spec.md` §4.1's *"the third card's
    same-source penalty is 0"* (`design.md` D8).
  - No other existing assertion edited, deleted, weakened, or absorbed.
- **Status:** pending

---

## Ship bookkeeping this item creates

Handled at wave close, not inside the slice:

- `evidence.md` for this item, gate numbers recorded at the moment of the run
  (`execution-model.md` §5.1).
- `spec.md` §5.2's regression walkthrough against merged `main`, with the honest note that no step
  demonstrates a penalty.
- `backlog.md`: 006 moves to Shipped, the wave C row closes, and the queue is left holding only item 012
  and the deliberately parked entries.
- `waves/wave-c-note.md` — what wave C actually cost, per `execution-model.md` §4.6, including whether
  005's sequential-alone dispatch was worth what it was predicted to be.
- **A recorded correction:** `waves/wave-c.md`'s claim that this item closes the head-article gap is
  wrong, and 005's `design.md` D12 and this item's `spec.md` §1.1 hold the reason. The wave note carries
  it forward so the next planner does not re-inherit it.
