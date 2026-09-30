# 025 — design note

Short, because the change is one call in one test. It exists to record why this call and not the others.

## D1 — Deliver back through the app's `OnBackPressedDispatcher`, not by injecting a key

`Espresso.pressBack()` injects `KEYCODE_BACK` through the window manager, which routes it to **whichever
window has input focus**. On a cold-booted CI emulator that is sometimes SystemUI's ANR dialog, not the app.

The replacement: capture the dispatcher inside `setContent` with
`LocalOnBackPressedDispatcherOwner.current` (`androidx.activity.compose`, on the classpath through
`activity-compose` 1.13.0), then call `onBackPressedDispatcher.onBackPressed()` on the UI thread via
`composeTestRule.runOnUiThread`. The rule stays `createComposeRule()` (the `v2` import it already uses).

**What this skips, exactly:** the platform's routing of a key or gesture to the focused window, then to the
activity, then to the dispatcher. That is platform and androidx code, and it is the nondeterministic part.
**What it keeps:** every line of app code on the back path. There is one, the `BackHandler` at
`ui/IntentionalReadingApp.kt:210`, which registers with this dispatcher. Both the key path (API 34, the CI
device) and the predictive-back path (`targetSdk = 36` on API 36+ devices) end at the same dispatcher.

This is the only test in the suite that covers back (`grep BackHandler|onBackPressed` in `src/test` and
`src/androidTest` finds nothing else), so the coverage matters and is kept.

## D2 — Make the bad condition permanent in the test

The ANR dialog appears only sometimes. A fix that passes on a clean emulator proves nothing about the
condition that failed. So the test **expands the notification shade immediately before the back step**,
using the test's existing `runShellCommand` (`cmd statusbar expand-notifications`), and collapses it
(`cmd statusbar collapse`) in `finally`, alongside the existing `animator_duration_scale` restore.

That gives the item an honest RED (the unchanged `Espresso.pressBack()` fails every time with the shade
expanded) and keeps *"back works without window focus"* checked on every run from now on.

**If the shade step turns out to be unreliable itself** — any of the 20 local runs fails for a reason other
than the one being fixed — stop and report. Do not drop it quietly, and do not replace it with a different
focus thief without saying so.

## D3 — Rejected

- **Retries, `@FlakyTest`, or a rerun step.** They hide the failure and contradict §82's *deterministic*.
- **Dismissing ANR dialogs in `android.yml`** (e.g. `hide_error_dialogs`, `CLOSE_SYSTEM_DIALOGS`). Fixes CI
  only, depends on the ANR being the only focus thief, and leaves the test broken on any device that shows
  one.
- **Capturing logcat and `dumpsys window` on failure in `android.yml`.** Useful for diagnosis, but it changes
  nothing about the result. Recorded here in case a different failure appears later.
- **`UiDevice.pressBack()`.** Adds uiautomator as a dependency and still delivers the key to the focused
  window.
- **`activity.dispatchKeyEvent(KEYCODE_BACK)`.** Avoids focus too, but whether `Activity.onKeyUp` reaches the
  dispatcher depends on the platform's `OnBackInvokedCallback` mode. D1 does not.
