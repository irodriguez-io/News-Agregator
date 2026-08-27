# 013 — slice plan (rewritten 2026-08-27)

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

Fixed for this item — do not re-decide these mid-implementation:

- **Step 0 is a stop gate, not a formality.** `spec.md` §5.1: reproduce the defect with **two fast
  consecutive swipes and no Undo**, before writing anything. If it does not reproduce, **stop and report** —
  the diagnosis is wrong again and the item goes back to design. The first version of this item skipped
  this check and shipped a fix for the wrong cause.
- **`«pkg»/ui/gesture/SwipeGesture.kt` is not touched.** It is already correct. `bf79c42` stays
  (`design.md` D2).
- **`SwipeGestureTest.kt:164` stays byte-identical**, as do all four tests added by `bf79c42`.
- **The `DiscoverScreen` scroll effects are not touched.** One of them lengthens the window; changing
  scroll behaviour is item 012's ground (`design.md` D1).
- **The gesture state stays per-article.** A persistent handler must not mean a shared or reused state.
- **No new dependency.** `androidx.compose.ui.test.junit4` is already declared; `libs.versions.toml` and
  both `build.gradle.kts` files are untouched.
- **Instrumented tests stay out of CI.** The new test is a local, on-demand guard on the same terms as
  `MainActivityLaunchSmokeTest` (002 slice 4).
- **Escalate rather than infer.** If the instrumented test cannot be made to fail first for the right
  reason, stop and report — do not weaken it and do not lean on the walkthrough instead (`design.md` D3).

---

## Slice 1: a Discover card accepts touch as soon as it is on screen

- **Scenarios:** `spec.md` §4 in full — "a card accepts a swipe immediately after the deck advances",
  "a card accepts a swipe immediately after Undo returns it", "a card accepts a button press immediately
  after the deck changes", "the gesture state is still per-card", "a committing card still refuses a
  second gesture".
- **Step 0 — reproduce first.** `spec.md` §5.1's no-Undo consecutive-swipe case. Record what you observe.
  Stop and report if it does not reproduce.
- **Files:** `«pkg»/ui/components/ArticleCard.kt` (the `pointerInput` keying and how the current gesture
  state is read), and a new instrumented test under `androidTest/…`.
- **Must not touch:** `«pkg»/ui/gesture/SwipeGesture.kt`, `«pkg»/ui/screens/discover/DiscoverScreen.kt`,
  `«pkg»/domain/**`, `«pkg»/ui/AppViewModel.kt`, `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/data/**`,
  `res/**`, any Gradle file, `docs/v1/**`, anything outside `android/`.
- **Reuse:** `rememberUpdatedState`, already imported and used in this file for `onSwipeCommit`
  (`ArticleCard.kt:102`). `MainActivityLaunchSmokeTest` as the shape and terms of a local instrumented
  guard. The existing `SwipeGesture.State` API unchanged.
- **Reference:** `design.md` D1 for the decision and the two rejected alternatives; `spec.md` §1.2 for the
  measured evidence.
- **Definition of done:**
  - Step 0 reproduced and recorded, or the item stopped.
  - An instrumented regression test that **fails first for the right reason** — a touch injected during
    the window is not received — and passes after the fix.
  - `./gradlew :app:connectedDebugAndroidTest` green locally against the running `Pixel_10`.
  - Both JVM gates green at **258** tests plus any added, 0 failures.
  - `SwipeGesture.kt` untouched; `SwipeGestureTest.kt` untouched; no existing assertion changed anywhere.
  - The gesture state is still constructed per article — shown by the "gesture state is still per-card"
    scenario.
  - `spec.md` §5.4's walkthrough (a), (b) and (c) run and recorded, with every swipe and tap aimed at the
    card's **true bounds** from `uiautomator dump`, and each result confirmed against the pulled state
    document rather than by the card leaving the deck.
- **Status:** pending

---

## Ship bookkeeping this item creates

Handled at close, not inside the slice:

- `evidence.md` recording **both** diagnoses — the wrong one, the fix built on it, and the evidence that
  overturned it. The process lesson is the more valuable half of this item and must not be tidied away.
- **A correction to `specs/005-android-preference-learning/evidence.md`.** Its walkthrough section says
  *"nothing surfaced where the assertion held but the screen was wrong."* That is not quite true: this
  item's defect was live throughout the 005 walkthrough, and several taps that "did not register" were
  attributed there to the undo toast's 4.5 s timeout when at least some were this defect discarding the
  touch. 005's recorded results stand — every step was confirmed against a pulled state diff — but the
  attribution was wrong and the file should say so, pointing at this item.
- `backlog.md`: 013 moves to Shipped; the `Debt` note about `DiscoverScreen`'s three scroll effects is
  updated to say they are now implicated in a real defect rather than a tidiness concern.
- `backlog.md`: the **queue-pane undo** entry owed from the previous design pass — widening
  `ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS` and adding undo affordances to Read Later
  and History, needing its own specification amendment and design pass like item 012, citing
  `contracts.md` §23's two reversible corrective actions.
- Note for item 006, next in wave C: this item touches no file 006 touches.
