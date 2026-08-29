# 013 Step 0 — the Undo window has a name: the ancestor scroll consumes the DOWN (2026-08-28)

Slice 1 of the third revision. **Diagnosis only — no file under `android/` is modified by this slice.**
Run over `adb` on `Pixel_10` (`emulator-5554`), fresh boot, against branch head `2bfd768` with temporary
`logcat` instrumentation that was reverted before this report was written (`git checkout -- android/`).

**Gate `spec.md` §5.1: closed.** Mechanism **M1**. All three other candidates falsified with evidence.
Fix shape: `design.md` D10's **M1 branch** — `ArticleCard.kt:125`'s `requireUnconsumed`.

---

## 1. The mechanism

While `DiscoverScreen.kt:74-87`'s article-change `animateScrollTo` is in flight, the ancestor
`ScrollView` **consumes the DOWN in the Initial pointer pass** — Compose's standard "touch stops a running
scroll animation" behaviour. `ArticleCard.kt:125` waits with `awaitFirstDown(requireUnconsumed = true)`,
which will not adopt a DOWN that is already consumed, so the gesture is never started and nothing is
recorded.

The card is not slow to attach, is not recreated, and does not refuse the touch. **It never hears it.**

### The paired observation that confirms it

Identical coordinates `(753, 1235)`, identical node, one variable — whether the scroll animation was
running when the finger landed.

**Failing, 0.3 s after Undo** (`undo-d0.3-scale1.logcat`, `undo-d0.3-scale1.state.json` → `records=0`):

```
11:11:27.219  compose article=322d8f25… node=65242305     ← restored card composes
11:11:27.238  animateScrollTo BEGIN from=445 to=1084       ← ancestor scroll starts, ~380 ms to run
11:11:27.508  PROBE down consumed=true  pos=753.0,1235.0   ← DOWN arrives mid-flight, already consumed
              (no "awaitFirstDown RETURNED", no "gestureState.down")
```

**Passing, 0.8 s after Undo** (`undo-d0.8-scale1.logcat`, `…state.json` → `records=1`, `science_aaas`):

```
11:11:37.061  animateScrollTo BEGIN from=445 to=1084
11:11:37.440  animateScrollTo END at=892                   ← scroll finishes
11:11:37.801  awaitFirstDown RETURNED article=322d8f25…    ← DOWN adopted, 361 ms later
11:11:37.801  gestureState.down -> true
11:11:37.801  PROBE down consumed=false pos=753.0,1235.0   ← same pixel, not consumed
```

The `PROBE` line is a temporary sibling `awaitFirstDown(requireUnconsumed = false)` that sees every DOWN
whether or not it was claimed. It is what separates "the touch never arrived" from "the touch arrived and
was taken", and it says plainly: **the touch arrives, and something upstream has already claimed it.**

### The reduced-motion discriminator, run first as `spec.md` §5.1 requires

`animator_duration_scale = 0` makes `DiscoverScreen.kt:81-85` substitute an instant `scrollTo`. The window
closes **completely** — every delay in the band commits:

| delay after Undo | scale 1 | scale 0 (reduced motion) |
| --- | --- | --- |
| 0.2 s | 0 | 1 |
| 0.3 s | 0 | 1 |
| 0.4 s | 0 | 1 |
| 0.5 s | 1 | 1 |
| 0.8 s | 1 | 1 |

`undo-d0.3-scale0.logcat` shows why: **no `animateScrollTo BEGIN` at all**, `PROBE down consumed=false`,
`awaitFirstDown RETURNED article=322d8f25…`, record written.

Reduced motion also zeroes the card's exit animation, so on its own this trial implicates the scroll
without isolating it. The consumption probe above is what isolates it: at scale 1, the *same* coordinate
is consumed inside the animation and unconsumed 361 ms after it ends.

---

## 2. Why 0.2 s "passed", and why 0.8 s passes — `spec.md` §5.1 condition 2

**0.8 s is the easy half:** the scroll animation runs about 380 ms from a start roughly one frame after the
restored card composes, so by 0.8 s it is long finished and the DOWN arrives unclaimed.

**0.2 s is the half that has been misread since `post-fix-walkthrough.md`, and it is not a pass.**

At the very bottom of the range the DOWN beats the scroll's start — `DiscoverScreen.kt:80` awaits
`withFrameNanos { }` before animating, and the touch can land inside that gap. But it also beats the
*restore*, and it is then attributed to **the article the reader is no longer looking at**
(`undo-d0.0-scale1.logcat`):

```
11:11:17.755  awaitFirstDown RETURNED article=8c80f6f9…   ← the OUTGOING article
11:11:17.777  compose article=322d8f25…                    ← the restored card composes 22 ms later
```

`undo-d0.0-scale1.state.json` records `ietf_oauth`, not `science_aaas`. Across three runs at delay 0 it
went to the wrong article **twice** and the right one once — a race, not a pass.

**This is why `spec.md` §1.5's central mystery does not exist.** The previous walkthrough scored the trial
by "did a weight move" (`spec.md` §5.4), a weight did move, and it read as a pass. Scored by *which*
article moved, the low end is a second defect, not a success. The shape is therefore **not a band open on
both sides** — it is a plain window that opens when `animateScrollTo` starts and closes when it ends, with
a race below it that was being counted on the wrong side of the ledger.

