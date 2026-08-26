# 010 — Launch theme — evidence

**Branch:** `feat/010-android-launch-theme` → `main`\
**Wave:** A (`specs/waves/wave-a.md`), second in the merge order, rebased onto merged `main` after 011\
**Implementer:** Codex, two sessions (initial slice, then one findings follow-up)\
**Reviewer:** Claude, this session — authored the spec, design note, slice plan and this file, and
wrote no product or test code

---

## Commit chain

| Commit | Kind | Contents |
|---|---|---|
| `29c9d80` | `docs(spec)` | `spec.md`, `design.md`, `slices.md` |
| `4c033da` | RED | `LaunchNightModeTest.kt`, `LaunchBackgroundTest.kt` |
| `e0a6196` | GREEN | colours, themes, `AppContainer`, `AppViewModel`, `MainActivity` |
| `76aace8` | RED (follow-up) | `LaunchThemeInheritanceTest.kt` |
| `e04577f` | GREEN (follow-up) | base-style extraction |

SHAs are post-rebase. One slice, one findings round.

## Gates

Reproduced by the reviewer with `--rerun-tasks` in a throwaway detached worktree, not read from the
implementer's report.

**RED at `4c033da`** — fails at `:app:compileDebugUnitTestKotlin` with unresolved `modeFor` and
`No parameter with name 'applyNightMode' found`. Missing behaviour, not a broken harness.

**GREEN at `e0a6196`** — 140 tests, 0 failures; `assembleDebug` successful.

**RED at `76aace8`** (follow-up) — `LaunchThemeInheritanceTest > API 31 launch theme extends the single
source of shared theme items FAILED`, 141 completed / 1 failed.

**GREEN at `e04577f`, on the rebased head** — `BUILD SUCCESSFUL` for both
`:app:testDebugUnitTest` and `:app:assembleDebug`; **142 tests, no failing test files**. The count is
141 from this item plus the one cross-client parity test item 011 landed on `main`.

The pre-rebase gate result was discarded and re-run: the rebase changed the tree, so the earlier number
no longer described what would merge.

## What review caught that the gates did not

**`res/values-v31/themes.xml` duplicated the base style instead of extending it.**

A qualified resource file *replaces* a style rather than merging into it, so the v31 file restated all
three shared items — `windowActionModeOverlay`, `windowBackground`, `windowNoTitle` — purely to add
`windowSplashScreenBackground`. The two files agreed on the day, so no test failed and nothing was
broken.

The hazard was what came next: any later edit to `values/themes.xml` would silently not reach API 31 and
above, and that divergence is invisible without an emulator — on exactly the API range this item exists
to fix. It is the same class of drift `design.md` D6 created the colour test to prevent, simply
unguarded for themes.

Fixed by extracting `Theme.IntentionalReading.Base`, so the shared items exist in one place and both
qualifiers inherit them. Verified by the reviewer from the **built APK**, not from the source:

```text
$ aapt2 dump resources app-debug.apk
    resource 0x7f0a0009 style/Theme.IntentionalReading
      ()    (style) size=0 parent=style/Theme.IntentionalReading.Base (0x7f0a000a)
      (v31) (style) size=1 parent=style/Theme.IntentionalReading.Base (0x7f0a000a)
        0x0101062c=@color/launch_background
    resource 0x7f0a000a style/Theme.IntentionalReading.Base
      ()    (style) size=3 parent=0x01030241
        0x01010054=@color/launch_background
        0x01010056=true
        0x010102dd=true
```

`0x01030241` is `Theme.Material.Light.NoActionBar`; `0x01010054` is `windowBackground`; `0x0101062c` is
`windowSplashScreenBackground`. The resolved API-31+ theme carries all four attributes through
inheritance, and `AndroidManifest.xml` still references `@style/Theme.IntentionalReading` unchanged.

## Platform facts, verified rather than recalled

`UiModeManager.setApplicationNightMode(I)V` is marked `since="31"` in
`~/Library/Android/sdk/platforms/android-37.0/data/api-versions.xml`; `MODE_NIGHT_NO`, `MODE_NIGHT_YES`
and `MODE_NIGHT_AUTO` carry no `since` and are therefore API 1. AOSP's javadoc
(`core/java/android/app/UiModeManager.java`) states it *"sets and persist[s] the night mode for this
application… until it is either modified by the application, the user clears the data for the
application, or this application is uninstalled"*, and requires no permission. That persistence is the
mechanism: the platform resolves the app's resources against the stored mode when it inflates the launch
window on the next cold start, before the process exists.

**There is no getter.** `UiModeManager` exposes `getNightMode()` — the *system* mode — and no
`getApplicationNightMode()`, confirmed from the same class listing. The app therefore cannot read back
what it set, which is why the dedupe field exists.

