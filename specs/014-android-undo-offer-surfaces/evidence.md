# 014 — The undo offer follows the reversible action, not the gesture · evidence

**Branch:** `feat/014-android-undo-offer-surfaces` · **Worktree:** `news-agregator-014`\
**Cut from:** `main` at `05657ed6bd1c0ed3203c7c1fc5014fae6e0b1545`\
**Wave:** D, third item, dispatched 2026-08-31 after 015 and 012 merged

**Authorized by Amendment 8**, committed on `main` in PR #19. This item does not fix a defect — it reverses
a decision items 007 (`spec.md` §1.1) and 008 (`design.md` D8) made deliberately. `spec.md` §1.2 has the
five documents that said otherwise and the owner decision of 2026-08-31 that overturned them.

---

## 1. Forecast reconciliation — `/feature-implementation` Step 0.4

`spec.md` §6's six assumptions, re-checked against `05657ed` **before** slice 1 was briefed. This is the
step `waves/wave-d.md` says is not a formality for this item, because item 006 was designed against an
unmerged tree and failed at dispatch weeks after plan approval.

| # | Assumption | Result at `05657ed` |
|---|---|---|
| 1 | Amendment 8 committed on `main` | ✅ present in `docs/v1/README.md` |
| 2 | 015 added `expectDiscoverHead`, set at four Discover lambdas | ✅ 4 occurrences in `AppViewModel.kt`, 4 `= true` in `IntentionalReadingApp.kt` |
| 3 | 015 left `ArticleStateMachine.kt`, `ArticleCard.kt`, `ui/screens/**` untouched | ✅ and item 012, merged since, touched none of this item's files either |
| 4 | `reversibleActions` still `setOf(SAVE, DISMISS)` | ✅ `ArticleStateMachine.kt:254`, unchanged |
| 5 | Undo plumbing unchanged — `raiseUndoOffer` maps two actions, `PendingUndoMessage` has two cases, `UiStateMapper` derives from the action | ✅ all three |
| 6 | The assertions this item changes still exist | ✅ `AppViewModelTest.kt:978`, `:1073`; `ArticleStateMachineUndoTest.kt:85`, `:99` |

**No re-planning was required.** The forecast matched the tree exactly, including line numbers.

## 2. Gate runs

Recorded at the moment of each run, `test-results` deleted first (`execution-model.md` §5.1 control 4).

| When | Slice | `:app:testDebugUnitTest` | `:app:assembleDebug` |
|---|---|---|---|
| _pending_ | | | |

## 3. Failing-first evidence

| Slice | RED reproduced | Test commit | Implementation commit |
|---|---|---|---|
| _pending_ | | | |

## 4. Existing assertions changed

`design.md` D5 is the plan. This table is what actually happened, and every row needs a reason.

| Test | Planned | Actual |
|---|---|---|
| _pending_ | | |

## 5. Slice reviews

`/feature-review` in slice mode, per slice, in arrival order.

## 6. Walkthrough

Per `spec.md` §5.3. Batched with item 016's at the end of the wave, against merged `main`
(`execution-model.md` §4.5, §6).

## 7. Departures from the plan

_None recorded yet._
