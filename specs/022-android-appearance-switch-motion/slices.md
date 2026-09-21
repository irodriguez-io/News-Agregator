# 022 — slice plan

Two slices, strictly sequential, sharing one item branch and one PR.

They are separable because the two halves of the defect are independent: slice 1 stops the restart, slice 2
gives the change a transition. **Slice 1 alone is shippable** and closes the owner's reported defect; slice 2
is the polish the owner chose on top of it. That ordering is deliberate — if slice 2's measurement
(`design.md` D4) forces the fallback all the way to an instant switch, slice 1 has already landed the fix.

**Amendment 10 and the two `docs/v1/**` edits land with the design commit**, before slice 1, approved at the
plan gate. Wave E's brief establishes the pattern: write the amendment once, up front, rather than letting
each slice amend.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **`android:configChanges="uiMode"` and no other key** (D1). Adding `orientation`, `screenSize`,
   `layoutDirection` or `fontScale` is out of scope and unverified.
2. **No `onConfigurationChanged` override is written** (D1). Compose handles it.
3. **`UiModeManager.setApplicationNightMode` stays, and item 010's dedupe guard stays** (D2). Removing
   either regresses a shipped feature.
4. **`ThemeDerivationTest` is not edited** (D3). If it fails, an authored value moved — stop and report.
5. **No new colour, token or seed.** Amendment 9's palette is fixed.
6. **Reduced motion is a branch inside the animation** (D7), and slice 1's fix is not conditional on it.
7. **No behaviour changes.** Destination, scroll position, undo offer, focus, swipe semantics and every
   count are untouched.
8. **No new dependency** (D8).

---

## Slice 1: the appearance change stops restarting the screen

**Objective.** Declare `uiMode` as an app-handled configuration change so that choosing an appearance
repaints in place instead of destroying and rebuilding the Activity.

- **Scenarios:** choosing a different appearance does not restart the screen; System still follows the phone
  while the app is open; selecting the appearance already in effect changes nothing; the launch frame is
  still correct on a cold start.
- **Files:** `app/src/main/AndroidManifest.xml`; instrumented tests under `app/src/androidTest/**`; a dated
  supersession note appended to `specs/010-android-launch-theme/design.md` D5 (D5 of this item).
- **Must not touch:** `ui/theme/**` (slice 2), `di/AppContainer.kt`, `ui/AppViewModel.kt`, `res/values*/**`,
  `themes.xml`, any screen or component.
- **Failing-first test:** an instrumented test asserting that with `Appearance.SYSTEM` in effect, a `uiMode`
  configuration change delivered to the running Activity leaves the resolved scheme following the system —
  and that the Activity instance survives it. This is **D6's guard** and it is the slice's acceptance
  criterion, not an extra.
- **Reaches green alone because:** the manifest attribute is self-contained and changes no Kotlin. Every
  existing unit test is untouched; the instrumented suite gains one class.
- **Definition of done:** all four gates green; the instrumented guard passing on the emulator job; item
  010's cold-start launch-frame behaviour re-verified unchanged; 010's D5 annotated.
- **Status: done.** RED `554fdee`, GREEN `5a9a3ce`, review follow-up `9dffad5`. The slice review found one
  thing: the first guard drove only the *system-toggle* path, leaving scenario 1 — the reader selecting an
  appearance, which is the path the defect was actually reported on — with no Activity-survival assertion.
  `ReaderAppearanceConfigurationInstrumentedTest` closes it by driving
  `AppViewModel.launchAppearanceChange`, the same method `IntentionalReadingApp.kt:424` calls from the
  Settings control. **Both guards were demonstrated to fail with `android:configChanges="uiMode"`
  removed**, and in the second the scheme assertions still passed while only Activity identity failed —
  which is what proves it isolates the survival property rather than passing incidentally. 385 JVM and 19
  instrumented tests green, re-run independently with `--rerun-tasks`.

---

## Slice 2: the colours cross-fade

**Objective.** Animate the resolved scheme between the palette being left and the one being entered over
300ms on M3 Standard easing, immediately under reduced motion, with both endpoints bit-identical to today's
authored values.

