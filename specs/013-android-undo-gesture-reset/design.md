# 013 — Design note (second revision, 2026-08-27)

The first version of this note is superseded in full: it argued a `commitInFlight` fix that `spec.md`
§1.3 records as wrong. The second version's D1, D2 and D4 stand unchanged and are reproduced here. **D3
is replaced** — it prescribed a verification strategy that could not be made to work (`spec.md` §1.5) —
and D5, D6 and D7 are new.

## D1 — Keep the pointer handler attached across head-article changes

`Modifier.pointerInput(key)` restarts its handler whenever the key changes, and `ArticleCard.kt` keys it
on `gestureState` — an object `remember(article.id, …)` rebuilds for every new head article. So the card's
touch handling is destroyed and relaunched on every deck change, and the relaunch measured **381–777 ms**
behind the card becoming visible.

**Decision:** key the pointer handler on something stable and read the current per-article values through
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

## D3 — *(replaced)* The instrumented test asserts the cause, because the window is unobservable in a test harness

**Superseded:** the second version required the instrumented test to reproduce the *symptom* — a touch
dropped during the attach window — and asserted that `ComposeTestRule.mainClock` would "make the timing
deterministic". Seven harness variations all passed against unchanged production code (`spec.md` §1.5).

The assumption was backwards. The window exists because a real frame clock is contended; `ComposeTestRule`
idles until composition and its coroutines have settled *before* it injects, which is precisely the
condition under which the handler is already attached. A harness built to remove timing windows cannot
observe one. This was a flaw in the verification design, not in D1.

**Decision:** the instrumented test asserts the **cause** — that a head-article change tears the handler
down — which is a composition fact and involves no race. The discriminator is a gesture that spans a
head-article change: a destroyed handler cancels it, and `awaitFirstDown` will not adopt a pointer that is
already pressed, so nothing commits. `spec.md` §5.3 gives the shape.

What is kept from the old D3, because it was right: this fix is Compose wiring with no pure object whose
contract changes, which is exactly why 258 green unit tests coexisted with a live defect. The project
parks instrumented tests **for CI** (002 slice 4), not for local use, and already keeps
`MainActivityLaunchSmokeTest` as an on-demand guard. This is a second one on the same terms, with no new
dependency.

Two alternatives were considered and rejected:

- **A test-only probe parameter on `ArticleCard`** — a `onGestureHandlerLaunch: () -> Unit = {}` default
  argument counted by the test. It would assert the cause directly and unambiguously, but it puts test
  scaffolding in a production signature to observe something the gesture-spanning test already observes
  without it.
- **An end-to-end `uiautomator` test replaying §5.1's two fast swipes.** It would assert the real symptom,
  but it needs `androidx.test.uiautomator`, which is a new dependency requiring approval, and it would be
  timing-dependent — the same flakiness the walkthrough already carries deliberately and a test should not.

## D4 — Why the specification carried a falsifiable prediction, and why that gate is now closed

`spec.md` §5.1 required reproducing the defect **without Undo**, before any fix, on the grounds that D1
predicts it must be reproducible that way. It reproduced (`investigation/step0-reproduction.md`).

That check was added because the first version reasoned from a plausible mechanism straight to a fix, and
the mechanism was wrong. It has now done its job twice over: it confirmed D1, and the attempt to satisfy
the gate that followed it is what exposed the broken verification design in D3 — before a fix was written,
not after one shipped.

The gate is spent. It is now evidence (`spec.md` §1.5), and §5.4 re-runs the same case as a GREEN.

## D5 — The persistent handler must read *every* per-article value through current state, not by capture

This is the hazard D1 creates and it is the likeliest way to get the fix wrong. The handler's body closes
over `article`, `gestureState`, `swipeCue`'s setter, the `translationX` and `rotationDegrees` `Animatable`s,
and the local `restoreCard` / `animateToGestureState` / `snapToGestureState` helpers. **All of them are
`remember(article.id)`-keyed.** Today that is invisible, because the handler dies and is rebuilt with fresh
captures every time they change — the bug and its own concealment are the same mechanism.

Key the handler on a stable key and the captures go stale: a persistent handler would animate the previous
card's `Animatable` and commit the previous card's `Article`. Nothing in the JVM suite would notice.

**Decision:** hoist the per-article values into a single holder built by one `remember(article.id, …)`,
and read it inside the handler through `rememberUpdatedState`. Read it **once per gesture, when the
gesture begins**, not per event — a gesture belongs to the card it started on, which is what `spec.md`
§4's "a gesture commits against the card it started on" asserts.

## D6 — The gesture state stays per-article, and that is not negotiable

`SwipeGesture.State` carries travel, intent and the commit lock. Sharing one across cards would make the
symptom disappear while introducing a worse defect — travel or a latched commit leaking from the card that
left. `spec.md` §4's "the gesture state is still per-card" exists to make that failure mode a test rather
than a review comment. `SwipeGesture.kt` is not touched.

## D7 — Splitting the button out is a scope decision, not a deferral of difficulty

The Save for later button fails in the same window and never consults the gesture state (`spec.md` §1.3),
so D1 would not fix it. Its mechanism is unknown; the honest options were to diagnose it here or to give it
its own item. Diagnosing it here would mean this item carrying two unrelated mechanisms and shipping only
when both are understood — and the first version of this item is a standing lesson about assuming two
symptoms in one window share a cause.

**Decision** (owner, 2026-08-27): it becomes item 014, queued in `backlog.md`, starting from its own Step 0
reproduction. This item keeps the evidence and drops the scenario, the walkthrough step, and the claim.
