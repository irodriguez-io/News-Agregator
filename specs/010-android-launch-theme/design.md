# 010 — Design note

Decisions for `spec.md`. Platform facts here were verified against the SDK installed on this machine
and against AOSP source, not recalled — each carries its citation.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths: `android/**`. Forbidden:
`pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`.

## D1 — No dependency, and what that costs

Put to the owner on 2026-08-25 with four options. **Decision: no new dependency.**
`androidx.core:core-splashscreen` is not approved, and `android/gradle/libs.versions.toml` is not
touched. `AGENTS.md` and item 004 §3 both make a new Android dependency an approval, not a
convenience.

The two approaches that were declined, recorded so a future item does not re-derive them:

- **`androidx.core:core-splashscreen`.** Backports the Android 12 splash to API 23+ and adds
  `keepOnScreenCondition`, which would hold the splash until the stored appearance is read. It does
  **not** remove the API 26–30 gap on its own — its splash background is still a static resource, so
  it needs the night-qualified colour below regardless. It buys control over *when* Compose is
  revealed, not correctness.
- **`android:windowDisablePreview="true"`.** Suppresses the preview window entirely below API 31, so
  nothing can flash. Costs launch feedback — tap, blank, app — and is ignored on API 31+ where the
  system splash is mandatory.

## D2 — `UiModeManager.setApplicationNightMode`, called once per process per distinct value

**Verified, not assumed.** `android/app/UiModeManager.setApplicationNightMode(I)V` is marked
`since="31"` in `~/Library/Android/sdk/platforms/android-37.0/data/api-versions.xml`, alongside
`MODE_NIGHT_NO`, `MODE_NIGHT_YES`, and `MODE_NIGHT_AUTO`, which carry no `since` and are therefore
API 1. The AOSP javadoc (`core/java/android/app/UiModeManager.java`) reads: *"Sets and persist the
night mode for this application… The mode is persisted for this application until it is either
modified by the application, the user clears the data for the application, or this application is
uninstalled."* It requires no permission.

That persistence is the whole mechanism. The platform stores the app's night mode and resolves the
app's resources against it — including when it inflates the launch window on the next cold start,
before the process exists. This is the documented remedy for exactly this defect: Android's dark-theme
guidance says to use it *"to let the system know what theme your app runs, which lets the system match
the theme during the splash screen."*

The mapping is total and has no fourth case:

| `Appearance` | Platform mode |
|---|---|
| `LIGHT` | `MODE_NIGHT_NO` |
| `DARK` | `MODE_NIGHT_YES` |
| `SYSTEM` | `MODE_NIGHT_AUTO` |

**The hazard, and the guard.** The javadoc also says a change *"will result in a configuration change
being applied to this application"* — and a `uiMode` configuration change recreates the activity. There
is **no getter**: `UiModeManager` exposes `getNightMode()` (the *system* mode) and no
`getApplicationNightMode()`, confirmed from the same `api-versions.xml` class listing. So the app
cannot read back what it previously set and must not call the setter indiscriminately.

