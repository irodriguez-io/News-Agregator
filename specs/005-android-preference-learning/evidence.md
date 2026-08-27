# 005 — Preference learning and personalized ranking — evidence

**Branch:** `feat/005-android-preference-learning` → `main`\
**Wave:** C (`specs/waves/wave-c.md`), running alone — it touches `AppViewModel.kt`,
`ArticleStateMachine.kt` and `DiscoverDeck.kt`, the three hub files of the client\
**Implementer:** Codex, nine sessions — four slices, three findings follow-ups, and two sessions that
stopped before writing code\
**Reviewer:** Claude, this session — authored the spec, design note, slice plan and this file, and wrote
no product or test code

---

## Commit chain

| Commit | Kind | Contents |
|---|---|---|
| `fdef7c2` | `docs(spec)` | `spec.md`, `design.md`, `slices.md` |
| `86d1da2` / `c890a1b` | RED / GREEN s1 | the delta table, the `1e10` clamp, `apply`, `reverse` |
| `391f365` | s1 fix | multi-topic coverage for §4.1's lead scenario |
| `6667b2f` | `docs(spec)` | slice 1 marked done |
| `2a7da33` | `docs(spec)` | **D7 corrected** — names a test, not a line range |
| `c9b40ed` | `docs(spec)` | **D4 amended** — the two consequences of deleting the recompute |
| `829c9f4` | RED s2 | latching, the `MARK_UNREAD` reversal, the undo seam |
| `70967be` | `docs(spec)` | **D7 corrected again** — the fifth assertion |
| `479c400` | GREEN s2 | as above |
| `e1870d1` | s2 fix | explicit transition preferences; three shims deleted |
| `49d0ea4` | `docs(spec)` | slice 2 marked done |
| `6197039` | RED s3 | the reconciliation fold and its two call sites |
| `b633755` | `docs(spec)` | **D8 amended** — pre-005 fixtures in the existing suite |
| `f8522b3` | GREEN s3 | as above |
| `41bd0ae` / `c3a71e5` | RED / GREEN s3 fix | committed imports reported truthfully |
| `8403dbb` | `docs(spec)` | slice 3 marked done |
| `6a3f577` / `3bf5db9` | RED / GREEN s4 | personalized score, ordered deck, mapper threading |
| `668b0ac` | `docs(spec)` | slice 4 marked done |

Cut from `main` at `48abb67` and **never rebased** — nothing landed on `main` during the item, so the
`waves/wave-b-note.md` §3 rebase obligation did not fire. Verified with
`git log --oneline feat/005-android-preference-learning..main`, empty at start and at close.

## Gates

Baseline on `main` at `48abb67`: **198 tests, 0 failures, `BUILD SUCCESSFUL`**, re-verified by the
reviewer in the item worktree before the first dispatch.

| Round | RED | GREEN |
|---|---|---|
| Slice 1 | compile failure on unresolved `PreferenceLearning` | 211 tests, 0 failures |
| s1 fix | **none observable** — the added test passes against a correct implementation; disclosed, not fabricated | 212 tests, 0 failures |
| Slice 2 | 222 tests, **2 failures** — the two authorized assertions | 222 tests, 0 failures |
| s2 fix | compile-time only — test compilation fails once the shims are gone | 222 tests, 0 failures |
| Slice 3 | compile failure on unresolved `PreferenceReconciliation` | 232 tests, 0 failures |
| s3 fix | 1 test completed, 1 failed | 233 tests, 0 failures |
| Slice 4 | compile failure on unresolved scoring, deck and mapper APIs | 254 tests, 0 failures |

`:app:assembleDebug` green at every round. **198 → 254 tests, +56.**

**Final verification** per `spec.md` §5.1, on `668b0ac` in a throwaway detached worktree with
`--rerun-tasks` and `test-results` deleted first: **254 tests, 0 failures, 0 errors, 0 skipped**, both
`BUILD SUCCESSFUL`, 24 of 24 tasks executed.

**Reviewer honesty note.** Every GREEN gate above was reproduced by the reviewer rather than read from
an implementer report. The RED rows are the exception and are recorded as reported: they were not
independently reproduced, because reproducing a RED requires checking out the test-only commit and
rebuilding, and the RED commits are in the chain for anyone who wants to. Where a RED could not honestly
be produced at all — the slice 1 follow-up — the implementer said so rather than breaking the
implementation to manufacture one, and that is recorded above as it was reported.

## The two wave obligations

Both discharged by `spec.md` §4.3, and both are real tests rather than assertions about state:

- **Runs exactly once** — `cold load writes one changed reconciliation and writes no unchanged
  reconciliation` asserts the *write count*, which is the observable form of the obligation. A load whose
  fold changes something performs exactly one write; a load whose fold changes nothing performs none.
- **Mark Unread does not drift** — `pre-learning unread after post-learning open and save preserves
  exactly the remaining signals` loads a pre-005 state, applies a post-005 signal to the same source,
  marks the pre-005 record unread, and asserts the source's weight and count equal exactly the signals
  still applied.

## What review caught

Four findings across four slices, plus five escalations from the implementer that were all correct.

