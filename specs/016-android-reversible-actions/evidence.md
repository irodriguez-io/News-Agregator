# 016 — Widen what is reversible · evidence

**Branch:** `feat/016-android-reversible-actions` · **Worktree:** `news-agregator-016`\
**Cut from:** `main` at `eb4742af173fb542884574aac916b9917e7ba8b8`\
**Wave:** D, fourth and last item, dispatched 2026-08-31 after 015, 012 and 014 merged

**Authorized by Amendment 8**, committed on `main` in PR #19 and shared with item 014. This item widens
`reversibleActions` beyond `SAVE`/`DISMISS` to `MARK_READ`, `MARK_UNREAD` and `REMOVE`, and it is the item
that carries the reversal arithmetic — `spec.md` §2, ratified by the owner on 2026-08-31.

---

## 1. Forecast reconciliation — `/feature-implementation` Step 0.4

`spec.md` §7's **eight** assumptions, re-checked against `eb4742a` before slice 1 was briefed.
`waves/wave-d.md` says this step is not a formality on this item: it is the step that would have caught
item 006's failure, and it caught 014's.

| # | Assumption | Result at `eb4742a` |
|---|---|---|
| 1 | Amendment 8 committed on `main`, including §2's arithmetic and the `MARK_UNREAD` re-application | ✅ `docs/v1/README.md:133`, with the amended-by markers in all five documents |
| 2 | Item 014 **removed** the `undoable` parameter; the offer follows the record | ✅ zero occurrences in product code — the five remaining strings in `AppViewModelTest.kt` are comments and a variable name |
| 3 | `reversibleActions` still `setOf(SAVE, DISMISS)`; `preferenceReversals` has two entries | ✅ `ArticleStateMachine.kt:253` and `:262-265` (one line earlier than the spec's `:254`/`:263`) |
| 4 | `transition`'s `MARK_UNREAD` branch unchanged | ✅ `:120-127`, still leaves `preferenceSignalApplied` false |
| 5 | `UndoRecord` has four fields; `PreferenceReversal` has its four cases including `MARK_READ` | ✅ the enum case this item needs already existed |
| 6 | `PendingUndoMessage` has two cases; `raiseUndoOffer` errors on anything but `SAVE`/`DISMISS`; `UiStateMapper:70` duplicates the set | ✅ all three |
| 7 | 015's `expectDiscoverHead = true` on the four Discover lambdas | ✅ four sites in `IntentionalReadingApp.kt`, preserved by this item |
| 8 | `only save and dismiss are reversible` and `UiStateMapperTest`'s `undoAvailable` cases still present | ✅ `:119` and `:543-549` |

**All eight held**, including the one that determines the plan's shape. Assumption 2 being true means
`slices.md`'s *If assumption 2 is false* contingency never applied and no call-site audit was needed.

**But the plan still had to be re-cut**, for a reason none of the eight assumptions could have caught. See
§5.

## 2. Gate runs

Recorded at the moment of each run, `test-results` deleted first (`execution-model.md` §5.1 control 4).
Every head row is the supervisor's own independent run, not the implementer's report.

| When | Slice | `:app:testDebugUnitTest` | `:app:assembleDebug` |
|---|---|---|---|
| 2026-08-31, base `eb4742a` | — (baseline) | 286 tests, 0 failures, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `bebe0b6` | A | **289 tests, 0 failures, 0 errors**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `ed80df4` | B | **297 tests, 0 failures, 0 errors**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |

The hosted `android.yml` runs exactly `:app:testDebugUnitTest :app:assembleDebug` with no lint step, so
local gate evidence is equivalent to the hosted gate's scope.

## 3. Failing-first evidence

| Slice | RED reproduced | Test commit | Implementation commit |
|---|---|---|---|
| A — the sinks | **yes** | `4e7e51f` | `bebe0b6` |
| B — the source | **yes, 96 focused tests with 8 intended failures** | `c6881ef` | `ed80df4` |

**Slice A's RED** is three `UiStateMapperTest` cases asserting `undoAvailable` for `MARK_READ`,
`MARK_UNREAD` and `REMOVE`. Red against `UiStateMapper.kt:70`'s `undoAction == SAVE || undoAction ==
DISMISS`, which returns false for all three. The enum cases and the `stringResource` mapping have **no
assertable RED at that layer** — a `when` over an enum is where the compiler is the test, and
`stringResource` needs a composition the CI unit source set does not run. They ride in the implementation
commit, stated plainly in the commit message rather than dressed up as a test.

**Slice B's RED** is the eight arithmetic and ViewModel cases: seven missing undo records and the
cross-pane case missing the Mark Read offer. Behavioural, not scaffolding.

## 4. Existing assertions changed

`design.md` D5 is the plan. This is what happened, and every row carries its reason in the commit message.

| Test | Planned | Actual |
|---|---|---|
| `ArticleStateMachineUndoTest` `only save and dismiss are reversible` (`:119`) | rewrite as `only Open is not reversible`, keeping the table shape | **done, and strengthened.** The original listed only non-reversible actions; the rewrite carries all six with an explicit expected boolean per action. |
| `ArticleStateMachineUndoTest` `reversible actions carry undo state without caller eligibility` (`:30`) | **not on D5's list** | **`MARK_READ` dropped from the non-reversible loop at `:49`; nothing else in the case touched.** Reported at slice 1's preflight before any edit, authorized at `81a03d6`, and added to D5 as a row. See §5.2. |
| `ArticleStateMachineUndoTest` `an idempotent no-op produces no undo record` | keep, extend with `MARK_READ` on an already-read article | **done and passing** — widening reversibility did not widen what counts as a change |
| `ArticleStateMachineUndoTest` `:314`, `:352`, `:385` | keep unchanged | **unedited and passing.** D1's new field did not leak into the `SAVE`/`DISMISS` path. |
| `UiStateMapperTest.kt:543-549` | extend; the four existing assertions keep their expected values | **done, unedited**, plus three new cases. `null → false`, `SAVE → true`, `DISMISS → true` all still hold under `undoAction != null`. |
| `AppViewModelTest` — cases asserting no offer for the three actions | each reported, then updated; count not knowable at design time | **the count is seven and it is now named** (`slices.md`, *Known consumers*). Established empirically by implementing the domain change and reading the gate. **Six passed unedited.** |
| `AppViewModelTest` `a labeled button press raises the same offer as a swipe` | — | **the seventh, reported then *strengthened*.** `MARK_READ` moved from asserting no offer to asserting `undoAvailable` plus the specific `MARKED_READ` message; `OPEN`'s branch untouched. Coverage increased, not reduced. |
| `ArticleStateMachineTest` (23 forward cases) | expected unchanged | **unedited and passing** |
| `PreferenceLearningTest` | expected unchanged, "11 cases" | **unedited and passing.** The count was wrong and is corrected — the file holds two classes, `PreferenceLearningDeltaTest` (parameterized) and `PreferenceLearningTest` (10 cases). D5 counted `@Test` annotations across the file. Flagged by the implementer against the gate XML; nothing is skipped. |

## 5. Departures from the plan

**Two, both found by the implementer stopping rather than proceeding, neither reaching shipped code.**
That is now the fifth and sixth such finding across three waves, and the fourth consecutive wave in which
the most valuable findings came from somewhere other than the review gate.

### 5.1 The slice plan was unimplementable in both directions — re-cut at `7c6c30e`

The original plan cut by layer: domain, then the offer, then the copy. **Both boundaries were crossed by a
dependency running the other way.**

- **Slice 1 → slice 2, at runtime.** Widening `reversibleActions` makes the domain produce records for the
  three new actions. Those records reach `AppViewModel.raiseUndoOffer`, whose
  `error("Only Save and Dismiss can raise an Undo offer")` lives in **slice 2's** file. The domain-only
  slice was implemented, its own 21 tests passed, and the full gate returned **292 tests, 7 failures**,
  every one of them that `IllegalStateException`. A domain-only slice could not be green on this tree.
- **Slice 2 → slice 3, at compile time.** The plan **stated this itself** and called it slice 3's RED:
  slice 2's three new `PendingUndoMessage` cases break `IntentionalReadingApp.kt:185-192`'s exhaustive
  `when` until slice 3 lands. A slice cannot both require a green gate and be the next slice's RED. *The
  plan recorded its own contradiction and read it as a feature.*

**Resolved by inverting the order**, owner-approved 2026-08-31: land the sinks while the set is still
narrow, so the new cases are unreachable and the tree stays green, then widen the set. Each slice is
independently green with a real failing-first commit. `spec.md` and `design.md` did not change — same work,
other order. Slice B's already-written tests and implementation were preserved as patches and re-landed as
its own commits; the branch was reset to `81a03d6` only so slice A could have a green gate beneath it.

> **The root is the one `waves/wave-c-note.md` §7 named, `014/evidence.md` §7 restated, and
> `execution-model.md` §2 still does not model: the plan was drawn by *who writes a file*, never by *who
> depends on it*.** Item 006 met it as a freeze rule, item 014 as a compile boundary, and this item as a
> **runtime** one. Three instances, three costumes, one root. **The §2 amendment owed at wave close must
> cover a runtime edge as well as an assertion edge** — it is not enough to ask who asserts against a hub
> file; it must also ask what that file's output flows into.

### 5.2 `design.md` D5 claimed a complete list and was stale, not wrong

D5 said "Complete as the tree stands today" — and it was, against the tree the design pass read. Item 014
then created `reversible actions carry undo state without caller eligibility`, whose loop at `:49` asserts
that `MARK_READ` carries no undo record. Amendment 8 overturns that.

**Caught at slice 1's preflight, before a single file was edited.** The implementer reported it and stopped
rather than absorbing it. The case's subject is *caller eligibility*, not the reversible set, and that
subject survives untouched: `SAVE` still carries a record with no eligibility argument, `OPEN` still
carries none. Authorized at `81a03d6` and recorded as a D5 row with its reason.

> **A design pass's assertion enumeration expires when the item beneath it merges.** D5 was accurate when
> written and stale when used, and no gate distinguishes the two. The report-before-editing protocol is what
> caught it — the same protocol wave D's *Lessons* item 3 was written to establish.

## 6. Slice reviews

`/feature-review` in slice mode, per slice.

**Slice A — PASS, 2026-08-31.** Range `7c6c30e..bebe0b6`. Scope held to exactly the six declared files; no
`domain/state/**`, no `ui/screens/**`, no dependency changes; all four `expectDiscoverHead = true` sites
intact. Strings match `spec.md` §5 verbatim and none bakes in "— Undo". **The one real risk in the diff was
checked against the code rather than the design note:** `undoAvailable = undoAction != null` is only safe if
the ViewModel can never hold a record for a non-reversible action. Traced — `undoAction` is
`undoRecord?.action` (`AppViewModel.kt:710`), and `undoRecord` is assigned in exactly one place (`:508`)
from `transition.undoRecord`, which the state machine builds only for `action in reversibleActions`. D2's
reasoning is true of the code. Test diff against `main` was +36 lines with zero deletions.

Two notes forward rather than findings. The new mapper tests are deliberately thin — asserting `OPEN →
false` at the mapper would reinstate the duplicate rule D2 exists to delete, so that scenario correctly
relocates to slice B's `only Open is not reversible`, where it landed. And dropping the `else` makes the
`when` exhaustive over all six `ArticleAction` cases, so a future action fails to compile rather than being
silently swallowed — a small improvement over what it replaced.

**Slice B — PASS, 2026-08-31.** Range `3cab362..ed80df4`. The product diff is `design.md` D1 and D2
exactly. `REMOVE` has no entry in `preferenceReversals` or `preferenceEvents` — its arithmetic is none in
both directions, per `contracts.md` §24.

**Proof discipline held without a second reminder.** The `MARK_UNREAD` round trip asserts the restored
record with `assertSame` and the preferences entry-for-entry by source and topic id. The cross-pane case
uses distinct source and topic ids for A and B **plus unrelated source and topic controls**, asserts exact
`PreferenceEntry` values including interaction counts, and pins A's weights to literals under "must move
exactly once in total" — it closes three scenarios in one test: offer survival across a destination change,
single-slot replacement, and no double-apply.

**Checked for vacuity**, given this programme's history with it: `assertPreferencesEqualEntryForEntryById`
asserts whole-map equality *and* five named-id equalities over a fixture with non-trivial weights and
interaction counts. Not a tautology.

Every deletion in the test diff is one of the three authorized sites, and the third is a strengthening.

## 7. Walkthrough

Per `spec.md` §6.3. **Not yet run.** Batched with items 015 and 014 at the end of the wave, against merged
`main` (`execution-model.md` §4.5, §6). Item 012's was already driven at its slice 2 and is not repeated.

Three owner checkpoints ride on it and remain open:

1. **The three new toast strings** (`spec.md` §5), judged on screen at §6.3 step 7.
2. **The widened Undo against real accumulated history, not fresh state** (`waves/wave-d.md` checkpoint 3).
   Every wave so far has found its most valuable defects this way and none of them by a gate.
3. **Wave sign-off** against merged `main`.

§6.3 step 3 is the one most likely to be wrong on screen — undoing `MARK_UNREAD` must send the source
weight back **up**, because it re-applies rather than reverses. It is the only place in V1 where undoing an
action applies a signal.
