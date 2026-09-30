# 025 — The instrumented suite does not depend on window focus

**Surface:** Android instrumented tests only. No product behaviour changes.
**Authority:** `docs/v1/09-testing-acceptance.md` §82 (*"automated deterministic tests pass"*);
`specs/definition-of-done.md` §2.2; item 021's scenario *"back still returns to Discover"*
(`specs/021-android-m3-motion/spec.md` §4).
**Branch:** `feat/025-android-instrumented-focus`, cut from `main` at `6b2682b`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 The failure

`DestinationTransitionInstrumentedTest.reducedMotionComposesDestinationAndBackResultImmediately` (item 021's
test) has failed three times in sixteen hosted `android.yml` runs, each time on code that passes unchanged:

| Run | Head | Attempt | Result |
|---|---|---|---|
| `35555180098` | `29c90cd` (022, delta `evidence.md` only) | 1 and 2 | **fail** both |
| `36657960615` | `bf34fd7` (024's merge, tree identical to passing `3a4d23c`) | 1 | **fail** |
| `36657960615` | `bf34fd7` | 2 | pass |

Done §2.2 needs three merges in a row green **without a re-run**. A gate with this failure rate cannot
supply them.

### 1.2 What the three failures have in common — read from the hosted logs

All three throw at **the same call**, `Espresso.pressBack()` at `DestinationTransitionInstrumentedTest.kt:67`:

```
androidx.test.espresso.base.RootViewPicker$RootViewWithoutFocusException:
Waited for the root of the view hierarchy to have window focus and not request layout for 10 seconds.
Root{… has-window-focus=false, layout-params-type=1 … ty=BASE_APPLICATION …}
DecorView{… has-window-focus=false, … is-layout-requested=false …}
```

All three follow an emulator cold boot (`Failed to load snapshot 'default_boot'`) with `adb: device offline`
retries. The run's artifact holds the HTML test report only; no logcat was kept.

### 1.3 Why only this test fails

**`Espresso.pressBack()` is the only Espresso call in the instrumented suite.** Every other test in the 28-test suite
drives the app with Compose test calls, which inject input into the Compose view directly and do not wait
for window focus. Espresso's root picker does wait, and gives up after ten seconds.

### 1.4 Reproduced locally, and what took the focus

On 2026-09-30, on this Mac's `Pixel_6_API_34`, freshly cold-booted, the same test failed on its first run:
same exception, same line, same root state. At that moment `dumpsys window` reported:

```
mCurrentFocus=Window{3340202 u0 Application Not Responding: com.android.systemui}
```

That is the system's own *"System UI isn't responding"* dialog, which cold-booted emulators are known to
show. **Lines 58–65 of the test (a Compose click and four asserts) passed under the same dialog**; only the
Espresso call failed. Expanding the notification shade (`cmd statusbar expand-notifications`) moves focus
the same way and reproduces the failure on demand.

**Limit, stated so it is not overclaimed:** the ANR dialog was observed locally, not in CI, because CI kept
no logcat. The match is strong (cold boot, identical exception, line and root state) but it is an
inference. The fix below does not depend on it: it removes the test's need for focus, whatever takes it.

---

## 2. Story

As the owner, I want the hosted instrumented job to give the same result for the same code, so that green
merges count toward done §2.2 without a re-run.

---

## 3. Out of scope

- Any production code (`app/src/main/**`). The app is not at fault: back works; the test cannot deliver it.
- Every other test file.
- `.github/workflows/android.yml`, including suppressing ANR dialogs or capturing logcat (`design.md` D3).
- Removing the Espresso dependency from `build.gradle.kts` (Compose's test library uses it anyway).
- The SystemUI ANR itself. It is the emulator's problem, not the app's.

---

## 4. Scenarios

### Scenario: back returns to Discover while another window holds input focus
Given reduced motion is set
And the reader is on Read Later with Settings closed
And the notification shade is expanded over the app, so the app's window does not have input focus
When back is delivered
Then the destination is Discover
And Discover's eyebrow is displayed and Read Later's does not exist
And `uiState` equals its value before navigation

### Scenario: every assertion item 021 made still holds
Given the test as item 021 wrote it
When it is changed by this item
Then each assertion at lines 58–65 and 70–73 is still made, unweakened

### Scenario: the shade does not outlive the test
Given the shade was expanded for the back step
When the test ends, passing or failing
Then the shade is collapsed and the original `animator_duration_scale` is restored

---

## 5. Verification

### 5.1 Gates

With `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
`ANDROID_HOME=$HOME/Library/Android/sdk` exported, from `android/`:
`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.
Counts stay **403 JVM / 28 instrumented**: this item changes a test, it adds none.

### 5.2 Repetition

On a **cold-booted** emulator, run the target test **20 times in a row** via `am instrument`. All 20 pass.
At least one run should happen with the natural SystemUI ANR dialog present, which is what today's cold
boot showed. Record `mCurrentFocus` for that run.

### 5.3 No focus-dependent call remains

`grep -rn "Espresso\.\|onView(" android/app/src/androidTest` returns nothing.

### 5.4 Hosted

The PR's `android.yml` passes on its **first attempt**. That alone proves little: a 3-in-16 failure passes
most single runs. The proof is §5.2's on-demand reproduction turning green. Done §2.2 itself is judged on the
next three merges to `main`, not on this PR.
