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
| 2026-08-31, head `04a0554` | 2 | **284 tests, 0 failures**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |

Slice 2 adds no test — see §3 — so the count is unchanged from the post-merge row, which is the expected
result and not a missing run.

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
| 2 | n/a — no unit test, by design | — | `04a0554` |

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

**Slice 2 — PASS, 2026-08-31.** Checked: the split is faithful — `DiscoverMasthead` and
`DiscoverOperationalBar` reuse the same string resources, typography styles, tokens and
`Arrangement.spacedBy(12.dp)` as the removed overload, and `CategoryChipRow` is untouched, so no restyling
crept in ahead of wave E. **The general `EditorialHeader` overload has zero added lines** — the commit only
deletes the Discover-specific overload and three now-unused imports — which is the structural proof that
Read Later and History cannot have changed, alongside their captures. `DiscoverScreen`'s column reads
masthead → state body → operational bar, and all three `LaunchedEffect`s keep their keys and bodies
including slice 1's `revealCardActions` call. Scope: exactly the three authorized files. No test edited;
284 tests, 0 failures, both gates green on the supervisor's own run.

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

**Driven at slice 2 rather than batched at wave end, deliberately:** this item's only evidence *is* the
walkthrough (`spec.md` §5.2 — composition order has no observer in `testDebugUnitTest`), so deferring it
would have meant reviewing and merging a slice with no evidence at all. The wave-end batch still applies to
the undo items. Captures are in `walkthrough/`.

| Step | Result |
|---|---|
| 1 — cold open, 411 dp | compact eyebrow and title, then the whole card: metadata, six-line title, excerpt, all three action controls, and the remaining-choices note, above the bottom navigation without scrolling. `item012-411-settled.png` |
| 1 — cold open, 360 dp | eyebrow and title, then the card; **no operational control above it**. This article's title fills the rest of the viewport — recorded, not asserted, per `spec.md` §1.4. `item012-360-cold.png` |
| 2 — scroll down | purpose copy → Refresh → *Content age · 3h* → degraded-source notice → *154 available in All* → category chips, every string unchanged. `item012-step2.png` |
| 4 — category change | selecting Science returned to the top with its card leading; Technology likewise, after re-locating the chip row. `item012-step4-science.png` |
| 5 — swipe advances the deck | the incoming card leads the viewport with its **complete action rail**, ready for the next decision without scrolling. This is item 008's D12 working with the new order, and it is the clearest single demonstration of the item's intent. `item012-step5-swipe.png` |
| 6 — failed refresh | with airplane mode on, *"Refresh failed. Showing the last available content."* appears below the content-age line, degraded notice directly beneath. `item012-step6-failed-refresh.png` |
| Read Later | full eyebrow, title, description and *Discover something new* action, unchanged. `item012-read-later.png` |
| History | full eyebrow, title, description and *Return to Read Later* action, unchanged. `item012-history.png` |

Airplane mode disabled and `wm size` / `wm density` reset afterwards; confirmed by the supervisor
(`1080x2424`, density `420`, `airplane_mode_on=0`).

**The step 3 regression — the wave B defect this item risked re-opening — is covered by slice 1's
`revealCardActions` and is re-checked at the wave-end walkthrough against merged `main`.**

## 5. Departures from the plan

- **`spec.md` §4 scenario 1 was corrected mid-item.** §4a has it in full. The supervisor's scenario, not
  the implementer's work, was wrong.
- **The walkthrough was driven at slice 2 instead of at wave end.** §4 says why.
- **Slice 2 carries no failing-first commit.** Planned that way (`slices.md`), because composition order has
  no observer at the JVM layer; slice 1 carried the item's RED. Stated rather than satisfied with a vacuous
  assertion.
- **No existing assertion moved.** The suite went 275 → 279 at slice 1 and stayed at 284 after `main` was
  merged in; slice 2 added none.
