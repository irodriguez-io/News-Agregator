# 025 — The instrumented suite does not depend on window focus · evidence

**Branch:** `feat/025-android-instrumented-focus`, from `main` at `6b2682b`. **Product code changed:** none.
**Diff:** one test file, 5 insertions, 2 deletions (`b00712f`).

---

## 0. What the design pass found

All three hosted failures (`35555180098` attempts 1–2 on `29c90cd`; `36657960615` attempt 1 on `bf34fd7`)
threw `RootViewWithoutFocusException` at the same call: `Espresso.pressBack()`,
`DestinationTransitionInstrumentedTest.kt:67`. **That was the only Espresso call in the instrumented suite**,
which is why no other test ever failed this way. The other 27 drive the app with Compose test calls, which do
not wait for window focus.

On a cold-booted local `Pixel_6_API_34`, the unchanged test failed the same way on its first run, with
`mCurrentFocus=Window{… Application Not Responding: com.android.systemui}`: SystemUI's own ANR dialog.
**CI kept no logcat, so that this dialog is what took focus on CI is inferred, not captured.** The fix does
not depend on it.

## 1. The fix

`b00712f` delivers back with `OnBackPressedDispatcher.onBackPressed()`, captured inside `setContent` from
`LocalOnBackPressedDispatcherOwner.current`, instead of `Espresso.pressBack()`. Nothing in `app/src/main`
overrides that composition local, so it is the dispatcher the `BackHandler` at
`ui/IntentionalReadingApp.kt:210` registers with. Every assertion 021 wrote is unchanged. The test still
catches a broken `BackHandler`: without one, back would finish the activity and the destination assertion
would fail.

## 2. RED — a recorded procedure, not a commit (`design.md` D2, amended)

**The approved RED did not work.** Expanding the notification shade from inside the test, just before back,
was tried first (2 of 2 runs): the shade took focus, lost it within 1–6 s, inside Espresso's 10 s wait, and
the unchanged test passed. The implementer stopped, as the brief required. What closes the shade mid-test was
not identified. **Owner's decision, 2026-09-30: fix only** — no focus thief in the test, RED as a procedure.

Procedure (`spec.md` §5.2): `cmd statusbar expand-notifications`, wait 2 s, confirm focus, run the test,
collapse.

| Tree | Focus before the run | Result |
|---|---|---|
| parent (`a7de751`), unchanged test | `NotificationShade` | **fail** 3 of 3 (design pass) + 1 of 1 (implementer): `RootViewWithoutFocusException` at `:67` |
| `b00712f` | `NotificationShade` | **pass** 10 of 10 (implementer) + 3 of 3 (orchestrator) |

**What this gives up:** the suite has no permanent check that back works without focus. If a
focus-dependent call is added again, only CI's occasional ANR will catch it.

## 3. Gates and repetition — run by the orchestrator on `b00712f`

| Check | Result |
|---|---|
| `:app:testDebugUnitTest` (`--rerun-tasks`) | **403 tests, 0 failures, 0 skipped** (unchanged) |
| `:app:assembleDebug`, `:app:assembleDebugAndroidTest` | pass |
| `:app:connectedDebugAndroidTest` | **28 tests, 0 failures** (unchanged), `Pixel_6_API_34` |
| Target test, 20 consecutive runs after a cold boot (`-no-snapshot-load`) | **20 of 20 pass**. Focus before the first: the launcher; the ANR dialog did not appear on this boot. |
| `grep -rn "Espresso\.\|onView(" android/app/src/androidTest` | no matches |

The implementer could not start the emulator from its sandbox for the cold-boot runs and stopped there,
correctly. The orchestrator ran §5.3 and the gates instead.

## 4. Hosted

*Filled in at the PR.*

## 5. Outstanding at close

- **Done §2.2** is judged on the next three merges to `main` without a re-run, not on this PR.
- **If the flake recurs with a different signature**, `design.md` D3 records the next lever: capture logcat
  and `dumpsys window` on failure in `android.yml`.
