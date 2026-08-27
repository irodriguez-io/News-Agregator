# 013 — slice plan (second revision, 2026-08-27)

Sized **S → one slice**. One item branch (`feat/013-android-undo-gesture-reset`), one PR targeting `main`.

Scenario names refer to `spec.md` §4. `«pkg»` = `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`;
JVM tests under `android/app/src/test/kotlin/io/irodriguez/intentionalreading/`; instrumented tests under
`android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/`.

**Every Gradle invocation needs both exported first** — `java` is not on this machine's `PATH` and a
worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

Head of this branch: **258 tests, 0 failures, `BUILD SUCCESSFUL`**. Delete `test-results` before every run
and read the `BUILD SUCCESSFUL` line, not the counts (`waves/wave-b-note.md` §7).

## What changed since the last plan, and why

The previous plan's Step 0 ran and **passed**: the defect reproduces with no Undo, confirmed against
pulled state documents (`investigation/step0-reproduction.md`). `spec.md` §1.2's diagnosis stands. The item
then stopped at the old §5.3, where seven pre-fix instrumented harnesses all passed against unchanged
production code — `ComposeTestRule` synchronizes away the exact window the symptom lives in.

Two things follow, and they are the whole of this revision:

- **The RED asserts the cause, not the symptom** (`design.md` D3, `spec.md` §5.3). It is now a
  gesture that spans a head-article change, which needs no clock control and fails first today.
- **The Save for later scenario is gone**, to item 014 (`design.md` D7).

Everything else is unchanged, including the fix in D1.

## Fixed for this item — do not re-decide these mid-implementation

- **Step 0 is closed.** Do not re-run it as a pre-fix gate; its result is evidence (`spec.md` §5.1). The
  same case returns after the fix as walkthrough step 1.
- **Do not try to reproduce the timing window in a Compose test.** `spec.md` §1.5 lists the seven
  variations that failed to. If the §5.3 test as written does not fail first, **stop and report** — do not
  reach back for a timing harness.
- **`«pkg»/ui/gesture/SwipeGesture.kt` is not touched.** It is already correct. `bf79c42` stays
  (`design.md` D2, D6).
- **`SwipeGestureTest.kt:164` stays byte-identical**, as do all four tests added by `bf79c42`.
- **The `DiscoverScreen` scroll effects are not touched.** One of them lengthens the window; changing
  scroll behaviour is item 012's ground (`design.md` D1).
- **The gesture state stays per-article.** A persistent handler must not mean a shared or reused state, and
  every per-article value the handler reads must come through current state rather than capture
  (`design.md` D5) — this is the likeliest way to get the fix wrong.
- **No new dependency.** `androidx.compose.ui.test.junit4` and `…ui.test.manifest` are already declared;
  `libs.versions.toml` and both `build.gradle.kts` files are untouched. In particular, do **not** add
  `androidx.test.uiautomator` (`design.md` D3, rejected alternative).
- **No test-only parameter on `ArticleCard`** (`design.md` D3, rejected alternative).
- **Instrumented tests stay out of CI.** The new test is a local, on-demand guard on the same terms as
  `MainActivityLaunchSmokeTest` (002 slice 4).
- **The Save for later button is out of scope**, including in the walkthrough. Item 014.
- **Escalate rather than infer.** If the instrumented test cannot be made to fail first for the right
  reason, stop and report — do not weaken it and do not lean on the walkthrough instead (`design.md` D3).

---

## Slice 1: a Discover card accepts a swipe as soon as it is on screen

- **Scenarios:** `spec.md` §4 in full — "a card accepts a swipe immediately after the deck advances",
  "a card accepts a swipe immediately after Undo returns it", "the gesture state is still per-card",
  "the touch handler survives a head-article change", "a gesture commits against the card it started on",
  "a committing card still refuses a second gesture".
- **RED first.** Write `spec.md` §5.3's instrumented test and watch it fail against unchanged production
  code, with **no commit recorded** — that is the right reason. Any other failure mode, or a pass, is a
  stop-and-report.
