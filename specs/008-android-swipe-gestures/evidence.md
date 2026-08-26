# 008 — Swipe gestures — evidence

**Branch:** `feat/008-android-swipe-gestures` → `main`\
**Wave:** B (`specs/waves/wave-b.md`), first in the merge order\
**Implementer:** Codex, five sessions — three slices plus two findings follow-ups and one folded-in defect\
**Reviewer:** Claude, this session — authored the spec, design note, slice plan and this file, and
wrote no product or test code

---

## Commit chain

| Commit | Kind | Contents |
|---|---|---|
| `b4d7604` | `docs(spec)` | `spec.md`, `design.md`, `slices.md` |
| `de23a0d` | `docs(spec)` | threshold boundary corrected — see §Design errors |
| `af6b09e` | `docs(spec)` | intent-lock boundary example corrected |
| `bb1104f` / `0590bd5` | RED / GREEN s1 | `SwipeGesture.kt` and its tests |
| `f1b84f4` | `docs(spec)` | slice 1 marked done |
| `46c673e` / `217202d` | RED / GREEN s2 | the undo offer, its id, the two announcements |
| `fa8a15e` | s2 fix | restored item 007's absorbed scenario test |
| `edc69f8` | `docs(spec)` | slice 2 marked done |
| `a222085` | s3 | the Compose surface — gesture, cues, exit, toast |
| `dbfbc38` / `4dc8e1a` | RED / GREEN s3 fix 1 | `launchUndo` on the ViewModel scope |
| `a48a07a` | s3 fix 2 | the two bottom surfaces stacked, not overlaid |
| `ca335f7` | `docs(spec)` | the owner's Mark read defect folded in, with D11 |
| `02caf9c` | s3 addition | opened-article actions scrolled into view |
| `9748177` | `docs(spec)` | slice 3 marked done |

## Gates

Reproduced by the reviewer with `--rerun-tasks` in a throwaway detached worktree at every round, never
read from the implementer's report.

| Round | RED | GREEN |
|---|---|---|
| Slice 1 | compile failure on unresolved `SwipeGesture`, `down`, `move` | 173 tests, 0 failures |
| Slice 2 | compile failure on unresolved `pendingUndoOffer`, `acknowledgeUndoOffer`, `UNDO_COMPLETED`, `UNDO_FAILED` | 180 tests, 0 failures |
| s2 fix | none; test-only split, declared as such | 181 tests, 0 failures |
| Slice 3 | none; Compose-only, declared in the slice plan in advance | 181 tests, 0 failures |
| s3 fix 1 | compile failure on unresolved `launchUndo` | 182 tests, 0 failures |
| s3 fix 2 | none; Compose-only | 182 tests, 0 failures |
| Mark read | none; Compose-only | 182 tests, 0 failures |

`:app:assembleDebug` green at every round. **163 → 182 tests.**

## What review caught

Three findings across three rounds. None would have failed a gate.

1. **Item 007's scenario test was absorbed.** `the slot holds one action, and the newest wins` was
   renamed into this item's offer-identity test. Every assertion survived, so coverage was intact — but
   that scenario is named at `specs/007-android-undo/spec.md:137` and bound at its `slices.md:62`, both
   merged, so the absorption removed the only link from a shipped scenario to a test.
2. **The undo toast and the live status message occupied the same rectangle.** Both were placed at
   `align(BottomCenter)` with identical padding. Not hypothetical: slice 2 pins a state that guarantees
   the overlap, because a refused Undo announces `UNDO_FAILED` *and* deliberately keeps the offer.
3. **`performUndo` ran in a composition scope.** `rememberCoroutineScope()` rather than the ViewModel's,
   so an undo was cancellable by composition teardown between its store write and its state adoption —
   and inconsistent with every other action, all of which go through a `launch*` method.

## Design errors caught before any code was written

Both are the reviewer's, in the slice plan, and the implementer stopped and asked rather than working
around them. This is the cheapest place for them to surface and is recorded as such.

1. **The threshold boundary.** The definition of done claimed the browser excludes travel of exactly the
   threshold. It does not: `06-ui-ux.md` §40 is `abs(horizontalTravel) >= 90px` and `js/ui/swipe.js:98`
   restores only on `Math.abs(deltaX) < threshold`, so exactly 90 commits. Corrected in `de23a0d`.
2. **The intent-lock example.** The plan gave `x = 10, y = 9` as a horizontal lock. Horizontal requires
   `abs(x) > abs(y) * 1.15`, and 9 × 1.15 = 10.35 > 10, so that gesture locks vertical. With `x = 10` the
   crossover is at `y ≈ 8.696`. Corrected in `af6b09e`.

## Owner walkthrough

Driven over `adb` on the `Pixel_10` API 37 emulator against the branch build at `02caf9c`, not handed
over as a checklist. **This is also Undo's first walkthrough** — item 007 shipped its engine with no
reachable surface, by design.

| Step | Result |
|---|---|
| Drag below the threshold | No commit; one record, unchanged |
| Left swipe past the threshold | Dismissed; toast `Not interested   Undo` |
| Undo from the toast | Article back at the head of Discover, its record deleted entirely, count restored |
| Right swipe past the threshold | Saved; toast `Saved to Read Later   Undo`; Read Later 1 |
| **Vertical drag from inside the card** | Column scrolled, card did not translate, nothing committed |
| Horizontal drag starting on the Read article button | No drag, no click, no commit; app stayed foreground |
| Labeled Not interested button | Committed, deck advanced, **no toast** |
| Reduced motion (`animator_duration_scale 0`) | Swipe still commits, deck advances, toast still offered |
| Return from the publisher | Triage row **and** Mark read both on screen with no scrolling |

The undo case confirms `design.md` D6 on the device: the article returns to the head of Discover on
dataset order alone, with no held-article pin re-established.

**Not performed:** the mid-drag cue frame, and a TalkBack gesture pass. Both are recorded as outstanding
in `specs/backlog.md`.

**Owner judgement still outstanding:** whether the exit feels *tactile, quiet, controlled* rather than
bouncy or playful (`06-ui-ux.md` §44). `adb` can drive a synthetic swipe; it cannot judge one.

## Scope

`git diff` across the item shows `android/gradle/libs.versions.toml`, `AndroidManifest.xml` and
`android/app/src/main/res/values/strings.xml` untouched — the five undo strings item 007 authored were
finally referenced rather than added to. `git grep 'undoable = true'` returns exactly one call site in
main source: the swipe commit.

## Outstanding

- The mid-drag cue and TalkBack passes above.
- `DiscoverScreen` now holds two scroll effects — the existing reset and D11's scroll-into-view. They are
  deliberately separate for readability. If a third appears, they should be reconciled.
