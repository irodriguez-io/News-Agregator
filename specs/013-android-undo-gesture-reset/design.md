# 013 — Design note (rewritten 2026-08-27)

The first version of this note is superseded in full. It argued a `commitInFlight` fix that `spec.md`
§1.3 now records as wrong. What follows replaces it.

## D1 — Keep the pointer handler attached across head-article changes

`Modifier.pointerInput(key)` restarts its handler whenever the key changes, and `ArticleCard.kt` keys it
on `gestureState` — an object `remember(article.id, …)` rebuilds for every new head article. So the card's
touch handling is destroyed and relaunched on every deck change, and the relaunch measured **381–777 ms**
behind the card becoming visible.

**Decision:** key the pointer handler on something stable and read the current gesture state through
`rememberUpdatedState`, which this file already uses for `onSwipeCommit` (`ArticleCard.kt:102`). The
handler stops being torn down; the state it drives stays per-article.

Two alternatives were considered and rejected:

- **Shorten the window by changing the scroll effects.** The D12 article-change scroll is what starves the
  handler's launch. Removing or deferring it would narrow the window without closing it, and scroll
  behaviour is item 012's ground — `backlog.md` already records three effects on this screen that will
  eventually need reconciling. Making the card touchable is the smaller and more honest change.
- **Await handler attachment before showing the card.** That trades a lost touch for a visible stall, and
  the reader cannot tell a stalled card from a slow one.

## D2 — `bf79c42` stays, and its rationale is corrected rather than deleted

`releaseCommitLock()` was committed against the wrong diagnosis. It stays because D1 makes it load-bearing
for a new reason: with one persistent handler, a gesture state still latched from a previous commit can be
swapped in behind it, and nothing would release it. The lock release is the precondition that makes a
persistent handler safe.

Correcting the reasoning in `spec.md` §1.4 rather than reverting and re-committing keeps the record honest
about what happened. A future reader deserves to see that the code was right and the reason was wrong,
which is a different failure from the code being wrong.

## D3 — The test is instrumented, and that is a considered exception

Every previous Android item in this project has been provable on the JVM, and `execution-model.md` treats
that as the norm. This fix is Compose wiring: there is no pure object whose contract changes, which is
exactly why 258 green unit tests coexisted with a live defect.

The project parks instrumented tests **for CI** (002 slice 4) — not for local use, and it already keeps
`MainActivityLaunchSmokeTest` as an on-demand guard in `android/app/src/androidTest/`. This item adds a
second one on the same terms. `androidx.compose.ui.test.junit4` is already declared, so no dependency rule
is touched and CI stays emulator-free.

**The stop condition matters more than the test.** If the instrumented test cannot reproduce the dropped
touch, the implementer stops and reports rather than weakening it or leaning on the walkthrough. The first
version of this item passed a unit gate and a diff review with the defect fully intact; the guard against
repeating that is refusing to accept a test that does not fail first.

## D4 — Why the specification now carries a falsifiable prediction

`spec.md` §5.1 requires reproducing the defect **without Undo**, before any fix, on the grounds that D1
predicts it must be reproducible that way.

This is the check the first version skipped. That version reasoned from a plausible mechanism straight to
a fix, and the mechanism was wrong; nothing in the process forced the diagnosis to be tested on its own
terms before code was written. A prediction that would be false if the diagnosis were wrong, tested first,
is the cheapest available guard against doing it a third time.
