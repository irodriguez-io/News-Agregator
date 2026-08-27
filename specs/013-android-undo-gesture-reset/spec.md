# 013 — A Discover card must accept touch as soon as it is on screen

**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/013-android-undo-gesture-reset/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`, **and `docs/v1/**`** — this item proposes no specification
amendment and needs none.

**Cut from:** `main` at `2613959`.

> **This specification was rewritten on 2026-08-27.** Its first version named the wrong root cause and a
> fix was implemented and reviewed against it before the walkthrough disproved it. §1.3 records what was
> wrong and why, because that is the more useful half of this item.

---

## 1. Why this item exists

### 1.1 What the reader sees

Acting on a Discover card shortly after tapping **Undo** does nothing at all. Not a missing undo offer —
the swipe or tap is simply discarded, and the card stays on screen.

### 1.2 What is actually happening

`ArticleCard.kt:83-98` builds a new `SwipeGesture.State` through `remember(article.id, …)`, and the card's
touch handling is `Modifier.pointerInput(gestureState)`. **`pointerInput` restarts its handler whenever
its key changes**, so every change of the Discover head article tears the gesture handler down and
relaunches it. The relaunch is not immediate, because `DiscoverScreen.kt:74-87`'s article-change scroll
(008 D12) is animating on the same frame clock.

Instrumented on the `Pixel_10` emulator, 2026-08-27 (logs and screenshots preserved with this item's
evidence):

```
10:58:10.825  compose ArticleCard article=eb011ad…    ← restored card enters composition
10:58:10.931  scroll article effect changed=true       ← the D12 scroll runs
10:58:10.978  cardBounds … centerY=1317.0             ← settled at its final position
10:58:11.206  pointerInput start article=eb011ad…     ← handler finally attaches, +381 ms
              (no "awaitFirstDown returned" — the DOWN was discarded)
```

A passing trial logs `pointerInput start` and `awaitFirstDown returned` in the same millisecond. Measured
attach latency ranged **381–777 ms**, varying with what else was animating.

**The card is therefore visible, settled and correctly positioned for up to roughly 0.8 s before it can
receive touch, and anything the reader does in that window is silently discarded.**

Undo is not the cause. It is the easiest way to reach the window, because it changes the head article
twice in quick succession — the deck advances, then reverts — while a scroll animates.

### 1.3 What the first version of this specification got wrong

It blamed `SwipeGesture.State.commitInFlight` remaining latched after a successful commit, and a fix
(`bf79c42`, `releaseCommitLock()`) was written, reviewed and merged to this branch against that theory.
The theory was wrong, and three pieces of evidence disprove it:

- In the failing trial `gestureState.down(...)` was **never called**; whenever it was called it returned
  `true` with `commitInFlight` already `false`.
- The card's **Save for later button** fails in the same window, and it never consults the gesture state.
- The **category chip** on the same screen works in that window, so the screen is live and only the card
  is affected.

Two process notes worth keeping. The unit tests written for that fix were correct and still pass — they
tested the new method's contract, which was never the symptom; a green unit gate was never going to catch
this. And the emulator walkthrough was made definition-of-done rather than optional precisely because the
recomputation path is unreachable from JVM tests. That decision is the only reason this did not ship
labelled as fixed.

### 1.4 What happens to `bf79c42`

**It stays, re-justified.** It was committed for a reason now known to be false, and this section says so
rather than quietly letting it stand. It earns its place under the new fix: once a single pointer handler
persists across head-article changes, a gesture state still latched from a previous commit can be swapped
in behind it. Releasing the lock when a commit resolves is the precondition that makes a persistent
handler safe. Its four unit tests remain valid exactly as written.

---

## 2. Story

As a **reader**, I want a Discover card to accept my swipe or tap as soon as it is on screen, so that an
action I take immediately after the deck changes is not silently lost.

---

## 3. Out of scope

- **Which inputs offer Undo.** The labeled buttons stay non-undoable and no keyboard shortcut is added.
  Specified asymmetry: `contracts.md` §31, `06-ui-ux.md` §70, 007 `spec.md` §1.1, 008 D8.
- **Undo in Read Later and History.** Its own future item, per the owner's decision of 2026-08-27.
- **The three `DiscoverScreen` scroll effects.** One of them makes this window longer, but changing scroll
  behaviour is item 012's ground. This item makes the card touchable during the window; it does not
  remove the window.
- **Any change to `docs/v1/**`**, to `SwipeGesture.kt`, or to the undo offer's duration, copy or affordance.
- **Instrumented tests in CI.** They stay parked (002 slice 4). The new test is a local, on-demand guard.

---

## 4. Scenarios

### Scenario: a card accepts a swipe immediately after the deck advances

Given a Discover card has just been committed by a swipe\
And the next card has become the head article\
When the reader swipes that new card as soon as it is on screen\
Then the swipe is received and commits its action

### Scenario: a card accepts a swipe immediately after Undo returns it

Given a swipe has been committed and the reader has tapped Undo\
When the reader swipes the returned card as soon as it is on screen\
Then the swipe is received and commits its action\
And it raises its own undo offer

### Scenario: a card accepts a button press immediately after the deck changes

Given the Discover head article has just changed\
When the reader presses Save for later on the new card as soon as it is on screen\
Then the press is received and the article is saved

### Scenario: the gesture state is still per-card

Given the head article changes\
When the new card is swiped\
Then its travel and its commit lock are its own\
And no travel or latched commit carries over from the card that left

### Scenario: a committing card still refuses a second gesture

Given a card whose swipe has committed and whose commit has not yet resolved\
When a second gesture is attempted on it\
Then the gesture is refused\
And this is the existing assertion at `SwipeGestureTest.kt:164`, which does not change

---

## 5. Verification

### 5.1 The prediction that must be tested first

If §1.2 is right, the defect **must also reproduce with no Undo involved** — two fast consecutive swipes,
where the second lands during the new card's handler restart.

**Reproduce that before any fix is written.** If consecutive fast swipes do not drop the second touch,
§1.2 is wrong too and this item stops for re-design rather than proceeding. The first version of this
specification skipped exactly this check.

### 5.2 Gates

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

Head of this branch: **258 tests, 0 failures, `BUILD SUCCESSFUL`**.

### 5.3 The instrumented regression test

This fix is Compose wiring and has no JVM-testable surface. The project parks instrumented tests **for
CI** (002 slice 4) but already keeps one as a local, on-demand guard — `MainActivityLaunchSmokeTest` in
`android/app/src/androidTest/`. `androidx.compose.ui.test.junit4` is already an `androidTestImplementation`
dependency, so **no new dependency is required**.

```sh
./gradlew :app:connectedDebugAndroidTest
```

The test changes the head article and injects a touch during the window, asserting the action is
received; `ComposeTestRule.mainClock` makes the timing deterministic.

**If the instrumented test cannot reproduce the dropped touch, stop and report.** Do not substitute the
walkthrough for it, and do not weaken it until it passes.

### 5.4 Walkthrough — required evidence

Driven over `adb` on the `Pixel_10` emulator. **Aim every swipe and tap at the card's true bounds, read
from `adb shell uiautomator dump`.** Fixed coordinates confounded the first attempt at this item, because
the deck scroll-resets and moves the card between trials.

1. Two fast consecutive swipes, no Undo. Both must commit.
2. Swipe → Undo → immediate swipe, at roughly 0.2 s, 0.4 s and 0.8 s. All must commit and raise an offer.
3. Save for later pressed in the same window. Must save.

For each, confirm from the pulled state document that a weight and a count actually moved. **A card
leaving the deck is not proof that the action landed** — that mistake produced a false pass during the
005 walkthrough and again here.
