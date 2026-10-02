# 030 — The toast eases in, the shadows show, and wide screens keep a reading width

**Surface:** Android only.
**Authority:** `docs/v1/06-ui-ux.md` §14.2, §16.2, §45, §48, §71.2, §78.4 (second edition);
`specs/backlog.md` Road to done row 7b. **No amendment.**
**Branch:** `feat/030-android-toast-shadows-content-cap`, cut from `main` at `e730efc`, PR targets `main`.

---

## 1. Why this item exists

Road 7's sweep found five things the second edition states and the Android client does not do
(`specs/backlog.md` row 7b):

1. **§45.2 toast transition.** The spec says *"M3 standard easing at an equivalent short duration, with the
   same opacity-and-slight-translation character"*. Today the toast appears and disappears instantly:
   `IntentionalReadingApp.kt:346-354` is a bare `if`. The browser's toast is 180 ms of opacity plus an
   8 px rise (`css/app.css:911-914`).
2. **§16.2 / §78.4 card shadow.** The spec asks for the deck card's ambient shadow *"tinted with the
   `tertiary` seed … at approximately 10% opacity"* in light. The code passes `tertiary` at 10% alpha as the
   shadow colour (`Theme.kt:155`, `ArticleCard.kt:223-229`). Android then multiplies that alpha by the
   theme's own shadow alphas, so the drawn shadow is far fainter. **Measured** on the emulator
   (debug build of `826f06c`, light, 1080×2400, 2026-10-01): the darkest shadow pixel below the card is `#F4F6FA`
   on a `#F7F9FD` background. That is about **1.4%** of the way from the background to `tertiary`
   (`#212B56`), where 10% would be about `#E2E4EC`. The only test reads source text
   (`ArticleCardTest.kt:240-251`).
3. **§16.2 sheet shadow.** The modal sheet has *"8dp+ elevation with a dimming scrim"*. The owner reads
   that as a drop shadow (2026-10-01). The scrim is present; no shadow is set (`SettingsSheet.kt:119-130`).
   **Pre-approved drop (owner, 2026-10-01):** if a test shows the shadow does not render, this part is
   dropped without coming back to the owner, and it is recorded in After done with that evidence.
4. **§14.2 content cap and tablet margin.** *"Central content is capped at 680 dp on large screens"*, and
   the tablet margin is 24 dp. Both are defined (`Spacing.kt:24-26`) and neither is applied: every
   screen uses the 18 dp `mobileMargin` at every width (`DiscoverScreen.kt:116`, `ReadLaterScreen.kt:42`,
   `HistoryScreen.kt:48`). At 768 dp, a width §71.2 ships, text runs about 732 dp wide.
5. **§48 test.** *"Every animation either surface adds must honour this, and a test must assert that it
   does."* Discover's two automatic scrolls, to the next card (`DiscoverScreen.kt:77-90`) and to reveal
   the actions after opening an article (`:94-111`), branch correctly to `scrollTo` under reduced motion,
   but no test asserts it.

### 1.1 Readings, for approval at the plan gate

- **Toast values.** 200 ms, which is M3's *short 4* duration and the M3 step nearest the browser's 180 ms,
  on M3 Standard easing `(0.2, 0, 0, 1)`, the curve §79.4 already uses. The toast rises 8 dp, matching the
  browser's 8 px, and fades from 0 to 1. On leaving it reverses: it fades and drops 8 dp. **A replacement
  toast** (a new message while one is showing) fades out the old one while the new one fades and rises in,
  as the browser's does.
- **Which messages count as the toast.** The Undo toast, and also Android's visible status message for
  import, export and reset outcomes (`LocalStateMessages.kt:79-98`). The browser has no visible
  counterpart for the second: its outcomes go to a hidden live region (`announceStatus`,
  `js/ui/toast.js:6`; for example `js/ui/settings.js:30`). On Android the status message uses the toast's
  exact surface, shape and slot (`IntentionalReadingApp.kt:339-354`). If it kept popping in beside a toast
  that eases, it would read as a defect.
- **A leaving toast cannot be tapped.** Today it vanishes at once, so nothing can tap it after it expires.
  The 200 ms exit keeps that rule: while it is leaving it ignores input, so a late tap cannot act.
- **"Large screens" and "tablet" start at 600 dp.** That is M3's compact/medium window boundary, and 600 is
  the first non-handset width §71.2 ships. Below 600 dp the margin stays 18 dp and nothing changes. From
  600 dp the margin is 24 dp, and the content is centred at no more than 680 dp. The width is the screen
  area the app is given, not the device, so split-screen behaves correctly. The top app bar and the bottom
  navigation stay full width, because they are chrome, not "central content".
