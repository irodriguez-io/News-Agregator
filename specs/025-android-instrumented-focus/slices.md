# 025 — slice plan

**One slice.** One call in one test file changes. Splitting it would leave a slice with nothing to prove.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **Back goes through `OnBackPressedDispatcher`** (`design.md` D1). Not `UiDevice`, not `dispatchKeyEvent`,
   not a retry.
2. **The shade step stays in the test** (D2). If it proves unreliable, stop and report.
3. **No assertion is removed or loosened.** Lines 58–65 and 70–73 keep every check they make today.
4. **Only `DestinationTransitionInstrumentedTest.kt` is edited.** If another file seems to need a change,
   stop.
5. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing the
   emulator back (handover: the app treats `0` as reduced motion).

---

## Slice 1: back is delivered without depending on window focus

**Objective.** `reducedMotionComposesDestinationAndBackResultImmediately` delivers back to the app's
`OnBackPressedDispatcher` and passes while another window holds input focus, with every existing assertion
intact.

- **Scenarios:** all three in `spec.md` §4.
- **Files:** `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/DestinationTransitionInstrumentedTest.kt`
  only.
- **Must not touch:** `android/app/src/main/**`, every other test file, `android/app/build.gradle.kts`,
  `android/gradle/libs.versions.toml`, `.github/workflows/**`, `pipeline/**`, `config/**`, the web runtime.
- **Hub-file edges (execution-model §2.1):** writes — this slice only; asserted by — nothing; receives —
  nothing. It is a leaf.
- **RED commit (`test(android): …`).** Expand the notification shade with the test's existing
  `runShellCommand("cmd statusbar expand-notifications")` immediately **before** `Espresso.pressBack()`
  (after line 65's assertions), and collapse it with `runShellCommand("cmd statusbar collapse")` in
  `withReducedMotion`'s `finally`. Keep `Espresso.pressBack()`. **Evidence:** the test fails locally with
  `RootViewWithoutFocusException` at the back call, and `dumpsys window | grep mCurrentFocus` names
  `NotificationShade` during the run. If it fails anywhere else, or passes, stop.
- **GREEN commit (`fix(android): …`).** Capture the dispatcher inside `setContent` via
  `LocalOnBackPressedDispatcherOwner.current` (`requireNotNull`), and replace `Espresso.pressBack()` with
  `composeTestRule.runOnUiThread { dispatcher.onBackPressed() }`. Remove the unused `Espresso` import.
  Leave `settleImmediateChange()` after it unchanged.
- **Reaches green alone because:** one test file changes; nothing asserts against it and nothing consumes
  it.
- **Definition of done:**
  - all four gates green, **403 JVM / 28 instrumented** (unchanged counts);
  - the target test passes **20 of 20** consecutive runs via `am instrument` on a **cold-booted** emulator,
    with at least one run's `mCurrentFocus` recorded (SystemUI ANR, if it appears);
  - `grep -rn "Espresso\.\|onView(" android/app/src/androidTest` returns nothing;
  - the PR's hosted `android.yml` passes on its first attempt.
- **Stop and report if:** `LocalOnBackPressedDispatcherOwner.current` is null or unavailable on the pinned
  stack; any Compose assertion fails with the shade expanded; the shade step fails any of the 20 runs; the
  RED fails anywhere other than the back call.
- **Status:** pending.

---

## Environment (this Mac)

- Export `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
  `ANDROID_HOME=$HOME/Library/Android/sdk`, or Gradle fails before any test runs.
- AVD `Pixel_6_API_34`. Cold boot: `$ANDROID_HOME/emulator/emulator -avd Pixel_6_API_34 -no-snapshot-load`.
- Single-test run: `adb shell am instrument -w -e class
  io.irodriguez.intentionalreading.DestinationTransitionInstrumentedTest#reducedMotionComposesDestinationAndBackResultImmediately
  io.irodriguez.intentionalreading.test/androidx.test.runner.AndroidJUnitRunner`, after installing both debug
  APKs with `adb install -r -t`.
- If a release build was ever installed, `adb uninstall io.irodriguez.intentionalreading` first, or
  installs fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
