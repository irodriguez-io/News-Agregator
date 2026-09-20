# 023 — slice plan

Two slices, strictly sequential, sharing one item branch and one PR.

Small item, deliberately. `D2` is what keeps it small: the exit and the entrance are already sequential in
the existing code, so neither slice restructures anything — slice 1 corrects values and adds one animated
property, slice 2 adds two more to the same block.

**Amendment 11 and the `docs/v1/**` edits land with the design commit**, before slice 1, approved at the
plan gate — the pattern item 022 used one week earlier.

### Merge order against 022 — known, not to be discovered at merge

**022 and 023 both amend `docs/v1/README.md`'s amendment log and §48's reduced-motion list, and both were
cut from `main` before either merged.** They will conflict in those two files, and both branches also
correct §79.3's "both transitions above" wording independently.

**Merge 022 first, then rebase 023 onto `main`.** The conflicts are mechanical — two appends to an ordered
log, two appends to a list, and one wording correction made twice — but they are conflicts, and
`waves/wave-e.md` is explicit that several items amending `06-ui-ux.md` on separate branches is a merge
nobody should be asked to review. On the rebase, §48's list must end up carrying **both** §79.4 and §79.5,
and §79.3's correction must be applied once, not twice.

Nothing in the **code** conflicts: 022 touches the manifest and `ui/theme/**`, 023 touches
`ui/components/ArticleCard.kt` and `ui/gesture/SwipeGesture.kt`. The two items are otherwise independent
and may be implemented in either order.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **No `AnimatedContent`, no `Crossfade`, no second composed card** (D2). The overlap does not exist today
   and creating it reopens items 013 and 015. This is the item's central constraint.
2. **The exit distance does not change.** `EXIT_FRACTION`, `EXIT_MINIMUM_DP`, `ROTATION_DIVISOR` and
   `MAX_ROTATION_DEGREES` are faithful ports of `js/ui/swipe.js` and stay (`spec.md` §1.2).
3. **`ArticleCardGestureTest` and `ArticleCardScrollGestureTest` are not edited.** They are items 013's and
   008's guards. If either fails, a settled behaviour moved — stop and report.
4. **The entrance does not gate input** (D4). A card at `alpha = 0f` on its first frame is swipeable. Do not
   "fix" this by delaying the pointer handler.
5. **Reduced motion is `snap()`, not a fast animation** (D6). §48 says effectively immediate.
6. **Assert the selection, not the literal.** Do not write a test that a duration constant equals its own
   value (`021 slices.md`).
7. **No behaviour changes.** §39–§43, attribution, undo, persistence, counts and every authored string are
   untouched.
8. **No new dependency** (D8).

---

## Slice 1: the card leaves on Android's curve, and fades as it goes

**Objective.** Bring the exit into compliance with §44.2 — Material 3 Emphasized at `300ms` — and add the
opacity fade the browser has and Android does not.

- **Scenarios:** the exit uses Android's curve, not the browser's; the card fades as it leaves; a
  reduced-motion preference removes both (exit half); nothing bounces, pulses or celebrates.
- **Files:** `ui/components/ArticleCard.kt` (the `motionSpec` and the `graphicsLayer` block),
  `ui/gesture/SwipeGesture.kt` (constants and their citations), and their JVM tests.
- **Must not touch:** `ui/screens/discover/**`, the exit distance constants, the intent lock, the threshold,
  any cue, any commitment rule.
- **Failing-first test:** that the exit spec selects M3 Emphasized at the recorded duration rather than
  §44.1's browser values, and that the exit's alpha target is fully transparent — and `snap`-equivalent
  under reduced motion.
- **Reaches green alone because:** it changes an animation spec and adds one draw-time property. The gesture
  state machine, its ten scenario tests, and both instrumented guards are untouched.
- **Definition of done:** all four gates green; `ArticleCardGestureTest` and `ArticleCardScrollGestureTest`
  passing **unedited**; item 008's `SwipeGesture` unit scenarios passing unedited; the new constants
  carrying a §79.5 citation in the block's existing style.

---

## Slice 2: the next card rises and fades into place

**Objective.** Give the replacement card an entrance — opacity `0 → 1` and a `12dp` rise to its resting
position over `300ms` on a decelerated curve — with no lateral movement, and without gating input.

- **Scenarios:** the replacement rises and fades into place; the arriving card accepts a swipe immediately;
  a swipe during an entrance is attributed to the card the reader saw; a restored card arrives the same way;
  a reduced-motion preference removes both (entrance half); a vertical drag still belongs to the scroll.
- **Files:** `ui/components/ArticleCard.kt` (the same `graphicsLayer` block and one new `Animatable` keyed
  on the existing `remember(article.id, …)`), `ui/gesture/SwipeGesture.kt` (two constants), JVM tests, and
  **new instrumented tests**.
- **Must not touch:** `ui/screens/discover/**`, the commit sequencing in the gesture's release path, the
  `remember` key set, any undo path.
- **Failing-first test:** JVM — that the entrance spec selects the recorded duration, curve and rise, and
  `snap`-equivalent under reduced motion. **Instrumented — that a swipe begun while the entrance is running
  is accepted, and that it commits against the arriving article and not the one that left.** The second is
  item 015's guarantee re-asserted against new motion and is the slice's real acceptance criterion.
- **Reaches green alone because:** the entrance is two draw-time properties on a card that is already
  composed; hit testing and composition are unaffected (D4), so no existing gesture behaviour changes.
- **Definition of done:** all four gates green; both existing instrumented guards passing **unedited**; the
  two new instrumented tests passing on the emulator job; the reduced-motion branch asserted for both halves.

---

## Walkthrough

Owner-driven, after both slices, per `spec.md` §6.4 — **with `animator_duration_scale` verified non-zero and
on a release build**, both of which cost a misdiagnosis on 2026-09-20.

It carries §44's character judgement — *tactile, quiet, controlled* — which `backlog.md` has held open since
wave B and which item 013 did not close. This is the fourth item on this surface; closing it is the
deliverable.

If the finding is "sluggish", the lever is the **entrance** duration, not the exit (D7). The exit is fixed by
§44.2.
