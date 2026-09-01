# 018 — slice plan

Three slices, strictly sequential. Each closes as a failing-first test commit plus an implementation commit.

**Cut from `main` only after 017 has merged.** Every value this item uses comes from 017's theme.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **No value is chosen here.** Colours, radii, type styles and spacing all come from 017 (§77.1). A
   missing token is a report, not a literal.
2. **The corrected file allocation** (`design.md` D2). `ArticleCard.kt`, `ArticleRow.kt`,
   `EditorialHeader.kt`, `EmptyStatePanel.kt` and `StatBand.kt` are **not** this item's, whatever the wave
   brief's matrix says.
3. **The shared controls ship unconsumed** (D1). Do not adopt them into any existing component.
4. **Destination order and the counts are behaviour** and do not change (§17, §18, §3.5).
5. **Contrast is asserted as a ratio, in both schemes** (D5).
6. **The 40 dp chip gets a 48 dp target** (D4). Shrinking the target is never the fix.

---

## Slice 1: the shared controls

**Objective.** Add the filled primary, tonal secondary and circular triage controls, plus the shared
pressed and disabled treatment.

- **Scenarios:** the shared primary control is filled and 52 dp; the shared triage control is 56 dp with a
  1.5 dp outline; pressed and disabled behave as specified; no colour, radius or font is named outside the
  theme package.
- **Files:** one new file under `ui/components/` for the controls, plus its tests.
- **Must not touch:** anything else.
- **Reaches green alone because:** the file is new and nothing calls it, so nothing can break; its tests are
  the only thing that exercises it.
- **Definition of done:** both gates green; 52 dp, 56 dp, 1.5 dp, 12%, 0.95 and 38% all asserted; triage
  controls carry accessible names; no literals.
- **Status:** pending

---

## Slice 2: the bottom navigation bar

**Objective.** 28 dp top corner radius, the tonal pill indicator, one baseline, compliant targets — with
destinations, order and counts untouched.

- **Scenarios:** the bottom bar keeps its destinations, order and counts; the active destination is marked
  by the tonal container; the active destination is not communicated by colour alone; all three destinations
  sit on one baseline; every navigation target meets the floor; selecting a destination changes nothing but
  what is shown.
- **Files:** `ui/components/BottomNavigationBar.kt` and its tests.
- **Must not touch:** `IntentionalReadingApp.kt` (slice 3), the chip row.
- **Reaches green alone because:** the composable's signature is unchanged — `destination`, `counts`,
  `onDestinationSelected` — so its single caller compiles untouched.
- **Definition of done:** both gates green; the indicator reads from the `tonal` role; targets ≥ 48 dp and
  the bar ≥ 54 dp; the counts still come from local state.
- **Status:** pending

**Do not change the composable's parameters.** Its one caller is `IntentionalReadingApp.kt`, which slice 3
edits — a signature change here would put this slice's compile failure inside the next slice, which
`execution-model.md` §2.1 rule 3 forbids as failing-first evidence.

---

## Slice 3: the top app bar and the category chips

**Objective.** Centre the masthead in the editorial register with its trailing settings control, and turn
the chip row into 40 dp pills with compliant targets and a compliant outline.

- **Scenarios:** the app bar carries one centred masthead and a trailing settings control; a category chip
  is a pill with a compliant target; chip selection states differ by fill and by more than colour; the
  unselected chip's outline clears the control-boundary floor; selecting a category changes nothing but what
  is shown.
- **Files:** `ui/IntentionalReadingApp.kt` (app bar only), `ui/components/CategoryChipRow.kt`, and tests.
- **Must not touch:** the bar from slice 2; any screen; any of 019's or 020's files.
- **Reaches green alone because:** both edits are presentational within existing signatures, and the chip
  row's caller (`DiscoverHeader.kt`) is not touched.
- **Definition of done:** both gates green; the masthead is centred and editorial; exactly one settings
  control, target ≥ 48 dp, not a destination; chips 40 dp visible with ≥ 48 dp targets; the outline's
  contrast ratio asserted in both schemes.
- **Status:** pending

**Two things in this slice are grouped because they are both small and both presentational**, not because
they are related. If either grows, split it rather than letting the slice outgrow one context window.

---

## Assumptions, each checkable at dispatch

1. **017 has merged**, and `outlineControl`, `tonal`, `primary`, `onPrimary`, `secondary`, the shape scale
   and the spacing rhythm all exist in the theme. If any is missing, stop and report.
2. **`BottomNavigationBar` still takes `destination`, `counts`, `onDestinationSelected`** and still uses
   M3's `NavigationBar`/`NavigationBarItem`. If it has been rewritten, re-read before planning slice 2.
3. **The top app bar is still hosted in `IntentionalReadingApp.kt`'s `Scaffold(topBar = …)`** and still
   renders `R.string.app_name` with a trailing settings `IconButton`.
4. **`CategoryChipRow` is still called only from `screens/discover/DiscoverHeader.kt`.** A second caller
   would change the allocation in `design.md` D2.
5. **No new user-facing string is required.** If one is, it is shared copy under §75.2 and this becomes a
   two-gate item — report before adding it.

---

## On existing assertions

`spec.md` §5.3's enumeration is **deliberately thin and says so**, because 017 merges between this design
and this dispatch and adds theme tests this plan cannot name yet.

**Read the tree at preflight.** Per `execution-model.md` §2.1 rule 5, an assertion list written at design
time and used at dispatch time is what went stale on item 016; if a test not on the list fails, report it
before editing it.
