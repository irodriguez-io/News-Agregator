# 014 — slice plan

**Size: S — two slices**, split at the domain/UI boundary so each starts from a small context window and
each closes with a real RED. One branch (`feat/014-android-undo-offer-surfaces`), one PR targeting `main`.

**This plan is a forecast.** It is written against a tree in which item 015 has merged but has not been
read. `spec.md` §6 lists the six assumptions and `/feature-implementation` Step 0.4 must check every one
before slice 1 begins. For this item that step is not a formality.

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

- **`reversibleActions` stays `setOf(SAVE, DISMISS)`.** This item changes who asks, not what is possible.
  `MARK_READ`, `MARK_UNREAD` and `REMOVE` are item 016's, including Discover's own *Mark read* button.
- **`ArticleAction.OPEN` never acquires an undo record** (`spec.md` §1.4).
- **One offer at a time.** Any eligible action replaces the slot, exactly as a second swipe does today
  (`contracts.md` §31).
- **No new string resource, no new `PendingUndoMessage` case, no change to `UiStateMapper`.** Item 016.
- **015's `expectDiscoverHead = true` is preserved at all four Discover lambdas.** Removing the `undoable`
  argument from the same call sites must not remove the other one.
- **`ui/components/ArticleCard.kt`, `ui/gesture/**`, `ui/screens/**` and the `androidTest` source set are
  not touched.** 013's commits and guards stay byte-identical.
- **`docs/v1/**` is not edited.** Amendment 8 is already committed when the branch is cut.
- **Do not start from 013's diagnosis** (`spec.md` §1.1). There is no gesture failure here.
- **Prove every reversal by the article id**, never by a count and never by "a weight moved"
  (`waves/wave-d.md`; item 015's trap applies here too).

---

> **Corrected 2026-08-31, at dispatch, and the correction is the supervisor's error to own.** Slice 1
> originally excluded `AppViewModelTest.kt`. But removing the `undoable` parameter from
> `AppViewModel.onArticleAction` and `launchArticleAction` breaks **33 call sites across roughly 20 cases**
> in that file, so slice 1 could not compile its own test sources, let alone reach a green gate. **The
> implementer found it and stopped rather than either widening its own scope or editing tests it was not
> authorized to touch.**
>
> This is item 006's failure in a different costume, and it has the same root that `waves/wave-c-note.md`
> §7 names: the plan was drawn by **who writes a file**, never by **who asserts against it**. A signature
> change is not confined to the layer that declares it.
>
> Slice 1 now carries the mechanical argument removal in `AppViewModelTest.kt`; slice 2 keeps every
> assertion change. The item's scenarios, definition of done and shipped behaviour are unchanged.

## Slice 1: the state machine builds an undo record for any reversible action

- **Scenarios:** *a swipe behaves exactly as it does today*, *Read article still offers nothing*, *Mark read
  still offers nothing*, *an idempotent press offers nothing* (`spec.md` §4).
- **Files:**
  - `«pkg»/domain/state/ArticleStateMachine.kt` — `:144`, drop the `undoable &&` conjunct; remove the
    `undoable` parameter from `transition`.
  - `«pkg»/ui/AppViewModel.kt` — remove `undoable` from `onArticleAction` and `launchArticleAction` and from
    the `ArticleStateMachine.transition` call; `persistArticleTransition` raises the offer from
    `transition.undoRecord` unconditionally (`design.md` D1). **Keep `expectDiscoverHead` exactly as 015
    left it.**
  - `«pkg»/ui/IntentionalReadingApp.kt` — remove `undoable = true` at `:290` only.
  - `…/test/…/domain/state/ArticleStateMachineUndoTest.kt` — per `design.md` D5.
  - `…/test/…/ui/AppViewModelTest.kt` — **mechanical only**: drop the `undoable` argument from every call
    that passes it. No assertion, expected value or test name changes here; those are slice 2's.
- **Failing-first commit:** a new `ArticleStateMachineUndoTest` case asserting that a `SAVE` committed
  **without** any eligibility argument carries an undo record naming the previous state, and that `OPEN` and
  `MARK_READ` still carry none. RED today because `undoable` defaults to `false`.
- **Definition of done:**
  - `transition` has no `undoable` parameter and builds a record for `SAVE` and `DISMISS` from any caller.
  - `OPEN` and `MARK_READ` carry no record; `only save and dismiss are reversible` (`:99`) still passes.
  - An idempotent no-op still carries no record (`:127`).
  - `a commit that is not marked undo-eligible offers nothing` (`:85`) is deleted, with `design.md` D5's
    reason in the commit message.
  - **`AppViewModelTest.kt` compiles and passes with the argument removed and nothing else changed.** Report
    the number of call sites touched. Any case that needs more than the argument dropped belongs to slice 2
    — name it and leave it.
  - Both gates green, `test-results` deleted first, count recorded at the moment of the run.
- **Status:** done — `fa96b67` (RED) + `8fa59c5` (GREEN), slice review PASS 2026-08-31

## Slice 2: every Discover surface raises the offer

- **Scenarios:** *the Save for later button raises the offer*, *the Not interested button raises the offer*,
  *a button press and a swipe compete for the one slot*, *an action whose write fails offers nothing*
  (`spec.md` §4).
- **Files:**
  - `…/test/…/ui/AppViewModelTest.kt` — the replacements and rewrites named in `design.md` D5.
  - `«pkg»/ui/AppViewModel.kt` and `«pkg»/ui/IntentionalReadingApp.kt` only if slice 1 left anything.
- **Failing-first commit:** `a labeled button press raises the same offer as a swipe` — a `SAVE` and a
  `DISMISS` driven through the plain `onArticleAction` path each raise an offer with the right
  `PendingUndoMessage`, and `OPEN` and `MARK_READ` raise none. Written before slice 1's implementation
  reaches it, or RED against slice 1's tree if slices are committed in order.
- **Definition of done:**
  - A `SAVE` and a `DISMISS` from the plain path each raise an offer, and undoing each restores the record
    **identified by article id** and reverses only that article's signal.
  - A button-sourced action followed by a swipe leaves the slot holding the swipe, and undoing reverses the
    swipe while the earlier action stands — asserted by both article ids.
  - A `SAVE` whose write fails raises no offer and announces the failure.
  - `a labeled button press is still not undo-eligible` (`:978`) and `launchArticleAction threads undo
    eligibility to the slot` (`:1073`) are replaced per `design.md` D5, each with its reason in the commit
    message.
  - Both gates green; count recorded at the moment of the run.
- **Status:** done — `86ec7f4` + `d97ec2d`, slice review PASS 2026-08-31

---

## On existing assertions

`design.md` D5 is the complete list of what changes and why, and it is a list of **specific cases with
specific reasons** — not a freeze rule. Item 006 shipped "no existing assertion may be edited" and became
unimplementable at dispatch (`waves/wave-c-note.md` §2); that sentence is not written here.

Two obligations follow from that:

1. **Any test not on D5's list that fails must be reported before it is edited**, with its name and the
   reason it moved.
2. **Every deletion or replacement carries its reason in the commit message**, so that a reviewer reading
   the diff alone can see that the assertion was overturned by Amendment 8 rather than dropped because it
   was inconvenient.
