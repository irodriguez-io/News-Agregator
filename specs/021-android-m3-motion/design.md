# 021 — design note

Four decisions.

---

## D1 — Extract the direction as a pure function, and assert that

**Decision.** The slide direction is a pure function of (current destination, target destination) over §18's
fixed order. Extract it, unit-test it, and let the composable consume it.

**Why.** §79.1's logic is the *direction*; the duration and easing are constants. A test that asserts a
duration constant equals 300 asserts that a number is itself — item 016's vacuous-assertion finding, where
`var x = 0; assertEquals(0, x)` stood in for a DoD bullet.

The direction function is the only part of this item with real logic, and it is fully testable at the JVM
layer. Everything else is a device judgment or a Compose UI test.

**Consequence:** this item's JVM test count will be small, and that is correct rather than a gap. §5.2 says
so explicitly so nobody pads it.

---

## D2 — Reduced motion is a branch inside each animation, not a wrapper around them

**Decision.** Each animation reads `reducedMotion` at its own site and collapses to an immediate change.

**Why.** §48 requires the *semantic outcome* to survive — the destination change and the sheet's presence
must still be clear. A single outer wrapper that skips animation wholesale risks skipping the state change
with it, and the scrim in particular must **still dim** under reduced motion (it is depth, not motion).

`reducedMotion` already resolves at `IntentionalReadingApp.kt:88`, in the same composable that owns both the
destination branch and the sheet, so no plumbing is added.

**Assert the branch, not the animation.** The reduced-motion path is the one animation-related thing a test
can prove.

---

## D3 — `ui/screens/settings/**` is claimed here

**Decision.** This item owns `SettingsSheet.kt` — §76.7's chrome as well as §79.2's reveal.

**Why.** The wave brief's matrix has no row for it, so §76.7 had no owner. Chrome and reveal read as one
thing: a 28 dp top radius, a scrim and a 350 ms decelerated rise are one gesture, and splitting them across
items would have two people tuning the same impression.

021 runs last, so claiming it collides with nobody.

**Constraint carried over:** §64's focus behaviour — trapped while open, restored on close — is behaviour and
is untouched. The sheet is already a `ModalBottomSheet` (`SettingsSheet.kt:82`), so this is styling, not
replacement.

---

## D4 — Wrapping the destination branch is the smallest change that delivers §79.1

**Decision.** Wrap the existing `when (destination)` at `IntentionalReadingApp.kt:257` in `AnimatedContent`.
Do not restructure navigation.

**Why.** The brief mentions Navigation3 libraries are recorded in the version catalogue and *deliberately
unused*. Introducing them here would be a dependency decision and a navigation rewrite inside a motion item.

**The risk this creates, named because it is the likeliest failure.** `AnimatedContent` adds a layer to the
composition tree. A UI test that locates a node by structure rather than by semantics can break with no
behaviour change — and this file is wave D's ground, carrying items 013, 014 and 016's undo tests plus 012's
scroll target. `spec.md` §5.3 lists them; report an unlisted failure before editing it.

---

## What this note does not decide

**Any value** — durations and easings are §79's, colours and radii are 017's. **Any behaviour** — the undo
offer's raising, lifetime and cross-destination validity (§70, Amendment 8), the back handler, focus
management, swipe semantics and every count are untouched.
