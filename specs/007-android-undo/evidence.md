# 007 — Undo — evidence

**Branch:** `feat/007-android-undo` → `main`\
**Wave:** A (`specs/waves/wave-a.md`), last in the merge order, rebased onto merged `main` after 011 and 010\
**Implementer:** Codex, five sessions — two slices plus three findings follow-ups\
**Reviewer:** Claude, this session — authored the spec, design note, slice plan and this file, and
wrote no product or test code

---

## Commit chain

| Commit | Kind | Contents |
|---|---|---|
| `7a91bfa` | `docs(spec)` | `spec.md`, `design.md`, `slices.md` |
| `94980f5` | RED s1 | `ArticleStateMachineUndoTest.kt` |
| `8b2c63e` | GREEN s1 | `UndoRecord.kt`, `ArticleStateMachine.kt` |
| `7e0bbe0` / `7c411f6` | RED / GREEN s1 fix 1 | honest reverse results |
| `1c53651` / `6e51db6` | RED / GREEN s1 fix 2 | distinct `Reverted` success shape |
| `2812ab8` | `docs(spec)` | slice 1 marked done |
| `01023a6` | RED s2 | `AppViewModelTest.kt`, `UiStateMapperTest.kt` |
| `fcc2232` | GREEN s2 | the in-memory slot, `AppUiState`, `UiStateMapper`, strings |
| `0f993cf` | s2 fix | tests call the public API directly |

SHAs are post-rebase. Two slices, three findings rounds — the most review-intensive item in the wave,
which is what `execution-model.md` §5 predicted when it called 007 wave A's one hard review.

## Gates

Reproduced by the reviewer with `--rerun-tasks` in a throwaway detached worktree at every round, never
read from the implementer's report.

| Round | RED | GREEN |
|---|---|---|
| Slice 1 | compile failure on unresolved `UndoRecord`, `reverse`, `undoable`, `undoRecord`, `ArticleTransitionErrorCode` | 145 tests, 0 failures |
| s1 fix 1 | 2 failures — the absent-record claim and the fabricated stale status | 147 tests, 0 failures |
| s1 fix 2 | 1 failure — the shape contract | 148 tests, 0 failures |
| Slice 2 | **8 assertion failures, no compile errors** — all six scenarios plus two | 156 tests, 0 failures |
| s2 fix | none; test-only refactor, declared as such | 156 tests, 0 failures |

**Final, on the rebased head `0f993cf`:** `BUILD SUCCESSFUL` for `:app:testDebugUnitTest` and
`:app:assembleDebug`; **163 tests, no failing test files.** That is 142 on `main` after 010 merged plus
21 from this item. Pre-rebase results were discarded rather than carried forward.

## Scope: this item wires no trigger, and that is deliberate

`spec.md` §1.1 and `design.md` D1. In the browser, Undo is reachable from exactly two surfaces —
`attachSwipe`'s `onCommit` (`js/ui/discover.js:263`) and `installDiscoverShortcuts`' arrow keys
(`:268-269`) — and **both live in `js/ui/swipe.js`, the module item 008 ports.** The three labeled
triage buttons at `:246-248` call the same helper without the flag, whose default is `false` (`:211`).
`contracts.md` §31 says the same in one line: *"Only the most recent eligible swipe action must be
retained."*

The asymmetry is design intent. Undo exists to recover a *mis-trigger*; a swipe or an arrow key can fire
by accident, a press on a button labeled "Save for later" cannot. `DESIGN.md` §8 requires a labeled
equivalent for every gesture, never the reverse.

So this item ships the engine with **no producer**. The `undoable` flag reaches `AppViewModel` and every
caller passes `false`; verified by grep at every round, empty every time. Extending Undo to Android's
labeled buttons was put to the owner on 2026-08-25 and declined, because it would invent a requirement
and leave the two clients disagreeing about which surfaces are reversible.