- **Files:** `«pkg»/ui/components/ArticleCard.kt` (the `pointerInput` key, the per-article holder, and how
  the handler reads it), and a new instrumented test under `androidTest/…`.
- **Must not touch:** `«pkg»/ui/gesture/SwipeGesture.kt`, `«pkg»/ui/screens/discover/DiscoverScreen.kt`,
  `«pkg»/domain/**`, `«pkg»/ui/AppViewModel.kt`, `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/data/**`,
  `res/**`, any Gradle file, `docs/v1/**`, anything outside `android/`.
- **Reuse:** `rememberUpdatedState`, already imported and used in this file for `onSwipeCommit`
  (`ArticleCard.kt:102`). `MainActivityLaunchSmokeTest` as the shape and terms of a local instrumented
  guard. The existing `SwipeGesture.State` API unchanged.
- **Reference:** `design.md` D1 for the fix and its two rejected alternatives, **D5 for the stale-capture
  hazard the fix creates**, D3 for why the test asserts the cause; `spec.md` §1.2 for the measured evidence
  and §1.5 for what already failed.
- **Definition of done:**
  - The §5.3 instrumented test **failed first with no commit recorded**, and passes after the fix.
  - `./gradlew :app:connectedDebugAndroidTest` green locally against the running `Pixel_10`.
  - Both JVM gates green at **258** tests plus any added, 0 failures.
  - `SwipeGesture.kt` untouched; `SwipeGestureTest.kt` untouched; no existing assertion changed anywhere.
  - No stale captures: every per-article value the handler reads is read through current state (D5), and
    the commit carries the article the gesture started on.
  - The gesture state is still constructed per article — shown by the "gesture state is still per-card"
    scenario.
  - `spec.md` §5.4's walkthrough (1) and (2) run and recorded, with every swipe aimed at the card's **true
    bounds** from `uiautomator dump`, and each result confirmed against the pulled state document rather
    than by the card leaving the deck.
- **Status:** pending

---

## Ship bookkeeping this item creates

Handled at close, not inside the slice:

- `evidence.md` recording **all three passes** — the wrong diagnosis and the fix built on it, the right
  diagnosis with a verification design that could not work, and what finally held. The process lessons are
  the more valuable half of this item and must not be tidied away. In particular: *a green unit gate never
  had a chance here*, and *a test harness that synchronizes away timing cannot test timing*.
- **A correction to `specs/005-android-preference-learning/evidence.md`.** Its walkthrough section says
  *"nothing surfaced where the assertion held but the screen was wrong."* That is not quite true: this
  item's defect was live throughout the 005 walkthrough, and several taps that "did not register" were
  attributed there to the undo toast's 4.5 s timeout when at least some were this defect discarding the
  touch. 005's recorded results stand — every step was confirmed against a pulled state diff — but the
  attribution was wrong and the file should say so, pointing at this item. *(Still owed; `f1f381b` recorded
  the obligation, it has not been discharged.)*
- **`backlog.md` gains item 014 — the Discover card's buttons in the same window.** `spec.md` §1.6 and
  `design.md` D7. Not yet diagnosed; it starts from its own Step 0 reproduction, and this item's history is
  the reason it does not inherit a cause. Number 014 is free: 012 is unscheduled, 013 is this item.
- `backlog.md`: 013 moves to Shipped. It has no Queued entry today — one is not needed on the way through.
- `backlog.md`: the `Debt` note about `DiscoverScreen`'s three scroll effects is updated to say they are
  now implicated in a real defect rather than a tidiness concern.
- `backlog.md`: the **queue-pane undo** entry owed from the first design pass — widening
  `ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS` and adding undo affordances to Read Later
  and History, needing its own specification amendment and design pass like item 012, citing
  `contracts.md` §23's two reversible corrective actions.
- **`Verification debt`: the instrumented suite is now two tests and still out of CI.** Record that the
  local `connectedDebugAndroidTest` run is the only thing that exercises either.
- Note for item 006, next in wave C: this item touches no file 006 touches.
