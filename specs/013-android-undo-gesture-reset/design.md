# 013 — Design note

One decision, one file pair. Short on purpose: the diagnosis in `spec.md` §1.2 is the substance, and the
fix follows from it.

## D1 — Release the lock when the commit resolves; do not weaken the lock

`SwipeGestureTest.kt:164`, *"a second gesture is refused while a commit is in flight"*, is authoritative
and **stays byte-identical**. The lock is correct while a commit is genuinely in flight — that is what
stops a second swipe from racing a pending write. What is missing is a release for when that commit
**resolves and the card survives**.

Two candidate shapes were considered and one is rejected:

- **Rejected: make `onComplete` call the existing `restoreCard()` unconditionally.** It is a one-word
  change and it does clear the lock, but `restoreCard()` also animates the travel home. On a successful
  commit the card is leaving, so that would pull a departing card back toward the centre for up to
  `EXIT_DURATION_MS` before it is removed — a visible snap, traded for a smaller diff.
- **Chosen: split the reset.** A lock-only release on `SwipeGesture.State` that touches no travel, called
  when the commit resolves on **both** outcomes; `restoreCard()` — lock release *plus* the animation
  home — stays on the failure and lost-pointer paths where the card is staying and the travel genuinely
  must return.

This keeps 008 D2's shape: the gesture is a pure object and the Composable is a shell. The new method is
pure, has no Android dependency, and is JVM-testable; the shell decides when to call it. It also keeps
the fix inside `commitInFlight`'s existing meaning rather than introducing a second flag to track
whether the first one is still trustworthy.

## D2 — The invariant that broke, stated so it cannot break silently again

`commitInFlight` was safe to latch forever because **the next card was always a different article**, and
`remember(article.id, …)` therefore built a fresh state. That assumption was never written down, and Undo
is the first path that returns the *same* article to the *same* slot.

The fix removes the dependency rather than patching the one path that violated it: once the lock is
released whenever a commit resolves, no caller has to reason about whether the card is leaving, coming
back, or staying. Any future path that returns a card to Discover — a queue-pane undo, a dataset refresh
that restores a dismissed article, item 012's header move re-keying the card — inherits correct
behaviour without knowing this file exists.

The assumption is recorded in `backlog.md` under `Debt` so the next person to touch the gesture does not
have to rediscover it.

## D3 — Why the walkthrough is definition-of-done rather than a nice-to-have

The state object's contract is fully unit-testable and the tests in `spec.md` §4 prove it. They cannot
prove the thing the owner actually hit, which is a Compose recomposition returning a latched state object
to a live card. Instrumented tests are a deliberate non-goal for CI (`backlog.md` Parked, from 002 slice
4), so the device check is the only evidence that covers the real path.

`waves/wave-b-note.md`'s headline lesson applies directly: wave B's two most valuable defects were found
by the owner using the app, and none by any gate. This defect was found the same way. Its fix gets
verified the same way.
