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

## D2 — The bad condition is reproduced on demand, outside the test (amended 2026-09-30)

**As approved:** the test would expand the notification shade itself just before back and collapse it in
`finally`, giving a committed RED and a permanent check.

**What happened:** the first RED attempt passed. Expanded from inside the test, the shade took focus and then
lost it within 1–6 s (2 of 2 runs), inside Espresso's 10 s wait. Expanded over the app by hand, it held focus
for 20 s; expanded **before** the test started, it failed the unchanged test 3 of 3 times. What closes it
mid-test was not identified.

**Owner's decision, 2026-09-30: fix only.** The test is not given a focus thief. The scenario is proved by a
procedure, recorded in `evidence.md`: shade expanded before the run, unchanged test fails; same condition
after the fix, passes 10 of 10 (`spec.md` §5.2). **What this gives up:** no permanent in-suite check that back
works without focus. If a focus-dependent call is ever added again, nothing catches it except CI's
occasional ANR.

**Consequence for the commit pair:** this slice has no failing test commit. Its RED is the procedure run on
the parent commit, recorded with output. That is a stated exception, not a precedent: the change is to a test,
the one attempt to produce the condition from inside it failed, and the owner chose not to investigate further.

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
