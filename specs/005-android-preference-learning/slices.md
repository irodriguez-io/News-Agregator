# 005 — slice plan

Sized **L → 4 ordered slices**. One item branch (`feat/005-android-preference-learning`), one PR
targeting `main`. Each slice closes as a failing-first test commit plus an implementation commit, and
must fit one fresh implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below; tests live under `android/app/src/test/kotlin/io/irodriguez/intentionalreading/`.

**Every Gradle invocation needs both of these exported first** — `java` is not on this machine's
`PATH`, and a worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

Baseline on `main` at `48abb67`: **198 tests, 0 failures, `BUILD SUCCESSFUL`**, verified 2026-08-26.
A count read from `test-results` after a failed build is the *previous* run's count — delete the
directory first and read the `BUILD SUCCESSFUL` line, not the counts (`waves/wave-b-note.md` §7).

Fixed for every slice — do not re-decide these mid-implementation:

- **The eight delta values and the two clamps are authoritative.** §11/§12 of
  `05-personalization-state.md` and `contracts.md` §21. No tuning, no new events, no rounding helper
  other than the `1e10` port in `design.md` D2.
- **±5.0 clamps a stored weight; ±6 clamps an article's topic *sum* at scoring time.** Two different
  rules, `design.md` D2 and D9. Merging them is a defect.
- **Signals apply to the record's stored snapshot**, never to the incoming dataset article
  (`design.md` D5).
- **`MARK_UNREAD` applies no Save signal. `REMOVE` applies no signal at all.** `contracts.md` §23 and
  §24; `design.md` D6. Both look asymmetric and both are correct.
- **A reversal no-ops when the entry is absent or its count is 0**, and an entry that reverses to
  `0/0` is deleted (`design.md` D2).
- **`preferences.categories` must never exist.** `05-personalization-state.md` §16 prohibits it.
- **Ranking writes nothing** — no base score, no preference, no record, no state write reachable from
  the mapper (§61; `design.md` D11).
