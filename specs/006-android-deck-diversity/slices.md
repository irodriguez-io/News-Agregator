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

**Before slice 1 starts:** item 005 must be merged, this branch rebased onto merged `main`, and both
gates run on the rebased head. The baseline for this item is that run's count — read from the
`BUILD SUCCESSFUL` run, not carried over from any 005 slice report (`waves/wave-b-note.md` §7).

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
- **No existing assertion may be edited, deleted, weakened, or absorbed.** 005's deck and score tests
  stay exactly as 005 left them; this slice only adds. Any pressure to change one is a report to the
  supervisor, not a decision (`waves/wave-b-note.md` §1).
- **No new dependency**, and no Compose file touched.
- Everything outside `android/` is untouched, except this item's own `specs/006-*/` documents.
- **Escalate rather than infer.** If an expected order in the scenarios below looks wrong against the
  browser's own tests, say so before implementing it — wave B's two cheapest fixes were arithmetic
  errors in the reviewer's plan, caught before any code was written (`waves/wave-b-note.md` §4).

---

## Slice 1: greedy sequencing in `DiscoverDeck`

- **Scenarios:** `spec.md` §4 in full — §4.1 same-source, §4.2 category, §4.3 composition with 005's
  weights, §4.4 purity and determinism.
- **Files:** `«pkg»/domain/state/DiscoverDeck.kt` (the `DeckSequencing` type, the per-step penalty
  computation, the greedy selection loop), and
  `domain/state/DiscoverDeckSequencingTest.kt` — a **new** test file, so that 005's
  `DiscoverDeckTest.kt` stays untouched and the review diff shows added coverage rather than edited
  coverage.
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
  - No existing assertion edited.
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
