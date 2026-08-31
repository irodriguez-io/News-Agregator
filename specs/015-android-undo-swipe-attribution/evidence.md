# 015 — A swipe must be attributed to the article the reader saw · evidence

**Branch:** `feat/015-android-undo-swipe-attribution` · **Worktree:** `news-agregator-015`\
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
| _pending_ | | | |

## 2. Failing-first evidence

One row per slice: the RED, reproduced independently in a throwaway worktree with the fix absent, and the
commit pair.

| Slice | RED reproduced | Test commit | Implementation commit |
|---|---|---|---|
| _pending_ | | | |

## 3. Slice reviews

`/feature-review` in slice mode, per slice, in arrival order.

## 4. Walkthrough

Per `spec.md` §5.3. Batched at the end of the wave against merged `main`
(`execution-model.md` §4.5, §6).

## 5. Departures from the plan

Anything the slice plan predicted wrongly, including every existing assertion that had to move and why.
_None recorded yet._
