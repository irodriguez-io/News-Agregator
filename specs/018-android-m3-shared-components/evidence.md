# 018 — Material 3 Expressive shared components · evidence

**Branch:** `feat/018-android-m3-shared-components`, cut from `main` at `98d4d4e`\
**Slices:** 3, all done\
**Tests:** 315 → **343**, 0 failures throughout\
**Implementer:** Codex (`gpt-5.6-sol high`), four fresh sessions — one per slice plus one findings follow-up\
**Reviewer:** the orchestrating Claude session — spec and plan author, not code author
(`execution-model.md` §5)

---

## 1. Forecast reconciliation — `/feature-implementation` Step 0.4

Item 017 merged **after** this item was designed, so all five of `slices.md`'s assumptions were checked
against `main` at `98d4d4e` before the first brief. **All five held.**

One thing was **added** to the plan at `d7f9399`, from a discovery in 017's walkthrough that post-dated this
design: `BottomNavigationBar.kt:111` sets `indicatorColor = tokens.fg` **explicitly**, so it overrides the
colour scheme and 017's `secondaryContainer = tonal` mapping does not reach the pill on its own. Slice 2's
DoD was already correct; what was missing was *where* it gets delivered. The note also flagged that
`selectedIconColor = tokens.surface` would become near-invisible on a pale pill — a consequence, not a
separate defect, and precisely the kind that passes a unit test and fails a walkthrough.

## 2. Gate runs

Every figure was **reproduced by the reviewer** with `--rerun-tasks` in a throwaway worktree
(`/tmp/verify-018`), per `execution-model.md` §5.1 control 1. No implementer-reported number was accepted.

| Slice | RED | GREEN | Reviewer-reproduced gate |
|---|---|---|---|
| 1 | `1ec1fbd` | `779fdaa` + findings fix `7a6a923` | 322 tests, 0 failures, 0 errors; `assembleDebug` ✓ |
| 2 | `5cafab2` | `852f950` | 330 tests, 0 failures, 0 errors, 0 skipped; `assembleDebug` ✓ |
| 3 | `d347185` | `eb4f375` | 343 tests, 0 failures, 0 errors, 0 skipped; `assembleDebug` ✓ |

Baseline at branch point: **315 tests, 0 failures.** Net **+28 tests**, none deleted or suppressed.

## 3. Failing-first evidence

All three slices produced **value-failing REDs**, reproduced independently:

- **Slice 1** — `322 tests completed, 3 failed`: `expected:<0.95> but was:<0.8>`,
  `expected:<52.0.dp> but was:<40.0.dp>`, `expected:<1.5.dp> but was:<1.0.dp>`. The file was new, so it was
  created in RED with deliberately wrong values.
- **Slice 2** — `325 tests completed, 3 failed`: indicator expected `tonal`, selected icon expected
  `onTonal`, and the **pair** expected `(tonal, onTonal)`.
- **Slice 3** — `343 tests completed, 7 failed`, with expected/found pairs for the app bar's composable and
  type style, both chip colour triples, and the three named dimensions against `null`.

**A discipline gap in slice 2, and the requirement added because of it.** Five tests landed in slice 2's
GREEN commit and three could have failed on the old code, so they were never observed to fail. Perturbation
confirmed all three discriminate — dropping `.clip(shapes.bottomBar)` fails 1, re-introducing a vertical
offset fails 1, introducing a `Color(...)` literal fails 4. Slice 3's DoD then required **every could-fail
test in RED**, and slice 3 complied: all 13 of its tests are in the RED commit.

*One reviewer perturbation was itself ill-chosen — a raw `dp` literal against the literal-scan test, which
correctly does not forbid dimensions, since it scans for `Color(`, `RoundedCornerShape(` and `Font*(` only.
The test was right and the perturbation was wrong.*

## 4. The one finding, and it was the brief's fault

**Slice 1, FINDINGS then PASS.** The dispatch brief said *"no colour, radius, dimension or font literal —
every value from 017."* The **dimension** half was wrong: `06-ui-ux.md` §77.1 prohibits a component naming a
**colour** and says nothing about dimensions, and item 017 was never asked to define 52 dp (§32.2), 56 dp and
1.5 dp (§35.2) or 48 dp (§72.2) — those are component and accessibility specifications.

With no correct way to satisfy the instruction, the implementation satisfied its letter by deriving all four
from the spacing scale: `sectionGap + gutter + baseUnit`, `sectionGap + gutter`, `sectionGap + tabletMargin`,
`baseUnit * 3f / 8f`. Every value numerically right, every expression semantically false.

**The consequential one was `minimumTouchTarget`,** which made §72.2's accessibility floor a function of two
spacing values — so a later spacing change could have silently dropped the app below an accessibility floor
with no test naming the cause.

Fixed at `7a6a923` as four named constants citing their sections. What settled the judgement: the test
already asserted `52.0.dp` and `1.5.dp` as literals, so there was no principled reason production could not
be equally direct. Slices 2 and 3 followed the corrected rule, and slice 2's constant even carries the
comment *"Never derived."*

