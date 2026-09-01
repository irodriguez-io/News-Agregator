# 021 — Material 3 Expressive motion

**Item:** 021\
**Branch:** `feat/021-android-m3-motion`\
**Wave:** E, fifth and last item — runs after 018, 019 and 020 have merged\
**Cut from:** `main`, after 020 is merged

Cites, and amends nothing: `docs/v1/06-ui-ux.md` §§17, 18, 44, 45, 46.2, 47, 48, 51, 64, 64.2, 76.7, 79,
79.1, 79.2, 73; **Amendment 8**.

---

## 1. Why this item exists

Everything else in wave E is static. This item makes moving between destinations, and opening Settings,
feel spatial rather than instantaneous — and it is the item most able to do harm, because motion is the one
thing this project's specification prohibits at length.

### 1.2 It runs last for two reasons

**The transition is indexed to bar position.** §79.1 makes the slide direction a function of where the
destination sits in the bottom bar, so item 018's bar must be final first.

**It shares `IntentionalReadingApp.kt` with item 018**, and that file is also wave D's ground — items 014
and 016 put the undo offer and the reversible-action set through it.

### 1.3 Today there is no transition at all

`IntentionalReadingApp.kt:257` switches destinations with a bare `when (destination)`. Nothing animates.
This item wraps it.

`SettingsSheet.kt:82` already uses `ModalBottomSheet`, so §64.2's container is right and this item styles
and animates it rather than replacing it.

### 1.4 Reduced motion is the acceptance criterion, not a caveat

§48 requires animation to become **effectively immediate** under a reduced-motion preference, with the
outcome still clear from text, state and live status. `reducedMotion` is already resolved at
`IntentionalReadingApp.kt:88` — the same composable that owns the destination branch and the sheet — so
**both animations this item adds sit where the flag already is.** No plumbing.

**Every animation must honour it and a test must assert it** (§48). An expressive transition that ignores
the setting is a defect, not a feature.

### 1.5 The constraints on motion are not this item's to relax

§47 prohibits bounce, confetti, pulse, autoplay, reward animation, celebratory motion and continuous card
movement. §44's swipe character constraint — **tactile, quiet, controlled; never bouncy, playful or
rewarding** — binds both surfaces and is explicitly not superseded.

"Expressive" authorises directional and spatial motion. It authorises nothing on those lists.

### 1.6 `ui/screens/settings/**` was unallocated, and this item claims it

`waves/wave-e.md`'s matrix has no row for `ui/screens/settings/**`, so §76.7's sheet chrome — 28 dp top
radius, surface-card toggles, dimming scrim — had no owner.

This item claims it, because the sheet's chrome and its reveal read as one thing, and because 021 runs last
and therefore collides with nobody.

---

## 2. Story

As **the reader**, I want moving between the three destinations to feel like moving sideways through one
publication rather than replacing a screen, and I want that to stop entirely if I have asked my device to
reduce motion.

---

## 3. Out of scope

- **Every screen's content and composition.** Items 019 and 020.
- **`ui/components/**`** in its entirety, including `UndoToast.kt`.
- **The bottom bar's own appearance.** Item 018's. This item reads its destination order.
- **Swipe motion's behaviour** — the surface, the 90 dp threshold, the cues, the commitment sequence
  (§39–§43). §44's curve may align with the M3 equivalent; its character constraint may not be relaxed.
- **The undo offer's raising, lifetime, eligibility or cross-destination validity** (§70, Amendment 8).
- **Any state, count, ranking or authored string.**
- **Any token value.** All from 017.
- Toast timing and live-status semantics (§45).

---

## 4. Scenarios

### Scenario: moving toward a destination slides in from that destination's side
Given the reader is on Discover
When they select Read Later
Then the incoming screen slides in from the side Read Later occupies in the bottom bar
And when they select History instead, it slides in from the opposite side

### Scenario: the transition uses the specified duration and easing
Given a destination change with motion enabled
When the transition runs
Then it lasts 300 ms
And it uses Material 3 Emphasized easing

### Scenario: the outgoing screen scales down and fades
Given a destination change with motion enabled
When the transition runs
Then the outgoing screen scales down and fades to 0.8 opacity

### Scenario: the Settings sheet rises and fades in on a decelerated curve
Given Settings is closed
When it is opened with motion enabled
Then the sheet slides up while fading in
And the entrance lasts 350 ms on a decelerated curve
And a scrim dims the content behind it

### Scenario: the Settings sheet tucks back out
Given Settings is open
When it is dismissed with motion enabled
Then it slides down while fading out

### Scenario: a reduced-motion preference makes the destination change immediate
Given a reduced-motion preference is set
When the reader changes destination
Then no slide, scale or fade is applied
And the new destination is fully composed immediately
And which destination is active remains clear from the bar and the screen

### Scenario: a reduced-motion preference makes the sheet appear immediately
Given a reduced-motion preference is set
When Settings is opened
Then no slide or fade is applied
And the sheet is present immediately
And the scrim still dims the content behind it

### Scenario: nothing bounces, pulses or celebrates
Given any animation this item adds
When it runs
Then it contains no bounce, overshoot, pulse, confetti or reward motion

### Scenario: swipe response keeps its character
Given the Discover card
When it is dragged and released past the threshold
Then the same action is emitted as before this item
And the card's response is not bouncy, playful or rewarding

