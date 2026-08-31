# 016 — slice plan

**Size: M — three slices**, split so that the arithmetic is settled and asserted before anything on screen
changes. One branch (`feat/016-android-reversible-actions`), one PR targeting `main`.

**This plan is a forecast.** It is written against a tree in which items 015 and 014 have merged but have not
been read. `spec.md` §7 lists eight assumptions; **assumption 2 is the one that can change this plan's
shape** and `/feature-implementation` Step 0.4 must check it first.

`«pkg»` = `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`; JVM tests under
`android/app/src/test/kotlin/io/irodriguez/intentionalreading/`.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug
```

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

## Slice 1: the reversal arithmetic, in the domain layer only

- **Scenarios:** the five arithmetic scenarios in `spec.md` §4 — Remove, Mark read with a signal, Mark read
  without one, Mark unread with a signal, Mark unread without one — asserted against `transition` and
  `reverse` directly, per `spec.md` §6.2.
- **Files:**
  - `«pkg»/domain/state/UndoRecord.kt` — `preferenceReapplication` and the `init` require (`design.md` D1).
  - `«pkg»/domain/state/ArticleStateMachine.kt` — `reversibleActions`, `preferenceReversals[MARK_READ]`,
    `preferenceReversals[action]` instead of `.getValue(action)`, the `preferenceSignalReversed` flag in the
    `MARK_UNREAD` branch, and `reverse`'s mirror branch (`design.md` D1, D2).
  - `…/test/…/domain/state/ArticleStateMachineUndoTest.kt` — new cases plus the rewrites in `design.md` D5.
- **Must not be touched in this slice:** anything under `ui/`, `res/`, or the forward-transition tests.
- **Failing-first commit:** the new `ArticleStateMachineUndoTest` cases, RED against the current tree because
  `reversibleActions` excludes all three actions and no record is produced. **The `MARK_UNREAD` round trip is
  the case that matters** — mark read, mark unread, undo, and the preferences map must equal the map from
  before the mark-unread, **entry for entry, keyed by source id and topic id**.
- **Definition of done:**
  - Every row of `spec.md` §2 holds, asserted against the comparator rather than through a consumer
    (`waves/wave-c-note.md` §2).
  - Undoing `REMOVE` restores a `SAVED` record and leaves the preferences map identical, entry for entry.
  - Undoing `MARK_READ` reverses the Mark Read signal only when the forward transition applied it.
  - Undoing `MARK_UNREAD` restores `signalsApplied.read = true` **and** re-applies the Mark Read weights;
    with no signal applied forward, nothing moves either way.
  - Undoing Discover's `MARK_READ` on an opened record leaves the First Open signal applied.
  - `only save and dismiss are reversible` (`:99`) is rewritten per `design.md` D5, with its reason in the
    commit message. `:314`, `:352` and `:385` still pass **unedited** — if any of them fails, the new field
    has leaked into the `SAVE`/`DISMISS` path, so stop and report.
  - `ArticleStateMachineTest` (23 forward cases) and `PreferenceLearningTest` (11 cases) pass unedited.
  - Both gates green, `test-results` deleted first, count recorded at the moment of the run.
- **Status:** pending

## Slice 2: the offer and its availability

- **Scenarios:** *the offer survives the reader changing destination*, *acting in a second pane does not
  double-apply or double-reverse*, *an action whose write fails offers nothing*, *Undo remains unavailable for
  Open* (`spec.md` §4).
- **Files:**
  - `«pkg»/ui/AppUiState.kt` — three `PendingUndoMessage` cases.
  - `«pkg»/ui/AppViewModel.kt` — `raiseUndoOffer`'s mapping and its narrowed `error` branch (`design.md` D3).
    **Nothing else in this file**, and `expectDiscoverHead` is left exactly as item 015 left it.
  - `«pkg»/ui/state/UiStateMapper.kt` — `:70` becomes `undoAction != null` (`design.md` D2).
  - `…/test/…/ui/AppViewModelTest.kt`, `…/test/…/ui/state/UiStateMapperTest.kt`.
- **Failing-first commit:** the cross-pane case in `design.md` D4 — mark A read in Read Later, switch to
  History, mark B unread, undo — asserting B restored with its Read signal re-applied and **A's weights moved
  exactly once in total**. RED because no offer is raised for either action today.
- **Definition of done:**
  - Each of `MARK_READ`, `MARK_UNREAD` and `REMOVE` raises an offer with its own `PendingUndoMessage`.
  - `undoAvailable` is true for all five reversible actions and false with no record; `UiStateMapperTest`'s
    four existing assertions keep their expected values.
  - The cross-pane case passes, asserted by article id and by source and topic id.
  - A failed write raises no offer and announces the failure.
  - **Before editing them, list every existing `AppViewModelTest` case that asserted no offer for these three
    actions** — `design.md` D5 says the count is not knowable from this document — and report the list with
    each case's reason.
  - Both gates green; count recorded at the moment of the run.
- **Status:** pending

## Slice 3: the copy

- **Scenarios:** *each new action's toast names what it did* (`spec.md` §4).
- **Files:**
  - `android/app/src/main/res/values/strings.xml` — the three strings in `spec.md` §5.
  - `«pkg»/ui/IntentionalReadingApp.kt` — the `PendingUndoMessage` → `stringResource` mapping at `:185-192`.
    **Nothing else in this file.**
- **Failing-first commit:** the mapping is exhaustive over `PendingUndoMessage`, so slice 2's three new cases
  make `IntentionalReadingApp.kt` **fail to compile** until this slice lands. That is the RED, and it is
  stated rather than dressed up: a `when` over an enum is where the compiler is the test. Nothing else at
  this layer is assertable — `stringResource` needs a composition, and the instrumented source set is out of
  CI.
- **Definition of done:**
  - Three strings exist with `spec.md` §5's text, and the mapping is exhaustive with no `else`.
  - Both gates green.
  - The strings are read on screen at `spec.md` §6.3 step 7 and the copy is the owner's call
    (`spec.md` §6.4).
- **Status:** pending

---

## If assumption 2 is false

`spec.md` §7 assumption 2: item 014 removed the `undoable` parameter and the offer now follows the record.

**If 014 instead landed as extra `undoable = true` arguments**, then widening `reversibleActions` raises no
offer anywhere, and slice 2 must also add `undoable = true` at the Read Later *Mark read* and *Remove* call
sites (`IntentionalReadingApp.kt:259-264`), the History *Mark unread* call site (`:303-305`) and Discover's
*Mark read* (`:283-285`). Slice 2's file list and DoD grow; slices 1 and 3 are unaffected.

**Re-plan before dispatching, do not absorb it silently.** Item 006 was designed against an unmerged tree and
failed at dispatch weeks after plan-mode approval (`waves/wave-c-note.md` §2), and this is the one place
where the same thing could happen here.

## On existing assertions

`design.md` D5 is the list, and it is specific cases with specific reasons — **not a freeze rule.** The
sentence "no existing assertion may be edited" is what made item 006 unimplementable and it is not written
here. Two obligations follow:

1. **Any test not on D5's list that fails must be reported before it is edited**, with its name and why it
   moved. Three named files are expected to pass entirely unedited — `ArticleStateMachineTest`,
   `PreferenceLearningTest`, and `ArticleStateMachineUndoTest.kt:314/:352/:385` — and a failure in any of
   them is a signal that the change reached further than the design intends.
2. **Every rewrite carries its reason in the commit message**, so a reviewer reading the diff alone can see
   that Amendment 8 overturned the assertion rather than that it was inconvenient.
