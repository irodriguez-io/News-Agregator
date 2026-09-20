# 022 — The appearance switch changes colour, not the screen

**Surface:** Android only.
**Authority:** `docs/v1/06-ui-ux.md` §46, §47, §48, §64, §79 (second edition); Amendment 9; **Amendment 10,
authored by this item**.
**Branch:** `feat/022-android-appearance-switch-motion`, cut from `main`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 The defect, as the owner found it

Choosing Light, Dark or System in the Settings sheet makes the whole screen flash white or black before the
new scheme appears. Reported on 2026-09-20 during wave E's walkthrough, in the owner's words: *"the screen
flickers then changes color scheme. The UX feels like an unfinished product."*

The flash is not a slow repaint and not an emulator artefact. It is the Activity being destroyed and rebuilt.

### 1.2 The cause is a manifest omission, not a theming bug

`android/app/src/main/AndroidManifest.xml` declares `MainActivity` with **no `android:configChanges`**.
`di/AppContainer.kt:26` calls `UiModeManager.setApplicationNightMode(...)`, which the AOSP javadoc states
*"will result in a configuration change being applied to this application"*. With nothing declared, a
`uiMode` configuration change recreates the Activity. The flash is that recreation.

Nothing in the theming code is wrong. `ui/theme/Theme.kt` already derives the scheme from Kotlin tokens and
would repaint in place given the chance.

### 1.3 Item 010 predicted this and wrote down the remedy

`specs/010-android-launch-theme/design.md` **D5** considered `android:configChanges="uiMode"` and declined it
— on scope, not on merit. Its words: *"If D2's fallback is ever taken, this is the next thing to try, and the
reasoning above is why it is written down."* D5's grounds for declining were that the flash had not been
observed and that the line also changes how the app responds to a **system** theme toggle while open, which
was a working path not worth trading.

The flash has now been observed. This item takes D5's own recommendation and pays its stated cost.

### 1.4 D5's supporting claim was re-verified against wave E, and holds

D5 argued the line is safe *"because nothing this app renders at runtime resolves from a night-qualified
resource."* Wave E rewrote the entire theme, so that claim was re-checked on 2026-09-20 against merged
`main` at `15e082c`:

- `app/src/main/res/values-night/` contains exactly one file, `colors.xml`, holding exactly one entry,
  `launch_background`. It is consumed only by `themes.xml` as `android:windowBackground` and is irrelevant
  once Compose is composing.
- Every runtime colour comes from `IntentionalReadingTokens` (26 `Color` fields, `ui/theme/Tokens.kt`) and
  the `ColorScheme` derived from it, both authored in Kotlin. Amendment 9 forbids any component outside the
  theme package from naming a colour, which makes this structural rather than incidental.

So no drawable, dimension, string or style the running app reads is night-qualified. Declining the Activity
rebuild loses the app nothing it currently uses.

### 1.5 The switch is also silent about itself, and that is the second half

Stopping the rebuild removes the flash but leaves the change abrupt — the palette replaces itself in a single
frame. Wave E's whole subject is that this client moves expressively (§79), and the appearance switch is the
one visible state change in the app that §79 does not cover. **The owner chose a cross-fade over an instant
swap on 2026-09-20.**

`06-ui-ux.md` is silent on this transition, and AGENTS.md forbids inventing requirements where an
authoritative specification is silent. Authoring the motion therefore requires an amendment; see §3.

### 1.6 What this item is not

It is not the Discover card swipe. That defect — the card leaving at roughly five screen-widths per second
and the replacement appearing with no transition at all — was found in the same session and is a separate
item, designed after this one. The two share no files.

---

## 2. Story

As a **reader**, I want the appearance control to change the app's colours, so that choosing a scheme reads
as a setting taking effect rather than the application restarting.

---

## 3. Amendment 10 — authored by this item, approved at the plan gate

`06-ui-ux.md` §79 has no subsection for the appearance transition, and §48's reduced-motion list does not
name it. Both are `docs/v1/**` changes and neither may be made silently (AGENTS.md; the precedent is
Amendments 7 and 8, each a narrow change carried by one item).

**Amendment 10, Android Appearance Transition**, is narrow and Android-only:

- adds **§79.4**, specifying the appearance scheme transition — `300ms`, Material 3 Standard easing, a
  cross-fade of the resolved colour scheme with no movement, no scaling and no change of layout;
- adds *the appearance scheme transition (§79.4)* to **§48**'s list of motion that a reduced-motion
  preference must remove or effectively eliminate;
- changes **no** behaviour: not a state transition, status value, signal, delta, count, ranking, undo path,
  gesture semantic, keyboard binding or authored string. The set of appearance options is unchanged, the
  control is unchanged, and appearance changes remain non-reversible and continue to clear the undo record
  (Amendment 8).

The amendment binds the Android client only. No `js/**` or `css/**` change is authorized or required.

---

## 4. Out of scope

