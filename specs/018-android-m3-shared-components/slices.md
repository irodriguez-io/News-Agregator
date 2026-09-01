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
- **Status:** **done** — RED `1ec1fbd`, GREEN `779fdaa`, findings fix `7a6a923`. Gate reproduced
  independently with `--rerun-tasks`: **322 tests, 0 failures, 0 errors**, `assembleDebug` successful
  (baseline 315). Slice review **FINDINGS, then PASS.**

**Genuine value-failing RED**, reproduced as `322 tests completed, 3 failed` — `expected:<0.95> but was:<0.8>`,
`expected:<52.0.dp> but was:<40.0.dp>`, `expected:<1.5.dp> but was:<1.0.dp>`.

**One finding, caused by an error in the dispatch brief rather than by the implementation.** The brief said
*"no colour, radius, dimension or font literal — every value from 017."* The **dimension** half was wrong:
§77.1 prohibits a component naming a **colour**, and says nothing about dimensions. 52 dp (§32.2), 56 dp and
1.5 dp (§35.2) and 48 dp (§72.2) are component and accessibility specifications, not entries in a spacing
rhythm, and item 017 was never asked to define them.

With no correct way to satisfy the instruction, the implementation satisfied its letter by deriving all four
from the spacing scale — `sectionGap + gutter + baseUnit` for 52, `sectionGap + gutter` for 48,
`sectionGap + tabletMargin` for 56, `baseUnit * 3f / 8f` for 1.5. Numerically correct, semantically false,
and a real hazard: **it made §72.2's accessibility floor a function of the spacing rhythm**, so a later
spacing change could silently drop the app below it.

Fixed at `7a6a923` as four named constants citing their sections. The tell that settled it: the test already
asserted `52.0.dp` and `1.5.dp` as literals, so there was no principled reason production could not be
equally direct.

**Non-blocking:** the constants use `Dp(52f)` rather than the more idiomatic `52.dp` — likely residue of the
brief's ban on `.dp` literals. Functionally identical; not worth a round trip.

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
- **Status:** **done** — RED `5cafab2`, GREEN `852f950`. Gate reproduced independently with
  `--rerun-tasks`: **330 tests, 0 failures, 0 errors, 0 skipped**, `assembleDebug` successful (baseline 322).
  Slice review PASS.

**Both required colour lines changed together**, which was this slice's trap: `indicatorColor` → `tonal` and
`selectedIconColor` → `onTonal`. RED reproduced as `325 tests completed, 3 failed`, including the **pair**
assertion — *"navigation indicator and selected icon expected:<(tonal, onTonal)>"*. Fixing the indicator
alone would have passed a colour assertion and left a near-invisible icon on a pale pill.

**The Android bar did carry the browser's 7 dp Discover lift** — `Modifier.offset(y = (-7).dp)` behind an
`elevated` flag on a private per-item helper. §18.2 drops it and this slice removed it. The public
composable's signature is **unchanged**; only the private helper lost a parameter, so
`IntentionalReadingApp.kt` compiles untouched for slice 3.

Named constants with citations, following slice 1's lesson: `BottomNavigationMinimumHeight` (§18) and
`BottomNavigationMinimumTarget` (§72.2, commented *"Never derived"*).

**A discipline gap, resolved by evidence rather than a round trip.** Five tests landed in the GREEN commit
and three of them could have failed on the old code — the shape-and-size-floors test, the one-baseline test,
and the literal scan. They were never observed to fail. Perturbation confirmed all three discriminate:
dropping `.clip(shapes.bottomBar)` fails 1; re-introducing a vertical offset fails 1; introducing a
`Color(...)` literal fails 4. *A first perturbation attempt was ill-chosen — a raw `dp` literal, which that
test correctly does not forbid, since it scans for `Color(`, `RoundedCornerShape(` and `Font*(` only.*

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
   M3's `NavigationBar`/`NavigationBarItem`. Verified at dispatch on `main` at `98d4d4e`.

   **Reconciled after item 017 merged — a discovery from 017's walkthrough that post-dates this design.**
   `BottomNavigationBar.kt:111` sets **`indicatorColor = tokens.fg` explicitly**, inside
   `NavigationBarItemDefaults.colors(...)`. It therefore *overrides* the scheme, and item 017 mapping
   `secondaryContainer = tokens.tonal` does **not** reach the pill on its own — on device the indicator
   renders as solid ink. Slice 2's DoD is unchanged and still correct; **that line is where it is delivered.**
   The same block also sets `selectedIconColor = tokens.surface`, which will need to become the on-tonal ink
   once the indicator is tonal, or the selected icon will be near-invisible against a pale pill.
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
