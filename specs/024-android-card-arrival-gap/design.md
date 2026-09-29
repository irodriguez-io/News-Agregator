# 024 — design note

Warranted: this item reopens 023's D2, the sequencing that keeps items 013 and 015 closed, on a surface
whose defects have repeatedly been invisible to the JVM gate. Every factual claim about a file below was
checked against `main` at `7667ddf` on 2026-09-29, with line numbers — 023's evidence §1 is why.

---

## D1 — Commit on departure, not on curve completion; the save does not move earlier

Three shapes were put to the owner on 2026-09-29 in plain terms. The owner chose the first.

| Option | Removes | Leaves | Cost |
|---|---|---|---|
| **Commit when the card has left the viewport** | the ~245 ms invisible tail | the save (undo's cost) | card and gesture files only; Amendment 12 |
| Save from release, swap when the full exit ends | the save | the ~245 ms tail — the larger part | view model; reopens D2 further |
| Both | both | ~nothing | the screen shows the old card after the view model has moved on — item 015's ground |

**Why the first.** It matches the benchmark exactly: undo is *save → entrance*, and this makes a swipe
*visible exit → save → entrance*. It keeps D2's two load-bearing properties — one card on screen, and the
commit issued only after the card has visibly exited — and it keeps the view model, persistence and
`ui/screens/discover/**` out of the diff. The handover expected a wider surface than 023's; the reverse is
true.

## D2 — What changes in 023's D2, stated precisely

023's D2 held that *"the head article does not change until the outgoing card has finished leaving."* That
now reads **"until the outgoing card is no longer visible."** What survives unchanged:

- **One composed card.** No `AnimatedContent`, no `Crossfade`, no second card. The head change still
  rebuilds the one card through `remember(article.id, …)` (`ArticleCard.kt:86-110`).
- **The commit follows the visible exit.** §43's order is preserved on screen.
- **The departed card declines touches while its commit is in flight** — `SwipeGesture.State.down` returns
  `false` while `commitInFlight` (`SwipeGesture.kt:91-92`), as today. That card is off-screen, so this is the
  existing "a tap in the gap finds no card" behaviour, not a new refusal.

What is new: **after the head changes, the departed card's exit coroutine is still running** — up to about
245 ms of curve on `Animatable`s that are no longer drawn, launched in `restoreScope`
(`ArticleCard.kt:114`, `:147`). It must not touch the replacement's state, and nothing in it may call the
commit a second time. The instrumented test for a swipe in this window is the item's real acceptance
criterion.

## D3 — "No longer visible" is a pure geometric test, with curve completion as the backstop

A new pure function in `SwipeGesture` — JVM-testable, no Android runtime, as `ExitEmphasizedEasing`'s lazy
path already arranges for that file:

```text
departed  ⇔  the card's rotated bounds, translated by translationX, lie wholly outside [0, viewportWidth]
inputs       card left and width in viewport coordinates, card height, translationX, rotationDegrees,
             viewportWidthPx
rotation     about the card's centre — graphicsLayer's default transform origin
```

For a rotation θ, the rotated card's horizontal half-extent is `(w/2)·cos θ + (h/2)·sin |θ|`. Rightward, it
has departed when `centreX + translationX − halfExtent ≥ viewportWidth`; leftward, when
`centreX + translationX + halfExtent ≤ 0`.

**The card's position and size are not known to the card today.** `ArticleCard` reads
`LocalConfiguration.screenWidthDp` for the viewport (`ArticleCard.kt:79-82`) but never measures itself.
It needs `onPlaced` or `onGloballyPositioned` on its own modifier chain — the pattern
`DiscoverScreen.kt:145` already uses for the card's offsets.

**Backstop: curve completion.** If the test never passes — viewports above roughly 615 dp, every phone in
landscape (`spec.md` §4) — the commit is requested when `animateToGestureState()` returns, exactly as today.
**Exactly once**, whichever comes first.

**Reduced motion is unchanged by construction.** `exitTranslationX` is `0f` and the spec is `snap()`
(`SwipeGesture.kt:137-139`, `ArticleCard.kt:262-266`), so the curve completes on its first frame and the
backstop fires immediately.

## D4 — Where the check runs

`Animatable.animateTo` takes a per-frame `block`. The commit path's translation animation evaluates D3 in
that block and requests the commit the first frame it passes; the backstop requests it after the exit
returns if the block never did. A guard makes the request idempotent. The commit request itself is not
suspending — `currentOnSwipeCommit` hands off to `viewModel.launchArticleAction`, which launches in
`viewModelScope` — so issuing it from the frame block does not tie the save's lifetime to the exit
animation's.

The non-commit path — a released-below-threshold card animating back — does not evaluate D3 and is
untouched (`ArticleCard.kt:140-141`).

## D5 — A failed save can now arrive mid-exit

Today the save starts after the exit, so a failure (`persisted = false`, `ArticleCard.kt:151-153`) always
reverses a card that has fully exited. Now it can arrive while the exit curve is still running. `restoreCard`
calls `gestureState.restore()` and animates the three `Animatable`s back; a new `animateTo` on an
`Animatable` cancels the one in flight, so the card reverses from wherever it is. **That is the correct
behaviour and it needs no new mechanism — but it is new timing, so it gets an instrumented test** (§50: a
failed save must not be visually finalised).

## D6 — Nothing else moves

No new dependency. No change to `EXIT_DURATION_MS`, `ExitEmphasizedEasing`, the distance or rotation
constants, the fade, the entrance, `articleSwipeMotionSpec` or `articleEntranceMotionSpec`. No change to
`AppViewModel`, `LocalStateRepository`, `ui/screens/discover/**` or `IntentionalReadingApp.kt`.

## D7 — The prediction, and when to check it

**Prediction:** the residual wait is the save (`fsync` on `Dispatchers.IO`, `LocalStateFile.kt:39`) plus
`publish()`'s re-rank on the main thread, and it will read like undo's.

**Check it at the walkthrough, side by side with undo.** If a pause remains, the lever is starting the save
before departure — D1's rejected rows — and not any motion value. Write the result into `evidence.md` and
correct this decision in place if it is wrong, with the original kept beneath, as 023's D7 and D8 were.