- The Discover card swipe motion (its own item).
- Any change to which appearance options exist, or to the Settings sheet's layout, copy or controls.
- Any change to undo. An appearance change clears the undo record under Amendment 8 and continues to;
  declining the Activity rebuild does not make it reversible. **The first draft of this spec said the
  live undo offer survived the switch, which contradicted Amendment 8; corrected 2026-09-20 after the
  slice 1 implementer refused to resolve the contradiction in either direction.**
- Any change to the launch frame, `values-night/colors.xml`, `themes.xml`, or item 010's cold-start
  behaviour. The platform night mode continues to be set and continues to be what makes the launch frame
  correct.
- Removing `UiModeManager.setApplicationNightMode`. Its persistence is the mechanism item 010 shipped; this
  item keeps the call and only declines the Activity rebuild it triggers.
- Any new colour, token or seed. Amendment 9's palette is fixed.
- Animating anything other than the resolved colour scheme.
- `orientation`, `screenSize` or any configuration key other than `uiMode`.

---

## 5. Scenarios

### Scenario: choosing a different appearance does not restart the screen
Given the reader is on any destination
When the reader selects an appearance different from the current one
Then the Activity is not destroyed and recreated
And the destination and scroll position are unchanged
And the undo record is cleared, exactly as Amendment 8 already requires of an appearance change

### Scenario: the colours cross-fade rather than snap
Given a reduced-motion preference is not set
When the resolved scheme changes from light to dark, or from dark to light
Then the resolved colour scheme animates from the old scheme to the new one over `300ms` on Material 3
Standard easing
And no element moves, scales, or changes size or position during the transition

### Scenario: the end states are exactly the authored palette
Given the transition has completed
Then every colour role and every token equals the value it holds today for that scheme, unchanged

### Scenario: selecting the appearance already in effect changes nothing
Given the reader's appearance is Dark
When the reader selects Dark
Then no platform night-mode call is made
And no transition runs

### Scenario: System still follows the phone while the app is open
Given the reader's appearance is System
And the app is open and composing
When the phone's own light/dark setting is changed
Then the app's resolved scheme follows it
And it arrives by the same cross-fade, not by a restart

### Scenario: a reduced-motion preference makes the change immediate
Given a reduced-motion preference is set
When the resolved scheme changes
Then the new scheme is in effect immediately, with no intermediate blend
And no flash or restart occurs

### Scenario: nothing bounces, pulses or celebrates
When the appearance changes
Then no motion on §47's prohibited list occurs
And the transition is a colour cross-fade only

### Scenario: the launch frame is still correct on a cold start
Given the reader's appearance is Dark
When the app is cold-started
Then the launch frame is the dark launch background, as item 010 shipped
And no flash of the light background occurs

---

## 6. Verification

### 6.1 Gates

Per `execution-model.md` §8, from `android/`, with both of these exported or Gradle fails before any test
runs:

```
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
ANDROID_HOME=$HOME/Library/Android/sdk
```

```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

`test.yml` will run on this PR and means nothing about web paths (§8.1). Hosted CI must be green on the exact
final head before the final review merges.

### 6.2 What is assertable, and what is not

**JVM-assertable.** The blend function: at fraction `0` it returns the light tokens and scheme exactly, at
`1` the dark ones exactly, and at intermediate fractions each role lies between the two. The existing
`ThemeDerivationTest` must keep passing **unedited** — it pins the authored values, and if the blend is
correct at its endpoints it cannot move them. The reduced-motion branch is assertable as a spec selection.

**Instrumented, and this is the load-bearing one.** That `Appearance.SYSTEM` still follows a system theme
toggle once the Activity stops being recreated. This is the exact risk D5 named, and the only reason it is
acceptable to take D5's line now is that item 018 put `connectedDebugAndroidTest` in CI, so the guard is
durable rather than a one-time observation. **A manual check is not sufficient for this scenario.**

**Not assertable, and therefore walkthrough evidence.** That the cross-fade reads as deliberate rather than
sluggish. §47's boundary is taste.

### 6.3 Walkthrough — required evidence

On a device, driven over `adb`, `screencap` rather than `uiautomator dump`:

1. Light → Dark from the Settings sheet: no flash, colours travel.
2. Dark → Light: the same.
3. System selected, phone theme toggled while the app is open: the app follows, by cross-fade.
4. With the system's remove-animations setting on: the change is immediate and still has no flash.
5. Cold start with Dark stored: the launch frame is dark, per item 010.
6. Frame timing across the transition, captured with `dumpsys gfxinfo`, on a **release** build.

### 6.4 The measurement that gates the cross-fade

`LocalIntentionalReadingTokens` is a `staticCompositionLocalOf`, which does not track reads — changing it
recomposes the whole content subtree. A `300ms` fade at 60 Hz is roughly eighteen such recompositions. If
that is too expensive the fade will itself judder, which would be an absurd outcome for a defect about
judder.

**This must be measured, not assumed** — `design.md` D4 carries the pre-authorised fallback so that a bad
measurement does not require a new design round.

### 6.5 Owner checkpoints

1. **Amendment 10's text**, at the plan gate. It is a `docs/v1/**` change.
2. **The walkthrough**, which carries the taste judgement in §6.2.
