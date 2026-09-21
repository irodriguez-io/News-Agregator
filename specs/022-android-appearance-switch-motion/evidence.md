# Item 022 — Slice 2 evidence

Measured 2026-09-20 in `news-agregator-022`, branch `feat/022-android-appearance-switch-motion`.

## Result and owner decision

**Rung 1 is the retained candidate by owner decision on 2026-09-20.** The token local is a dynamic
`compositionLocalOf`; both all 26 tokens and all 48 derived `ColorScheme` roles fade across the same
progress float over 300ms on Material 3 Standard easing. Each endpoint scheme is derived with the unchanged
Boolean factory before blending. Reduced motion selects `snap()` and uses the resolved endpoint in the
same composition. Initial composition starts at the selected palette.

The owner rejected rung 2 because 17 production files read `LocalIntentionalReadingTokens.current`
directly. Keeping those readers at the old palette until the end makes most of the surface snap after a
300ms delay while Material surfaces fade. That split transition costs the whole-surface cross-fade, and
the first-switch figures (rung 1: 9/72; rung 2: 10/74) do not establish a performance benefit for it.
All three measurement records below remain intact.

The owner accepted the fade on the characterisation of rung 1's cost as **first-use dominated**: across
its first three switches jank falls **12.50% → 5.13% → 2.56%**, and the recorded frame-time tail falls
**48ms → 31ms → 21ms**. Those times are the captured `gfxinfo` p99 values, rather than independently
measured absolute maxima. Later warm misses remain in the per-run table; this is not a zero-jank claim.
R8 remains disabled (`isMinifyEnabled = false`) and an app-specific baseline profile remains disabled/not
configured. Both are untried levers against the first-use cost and remain outside this item's scope.
The D4 rung 3 decision is resolved in favour of retaining the full fade. **Rung 4 was not taken.**

## RED history and test authority

- Original RED remains `a61e30231de88a737610a830d5b296e396eece43`.
- Owner's D3 correction is `7cc1e1795c7a64dfa3388ddbbf606f1f429e9177`.
- Authorised corrected RED is `0975ea2aeb2c9a1e6c2ada203e0f03411397acd7`.

Only `AppearanceTransitionTest.kt` was corrected: removed fractional-factory calls now derive endpoint
schemes and blend them; the tint expectation now blends the two derived endpoint tints. The original
strict midpoint bounds remain. Additional bounds cover all four Oklab components of all 48 roles at
101 fractions in both directions. Token endpoint/interior and normal/reduced-motion spec assertions remain.
All 55 pre-existing Kotlin test files remain byte-identical to `b345a5a`, including `ThemeDerivationTest`.

For corrected RED, the uncommitted implementation was copied outside the checkout and both production
files were restored to committed HEAD. `:app:testDebugUnitTest` then failed on the absent implementation:

```text
> Task :app:compileDebugUnitTestKotlin FAILED
Unresolved reference 'blendTokens'.
Unresolved reference 'blendColorSchemes'.
Unresolved reference 'appearanceTransitionSpec'.
BUILD FAILED in 1s
```

This is a missing-API compilation RED, not a claim that JVM assertions executed. Production files were
unchanged in the corrected-test commit; implementation was restored only after that commit.

## Gate execution

Both environment variables were exported for every Gradle invocation:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

The earlier rung 2 JVM/build command completed (historical gate record):

```text
> Task :app:assembleDebug
> Task :app:assembleDebugAndroidTest
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1s
72 actionable tasks: 9 executed, 63 up-to-date
```

391 JVM tests, zero failures/errors/skips; all six appearance-transition tests pass.
`ThemeDerivationTest`: 8 tests, zero failures, file unedited.

The earlier rung 2 device gate completed (historical gate record):

```text
Starting 19 tests on Pixel_10(AVD) - 17
Finished 19 tests on Pixel_10(AVD) - 17
BUILD SUCCESSFUL in 46s
68 actionable tasks: 1 executed, 67 up-to-date
```

19 tests, zero failures/errors/skips, including:

- `AppearanceConfigurationInstrumentedTest.systemStillFollowsThePhoneWhileTheAppIsOpenWithoutRecreatingTheActivity`
- `ReaderAppearanceConfigurationInstrumentedTest.choosingADifferentAppearanceDoesNotRestartTheScreen`

### Retained rung 1 gates after owner decision

