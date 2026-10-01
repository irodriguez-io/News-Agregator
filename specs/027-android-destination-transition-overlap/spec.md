# 027 — One destination's text at a time

**Surface:** Android only, the destination transition in `ui/IntentionalReadingApp.kt`.
**Authority:** `docs/v1/06-ui-ux.md` §18, §47, §48, §79.1, §79.3, §79.4 (second edition);
**Amendment 13, authored by this item**; `specs/backlog.md` Road to done row 4.
**Branch:** `feat/027-android-destination-transition-overlap`, cut from `main` at `2397ec4`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 The finding

Raised during item 023's walkthrough on 2026-09-22, on a signed release build: moving between Read Later,
Discover and History *"renders the outgoing and incoming labels simultaneously for an instant"*
(`specs/023-android-card-swipe-motion/evidence.md` §4, finding 3). **Added to the Road to done by the owner by
name, 2026-09-29** (`definition-of-done.md` §3).

### 1.2 The cause, verified in code

The transition is item 021's `AnimatedContent` at `IntentionalReadingApp.kt:280-385`. Per §79.1 the incoming
destination slides in laterally over `300ms` while the outgoing one scales to `0.95` and fades to `0.8`
opacity (`:283-310`). The two overlap for the whole slide, and **neither destination draws a background of
its own**: `ReadLaterScreen` (`screens/readlater/ReadLaterScreen.kt:39`) and `HistoryScreen`
(`screens/history/HistoryScreen.kt:43`) are bare `LazyColumn`s, and Discover's only `Surface`s are its
loading and state panels (`screens/discover/DiscoverScreen.kt:178, 203`). The only background is the
`Scaffold`'s `containerColor = tokens.bg` (`:216`), beneath both. So the incoming destination's text is
drawn directly over the outgoing destination's text, which is still at 80% opacity.

The bottom bar is outside `AnimatedContent` and does not move; the overlapping "labels" are the
destinations' headers and content.

### 1.3 Why it needs an amendment

The freeze test (`definition-of-done.md` §3) found it is **not a defect**: §79.1 asks for exactly an
outgoing destination that stays 80% visible while the incoming one slides in, and is silent on whether the
incoming one covers it. Choosing that it does is filling in a silent specification, which an implementation
may not do (`AGENTS.md`). The precedent is Amendments 10–12, authored by items 022–024 for the same reason.

### 1.4 What the fix is, as the owner chose it on 2026-10-01

**The incoming destination is opaque.** It is drawn on the page background and slides *over* the outgoing
one, like a sheet laid over a page. The outgoing destination still recedes — `0.95` scale, `0.8` opacity —
on the strip the incoming one has not yet reached, and is never visible through it.

**Rejected at the same decision** (`design.md` D1): fading the outgoing destination fully out (text still
overlaps mid-fade, as a ghost); both together (two rule changes where one removes the overlap); Material's
shared-axis pattern (replaces §79.1's motion rather than correcting it).

---

## 2. Story

As a **reader**, I want changing destination to show one destination's text at a time, so that the move
reads as one page replacing another rather than two pages printed over each other.

---

## 3. Amendment 13 — authored by this item, approved at the plan gate

**Amendment 13, Android Destination Transition Coverage**, is narrow and Android-only:

- **§79.1**'s `incoming` row gains *"opaque"*, and one paragraph states that the incoming destination is
  drawn on the page background (`bg`) and covers the outgoing destination as it slides in, so the outgoing
  destination is visible only where the incoming one has not yet arrived and never through it; at rest the
  page background is the one the scaffold already shows, so nothing changes outside the transition;
- changes **nothing else in §79.1** — not the `300ms`, the Emphasized easing, the direction rule, the
  outgoing `scale-down` or its `0.8` opacity;
- changes **no** behaviour: not a destination, the destination order (§18), a state transition, status value,
  signal, count, ranking, undo path, keyboard binding or authored string. §48's reduced-motion rule and
  §79.3 are untouched and continue to bind; §47's prohibitions continue to bind.

The amendment binds the Android client only. No `js/**` or `css/**` change is authorized or required. It
lands with this item's design commit, before slice 1, as Amendments 10–12 did.

---

## 4. Out of scope

- Any change to §79.1's duration, easing, direction, outgoing scale or outgoing opacity.
- The bottom bar, the top app bar, the settings sheet (§79.2), the appearance transition (§79.4) and the card
  motion (§79.5).
- Any change inside `ReadLaterScreen`, `HistoryScreen`, `DiscoverScreen` or their components.
- The browser runtime.
- The other two findings of 023's walkthrough (exit velocity is Road to done row 6; the arrival gap shipped
  as 024).

---

## 5. Scenarios

### Scenario: moving toward Read Later shows only the incoming destination where it has arrived
Given the reader is on Discover with motion enabled
When the reader selects Read Later
And the transition is part-way through, with Read Later entering from the left
Then every pixel inside the area Read Later has slid over shows Read Later or the page background
And no pixel of Discover is visible inside that area

### Scenario: moving toward History shows only the incoming destination where it has arrived
Given the reader is on Discover with motion enabled
When the reader selects History
And the transition is part-way through, with History entering from the right
Then no pixel of Discover is visible inside the area History has slid over

### Scenario: the outgoing destination still recedes where it is uncovered
Given a destination transition is part-way through
Then the outgoing destination remains visible on the strip the incoming one has not reached
And it is drawn at §79.1's `0.95` scale and `0.8` opacity, unchanged

### Scenario: nothing changes at rest
Given no transition is running, in the light scheme and in the dark scheme
Then every destination's background is the scaffold's `bg` for the scheme in effect
And nothing on screen differs from the parent commit (checked at the walkthrough, §6.4)

### Scenario: a reduced-motion preference still makes the change immediate
Given the system's remove-animations setting is on
When the reader changes destination
Then the target destination is composed immediately and the outgoing one is gone, as today
(`DestinationTransitionInstrumentedTest.reducedMotionComposesDestinationAndBackResultImmediately`, unchanged)

### Scenario: an appearance change carries the page background with it
Given the reader changes appearance in Settings while on any destination
Then the destination's background cross-fades with the scaffold's under §79.4, with no frame in which the two
differ

---

## 6. Verification

### 6.1 Gates

With `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
`ANDROID_HOME=$HOME/Library/Android/sdk` exported, from `android/`:
`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.
Counts: **404 JVM**, unchanged; **instrumented 28 + the new tests** (slice plan names them).

### 6.2 §79.1's values are untouched

`git diff main -- android/app/src/main` shows no change to `durationMillis = 300`, the Emphasized
`PathEasing` control points, `targetScale = 0.95f`, `targetAlpha = 0.8f` or `destinationSlideDirection`.

### 6.3 Slowed-motion capture — orchestrator, evidence not gate

On a debug build with `adb shell settings put global animator_duration_scale 10`, screenshot Discover →
Read Later and Discover → History part-way through; record both in `evidence.md`. Restore the scale to `1`.
Overlap is a compositing fact, so a debug build is sufficient here; smoothness is not being judged.

### 6.4 Owner walkthrough — signed release build

Check `animator_duration_scale` reads `1` first. Move Read Later ↔ Discover ↔ History in both directions,
in both schemes: the owner confirms the text never overlaps and the move still reads as §79.1's directional
slide. Uninstall the release build afterwards (`INSTALL_FAILED_UPDATE_INCOMPATIBLE` otherwise).
