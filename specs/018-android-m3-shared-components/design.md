# 018 — design note

Five decisions.

---

## D1 — The shared controls are added unconsumed, not extracted

**Decision.** Ship the filled primary, tonal secondary and circular triage controls as new shared
composables that no existing component calls. 019 and 020 adopt them.

**Why.** Those treatments live inline in `ArticleCard.kt` (019) and `ArticleRow.kt` (020). Extracting them
here means editing both, which puts this item in direct conflict with the two items that follow it and
breaks the wave's file allocation.

**Cost, accepted and visible:** between this merge and 019/020's, the tree holds two treatments of the same
control. That is the same transitional duplication item 017 accepted for token names, for the same reason —
it is bounded, and it ends inside the wave.

**Rejected:** extract now and have 019/020 rebase. It converts a clean sequential dependency into a merge
conflict in files those items are rewriting anyway.

---

## D2 — The corrected file allocation, not the wave brief's

**Decision.** This item owns `IntentionalReadingApp.kt`'s app bar and bar hosting,
`components/BottomNavigationBar.kt`, `components/CategoryChipRow.kt`, and the new shared-control file. It
does **not** own `ui/components/**`.

**Why.** Measured by caller: `ArticleRow`, `EditorialHeader`, `EmptyStatePanel` and `StatBand` are called
only from Read Later and History, so they are item 020's. `ArticleCard` is 019's. The brief's
`ui/components/**` row would have this item editing five files that two later items rewrite.

Recorded in `spec.md` §1.2 and folded back into `waves/wave-e.md` at the end of the design sweep.

---

## D3 — The pill indicator is Material 3's, not a hand-drawn shape

**Decision.** Use `NavigationBarItem`'s own indicator, with `indicatorColor` set to the `tonal` role.

**Why.** M3 already draws, animates and sizes the active indicator, and it already handles the selected
icon and label colours. Hand-drawing a pill behind the icon would reimplement all of that and would have to
be kept in step with item 021's transitions.

**Consequence for §73.** The indicator alone is a colour difference. M3's selected/unselected icon and
label colour pair carries the rest, and the label weight change is what satisfies "not by colour alone" —
assert that, not merely the indicator.

---

## D4 — The 40 dp chip carries a 48 dp target

**Decision.** The chip's visible pill stays 40 dp per §22.2; its touch target is expanded to 48 dp.

**Why.** §72.2 admits no exception, and 40 dp is the one specified dimension in this wave that falls below
the floor. The visible/target distinction is exactly what §72 states.

**Watch at 360 dp.** Expanding targets in a horizontally scrolling row can push the row's content height
up. If the row cannot carry 48 dp targets at 360 dp without breaking, that is a stop condition, not
something to solve by shrinking the target.

---

## D5 — Contrast is asserted as a ratio, in both schemes

**Decision.** The unselected chip's outline uses 017's `outlineControl`, and this item asserts the **ratio**
against the surface behind it, in both schemes.

**Why.** §73.1, and the three candidate values that failed that floor while looking acceptable during wave
E's palette pass. This item is the first consumer of `outlineControl`, so it is where the floor is proven in
situ rather than in the theme's own unit test.

---

## What this note does not decide

**Any value.** Every colour, radius, spacing step and type style arrives from item 017. A control that needs
one 017 did not define is a report to the supervisor (§77.1), never a literal.

**Motion.** Item 021 indexes its directional transitions to the bar this item finalises. Nothing here
animates.