All four gates were rerun on the restored rung 1 source, with both required environment exports:

```text
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
> Task :app:assembleDebug
> Task :app:assembleDebugAndroidTest UP-TO-DATE
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 1s
72 actionable tasks: 9 executed, 63 up-to-date

./gradlew :app:connectedDebugAndroidTest
Starting 19 tests on Pixel_10(AVD) - 17
Finished 19 tests on Pixel_10(AVD) - 17
BUILD SUCCESSFUL in 52s
68 actionable tasks: 1 executed, 67 up-to-date
```

391 JVM tests and 19 instrumented tests, zero failures/errors/skips. Both slice 1 appearance guards pass;
`ThemeDerivationTest` passes unedited. All 55 pre-existing test files are byte-identical to the original
baseline, and `AppearanceTransitionTest.kt` is byte-identical to corrected RED `0975ea2`. No test changed
for this owner decision. All three device animation scales remain `1.0`.

Logs: `/tmp/022-retained-rung1-gates.log`, `/tmp/022-retained-rung1-connected.log`, and
`/tmp/022-retained-rung1-release.log`.

These are local Pixel_10/API 37 results, not hosted CI evidence. No push, PR, merge, or deployment was made.

## Retained build at `342e925` matched the earlier rung 1 measurement

The owner-selected source restores the exact `Theme.kt` used for the earlier rung 1 measurement; the
dynamic `Tokens.kt` is unchanged from that measured variant. `:app:assembleRelease` was rerun after the
restoration (`BUILD SUCCESSFUL in 3s`, 47 tasks: 8 executed, 39 up-to-date).

The rebuilt unsigned release APK was compared entry by entry with `/tmp/022-rung1-release.apk`:
**all 129 APK content entries are byte-identical**, including every DEX, resource, manifest and native
library. The only extra entries in the old signed APK are `META-INF/ANDROIDD.RSA`, `META-INF/ANDROIDD.SF`
and `META-INF/MANIFEST.MF`, its signing metadata. No runtime/build-content difference exists.
**This comparison established equivalence for `342e925`; `gfxinfo` was not rerun.**
The later preference-injection review fix below preserves production behaviour and leaves these figures unchanged.

- Earlier measured, signed APK SHA-256: `d3d1dd38a82818147ae6346ffb74ed383db3c4d12ed54331c4468e8f5def03c0`.
- Rebuilt unsigned APK SHA-256: `9042a647963bfc55c6c1af11c0056bdf1754880a3034a1a5f52aa106a28a0139`.
- Local entry comparison: `/tmp/022-retained-rung1-apk-comparison.json`.

## Release measurement method

- Device: `Pixel_10`, `emulator-5554`, Android API 37; 1080×2424, density 420, 60 Hz; Skia/OpenGL.
- All three animation scales were verified at `1.0` before normal-motion measurements.
- Built each candidate with `./gradlew :app:assembleRelease`.
- This worktree has no untracked `android/keystore.properties`, contrary to the brief's environment
  assumption. The generated release APK was signed with the existing local development key using SDK
  `apksigner`, without changing Gradle configuration. Package flags confirmed no `DEBUGGABLE` flag.
  The measured APK is a release build, despite the development signing identity.
- Installed only the APK using `adb -s emulator-5554 install -r /tmp/022-<candidate>-release.apk`.
  No separate generated dex-metadata/profile sidecar was installed. No compilation optimisation was requested.
- Cold-launched the app, waited for its populated Discover surface, opened Settings and waited for the
  sheet reveal to finish. Each sequence begins in dark appearance. Twelve alternating switches follow,
  starting Dark → Light. Each candidate was reinstalled and relaunched before its sequence.
- Before **each** switch: `adb shell dumpsys gfxinfo io.irodriguez.intentionalreading reset`.
- Tap the visible Light or Dark row through `adb shell input tap`, wait 0.85s after the input command
  returns, then capture `adb shell dumpsys gfxinfo io.irodriguez.intentionalreading framestats`.
- Static/rung 1 taps: `(160,1190)` / `(160,1320)`. Rung 2 taps: `(160,1220)` / `(160,1350)`.
  Both pairs are inside the same normal-motion rows, identified with `adb exec-out screencap -p`.