## Decisions taken during design

- **No new dependency.** `androidx.core:core-splashscreen` was put to the owner on 2026-08-25 and
  declined. `android/gradle/libs.versions.toml` is untouched. `design.md` D1 records the two rejected
  approaches and what each cost.
- **`android:configChanges="uiMode"` was considered and declined** (`design.md` D5). It would have made
  the recreation hazard structurally impossible, but at the price of changing how the app responds to a
  system theme toggle while open — a path that works today and is covered by 004's walkthrough.
- **The item was given a CI-visible half it would not otherwise have had** (`design.md` D4/D6). The
  night-mode applier is an injected `(Appearance) -> Unit` with a no-op default, following the pattern
  item 004 established for transport, so the mapping and the dedupe are JVM-testable. The colour test
  reads both `colors.xml` files and compares the parsed hex to `lightTokens().bg` and `darkTokens().bg`,
  failing loudly if the colour is absent rather than passing vacuously.
- **The applier is called from `adoptPersistedState` and nowhere else** — the single choke point every
  appearance write already passes through. A process-scoped `lastAppliedAppearance` field returns early
  when the mode is unchanged, so the platform is called at most once per cold start. This matters
  because `adoptPersistedState` also runs on every successful article action.

## Known behaviour this item introduces

On API 31 and above the app now persists its night mode with the platform. A reader who already had a
non-System appearance stored before this item shipped gets one reconciling call on the first launch of
the new build.

## Walkthrough — performed 2026-08-25 against merged `main`

Driven by the orchestrator over `adb` on the `Pixel_10` API 37 emulator (`execution-model.md` §6),
against the APK built from merged `main` (`92223cd`). `ffmpeg` was unavailable, so the launch
transition was stretched by setting `window_animation_scale`, `transition_animation_scale` and
`animator_duration_scale` to 10, making the pre-Compose frame capturable with plain `screencap`. All
three were restored to 1 afterwards.

| Check | Result |
|---|---|
| Scenario 1 — stored Dark, system **Light**, cold start | **Pass.** The window expands from the launcher icon already dark and stays dark while content fades in. No light frame at any point. |
| Scenario 2 — stored Light, system **Dark**, cold start | **Pass.** The window emerges light over the dark launcher wallpaper. No dark frame. |
| Scenario 3 — stored System | **Pass.** Follows the device in both directions. |
| Scenario 4 — change takes effect next cold start | **Pass.** Each appearance change followed by force-stop and cold start painted the new colour. |
| `spec.md` §5 step 5 — recreation loop | **Pass, and the D2 fallback is not needed.** Two consecutive cold starts with an unchanged appearance produced exactly two `Displayed` events and identical `TotalTime` of 383 ms, with no relaunch or configuration-change entries for the package in `logcat`. A set-to-same-value produces no configuration change, so the startup reconcile stays. |
| `spec.md` §5 step 6 — system toggle while open | **Pass.** Toggling the system theme with the app open re-themes it live and preserves state; unchanged from 004. |

**The mechanism was also confirmed directly rather than only by its effect.** With the system in Light
and the stored appearance Dark, `dumpsys activity` reported the package's
`RequestedOverrideConfiguration` as `?uimode night` while `dumpsys uimode` reported the system as
`mNightMode=1 (no)`. The per-application override is live and is precisely what resolves
`@color/launch_background` when the platform inflates the launch window on the next cold start.

**Owner visual pass — signed off 2026-08-25.** The launch frame reads as seamless, not merely as the
right colour. That was the one judgment `adb` could not make, and it closes the last checkpoint
`waves/wave-a.md` §Owner checkpoints held open for this item.

## Outstanding

- **Scenarios 1–4 were emulator-only by construction** — no JVM test observes a pre-Compose frame — so
  this item merged on its JVM half and the walkthrough ran at wave close against merged `main`, per
  `execution-model.md` §6. `slices.md` says the item is not shippable until the walkthrough is recorded;
  that is now satisfied above.
- **The API 26–30 gap stands**, as `spec.md` §6 states plainly: a reader whose stored appearance
  contradicts the system theme still gets one wrong launch frame there. Closing it needed the dependency
  the owner declined.

## Reviewer independence

All product and test code in this item was written by the implementer agent (Codex) across two fresh
sessions. The reviewer authored the specification, design note, slice plan and this evidence file, and
wrote no product or test code. Every gate result quoted here was reproduced by the reviewer with
`--rerun-tasks` in a throwaway worktree, and the merged-theme evidence was read from the built APK
rather than accepted from the implementer's report.