The guard is a process-scoped field holding the last mode this process pushed. The applier is invoked
from `adoptPersistedState` — the single choke point every appearance write already funnels through
(restore at startup, `setAppearance`, `resetLocalData`, and item 009's import when it arrives) — and
returns immediately when the mode is unchanged. The consequence is at most **one** platform call per
cold start, which is the startup reconcile, and it is a no-op at the platform level whenever the stored
value already matches what the platform holds.

That reconcile is what closes the upgrade case: a reader who already had Dark stored before this item
shipped has no platform override, and the first launch on the new build establishes it.

**Fallback, pre-authorised so it does not need a new design round.** If walkthrough step 5 shows the
app visibly restarting itself on a second cold start with an unchanged appearance, then the platform is
firing a configuration change for a set-to-same-value and the startup reconcile must go: drop the call
from the restore path, keep it on `setAppearance` and `resetLocalData` only, and accept that a reader
upgrading with a stored override keeps the flash until they re-select the setting once. Record it in
`evidence.md` as a taken fallback, not as a defect.

## D3 — The launch background is the `bg` token, as a night-qualified colour resource

Two resource files, one colour name:

| File | `launch_background` |
|---|---|
| `res/values/colors.xml` | `#FFF2F6FB` |
| `res/values-night/colors.xml` | `#FF050A15` |

Those are `LightTokens.bg` and `DarkTokens.bg` (`ui/theme/Tokens.kt:38`, `:47`), which
`ThemeDerivationTest` already pins as `#F2F6FB` and `#050A15`. Reusing them is what makes the launch
frame and the first composed frame the same colour, which is the entire point — a launch frame that is
merely *dark* rather than *this* dark still reads as a flash.

`themes.xml` sets `android:windowBackground` to `@color/launch_background`, and a `values-v31`
variant additionally sets `android:windowSplashScreenBackground` to the same reference, because on
API 31+ the system splash is mandatory and reads that attribute. One `values-v31/themes.xml` covers
both appearances: the colour reference is what carries the night qualifier, so no
`values-night-v31` is needed.

**The theme parent does not change.** `android:style/Theme.Material.Light.NoActionBar` stays. Only
`windowBackground` is visible before Compose starts, and it is being overridden; swapping the parent
for a DayNight family would be a wider change than this item needs and the DayNight parents are not
available at `minSdk` 26 without a qualifier.

**Nothing new is added to the palette.** If the implementer finds themselves authoring a third colour,
that is a report to the supervisor, not a decision to make.

## D4 — The applier is an injected function, so the ViewModel stays JVM-testable

`AppViewModel` must not acquire a `Context` to reach a system service — that would put an
`android.content.Context` in the constructor and break every existing JVM test. It follows the pattern
item 004 established for transport (D10 there): the capability is a function type with a no-op
default, supplied by `AppContainer`, faked in tests.

```kotlin
// AppViewModel constructor
private val applyNightMode: (Appearance) -> Unit = {},
```

`AppViewModel.Factory` gains the same parameter with the same default, so every existing construction
site and test keeps compiling untouched. `AppContainer` builds the real one from the application
context and guards the API level there, in the one place that knows about the platform:

```kotlin
val applyNightMode: (Appearance) -> Unit =
    if (Build.VERSION.SDK_INT >= 31) { appearance -> uiModeManager.setApplicationNightMode(modeFor(appearance)) }
    else { _ -> }
```

Tests inject a recorder and assert the *sequence* of applied modes, which is what makes scenarios 6–8
CI-visible rather than emulator-only. The process-scoped dedupe field from D2 lives in `AppViewModel`,
not in the applier, so the dedupe is what the tests observe.

**Nothing is persisted by this item.** The platform holds the night mode; `LocalState` is unchanged and
`contracts.md` §29 is untouched.

## D5 — `android:configChanges="uiMode"` was considered and declined

It would guarantee that a night-mode configuration change never recreates the activity, which would
make D2's hazard structurally impossible rather than merely guarded. It is safe in principle here,
because nothing this app renders at runtime resolves from a night-qualified resource — the palette
comes from Kotlin tokens through `IntentionalReadingTheme`, and the only night-qualified resource this
item introduces is the launch background, which is irrelevant once Compose is running.

Declined anyway, on scope: it changes how the app responds to a *system* theme toggle while open — a
path that works today and is covered by 004's walkthrough. Trading a working path for a hazard that
D2's field already guards is a bad exchange in an item this small. If D2's fallback is ever taken,
this is the next thing to try, and the reasoning above is why it is written down.

## D6 — Why the emulator is the gate, and what CI can still hold

No JVM test observes a pre-Compose frame. Instrumented tests are parked from CI
(`specs/backlog.md` §Parked, from 002 slice 4), and even an instrumented test would struggle: the frame
in question is painted by the system before the test's own process attaches.

So the item is split deliberately. What CI holds is the two things that would silently break the fix:
the colour correspondence between the XML resources and the authored tokens, and the appearance-to-mode
mapping with its dedupe. What the emulator holds is whether the frame is actually right, and that is
`spec.md` §5's walkthrough. Neither half is sufficient; both are cheap.

The colour test reads `src/main/res/values/colors.xml` and `src/main/res/values-night/colors.xml` as
text from the module directory and compares the parsed hex to `lightTokens().bg` and `darkTokens().bg`.
Reading a file from a JVM test is established here — `SampleDatasetTest` does it — and this is the
same discipline as a frozen-copy assertion: the test exists precisely so that editing one side without
the other fails the build.