- No screenshots, Gradle builds, or instrumented suites ran during these three normal-motion sequences.
- Counts include both app windows, control/ripple interaction and configuration handling, not just the
  300ms colour interpolation. Percentages alone cannot isolate recomposition cost. First-use and warm
  results are therefore shown separately; they are not averaged into an asserted fade-only latency.

## Results

“Warm” below means runs 3–12, excluding both initial directions; all twelve runs are retained below.

| Candidate | First switch: janky/frames | First p95 | Warm janky/frames | Warm p95 range |
| --- | ---: | ---: | ---: | ---: |
| Original static token local | 14/65 (21.54%) | 57ms | 22/782 (2.81%) | 19–25 ms |
| Rung 1: dynamic local, tokens fade | 9/72 (12.50%) | 48ms | 24/781 (3.07%) | 18–25 ms |
| Rung 2: scheme fade, tokens settle | 10/74 (13.51%) | 46ms | 20/822 (2.43%) | 19–22 ms |

The static run's first switch and later misses triggered measurement of rung 1 and then rung 2 in order.
Rung 2's warm aggregate was numerically lower, but the short sequential runs and unequal launch-to-first-switch
delays do not establish statistical superiority. At the D4 rung 3 checkpoint, the owner rejected its visual
cost and selected rung 1 on 2026-09-20. Rung 2 is preserved here as a rejected measured candidate.

| Candidate | Run | Direction | Frames | Janky | p50 | p90 | p95 | p99 | Missed vsync | Slow UI |
| --- | ---: | --- | ---: | --- | --- | --- | --- | --- | ---: | ---: |
| static | 1 | dark-to-light | 65 | 14 (21.54%) | 21ms | 48ms | 57ms | 97ms | 6 | 11 |
| static | 2 | light-to-dark | 76 | 5 (6.58%) | 17ms | 30ms | 31ms | 48ms | 0 | 3 |
| static | 3 | dark-to-light | 78 | 2 (2.56%) | 17ms | 18ms | 19ms | 20ms | 0 | 0 |
| static | 4 | light-to-dark | 78 | 2 (2.56%) | 17ms | 19ms | 20ms | 21ms | 0 | 0 |
| static | 5 | dark-to-light | 78 | 2 (2.56%) | 17ms | 19ms | 21ms | 22ms | 0 | 0 |
| static | 6 | light-to-dark | 78 | 2 (2.56%) | 17ms | 18ms | 19ms | 20ms | 0 | 0 |
| static | 7 | dark-to-light | 78 | 2 (2.56%) | 17ms | 20ms | 20ms | 21ms | 0 | 0 |
| static | 8 | light-to-dark | 78 | 2 (2.56%) | 17ms | 19ms | 20ms | 23ms | 0 | 0 |
| static | 9 | dark-to-light | 79 | 2 (2.53%) | 17ms | 18ms | 19ms | 20ms | 0 | 0 |
| static | 10 | light-to-dark | 77 | 4 (5.19%) | 18ms | 22ms | 25ms | 32ms | 0 | 0 |
| static | 11 | dark-to-light | 80 | 2 (2.50%) | 17ms | 18ms | 19ms | 21ms | 0 | 0 |
| static | 12 | light-to-dark | 78 | 2 (2.56%) | 17ms | 18ms | 19ms | 20ms | 0 | 0 |
| rung1 | 1 | dark-to-light | 72 | 9 (12.50%) | 17ms | 32ms | 48ms | 48ms | 1 | 4 |
| rung1 | 2 | light-to-dark | 78 | 4 (5.13%) | 17ms | 28ms | 30ms | 31ms | 0 | 1 |
| rung1 | 3 | dark-to-light | 78 | 2 (2.56%) | 17ms | 18ms | 19ms | 21ms | 0 | 0 |
| rung1 | 4 | light-to-dark | 79 | 2 (2.53%) | 17ms | 19ms | 20ms | 23ms | 0 | 0 |
| rung1 | 5 | dark-to-light | 78 | 2 (2.56%) | 17ms | 19ms | 20ms | 21ms | 0 | 0 |
| rung1 | 6 | light-to-dark | 78 | 2 (2.56%) | 17ms | 17ms | 18ms | 20ms | 0 | 0 |
| rung1 | 7 | dark-to-light | 78 | 2 (2.56%) | 17ms | 19ms | 20ms | 21ms | 0 | 0 |
| rung1 | 8 | light-to-dark | 77 | 4 (5.19%) | 17ms | 23ms | 25ms | 32ms | 0 | 0 |
| rung1 | 9 | dark-to-light | 79 | 2 (2.53%) | 17ms | 19ms | 19ms | 21ms | 0 | 0 |
| rung1 | 10 | light-to-dark | 77 | 4 (5.19%) | 18ms | 24ms | 25ms | 38ms | 0 | 0 |
| rung1 | 11 | dark-to-light | 79 | 2 (2.53%) | 17ms | 19ms | 20ms | 23ms | 0 | 0 |
| rung1 | 12 | light-to-dark | 78 | 2 (2.56%) | 17ms | 19ms | 20ms | 21ms | 0 | 0 |
| rung2 | 1 | dark-to-light | 74 | 10 (13.51%) | 20ms | 38ms | 46ms | 53ms | 0 | 7 |
| rung2 | 2 | light-to-dark | 81 | 4 (4.94%) | 17ms | 19ms | 20ms | 34ms | 0 | 2 |
| rung2 | 3 | dark-to-light | 82 | 2 (2.44%) | 17ms | 18ms | 19ms | 20ms | 0 | 0 |
| rung2 | 4 | light-to-dark | 82 | 2 (2.44%) | 17ms | 20ms | 20ms | 22ms | 0 | 0 |
| rung2 | 5 | dark-to-light | 82 | 2 (2.44%) | 17ms | 19ms | 20ms | 21ms | 0 | 0 |
| rung2 | 6 | light-to-dark | 82 | 2 (2.44%) | 17ms | 19ms | 20ms | 21ms | 0 | 0 |
| rung2 | 7 | dark-to-light | 82 | 2 (2.44%) | 17ms | 19ms | 22ms | 23ms | 0 | 1 |
| rung2 | 8 | light-to-dark | 82 | 2 (2.44%) | 17ms | 19ms | 21ms | 23ms | 0 | 0 |
| rung2 | 9 | dark-to-light | 82 | 2 (2.44%) | 17ms | 20ms | 20ms | 22ms | 0 | 0 |
| rung2 | 10 | light-to-dark | 82 | 2 (2.44%) | 17ms | 20ms | 20ms | 21ms | 0 | 0 |
| rung2 | 11 | dark-to-light | 83 | 2 (2.41%) | 17ms | 20ms | 21ms | 23ms | 0 | 1 |
| rung2 | 12 | light-to-dark | 83 | 2 (2.41%) | 17ms | 19ms | 19ms | 21ms | 0 | 0 |

