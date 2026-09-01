# 016 — slice plan

**Size: M — two slices**, cut on the dependency line: **wire what receives an undo record before you make
records.** One branch (`feat/016-android-reversible-actions`), one PR targeting `main`.

`«pkg»` = `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`; JVM tests under
`android/app/src/test/kotlin/io/irodriguez/intentionalreading/`.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug
```

---

## Re-cut 2026-08-31, at slice 1's gate — read this before anything else

**The original three-slice plan was unimplementable and this section replaces it.** It was cut by layer —
domain, then offer, then copy — and both boundaries were crossed by a dependency running the other way:

- **Slice 1 → slice 2, at runtime.** Widening `reversibleActions` makes the domain produce records for
  `MARK_READ`, `MARK_UNREAD` and `REMOVE`. Those records reach `AppViewModel.raiseUndoOffer`, whose
  `error("Only Save and Dismiss can raise an Undo offer")` branch lives in **slice 2's** file. The domain-only
  slice was implemented, its own 21 tests passed, and the full gate returned **292 tests, 7 failures**, every
  one of them that `IllegalStateException`. A domain-only slice cannot be green on this tree.
- **Slice 2 → slice 3, at compile time.** The original plan **stated this itself** and called it slice 3's
  RED: slice 2's three new `PendingUndoMessage` cases make `IntentionalReadingApp.kt:185-192`'s exhaustive
  `when` fail to compile until slice 3 lands. A slice cannot both require a green gate and be the next
  slice's RED.

**The root is the wave's own open lesson.** `waves/wave-d.md`'s *Lessons* item 1 — `execution-model.md` §2's
collision matrix orders hub files by **who writes them** and asks nobody who **depends on them**. Item 006
met it as a freeze rule, item 014 as a compile boundary, and this item as a runtime one. **Third instance,
same root**, and the §2 amendment owed at wave close must cover a runtime edge as well as an assertion edge.

**The fix is to invert the order**, approved by the owner on 2026-08-31. Land the sinks — the enum, the
mapping, the copy — while `reversibleActions` is still narrow, so the new cases are unreachable and the tree
stays green. Then widen the set, and everything downstream already handles it. Each slice is independently
green and each has a real failing-first commit. Nothing in `spec.md` or `design.md` changes: D2, D3 and the
§2 arithmetic are the same work in the other order.

---

## Fixed for this item — do not re-decide these mid-implementation

- **`spec.md` §2's table is the arithmetic.** It is settled at design time, ratified by the owner on
  2026-08-31 and carried into Amendment 8. If the implementation cannot satisfy a row, stop and report — do
  not adjust the row.
- **`REMOVE` moves no weight, in either direction.** `contracts.md` §24, `spec.md` §2.1.
- **Undo of `MARK_UNREAD` re-applies the Mark Read signal.** `spec.md` §2.2, `design.md` D1.
- **`ArticleAction.OPEN` never becomes reversible**, and `raiseUndoOffer` keeps an `error` branch for it.
- **`allowedFrom`, `isIdempotentNoOp` and every forward transition are unchanged.** This item changes what
  can be taken back, not what can be done.
- **`PreferenceLearning.apply` and `.reverse` are not modified**, and the clamp asymmetry in `spec.md` §2.3
  is not fixed here.
- **No per-pane affordance.** `ui/screens/readlater/**` and `ui/screens/history/**` are not touched
  (`design.md` D6).
- **One undo record at a time**, memory only, cleared by import and reset exactly as today.
- **`docs/v1/**` is not edited.** Amendment 8 is already committed.
- **Prove every reversal by the article id and every weight by source id and topic id** — never by an
  aggregate, never by "a weight moved" (`waves/wave-d.md`; item 015's trap).

---

## Slice A: the sinks — the offer, its availability, and the copy

Everything that consumes an undo record, landed while the domain still produces none for the new actions.
**The tree stays green throughout:** `reversibleActions` is untouched, so the three new `PendingUndoMessage`
cases are unreachable and `undoAvailable = undoAction != null` is behaviour-identical to the line it
replaces while only `SAVE` and `DISMISS` can produce a record.

- **Scenarios:** *each new action's toast names what it did*, and the availability half of *Undo remains
  unavailable for Open* (`spec.md` §4).
- **Files:**
  - `«pkg»/ui/AppUiState.kt` — three `PendingUndoMessage` cases (`design.md` D3).
  - `«pkg»/ui/AppViewModel.kt` — `raiseUndoOffer`'s mapping and its `error` branch narrowed to `OPEN`
    alone (`design.md` D3). **Nothing else in this file**, and `expectDiscoverHead` is left exactly as item
    015 left it.
  - `«pkg»/ui/state/UiStateMapper.kt` — `:70` becomes `undoAvailable = undoAction != null` (`design.md` D2).
    This deletes a second copy of `reversibleActions` living in a different layer; that deletion is the
    point.
  - `android/app/src/main/res/values/strings.xml` — the three strings in `spec.md` §5.
  - `«pkg»/ui/IntentionalReadingApp.kt` — the `PendingUndoMessage` → `stringResource` mapping at
    `:185-192`, extended to stay exhaustive with no `else`. **Nothing else in this file.**
  - `…/test/…/ui/state/UiStateMapperTest.kt`.
- **Must not be touched in this slice:** `domain/state/**` — in particular `reversibleActions`,
  `preferenceReversals` and `UndoRecord`. If this slice needs a domain change to go green, stop and report:
  the re-cut is wrong.
- **Failing-first commit:** three new `UiStateMapperTest` cases asserting `undoAvailable` is true for
  `MARK_READ`, `MARK_UNREAD` and `REMOVE`. **RED against the current tree** because `:70` reads
  `undoAction == SAVE || undoAction == DISMISS`. The enum and the string mapping have no assertable RED at
  this layer — a `when` over an enum is where the compiler is the test, and `stringResource` needs a
  composition the CI source set does not run — so they ride in the implementation commit and are stated
  plainly rather than dressed up.
- **Definition of done:**
  - `PendingUndoMessage` has five cases and the `IntentionalReadingApp` mapping is exhaustive with no `else`.
  - Three strings exist with `spec.md` §5's text.
  - `raiseUndoOffer` maps all five reversible actions and `error`s on `OPEN` alone. The `error` branch is
    unreachable — `OPEN` is not in `reversibleActions` — and is kept as the guard that says so.
  - `undoAvailable` is true for all five reversible actions and false with no record. `UiStateMapperTest`'s
    four existing assertions at `:543-549` **keep their expected values unedited** — `null → false`,
    `SAVE → true`, `DISMISS → true` all stay true under `undoAction != null`. If any of them moves, stop and
    report.
  - **The whole suite is green at 286 tests plus whatever this slice adds, with zero failures**, and
    `:app:assembleDebug` is green. This slice changes no behaviour that any existing test observes.
  - Both gates green, `test-results` deleted first, count recorded at the moment of the run.
- **Status:** pending

## Slice B: the source — the reversal arithmetic

- **Scenarios:** the five arithmetic scenarios in `spec.md` §4 — Remove, Mark read with a signal, Mark read
  without one, Mark unread with a signal, Mark unread without one — asserted against `transition` and
  `reverse` directly per `spec.md` §6.2; plus *Discover's Mark read is reversible*, *the offer survives the
  reader changing destination*, *acting in a second pane does not double-apply or double-reverse*, and *an
  action whose write fails offers nothing*.
- **Files:**
  - `«pkg»/domain/state/UndoRecord.kt` — `preferenceReapplication` and the `init` require (`design.md` D1).
  - `«pkg»/domain/state/ArticleStateMachine.kt` — `reversibleActions`, `preferenceReversals[MARK_READ]`,
    `preferenceReversals[action]` instead of `.getValue(action)`, the `preferenceSignalReversed` flag in the
    `MARK_UNREAD` branch, and `reverse`'s mirror branch (`design.md` D1, D2).
  - `…/test/…/domain/state/ArticleStateMachineUndoTest.kt` — new cases plus the rewrites in `design.md` D5.
  - `…/test/…/ui/AppViewModelTest.kt` — the cross-pane case, and the updates named below.
- **Must not be touched in this slice:** `res/**`, `ui/AppUiState.kt`, `ui/IntentionalReadingApp.kt`,
  `ui/state/UiStateMapper.kt`, and `raiseUndoOffer` — slice A owns all of them and they are already correct.
- **Prior work to re-land, not to rewrite.** Slice B's failing-first tests and its domain implementation were
  both written on 2026-08-31 against the original slice 1 and are preserved as patches; they were removed
  from the branch only so that slice A could have a green gate beneath it. Apply the test patch as the RED
  commit, **verify it is red for the intended reason**, then apply the implementation. Review both as your
  own work — the arithmetic is settled but the assertions are not exempt from scrutiny.
- **Failing-first commit:** the arithmetic cases, RED because `reversibleActions` excludes all three actions
  and no record is produced. **The `MARK_UNREAD` round trip is the case that matters** — mark read, mark
  unread, undo, and the preferences map must equal the map from before the mark-unread, **entry for entry,
  keyed by source id and topic id**. The cross-pane case in `design.md` D4 joins it: mark A read in Read
  Later, switch to History, mark B unread, undo — B restored with its Read signal re-applied and **A's
  weights moved exactly once in total**.
- **Definition of done:**
  - Every row of `spec.md` §2 holds, asserted against the comparator rather than through a consumer
    (`waves/wave-c-note.md` §2).
  - Undoing `REMOVE` restores a `SAVED` record and leaves the preferences map identical, entry for entry.
  - Undoing `MARK_READ` reverses the Mark Read signal only when the forward transition applied it.
  - Undoing `MARK_UNREAD` restores `signalsApplied.read = true` **and** re-applies the Mark Read weights;
    with no signal applied forward, nothing moves either way.
  - Undoing Discover's `MARK_READ` on an opened record leaves the First Open signal applied.
  - The cross-pane case passes, asserted by article id and by source and topic id.
  - A failed write raises no offer and announces the failure.
  - `only save and dismiss are reversible` (`:119`) is rewritten per `design.md` D5, and
    `reversible actions carry undo state without caller eligibility` (`:30`) drops `MARK_READ` from its
    non-reversible loop per D5's added row. **Both carry their reason in the commit message.**
  - `:314`, `:352` and `:385` still pass **unedited** — if any of them fails, the new field has leaked into
    the `SAVE`/`DISMISS` path, so stop and report.
  - `ArticleStateMachineTest` (23 forward cases) and `PreferenceLearningTest` (11 cases) pass unedited.
  - **The seven tests that the original slice 1 broke come back green**, because slice A already put the
    mapping in place. They are the ones that asserted no offer for these three actions, and they are named
    in the *Known consumers* list below — each is reported before it is edited, with its reason.
  - Both gates green, `test-results` deleted first, count recorded at the moment of the run.
- **Status:** pending

### Known consumers — the list `design.md` D5 said was not knowable

Established empirically on 2026-08-31 by implementing the domain change and reading the gate. These seven
failed with `IllegalStateException: Only Save and Dismiss can raise an Undo offer`. **Slice A removes that
exception**, so under the re-cut they are expected to pass unedited or to need only the offer they now
correctly receive. Any that still needs an edit is reported with its reason before it is touched.

`AppViewModelTest`:
- `every article action persists its state-machine transition`
- `a labeled button press raises the same offer as a swipe`
- `History row actions reopen in place then mark unread back to Read Later`
- `Read Later row actions update queue and History projections immediately after writes`
- `the guard is confined to the Discover card`
- `pre-learning unread after post-learning open and save preserves exactly the remaining signals`

`LaunchNightModeTest`:
- `the platform is told once per process, not once per action`

---

## On existing assertions

`design.md` D5 is the list, and it is specific cases with specific reasons — **not a freeze rule.** The
sentence "no existing assertion may be edited" is what made item 006 unimplementable and it is not written
here. Two obligations follow:

1. **Any test not on D5's list that fails must be reported before it is edited**, with its name and why it
   moved. This already paid for itself once: slice 1's preflight found
   `reversible actions carry undo state without caller eligibility` — created by item 014 *after* the design
   pass read the tree — and it is now a D5 row. Three named files are expected to pass entirely unedited:
   `ArticleStateMachineTest`, `PreferenceLearningTest`, and `ArticleStateMachineUndoTest.kt:314/:352/:385`.
   A failure in any of them means the change reached further than the design intends.
2. **Every rewrite carries its reason in the commit message**, so a reviewer reading the diff alone can see
   that Amendment 8 overturned the assertion rather than that it was inconvenient.
