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
| 2026-08-31, base `6c857a6` | — (baseline) | 275 tests, 0 failures, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |
| 2026-08-31, head `318ee86` | 1 | **280 tests, 0 failures**, `BUILD SUCCESSFUL` | `BUILD SUCCESSFUL` |

Both head rows are the **supervisor's own runs**, not the implementer's report, with
`test-results` deleted first (`execution-model.md` §5.1 control 4).

## 2. Failing-first evidence

One row per slice: the RED, reproduced independently in a throwaway worktree with the fix absent, and the
commit pair.

| Slice | RED reproduced | Test commit | Implementation commit |
|---|---|---|---|
| 1 | **yes, independently** | `89de9aa` | `318ee86` |

The RED was reproduced in a throwaway detached worktree at `89de9aa`, with the fix confirmed absent
(`grep -c expectDiscoverHead` on `AppViewModel.kt` = 0):

```
> Task :app:testDebugUnitTest FAILED
AppViewModelTest > a refused action leaves the reader nothing to undo FAILED
AppViewModelTest > a swipe is refused when the head changed under it for any other reason FAILED
AppViewModelTest > a swipe in the window after Undo is not attributed to the displaced article FAILED
280 tests completed, 3 failed
```

**It failed for the right reason, which is the whole of `spec.md` §1.4.** The displaced article
`00000000000000000002` was persisted after Undo, and the previous head `00000000000000000001` was
persisted after the category changed — the failures name the article that moved, not merely that
something moved.

Two of the five new cases — *an ordinary swipe still commits* and *the guard is confined to the Discover
card* — passed at `89de9aa` as well as at `318ee86`. That is correct: they are regression guards for
behaviour the guard must not break, not assertions about the fix.

## 3. Slice reviews

`/feature-review` in slice mode, per slice, in arrival order.

## 4. Walkthrough

Per `spec.md` §5.3. Batched at the end of the wave against merged `main`
(`execution-model.md` §4.5, §6).

## 5. Departures from the plan

Anything the slice plan predicted wrongly, including every existing assertion that had to move and why.
_None recorded yet._
