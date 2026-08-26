# 008 — Swipe gestures

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/06-ui-ux.md` §§3.4/10/39–45/48/49, `docs/v1/contracts.md` §§31/37,
`design-reference/DESIGN.md` §§4/8, `docs/v1/README.md` Amendment 6\
**Wave:** B (`specs/waves/wave-b.md`) · **Branch:** `feat/008-android-swipe-gestures` → `main`

---

## 1. Problem

The Android Discover card has three labeled controls and no gesture. The browser has both: a card that
can be dragged horizontally from any non-interactive area, committing `dismiss` past 90px to the left
and `save` past 90px to the right (`js/ui/swipe.js:26-154`, `06-ui-ux.md` §§39–42).

**This is enrichment, not a compliance gap, and the scope follows from that.** `DESIGN.md:8` and
`06-ui-ux.md` §3.4 require a labeled equivalent for every gesture, never the reverse. Android shipping
buttons first is the compliant direction; nothing is broken today. What is missing is the fast path —
and, because of how the browser wires Undo, everything downstream of it.

### 1.1 This item completes item 007

Item 007 shipped the undo engine with **no producer**, deliberately and with the reasoning recorded in
`specs/007-android-undo/spec.md` §1.1 and `design.md` D1/D4. In the browser, an undo record is created
only when `undoable && (action === "save" || action === "dismiss")`
(`js/state/article-state.js:141`), and the only call sites that pass `undoable: true` are the swipe
controller and the arrow-key shortcuts — both in `js/ui/swipe.js`, the module this item ports.
`contracts.md` §31 says it in one line: *"Only the most recent eligible swipe action must be
retained."*

So today `AppViewModel.undoRecord` is unreachable: every caller in the tree passes `undoable = false`,
`AppUiState.undoAvailable` is permanently false, `performUndo` is never called, and the five strings
007 authored (`undo_toast_saved`, `undo_toast_dismissed`, `undo_action`, `undo_completed`,
`undo_failed`) are unreferenced. **This item supplies the trigger, the actionable toast, and Undo's
entire owner walkthrough.** An implementation of 008 that leaves Undo unreachable has not done the job:
`06-ui-ux.md` §43 makes the toast and Undo availability part of swipe commitment itself, and a
mis-swipe with no way back is worse than no swipe (`waves/wave-b.md`).

### 1.2 What the JVM gate can and cannot see

Instrumented tests are parked from CI by decision, not oversight (`specs/backlog.md` §Parked; 002
slice 4), so `:app:testDebugUnitTest` cannot observe a Composable. This item's verification is
therefore split by construction, and the split is designed rather than discovered:

- **The decisions** — intent lock, threshold, direction, rotation, exit distance, reduced motion — live
  in a pure Kotlin object with no `android.*` import, driven by JVM tests over synthetic pointer
  sequences (`design.md` D2).
- **The wiring** — that a committed swipe is undo-eligible, that a button press is not, that an offer
  is raised, acknowledged, undone, or refused — lives in `AppViewModel` and is JVM-tested.
- **The feel** — that the gesture does not fight the card's vertical scroll, that the cue reads, that
  the exit is quiet — is emulator-only and is proven in §5, not by a test.

### 1.3 One defect folded in from outside this item's scope

The owner reported, during wave B, that returning from the publisher leaves **Mark read** below the
fold: the reader comes back to Discover and the one action they returned to perform is off screen.

It is not a defect this item introduced. It has been there since item 002 and item 004 did not touch it.
The mechanism is that the card *grows* when the article becomes `OPENED` — the opened acknowledgment
panel is inserted above the action row (`ArticleCard.kt:222-224`) and Mark read is added below it
(`ArticleCard.kt:427-440`) — while `DiscoverScreen`'s scroll reset is keyed on `selectedCategory`,
`article.id` and `state::class` (`DiscoverScreen.kt:57-59`), none of which change on that transition. So
the scroll position is preserved and the taller card pushes its own controls past the viewport.

It is folded into this item on the owner's decision of 2026-08-26, because this item already owns both
files and had a round in flight. `design.md` D11 records the fix.

## 2. Story

As a reader, I want to triage the article on screen with a flick left or right, so that a queue of
choices costs me a gesture rather than a considered press — and, because a flick can be accidental in
a way a labeled press cannot, I want the last one back.

## 3. Out of scope

- **Hardware-keyboard shortcuts.** `06-ui-ux.md` §49's Left/Right/Z describe the browser's input model.
  Android's equivalent of the keyboard path is TalkBack, which reaches the labeled buttons already on
  the card. No `onKeyEvent` handling is added (`design.md` D8).
- **Swipe anywhere but the Discover card.** `06-ui-ux.md:1208` — Read Later is an editorial list, not
  swipe cards. `ArticleRow`, History, and Settings are untouched.
- **Any preference-weight arithmetic.** `preferences` stays empty and `UndoRecord.preferenceReversal`
  stays null until item 005 (`specs/007-android-undo/design.md` D5). The undo path must not read or
  write `preferences`.
- **Any change to 007's engine.** `ArticleStateMachine.transition`, `reverse`, `UndoRecord`, and the
  slot's lifecycle are consumed as shipped. A diff that edits `ArticleStateMachine.kt` is a report to
  the supervisor, not a decision.
- **New dependencies.** No Robolectric, no `compose-ui-test`, no `core-splashscreen`-style additions.
  `android/gradle/libs.versions.toml` is untouched (`design.md` D10).
- **Import, export, and the Settings surface.** Item 009, running concurrently on its own branch.
  `ui/screens/settings/**` and `data/local/state/**` are not this item's files.
- **Any change to `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `tests/**`, or
  `docs/v1/**`.** Amendment 6 confines this item to `android/`.

## 4. Scenarios

### 4.1 The gesture, as arithmetic

### Scenario: a touch that has barely moved locks no intent

Given a pointer pressed on the card\
When it moves less than the intent slop in both axes\
Then no intent is locked\
And the card has not moved\
And releasing commits nothing

### Scenario: a mostly-vertical drag never becomes a swipe

Given a pointer pressed on the card\
When it moves far enough to lock intent, with vertical travel at least as large as horizontal\
Then the intent is vertical\
And the card does not translate\
And the pointer stream is left unconsumed for the scrolling parent

### Scenario: a decisively horizontal drag locks horizontal

Given a pointer pressed on the card\
When it moves far enough to lock intent, with horizontal travel more than 1.15 times the vertical\
Then the intent is horizontal\
And the card translates by the horizontal travel

### Scenario: intent is locked once and does not change mid-gesture

Given a drag that has already locked vertical\
When it later travels much further horizontally than vertically\
Then the intent is still vertical\
And the card has not translated at any point

### Scenario: rotation follows travel and is clamped

Given a horizontal drag\
When the card is translated\
Then the rotation is the travel divided by the browser's divisor\
And it never exceeds 4.5 degrees in either direction

### Scenario: releasing short of the threshold changes nothing

Given a horizontal drag whose travel is less than the commit threshold\
When the pointer is released\
Then no action is emitted\
And the card returns to rest\
And no state change and no preference signal occur

### Scenario: releasing at or past the threshold emits the direction's action

Given a horizontal drag that has reached the commit threshold\
When the pointer is released to the left\
Then `dismiss` is emitted\
And when released to the right past the threshold\
Then `save` is emitted

### Scenario: a cancelled gesture restores whatever its travel was

Given a horizontal drag past the commit threshold\
When the gesture is cancelled rather than released\
Then no action is emitted\
And the card returns to rest

### Scenario: a second gesture is refused while a commit is in flight

Given a committed swipe whose action has not yet returned\
When a new pointer is pressed on the card\
Then the new gesture locks no intent and emits nothing

### Scenario: reduced motion removes the rotation and the exit travel

Given the reader has animations turned off\
When the card is dragged and committed\
Then the rotation is zero at every travel\
And the exit translation is zero\
And the same action is still emitted

### 4.2 The commit, the offer, and Undo

### Scenario: a committed swipe is undo-eligible

Given an article on the Discover card\
When a swipe commits `save` or `dismiss`\
Then the article's state changes as the labeled button would change it\
And the undo slot holds that action\
And Undo is reported as available

### Scenario: a labeled button press is still not undo-eligible

Given an article on the Discover card\
When Not interested, Save for later, Read article, or Mark read is pressed\
Then the undo slot is unchanged\
And no undo offer is raised

### Scenario: each committed swipe raises its own offer

Given a swipe that has raised an undo offer\
When a second swipe of the same direction commits on the next article\
Then a second, distinct offer is raised\
And the slot holds only the newer action

### Scenario: the offer's message names the action

Given a committed swipe\
When the offer is raised\
Then a committed `save` offers `Saved to Read Later`\
And a committed `dismiss` offers `Not interested`\
And both carry the action label `Undo`

### Scenario: the offer expires without withdrawing Undo

Given a raised undo offer\
When it is acknowledged, whether by its timeout or by being superseded\
Then no toast is presented for it\
And the undo slot still holds the action\
And Undo is still reported as available

### Scenario: Undo from the offer restores the article and announces

Given a raised undo offer for a committed dismiss\
When Undo is taken\
Then the article's record returns to exactly what it was before the swipe\
And `Undo completed.` is announced through the existing live region\
And the offer is withdrawn\
And Undo is reported as unavailable

### Scenario: a refused Undo announces its failure and keeps the offer

Given a raised undo offer whose article no longer has the record it names\
When Undo is taken\
Then no local state is written\
And `Undo could not be completed.` is announced\
And the offer is still presented

### Scenario: a swipe whose write fails is not visually finalized

Given an article on the Discover card\
When a swipe commits and the local state write fails\
Then the card returns to rest rather than exiting\
And the existing persistence-failure message is announced\
And no undo offer is raised

### Scenario: returning from the publisher leaves the next decision on screen

Given an article the reader opened at the publisher\
When the reader returns to Discover\
Then the card's triage controls and the Mark read control are both on screen without scrolling\
And the scroll movement is immediate rather than animated when the reader has animations turned off

### Scenario: resetting local data withdraws the offer

Given a raised undo offer\
When local data is reset\
Then the offer is withdrawn\
And the undo slot is empty

## 5. Verification

### 5.1 Gates

Both Android gates, re-run by the reviewer with `--rerun-tasks` in a throwaway worktree rather than
read from an implementer report:

```sh
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Baseline at wave B open: **163 tests**, green on `main` at `75f2821`.

### 5.2 Owner walkthrough

Driven over `adb` by the orchestrator on the `Pixel_10` API 37 emulator, batched at the end of wave B
against merged `main` (`execution-model.md` §6). Screenshot every step. **This is also Undo's first
walkthrough** — 007 has none by design.

1. **The cue.** Drag the card slowly left without releasing. `← Not interested` appears. Drag right:
   `Save for later →`. Release short of the threshold both times: the card returns and the article is
   still on screen.
2. **Commit left.** Drag left past the threshold and release. The card exits, the next article
   appears, and a toast reads `Not interested` with an `Undo` button.
3. **Undo.** Press `Undo` before the toast expires. The dismissed article is back on screen and
   `Undo completed.` is announced. Confirm through Settings → the History and Read Later counts that
   nothing else moved.
4. **Commit right.** Drag right past the threshold and release. The toast reads `Saved to Read Later`.
   Let it expire without pressing Undo. The article is in Read Later.
5. **The scroll is not broken.** With a long card, drag vertically from the middle of the card: the
   Discover column scrolls and the card does not translate. This is the defect class 002 already hit
   once on the emulator; it is the single most important step here.
6. **The buttons still work.** Press Not interested and Save for later. Both commit, and **no toast
   appears** — the offer is a swipe-only affordance.
7. **Drag from a control.** Begin a horizontal drag with the finger on the Read article button. The
   card does not translate.
8. **TalkBack.** With TalkBack on, confirm the three card controls are still reachable and labeled,
   that the toast is announced when a swipe commits, and that its `Undo` button can be focused and
   activated.
9. **Reduced motion.** Turn animations off in developer options. Swipe: the card carries no rotation
   and no exit travel, the action still commits, and the toast still appears.
10. **Return from the publisher.** Press Read article, let the browser open, then come back. The
    triage controls and **Mark read** must both be on screen with no scrolling. This is the defect the
    owner reported during wave B (§1.3); it is the reason this step exists.
11. **Airplane mode is irrelevant here** — no network path is touched. Confirm only that a swipe
    still commits with the device offline.

**What the owner is asked for, and only this:** step 5 and step 2 as a judgment — does the gesture
fight the scroll, and does the exit feel *tactile, quiet, controlled* rather than bouncy or playful
(`06-ui-ux.md` §44). `adb` can drive a synthetic swipe; it cannot judge one.
