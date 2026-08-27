# 013 — slice plan

Sized **XS/S → one slice**. One item branch (`feat/013-android-undo-gesture-reset`), one PR targeting
`main`. The slice closes as a failing-first test commit plus an implementation commit.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`;
tests live under `android/app/src/test/kotlin/io/irodriguez/intentionalreading/`.

**Every Gradle invocation needs both of these exported first** — `java` is not on this machine's `PATH`,
and a worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

Baseline on `main` at `2613959`: **254 tests, 0 failures, `BUILD SUCCESSFUL`**, verified 2026-08-27. A
count read from `test-results` after a failed build is the *previous* run's count — delete the directory
first and read the `BUILD SUCCESSFUL` line (`waves/wave-b-note.md` §7).

Fixed for this item — do not re-decide these mid-implementation:

- **`SwipeGestureTest.kt:164` stays byte-identical.** *"a second gesture is refused while a commit is in
  flight"* is authoritative. The lock is correct while a commit is in flight; this item adds a release
  for when it resolves. Weakening that test is a finding, not a judgement call.
- **The lock-only release touches no travel** (`design.md` D1). `translationX` and `exitTranslationX` are
  not modified by it. That is what makes it safe to call on the success path.
- **It is called on both commit outcomes** — persisted and not persisted. Calling it only on success
  would leave the failure path correct by accident rather than by construction (`design.md` D2).
- **`restoreCard()` keeps the animation** and stays on the failure and lost-pointer paths only.
- **Which inputs offer Undo does not change.** The labeled buttons stay non-undoable and no keyboard
  shortcut is added (`spec.md` §1.1; 007 spec §1.1; 008 D8).
- **`docs/v1/**` is untouched.** This item proposes no amendment.
- **No new dependency.** `android/gradle/libs.versions.toml` is untouched.
- **Nothing under `«pkg»/domain/` is touched at all** — this is a UI-layer defect.
- **Escalate rather than infer.** If the failing-first test cannot reproduce the defect at the state
  level, stop and report: the diagnosis in `spec.md` §1.2 would be wrong and that matters more than the
  fix. Five escalations on item 005 were all correct.

---

## Slice 1: release the swipe lock when the commit resolves

- **Scenarios:** `spec.md` §4 in full — "a resolved commit releases the lock", "a resolved failed commit
  still restores the travel", "the lock still holds before the commit resolves", "releasing the lock does
  not fabricate an action", "releasing the lock leaves the travel alone".
- **Files:** `«pkg»/ui/gesture/SwipeGesture.kt` (the lock-only release),
  `«pkg»/ui/components/ArticleCard.kt` (call it from the `onSwipeCommit` completion callback at
  `:158-161`, on both outcomes), and the test `ui/gesture/SwipeGestureTest.kt`.
- **Must not touch:** `«pkg»/domain/**`, `«pkg»/ui/AppViewModel.kt`, `«pkg»/ui/state/UiStateMapper.kt`,
  `«pkg»/ui/screens/**`, `«pkg»/ui/IntentionalReadingApp.kt`, `«pkg»/data/**`, `res/**`, any Gradle file,
  `docs/v1/**`, anything outside `android/`.
- **Reuse:** the existing `commitInFlight`, `committedAction`, `restore()` and `reset()` members as they
  stand. This slice adds one method and one call site; it does not restructure the gesture object. A
  second flag tracking whether `commitInFlight` is still trustworthy is a defect, not a design.
- **Reference:** `spec.md` §1.2 for the mechanism, `design.md` D1 for why the reset is split rather than
  `restoreCard()` being made unconditional.
- **Definition of done:**
  - Both gates green; test count above 254 by the number of tests added.
  - A test proving a state whose commit resolved as persisted accepts a new gesture and can commit again.
  - A test proving a commit that resolved as **not** persisted still returns the travel home.
  - A test proving the lock-only release leaves `translationX` and `exitTranslationX` untouched.
  - A test proving `committedAction` still reports the committed action afterwards, and that a release
    which has travelled nowhere emits nothing.
  - `SwipeGestureTest.kt:164` byte-identical — verify with `git diff` on that hunk, not by eye.
  - `git grep` shows the lock-only release reached on **both** branches of the commit completion callback.
  - `spec.md` §5.2's walkthrough run and recorded, including the 0.2 s / 0.4 s / 0.8 s repeats and the
    pulled-state confirmation that each second swipe moved a weight and a count.
  - No existing assertion edited anywhere.
- **Status:** pending

---

## Ship bookkeeping this item creates

Handled at close, not inside the slice:

- `evidence.md` with the gate numbers recorded **at the moment of each run**, not reconstructed
  (`execution-model.md` §5.1), and the walkthrough results including the pre-fix failure timings.
- `backlog.md`: 013 moves to Shipped; a **new queued entry** for queue-pane undo per the owner's decision
  of 2026-08-27 — widening `ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS` and adding undo
  affordances to Read Later and History, needing its own specification amendment and design pass like
  item 012, citing `contracts.md` §23's two reversible corrective actions.
- `backlog.md` `Debt`: `commitInFlight`'s safety depended on the next card carrying a different
  `article.id`; any future path that returns a card to the same slot must release the lock
  (`design.md` D2).
- Note for item 006, which is next in wave C: this item touches no file 006 touches
  (`DiscoverDeck.kt` and its tests), so the two do not collide.
