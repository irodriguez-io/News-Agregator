# 022 — design note

A design note is warranted: this item changes a manifest declaration that governs Activity lifecycle, it
revisits a decision another item recorded and declined, and it authors motion that `docs/v1/**` is silent
about. D1 and D5 are durable decisions and become ADRs at ship time.

---

## D1 — `android:configChanges="uiMode"`, and that key alone

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:configChanges="uiMode">
```

This tells the platform that the app handles `uiMode` changes itself, so a night-mode change is delivered as
`onConfigurationChanged` instead of destroying and rebuilding the Activity. Compose observes the new
`Configuration` and recomposes; nothing else is required of the app, and **no `onConfigurationChanged`
override is written**.

**Only `uiMode`.** Not `orientation`, not `screenSize`, not `layoutDirection`, not `fontScale`. Every other
configuration key keeps today's behaviour. Widening this attribute is the standard way this fix goes wrong —
each added key silently transfers responsibility for that configuration from the platform to the app, and
this item has verified the safety argument for exactly one of them (`spec.md` §1.4).

**Why this is safe here and would not be safe in a typical app.** The safety rests entirely on the app
reading no night-qualified resource at runtime, which §1.4 re-verified against merged `main`. Amendment 9
makes it structural: no component outside the theme package may name a colour, so there is no path by which a
component could come to depend on a `values-night` lookup without violating the amendment first.

**The cost, paid deliberately.** The system theme toggle path stops being handled by a restart and starts
being handled by recomposition. That is D5's stated objection and the reason it declined. It is covered by an
instrumented test (D6), which is the thing that did not exist when D5 was written.

## D2 — `setApplicationNightMode` stays, and item 010's mechanism is untouched

The call at `di/AppContainer.kt:26` is what makes the **launch frame** correct on the next cold start — the
platform persists the app's night mode and resolves `values-night/colors.xml` before the process exists. That
is item 010's entire deliverable and removing the call would regress a shipped feature to fix a different
one.

D1 does not remove the call. It declines the Activity rebuild the call triggers. Both effects of
`setApplicationNightMode` — persistence for the launch frame, and a configuration change — are still
produced; only the app's reaction to the second changes.

Item 010's **D2 dedupe guard also stays**: the process-scoped field in `AppViewModel` that suppresses a
platform call when the mode is unchanged. It is what makes `spec.md`'s "selecting the appearance already in
effect changes nothing" scenario true, and it is unaffected by this item.

## D3 — The cross-fade is one progress float across the token set, not thirty animations

`ui/theme/Theme.kt` resolves `IntentionalReadingTokens` (26 `Color` fields) and derives the whole
`ColorScheme` from it. The fade therefore has exactly one natural seam.

```
progress : Float   0f = the scheme being left, 1f = the scheme being entered
tokens   = lerp(from, to, progress)          // 26 colour lerps, one function
scheme   = intentionalReadingColorScheme(tokens, darkFraction = progress-resolved)
```

Driven by a single `animateFloatAsState` with `tween(300, easing = <M3 Standard>)`.

**`surfaceTint` is the trap.** `intentionalReadingColorScheme` currently takes `darkTheme: Boolean` and uses
it in exactly one place — `surfaceTint = (if (darkTheme) tokens.bg else tokens.tertiary).copy(alpha = 0.10f)`.
A boolean flips at some instant during the fade, so that one role would snap while the other 40 travel. The
factory must take a fraction and lerp both variants, or the two derived schemes must be lerped whole. Either
is acceptable; the implementer picks one and tests the endpoints.

**The endpoint guarantee is the regression guard.** At `0f` and `1f` the output must be bit-identical to
today's, which is why `ThemeDerivationTest` must keep passing **unedited**. An implementer who finds
themselves editing that test has changed an authored value and must stop and report.

**Why not `Crossfade`, and why not per-role `animateColorAsState`.** `Crossfade` composes both trees at once,
which would duplicate every screen's state and input handling for 300ms — unacceptable while a swipe or an
undo offer may be live. Per-role `animateColorAsState` means ~40 independent animations that can desynchronise
and cannot be tested as one function.

## D4 — The recomposition cost is measured, and the fallback is pre-authorised

`LocalIntentionalReadingTokens` is a `staticCompositionLocalOf`. Static locals do not track reads: changing
one recomposes the entire content subtree rather than only the readers. A 300ms fade at 60 Hz is roughly
eighteen whole-subtree recompositions in a debug-built Compose tree.

**Measure before accepting.** `dumpsys gfxinfo` across the transition on a **release** build, per
`spec.md` §6.3.6. Release, because Compose debug builds are not representative — Google's guidance is
explicit and it already cost this project a misdiagnosis this session.

**Pre-authorised fallback, so a bad measurement needs no new design round.** In order:

1. Change `LocalIntentionalReadingTokens` from `staticCompositionLocalOf` to `compositionLocalOf`, so only
   actual readers recompose. This is a one-word change with a real cost — a dynamic local is slower to read —
   and it is the correct first move if the fade janks.
2. If that is still not enough, **reduce the fade to the `ColorScheme` only** and let the token local settle
   at the end of the animation. Most of the visible surface is Material roles.
3. If neither works, **take the instant switch** — the owner's rejected option — record it in `evidence.md`
   as a taken fallback with the measurement that forced it, and ship the flash fix alone. The defect in
   `spec.md` §1.1 is closed either way; only §1.5's polish is lost.

Recording a taken fallback is not a defect. Shipping a fade that judders would be.

## D5 — This supersedes item 010's D5, and says so in that file

`specs/010-android-launch-theme/design.md` D5 declined this attribute and invited its reconsideration. The
project's convention is that a superseded decision is annotated where it lives rather than left to be
rediscovered — the precedent is `execution-model.md` §2.1 and §8.1, both corrected in place.

This item appends a dated note to 010's D5 recording that the attribute was adopted by 022, why, and that
D5's safety argument was re-verified rather than assumed. It does not rewrite D5's reasoning, which was
correct for its moment.

## D6 — The system-toggle guard is instrumented, not manual

The one genuinely new risk is that `Appearance.SYSTEM` stops following the phone once the restart is gone.
It is invisible to every JVM test, because it turns on whether the platform delivers a configuration change
to a live Activity and whether Compose propagates it.

Item 018 put `connectedDebugAndroidTest` in CI (`execution-model.md` §8.2), so this is now gateable. The test
drives a `uiMode` configuration change against a running Activity with `Appearance.SYSTEM` in effect and
asserts the resolved scheme followed.

**This is the item's acceptance criterion, not a nice-to-have.** Without it the item trades a visible defect
for an invisible one, which is the trade `backlog.md`'s verification-debt section exists to prevent.

## D7 — Reduced motion is a branch inside the animation, matching 021

`reducedMotion` is already resolved and threaded (`di/AppContainer.kt:30`, `ui/IntentionalReadingApp.kt:102`).
Item 021 established the shape: the spec is selected by the flag, `snap()` under reduced motion, and a test
asserts it (`06-ui-ux.md` §79.3 — *"A test must assert that each animation honours it"*).

Under reduced motion the scheme changes immediately **and still does not flash**, because D1 is a manifest
declaration and is not conditional on any preference. The two halves of this item are independent, which is
also why they are separate slices.

## D8 — No new dependency

Everything used here — `animateFloatAsState`, `tween`, `androidx.compose.ui.graphics.lerp(Color, Color, Float)`,
the M3 easing set — is already on the classpath. `AGENTS.md`'s dependency-approval rule is not engaged.