- **Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*`** — the constraint item 003
  established, and it binds the two new files and the new `domain/ranking/` package.
- **One lock.** The existing `stateMutex` guards everything in `AppViewModel`; do not add a second, and
  do not hold it across a file write beyond what the existing paths already do.
- **Only one existing test and two existing assertions may be edited, and only in slice 2**
  (`design.md` D7, as corrected 2026-08-26): the whole of `ArticleStateMachineUndoTest.kt:296-326`
  including its title, plus `AppViewModelTest.kt:1183` and `AppViewModelTest.kt:1363`.
  Editing, deleting, weakening, or absorbing any other existing test is a finding, not a judgement call.
- **No new dependency.** `android/gradle/libs.versions.toml` is untouched.
- **No Compose file is touched by this item**, and no slice may substitute a screenshot for a test
  (`design.md` D13).
- Everything outside `android/` is untouched in all four slices, except this item's own `specs/005-*/`
  documents.
- **005 runs alone, but it is the branch that rebases.** If anything lands on `main` first, rebase onto
  merged `main` and re-run both gates on the rebased head; pre-rebase numbers are discarded
  (`waves/wave-b-note.md` §3).
- **Escalate rather than infer.** Three of wave B's cheapest fixes came from the implementer stopping to
  question the reviewer's own arithmetic before writing code (`waves/wave-b-note.md` §4). If a number or
  a boundary in this plan looks wrong, say so before implementing it.

---

## Slice 1: the learning arithmetic, with no call sites

Pure Kotlin, no integration. Everything here is JVM-testable against the browser's numbers, and the
browser's numbers are the specification.

- **Scenarios:** `spec.md` §4.1 in full — "each of the four events applies its exact V1 delta", "a topic
  that appears twice on one article is trained once", "a forced tag trains preferences like any other
  tag", "weights are clamped at both ends", "a lazily created entry appears only when a signal reaches
  it" — plus §4.2's "an interaction count never falls below zero" and "an entry that reverses to nothing
  is removed".
- **Files:** new `«pkg»/domain/state/PreferenceLearning.kt` (the delta table, the clamp, `apply`,
  `reverse`), new test `domain/state/PreferenceLearningTest.kt`.
- **Must not touch:** `«pkg»/domain/state/ArticleStateMachine.kt`, `«pkg»/domain/state/DiscoverDeck.kt`,
  `«pkg»/ui/**`, `«pkg»/data/**`, `res/**`, any Gradle file.
- **Reuse:** `LocalState.Preferences` and `PreferenceEntry` as they stand — the types already exist,
  already validate, and already round-trip. No new model type belongs in this slice.
- **Reference:** `js/state/preferences.js` in full. `applyToEntry`, `reverseFromEntry`, `clampWeight`,
  and `uniqueTopicIds` are the four functions being ported, one for one.
- **Definition of done:**
  - Both gates green; test count above 198 by the number of tests added.
  - A test per event asserting the exact source and topic deltas of §11, driven from a table.
  - A test proving `apply` then `reverse` of the same event returns an entry to its exact prior weight
    and count, for all four events.
  - A test proving the `1e10` rounding: a weight built by accumulating deltas equals the literal value a
    browser-computed document would carry, to the last bit.
  - A test at each clamp boundary: `+5.0` with Save for Later stays `+5.0` and still increments the
    count; `-5.0` with Not Interested stays `-5.0`.
  - A test proving a duplicated tag ID moves its topic once.
  - A test proving reversal no-ops on an absent entry and on an entry at 0 interactions.
  - A test proving an entry at exactly one interaction whose weight is exactly its delta is **removed**
    from the map by reversal.
  - No existing assertion edited.
- **Status:** done

## Slice 2: the state machine and the undo path

The integration slice, and the one that changes shipped behaviour. Latching, the reversal guard, and
007's declared seam.

- **Scenarios:** `spec.md` §4.1's "repeating an action does not train twice" and "Remove from Read Later
  trains nothing"; §4.2's "Mark Unread reverses the Read signal it can prove was applied", "Mark Unread
  on a record with no applied Read signal changes no weight", "Undo Save and Undo Dismiss reverse the
  signal they applied", "an Undo of an action that applied no signal touches no weight", "an undone
  action can be redone without training twice", "reversal trains against the record's stored snapshot".
- **Files:** `«pkg»/domain/state/ArticleStateMachine.kt` (preferences on all four transition variants;
  apply-on-latch; the `MARK_UNREAD` reversal; the undo reversal),
  `«pkg»/domain/state/UndoRecord.kt` (`PreferenceReversal` becomes a real enum),
  `«pkg»/domain/model/ArticleRecord.kt` (remove the `signalsApplied` default),
  `«pkg»/domain/model/LocalState.kt` (delete `SignalsApplied.derivedForAndroid`),
  `«pkg»/ui/AppViewModel.kt` (`localState.copy(articles = …, preferences = …)` at the three persist
  helpers), and the tests `domain/state/ArticleStateMachineTest.kt`,
  `domain/state/ArticleStateMachineUndoTest.kt`, `ui/AppViewModelTest.kt`.
- **Must not touch:** `«pkg»/domain/state/DiscoverDeck.kt`, `«pkg»/domain/ranking/**` (does not exist
  yet), `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/data/**`, `«pkg»/ui/screens/**`,
  `«pkg»/ui/components/**`, `res/**`, any Gradle file.
- **Reuse:** slice 1's `PreferenceLearning` for all arithmetic — a second copy of a delta or a clamp
  anywhere in this slice is a finding. The existing `allowedFrom` map, `isIdempotentNoOp`, and the
  `Applied`/`Reverted`/`Unchanged`/`Invalid` shape all stay; this slice adds a field and two arithmetic
  calls, it does not restructure the machine.
- **Reference:** `js/state/article-state.js` — `applySignal` (`:47-54`), the `mark_unread` branch
  (`:122-131`), the undo record's `preferenceSignal` (`:137-145`), and `undoArticleAction` (`:151-165`).
- **Fixed decisions — do not re-open mid-implementation:**
  - `ArticleTransition` carries `preferences` on the interface, and `Invalid`/`Unchanged` carry the
    unchanged map. Not a nullable field on `Applied` only (`design.md` D3).
  - `ArticleRecord.signalsApplied` loses its default; the compiler is the checklist.
  - `OPEN`'s idempotency check reads `signalsApplied.opened`, not `openedAt` (`design.md` D4).
  - `preferenceReversal` is set **only** when the forward transition actually applied a signal.
  - `reverse()` reverses the recorded event **before** restoring `previousRecord`, against the current
    record's snapshot.
  - The three named existing assertions are updated to directional ones; nothing else is edited.
- **Definition of done:**
  - Both gates green.
  - A test per action proving the signal is applied exactly once, and a second identical action changes
    no weight and no count.
  - A test proving `REMOVE` from `saved` changes no weight and leaves `saved` latched.
  - A test proving `MARK_UNREAD` with `read` latched subtracts exactly the Mark Read delta, decrements
    each count by one, clears the flag, and applies no Save signal.
  - A test proving `MARK_UNREAD` with `read` **not** latched changes no weight and still moves the
    record to `saved`.
  - A test proving a Save→Undo→Save sequence leaves weights and counts equal to their values after the
    first Save.
  - A test proving an Undo whose forward transition applied no signal leaves `preferences` unchanged and
    still restores the previous record.
  - A test proving a reversal on a record whose article is absent from the current dataset applies to
    the stored snapshot's source and tags.
  - `git grep -n 'derivedForAndroid'` returns nothing.
  - Exactly three existing assertions changed, all named in `design.md` D7, each with a comment naming
    the scenario it now encodes.
- **Status:** done

## Slice 3: the reconciliation fold

- **Scenarios:** `spec.md` §4.3 in full.
- **Files:** new `«pkg»/domain/state/PreferenceReconciliation.kt` (`reconcile(state): LocalState`),
  `«pkg»/ui/AppViewModel.kt` (the two call sites and the single conditional write), new test
  `domain/state/PreferenceReconciliationTest.kt`, and `ui/AppViewModelTest.kt` for the wiring.
- **Must not touch:** `«pkg»/domain/state/ArticleStateMachine.kt`, `«pkg»/domain/state/DiscoverDeck.kt`,
  `«pkg»/domain/ranking/**`, `«pkg»/data/**`, `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/ui/screens/**`,
  `res/**`, any Gradle file.
- **Reuse:** slice 1's `PreferenceLearning.apply` for the folded deltas. The existing `saveLocalState`
  for the write, and the existing `adoptPersistedState` for adoption of its result.
- **Fixed decisions — do not re-open mid-implementation:**
  - The claimed count is computed per `design.md` D1: for a source, the true flags of records from that
    source; for a topic, the true flags of records carrying that tag.
  - Only **missing** signals are folded in. Weight is never audited or recomputed.
  - Over-count lowers the count to `claimed` and leaves the weight alone (`design.md` D8).
  - Records are folded in **article ID ascending** order.
  - Called from `restoreLocalState()` and `importLocalData()` only, both on `Success`. **Never** from
    `adoptPersistedState`, never after `reset()`, never on a save result — `design.md` D8 explains what
    that would hide.
  - The reconciled state is written exactly once, and only when the fold changed something.
  - A failed load reconciles nothing and writes nothing; the recovery lock is untouched.
- **Definition of done:**
  - Both gates green.
  - A test folding the concrete pre-005 record of `spec.md` §4.3 — `read`, `openedAt` populated, empty
    `preferences` — and asserting `+0.35 / 2` on the source and `+0.25 / 2` on each topic.
  - A test proving a second `reconcile` of the reconciled state returns an equal state.
  - A test proving the drift scenario end to end: load a pre-005 state, apply a post-005 signal to the
    same source, `MARK_UNREAD` the pre-005 record, and assert the source's weight and count equal
    exactly the signals still applied. **This is one of the wave's two named evidence obligations**
    (`waves/wave-c.md` §Gates).
  - A test proving the fold covers `saved` and `dismissed` flags, not only `opened` and `read`.
  - A test proving over-count lowers the count and leaves the weight.
  - A test proving determinism where clamping bites: the same input bytes reconcile to equal states.
  - A ViewModel test proving a load whose fold changes something performs **exactly one** write, and a
    load whose fold changes nothing performs **none**. This is the observable form of "runs exactly
    once" — the wave's other named evidence obligation.
  - A ViewModel test proving an import is reconciled, and that replacement semantics still hold — a
    record present only in the pre-import state is gone.
  - A ViewModel test proving a failed load raises the recovery notice and attempts no write.
  - A ViewModel test proving an ordinary article action's save result is **not** reconciled.
  - No existing assertion edited in this slice.
- **Status:** done

## Slice 4: personalized scoring and deck order

The slice that changes what the reader sees. Still fully JVM-testable — no Compose file is touched.

- **Scenarios:** `spec.md` §4.4 in full, plus §4.5.
- **Files:** new `«pkg»/domain/ranking/PersonalizedScore.kt`,
  `«pkg»/domain/state/DiscoverDeck.kt` (the ordered candidate list, the comparator, the pin),
  `«pkg»/ui/state/UiStateMapper.kt` (a `preferences` parameter threaded to `discover`),
  `«pkg»/ui/AppViewModel.kt` (pass `localState.preferences` from `mapUiState`), new test
  `domain/ranking/PersonalizedScoreTest.kt`, and `domain/state/DiscoverDeckTest.kt` — new, since
  `DiscoverDeck` has no dedicated test today — plus `ui/state/UiStateMapperTest.kt`.
- **Must not touch:** `«pkg»/domain/state/ArticleStateMachine.kt`,
  `«pkg»/domain/state/PreferenceReconciliation.kt`, `«pkg»/data/**`, `«pkg»/ui/screens/**`,
  `«pkg»/ui/components/**`, `«pkg»/ui/gesture/**`, `res/**`, any Gradle file.
- **Reuse:** `DiscoverDeck.isEligible` unchanged — it already matches `js/state/selectors.js:3-5`, and
  eligibility is not this item's subject. `Article.score.base` read-only.
- **Reference:** `js/ranking/personalize.js` in full, `js/ranking/deck.js:5-24` for the comparator, and
  `js/app.js:210-224` for how the held card is chosen from the built deck.
- **Fixed decisions — do not re-open mid-implementation:**
  - Five score components on the result type, not a bare total (`design.md` D9).
  - `total` is unclamped (§54).
  - Unique topic IDs are deduplicated once and reused for both the weight sum and the exploration
    minimum.
  - The comparator is §58's five keys in that order, with unknown publication dates **last** — an
    explicit nulls-last comparator, not `compareBy` on a nullable.
  - The pin is applied **after** ordering; `availableCount` and `remainingCount` keep today's meaning.
  - No Compose file, and no new UI state field beyond the mapper's parameter.
- **Definition of done:**
  - Both gates green.
  - A test asserting all five components separately for an article with a weighted source and three
    topics.
  - A test at the ±6 topic clamp proving the component clamps while the stored weights do not.
  - A test per exploration row of §57, including the no-topics case and the `min(3, …)` cap.
  - A test proving the full five-key tie-break chain, with a deliberate collision at each key.
  - A test proving two builds of the same inputs are identical.
  - A test proving a held card at personalized position five is still the card returned, and that the
    counts are unaffected by the pin.
  - A test proving a build modifies nothing: preferences, records, and base scores are equal before and
    after, by value.
  - A test proving eligibility and category filtering are unchanged from today's behaviour, and that
    `availableCount` still counts eligible articles.
  - A mapper test proving the deck shown reflects preferences, and queue tests proving Read Later and
    History order by `savedAt`/`readAt` descending regardless of weights.
  - No existing assertion edited in this slice.
- **Status:** pending

---

## Ship bookkeeping this item creates

Handled at wave close, not inside a slice:

- `evidence.md` for this item, with the gate numbers recorded **at the moment of each run**, not
  reconstructed (`execution-model.md` §5.1).
- `spec.md` §5.2's walkthrough, run against merged `main` on accumulated history, with the second
  question asked at every step.
- `backlog.md`: 005 moves to Shipped; the wave C row updates; `future-items.md`'s
  §"The item that ports preference learning" is marked resolved with the decision taken.
- The correction in `design.md` D12 carried into 006's design pass, so 006 is not sized against a claim
  that turns out to be 005's.
