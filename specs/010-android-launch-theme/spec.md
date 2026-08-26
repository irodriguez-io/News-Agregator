# 010 — Launch theme

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/contracts.md` §29, `docs/v1/06-ui-ux.md`, `design-reference/DESIGN.md`,
`docs/v1/README.md` Amendment 6\
**Wave:** A (`specs/waves/wave-a.md`) · **Branch:** `feat/010-android-launch-theme` → `main`

---

## 1. Problem

Item 003 shipped a gate against exactly one flash and recorded that it did not cover another. D8 keeps
the app from composing any content until the stored appearance resolves
(`specs/003-android-local-state-persistence/design.md:100-105`), so no *composed* frame is ever the
wrong colour. But Android paints the activity's launch window from the manifest theme before the
process has run a line of application code, and that theme is
`android:style/Theme.Material.Light.NoActionBar` with no night variant
(`android/app/src/main/res/values/themes.xml:3`). A reader whose stored appearance is Dark gets a
white frame on every cold start, before anything the app controls can intervene.

003 stated the constraint that makes it a design problem rather than a one-line fix: *"no static
`windowBackground` can be correct when the stored appearance may be Dark while the system is Light"*
(`specs/003-android-local-state-persistence/evidence.md:97-101`). The stored appearance lives in
`LocalState.settings.appearance` (`contracts.md` §29) and is read off disk by `AppViewModel.init`. At
launch-window time there is no `AppViewModel`, no `LocalState`, and no disk read — only resource
resolution against the system configuration.

Three settings exist, and the frame has to be right in all three:

| Stored appearance | System is Light | System is Dark |
|---|---|---|
| Light | correct today | **wrong today** |
| Dark | **wrong today** | correct today |
| System | correct today | **wrong today** |

Two of the six combinations work by accident, because the launch window is unconditionally light.

## 2. Story

As a reader who has chosen Dark, I want the app to be dark from the first frame it paints, so that
opening it at night does not flash a white screen at me before the interface I chose appears.

## 3. Out of scope

- **New dependencies.** `androidx.core:core-splashscreen` was put to the owner on 2026-08-25 and
  **declined**. Nothing is added to `android/gradle/libs.versions.toml`
  (`design.md` D1).
- **A splash screen, a brand frame, a logo, or an animation.** This item makes the existing launch
  window the right colour. It does not introduce a new launch experience.
- **Any change to composed frames.** D8's gate stands exactly as item 003 left it, and this item
  neither relaxes it nor relies on it.
- **Any change to the palette.** The launch background reuses the authored `bg` token for each
  appearance and introduces no new colour (`design.md` D3).
- **Any change to `LocalState` or `contracts.md` §29.** The stored appearance is read, never
  reshaped, and nothing new is persisted to the local-state document (`design.md` D4).
- **`android:configChanges` on the activity.** Considered and declined; see `design.md` D5. The
  activity keeps the platform's default recreation behaviour on a system theme change.
- **Any change to `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `tests/**`, or
  `docs/v1/**`.** Amendment 6 confines this item to `android/`.

## 4. Scenarios

Scenarios 1–4 are observable only on a device (`design.md` D6). Scenarios 5–8 are JVM-testable and are
the CI-visible half of this item.

### Scenario: a dark reader on a light device gets a dark first frame

Given the stored appearance is Dark\
And the device's system theme is Light\
When the app is cold started on API 31 or above\
Then the frame painted before Compose starts is the dark background colour\
And no light frame appears at any point during launch

### Scenario: a light reader on a dark device gets a light first frame

Given the stored appearance is Light\
And the device's system theme is Dark\
When the app is cold started on API 31 or above\
Then the frame painted before Compose starts is the light background colour\
And no dark frame appears at any point during launch

### Scenario: a reader who follows the system gets whatever the system is

Given the stored appearance is System\
When the app is cold started\
Then the frame painted before Compose starts matches the device's system theme\
And it changes with the system theme without the reader touching the app

### Scenario: choosing an appearance takes effect on the next cold start

Given the stored appearance is System on a light device\
When the reader selects Dark in Settings\
And the app is force-stopped and cold started again\
Then the frame painted before Compose starts is the dark background colour

### Scenario: the launch background equals the composed background

Given the authored appearance tokens\
When the launch background colour resources are read\
Then the light launch background equals the light `bg` token exactly\
And the dark launch background equals the dark `bg` token exactly

### Scenario: the stored appearance is pushed to the platform when it changes

Given a running application\
When the stored appearance changes to Light, to Dark, or to System\
Then the corresponding platform night mode is applied for this application\
And the mapping is Light to no-night, Dark to night, and System to automatic

### Scenario: the platform is told once per process, not once per action

Given an application whose stored appearance has already been pushed to the platform\
When any number of further local-state writes occur that do not change the appearance\
Then the platform night mode is not applied again

### Scenario: resetting local data returns the launch frame to following the system

Given the stored appearance is Dark\
When the reader resets local data\
Then the stored appearance returns to System\
And the platform night mode for this application is set back to automatic

## 5. Verification

The JVM gates, re-run by the reviewer in a throwaway worktree rather than read from an implementer
report:

```sh
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Scenarios 5–8 are covered there: the colour-resource correspondence by a test that reads the two
`colors.xml` files and compares them to `lightTokens()`/`darkTokens()` in the manner
`ThemeDerivationTest` already uses, and scenarios 6–8 by a fake night-mode applier injected into
`AppViewModel` (`design.md` D4).

**Scenarios 1–4 are emulator-only, and that is the substance of this item.** No JVM test can observe a
pre-Compose frame. The orchestrator drives this over `adb` on a Pixel API 37 emulator
(`execution-model.md` §6), batched into wave A's walkthrough against merged `main`:

1. Set the system theme to Light. Store appearance Dark. Force-stop, cold start, and capture the
   launch with `adb shell screenrecord` at a high frame rate; step the frames and confirm none is
   light.
2. Set the system theme to Dark. Store appearance Light. Same capture; confirm no dark frame.
3. Store appearance System. Cold start under each system theme; confirm the launch frame follows.
4. Change the appearance in Settings, force-stop, cold start, and confirm the launch frame changed
   with it.
5. **Cold start twice in a row with an unchanged appearance and confirm the app does not visibly
   restart itself.** This is the check for the one hazard in `design.md` D2 — if a recreation loop
   appears here, the fallback in D2 applies and it is a finding, not a surprise.
6. Toggle the system theme while the app is open and confirm the app still re-themes as it did at 004.

**Owner checkpoint.** Whether the launch frame reads as *seamless* — as opposed to merely the right
colour — is a visual judgment `adb` cannot make. The owner is asked for that pass, and for it only,
per `waves/wave-a.md` §Owner checkpoints.

## 6. The gap this item does not close

**On API 26 through 30, a reader whose stored appearance contradicts the system theme still gets one
wrong launch frame.** `UiModeManager.setApplicationNightMode` is API 31 and above — verified against
`platforms/android-37.0/data/api-versions.xml`, which marks it `since="31"` — and the platform offers
no equivalent below it that does not require a new dependency. Below API 31 the launch window follows
the *system* configuration only, so Light-on-light, Dark-on-dark, and System are correct and the two
contradicting combinations are not.

This is stated rather than buried because the alternative was a dependency the owner declined. The
supported range is API 26+; the emulator this project verifies on is API 37. If the gap ever needs
closing, `design.md` D1 records the two approaches that would do it and what each costs.
