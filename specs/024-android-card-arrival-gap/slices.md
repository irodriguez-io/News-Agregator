# 024 — slice plan

**One slice.** The change is one decision point in one file — when the card requests its commit — plus a pure
geometry function beside it. Splitting it would leave a slice with nothing to prove.

**Amendment 12 and the `docs/v1/**` edits land with the design commit**, before the slice, approved at the
plan gate — the pattern items 022 and 023 used.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **One composed card.** No `AnimatedContent`, no `Crossfade`, no second card (`design.md` D2).
2. **The save does not start before the card has departed.** Not at release, not concurrently with the
   exit (D1). If that looks like the better fix, stop and report — it is a rejected option, not an
   optimisation.
3. **No motion value changes.** `EXIT_DURATION_MS`, `ExitEmphasizedEasing`, `EXIT_FRACTION`,
   `EXIT_MINIMUM_DP`, `ROTATION_DIVISOR`, `MAX_ROTATION_DEGREES`, the fade and the entrance are 023's (D6).
4. **The commit is requested exactly once**, on departure or on curve completion, whichever is first (D3).
5. **These are not edited:** `ArticleCardGestureTest`, `ArticleCardScrollGestureTest`, and 023's three
   entrance tests in `DiscoverScreenLayoutTest.kt:57-176`. If one fails, a settled behaviour moved — stop.
6. **`performTouchInput` advances the test clock 16–32 ms** (023 `evidence.md` §3). Do not write an assertion
   that depends on it leaving the clock unchanged.
7. **Assert behaviour, not literals.** Do not assert a constant equals its own value (`021 slices.md`).

---

## ~~Slice 1: the card requests its commit once it has left the viewport~~  ·  **done**

**RED `ecc6892`, harness correction `e884150`, GREEN `123528a`.** Slice gate PASS, 2026-09-29. 400 → 403 JVM,
23 → 28 instrumented, all four gates re-run independently. One implementer stop, correct: the wide-viewport
RED test laid out 840 dp while `LocalConfiguration` still reported 411 dp; corrected in the harness only.
One superseded `ArticleCardTest` assertion removed in RED — it encoded the wait for the whole exit that
Amendment 12 replaces.

**Objective.** Request the swipe commit on the first frame the departing card's rotated bounds are wholly
outside the viewport, or when the exit curve completes if that never happens — exactly once — without
changing any motion value, and without opening a window in which the arriving card mis-attributes or
declines a swipe.

- **Scenarios:** all seven in `spec.md` §5.
- **Files:** `ui/gesture/SwipeGesture.kt` (the pure departure function, with a §79.5 / Amendment 12
  citation in the block's existing style), `ui/components/ArticleCard.kt` (self-measurement per D3, and the
  commit request per D4), `SwipeGestureTest.kt` / `ArticleCardTest.kt` (JVM), and new instrumented tests —
  in `DiscoverScreenLayoutTest.kt` beside 023's, reusing its `startReplacementEntrance` host, or a new class.
- **Must not touch:** `ui/AppViewModel.kt`, `ui/screens/discover/**`, `ui/IntentionalReadingApp.kt`,
  `data/**`, `domain/**`, any motion constant or spec function, the non-commit restore path
  (`ArticleCard.kt:140-141`), the intent lock, the threshold, any cue.
- **Failing-first tests:**
  - **JVM** — the departure function is false while any rotated edge of the card is inside the viewport and
    true once none is, leftward and rightward, including at `MAX_ROTATION_DEGREES`; and at a wide viewport
    (e.g. 840 dp) the exit target never satisfies it, so the backstop governs.
  - **Instrumented** — (a) after a committed swipe at phone width, the commit callback has been invoked
    exactly once before 300 ms of test clock have elapsed since release, and exactly once after the curve
    completes; (b) with the callback completing `true` and the head advanced, a swipe on the replacement
    begun before the departed card's 300 ms curve would have ended is tracked and commits against the
    replacement article; (c) with the callback completing `false` mid-exit, the card returns to rest at full
    opacity, the head is unchanged, and a new swipe is accepted.
- **Reaches green alone because:** it moves one call earlier inside the existing commit launch and adds a
  pure function; state, persistence, the view model and every motion value are untouched.
- **Definition of done:** all four gates green; the five tests in item 5 above passing **unedited**; the
  three new instrumented tests passing on the emulator job; the departure function covered for both
  directions, maximum rotation, and the wide-viewport fallback.
- **Status:** done.

---

## Walkthrough

Owner-driven after the slice, per `spec.md` §6.3 — **release build, `animator_duration_scale` verified
non-zero, release package uninstalled afterwards.** Judge a swipe side by side with undo's restore.

It carries §44's *tactile, quiet, controlled* judgement. **Test `design.md` D7's prediction at it** and write
the result into `evidence.md`: if a pause remains, the lever is the save's start, not any motion value.
