# 010 — slice plan

Sized **XS → 1 slice**. One item branch (`feat/010-android-launch-theme`), one PR targeting `main`.
The slice closes as a failing-first test commit plus an implementation commit.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

One slice, not two, because the two halves are inseparable: the colour resources without the night-mode
push are correct only when the reader follows the system, and the push without the resources has
nothing to resolve to. Splitting them would produce a first slice that cannot be honestly called done.

## Slice 1: the launch window follows the stored appearance

- **Scenarios:** all eight in `spec.md` §4. Scenarios 5–8 are proven by JVM tests in this slice;
  scenarios 1–4 are proven by the emulator walkthrough in `spec.md` §5, run against merged `main` at
  the end of wave A and recorded in `evidence.md`.
- **Files:**
  - `android/app/src/main/res/values/colors.xml` — `launch_background` `#FFF2F6FB` (new file if absent)
  - `android/app/src/main/res/values-night/colors.xml` — `launch_background` `#FF050A15` (new)
  - `android/app/src/main/res/values/themes.xml` — `android:windowBackground` →
    `@color/launch_background`
  - `android/app/src/main/res/values-v31/themes.xml` — adds `android:windowSplashScreenBackground` →
    `@color/launch_background` (new)
  - `«pkg»/di/AppContainer.kt` — builds the real applier, guards `Build.VERSION.SDK_INT >= 31` here
  - `«pkg»/ui/AppViewModel.kt` — the `applyNightMode: (Appearance) -> Unit = {}` constructor and
    `Factory` parameter, the process-scoped last-applied field, and the call from
    `adoptPersistedState`
  - `«pkg»/MainActivity.kt` — only if the applier genuinely cannot be reached from `AppContainer`;
    prefer not touching it
  - `android/app/src/test/kotlin/**` — the colour-correspondence test and the applier tests
- **Must not touch:** `android/gradle/libs.versions.toml` and every other Gradle file;
  `«pkg»/domain/**`; `«pkg»/ui/theme/**` (the tokens are read, never edited); `«pkg»/ui/screens/**`
  and `«pkg»/ui/components/**`; `«pkg»/data/**`; everything outside `android/` except this item's own
  `specs/010-*/` documents.
- **`AndroidManifest.xml`:** expected to need **no change**. `android:theme="@style/Theme.IntentionalReading"`
  is already on `<application>` (`:20`) and the activity inherits it. If the implementer believes a
  manifest change is required, that is a report to the supervisor with the reason, not a silent edit.
  In particular, `android:configChanges` is **not** to be added (`design.md` D5), and
  `android:windowDisablePreview` is **not** to be added (`design.md` D1).
- **Reuse:** `lightTokens().bg` and `darkTokens().bg` are the source of both hex values — do not
  author a colour (`design.md` D3). Follow `ThemeDerivationTest`'s existing hex-assertion style for the
  correspondence test and `SampleDatasetTest`'s precedent for reading a file from a JVM test. Follow
  item 004's D10 pattern for the injected capability: a function type with a no-op default on both
  `AppViewModel` and its `Factory`, real implementation in `AppContainer`, fake in tests
  (`design.md` D4).
- **Fixed decisions — do not re-open mid-implementation:**
  - **No new dependency.** `core-splashscreen` is declined (`design.md` D1). If the slice appears to
    need a dependency, that is a report to the supervisor.
  - The API-level guard lives in `AppContainer`, not in `AppViewModel`. `AppViewModel` stays free of
    `android.*` imports so the JVM tests keep working.
  - The applier is called from `adoptPersistedState` and nowhere else — that is the one choke point
    every appearance write already passes through (`design.md` D2).
  - The dedupe field lives in `AppViewModel`, not in the applier, so the tests can observe it.
  - The theme parent does not change; only `windowBackground` is overridden (`design.md` D3).
- **Definition of done:**
  - Both gates green: `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug`.
  - A test that reads both `colors.xml` files and asserts the parsed hex equals `lightTokens().bg`
    and `darkTokens().bg` — and that fails if either side is edited alone.
  - A test asserting the three-way mapping: `LIGHT` → `MODE_NIGHT_NO`, `DARK` → `MODE_NIGHT_YES`,
    `SYSTEM` → `MODE_NIGHT_AUTO`.
  - A test asserting the applier is invoked **once** across a startup restore followed by several
    article actions that do not change the appearance.
  - A test asserting a reset pushes the automatic mode.
  - A test asserting a fresh `AppViewModel` built without an applier still works — the default is a
    no-op and no existing test needed editing to accommodate this item.
  - `git diff` shows `libs.versions.toml` untouched and no `configChanges` in the manifest.
  - No assertion from the existing suite deleted.
- **Deferred to wave A's batched walkthrough:** scenarios 1–4 plus the recreation-loop check
  (`spec.md` §5 step 5) and the owner's visual pass. The slice is *done* when the JVM half is green;
  the **item** is not shippable until the walkthrough is recorded in `evidence.md`.
- **Status:** pending
