# 013 — Design note (third revision, 2026-08-27)

The first version of this note is superseded in full: it argued a `commitInFlight` fix that `spec.md`
§1.3 records as wrong. The second version's D1, D2 and D4–D7 stand and are reproduced below; its D3 is
reproduced with its outcome recorded, since the test it prescribed was built, is green, and did **not**
catch the remaining defect.

**What this revision adds.** D1's claim was true and narrower than it was stated: it fixed the
deck-advance path and left the Undo restore path broken (`spec.md` §1.4). The remaining mechanism is not
known. **D8** enumerates the candidates and the observation that separates each; **D9** records the
owner's artificial-lag proposal and why it is rejected as an input lag; **D10** pre-registers a fix shape
per candidate so slice 2 needs no fourth design pass; **D11** says why no visible non-interactive state
is added.

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

**Scope correction, third revision.** This decision is **partially confirmed and partially falsified.**
It fixed the deck-advance path — two fast swipes 0.35 s apart now produce two records where they produced
one — and it does not fix the Undo restore path, where a swipe 0.3–0.5 s after Undo is still discarded.
`pointerInput(Unit)` stops the handler being *re-keyed*; it does not stop the node being *recreated*, and
it says nothing about a DOWN that never reaches the handler in the first place. Everything D1 decided
stays in the code. What it claimed about the reader-facing outcome was too broad, and D8 is where the rest
of that outcome gets earned.

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

**Outcome, third revision: this decision was correct and insufficient.** The test was built
(`ArticleCardGestureTest`, `1f19cea`), its RED was reproduced independently with the fix absent, and it is
green at `29344aa`. It also could not have caught the Undo window, because it asserts persistence across a
re-key and that is exactly what was fixed. A test that asserts the cause is only as good as the cause,
which is the argument for D8 rather than against D3.

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

## D8 — Diagnose before fixing, and the diagnosis must explain the 0.2 s pass

The Undo window is **a band, not a floor**: 0.2 s lands, 0.3–0.5 s is discarded, 0.8 s lands
(`spec.md` §1.4). Attach latency alone cannot produce that shape — it predicts a monotonic floor. So the
mechanism is either something that is not yet present at 0.2 s, or something that is armed and then
released, and nothing proposed so far accounts for either.

**Decision:** slice 1 is a diagnosis with no product code in it, gated by `spec.md` §5.1, and it must
explain the passes as well as the failures. Four candidates, each with the observation that separates it:

- **M1 — an ancestor scrollable consumes the DOWN.** `DiscoverScreen.kt:74-87` runs
  `scrollState.animateScrollTo(cardTopOffset)` when the head article changes, which on the Undo path is
  animating in exactly this window, and `ArticleCard.kt:125` waits with
  `awaitFirstDown(requireUnconsumed = true)` — a DOWN an ancestor has already consumed is never adopted.
  This is the only candidate that predicts the band's shape without extra assumptions: at 0.2 s the effect
  has published but the animation has not begun (it awaits `withFrameNanos {}` first), at 0.3–0.5 s it is
  running, by 0.8 s it is done.
  *Discriminators, cheapest first:* **the 0.4 s trial under reduced motion**, where
  `DiscoverScreen.kt:81-85` substitutes an instant `scrollTo` — the window should close; then
  `scrollState.isScrollInProgress` logged at the moment of the DOWN; then whether `awaitFirstDown` returns
  at all.
- **M2 — the card node is recreated, not re-keyed.** If the Undo publish moves Discover through a
  different `DiscoverUiState` subclass, or otherwise swaps the `when (state)` branch, the `ArticleCard`
  subtree is destroyed and rebuilt and `pointerInput(Unit)` relaunches with fresh attach latency.
  *Discriminator:* a `compose ArticleCard` log at a **new** composition identity across the Undo, plus a
  `pointerInput start` with no preceding `awaitFirstDown returned`. M2 owes a separate explanation for the
  0.2 s pass, and if it cannot give one it is at most half the story.
- **M3 — the overlay takes the touch.** `IntentionalReadingApp.kt:313-331` swaps `UndoToast` out and
  `LiveStatusMessage` (`UNDO_COMPLETED`) in, in the same bottom-aligned `Column`.
  *Largely already falsified* — the walkthrough failed at `y = 1285`, in the card's text area, as well as
  at `y = 1760`. Listed so it is closed out with evidence rather than by assumption.
