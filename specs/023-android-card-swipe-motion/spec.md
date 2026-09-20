# 023 — The card leaves, and the next one arrives

**Surface:** Android only.
**Authority:** `docs/v1/06-ui-ux.md` §23, §43, §44, §46, §47, §48, §79 (second edition); Amendment 9;
**Amendment 11, authored by this item**.
**Branch:** `feat/023-android-card-swipe-motion`, cut from `main`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 The defect, as the owner found it

Reported 2026-09-20 during wave E's walkthrough: a swiped card *"moves horizontally and when it reaches the
border suddenly disappears and a new card appears in the center of Discovery, without any transition."*

### 1.2 What is actually wrong, after the first diagnosis was corrected

The first reading of this defect was that the exit distance was anomalous — a 620 dp floor beating the
viewport term and firing the card roughly 1.5 screen widths. **That reading was wrong and is recorded here
so it is not re-derived.** `js/ui/swipe.js:108` computes `Math.max(window.innerWidth * 0.82, 620)`, and item
008 ported it faithfully; in viewport-relative terms the two agree closely (Android ≈ 1.5 viewports at
411 dp, a phone browser ≈ 1.6 at 390 px). The distance is not the fault.

Three things are:

**1. The Android exit runs the browser's curve.** `06-ui-ux.md` §80 records that §44 was **split on curve
only** for the second edition. §44.1 keeps `280ms cubic-bezier(0.2, 0.8, 0.2, 1)` for the browser; **§44.2
requires Material 3 Emphasized easing at the M3 equivalent duration for Android.** `ArticleCard.kt:103-110`
uses `CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)` at `SwipeGesture.EXIT_DURATION_MS = 280` — §44.1's values, on
Android.

Item 008 shipped before the second edition, when §44 carried one value for both surfaces, and its
`design.md` cites `06-ui-ux.md §44` for the 280 correctly *for its moment*. Nothing in wave E revisited it:
`waves/wave-e.md`'s collision matrix allocated motion to item 021, whose scope was destination transitions
and the Settings sheet. The card fell between the two. **This is a compliance gap and needs no amendment —
the requirement already exists and the code does not meet it.**

**2. The card never fades.** The browser animates to `opacity: 0` alongside the translate and rotate
(`js/ui/swipe.js:114`). Android's `graphicsLayer` (`ArticleCard.kt:178-181`) sets `translationX` and
`rotationZ` and nothing else, so the card holds full opacity until the frame it leaves the viewport. This
is most of *"suddenly disappears"*: it does not dissolve, it exits frame at speed and is simply gone.

**3. Nothing specifies the entrance, on either surface.** §43 lists the post-commitment sequence and step 3
is only *"the next eligible card appears"*. No section says how. `DiscoverScreen.kt` has no
`AnimatedContent` and no `Crossfade` — verified — and `ArticleCard.kt:83`'s `remember(article.id, …)`
wrapping `Animatable(0f)` means the replacement is constructed at rest, centred, at full opacity. It does
not appear; it is simply already there.

Authoring the entrance is new motion in a place the specification is silent, and AGENTS.md forbids filling
that silence from an implementation. **The owner chose a rise-and-fade in place, with no lateral movement,
on 2026-09-20.** See §3.

### 1.3 This is the fourth item in a row on this surface, and that governs the design

Items 008, 013 and 015 all landed here, and `specs/backlog.md` records that **013's whole history is defects
a green JVM gate could not see.** Two guards exist because of them and both are instrumented:
`ArticleCardGestureTest` (a gesture across a head-article change) and `ArticleCardScrollGestureTest`.

Two settled behaviours are therefore load-bearing and must survive untouched:

- **013 — a card accepts a swipe as soon as it is on screen.** An entrance animation must not introduce a
  window in which the card declines a touch. `013 design.md` D9 and D11: *"there is no window in which the
  card declines a touch."*
- **015 — a swipe must be attributed to the article the reader saw.** Any change that puts two cards on
  screen at once reopens the question of which one a touch belongs to.

`backlog.md`'s verification debt also carries **the owner's judgement on whether the swipe motion feels
right** (§44: tactile, quiet, controlled), open since wave B and still open after 013. This item's
walkthrough closes it or restates it.

### 1.4 What this item is not

It is not the theme-switch flicker — that is item 022, designed and approved, on
`feat/022-android-appearance-switch-motion`. The two share no files.

---

## 2. Story

As a **reader**, I want a swiped card to leave and the next one to arrive, so that deciding about an article
reads as a queue advancing rather than as content being replaced between frames.

---

## 3. Amendment 11 — authored by this item, approved at the plan gate

§43 step 3 says the next card *"appears"* and no section specifies how; §44 specifies the card's response to
the gesture and does not name opacity. Both are `docs/v1/**` changes. The precedent is Amendment 10, which
item 022 authored for the same reason one week earlier.

**Amendment 11, Android Card Swipe Motion**, is narrow and Android-only:

- adds **§79.5**, specifying the Android card's exit and entrance — the exit deferring to §44.2 for curve
  and duration and additionally fading to transparent, matching the browser reference implementation; the
  entrance a **rise and fade in place**, with no lateral movement, no scaling and no rotation;
- states that the entrance **does not gate input**: §43's sequence and item 013's settled behaviour are
  unchanged, and the arriving card accepts a swipe from the first frame it is on screen;
