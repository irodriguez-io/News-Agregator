# 013 slice 2 — walkthrough after the `requireUnconsumed` fix (2026-08-28)

`spec.md` §5.4, all four steps, driven by the **orchestrator** over `adb` on `Pixel_10`
(`emulator-5554`) against branch head `11d8353`. The implementer was explicitly barred from running this;
this item has produced a false pass twice by letting the author score their own walkthrough.

**Result: all four steps pass, and the headline symptom is gone.**

Every claim below is read from the pulled state document, and — per the correction slice 1 owed to §5.4 —
**scored by which article moved, not merely that a weight moved.** Deck head after `pm clear` is
`322d8f25…` (`science_aaas`); the article behind it is `8c80f6f9…` (`ietf_oauth`).

## Step 1 — two fast consecutive swipes, no Undo, 0.35 s apart: **passes**

```
records=2 articles=['322d8f25:dismissed', '8c80f6f9:dismissed']
sources={'science_aaas': (-0.35, 1), 'ietf_oauth': (-0.35, 1)}
```

Both commit, two distinct articles, two source weights moved. The deck-advance path `29344aa` fixed has
not regressed.

## Step 2 — swipe → Undo → reswipe: **passes at every delay**

Records after the reswipe, with the article id asserted. `1 = 322d8f25` is the only correct outcome —
the reswipe landing on the *restored* card.

| delay after Undo | before the fix (`step0-undo-window.md`) | after the fix |
| --- | --- | --- |
| 0.05 s | — | **1 — `322d8f25`** |
| 0.1 s | — | **1 — `322d8f25`** |
| 0.15 s | — | **1 — `322d8f25`** |
| 0.2 s | 0 — discarded | **1 — `322d8f25`** |
| 0.3 s | 0 — discarded | **1 — `322d8f25`** |
| 0.4 s | 0 — discarded | **1 — `322d8f25`** |
| 0.5 s | 1 | **1 — `322d8f25`** |
| 0.6 s | — | **1 — `322d8f25`** |
| 0.7 s | — | **1 — `322d8f25`** |
| 0.8 s | 1 | **1 — `322d8f25`** |
| 1.2 s | — | **1 — `322d8f25`** |

Each of the five §5.4 delays also raised its own undo offer — an `Undo` affordance was present in the
`uiautomator` dump taken after the reswipe in every case, as the scenario requires.

The window is closed across its whole width and 130 ms either side of it, and nothing that worked before
regressed.

## Step 3 — 0.4 s delay, 900 ms drag, photographed 400 ms in: **passes**

`walkthrough-s2-mid-drag-undo-0.4s.png`. The card is translated well to the left, rotated, and showing its
**"Not interested"** cue, while *"Undo completed."* is still on screen. The pre-fix frame at the identical
delay and harness (`walkthrough-mid-drag-undo-0.4s.png`) shows the card **not moved at all**. The gesture
commits: `records=1 articles=['322d8f25:dismissed']`.

## Step 4 — settled control, 3 s apart: **passes**

```
records=2 articles=['322d8f25:dismissed', '8c80f6f9:dismissed']
```

Additionally, the **save** direction still works — a right swipe gives
`records=1 articles=['322d8f25:saved'] sources={'science_aaas': (0.45, 1)}`, a positive weight, so the fix
did not disturb direction handling.

## The regression this fix actually risked, checked explicitly

`requireUnconsumed = false` lets the card look at a DOWN the ancestor scroll has claimed, so the thing to
prove is that it does **not** now steal a vertical scroll. A 1000 px vertical drag started on the card:

- the page scrolled — the card's source label moved from `y = 1530` to `y = 638`;
- **`records=0`** — no swipe was committed.

`SwipeGesture`'s intent slop does exactly the job `design.md` D10 relied on it for, and `SwipeGesture.kt`
was not touched to achieve it.

## Still open, and deliberately not fixed here

**The delay-0 misattribution.** At a nominal delay of 0 the DOWN is still adopted against the *outgoing*
article — three runs gave `ietf_oauth`, `ietf_oauth`, `science_aaas`. This is unchanged by the fix, which
is correct: it is a publish-ordering defect, not a consumption defect, it is **not** in this item's
scenarios (`spec.md` §4), and slice 2's brief instructed the implementer to leave it alone. It needs its
own item. Recording it here so it is carried rather than rediscovered.

## Method

- `pm clear` before every trial, so each runs from an identical 215-article deck.
- Every swipe aimed at `(800→150, 1800)`, inside the card's true bounds read from `uiautomator dump`.
- The whole sequence runs inside one on-device `adb shell`, so the toast is still live when Undo is tapped.
- **Harness offset:** `input` costs ~130 ms of process startup between the Undo tap returning and the swipe
  beginning, so every delay quoted is nominal and the true delay is ~130 ms higher. The sweep from 0.05 s
  to 1.2 s covers the measured band with margin on both sides regardless.
- Emulator carried the branch-head APK built from `11d8353`. Note the RED reproduction run installs the
  *unfixed* build; it was rebuilt and reinstalled before this walkthrough.
