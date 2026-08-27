# 013 — A Discover card must accept a swipe as soon as it is on screen

**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/013-android-undo-gesture-reset/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`, **and `docs/v1/**`** — this item proposes no specification
amendment and needs none.

**Cut from:** `main` at `2613959`.

> **Second revision, 2026-08-27.** The first version named the wrong root cause (§1.3). The second
> version got the cause right and the *verification* wrong: its §5.1 prediction passed, and its §5.3
> instrumented gate could not be made to fail first, so the item stopped before any fix was written
> (§1.5). This revision keeps §1.2's diagnosis unchanged, replaces §5.3, and splits the button-press
> scenario out to item 014.

---

## 1. Why this item exists

### 1.1 What the reader sees

Acting on a Discover card shortly after the deck changes does nothing at all. Not a missing undo offer —
the swipe is simply discarded, and the card stays on screen.

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
  That is now **item 014** — see §1.6.
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

### 1.5 What Step 0 established, and where the second version stopped

`investigation/step0-reproduction.md` records the run in full. Two results:

- **The diagnosis survived its own falsifiable test.** Two fast consecutive swipes with no Undo, aimed at
  bounds read from `uiautomator dump`, produced **one** new record. The second card's source weight and
  interaction count did not move. The second swipe was discarded, confirmed against pulled state
  documents rather than by the card leaving the deck. §1.2 stands unchanged.
- **The instrumented gate could not be made to fail first.** Seven pre-fix harness variations — paused
  `mainClock`, raw `MotionEvent` injection, the real `DiscoverScreen` scroll path, composition- and
  layout-bound injection — all **passed** against unchanged production code. Per D3 the implementer
  stopped and reported rather than weakening the test. Nothing was committed.

The reason is structural and it is a flaw in the old §5.3, not in the diagnosis: the window exists because
a real frame clock is starved by a competing animation, and `ComposeTestRule` is built to synchronize
exactly that away — it idles until composition and its coroutines settle before injecting. **A test
harness whose job is to eliminate timing windows cannot observe a timing window.** §5.3 is therefore
rewritten to assert the *cause* — the handler is torn down at all — which is a composition fact and not a
race. Symptom coverage moves to §5.1's reproduction and §5.4's walkthrough.

### 1.6 What moves to item 014

The **Save for later button** fails in the same window and never touches the gesture state, so §4's
persistent-handler fix would not address it. It has a different mechanism, it has not been diagnosed, and
diagnosing it here would repeat the first version's mistake of reasoning from a plausible shared cause to
a shared fix. Owner decision of 2026-08-27: it becomes **item 014**, with its own investigation.

It stays in §1.3 as evidence, because that is what disproved the `commitInFlight` theory. It is out of
this item's scope, scenarios, and walkthrough.

---

## 2. Story

As a **reader**, I want a Discover card to accept my swipe as soon as it is on screen, so that a swipe I
make immediately after the deck changes is not silently lost.

---

## 3. Out of scope

- **The Save for later button in the same window.** Item 014, per §1.6.
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

The first three are the reader-facing outcomes. The last three are the mechanism, and they are what §5.3
asserts — stated as scenarios because they are what the fix actually changes.

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

### Scenario: the gesture state is still per-card

Given the head article changes\
When the new card is swiped\
Then its travel and its commit lock are its own\
And no travel or latched commit carries over from the card that left

### Scenario: the touch handler survives a head-article change

Given a swipe is in progress on the Discover card\
When the head article changes while the pointer is still down\
Then the gesture is not cancelled\
And carrying it past the commit threshold and lifting still commits

### Scenario: a gesture commits against the card it started on

Given a swipe is in progress on the Discover card\
When the head article changes while the pointer is still down\
And the reader carries the gesture past the commit threshold and lifts\
Then the committed article is the one the gesture started on

### Scenario: a committing card still refuses a second gesture

Given a card whose swipe has committed and whose commit has not yet resolved\
When a second gesture is attempted on it\
Then the gesture is refused\
And this is the existing assertion at `SwipeGestureTest.kt:164`, which does not change

---

## 5. Verification

### 5.1 The prediction — tested, passed, closed

The second version required the defect to reproduce with **no Undo involved** before any fix was written.
It does: `investigation/step0-reproduction.md`, with `step0-state-before.json` / `step0-state-after.json`
and both `uiautomator` dumps. **This gate is closed and does not run again.** Its result is now evidence
(§1.5), and the §5.4 walkthrough re-runs the same case after the fix as a GREEN.

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

### 5.3 The instrumented regression test — the cause, not the window

**This section replaces the one the second version stopped at.** Do not attempt to reproduce the timing
window in `ComposeTestRule`; §1.5 records seven variations that could not, for a structural reason.

Assert the composition fact instead. The defect is that a head-article change **tears the pointer handler
down**; a torn-down handler cancels whatever gesture it was running, and `awaitFirstDown` will not adopt a
pointer that is already pressed. So a gesture that spans a head-article change is the discriminator, and
it needs no clock control at all:

1. Render `ArticleCard` directly, with the article driven by a test-owned `mutableStateOf` and a recording
   `onSwipeCommit`.
2. `performTouchInput { down(center); moveBy(…) }` — carry the drag past `INTENT_SLOP_DP` so the gesture
   is live, pointer still down.
3. Swap the state's article for a different one. Composition settles.
4. `performTouchInput { moveBy(…past THRESHOLD_DP…); up() }`.
5. Assert `onSwipeCommit` fired, **with the article the gesture started on**.

**Today this fails at step 5 with no commit at all** — the handler was destroyed at step 3 and its
replacement never saw a DOWN. After the fix it commits. That is a genuine fail-first, it is deterministic,
and it needs no new dependency: `androidx.compose.ui.test.junit4` is already an `androidTestImplementation`
and `androidx.compose.ui.test.manifest` a `debugImplementation`.

It runs on the same terms as `MainActivityLaunchSmokeTest` — a local, on-demand guard, not in CI
(002 slice 4):

```sh
./gradlew :app:connectedDebugAndroidTest
```

**This case is deliberately artificial.** In the app the head article changes only as a *result* of a
committed action or an Undo, both of which require the pointer to be up already. It is written because it
is the cheapest deterministic observation of the mechanism, and §4 names it as a scenario rather than
hiding it in a test. The reader-facing outcomes are covered by §5.1's reproduction and §5.4.

**If this test does not fail first for the stated reason, stop and report.** Do not weaken it, and do not
substitute the walkthrough for it. That stop condition has now saved this item once (§1.5).

### 5.4 Walkthrough — required evidence

Driven over `adb` on the `Pixel_10` emulator. **Aim every swipe at the card's true bounds, read from
`adb shell uiautomator dump`.** Fixed coordinates confounded the first attempt at this item, because the
deck scroll-resets and moves the card between trials.

1. Two fast consecutive swipes, no Undo — the §5.1 case, re-run as a GREEN. Both must commit.
2. Swipe → Undo → immediate swipe, at roughly 0.2 s, 0.4 s and 0.8 s. All must commit and raise an offer.

For each, confirm from the pulled state document that a weight and a count actually moved. **A card
leaving the deck is not proof that the action landed** — that mistake produced a false pass during the
005 walkthrough and again here.

The Save for later button in the same window is **not** walked here; it is item 014's evidence.
