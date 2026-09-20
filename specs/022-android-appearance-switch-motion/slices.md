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

---

## Walkthrough

Owner-driven, after both slices, per `spec.md` §6.3. It carries the taste judgement that no gate can settle:
whether 300ms reads as deliberate or as sluggish.