- **The card shadow is calibrated by measurement.** The rendered shadow's darkest pixel must sit 8–12% of
  the way from `bg` to `tertiary`, and be tinted toward `tertiary` rather than neutral grey. The dark scheme
  keeps its near-black shadow, unchanged (§78.4).
- **Android 8.0 and 8.1 draw shadows in grey.** Coloured shadows start at Android 9 (API 28), and `minSdk`
  is 26 (`app/build.gradle.kts:24`). On API 26–27 the card's shadow will be neutral. Drawing a custom
  shadow to cover two old versions is out of proportion, so it is recorded as a known limitation.

---

## 2. Story

As a **reader**, I want the app's small moments to feel calm and its layout to fit my screen, so that
undoing, reading on a tablet and seeing which card is in front all work the way the design describes.

---

## 3. Scenarios

### Scenario: the toast eases in
Given motion is not reduced
When an Undo toast appears
Then it starts transparent and 8 dp below its resting place
And over 200 ms on M3 Standard easing it reaches full opacity at rest
And its Undo action works from the first frame

### Scenario: the toast eases out, and cannot be tapped while leaving
Given an Undo toast is showing and motion is not reduced
When it expires or the reader taps Undo
Then it fades and drops 8 dp over 200 ms
And a tap on it while it is leaving does nothing

### Scenario: the status message moves the same way
Given motion is not reduced
When an import, export or reset outcome is shown
Then the status message enters and leaves as the toast does

### Scenario: reduced motion shows and hides the toast at once
Given motion is reduced
When a toast or status message appears or leaves
Then it is at full opacity in its resting place on the first frame, and gone on the first frame after
leaving

### Scenario: the toast's timing and semantics are unchanged
Then the Undo toast still stays about 4.5 s, the status message about 6 s
And both still announce through a polite live region and take no focus
And every existing toast, undo and destination test passes unedited

### Scenario: the card's shadow is visible and navy
Given Discover shows a card in the light scheme
Then the darkest shadow pixel just below the card is 8–12% of the way from `bg` to `tertiary`
And its darkening is tinted toward `tertiary`, not neutral
Given the dark scheme
Then the shadow is near-black, as before

### Scenario: the settings sheet casts a shadow
Given the Settings sheet is open
Then just above its top edge the scrim is darker than the scrim further away
Or, if that does not render, the sheet shadow is dropped under the owner's pre-approval and recorded

### Scenario: handsets are unchanged
Given widths of 360, 390 and 430 dp
Then every screen's content sits 18 dp from each edge, as today

### Scenario: wide screens keep a reading width
Given a width of 600 dp
Then content sits 24 dp from each edge (552 dp wide)
Given a width of 768 dp
Then content is 680 dp wide and centred (44 dp each side)
And the app bar and bottom navigation still span the full width
And Discover, Read Later and History all follow the same rule

### Scenario: Discover's automatic scrolls honour reduced motion
Given motion is reduced
When the next card arrives, or an opened article returns the reader to Discover
Then the scroll reaches its target on the first frame
Given motion is not reduced
Then the same scroll is still between start and target partway through

---

## 4. Out of scope

- The toast's duration, text, position, shape and colours, and the 12 dp radius (After done).
- Any shadow besides the card and the sheet; the sheet's corners and scrim.
- Settings sheet width (Material's sheet has its own maximum).
- Landscape exit-to-entrance timing (After done, 024).
- The browser runtime, `pipeline/**`, `config/**`, `docs/v1/**`.

---

## 5. Verification

### 5.1 Gates

With `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
`ANDROID_HOME=$HOME/Library/Android/sdk` exported, from `android/`:
`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.
Baseline **405 JVM / 46 instrumented**; each slice reports the counts after.

**Every test asserts the rendered result** (backlog guardrail): pixels for opacity and shadows, laid-out
bounds for position and width, and scroll position for scrolling. None reads source text.

### 5.2 Orchestrator

Screenshots of Discover at 360, 600 and 768 dp; the card shadow measured from pixels in light; the sheet
shadow in light; and a frame captured mid-entrance of the toast. All are recorded in `evidence.md`.

### 5.3 Owner walkthrough on a signed release build

With `animator_duration_scale` at `1`: save and dismiss a few cards, and judge whether the toast's arrival
reads as calm and whether the card now stands off the page. Open Settings and judge the sheet. Uninstall
afterwards. Findings go through the freeze test.
