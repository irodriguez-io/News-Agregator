# 024 — The next card arrives without a pause

**Surface:** Android only.
**Authority:** `docs/v1/06-ui-ux.md` §43, §44, §48, §50, §79.3, §79.5 (second edition); Amendment 11;
**Amendment 12, authored by this item**.
**Branch:** `feat/024-android-card-arrival-gap`, cut from `main` at `7667ddf`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 The defect, as the owner found it

Found by item 023's walkthrough on 2026-09-22, on a signed release build: after a swiped card leaves,
**roughly half a second of nothing** passes before the replacement arrives — *"enough for my brain to doubt
whether a new card will arrive."* Undo, by contrast, feels right: *"if we could reduce the gap between swipes
to be similar to the gap between the undo and the restored card appearing, we will have an optimal UX."*
(`specs/023-android-card-swipe-motion/evidence.md` §4.)

### 1.2 The gap has two parts, and the handover named only one

`ArticleCard.kt:147-155` launches the commit only after `animateToGestureState()` returns — that is, after
the exit's full `300ms` curve. Then `onSwipeCommit` → `AppViewModel.launchArticleAction`
(`AppViewModel.kt:179-188`) → `onArticleAction` (`:372-412`) takes `stateMutex`, runs the transition, saves,
and publishes. Only then does the head article change and 023's entrance begin.

**Part 1 — the exit's invisible tail, about 245 ms, and it is most of the gap.** The exit runs Material 3
Emphasized (`SwipeGesture.ExitEmphasizedEasing`, `SwipeGesture.kt:25-38`), which front-loads its travel:
progress is `0.40` at 50 ms, `0.77` at 75 ms and `0.87` at 100 ms. The exit target is
`max(0.82 × viewport, 620dp)` (`SwipeGesture.kt:137-142`), so on a 360–411 dp phone a card released at the
90 dp threshold is **fully off-screen roughly 55–60 ms into the exit**. The remaining ~245 ms draws nothing, and
the commit waits for it. *(Computed from the curve's control points, `Spacing.kt:23`'s 18 dp margin and
`MAX_ROTATION_DEGREES`; not measured on a device.)*

**Part 2 — the save.** `saveLocalState` writes to disk with an `fsync` (`LocalStateFile.kt:39`) on
`Dispatchers.IO` (`LocalStateRepository.kt:23`), then `publish()` (`AppViewModel.kt:696-698`) re-ranks the
deck through `UiStateMapper.map` on the main thread. **Undo pays exactly this cost** — `performUndo` takes
the same lock and `persistUndoTransition` saves and publishes the same way — and the owner calls undo
optimal. Its duration has not been measured.

**Two corrections to the record, so they are not carried forward.** The disk write is not on
`Dispatchers.Main.immediate` — only the coroutine that awaits it is. And the deck re-rank happens in
`publish()`, not in `adoptPersistedState`, which only adopts the saved state and the appearance.

### 1.3 What the fix is, as the owner chose it on 2026-09-29

**Start the commit the moment the departing card is no longer visible**, rather than when its curve
finishes. The swipe then reads *exit → save → entrance*, which is undo's *save → entrance* with the visible
exit in front — the owner's benchmark. The ~245 ms tail is removed; what remains is the save, the same wait
undo has.