### Scenario: the sheet is a modal with 28 dp top corners and a scrim
Given Settings is open
When the sheet is rendered
Then its top corners are 28 dp
And a scrim dims the content behind it
And it is not a full-page destination

### Scenario: the sheet still manages focus
Given Settings is opened and then closed
When focus is inspected
Then focus was trapped while open
And focus is restored on close

### Scenario: changing destination changes nothing but what is shown
Given any destination change
When it completes
Then no article status, count, preference signal or undo record changes

### Scenario: a live undo offer survives a destination change
Given an undo offer is showing
When the reader changes destination inside the offer's lifetime
Then the offer is still present and still actionable

### Scenario: back still returns to Discover
Given the reader is on Read Later or History with Settings closed
When the system back affordance is used
Then the destination becomes Discover

---

## 5. Verification

### 5.1 Gates

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

One gate surface: `android.yml`.

**Definition-of-done diff check** — must print nothing:

```bash
git diff --name-only main... \
  | grep -vE '^android/app/src/(main/kotlin/io/irodriguez/intentionalreading/ui/(IntentionalReadingApp\.kt|screens/settings/)|test/|androidTest/)' \
  | grep -v '^specs/'
```

### 5.2 What is assertable, and what is not

**This item is the least JVM-assertable in the wave, and pretending otherwise would produce vacuous tests.**
Item 006's lesson — do not assert a property through a mechanism that cannot observe it — and item 016's —
a DoD bullet that is unassertable at its layer is a defect in the DoD — both apply directly.

**Assertable at the JVM layer:** the direction *decision* — a pure function from (current destination, target
destination) to a slide direction, given §18's fixed order. **Extract that function and assert it
directly**; it is the part of §79.1 that carries the logic.

**Assertable in a Compose UI test:** that a reduced-motion preference produces no animation; that the sheet
traps and restores focus; that a live undo offer survives a destination change.

**Assertable only on a device:** whether 300 ms on Emphasized easing reads as *controlled* rather than
*playful*. That is §44's and §47's constraint and it is a judgment, not a measurement.

**Do not write a test that asserts a duration constant equals 300.** It asserts that a number is itself.
Assert the direction function, assert the reduced-motion branch, and put the character judgment in the
walkthrough where it belongs.

### 5.3 On the existing assertion surface

Named with reasons, per `execution-model.md` §2.1 rule 5. **Three items merge beneath this one.** Read the
tree at preflight and report anything unlisted before editing it.

| Test | Why this item reaches it | Expected |
|---|---|---|
| items 014 / 016 undo-offer tests in this file's ground | this item edits `IntentionalReadingApp.kt`, where the offer is raised and hosted | **must stay green untouched** |
| item 013's undo gesture-reset tests | the same scaffold | untouched |
| item 012's opened-article scroll test | Discover is composed inside the branch this item wraps | must stay green — wrapping in `AnimatedContent` must not break the scroll target |
| the back-handler test at `IntentionalReadingApp.kt:196` | same file | untouched |
| any test asserting screen content by composition structure | `AnimatedContent` adds a layer | **most likely casualty** — enumerate at preflight |

**The last row is the real risk.** Wrapping a `when` in `AnimatedContent` changes the composition tree, and
a UI test that finds a node by structure rather than by semantics can break without any behaviour changing.
That is a test to fix, not a reason to abandon the transition — but report it before editing.

### 5.4 Walkthrough — required evidence

**This item's walkthrough carries more weight than its tests.**

1. Discover → Read Later → Discover → History → Discover. Is the direction consistent with the bar?
2. Does any transition read as bouncy, playful or rewarding? If yes, that is a §44/§47 finding.
3. Open and dismiss Settings. Does the scrim dim, and does the exit tuck rather than snap?
4. **Enable the system reduced-motion setting and repeat 1 and 3.** Everything immediate; nothing lost.
5. Raise an undo offer, change destination inside its lifetime: still present, still actionable. Capture
   with `screencap` — `uiautomator dump` cannot see the toast. **Score by article id**; a missed Undo looks
   exactly like a passing run.
6. Swipe a card left and right: unchanged outcome, restrained response.
7. Allow ~1.4 s after an action before changing destination, or the switch races the recomposition.

And after each step: *and is what the reader needs next actually on screen?*

### 5.5 Owner checkpoints

A walkthrough look at merge, **and the wave sign-off**, which lands with this item since it is the last:
`wave-e.md`'s checkpoint 5, on a device, in both schemes.

**One judgment only the owner can make:** does the motion read as expressive or as busy? §47's list is
prohibitive but the boundary is taste, and this is the item where the wave's character is decided.

### 5.6 Stop conditions

- The direction logic cannot be extracted as a testable function from §18's order.
- A reduced-motion branch cannot be asserted.
- Wrapping the destination branch breaks item 012's scroll target, or any 013/014/016 undo test.
- The sheet's chrome cannot be styled without changing its focus behaviour (§64).
- A test not listed in §5.3 fails.

---

## 6. The gap this item does not close, and what it closes for the wave

**Swipe motion's curve aligns; its behaviour does not change.** §39–§43 are untouched.

**This item completes wave E.** With it merged, the Android client is the application the design sources
describe, minus imagery — and the only thing standing between it and the full design is a dataset that
carries pictures (§74.2).

**Wave close work follows and is not part of this item:** `evidence.md` per item, the batched walkthrough
record, `backlog.md`, `wave-e-note.md`, and **retiring the legacy token names item 017 kept for the length
of the wave.**
