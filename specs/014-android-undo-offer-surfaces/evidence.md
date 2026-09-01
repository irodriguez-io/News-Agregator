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
| 2026-08-31, base `05657ed` | — (baseline) | 284 tests, 0 failures, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `8fa59c5` | 1 | **284 tests, 0 failures**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `d97ec2d` | 2 | **286 tests, 0 failures**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |

The count is unchanged because slice 1 adds one case and deletes one. Head row is the supervisor's own run,
`test-results` deleted first.

## 3. Failing-first evidence

| Slice | RED reproduced | Test commit | Implementation commit |
|---|---|---|---|
| 1 | **yes, independently** | `fa96b67` | `8fa59c5` |
| 2 | **yes, against the item base** — see below | `86ec7f4` | `d97ec2d` (rename only) |

Reproduced in a throwaway detached worktree at `fa96b67`, with `undoable` confirmed still present in
`ArticleStateMachine.kt` (fix absent): **285 tests completed, 1 failed** —
`reversible actions carry undo state without caller eligibility`. Behavioural, and **no temporary
scaffolding was needed**: the new case simply calls `transition` without an eligibility argument, which
defaults to `false` on the old tree and so produces no undo record.

**Slice 2 is test-only and its cases pass on the post-slice-1 tree. That is correct, not a missing RED**,
and the brief anticipated it: slice 1 shipped the production change, so slice 2 adds the coverage that
change earns. The meaningful question is whether the cases are red against **the item's base**, and they
are — dropped onto a throwaway worktree at `05657ed`, `a button save followed by a swipe leaves the button
save standing after Undo` fails behaviourally, because before this item a button `SAVE` raised no offer at
all. They assert behaviour 014 introduces; they are not tautologies.

## 4. Existing assertions changed

`design.md` D5 is the plan. This table is what actually happened, and every row needs a reason.

| Test | Planned | Actual |
|---|---|---|
| `ArticleStateMachineUndoTest` `a commit that is not marked undo-eligible offers nothing` | delete | **deleted** — its subject, the `undoable` flag, no longer exists |
| `ArticleStateMachineUndoTest` `only save and dismiss are reversible` | keep, must pass | **kept and passing** — slice 1 does not widen the action set |
| `AppViewModelTest` `a labeled button press is still not undo-eligible` | replace (D5 assigned it to slice 2) | **rewritten in slice 1** as `a labeled button press raises the same offer as a swipe`, keeping the `OPEN` and `MARK_READ` no-offer cases intact. Moved forward because its assertion is falsified *by slice 1*; leaving it for slice 2 would have ended slice 1 with a red gate. |
| `AppViewModelTest` `a refused Undo announces its failure and keeps the offer` | **not anticipated** | **setup repaired**: its second action changed from `SAVE` to `OPEN`. Name and all six assertions byte-identical. See §7. |
| `AppViewModelTest` — 28 further call sites | drop the argument, change nothing else | **done, 28 sites.** The design's figure of 33 counted comments and variable names; the implementer's count is the correct one. |
| `AppViewModelTest` `launchArticleAction threads undo eligibility to the slot` | rewrite | **renamed in slice 2** to `a launched save raises the offer`; every assertion byte-identical. There was no eligibility left to thread. |

## 5. Slice reviews

`/feature-review` in slice mode, per slice, in arrival order.

**Slice 2 — PASS, 2026-08-31.** Checked: `AppViewModelTest.kt` is the only file that changed — no
production code, as the slice required. Two new cases, both asserting **by article id, source id and each
topic id**, and both overriding the `article()` helper's shared `oauth` tag with distinct topic ids so the
topic assertions cannot be vacuously true — item 015's lesson applied without a second reminder. The
competing-slot case proves the harder half of the scenario: the button-saved article's weights are
unchanged through the swipe **and** through the Undo, while the swiped article's move and then reverse.
One existing test renamed, assertions untouched.

Two coverage findings reported rather than papered over: the failed-write scenario was **already covered**
by `a swipe whose write fails is not visually finalized`, which now exercises the action-owned path; and
domain-level idempotency was already covered by `an idempotent no-op produces no undo record`, so the new
case adds the ViewModel-layer no-offer proof rather than duplicating it.

One fixture note: the already-saved case originally hand-built its saved state, which
`PreferenceReconciliation` normalized on load and made the test lie. The implementer arranged the state
through a real first `SAVE` on a first view model over the same store instead. No assertion was weakened.

**Slice 1 — PASS, 2026-08-31.** Checked: the implementation matches `design.md` D1 exactly — `transition`
builds the undo record on `action in reversibleActions` alone, `persistArticleTransition` raises the offer
from `transition.undoRecord` unconditionally, and the `undoable` parameter is gone from all four
signatures. **Item 015's `expectDiscoverHead` survives on both public signatures and at all four
`= true` call sites** (verified by count, not by report). `reversibleActions` is still exactly
`setOf(SAVE, DISMISS)` at `:253` — this slice widens nothing. Scope is the five authorized files. The five
remaining `undoable` strings in `AppViewModelTest.kt` are two comments and a variable name.

## 6. Walkthrough

Per `spec.md` §5.3. Batched with item 016's at the end of the wave, against merged `main`
(`execution-model.md` §4.5, §6).

## 7. Departures from the plan

**Three, all found by the implementer stopping rather than proceeding, and all supervisor errors.** None
reached shipped code. This is the same pattern as item 006 in wave C, now at three occurrences across two
waves.

**1. The slice split could not compile.** Slice 1 removed the `undoable` parameter from `AppViewModel` but
excluded `AppViewModelTest.kt`, which carried 28 calls passing it. Slice 1 could not have built its own
test sources, let alone reached a green gate. Fixed at `991ea60`: slice 1 now carries the mechanical
argument removal, slice 2 keeps every assertion change.

> **The root is the one `waves/wave-c-note.md` §7 already named and `execution-model.md` §2 still does not
> model: the plan was drawn by *who writes a file*, never by *who asserts against it*. A signature change
> is not confined to the layer that declares it.** Item 006 froze an assertion its predecessor had made
> unfreezable; this item split a slice across a compile boundary. Same root, different costume.

**2. `design.md` D5 claimed a complete assertion list and did not have one.** It enumerated eleven
`AppViewModelTest` line numbers against roughly twenty affected cases. Replaced by a rule — *every* call
loses the argument (mechanical, slice 1); *only* the named cases change what they assert (slice 2) —
because line numbers in a 2 100-line test file go stale between design and dispatch, and an incomplete list
reads as an exhaustive one.

**3. A test whose *premise*, not whose assertion, the change destroys.** `a refused Undo announces its
failure and keeps the offer` needed an action that persists — firing a rigged `saveBehavior` that drops the
target's record — **without** replacing the undo slot. It used `SAVE` with `undoable = false`. Removing the
flag makes every `SAVE` claim the slot, so Undo succeeded where the test expected `UNDO_STALE`. The
implementer diagnosed it and proposed the repair: use `OPEN`, which persists but is not reversible. Name
and all six assertions unchanged.

> **D5 reasoned about which tests *assert* the old rule. It never considered tests that *depend on the
> flag to construct a state*.** That class is invisible to a line-number enumeration and is worth carrying
> into `wave-d-note.md`: when a design pass removes a capability, ask not only which assertions claim it
> but which fixtures *use* it.

**A fourth error was the supervisor's alone and was self-corrected before it landed:** the first slice-1
dispatch told the implementer that ending the slice with two known failures was acceptable. A slice closes
green. Corrected in the same exchange.