**Why it was still built now.** `contracts.md` §23 ties Undo Not Interested and Undo Save for Later to
the reversal of the `dismissed` and `saved` learning signals, which item 005 introduces. Today
`ArticleStateMachine` never sets either flag true, so every undo record carries no reversal payload and
Undo is pure state-machine inversion. After 005 the same item has to unwind weight arithmetic against
records already on disk that claim signals with no deltas behind them.

## What review caught that the gates did not

Three findings across three rounds, none of which any gate would have failed on.

**1. The reverse path set a trap for its own next slice** (`ArticleStateMachine.reverse`). It returned
`Applied(records = nextRecords, record = previousRecord ?: current)`. When `previousRecord` was null the
key had just been removed from `nextRecords`, so `Applied.record` handed back a record provably absent
from its own records map — an invariant nothing else in the type violates. Not hypothetical:
`AppViewModel.kt` already did `localState.articles.getValue(article.id)`, which throws on an absent key,
and slice 2 was about to wire that path.

**2. The first fix undid the type safety it was for.** It made `Applied.record` nullable, which pushed
nullability onto **35 forward-transition call sites** that all legitimately have a record, then added two
test-only files — 43 lines defining extension properties on `ArticleRecord?` named `status`,
`firstSeenAt`, `openedAt`, `savedAt`, `dismissedAt`, `readAt`, each throwing via `requireForwardRecord()`.
That restored the illusion of non-nullability across the whole test source set, so a future test author
would get a runtime exception where the point had been to get a compile-time prompt.

The 35-site churn was the evidence that the shape was wrong: exactly one situation lacks a resulting
record, and making the common case nullable to accommodate the rare one is backwards. Redirected to a
distinct `ArticleTransition.Reverted(records, record: ArticleRecord?)`. Net **−15** test lines, both
accessor files deleted, `ArticleStateMachineTest.kt` byte-identical to its pre-fix form, and
`AppViewModel` back to `getValue` — safe again, because `reverse` returns `Reverted` and never reaches
that path.

*The reviewer offered both shapes in the follow-up brief without predicting the churn. The redirect cost
a round trip that a firmer brief would have avoided.*

**3. Slice 2's tests reached production API through reflection.** Five helpers across ~35 call sites:
`invokeSuspendForTest` calling `onArticleAction` and `performUndo` via `javaClass.getMethod(...)` with a
hand-built `Continuation`, plus `getMethod("getUndoAvailable")` and
`getMethod("getPendingUndoMessage")...toString()`. Every target was a public member of a class in the
same module, and the same file already called `viewModel.onArticleAction(...)` directly.

The motive was visible in the implementer's own report — *"no compilation failures"* at RED. Reflection
let the red tests compile against API that did not exist yet. That inverts the red-first rule: instead of
failing honestly, the harness was decoupled from the code. A rename would then fail at runtime with a
reflection error rather than at compile time, and `pendingUndoMessageForTest` returned `String?` via
`.toString()`, discarding the `PendingUndoMessage` type. Slice 1's RED had been an honest compile
failure, which review accepted — so this was a regression in method inside one item.

Fixed by direct calls at every site, with the pending-message assertions **strengthened** to compare
typed `PendingUndoMessage.SAVED` / `.DISMISSED`. Net **−77** test lines, production tree byte-identical.
The same commit also removed a reflection call that had crept into slice 1's shape-contract test, which
review had missed.

## Decisions taken during design

- **The undo record stores the entire prior record, not a status** (`design.md` D2). That is what makes
  restoration exact: `firstSeenAt`, `openedAt` and every `signalsApplied` flag go back to what they were,
  with no timestamp rewritten to the moment of the undo. `previousRecord == null` is meaningful and
  distinct from absent — it means the article had no record, so undo *deletes* the key. A test proves
  `LocalStateValidator` accepts the result.
- **The slot is in memory only** (`design.md` D3, `contracts.md` §31). Not in `LocalState`, not in
  `SavedStateHandle`, not on disk. It survives a configuration change because the `ViewModel` does, and
  dies with the process — the closest honest analogue of the browser's *"reloading the page clears Undo
  availability"*, pinned by its own scenario so the behaviour is chosen rather than discovered.
