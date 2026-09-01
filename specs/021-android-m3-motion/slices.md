# 021 — slice plan

Two slices, strictly sequential. This item is smaller than the rest of the wave and padding it would produce
vacuous tests (`design.md` D1).

**Cut from `main` only after 020 has merged.** The transition is indexed to item 018's bar and this item
shares `IntentionalReadingApp.kt` with 018.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **§47's prohibitions and §44's character constraint are not relaxed.** No bounce, overshoot, pulse,
   confetti or reward motion. "Expressive" does not authorise them.
2. **Reduced motion is a branch inside each animation** (D2), and **the scrim still dims under it** — it is
   depth, not motion.
3. **The direction is a pure function, extracted and unit-tested** (D1). Do not assert that a duration
   constant equals its own value.
4. **No navigation library is introduced** (D4). Wrap the existing `when`.
5. **No behaviour changes.** The undo offer's raising, lifetime and cross-destination validity, the back
   handler, focus management, swipe semantics and every count are untouched.
6. **No value is chosen here.** Durations and easings from §79; everything visual from 017.

---

## Slice 1: directional destination transitions

**Objective.** Wrap the destination branch so each change slides in from the side the destination occupies,
with the outgoing screen scaling down and fading — and immediately, with no animation, under reduced motion.

- **Scenarios:** moving toward a destination slides in from that destination's side; the transition uses the
  specified duration and easing; the outgoing screen scales down and fades; a reduced-motion preference makes
  the destination change immediate; nothing bounces, pulses or celebrates; changing destination changes
  nothing but what is shown; a live undo offer survives a destination change; back still returns to Discover.
- **Files:** `ui/IntentionalReadingApp.kt`, a new direction function (theme-free, `ui/` level), and tests.
- **Must not touch:** `ui/screens/settings/**` (slice 2), any component, any screen.
- **Reaches green alone because:** the direction function is new and independently testable, and wrapping the
  branch changes no screen's inputs or callbacks — every screen is called with the same arguments.
- **Definition of done:** both gates green; the direction function unit-tested over all six ordered pairs;
  the reduced-motion branch asserted; **items 012, 013, 014 and 016's tests in this file's ground still
  green**; the back-handler test still green.
- **Status:** pending

**The likeliest failure is a UI test that locates a node by composition structure** (D4). Report it before
editing it.

---

## Slice 2: the Settings sheet's chrome and reveal

**Objective.** 28 dp top corners, surface-card toggles and a dimming scrim, with the 350 ms decelerated rise
and the reverse-tuck exit — and immediate presence, scrim intact, under reduced motion.

- **Scenarios:** the Settings sheet rises and fades in on a decelerated curve; the Settings sheet tucks back
  out; a reduced-motion preference makes the sheet appear immediately; the sheet is a modal with 28 dp top
  corners and a scrim; the sheet still manages focus.
- **Files:** `ui/screens/settings/SettingsSheet.kt`, the sheet's hosting in `ui/IntentionalReadingApp.kt`,
  and tests.
- **Must not touch:** the destination transition from slice 1; the sheet's contents' behaviour — appearance,
  import, export and reset are §64–§66's and unchanged.
- **Reaches green alone because:** the sheet is already a `ModalBottomSheet` with its state and callbacks in
  place; this slice changes its shape, scrim and animation only.
- **Definition of done:** both gates green; focus trapped while open and restored on close, asserted; the
  scrim present under reduced motion; no change to appearance, import, export or reset behaviour.
- **Status:** pending

---

## Assumptions, each checkable at dispatch

1. **017, 018, 019 and 020 have all merged**, and `main` is green on the last of them.
2. **`IntentionalReadingApp.kt` still switches destinations with a bare `when (destination)`** and still
   resolves `reducedMotion` in the same composable. If item 018 restructured the scaffold, re-read before
   planning slice 1.
3. **`SettingsSheet.kt` still uses `ModalBottomSheet` with `rememberModalBottomSheetState`.** If it has
   become a dialog, slice 2's scope changes.
4. **The bottom bar's destination order is still Read Later, Discover, History** (§18). The direction
   function is derived from it; a different order silently inverts every transition.
5. **`UndoToast` is still hosted globally**, outside the destination branch. If item 020's inset work moved
   it, the cross-destination offer scenario needs re-checking — and moving it was out of 020's scope.

---

## On existing assertions

**Three items merge beneath this one, and this file is wave D's ground.** `spec.md` §5.3 names five cases
with reasons and is **not a freeze**.

Per `execution-model.md` §2.1 rule 5: read the tree at preflight, and **report any unlisted failure before
editing it.** The undo tests from items 013, 014 and 016 all live in this file's territory; a motion item
that edits one of them has almost certainly changed behaviour it was not authorised to change.