A reduced-motion diagnostic on rung 1 produced 7–8 frames per completed switch, versus roughly 78 with
normal motion. It also reported initial-window deadline misses, so the normal-motion aggregate should not
be equated directly with colour-fade jank. This is a diagnostic, not an acceptance threshold: its first
runs overlapped a build, and reduced motion changes the existing sheet insets/position. An earlier diagnostic
using the original coordinates missed alternate controls and was discarded. The animation scale was restored
to `1.0`, with transition/window scales also `1.0` and the original system night mode still `yes`.

## Implementation decisions

- Oklab interiors preserve precision for almost identical endpoints such as tertiary. At `0f` and `1f`,
  both blend helpers return their input objects, avoiding any endpoint colour-space round trip. The tint
  equality assertion compares in the endpoint colour space; the bounds check uses the higher-precision
  Oklab components directly.
- The endpoint scheme factory is byte-identical to its pre-slice version. No seed, authored palette,
  dependency, manifest, screen, component, ViewModel, container, resource, pipeline or web file changed.
- Following the slice 2 review, the theme accepts `reducedMotion: () -> Boolean = { false }` and
  `IntentionalReadingApp` passes its existing callback. The theme has no Context/Application/DI lookup;
  direct test hosts use the deterministic default. The production callback and its interpretation are unchanged.
- A fixed light/dark progress axis gives cold starts the selected endpoint and lets an interrupted switch
  reverse from its current colour. No second content tree, layout animation, spring, pulse, or bounce is added.
- The retained rung 1 animates direct token readers and Material roles together. Rung 2's delayed token
  settlement was removed by owner decision; no delayed snap remains in the theme.
- Release endpoint screenshots were captured at `/tmp/022-rung2-dark.png` and `/tmp/022-rung2-light.png`.
  These historical rung 2 screenshots establish endpoint colours and Settings layout, not the retained
  candidate's motion or subjective smoothness. The
  existing instrumented guards establish System-following and Activity survival.
