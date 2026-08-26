# 006 — Design note

Decisions for `spec.md`. The browser's implementation and its tests are the specification; where they
answer a question the answer is ported and cited rather than re-derived.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths:
`android/app/src/main/kotlin/io/irodriguez/intentionalreading/domain/state/DiscoverDeck.kt` and its
tests, plus this item's own `specs/006-android-deck-diversity/`. Forbidden: `pipeline/**`, `config/**`,
`js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`.

**Runs after 005, on a branch cut from `main`.** This branch exists now carrying only these three
documents, so the design is committed with the wave's design pass (`execution-model.md` §4). Before its
first implementation slice it **rebases onto merged `main`** — which is where 005's restructured
`DiscoverDeck` lives — and both gates run on the rebased head, with pre-rebase numbers discarded
(`waves/wave-b-note.md` §3). A docs-only branch rebases cleanly; the rebase is bookkeeping here, but the
re-gate is not.

## D1 — The greedy algorithm is ported as an algorithm, not as an ordering rule

The penalties are **not** a sort key that could be folded into §58's comparator. They depend on what has
already been selected, so each selection step re-evaluates every remaining candidate
(`js/ranking/deck.js:47-72`):

```text
remaining = eligible, filtered by category, scored, sorted by §58
selected  = []
while remaining is not empty:
    for each candidate in remaining:
        sequencingScore = personalizedTotal + sameSourcePenalty + categoryPenalty
    winner = the best candidate by (sequencingScore desc, then §58's five keys)
    move winner from remaining to selected
```

An implementation that adds the penalties to the candidate's stored score, or that sorts once with a
penalty term, produces different output and is a defect. The penalties are recomputed from scratch at
every step because `selected.at(-1)` and `.at(-2)` change at every step.

## D2 — `minWith` replaces the browser's per-step sort, and is provably identical

The browser maps and re-sorts the whole remaining list at every step, then takes element 0
(`js/ranking/deck.js:59-68`). Taking the minimum under a **total** comparator is the same element as
sorting by it and reading index 0, and both comparators here are total: `compareSequenced` falls through
to `compareCandidates`, which ends at article ID, which is unique within a dataset
(`04-taxonomy-scoring.md` §11).

**Decision:** compute the per-step penalties into a local value and select with
`remaining.minWithOrNull(sequencedComparator)`. Same output, one pass instead of a sort, and no sorted
copy allocated per step. This is not an optimisation with a behavioural cost to weigh — it is the same
selection expressed without the intermediate list, and it matters because of D4.

The initial §58 sort **stays**: the greedy loop's tie-breaks assume it, and it is also what makes the
first selection step's output equal to 005's head card by construction.

## D3 — The head card cannot move, and a test says so

At the first selection step `selected` is empty, so `previous` is absent and both penalties are 0 for
every candidate. The winner is therefore whichever candidate §58's order already put first — item 005's
head card, unchanged.

**Decision:** this is asserted, not assumed. A test builds a deck whose sequencing demonstrably reorders
positions 2 and 3 and asserts position 1 is byte-identical to the unsequenced order's first element.
That test is this item's honesty check, and it is what pins `spec.md` §1.1 and item 005's `design.md`
D12 in code rather than in prose.

## D4 — The cost, and why it is bounded

The loop is O(n²) comparisons with `minWith`, or O(n² log n) with the browser's per-step sort. **n is
bounded at 500** by V1 retention — 45 days, 40 per source, 500 total (`pipeline/retention.py:1`) — and
Android rebuilds the deck inside `UiStateMapper` on every `publish()`, which runs on
`Dispatchers.Main.immediate`.

Worst case at n = 500: ~125,000 candidate evaluations per rebuild with `minWith`, against ~2.2 million
comparisons if each step sorts. That is the whole of D2's reason.

**Decision:** the faithful greedy port runs synchronously in the mapper, as the deck build already does.
No caching, no memoisation keyed on state, no background dispatch. Each of those is a correctness risk
(§62 requires a rebuild after every eligibility change, an undo, an import, a reset, and a dataset
reload) traded against a cost that is bounded and small. `spec.md` §5.2 step 6 is the check that the
judgement was right, and it is one of the two things the owner is actually asked for.

If step 6 shows a stall, the fix is **not** a cache: it is that only the head card is consumed today, so
the sequenced tail can be computed lazily. That is a follow-up item with a real design question, not a
mid-implementation decision.

## D5 — The exact penalty conditions, with the traps named

Ported from `penaltiesFor` (`js/ranking/deck.js:26-38`):

**Same-source, −8:** applied when the candidate's `source.id` equals **the previously selected card's**
`source.id`. Not any earlier card's. Not a count of how many of that source are already selected.

**Category, −5:** applied when **all** of the following hold:

- the **all** view is selected — on Android that is `selectedCategory == null`, and conflating it with a
  `Category.ALL` value that does not exist in the model is the obvious way to get this wrong;
- at least two cards are already selected;
- the previously selected card's category equals the candidate's category;
- **and** the card before that one also equals the candidate's category.

Both penalties are additive: a candidate can carry −13.

Neither is a prohibition (§59, explicitly). A candidate stronger by more than the penalty still wins, and
two of `spec.md`'s scenarios exist only to prove that the penalty is a subtraction rather than a filter.

## D6 — Penalties are recorded on the candidate, not on the article

`DeckCandidate`, added by 005 as `(article, score)`, gains a third field mirroring the browser's
`sequencing` object:

```kotlin
data class DeckSequencing(
    val score: Double,                 // personalized total + both penalties
    val sameSourcePenalty: Double,     // 0.0 or -8.0
    val categoryPenalty: Double,       // 0.0 or -5.0
)
```

The browser's own test asserts `"sequencing" in articles[0] === false`
(`tests/js/ranking.test.js:142`) — the penalties live on the candidate wrapper and never on the article.
Kotlin's `Article` is a `data class` with no such field, so the type system already enforces what that
test checks; the Android equivalent asserts instead that the input `articles`, `records`, and
`preferences` are equal by value before and after a build (§61; `spec.md` §4.4).

`score` is a `Double` because 005's `PersonalizedScore.total` is; the penalties are whole numbers but
adding an `Int` penalty to a `Double` total and storing an `Int` would be a rounding trap for no benefit.

## D7 — Sizing: one slice

XS/S. One file, one new nested type, one test file, no call-site changes — `UiStateMapper` already
receives what it needs from 005, and `DiscoverDeckState.article` keeps its meaning. The item ships as a
single failing-first test commit plus an implementation commit.

The temptation this item's size creates is to fold it into 005's PR at review time. It stays separate: it
has its own scenarios, its own evidence, and — because of §1.1 — its own honest claim about what it does
and does not change. Merging it into 005 would bury that.