1. **Slice 1 — §4.1's lead scenario was not encoded as written.** It specifies an article carrying *two
   distinct topics*; no test in the file built one. Every article had one tag, none, or the same ID
   twice, so the topic fold was only ever exercised at n=1 — an implementation training
   `uniqueTopicIds.first()` would have passed all thirteen tests. Coverage, not a live bug, but slices
   2–4 all reuse this file as their arithmetic.
2. **Slice 2 — three compatibility shims silently substituted an empty preferences map.** A secondary
   constructor on `Unchanged` plus `transition` and `reverse` overloads, added so ~15 existing test call
   sites would keep compiling. `AppViewModel` persists `preferences = transition.preferences`, so any
   future call site taking a shim would have written an empty map to disk and **wiped every learned
   preference**, with no compile error and no failing test. This is precisely the hazard D4 removes from
   the record API *in the same slice*; D3 rejected a nullable field for the same reason, and a parameter
   defaulting to empty is worse than nullable because it supplies confidently wrong data rather than
   absent data. Removing them made 34 call sites explicit — and surfaced a `UiStateMapperTest` case that
   had been discarding the preferences its own `OPEN` produced.
3. **Slice 3 — a committed import could be reported as failed.** `LocalStateStore.importState` writes
   the imported document and clears the recovery lock *before* returning `Success`. If the reconciliation
   write then failed, the code announced `IMPORT_FAILED` and never adopted: the reader saw their old data,
   was told the import failed, and would find it replaced at the next launch. The cold-load path already
   handled the identical case correctly; the import path now mirrors it. Not data loss — the fold is
   idempotent and self-heals — but an untruthful report and a UI disagreeing with disk.
4. **Slice 4 — nothing.** Passed first time. The two `AppViewModelTest` fixture lines it changed were
   examined and are justified: without them the just-opened article legitimately out-ranks the expected
   leader, which would have left the pin-release assertions unable to distinguish a released pin from a
   held one. Changing them restores falsifiability rather than papering over a defect.

## What the plan got wrong

Five times the implementer stopped before writing code, and five times it was right. All five were
defects in the plan, not in the work, and all five are recorded as amendments rather than quietly fixed.

1. **D7 counted lines when the unit is the test.** It authorized
   `ArticleStateMachineUndoTest.kt:324-325` but missed `:317` in the same test body — the
   `assertNull(preferenceReversal)` that is the very deferral 005 closes.
2. **D7 also missed that `:322` copied only `articles`**, so `after.preferences` was `before.preferences`
   by identity. Both assertions D7 *did* name would have passed **vacuously** post-005: the test would
   have looked updated while proving nothing.
3. **D4 did not follow through on deleting the recompute.**
   `ArticleStateMachineTest.kt:325` asserts `SAVE` clears an inconsistent `dismissed` flag — the
   "enforcing structural signals" half of its own test's name, which is exactly what D4 deletes. Safe to
   flip, because the input is unreachable through the state machine and rejected by the validator; the
   recompute only ever laundered garbage. D4 also said one flag flips where two do, since the same
   `SAVE` latches its own signal.
4. **A defaulted field is invisible to an assertion grep.** `ArticleStateMachineUndoTest.kt:48` compares
   a whole `UndoRecord` against an expected value whose `preferenceReversal` takes the field's `= null`
   default. Two separate sweeps missed it because it names no preference at all; only the compiler and a
   full run found it. **Grepping for assertions cannot find the surface a defaulted field covers.**
5. **The pre-005 test suite encodes assumptions this item invalidates.** Three `AppViewModelTest`
   fixtures are pre-005 documents and now trigger the fold on load. The clearest is the export test,
   whose fixture is simply `localState(record(article(71), READ))` — a read record and no preferences,
   which is not contrived but *the* state reconciliation exists to repair. Resolved by correcting the
   fixtures' counts rather than the assertions: changing an assertion would let a fold defect hide behind
   an adjusted expectation, changing only the counts cannot, because the original assertions still run.

The through-line is that **the plan was written against the specification and the browser, and the
existing test suite was the thing neither of those describes.** Three of the five were invisible until
something was compiled or run.

## Deliberate asymmetries, all tested

Kept because they are correct, and each is the kind of rule an implementer optimising for symmetry
"fixes":

- **`MARK_UNREAD` applies no Save signal** even though it moves the article to `saved` (`contracts.md` §23).
- **`REMOVE` applies no negative signal** even though it moves the article to `dismissed` (§24 — removing
  an item from a backlog does not mean the topic is unwanted).
- **Over-count lowers the count and leaves the weight** (D8). The document says how many signals are
  outstanding, not which event produced the stranded weight, so the weight can only be guessed.
- **±5.0 clamps a stored weight; ±6 clamps an article's topic *sum* at scoring time.** Two rules, two
  slices, never merged.

## Outstanding

- **`spec.md` §5.2's owner walkthrough has not been run.** It requires merged `main`, an emulator install
  carrying **real accumulated history**, and an owner judgement at steps 4 and 5 on whether the order is
  *better* rather than merely different. Ranking quality is a product question no test answers.
- **006** inherits `DiscoverDeck` holding the ordered list its penalties sequence, and `design.md` D12's
  correction: the wave brief's claim that the Android head article differs from the browser's until 006
  lands is wrong, and 005 closes that gap by itself.
