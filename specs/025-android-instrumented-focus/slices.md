# 025 — slice plan

**One slice, one commit.** One call in one test file changes. Splitting it would leave a slice with nothing to prove.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **Back goes through `OnBackPressedDispatcher`** (`design.md` D1). Not `UiDevice`, not `dispatchKeyEvent`,
   not a retry.
2. **No focus thief in the test** (D2, amended: owner's decision 2026-09-30). The shade is used only in the
   §5.2 procedure, from `adb`, outside the test.
3. **No assertion is removed or loosened.** Lines 58–65 and 70–73 keep every check they make today.
4. **Only `DestinationTransitionInstrumentedTest.kt` is edited.** If another file seems to need a change,
   stop.
5. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing the
   emulator back (handover: the app treats `0` as reduced motion).

---

## ~~Slice 1: back is delivered without depending on window focus~~  ·  **done**

**Commit `b00712f`.** Slice gate PASS, 2026-09-30. 403 JVM / 28 instrumented, unchanged. Two implementer
stops, both correct: the in-test shade RED passed (led to D2's amendment), and the sandbox could not start the
emulator for the cold-boot runs, which the orchestrator ran instead.

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
- **RED — a recorded procedure, not a commit** (D2, amended). On the parent commit `HEAD` as you receive it,
  with the unchanged test: run `spec.md` §5.2's procedure (shade expanded before the run). It fails with
  `RootViewWithoutFocusException` at the back call. Record the output. If it passes, stop.
- **The one commit (`fix(android): …`).** Capture the dispatcher inside `setContent` via
  `LocalOnBackPressedDispatcherOwner.current` (`requireNotNull`), and replace `Espresso.pressBack()` with
  `composeTestRule.runOnUiThread { dispatcher.onBackPressed() }`. Remove the unused `Espresso` import.
  Leave `settleImmediateChange()` after it unchanged.
- **Reaches green alone because:** one test file changes; nothing asserts against it and nothing consumes
  it.
- **Definition of done:**
  - all four gates green, **403 JVM / 28 instrumented** (unchanged counts);
  - §5.2's procedure, after the fix: **10 of 10** pass with the shade expanded before each run;
  - §5.3: the target test passes **20 of 20** consecutive runs via `am instrument` on a **cold-booted**
    emulator, `mCurrentFocus` recorded before the first;
  - `grep -rn "Espresso\.\|onView(" android/app/src/androidTest` returns nothing;
  - the PR's hosted `android.yml` passes on its first attempt.
- **Stop and report if:** `LocalOnBackPressedDispatcherOwner.current` is null or unavailable on the pinned
  stack; any Compose assertion fails with the shade expanded; any run of §5.2 or §5.3 fails after the fix;
  the RED procedure passes, or fails anywhere other than the back call.
- **Status:** done.

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
