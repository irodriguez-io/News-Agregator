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
- **Status:** **done** — RED `1d160a3`, GREEN `48c4483`. Gate reproduced independently: **382 unit tests,
  0 failures**, `assembleDebug` and `assembleDebugAndroidTest` successful (baseline 381). **Instrumented
  suite run by the reviewer: 14 tests, 0 failed** (the 12 that existed plus 2 new). Slice review PASS.

**RED was a value failure on the direction function**, reproduced as
`READ_LATER -> DISCOVER expected:<FROM_RIGHT> but was:<FROM_LEFT>` — the function was written in RED
returning a deliberately wrong direction, and is unit-tested over **all six ordered pairs**. **No vacuous
duration assertion was written**, per D1.

**Where a RED could not fail, that was reported rather than contrived.** The reduced-motion and undo-offer
instrumented assertions *passed against the original bare `when`*, because they guard behaviour that already
held. The implementer said so explicitly instead of manufacturing a failure.

### The predicted casualty happened, and the protocol worked

`spec.md` §5.3 named *"any test asserting screen content by composition structure"* as the likeliest
casualty, and the dispatch reconciliation named **`MainActivityLaunchSmokeTest`** as the most exposed of the
five bounds-locating instrumented tests.

**It failed** on the first full post-change run: `AnimatedContent` evaluated its transition during initial
equal-state composition.

**The implementer reported it before editing anything**, then fixed **production** — a no-op transform when
the state is unchanged — and left the test untouched. That is §2.1 rule 5's protocol executed exactly as
written, on the one occasion in this wave where it was actually needed.

**Verified unedited:** `MainActivityLaunchSmokeTest`, item 019's `DiscoverScreenLayoutTest` (Amendment 7's
ordering guard), item 020's `ReadingListLayoutTest` (the toast-overlap guard), `ArticleRowLayoutTest` and
`CategoryChipRowLayoutTest`.

**Wave D's ground is intact.** The scaffold diff adds animation imports and wraps the `when (destination)`;
`UndoToast`, `LiveStatusMessage`, `SettingsSheet`, the back handler and the recovery notice are **all
untouched**. M3 Emphasized easing is implemented as a real two-segment `PathEasing`, not a guessed
cubic-bezier.

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
   resolves `reducedMotion` in the same composable. **Verified at dispatch:** the `when` is at line 261 and
   `reducedMotion` resolves at line 92. Item 018 edited only the app bar, shifting lines by four without
   restructuring; the file is 509 lines.

   **Also verified:** `UndoToast` (line 341), `LiveStatusMessage` (347) and `SettingsSheet` (354) are all
   hosted **after** and outside the destination branch, which is what makes Amendment 8's cross-destination
   offer work and what slice 1 must not disturb.
3. **`SettingsSheet.kt` still uses `ModalBottomSheet` with `rememberModalBottomSheetState`.** If it has
   become a dialog, slice 2's scope changes.
4. **The bottom bar's destination order is still Read Later, Discover, History** (§18). The direction
   function is derived from it; a different order silently inverts every transition.
5. **`UndoToast` is still hosted globally**, outside the destination branch. If item 020's inset work moved
   it, the cross-destination offer scenario needs re-checking — and moving it was out of 020's scope.

---

## Reconciled at dispatch — two things changed since this item was designed

**1. Instrumented tests now compile AND run in CI** (PR #32), on a pinned 411 dp emulator. This item's
`spec.md` §5.2 listed three claims as *"assertable in a Compose UI test"* — that a reduced-motion preference
produces no animation, that the sheet traps and restores focus, and that a live undo offer survives a
destination change. **When this item was designed those would have been written and never run.** They are
now gateable, and this item's two most consequential guarantees — reduced motion honoured, and Amendment 8's
cross-destination offer surviving — belong there rather than in a walkthrough note.

**2. The "likeliest casualty" is no longer abstract.** §5.3 warned that *"any test asserting screen content
by composition structure"* could break when `AnimatedContent` adds a layer. There are now **12 instrumented
tests across 7 files, and five of them locate nodes by `boundsInRoot`:**

| Test | Exposure to an `AnimatedContent` layer |
|---|---|
| `MainActivityLaunchSmokeTest` | **high** — launches the real activity through the scaffold |
| `ReadingListLayoutTest` (020's toast-overlap guard) | **high** — asserts toast-vs-row bounds in the real hosting |
| `DiscoverScreenLayoutTest` (019's Amendment 7 ordering guard) | medium — composes the screen, but asserts bounds ordering |
| `CategoryChipRowLayoutTest`, `ArticleRowLayoutTest` | low — component-scoped |

**019's ordering guard and 020's toast guard did not exist when this item was designed.** They are the two
newest and most valuable assertions in the project, and this item is the one that can break them.

**Four items now merge beneath this one, not three.**

## On existing assertions

**Four items merge beneath this one, and this file is wave D's ground.** `spec.md` §5.3 names five cases
with reasons and is **not a freeze**.

Per `execution-model.md` §2.1 rule 5: read the tree at preflight, and **report any unlisted failure before
editing it.** The undo tests from items 013, 014 and 016 all live in this file's territory; a motion item
that edits one of them has almost certainly changed behaviour it was not authorised to change.
