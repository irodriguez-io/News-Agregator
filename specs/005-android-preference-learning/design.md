# 005 — Design note

Decisions for `spec.md`. Where the web client already answers a question, the answer is ported and
cited rather than re-derived. Where the wave brief offered a choice, the choice is made here and stated
as a decision — `waves/wave-b-note.md` §5: briefs state decisions, they do not offer shapes.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths: `android/**` plus this
item's own `specs/005-android-preference-learning/`. Forbidden: `pipeline/**`, `config/**`, `js/**`,
`css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`. The `ArticleDataset v1` contract is
consumed as frozen.

**005 runs alone in wave C** (`execution-model.md` §2): it touches `AppViewModel.kt`,
`ArticleStateMachine.kt`, and `DiscoverDeck.kt`, the three hub files of the client. Nothing else runs
beside it. **It is still the branch that rebases** — if anything at all lands on `main` first, this
branch rebases onto merged `main` and re-runs both gates on the rebased head, with pre-rebase numbers
discarded (`waves/wave-b-note.md` §3, which is where the wave's most expensive defect was found).

## D1 — The reconciliation is by invariant, not by marker

Owner decision, 2026-08-26, taken against the arithmetic rather than in the abstract.

`interactions` is **exactly recomputable** from the record set, and weight is not. `05-personalization-state.md`
§13 defines `interactions` as the number of *currently applied* learning signals affecting that
preference, `contracts.md` §15 says the same, and every applied signal is recorded in exactly one
record's `signalsApplied` — reversal decrements the flag and the count together (§13, §23). Therefore:

```text
claimed(S) = Σ over records r where r.article.source.id == S
               of count(true flags in r.signalsApplied)

claimed(T) = Σ over records r whose r.article.tags contain T
               of count(true flags in r.signalsApplied)
```

The fold compares `claimed` with the stored count and applies **only the missing signals' deltas**.
Weight is not audited, because clamping is order-dependent and a stored weight cannot be recomputed
from the records.

**Why this rather than the one-time pass `future-items.md` proposed.** A first-init pass needs a
persisted marker; `contracts.md` §14 forbids new keys in the state document, so the marker would have to
live in a separate app-local file — and having paid that cost, it still would not cover a document
imported *after* the marker was set, which is exactly the case
`specs/009-android-import-export/design.md` D1 recorded for this item. The invariant version is
idempotent because it recomputes rather than remembers: the second pass finds `claimed == stored` and
does nothing.

**Why it is not a self-healing mask.** See D8 — it runs at exactly two call sites, and never on the
result of an ordinary save.

## D2 — The delta table and the clamp are ported verbatim, including the rounding

New pure file `«pkg»/domain/state/PreferenceLearning.kt`, mirroring `js/state/preferences.js`. No
Android imports; `domain/**` stays framework-free, the constraint item 003 established.

```text
event            source    each topic
Not Interested   -0.35     -0.20
Save for Later   +0.45     +0.30
First Open       +0.10     +0.05
Mark Read        +0.25     +0.20
```

Authoritative in `05-personalization-state.md` §11 and `contracts.md` §21; identical to
`INTERACTION_DELTAS` at `js/state/preferences.js:1-6`.

**The clamp is two operations, not one**, and the order is the browser's
(`js/state/preferences.js` `clampWeight`):

```js
Math.max(-5, Math.min(5, Math.round(value * 1e10) / 1e10))
```

The `1e10` round is not decoration — it is what stops accumulated `Double` error from making
`0.1 + 0.05 + 0.05` unequal to a browser-computed value in an exported document. The Kotlin port is
`(value * 1e10).roundToLong() / 1e10` then `coerceIn(-5.0, 5.0)`. `kotlin.math.roundToLong` and JS
`Math.round` are both `floor(x + 0.5)`, so they agree on negative half-way values too — which
`Math.round(-0.5) == -0.0` and Kotlin's `-0.5.roundToLong() == 0` confirm. Any other rounding helper
(`BigDecimal`, `String.format`, `roundToInt`) is a finding.

**The ±5.0 clamp and the ±6 topic-component clamp are different things** and must not be merged:
§12 bounds each stored weight to ±5.0; §56 bounds the *sum* of an article's topic weights to ±6 at
scoring time, without touching the stored values.

Apply and reverse mirror `applyToEntry` and `reverseFromEntry` exactly, including the two behaviours
that look like edge cases and are not:

- **Reversal no-ops** when the entry is absent or its count is already 0. This is the guard that makes
  today's un-reconciled disk contents harmless until the reader's first post-005 signal (`spec.md` §1.1).
- **An entry that reverses to `weight == 0 && interactions == 0` is deleted**, not left in the map. It
  is why an exported document's `preferences` map stays small, and a test asserts it.

## D3 — The transition carries preferences; the state machine does not take `LocalState`

`ArticleStateMachine.transition` currently takes and returns `Map<String, ArticleRecord>`, and
`AppViewModel` does `localState.copy(articles = transition.records)`. The browser's `transitionArticle`
takes and returns the whole state, because in JS that is free.

**Decision:** `ArticleTransition` gains `preferences: LocalState.Preferences` alongside `records`, on
the interface, carried by all four variants — `Invalid` and `Unchanged` carry the unchanged map.
`transition()` and `reverse()` take `preferences` as a parameter beside `records`. `AppViewModel` becomes
`localState.copy(articles = t.records, preferences = t.preferences)`.

Passing the whole `LocalState` was considered and rejected: the state machine has no business with
`settings`, `session`, or `schemaVersion`, and widening its input to get one field is how a hub file
becomes a god object. A nullable `preferences?` on `Applied` only was rejected for the reason wave A and
wave B both recorded — a nullable field that five of six construction sites cannot populate loses the
guarantee the type existed to give (`waves/wave-b-note.md` §5).

## D4 — Stored `signalsApplied` becomes authoritative, and the derivation helper is deleted

`SignalsApplied.derivedForAndroid` exists because 003 had no arithmetic to latch
(`specs/003-android-local-state-persistence/design.md` D9). After this item the flags *are* the latch,
and a helper that recomputes them from status is a live hazard: it would silently overwrite a `saved`
or `dismissed` flag that arrived by import.

**Decision:** delete `SignalsApplied.derivedForAndroid`, and **remove the default value from
`ArticleRecord.signalsApplied`** so that every construction site must state the flags. The compiler then
enumerates the call sites, which is cheaper than a review pass looking for the ones that forgot.

The validator's three equalities still bind and are not weakened
(`LocalStateValidator.kt:137-145`): `opened == (openedAt != null)`, `read == (status == READ)`, and
`dismissed` implies a dismissed status. Latching happens *within* those constraints — which is possible
because every transition that sets a flag sets the matching field in the same step, exactly as the
browser does. `saved` carries no such equality and is a free latch, which is what `contracts.md` §20's
`status = saved, signalsApplied.saved = false` example depends on.

**Amendment, 2026-08-26, raised by the slice 2 implementer before writing code.** Deleting the
recompute has two consequences D4 did not spell out, both settled here:

1. **`ArticleStateMachineTest.kt:325` must flip.** Its enclosing test, `Android actions preserve
   foreign learning signals while enforcing structural signals` (`:278`), builds a deliberately
   inconsistent record — `status = OPENED` with `dismissed = true` — applies `SAVE`, and asserts the
   flag is cleared to `false`. That assertion *is* the "enforcing structural signals" half of its own
   name, which is precisely what this decision deletes. Under the browser, nothing ever clears
   `dismissed`; the flag carries forward. **Two of that assertion's four flags flip, not one:** the
   same `SAVE` also latches its own signal, so the line becomes
   `opened = true, saved = true, dismissed = true, read = false`. The test is renamed accordingly and
   thereby becomes a test of the latch invariant rather than of the derivation that replaced it.

   **This is safe because the input is unreachable.** Through the state machine, `dismissed = true`
   implies `status == DISMISSED` (`DISMISS` sets both), and `SAVE`, `OPEN` and `MARK_READ` are all
   disallowed from `DISMISSED`; off disk or by import, `LocalStateValidator` rejects it outright. The
   carried-forward flag would fail validation at the next save, which is the correct place for an
   inconsistent record to be caught — the recompute silently laundering it is the hazard, not the fix.

   The sibling test at `:265`, `Android transitions derive only the signals structurally forced by the
   record`, still passes unchanged under latching and must not be touched.

2. **The test helper keeps deriving; only the production helper dies.** `derivedForAndroid` has exactly
   three usages: the `ArticleRecord` default, the helper itself, and a default on the private `record()`
   fixture at `ArticleStateMachineTest.kt:380`. The hazard D4 names is a *production* helper that
   recomputes flags from status. The fixture's default is inlined into the test file with its semantics
   **unchanged** — making it all-false instead would silently alter the meaning of every existing test
   that relies on it, which is a test weakening wearing mechanical clothes.

   The seven `ArticleRecord(` construction sites in `AppViewModelTest.kt`, `UiStateMapperTest.kt`,
   `ArticleStateMachineTest.kt` and `ArticleStateMachineUndoTest.kt` take explicit flags as mechanical
   compilation repair. That is the compiler being the checklist, exactly as intended; no assertion in
   those files changes as a result. `LocalStateValidator.kt:184` already passes `signalsApplied`
   explicitly and needs no change.

`OPEN`'s idempotency check moves from `existing?.openedAt != null` to
`existing?.signalsApplied?.opened == true`, matching `js/state/article-state.js:81`. Equivalent under the
validator's invariant, but the flag is now the reason rather than a proxy for it, and the comment 003
left at `ArticleStateMachine.kt:172-174` is removed with it.

## D5 — Signals are applied against the record's stored snapshot

The browser applies and reverses against `record.article` — the validated snapshot — not against the
candidate article handed to the transition (`js/state/article-state.js:88`, `:121`). This is not
incidental: a `read` record whose article has left the dataset must still reverse against the tags it was
trained on, and `05-personalization-state.md` §7 keeps complete snapshots precisely so History survives
the dataset.

**Decision:** apply and reverse against `current.article` for an existing record and against the
newly created record's snapshot for a first interaction. Passing the incoming dataset `article` is a
finding even when the two are equal, because the case where they differ is the one that matters.

## D6 — Which transitions train, and which deliberately do not

| Action | Signal | Notes |
|---|---|---|
| `OPEN` | First Open, if `opened` not latched | §17–20 |
| `SAVE` | Save for Later, if `saved` not latched | §21–23 |
| `DISMISS` | Not Interested, if `dismissed` not latched | §24–25 |
| `MARK_READ` | Mark Read, if `read` not latched | §26–28 |
| `MARK_UNREAD` | **reverses** Mark Read, only if `read` was latched | §29, `contracts.md` §23 |
| `REMOVE` | none | §30–31, `contracts.md` §24 |

`MARK_UNREAD` applies no Save signal even though it moves the article to `saved`
(`contracts.md` §23), and `REMOVE` applies no negative signal even though it moves the article to
`dismissed` (§24 — *"removing an item from a backlog does not necessarily mean the topic/source is
unwanted"*). Both are the kind of rule an implementer optimising for symmetry will "fix"; both are
tested.

## D7 — `PreferenceReversal` becomes a real type, and 007's assertion is deliberately replaced

`UndoRecord.preferenceReversal` is declared today as `typealias PreferenceReversal = Nothing`, always
null, never read — 007 left it there so this item would not have to reshape the undo path
(`specs/007-android-undo/design.md` D5).

**Decision:** the typealias becomes an enum of the four learning events, and the field is populated
**only when the forward transition actually applied a signal**, mirroring
`preferenceSignalApplied ? EVENT_FOR_ACTION[action] : null` (`js/state/article-state.js:144`). This is
what stops an undone-then-redone Save from training twice, and what makes an Undo of a no-op Save touch
no weight.

`reverse()` reverses the recorded event against the **current** record's snapshot before restoring
`previousRecord`, matching `undoArticleAction` (`js/state/article-state.js:157-160`). Restoring
`previousRecord` restores the flags for free; only the weights need the explicit reversal.

**Three existing assertions are changed on purpose, and each is a deliberate act rather than a test
weakening.** `waves/wave-b-note.md` §1 flags a shipped item's scenario being quietly absorbed as a
review finding, so these are named in advance:

- `ArticleStateMachineUndoTest.kt`, the test `the undo record carries a reversal field that is not yet
  used` (`:296-326`) **in full, including its title** — it is 007's placeholder for precisely this
  deferral, and every line of it encodes the deferred state. `:317`'s
  `assertNull(dismiss.undoRecord?.preferenceReversal)` becomes an assertion of
  `PreferenceReversal.NOT_INTERESTED`; `:322` must thread `reversed.preferences` into `after` rather
  than copying only `articles`; `:324-325`'s `assertSame(preferences, after.preferences)` becomes
  directional — the Dismiss moved the weights, the Undo restored them.
- `AppViewModelTest.kt:1183` and `:1363` — `assertEquals(preferences, persisted.preferences)` likewise.

**Correction, 2026-08-26, raised by the slice 2 implementer before writing code.** This decision
originally named `:324-325` and counted three assertions. It missed `:317`, which is in the same test
body and asserts the same deferral, and it did not account for `:322` copying only `articles`, which
would have left `:324-325` vacuous rather than directional. Counting lines was the error; the unit is
the test. The intent is unchanged and nothing is weakened — a null-check becomes a value-check and a
vacuous equality becomes a directional one. `waves/wave-b-note.md` §4 again: the implementer stopping
to question the plan's arithmetic was cheaper than the review round that would have caught it.

**Second correction, 2026-08-26, raised by the slice 2 implementer with RED already committed.** One
further existing assertion changes: `ArticleStateMachineUndoTest.kt:48`, in the test `an undo-eligible
save records what it replaced`. It compares the whole `UndoRecord` against a constructed expected value
whose `preferenceReversal` takes the field's `= null` default; a first Save now correctly records
`SAVE_FOR_LATER`. The expected record gains an explicit
`preferenceReversal = PreferenceReversal.SAVE_FOR_LATER`, which leaves the assertion full-record and
strictly more specific than before.

Both earlier sweeps missed it because it asserts nothing *about* preferences by name — it inherits the
null through a constructor default. **Grepping for assertions is not sufficient to find the surface a
defaulted field covers; the compiler and a full test run are.** The sibling `UndoRecord(` constructions
at `:260` and `:280` are refusal-path *inputs* whose transitions return `Invalid` before any reversal,
and they correctly need no change.

No other existing assertion may be edited. 007's other scenarios remain 007's.

## D8 — The fold runs at exactly two call sites, and never on a save

The pure function lives in `«pkg»/domain/state/PreferenceReconciliation.kt`:
`reconcile(state: LocalState): LocalState`. It is called from **two places only**, both in
`AppViewModel`:

1. `restoreLocalState()`, on a `LocalStateResult.Success` — the cold load;
2. `importLocalData()`, on a `LocalStateResult.Success` — after a valid import.

**It must not be called from `adoptPersistedState`.** That is the tempting single choke point — every
path funnels through it — and it is the wrong one twice over: it also runs on the result of every
ordinary `save`, so the fold would re-run on each article action, and, far worse, it would **silently
correct any arithmetic bug in the transitions themselves**. A self-healing pass over a broken state
machine turns a failing test into a passing one. The two named call sites are the whole surface.

It is also not called after `reset()`: the default state has no records, so the fold is a no-op by
construction, and adding the call anyway invites the reader of the diff to think it is load-bearing.

**Persistence.** When the fold changes anything, the reconciled state is written once, through the
existing `saveLocalState`, and the write's result is adopted as usual. When it changes nothing, no write
occurs — which is the observable form of "runs exactly once" and is how `spec.md` §4.3's idempotency
scenario is asserted. A failed load never reconciles and never writes; the recovery lock stays exactly
as it is today.

**Determinism.** Records are folded in **article ID ascending** order. The fold is otherwise
order-sensitive only where clamping bites, and a test builds a state where it does.

**The over-count case.** Where a stored count exceeds the claimed count, the count is lowered to
`claimed` and the **weight is left unchanged**. The document says how many signals are outstanding; it
does not say which event produced the stranded weight, so the weight cannot be recovered — only guessed.
The count is the more expensive of the two errors anyway (`spec.md` §1.1: exploration is worth up to 3,
a single delta at most 0.45). This case is reachable exactly once per document, from the mirror hazard
`specs/009-android-import-export/design.md` D1 recorded: a pre-005 Android `MARK_UNREAD` cleared a
browser record's `read` flag without decrementing anything. After this item ships it is unreachable,
because every flag clearing reverses.

## D9 — Scoring goes in a new `domain/ranking/` package

New pure file `«pkg»/domain/ranking/PersonalizedScore.kt`, mirroring `js/ranking/personalize.js`, with a
component-carrying result type rather than a bare `Double`:

```kotlin
data class PersonalizedScore(
    val total: Double,
    val base: Int,
    val sourcePreference: Double,
    val topicPreference: Double,
    val exploration: Double,
)
```

The browser returns the same five fields for the same reason — the components are what a test asserts
against, and asserting a total alone cannot distinguish a wrong weight from a wrong exploration bonus.
No debug surface consumes them on Android (`spec.md` §3).

Mirroring the browser's `ranking/` and `state/` split keeps `DiscoverDeck` as the ordering module and
puts the arithmetic where 006 will also want it. `domain/ranking/**` inherits `domain/**`'s no-framework
rule.

The four components, from §§54–57, with no latitude:

- `sourcePreference` = the source's stored weight, 0 when absent;
- `topicPreference` = `clamp(Σ unique topic weights, -6, +6)`;
- `exploration` = `min(3, sourceExploration + topicExploration)`, where source exploration is
  `0→+3, 1→+2, 2→+1, 3+→0` on the source's interaction count and topic exploration is
  `0→+2, 1→+1, 2→+0.5, 3+→0` on the **lowest** interaction count among the article's unique topics, and
  is 0 when the article has no topics;
- `total` = `base + sourcePreference + topicPreference + exploration`, **unclamped** — §54 is explicit
  that it is a ranking value, not a percentage.

Unique topic IDs are deduplicated once and reused for both the weight sum and the exploration minimum
(`js/ranking/personalize.js:22-32`), which is also what §15's *"duplicate topic IDs must never cause
repeated preference application"* requires.

## D10 — The deck becomes an ordered list, and the pin is applied after ordering

`DiscoverDeck.build` keeps its three outputs and gains the ordered candidates:

```kotlin
data class DeckCandidate(val article: Article, val score: PersonalizedScore)

data class DiscoverDeckState(
    val article: Article?,        // held ?: candidates.firstOrNull()
    val candidates: List<DeckCandidate>,
    val availableCount: Int,
    val remainingCount: Int,
)
```

Eligibility and category filtering are unchanged — `isEligible` already matches
`js/state/selectors.js:3-5` — and `availableCount` stays the eligible count, so personalization changes
the order and nothing else about the counts.

The comparator is §58's five keys in order: total descending, base descending, publication date
descending with unknown last, source ID ascending, article ID ascending. Unknown publication dates sort
last via the browser's `Number.NEGATIVE_INFINITY` treatment (`js/ranking/deck.js:11-13`); in Kotlin that
is an explicit nulls-last comparator, not `compareBy` on a nullable, whose default is nulls-first.

**The pin still wins.** `heldArticleId` selects from the ordered candidates by ID, exactly as it selects
from the eligible list today, and is applied *after* ordering. A rerank that moves the held card to
position five must still show the held card (`waves/wave-c.md`; `DiscoverDeck.kt:25`) — the browser does
the same at `js/app.js:213-219`, and `clearHeldArticleIfNeeded` already drops the pin when the record
stops being `opened`.

`UiStateMapper.map` and its private `discover` helper gain a `preferences` parameter, threaded from
`AppViewModel.mapUiState`. That is the whole of the UI-layer change: no Compose file is touched by this
item, and there is no Compose-only slice.

## D11 — Purity, stated as a constraint rather than a hope

§61 forbids ranking from modifying base scores, preferences, interaction counts, persisted records, or
the dataset. Two structural consequences, both checkable in review:

- `DiscoverDeck` and `PersonalizedScore` take `preferences` and return values. Neither has a write path
  to reach, because neither is handed anything that can write.
- `mapUiState()` runs on every `publish()`. A fold, a save, or any mutation reachable from the mapper is
  a defect of the same class as the wave B lock-across-a-write finding, and the reviewer looks for it
  explicitly.

## D12 — What this item leaves for 006, and a correction to the wave brief

006 owns the −8 same-source and −5 third-consecutive-category penalties (§§59–61), on its own branch,
after this merges. This item leaves `DiscoverDeck` holding the ordered list those penalties sequence, so
006's expected surface is `DiscoverDeck.kt` and its tests only, as `waves/wave-c.md` says.

**The wave brief's claim that the Android head article differs from the browser's until 006 lands is
wrong, and it is worth recording why.** `penaltiesFor` reads `selected.at(-1)`, so the first selection
step carries no penalty (`js/ranking/deck.js:26-38`); both clients render exactly one card; and the
browser rebuilds the deck on every render (`js/app.js:210`). The head card is therefore chosen by
personalized order alone in both clients. 006 is a genuine spec-parity gap with no reader-visible
effect at today's surface — the owner confirmed on 2026-08-26 that it ships anyway, as its own item.
005 closes the head-article gap by itself.

## D13 — Sizing, and why this item is four slices

`execution-model.md` §5 calls 005 *"one review larger than all of wave A combined"*. The slice plan
splits it so that each slice is one coherent objective with its own failing-first test commit, and so
that the arithmetic is proven before anything depends on it:

1. the learning arithmetic, with no call sites;
2. the state machine and undo integration;
3. the reconciliation fold and its two call sites;
4. personalized scoring, deck ordering, and the one UI-layer signature change.

Every slice is fully JVM-testable. Wave B's two Compose-only slices needed `assembleDebug` plus a
walkthrough as their evidence; this item has no such slice, and a slice report that offers a screenshot
in place of a test is a finding.

**Review of each slice feeds the next slice's brief** rather than a later findings round — the practice
that made 009's slice 3 pass first time (`waves/wave-b-note.md` §6).
