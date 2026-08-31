# 015 — A swipe must be attributed to the article the reader saw

**Status:** draft (awaiting plan gate)\
**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/015-android-undo-swipe-attribution/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`, **and `docs/v1/**`** — this item proposes no amendment and needs
none.\
**Authority:** `docs/v1/contracts.md` §§18/22/23/31, `docs/v1/05-personalization-state.md` §§36–41,
`docs/v1/06-ui-ux.md` §§39–45, `docs/v1/README.md` Amendment 6\
**Wave:** D (`specs/waves/wave-d.md`), leads · **Branch:** `feat/015-android-undo-swipe-attribution` → `main`\
**Cut from:** `main` at `6c857a6` — the merge of PR #19, which carries this item's design artifacts and the wave's amendments. Hosted CI green on that commit (Test `33422480339`, Pages `33422480328`).

---

## 1. Why this item exists

### 1.1 What the reader sees

Swipe a card, tap **Undo**, and swipe again immediately. The card the reader is looking at is the one Undo
just restored. The action is recorded against a **different** article — the one that was leaving the deck
because the restore displaced it. The reader trains a preference for an article they never chose.

Three runs at delay 0, from item 013's investigation, attributed the swipe to `ietf_oauth`, `ietf_oauth`
and `science_aaas`. **A race, not a constant.**

### 1.2 The mechanism, and it is known

`ArticleCard` keeps its per-article gesture values in `remember(article.id, …)`
(`ui/components/ArticleCard.kt:86-115`) and exposes them to the pointer handler through
`rememberUpdatedState` (`:118`). The handler reads that state **once, at pointer DOWN** (`:126`) and
carries the reference all the way to the commit (`:151`):

```kotlin
val down = awaitFirstDown(requireUnconsumed = false)
val gesture = currentGestureValues          // ArticleCard.kt:126 — captured here
…
currentOnSwipeCommit(gesture.article, articleAction) { … }   // :151 — committed against that capture
```

`AppViewModel.persistUndoTransition` restores the record and calls `publish()`
(`ui/AppViewModel.kt:536-546`) on `Dispatchers.Main.immediate`, so the new head article is published
**before** the frame on which the restored card composes. Between the publish and that composition,
`gestureValues` still holds the outgoing article, and a DOWN in that gap adopts it. 013 measured
`awaitFirstDown RETURNED article=8c80f6f9…` firing **22 ms before** the restored card composed
(`specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md` §2).

**It is a publish-ordering defect, not a consumption one.** 013's `requireUnconsumed = false` fix neither
causes it nor worsens it; that was verified in 013's walkthrough and is not re-derived here.

### 1.3 Two facts that shape the fix

**The reader gets no drag feedback in that window either.** `Modifier.graphicsLayer` reads the *current*
`gestureValues` (`ArticleCard.kt:181-184`) while the handler mutates the *captured* one, so in the gap the
card does not translate and shows no cue. The reader's swipe already looks like it did nothing. The only
thing it actually does is write the wrong record.

**The reader's intent in that window is unresolvable.** They tapped Undo and then swiped; the article they
meant to act on is whatever Undo brings back. Committing against the displaced article is not a
near-miss — it is the opposite of what they asked for. Doing nothing is the only honest outcome, and it is
what the screen is already showing them.

### 1.4 The trap, and it is the whole reason this item is dangerous

Scored by *"did a weight move"*, every one of these runs is a **pass**. That is what made 013's failure
window look like a band open on both sides and cost it a whole pass.

> **Score by which article moved.** Any test, log line or walkthrough step that does not name the article
> id is not evidence in this item.

### 1.5 What this item does not inherit

013 spent two of its four passes on an inherited diagnosis. This item inherits 013's *measurements* and
none of its *theories*. In particular it does not assume anything about pointer arbitration, handler
attach latency, or the three scroll effects in `DiscoverScreen.kt` — see §3.

---

## 2. Story

As a **reader**, I want the swipe I make to be recorded against the card I am looking at, so that a swipe
immediately after Undo trains a preference for the article I chose or trains nothing at all — never for a
different article.

---

## 3. Out of scope

- **`ui/components/ArticleCard.kt`, `ui/gesture/SwipeGesture.kt` and `ui/screens/discover/DiscoverScreen.kt`.**
  Named explicitly because `waves/wave-d.md`'s collision matrix left an open `?` on `ArticleCard.kt`. This
  item **does not touch it** — see `design.md` D1 and D2. That resolves the `?` cell: no collision with
  item 014.
- **Any input lockout, refusal window, artificial lag, or visible non-interactive card state.** 013 §1.6
  rejected all of these and the rejection stands. Refusing a *commit whose target cannot be determined* is
  not refusing an *input*; `design.md` D3 states the difference and why it is not the same proposal.
- **`ArticleAction.OPEN`.** The publisher-opening lambda (`IntentionalReadingApp.kt:138-145`) is shared by
  all three destinations, and splitting it to carry a Discover-only guard is scope this item does not need.
  Residual risk is stated in `design.md` D4 and it is no worse than today.
- **Which surfaces offer Undo** (item 014) and **which actions are reversible** (item 016). This item
  changes neither set.
- **Any `docs/v1/**` change.** Nothing here alters what is reversible, what is offered, or what is
  announced.
- **The undo offer's duration, copy or affordance**, and `UndoToast.kt`.
- **013's commits.** `bf79c42`, `1f19cea` and `29344aa` stay, and `ArticleCardGestureTest` stays
  byte-identical.
- **Instrumented tests in CI.** Still parked (002 slice 4).

---

## 4. Scenarios

### Scenario: a swipe in the window after Undo is not attributed to the displaced article

Given the reader has dismissed article A and article B has become the head of Discover\
And the reader has tapped Undo, so article A is the published head again\
When an action is committed from the Discover card against article B\
Then the action is refused\
And article B's record is exactly what it was before\
And no preference weight or interaction count for article B's source or topics has moved\
And article A remains the head of Discover

### Scenario: an ordinary swipe still commits

Given article A is the head of Discover\
When the reader swipes it\
Then the action is applied and persisted against article A\
And the undo offer is raised

### Scenario: a swipe is refused when the head changed under it for any other reason

Given article A is the head of Discover and a swipe on it has begun\
And the reader's category selection changes the head article to C\
When the swipe commits against article A\
Then the action is refused and no weight moves for A

### Scenario: the guard is confined to the Discover card

Given the reader is on Read Later\
And the article in the row they act on is not the head article of Discover\
When they mark that article read\
Then the action is applied and persisted normally

### Scenario: a refused action leaves the reader nothing to undo

Given an action from the Discover card has been refused as described above\
When the reader looks for an undo offer\
Then none was raised\
And the previously pending offer, if any, is unchanged

---

## 5. Verification

### 5.1 Gates

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

Delete `test-results` before every run and read the `BUILD SUCCESSFUL` line rather than the counts
(`waves/wave-b-note.md` §7). Record the count at the branch head in `evidence.md` at the moment of the run.

### 5.2 What the unit tests can and cannot prove

They prove the **invariant**: no action committed from the Discover card is ever recorded against an
article that is not the published head. They cannot reproduce the **defect**, because the defect is a
Compose recomposition race and `testDebugUnitTest` has no composition. 013 established that trying to
reproduce a timing window in a Compose test harness does not work — the harness idles until composition
settles, which is the condition under which there is no window
(`013/investigation/step0-reproduction.md`).

**This is stated here so that no slice tries to satisfy it with a test that cannot see it, and so that no
DoD bullet demands proof at a layer that has no observer** (`waves/wave-c-note.md` §2; wave C's third plan
defect). The invariant is asserted in `AppViewModelTest`. The defect's absence is proved by §5.3.

### 5.3 Walkthrough — required evidence, and the only proof the defect is gone

Driven over `adb` on the `Pixel_10`, **inside one on-device `adb shell`**, per `waves/wave-d.md`'s
walkthrough method. `uiautomator dump` cannot see the Undo toast; `screencap` at 0.35 s shows it plainly.
Card action buttons sit roughly 150 px **above** their text labels — read the clickable node's bounds.

1. Swipe article A. Note A's id from the pulled state document.
2. Tap Undo. Confirm A is restored: its record is gone from `articles` and its source weight is back.
3. Swipe again at delay **0**, three runs. For each run, pull the state document and name **which article
   id acquired a record and which source weight moved**. Every run must show either A or nothing — never
   the displaced article.
4. Repeat at delay **0.2 s** and **0.8 s**: both must commit against the restored article, as they do
   today. Neither may regress into a refusal.
5. A settled swipe 3 s after Undo still commits.
6. For each step, and per `execution-model.md` §9: *and is what the reader needs next actually on screen?*

Record every claim with the pulled state document and a `screencap`. **A card leaving the deck is not
proof that the right action landed.**

### 5.4 Stop conditions

Stop and report rather than proceeding if:

- the guard refuses a swipe at 0.2 s or 0.8 s that lands correctly today — that is a 013 regression and the
  design is wrong, not the test;
- the §5.3 delay-0 runs still attribute a swipe to the displaced article after the gates are green;
- **any existing test fails.** The guard is opt-in and defaults to off (`design.md` D2), so no existing
  case should move. A failing existing case means the guard has leaked outside the Discover card path,
  which is a design error — report it rather than editing the test around it.