- **Scenarios:** the colours cross-fade rather than snap; the end states are exactly the authored palette; a
  reduced-motion preference makes the change immediate; nothing bounces, pulses or celebrates.
- **Files:** `ui/theme/Theme.kt`, `ui/theme/Tokens.kt` (a blend function only — **no authored value may
  change**), and their tests.
- **Must not touch:** `AndroidManifest.xml` (slice 1), any screen, any component, any seed, any authored
  colour.
- **Failing-first test:** a JVM test of the blend — at `0f` it equals the light tokens and derived scheme
  exactly, at `1f` the dark ones exactly, at `0.5f` every role lies strictly between — plus a test that the
  reduced-motion branch selects an immediate spec. `ThemeDerivationTest` runs unedited alongside as the
  regression guard.
- **Reaches green alone because:** the blend is a new pure function, and at its endpoints the existing theme
  is reproduced exactly, so nothing downstream observes a change except during a transition.
- **Definition of done:** all four gates green; `ThemeDerivationTest` passing **unedited**; the blend
  unit-tested at both endpoints and an interior point; the reduced-motion branch asserted; **and the
  release-build `gfxinfo` measurement from `design.md` D4 recorded in `evidence.md`** — with the fallback
  taken and recorded if the measurement demands it.
- **Status: done.** RED `a61e302`, corrected RED `0975ea2`, GREEN `440d4f1`, owner decision `342e925`,
  review follow-up `7ed8d07` + `82bc5e1`. 391 JVM and 20 instrumented tests green, re-run independently
  with `--rerun-tasks`; `ThemeDerivationTest` and all 55 pre-existing test files byte-identical.

  **Three things went wrong here and all three were design errors of mine, caught by the implementer
  refusing to paper over them.** Worth reading before the next motion item:

  1. **D3 offered two blend shapes as equivalent and they are not** (corrected in `7cc1e17`). `surfaceTint`
     is the one role computed from different source tokens in each scheme, so blending tokens *then*
     deriving pulls near-white `light.bg` into the tint mid-fade and overshoots both endpoints. Deriving
     both schemes and blending *those* makes every role monotonic by construction.
  2. **The RED tests then encoded the rejected shape**, so the corrected design could not compile against
     them. Resolved by explicitly authorising two scoped test edits — the removed call sites and the one
     assertion demanding the rejected calculation — while requiring the endpoint-bounds test to survive
     and get stricter. `a61e302` was kept in history; `0975ea2` supersedes it and was verified red first.
  3. **D4's ladder optimised a number without weighing what it looked like.** Rung 2 measured best on warm
     runs but splits the transition: **17 files read `LocalIntentionalReadingTokens` directly**, so the
     card, bottom bar, chips and Settings sheet would hard-cut at 300ms behind a fading backdrop. The
     owner chose rung 1 on 2026-09-20 — whole-surface fade, dynamic local. Rung 1 and rung 2 were
     statistically indistinguishable on first-switch anyway (9/72 against 10/74).

  **The fade's residual cost is first-use only** and is characterised in `evidence.md`: 12.50% → 5.13% →
  2.56% janky across successive switches, worst frame 48 → 31 → 21 ms, on an unoptimised release build.
  **R8 and a baseline profile are both still disabled and are the untried levers**; neither is in scope
  here. Do not reopen the fade decision on the 12.5% figure alone.

  The slice review found one thing: `Theme.kt` resolved `reducedMotion` by casting `LocalContext` to the
  Application and reading the DI container, which inverted the layering, made §79.3's required assertion
  impossible to write, and silently made six existing instrumented layout tests depend on the emulator's
  `animator_duration_scale` — `execution-model.md` §8.3 again. Fixed by injecting
  `reducedMotion: () -> Boolean = { false }`, matching `SettingsSheet.kt:78`, `DiscoverScreen.kt:52` and
  `ArticleCard.kt:69` and item 010's D4. The new guard establishes its own value and passes at animation
  scale both `1.0` and `0`.

---

## Walkthrough

Owner-driven, after both slices, per `spec.md` §6.3. It carries the taste judgement that no gate can settle:
whether 300ms reads as deliberate or as sluggish.