## 5. Existing assertions changed

**None.** Item 017's thirteen theme tests, both launch tests, every wave-D undo test in
`IntentionalReadingApp.kt`'s ground, and each earlier slice's tests all stayed green and **unedited**
throughout. No `@Ignore`, `@Disabled` or `assumeTrue` was introduced. **No unlisted test failed at any
point**, so §2.1 rule 5's report-before-editing protocol was never invoked.

## 6. Two things the wave brief got wrong, and one thing the reviewer got wrong

**The wave brief's file allocation** gave all of `ui/components/**` to this item. Four of its files are
called only from Read Later and History and belong to item 020. Corrected at design time in PR #29 and
honoured here: `ArticleRow.kt`, `EditorialHeader.kt`, `EmptyStatePanel.kt` and `StatBand.kt` are untouched,
as are `ArticleCard.kt` and every screen.

**The reviewer's own file knowledge was incomplete.** The reviewer stated during 017's walkthrough that the
Android bar had no vertical lift. It did — `Modifier.offset(y = (-7).dp)` behind an `elevated` flag on a
private per-item helper, missed by reading only the first 60 lines of the file. Slice 2's brief happened to
be correct by specification (§18.2's one-baseline rule) rather than by knowledge, and the implementer found
the offset and removed it.

## 7. Slice reviews

Three verdicts: **FINDINGS then PASS** (slice 1), **PASS** (slice 2), **PASS** (slice 3). One follow-up brief
dispatched, to a fresh session, resolved in a single `refactor(android):` commit with the test count
unchanged and no test edited.

**Non-blocking observations recorded rather than filed:**

- Named dimensions use `Dp(52f)` rather than the more idiomatic `52.dp` — likely residue of the brief's ban
  on `.dp` literals. Functionally identical.
- The three shared controls ship **unconsumed** by design (D1), so between this merge and items 019's and
  020's, the tree holds two treatments of the same control. Bounded and intended.

## 8. Definition of done

| Item | Status |
|---|---|
| Filled primary 52 dp, `primary` fill, `onPrimary` label (§32.2) | ✓ |
| Tonal secondary, `tonal` fill, `onTonal` label (§33.2) | ✓ |
| Triage 56 dp, 1.5 dp `secondary` outline, accessible name (§35.2) | ✓ |
| Pressed 12% + 0.95 scale; disabled 38%, non-interactive (§37.2) | ✓ |
| Bottom bar 28 dp top corners from `shapes.bottomBar` (§76.3) | ✓ |
| Indicator reads `tonal`; selected icon reads `onTonal` — **asserted as a pair** (§8.2) | ✓ |
| Destinations, order and counts unchanged (§17, §18, §3.5) | ✓ asserted |
| All three destinations on one baseline — the 7 dp lift removed (§18.2) | ✓ |
| Nav targets ≥ 48 dp, bar ≥ 54 dp (§18, §72.2) | ✓ named constants, cited |
| App bar: centred masthead in the editorial register (§76.4) | ✓ `CenterAlignedTopAppBar` + `headlineSmall` |
| One settings control, ≥ 48 dp target, not a destination (§20.2, §2.3) | ✓ |
| Chips 40 dp visible, ≥ 48 dp target, pill shape (§22.2, §72.2) | ✓ |
| Chip selected = `primary` fill; unselected = `outlineControl` (§22.2, §78.3) | ✓ |
| **Chip outline ≥ 3:1 in both schemes, asserted as a ratio** (§73.1) | ✓ |
| Selection state not colour-only (§73) | ✓ via `stateDescription` |
| Shared controls ship unconsumed; 019's and 020's files untouched | ✓ |
| No new dependency; no colour, radius or font literal | ✓ asserted by source scan |
| No behaviour change | ✓ no status, count, signal or undo record touched |

## 9. The 360 dp check, and the limit of its durability

**D4's risk was measured, not argued.** An instrumented test on a 360 dp Pixel 10 confirmed the chip row
stays 48 dp high and horizontally scrollable, with a 40 dp visible pill accepting taps through the expanded
target. The stop condition did not trigger.

**That evidence is not gate-protected.** `android.yml` runs only `:app:testDebugUnitTest :app:assembleDebug`
— **no instrumented tests, and `androidTest` sources are never compiled in CI at all.** So
`CategoryChipRowLayoutTest.kt` proves its claim at the moment it was run and will not fail a future
regression; a break in that file would not even fail to compile in CI.

This is a **pre-existing project gap**, not this item's defect — item 018 is simply the first to have a stake
in it. Worth a decision before item 019, which inherits a 360 dp requirement of its own.

## 10. Walkthrough

**Outstanding.** Batched at wave close per `execution-model.md` §6, plus the owner look this item's merge
requires (`wave-e.md` checkpoint 4). `spec.md` §5.4 states the steps.

**The step that matters most:** the active pill must read as an *indicator* rather than a highlight, with a
legible icon on it. That is the pairing slice 2 was briefed around, and it is a visual judgment no test
settles.

## 11. Hosted CI

Recorded at PR time.