- adds *the card entrance (§79.5)* to **§48**'s reduced-motion list;
- changes **no** behaviour: not a state transition, status value, signal, delta, count, ranking, deck
  ordering, undo path, undo arithmetic, gesture semantic, threshold, cue, commitment rule or authored
  string. §39–§43 are untouched, §23's one-primary-card rule is untouched, and §47's prohibitions continue
  to bind.

The amendment binds the Android client only. No `js/**` or `css/**` change is authorized or required.

---

## 4. Out of scope

- The theme-switch flicker (item 022).
- §23's optional offset secondary card. It is permitted and is not drawn on Android today; drawing it is
  its own item and the owner's chosen entrance does not require it.
- Any change to §39–§43 — the swipe surface, threshold, cues, commitment, or the post-commitment sequence.
- Any change to the exit **distance**: `EXIT_FRACTION`, `EXIT_MINIMUM_DP` and the rotation constants are
  faithful ports and stay (§1.2).
- Any change to the browser. The fade is brought to Android; nothing is taken from `js/**`.
- Any behaviour change to undo, attribution, or persistence.
- Lateral entrance motion, promotion-from-depth, or any deck-advance metaphor. Explicitly rejected by the
  owner on 2026-09-20 and by §23's prohibition on exaggerated swipe-app aesthetics.

---

## 5. Scenarios

### Scenario: the exit uses Android's curve, not the browser's
When a swipe is committed
Then the card's exit animates on Material 3 Emphasized easing at the M3 duration this item records
And it does not use `cubic-bezier(0.2, 0.8, 0.2, 1)` at `280ms`, which §44.1 scopes to the browser

### Scenario: the card fades as it leaves
When a swipe is committed
Then the card's opacity animates to fully transparent over the exit
And the translate and rotation of the exit are unchanged from today

### Scenario: the replacement rises and fades into place
Given a swipe has been committed and the state action has been processed
When the next eligible card appears
Then it animates from transparent to opaque while rising a short distance to its resting position
And it does not move laterally, scale, or rotate

### Scenario: the arriving card accepts a swipe immediately
Given the replacement card's entrance is still running
When the reader begins a swipe on it
Then the gesture is accepted and tracked from that first frame
And item 013's settled behaviour is unchanged — there is no window in which the card declines a touch

### Scenario: a swipe during an entrance is attributed to the card the reader saw
Given the replacement card's entrance is still running
When the reader commits a swipe
Then the action is applied to the arriving article, not to the one that just left
And item 015's attribution guarantee is unchanged

### Scenario: a restored card arrives the same way
Given the reader undoes a committed swipe
When the restored article becomes the head card
Then it arrives by the same entrance
And it accepts a swipe from its first frame, per item 013

### Scenario: a reduced-motion preference removes both
Given a reduced-motion preference is set
When a swipe is committed
Then the card leaves and the replacement arrives effectively immediately, with no fade and no rise
And the outcome remains fully clear from text, state and live status

### Scenario: nothing bounces, pulses or celebrates
When a card leaves or arrives
Then no motion on §47's prohibited list occurs
And the motion stays within §44's tactile, quiet, controlled character

### Scenario: a vertical drag still belongs to the scroll
Given the card is inside Discover's vertical scroll
When the reader drags vertically on the card during or after an entrance
Then the scroll receives the gesture, per item 008's intent lock and `ArticleCardScrollGestureTest`

---

## 6. Verification

### 6.1 Gates

Per `execution-model.md` §8, from `android/`, with both exported or Gradle fails before any test runs:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
ANDROID_HOME=$HOME/Library/Android/sdk

./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

Hosted CI green on the exact final head before the final review merges.

### 6.2 The regression surface is the point of this item

**`ArticleCardGestureTest` and `ArticleCardScrollGestureTest` must pass unedited.** They are items 013's and
008's guards, they are instrumented, and `backlog.md` records that this surface's defects have historically
been invisible to the JVM gate. An implementer who finds themselves editing either has changed a settled
behaviour and must stop and report.

**New instrumented coverage is required for the two scenarios a JVM test cannot reach:** that a swipe begun
during an entrance is accepted, and that it is attributed to the arriving article.

### 6.3 What is JVM-assertable

The exit spec's curve and duration as a selected value; the entrance spec's curve, duration and travel; and
that the reduced-motion branch selects an immediate spec for both. Per `021 slices.md`, **do not assert that
a duration constant equals its own value** — assert the selection, not the literal.

### 6.4 Walkthrough — required evidence

On a device, `screencap`, **and with two preconditions that cost this project a misdiagnosis on
2026-09-20**:

- `adb shell settings get global animator_duration_scale` must not be `0`. At `0` the app is correctly in
  reduced motion and every spec is `snap()`; instrumented runs leave it there.
- A **release** build, not debug. Note that this project sets `isMinifyEnabled = false` and ships no
  baseline profile, so the result is a floor rather than a ceiling.

1. Swipe left and swipe right: the card fades as it leaves.
2. The replacement rises and fades into place.
3. A swipe begun while the entrance is still running is accepted and lands on the arriving article.
4. Undo: the restored card arrives the same way and is immediately swipeable.
5. With the system's remove-animations setting on: both are immediate.
6. A vertical drag still scrolls.

### 6.5 Owner checkpoints

1. **Amendment 11's text**, at the plan gate.
2. **The walkthrough**, which carries §44's character judgement — *tactile, quiet, controlled* — open in
   `backlog.md` since wave B and unclosed after 013. This item is the fourth on this surface; the judgement
   is the deliverable, not a formality.