> **Method correction owed to `spec.md` §5.4.** "A card leaving the deck is not proof that the action
> landed" is not strong enough. **A record appearing is not proof either — the record must name the
> article the reader was looking at.** Slice 3's walkthrough must assert the article id, and this is a
> third instance of this item's recurring failure: a check that is one notch too weak to see the defect.

---

## 3. The other three candidates — `spec.md` §5.1 condition 3

**M2 — the card node is recreated, not re-keyed. Falsified.** `pointerInput START` logs **exactly once
per process**, and the node identity probe holds the same value across every head-article change including
the Undo restore — `node=65242305` at the first swipe, at the deck advance, and at the restore, in every
run captured. What *does* change is `values=…`, the per-article `ArticleGestureValues`, which is D5/D6
behaving as designed. The subtree is not destroyed; D1's fix is intact and doing its job.

**M3 — the overlay takes the touch. Falsified.** At 0.8 s the `UNDO_COMPLETED` `LiveStatusMessage` is on
screen at the same moment the DOWN at `(753, 1235)` arrives **unconsumed** and commits. The consumer
correlates with scroll-in-flight and not at all with which overlay is mounted, and it claims a DOWN 900 px
above the toast's own bounds (`[645,2062][743,2125]`, `uiautomator-card-bounds.xml`). An overlay that took
touches outside its bounds could not pass the 0.8 s trial.

**M4 — the gesture state refuses the DOWN. Falsified.** `gestureState.down -> true` on **every** occasion
it was reached, in every trial. In the failing trials it is never reached at all, because
`awaitFirstDown` never returns. `bf79c42`'s `releaseCommitLock()` is not implicated, and stays for the
reason `design.md` D2 gives.

---

## 4. What this implies for slice 2 — `spec.md` §5.1 condition 4

`design.md` D10's **M1 branch applies unchanged**, and its reasoning survives contact with the evidence:
the card's gesture is horizontal, the ancestor's is vertical, and `SwipeGesture`'s intent slop already
prevents the card from stealing a vertical drag. The card is entitled to look at a DOWN the scroll has
claimed. The change is `ArticleCard.kt:125`'s `requireUnconsumed`.

The RED D10 pre-registers is now known to be the right shape: wrap `ArticleCard` in a `verticalScroll`,
start a long `animateScrollTo` with `mainClock` paused, hold it mid-flight, inject. This is a controllable
programmatic animation, which `spec.md` §5.3 explicitly permits and which the seven failed harnesses were
not.

Two things slice 2 should carry that this diagnosis adds:

- **`requireUnconsumed = false` alone does not fix the delay-0 misattribution** in §2. That is a separate
  ordering defect — the DOWN adopted against the outgoing article — and it is *not* in this item's
  scenarios. Record it rather than absorb it: it is the honest reading of `undo-d0.0-scale1.state.json`,
  and quietly widening slice 2 to cover it is how this item lost its first two passes.
- **Do not suppress or shorten the D12 scroll.** D10 already rejects it, and §1 shows why it would be
  aimed at the wrong thing: the scroll is behaving correctly, and the card is simply refusing to listen
  through it.

---

## 5. Method

- Fresh emulator boot; `pm clear` before every trial, so each runs from an identical deck — head article
  `322d8f25471b4a211096` (`science_aaas`), 215 articles fetched live.
- Every swipe aimed at `(800→150, 1800)`, inside the card's true bounds `[47,1457][1033,2151]` read from
  `adb shell uiautomator dump` (`uiautomator-card-bounds.xml`). The card's parent is the
  `scrollable=true` `ScrollView` `[0,310][1080,2151]` — the ancestor named in M1.
- The whole swipe → Undo → delay → reswipe sequence runs inside **one on-device `adb shell`** so the toast
  is still live when Undo is tapped. Tapping Undo from the host after a `uiautomator dump` misses it; that
  cost one discarded trial.
- **Harness offset:** `input` costs ~130 ms of process startup between the Undo tap returning and the
  swipe beginning (measured), so every delay quoted here is a *nominal* `sleep` value and the true delay is
  ~130 ms higher. This is why the band sits at 0.2–0.4 s in this report and 0.3–0.5 s in
  `post-fix-walkthrough.md`. It shifts the numbers; it changes nothing about the mechanism, which is
  established from timestamps in the same log rather than from the nominal delay.
- Instrumentation was four `Log.d("D13", …)` probes in `ArticleCard.kt` and three in `DiscoverScreen.kt`,
  plus the sibling consumption probe. **Reverted; `android/` is byte-identical to `2bfd768`.**
- Gates re-run after the revert: `:app:testDebugUnitTest` **258 tests, 0 failures**; `:app:assembleDebug`
  `BUILD SUCCESSFUL`.

## 6. Artifacts

`step0-undo-window/` — for each of the four trials, the full `D13` logcat and the pulled state document:

| file | delay | animator scale | result |
| --- | --- | --- | --- |
| `undo-d0.0-scale1` | 0 | 1 | `records=1` but **`ietf_oauth`** — wrong article |
| `undo-d0.3-scale1` | 0.3 s | 1 | `records=0` — DOWN consumed, discarded |
| `undo-d0.8-scale1` | 0.8 s | 1 | `records=1`, `science_aaas` — commits |
| `undo-d0.3-scale0` | 0.3 s | 0 | `records=1`, `science_aaas` — window closed |

`uiautomator-card-bounds.xml` — the settled pre-swipe tree the coordinates were read from.