- Raw per-switch reports and JSON summaries remain locally in `/tmp/022-gfxinfo/{static,rung1,rung2}/`;
  the reproducer is `/tmp/022-measure.py`. Build/test logs are `/tmp/022-corrected-red.log`,
  `/tmp/022-final-green-gate.log`, `/tmp/022-final-connected.log` and `/tmp/022-rung2-release.log`.

Measured, locally signed release APK SHA-256 values:

- `022-static-release.apk`: `707a395605ec5f5f55147fca8d0f511af172df0a56f455956102f5d149fd8830`
- `022-rung1-release.apk`: `d3d1dd38a82818147ae6346ffb74ed383db3c4d12ed54331c4468e8f5def03c0`
- `022-rung2-release.apk`: `632ecf481b8762746c89d9fcb1b144ec92bf075446c55b090729a52260ceb739`

## Slice 2 review follow-up — inject the reduced-motion preference

The theme now takes `reducedMotion: () -> Boolean = { false }`, matching existing component signatures.
`IntentionalReadingApp` supplies the same callback already resolved in its scope. Each theme composition
invokes that callback to select the spec and the immediate endpoint; it no longer reaches through Context
or Application into DI. The production flag source and runtime meaning are unchanged. Blend helpers,
Boolean scheme factory, fixed light-to-dark axis and rung 1 dynamic local are unchanged. No release
measurement was repeated and no recorded measurement figure was edited for this wiring change.

The new `ThemeReducedMotionInstrumentedTest` exercises the actual theme composition rather than only the
pure spec selector. Instrumentation is used because the existing plain JVM dependencies do not host an
Android Compose UI/animation clock. No dependency was added. The test supplies a mutable fake callback,
fixes `MotionDurationScale` at 1 in its test effect context, and manually controls the frame clock. It
checks both directions, immediate endpoints when the fake returns true, intermediate token and scheme
values when it returns false, and a false → true → false sequence in one composition. It inherits neither
the Application callback nor the emulator animation scale.

RED test commit: `7ed8d07f3c6b0978306bdc4d51e7a48394135248`.

```text
> Task :app:compileDebugAndroidTestKotlin FAILED
ThemeReducedMotionInstrumentedTest.kt:43:17 No parameter with name 'reducedMotion' found.
BUILD FAILED in 1s
```

After adding the injectable API, the flag wire was temporarily disconnected with
`val reducedMotionEnabled = false`, leaving the committed test unchanged. This proved the behavioural
assertion catches an ignored callback:

```text
java.lang.AssertionError: Injected reduced motion must select the endpoint immediately
> Task :app:connectedDebugAndroidTest FAILED
BUILD FAILED in 13s
```

The real callback invocation was then restored. All pre-existing test files remain byte-identical,
including the original 55 and the corrected `AppearanceTransitionTest`. No assertion was changed to pass.

All four gates passed with the required Java/Android environment exports:

```text
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
BUILD SUCCESSFUL in 1s
72 actionable tasks: 8 executed, 64 up-to-date

./gradlew :app:connectedDebugAndroidTest
Starting 20 tests on Pixel_10(AVD) - 17
Finished 20 tests on Pixel_10(AVD) - 17
BUILD SUCCESSFUL in 48s
68 actionable tasks: 1 executed, 67 up-to-date
```

391 JVM tests and 20 instrumented tests, zero failures/errors/skips. The device count is the original 19
plus the new wiring guard; both slice 1 guards remain green. `ThemeDerivationTest` remains unedited and
passing. The full instrumented XML is retained locally at `/tmp/022-injection-full-suite.xml`.

The new guard was also run alone with the device's `animator_duration_scale` explicitly set to `0`:

```text
Starting 1 tests on Pixel_10(AVD) - 17
Finished 1 tests on Pixel_10(AVD) - 17
BUILD SUCCESSFUL in 11s
```

It still passed, including its normal-motion intermediate-palette assertions. The test's injected flag and
effect-context clock scale therefore do not inherit that device setting. The original device scale `1.0`
was restored in `finally`. The diagnostic log is `/tmp/022-injection-scale-zero.log`.

Local logs: `/tmp/022-injection-red.log`, `/tmp/022-injection-disconnected-red.log`,
`/tmp/022-injection-green-gates.log`, `/tmp/022-injection-green-connected.log`.