**Rejected at the same decision:** starting the save at release and swapping when the full exit finishes
(removes Part 2 and keeps Part 1, the larger); and combining both (smallest gap, but the screen must keep
showing the old card after the view model has moved on — item 015's ground). See `design.md` D1.

### 1.4 This is the fifth item on this surface

Items 008, 013, 015 and 023 all landed here. Two settled behaviours are load-bearing and this item reopens
the sequencing that protects them — 023's D2 — so both are re-proved with instrumented tests, not
structural arguments:

- **013 — a card accepts a swipe as soon as it is on screen.** There is no window in which the card
  declines a touch.
- **015 — a swipe is attributed to the article the reader saw.**

**This item creates one new window that did not exist before:** the replacement card is live while the
departed card's exit curve is still running, unobserved, on its own animation state. A swipe in that window
must behave exactly as any other swipe on the arriving card.

---

## 2. Story

As a **reader**, I want the next card to arrive as soon as the one I swiped has left, so that deciding about
an article feels like one continuous motion rather than a pause I have to wait through.

---

## 3. Amendment 12 — authored by this item, approved at the plan gate

§43 orders the post-commitment sequence — the card exits, the state action is processed, the next card
appears — and §79.5 gives the exit's fade, deferring to §44.2 for its curve and duration. Neither says
**when the exit counts as finished** for §43's ordering. Reading it as *when the curve completes* is what
produces Part 1 of the gap. Choosing the other reading is an interpretation of a silent specification, which
`AGENTS.md` forbids an implementation to make, so this item authors it.

**Amendment 12, Android Card Exit Completion**, is narrow and Android-only. It adds to §79.5's **Exit**
paragraph:

> **For §43's sequence, the exit is complete once the departing card is no longer visible** — when its
> rotated bounds have left the viewport — **or when its curve completes, whichever is first.** The state
> action is processed from that point. The remainder of the curve draws nothing on screen and does not delay
> it. §43's order is unchanged: the card visibly exits before the state action is processed, and the next
> card appears after it.

It changes **no** behaviour and no value: not §44.2's curve or duration, §79.5's fade or entrance, §43's
order, §50's rule that a failed save must not be visually finalised, any state transition, status value,
signal, delta, count, ranking, deck ordering, undo path, gesture semantic, threshold, cue, commitment rule or
authored string. §47's prohibitions continue to bind. The browser is unaffected; no `js/**` or `css/**`
change is authorized or required.

---

## 4. Out of scope

- **Starting the save before the card has left** — the rejected options of §1.3. If the walkthrough still
  finds a gap, that is the next lever, and it is a new item.
- Any change to §44.2's curve or duration, the exit distance constants, §79.5's fade, or the entrance.
- **Wide viewports where the card never fully leaves — which includes every phone in landscape.** The exit
  target is `max(0.82 × viewport, 620dp)` and the card is full-width less an 18 dp margin, so above roughly
  **615 dp** the exit does not carry the card's trailing edge past the screen edge; it leaves by fading, not
  by travel. There the curve-completion backstop governs and the gap is unchanged. The defect was found in
  portrait, and closing it in landscape means changing the exit distance or treating a near-transparent card
  as gone — both are §44 or §79.5 questions for a later item.
- The exit carrying the gesture's release velocity — owner decision pending, needs a §44.2 amendment.
- Destination transitions showing both tabs' text at once — item 021's ground.
- `AppViewModel`, `ui/screens/discover/**`, persistence, and the browser.

---

## 5. Scenarios

### Scenario: the commit starts when the card has left the viewport
Given a phone-width viewport
When a swipe is committed
Then the state action is requested once the departing card's rotated bounds have left the viewport
And it is requested before the exit's `300ms` curve completes
And it is requested exactly once

### Scenario: the commit never starts while the card is still visible
Given a swipe has been committed
When any part of the departing card is still inside the viewport
Then the state action has not been requested

### Scenario: where the card cannot leave, the curve's completion starts the commit
Given a viewport wide enough that the exit does not carry the card off-screen
When a swipe is committed
Then the state action is requested when the exit's curve completes, as today

### Scenario: a swipe on the arriving card while the old exit is still finishing
Given a swipe has been committed and saved
And the replacement card is on screen while the departed card's exit curve has not yet completed
When the reader swipes the replacement
Then the gesture is accepted and tracked from its first frame, per item 013
And the action is applied to the replacement article, per item 015

### Scenario: a failed save returns the card, wherever the exit had reached
Given a swipe has been committed
When the save fails, including while the exit curve is still running
Then the card returns to its resting position at full opacity
And the head article is unchanged, per §43 and §50
And the card accepts a new swipe

### Scenario: reduced motion is unchanged
Given a reduced-motion preference is set
When a swipe is committed
Then the state action is requested immediately, as today, and the replacement arrives with no fade and no rise

### Scenario: nothing else about the motion changes
When a card leaves or arrives
Then the exit's curve, duration, distance, rotation and fade, and the entrance, are exactly item 023's
And no motion on §47's prohibited list occurs

---

## 6. Verification

### 6.1 Gates

From `android/`, with both exported or Gradle fails before any test runs:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
ANDROID_HOME=$HOME/Library/Android/sdk

./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

Hosted CI green on the exact final head before the final review merges.

### 6.2 The regression surface

**These pass unedited:** `ArticleCardGestureTest`, `ArticleCardScrollGestureTest`, and 023's three entrance
tests in `DiscoverScreenLayoutTest` (`:57-176`). An implementer who finds themselves editing any of them has
moved a settled behaviour and must stop and report.

**New instrumented coverage** for the four scenarios a JVM test cannot reach: the commit is requested before
the curve completes and exactly once; a swipe in the new window is accepted and attributed to the
replacement; and a failed save mid-exit returns the card.

**JVM coverage** for the geometry: that the departure test is false while any rotated edge of the card is
inside the viewport and true once none is, for both directions, and that a wide viewport falls back to
curve completion.

### 6.3 Walkthrough — required evidence

On a **release** build with `adb shell settings get global animator_duration_scale` not `0`, and the release
package uninstalled afterwards:

1. Swipe left and right: the card leaves on the same motion as 023, and the replacement follows **without a
   perceptible pause** — judged against undo's restore, side by side.
2. A swipe on the replacement as it arrives is accepted and lands on it.
3. With the system's remove-animations setting on: both are immediate.
4. A vertical drag still scrolls.

### 6.4 Owner checkpoints

1. **Amendment 12's text**, at the plan gate.
2. **The walkthrough**, which carries §44's *tactile, quiet, controlled* judgement — open since wave B, and
   for the first time with its named cause addressed.

### 6.5 A prediction, recorded so the walkthrough tests it

**The remaining gap will be the save plus the re-rank, and it will read like undo's.** If the walkthrough
still finds a pause, the cause is Part 2, the lever is starting the save earlier (§4's first bullet), and
it is **not** any motion value. Revisit this at the walkthrough; 023's D7 was a prediction nobody revisited.
