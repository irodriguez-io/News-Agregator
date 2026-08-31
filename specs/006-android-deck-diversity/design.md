# 006 — Design note

Decisions for `spec.md`. The browser's implementation and its tests are the specification; where they
answer a question the answer is ported and cited rather than re-derived.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths:
`android/app/src/main/kotlin/io/irodriguez/intentionalreading/domain/state/DiscoverDeck.kt` and its
tests, plus this item's own `specs/006-android-deck-diversity/`. Forbidden: `pipeline/**`, `config/**`,
`js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`.

**Runs after 005, on a branch cut from `main`.** This branch was cut carrying only these three
documents, so the design is committed with the wave's design pass (`execution-model.md` §4).

**What actually happened, recorded because the prediction was wrong in two ways.** By the time the first
implementation slice was dispatched the branch sat 46 commits behind `main` — 005's merge and the whole
of the unplanned item 013. It was **merged, not rebased** (`c27d87e`, 2026-08-31): `AGENTS.md` requires
existing history be preserved, and a rebase of a branch this far behind buys nothing a merge does not.
The merge was clean — none of the documentary conflicts the wave brief predicted in `specs/backlog.md`
or `005/evidence.md` materialised. The re-gate on the merged head, which was never optional
(`waves/wave-b-note.md` §3), gave the baseline `slices.md` now records.

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

## D8 — §58's comparator is asserted directly; the deck test asserts the sequenced order

**Written after the first dispatch escalated.** The implementer was briefed to touch no existing
assertion, found that this item cannot satisfy that, and stopped without writing code. It was right.

`DiscoverDeckTest.kt`'s *"candidate order follows all five keys with a deliberate collision at each key"*
asserts the full ordered `deck.candidates` list, ending `sourceAFirst, sourceASecond, sourceB`. Those
last three tie at personalized total 95, and the two `source-a` articles are adjacent **on purpose**, so
that key 5 — article ID ascending — has a collision to resolve. D5's −8 breaks exactly that adjacency:
once `sourceAFirst` is selected, `sourceASecond` scores 87 against `sourceB`'s 95, and the deck must end
`sourceAFirst, sourceB, sourceASecond`. **This item's scope guarantees that assertion fails.** No
implementation satisfies both it and `spec.md` §4.1.

The blast radius is that one assertion and no other. The remaining `DiscoverDeckTest` tests use distinct
sources in single-category fixtures, so the −5 applies uniformly and cannot reorder them;
`ArticleStateMachineUndoTest` reads only `.article`; `UiStateMapper` consumes `deck.article` and the two
counts, never `candidates`; and `DeckCandidate` is constructed only inside `DiscoverDeck.kt`, so D6's
third field breaks no call site.

**The browser does not have this problem, and the reason is structural.** `js/**` asserts
`compareCandidates` directly against a sorted array (`tests/js/ranking.test.js:60-74`) and asserts
`buildDeck` separately (`:92-143`). Item 005 routed its comparator assertion through
`DiscoverDeck.build()` instead — reasonably, since nothing then reordered the comparator's output — and
006's slice plan then froze that coupling.

**Decision:** port the browser's test structure along with its algorithm.

- `DiscoverDeck.candidateComparator` becomes `internal` rather than `private`. Kotlin's `internal` is
  module-scoped and `:app`'s unit-test source set has friend access, so the comparator is reachable from
  tests without widening the public API.
- 005's five-key assertion sorts the candidate list with `candidateComparator` directly. Same fixture,
  same eight IDs, same order, same test name — only what is sorted changes.
- 006 adds a test asserting that same fixture's **sequenced** order, ending `sourceAFirst, sourceB,
  sourceASecond`, with `sameSourcePenalty == -8.0`. The coverage moved off `build()` is replaced, not
  dropped; net assertions go up.

**The rejected alternative, and why it is rejected.** Correcting the expected list in place is a one-line
change and it *works* — at that step the previously selected card is `publicationUnknown`, whose source
matches neither candidate, so no penalty applies and §58 still decides by keys 4 and 5. Every key still
bites. It is rejected because a test named for the five keys would then silently also encode the
sequencing rules, and every future change to sequencing — item 012, any third penalty — perturbs it again
and makes someone re-derive whether the new order is legitimate. That re-derivation is precisely what
this item just paid for once. Splitting the two concerns once makes the comparator test stable against
all future sequencing work.

**Also rejected: leaving `candidates` unsequenced and adding a second, sequenced field.** It would edit
nothing, but `candidates` *is* the deck. Two orderings on one state object leaves every later reader
asking which one Discover consumes, and `spec.md` §1.1's honest claim about what this item changes gets
harder to state, not easier.

**Sizing is unchanged.** D7 still holds: one slice. This adds one visibility modifier and one retargeted
assertion.
