# 012 — The Discover card leads the viewport · evidence

**Branch:** `feat/012-android-discover-card-first` · **Worktree:** `news-agregator-012`\
**Cut from:** `main` at `6c857a61881064b048e9939ebc97505d7f462545` (merge of PR #19)\
**Hosted CI green on that commit:** Test `33422480339`, Pages `33422480328`\
**Wave:** D, dispatched 2026-08-31 concurrently with the other of 015 / 012

---

## 1. Gate runs

Recorded at the moment of each run, per `execution-model.md` §5.1 control 4. Delete
`app/build/test-results/testDebugUnitTest` before every run and read the `BUILD SUCCESSFUL` line rather
than the counts (`waves/wave-b-note.md` §7).

| When | Slice | `:app:testDebugUnitTest` | `:app:assembleDebug` |
|---|---|---|---|
| 2026-08-31, base `6c857a6` | — (baseline) | 275 tests, 0 failures, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `2101b1e` | 1 | **279 tests, 0 failures**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `a6dfae1` | after merging `main` (015) | **284 tests, 0 failures**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |

**Item 015 merged as `88e71b7` and was merged into this branch**, per `execution-model.md` §4.4. The
post-merge row is the authoritative one and **the pre-rebase numbers above are superseded, not averaged** —
wave B's cross-item defect was caught precisely by re-gating a rebased head that both branches' green gates
had passed (`waves/wave-b-note.md` §3). No cross-item defect here: 280 (015 on `main`) + 4 (this item's new
cases) = 284, and the merge was conflict-free.

Head rows are the **supervisor's own runs**, `test-results` deleted first
(`execution-model.md` §5.1 control 4).

## 2. Failing-first evidence

One row per slice: the RED, reproduced independently in a throwaway worktree with the fix absent, and the
commit pair.

| Slice | RED reproduced | Test commit | Implementation commit |
|---|---|---|---|
| 1 | **yes, independently** | `b8fbb96` | `2101b1e` |

Reproduced in a throwaway detached worktree at `b8fbb96`, with `DiscoverScrollTargets.kt` confirmed absent:

```
> Task :app:compileDebugUnitTestKotlin FAILED
DiscoverScrollTargetsTest.kt:11:13 Unresolved reference 'DiscoverScrollTargets'.
DiscoverScrollTargetsTest.kt:23:13 Unresolved reference 'DiscoverScrollTargets'.
DiscoverScrollTargetsTest.kt:35:13 Unresolved reference 'DiscoverScrollTargets'.
DiscoverScrollTargetsTest.kt:47:13 Unresolved reference 'DiscoverScrollTargets'.
BUILD FAILED
```

**This is a compile-level RED and it was declared as one** rather than dressed up as behavioural
(`design.md` D3). It is the strongest form available for a new pure function, and the behavioural proof
that the previous target was wrong is `spec.md` §5.3 step 3, at the wave walkthrough.

**Assumption 4 resolved:** `scrollState.viewportSize` exists — Compose BOM `2026.08.00` resolves Foundation
`1.12.0`, which exposes `getViewportSize()`. `BoxWithConstraints` was not needed.

## 3. Slice reviews

`/feature-review` in slice mode, per slice, in arrival order.

**Slice 1 — PASS, 2026-08-31.** Checked: four named cases with four *distinct* expected values (0, 260,
700, 0), so the assertion is real arithmetic and not a restatement of the implementation; declared
boundaries respected — only `DiscoverScreen.kt` plus the two new files differ, and the two other scroll
effects (`:71-73`, `:74-87`) are untouched, as is `ui/AppViewModel.kt`, which was item 015's ground while
it was in flight; coverage 275 → 279 with no existing case edited; no copy authored and no string resource
added.

Two things read carefully rather than waved through:

- **`cardBottomOffset` is the bottom of `CardBody`, not of `ArticleCard`** — `CardBody` also contains the
  remaining-choices side note. Aligning `CardBody`'s bottom with the viewport bottom therefore reveals the
  side note as well as the card, and the **Mark read** button, which sits above both, stays on screen. The
  target is slightly more generous than the DoD wording; it is not wrong, and the walkthrough is what
  confirms it.
- **Slice 1 alone is behaviour-neutral.** With the card still last in the column, `cardBottomOffset -
  viewportHeight` and `maxValue` differ only by the column's bottom padding, so nothing regresses between
  the two slices. That is why this slice was ordered first.

## 4a. The scenario this item had to correct, and why

**Slice 2's walkthrough failed a scenario that could not have passed, and the implementer stopped rather
than claim it.** That is the outcome the brief asked for and it is recorded here in full because the defect
was the supervisor's.

`spec.md` §4's first scenario originally required the card's **full action rail** to be visible at 360 dp
on cold open. Codex built the reorder, took both gates green (284 tests, 0 failures), drove the emulator,
and reported: at 411 dp everything is visible; at 360 dp the article title alone reaches the bottom
navigation. It then refused to commit, on the grounds that closing the gap needed either
`ArticleCard.kt` or restyling — both fenced off by `spec.md` §3.

Verified independently against the captures and the corpus:

- `walkthrough/item012-411-settled.png` — the intent delivered: compact masthead, then the whole card with
  metadata, six-line title, excerpt and all three action controls, plus the remaining-choices note, above
  the bottom navigation with no scrolling.
- `walkthrough/item012-360-cold.png` — the card leads the viewport and no operational control sits above
  it, but this article's title fills the rest of the screen.
- `ArticleCard` sets `maxLines = 4` on the excerpt and **nothing** on the title, and `06-ui-ux.md` §25
  requires exactly that: *"no arbitrary hard two-line clamp"*, *"supports very long titles"*.
- `06-ui-ux.md` §71 requires actions be **reachable** at every width — not visible without scrolling.
- **Amendment 7 binds one sentence**, and it is about ordering, not about the card's bottom edge.

So the scenario asserted something (a) past its own amendment, (b) contrary to §71's actual requirement,
and (c) unreachable without violating §25. It was corrected rather than weakened, and `spec.md` §1.4 now
carries the measurement table and hands the 360 dp case to wave E's item 019. **Owner decision,
2026-08-31.**

The transferable lesson, and it is the same shape as wave C's: *a scenario must assert what the item
controls.* This one asserted a geometric outcome that depends on the length of a title in the dataset.

## 4. Walkthrough

Per `spec.md` §5.3. Batched at the end of the wave against merged `main`
(`execution-model.md` §4.5, §6).

## 5. Departures from the plan

Anything the slice plan predicted wrongly, including every existing assertion that had to move and why.
_None recorded yet._
