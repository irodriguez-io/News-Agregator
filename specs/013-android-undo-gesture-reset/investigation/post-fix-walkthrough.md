# 013 post-fix walkthrough — the third stop (2026-08-27)

Run by the orchestrator over `adb` on `Pixel_10` (`emulator-5554`) against branch head `29344aa`, which
carries the D1/D5 fix and its instrumented guard. `spec.md` §5.4.

**Result: step 1 passes, step 2 fails.** A swipe roughly **0.3–0.5 s after Undo is still discarded**, with
the card settled, visible and correctly positioned. That is this item's headline symptom, unfixed, on the
path the item is named after.

## What held

All three gates were re-run independently of the implementer and are green: `:app:testDebugUnitTest`
**258 tests, 0 failures**; `:app:assembleDebug` and `:app:connectedDebugAndroidTest` both
`BUILD SUCCESSFUL`.

§5.3's RED was reproduced independently in a throwaway worktree at `1f19cea` — the test commit with the
fix absent:

```
expected:<[Article(id=first, title=First article, …)]> but was:<[]>
ArticleCardGestureTest.kt:86
```

No commit recorded, which is the failure mode §5.3 requires. The RED is real and the test is honest.

## Step 1 — two fast consecutive swipes, no Undo: **passes**

| | records | source weights |
| --- | --- | --- |
| Step 0, pre-fix (`step0-reproduction.md`) | 1 | one source moved |
| Post-fix, 0.35 s apart | **2** | `w3c_webauthn` −0.35/1 **and** `ietf_oauth` −0.35/1 |

A settled control (3 s apart, each swipe aimed at bounds read from `uiautomator dump`) also gives 2.
`walkthrough-state-fastpair.json`, `walkthrough-state-control-settled.json`.

**The deck-advance path is genuinely fixed**, and the fix is worth keeping on that evidence alone.

## Step 2 — swipe → Undo → immediate swipe: **fails in a 0.3–0.5 s window**

Records after the reswipe. Undo removes the record, so **1 = the reswipe landed, 0 = it was discarded.**

| delay after Undo | y = 1760 | y = 1285 (card text area) |
| --- | --- | --- |
| 0.2 s | 1 | 1 |
| 0.3 s | 0, 0 *(2 runs)* | — |
| 0.4 s | 0, 0, 0 *(3 runs)* | **0** |
| 0.5 s | 0, 0 *(2 runs)* | — |
| 0.8 s | 1 | 1 |

Also 0 at 0.4 s with a **300 ms** swipe instead of 100 ms, so it is not a fling-sampling artifact. Two
different y values, inside the card's true bounds in both cases, so it is not a coordinate artifact.

### The touch never reaches the handler

A 900 ms drag started 0.4 s after Undo, photographed 400 ms in — the card has **not moved at all**, no
translation and no swipe cue (`walkthrough-mid-drag-undo-0.4s.png`). The identical harness at a 1.2 s
delay shows the card translated well left and rotated (`walkthrough-mid-drag-undo-1.2s.png`). The harness
is sound; the input is being discarded.

`walkthrough-card-settled-undo-0.4s.png` shows the card at that same 0.4 s mark: fully restored, settled,
nothing animating that a reader could see.

## What this means for the diagnosis

`design.md` D1 claimed that keeping one pointer handler attached across head-article changes makes the
card touchable. **That holds for deck-advance and is falsified for Undo.** `pointerInput(Unit)` stops the
handler being *re-keyed*; it does not stop the node being *recreated* if the card leaves and re-enters
composition, and it does not cover whatever else is specific to the restore path. The mechanism behind the
remaining window is not known, and guessing at it is what produced this item's first stop.

§5.3's instrumented test cannot see this: it asserts handler persistence across a re-key, which is exactly
what was fixed. A third verification design is needed, not a third fix written against an untested cause.

**Owner decision, 2026-08-27: the item returns to design.** `1f19cea` and `29344aa` stay on the branch as
partial, correct, evidenced work.

## Two method notes worth keeping

- **The first fast-pair trial read as a failure and was not one.** It gave 1 record, but a
  *"Refresh failed. Showing the last available content."* banner appeared between the two swipes and moved
  the card. Re-run cleanly it gives 2. A layout shift mid-trial is as good at faking this defect as the
  defect is.
- **Emulator DNS was unavailable.** Rather than a synthetic fixture, the real production payload
  (209,610 bytes from `https://irodriguez.io/News-Agregator/data/articles.json`) was fetched on the host
  and seeded into the app's dataset cache, so the walkthrough ran against the same 204-article dataset a
  reader would get.
