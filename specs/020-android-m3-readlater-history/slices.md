# 020 — slice plan

Three slices, strictly sequential. Runs concurrently with item 019, which shares no file with it.

**Cut from `main` only after 018 has merged.**

---

## Fixed for this item — do not re-decide these mid-implementation

1. **The null rules are preserved, not decided** (`design.md` D1). `if (minutes > 0) … else "Unavailable"`
   stays. Formatting an unknown sum as `0 min` is a defect, not a tidy-up.
2. **No new user-facing string** (D2). `res/values/strings.xml` in this item's diff is a **stop**.
3. **These four files are ours**, whatever the wave brief's matrix says: `ArticleRow.kt`,
   `EditorialHeader.kt`, `EmptyStatePanel.kt`, `StatBand.kt` (D3).
4. **The toast is not touched** (D4). The overlap is fixed with a list inset.
5. **No thumbnail, no reserved region** (D5, §74.2).
6. **No value is chosen here.** All tokens from 017; all shared controls from 018.
7. **No behaviour changes.** Mark read, Remove, Reopen, Mark unread and their count updates are untouched.

---

## Slice 1: the Queue Row

**Objective.** Turn `ArticleRow` into the design's Queue Row — 16 dp radius, tonal fill, no shadow, no
thumbnail, two-line title clamp, compliant action targets.

- **Scenarios:** the Queue Row is a tonal container with no shadow; no thumbnail and no reserved space; the
  row title clamps at two lines; row actions keep compliant targets.
- **Files:** `ui/components/ArticleRow.kt` and its tests.
- **Must not touch:** the screens (slice 3), `StatBand.kt` / `EditorialHeader.kt` (slice 2).
- **Reaches green alone because:** the composable's signature is unchanged, so both callers —
  `ReadLaterScreen.kt` and `HistoryScreen.kt` — compile untouched.
- **Definition of done:** both gates green; the clamp asserted on the title element; targets ≥ 48 dp; no
  media region; no literals; every value the row displays unchanged.
- **Status:** pending

**Do not change the parameters.** Two screens call this row and slice 3 edits both — a signature change here
would put this slice's compile failure into a later slice, which §2.1 rule 3 forbids as failing-first
evidence.

---

## Slice 2: the StatBand and the screen headers

**Objective.** The three-column pill container with editorial numerals, and both screen headers on the new
type scale — with every displayed value and every omission rule untouched.

- **Scenarios:** the StatBand groups three values in a pill container; an unknown reading-time sum is
  omitted, never zeroed; a known sum counts only known values; an unavailable topic is omitted, not
  inferred.
- **Files:** `ui/components/StatBand.kt`, `ui/components/EditorialHeader.kt`, and tests.
- **Must not touch:** the arithmetic. `knownReadingTimeValue` and `availableStatValue` keep their behaviour.
- **Reaches green alone because:** the existing sum and omission tests are the acceptance criteria and
  already exist; this slice is done when they are still green against the new presentation.
- **Definition of done:** both gates green; **the existing null-rule tests green and unedited**; numerals in
  the editorial register; no literals.
- **Status:** pending

**If a null-rule test needs editing to pass, the change is wrong.** Report it.

---

## Slice 3: the two screens, the empty states, and the toast inset

**Objective.** Re-lay out both screens on 017's spacing and shape, raise History's empty state to full
visual weight, and give both lists a bottom inset that clears a showing Undo offer.

- **Scenarios:** History's empty state is high fidelity and uses existing copy; the last row's actions are
  reachable while the Undo toast shows; History groups by local date; every value shown is unchanged; Mark
  unread still returns the article and updates both counts; nothing outside the theme names a value.
- **Files:** `ui/screens/readlater/ReadLaterScreen.kt`, `ui/screens/history/HistoryScreen.kt`,
  `ui/components/EmptyStatePanel.kt`, and tests.
- **Must not touch:** `UndoToast.kt`, `IntentionalReadingApp.kt` (D4).
- **Reaches green alone because:** both screens keep their existing state inputs and callbacks; the inset and
  the layout are presentational.
- **Definition of done:** both gates green; the grouping and Mark-unread tests green; the empty state uses
  only existing strings; **the toast-overlap screenshot captured with `screencap`**, at the bottom of a
  scrolled list with an offer showing.
- **Status:** pending

---

## Assumptions, each checkable at dispatch

1. **017 and 018 have both merged.**
2. **`StatBand.kt` still contains `if (minutes > 0) "~$minutes min" else "Unavailable"` and
   `availableStatValue`.** If the omission logic has moved or changed, stop and report — D1 assumes it is
   there to preserve.
3. **`ArticleRow` is still called only from `ReadLaterScreen.kt` and `HistoryScreen.kt`.** A third caller
   changes the allocation in D3.
4. **The four component files are still unclaimed by 018's merged diff.** If 018 touched any of them,
   report — the allocation correction did not hold.
5. **Every string the empty states need still exists** in `res/values/strings.xml`. If one is missing, that
   is D2's stop condition, not a licence to add it.
6. **The Undo toast is still hosted globally** in `IntentionalReadingApp.kt`. If it has been re-parented,
   the inset fix may no longer be the right one.

---

## On existing assertions

`spec.md` §5.3 names four cases with reasons. **Not a freeze.** Two items merge beneath this one and a third
runs beside it.

Per `execution-model.md` §2.1 rule 5: read the tree at preflight and **report any unlisted failure before
editing it.** The row most likely to bite is the reading-time sum — a presentation change that edits an
arithmetic test has changed behaviour, whatever the diff looks like.