- **The held-article pin is NOT re-established on undo** (`design.md` D6). The wave brief assumed it had
  to be; the browser disagrees. `heldOpenedArticleId` is assigned only on a successful open
  (`js/app.js:287`) and the undo branch (`:312-319`) never touches it. The reader is not bounced because
  **dataset order returns the article on its own**: an article that was on screen preceded every other
  eligible article, so un-dismissing it restores it to head via `eligible.firstOrNull()`. The scenario
  asserts both halves — the target is back on screen *and* `heldArticleId` is still null — so a future
  implementer cannot "fix" this by re-pinning and quietly diverge.
- **The existing 6-second announcement is the wrong toast vehicle, and no replacement ships here**
  (`design.md` D4). It has no action slot, its six seconds is not the browser's 4500 ms, and it
  auto-acknowledges. The browser's undo toast is a genuinely different component. But instrumented tests
  are parked from CI, so a Composable built here would be both unreachable *and* unverified by the only
  gate that runs. This item ships the part the JVM suite can hold — availability, the pending message,
  and the five strings — and 008 renders it.
- **The reversal field is carried and left null** (`design.md` D5). `contracts.md` §31 names it; item 005
  fills it. The implementer used `typealias PreferenceReversal = Nothing`, which review judged sound
  rather than too clever: `Nothing?` is inhabited only by `null`, so "always null" becomes a compile-time
  guarantee instead of a convention. A test proves `preferences` is unchanged across an undo.

## Known behaviour this item introduces

Nothing a reader can do on the device changes. The undo engine exists and is fully tested, and no
surface reaches it until item 008 lands swipe.

`ArticleTransition` gains a `Reverted` variant, so any future `when` over it must handle four cases
rather than three. `AppViewModel` exposes `undoAvailable` and `pendingUndoMessage` on `AppUiState`, both
inert until 008 renders them.

## Walkthrough — the negative check, performed 2026-08-25 against merged `main`

`spec.md` §5 states there is no owner walkthrough for this item and why. What wave A's batched
walkthrough could confirm is a negative, and it did, on the `Pixel_10` API 37 emulator against the APK
built from merged `main` (`92223cd`):

- Save for later still commits — Read Later went 0 → 1, the deck advanced from 205 to 204 available, and
  a new head article was presented.
- **No toast, banner, button, or any other affordance appeared anywhere** as a side effect of this item.
- Read Later, History, Settings and Reset behave exactly as they did at 004.

That is the whole of what this item is allowed to change on a device today, and it changed nothing.

## Outstanding

- **The trigger, the actionable toast, and the walkthrough are item 008's.** `spec.md` §5 states there is
  no owner walkthrough for this item and why: nothing reachable from the interface touches the slot. What
  wave A's batched walkthrough confirms is a negative — Save and Not interested still commit, still
  advance the deck, and no new affordance appeared anywhere.
- **Item 009 must clear the slot on import.** The browser clears on both import and reset
  (`js/app.js:352`, `:361`); this item implements reset, and import does not exist yet. Recorded in
  `design.md` D3 so 009's designer does not have to rediscover it.
- **`AppViewModel` now has two `error(...)` arms** for transitions that cannot occur — a forward action
  returning `Reverted`, and an undo returning `Applied` or `Unchanged`. Correct today and unreachable by
  construction; if a third writer of transitions appears, prefer making them unrepresentable.

## Reviewer independence

All product and test code in this item was written by the implementer agent (Codex) across five fresh
sessions. The reviewer authored the specification, design note, slice plan and this evidence file, and
wrote no product or test code. The one exception is recorded here in full: the reviewer resolved a
single rebase conflict in `AppViewModel.kt` — two adjacent private field declarations,
`lastAppliedAppearance` from item 010 and `undoRecord` from this item, where both sides were additive and
both were kept. No logic was authored and the resolution is covered by the 163-test gate re-run on the
rebased head.

Every gate result quoted here was reproduced by the reviewer with `--rerun-tasks` in a throwaway
worktree rather than accepted from the implementer's report.