- **M4 — the gesture state refuses the DOWN.** `gestureState.down(...)` returning `false`, from a commit
  lock or an intent state carried across the restore. *Contradicted on the deck-advance path and never
  checked on the Undo path.* One log line settles it, so check it.

The rejected alternative is to skip this and fix M1, which is the leading candidate. It is rejected because
this item has stopped twice for exactly that move, and the second time it stopped *after* a fix was merged
to the branch with a green instrumented guard defending it. The reduced-motion trial in `spec.md` §5.1
costs one `adb` run; a third wrong fix costs another full pass.

## D9 — Artificial lag: rejected as an input lag, on this item's own measurements

The owner proposed (2026-08-27) a deliberate lag after a successful swipe, so the next swipe cannot land
close enough in time to be lost. Three forms were considered.

- **L1 — suppress input for N ms after a successful commit.** **Rejected: it does not intersect the
  defect.** The deck-advance path is the one a post-commit gate protects, and it already accepts a swipe
  0.35 s later (`spec.md` §1.2). The broken path is the Undo restore, which a post-commit gate never sees.
- **L2 — suppress input for N ms after the Undo restore.** **Rejected on two counts.** The band is
  0.3–0.5 s and **0.2 s currently works**, so a gate wide enough to cover the band would refuse a swipe
  that lands today — trading a silent loss for a guaranteed refusal. And a silent refusal is the same
  reader experience as the defect: the swipe does nothing. Making it honest requires a visible
  non-interactive card state, which is D11.
- **L3 — lag the state change, not the input.** Hold the restored head article until the frame clock is
  quiet, so the card enters composition already able to hear touch. **Kept as a fallback remedy under D10,
  not as this item's plan.** It refuses nothing, is invisible to the reader, and invents no UX. Its hazard
  is that a wall-clock constant is a guess with a number on it: any deferral must be bounded by an
  observable settle signal — `scrollState.isScrollInProgress` going false, or a frame boundary — never by
  a millisecond literal chosen to sit outside a measured band. A band measured on one emulator is not a
  constant.

Recording all three rather than only the survivor, because the owner's instinct here was sound and the
measurement is what disqualified it: the input arrives too *late* to be heard in one window and early
enough in another, which is not a problem lag can solve.

## D10 — The fix is chosen by D8's output, and its shape is pre-registered per mechanism

So that slice 2 needs no fourth design pass, and so that no one improvises under time pressure:

- **If M1:** make the card's DOWN win the arbitration rather than changing scroll behaviour. The narrow
  change is `ArticleCard.kt:125`'s `requireUnconsumed` — the card's gesture is horizontal and the ancestor
  is vertical, so the card is entitled to look at a DOWN the scroll has claimed, and `SwipeGesture`'s
  existing intent slop is what prevents it from stealing a vertical drag. **RED:** wrap `ArticleCard` in a
  `verticalScroll`, start a long `animateScrollTo` with `mainClock` paused, hold it mid-flight, inject.
  A programmatic animation on the test clock is controllable, which an attach race is not — this is
  permitted by `spec.md` §5.3 and is not a retreat to the seven harnesses that failed. Suppressing the D12
  scroll on the restore path is the rejected alternative: it is item 012's ground and it removes a window
  instead of making the card able to hear through it.
- **If M2:** stabilise the card's identity across the restore so the subtree is not destroyed — a `key`
  on the state's identity rather than its class, or removing whatever intermediate state the publish
  passes through. D9's **L3** is the fallback here if identity cannot be stabilised, bounded as D9 requires.
- **If M3:** the overlay must not occupy or consume outside its own bounds. Narrow and local.
- **If M4:** whatever state survives the restore is reset when the card is rebuilt, under D6 — per-article,
  never shared.
- **If none fits:** stop and report (`spec.md` §5.5).

## D11 — No visible non-interactive card state, and therefore no honest lockout

An input gate is only honest if the reader can see that the card is not listening. `docs/v1/06-ui-ux.md`
§39 defines the swipe surface, §40 the 90 px threshold, §44 the 280 ms motion and its "tactile, quiet,
controlled" character, and §45 the 180 ms toast. None of them contemplates a card that declines touch, and
`AGENTS.md` says not to invent a requirement where an authoritative specification is silent.

**Decision:** this item adds no dim, disabled, spinner or "settling" treatment, and proposes no amendment
to add one. If a residual window survives the fix, the honest response is to record it as verification
debt with its measurement, not to dress it up as intended behaviour. `spec.md` §4's "no reader input is
refused in order to achieve this" is the scenario that holds this decision to account.
