# 030 — slice plan

**Four slices, each a RED and a GREEN commit (slice 4 is test-only; see there), run in order on one
branch.** They are split by objective, so each Codex session holds one idea, and the files barely overlap:
slice 1 is the toast, slice 2 the two shadows, slice 3 the reading width, and slice 4 Discover's scroll test.
Slices 3 and 4 both touch Discover, so slice 4 runs after slice 3.

---

## Fixed for this item: do not re-decide these mid-implementation

1. **Toast:** 200 ms, `CubicBezierEasing(0.2f, 0f, 0f, 1f)`, fade 0↔1 plus 8 dp vertical travel, in and
   out; a leaving copy is inert; reduced motion is immediate (`design.md` D1). The 4.5 s and 6 s timers,
   texts, shapes, colours and live-region semantics are unchanged.
2. **Card shadow:** a new `deckShadow` token role. Light is `tertiary` at a calibrated alpha, with the
   rendered peak 8–12% of the way from `bg` to `tertiary`; dark is `bg` at 10%, unchanged. **No change to
   `colorScheme.surfaceTint`, and no window-theme shadow attributes** (`design.md` D2).
3. **Sheet shadow:** `shadow(8.dp, shapes.modalSheet, clip = false)` with the default colour. If it does not
   render, revert it and report: this is the owner's pre-approved drop (`design.md` D3).
4. **Reading width:** `readingHorizontalPadding` exactly as `design.md` D4. Take the two margins and the
   680 dp cap from `spacing`. `Spacing.kt` has no threshold, and it is out of bounds, so the 600 dp
   threshold is one named constant in `ReadingWidth.kt` (corrected 2026-10-02, when slice 3's implementer
   raised the conflict).
5. **Every test asserts the rendered result**: pixels, laid-out bounds or scroll position, never source
   text. Where an animation is checked, pause the clock with `mainClock.autoAdvance = false` and step it.
6. **Every animation this item adds ships its reduced-motion branch and its test in the same slice** (§48).
7. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing back.

**Common must-not-touch, every slice:** `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`,
`.github/workflows/**`, `pipeline/**`, `config/**`, `docs/**`, `specs/**`, the web runtime, and any file not
listed in the slice. **Existing tests:** edit only the ones a slice lists, each for the reason given. If any
other existing test fails, stop and report **before** editing it (execution-model §2.1 rule 5).

---

## Slice 1: the toast eases in and out

**Objective.** The Undo toast and the status message enter and leave with §45.2's motion, immediately under
reduced motion, and a leaving copy cannot be tapped.

- **Scenarios:** `spec.md` §3, the first five (the toast and the status message).
- **Files, production** (under `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`): a new
  `ui/components/ToastRegion.kt`, and `ui/IntentionalReadingApp.kt`, where only the toast `Column` at
  `:338-356` is replaced by a `ToastRegion(...)` call.
- **Files, tests:** a new `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/ToastMotionInstrumentedTest.kt`.
  Compose `ToastRegion` directly, with motion and with reduced motion, and assert:
  1. **with motion, on entry:** at 0 ms the toast's laid-out top is 8 dp below its resting top and its pixels
     match the background. At about 100 ms its top is between the two and its surface pixel lies strictly
     between the background and `toastSurface`. At 200 ms it is at rest and fully opaque. The Undo button
     invokes its callback when clicked at 0 ms;
  2. **with motion, on exit:** with the message set to `null` or replaced, the old toast is still drawn at
     about 100 ms, but a click on its Undo **does not** invoke the callback, and it is gone after 200 ms;
  3. **status message:** the same entry and exit for `LiveStatusMessage`;
  4. **reduced motion:** on the first frame after showing, the toast is at rest and fully opaque; on the first
     frame after clearing, it is gone;
  5. **semantics:** the resting toast keeps its polite live region and takes no focus.
- **Must not touch:** `UndoToast.kt`, `LocalStateMessages.kt`, `ui/theme/**`, `AppViewModel.kt`, every
  existing test file, and the common list above.
- **RED commit (`test(android): …`).** Add the test file and a `ToastRegion` that only forwards to today's
  instant `if`, extracted unchanged from `IntentionalReadingApp.kt`, so the test compiles. Do **not** edit
  `IntentionalReadingApp.kt` in RED. The motion checks fail and the reduced-motion checks pass. Record which
  is which.
- **GREEN commit (`feat(android): …`).** Implement `design.md` D1 in `ToastRegion.kt`, and replace the
  `Column` in `IntentionalReadingApp.kt` with the call.
- **Definition of done:** all four gates green, with counts against 405 JVM / 46 instrumented;
  `DestinationTransitionInstrumentedTest` and every undo test pass unedited; the diff touches only the
  listed files.
- **Stop and report if:** a pixel or bounds read cannot be stabilised on a paused clock; the extraction
  changes the toast's position at rest by more than 1 px; any existing test changes result.
- **Status:** done. RED `1fbbd84`, GREEN `c98c3c2`. Gates 405 JVM / 55 instrumented (reviewer re-run with `--rerun-tasks`: green). Slice review: PASS.

---

## Slice 2: the card's shadow shows, and the sheet casts one

**Objective.** In light, the card's shadow renders at 8–12% `tertiary`. The Settings sheet casts a drop
shadow, or that part is dropped under the pre-approval.

- **Scenarios:** `spec.md` §3, *the card's shadow is visible and navy* and *the settings sheet casts a
  shadow*.
- **Files, production:** `ui/theme/Tokens.kt` (add `deckShadow`: the field, `tokensFrom`, `blendTokens`),
  `ui/components/ArticleCard.kt` (the two shadow colour arguments at `:228-229` only),
  `ui/screens/settings/SettingsSheet.kt` (the `sheetModifier` at `:114-118` only).
- **Files, tests:** a new `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/ShadowRenderingInstrumentedTest.kt`:
  1. **card, light:** compose Discover with a card and `captureToImage()` the root. In the column of pixels
     directly below the card's bottom edge (from its laid-out bounds), find the darkest pixel. Its distance
     from `bg` toward `tertiary` is 8–12% on each RGB channel's normalised scale, and the red channel
     darkens more than the blue, which is navy's signature on a pale-blue ground;
  2. **card, dark:** the same pixel is darker than or equal to `bg`, with no channel lifted toward
     `tertiary`;
  3. **sheet:** with the sheet open, capture the sheet's window root. The scrim pixel 4 dp above the
     sheet's top edge is darker than the scrim pixel 48 dp above it.

  Edit **one existing test**: `ArticleCardTest.kt:238-251`, *"the deck card shadow uses the theme shadow tint
  for both shadow channels"*. Replace it with an assertion that `ArticleCard` passes `deckShadow` to both
  channels, or delete it and name the new rendered test as its successor. It reads source text, and the
  source it reads is what this slice changes.
- **Must not touch:** `ui/theme/Theme.kt`, `colorScheme.surfaceTint`, `res/values*/themes.xml`, the card's
  elevation and shape, the sheet's shape, scrim and motion, and the common list above.
- **RED commit.** Add only the new test file. Cards 1 and 3 fail; card 2 should pass. Record the measured
  percentage, around 1.4%, and the sheet pixels.
- **GREEN commit.** Implement D2 and D3. **Calibrate:** run test 1, adjust only the light `deckShadow`
  alpha, and repeat until it lands in 8–12%. Report the final alpha and the measured percentage.
  **Sheet:** if test 3 still fails with the modifier in place, revert the `SettingsSheet.kt` change, mark
  test 3 `@Ignore("Owner's pre-approved drop: the sheet shadow does not render — 030 evidence")`, and
  report the pixels. That is the only permitted `@Ignore`.
- **Definition of done:** all four gates green, with counts; `ThemeColorSchemeTest` and
  `AppearanceTransitionTest` pass unedited; the final alpha and the measured percentages are in the report.
- **Stop and report if:** no light alpha up to 1.0 reaches 8%; reaching 8% needs a theme attribute; the dark
  shadow visibly changes; any existing test other than the one listed changes result.
- **Status:** done. RED `9af7d07` (card peak about 1%; sheet near/far pixels identical), GREEN `f5b3282`. Light `deckShadow` alpha **0.65**, rendered peak `#E1E4EB`, about 10.3% navy. **Sheet shadow dropped** under the owner's pre-approval: with the modifier in place the near and far scrim pixels were still identical. It was reverted, and test 3 carries the permitted `@Ignore`. Gates 404 JVM / 58 instrumented, 1 skipped (reviewer re-run with `--rerun-tasks`: green). Slice review: PASS.

---

## Slice 3: wide screens keep a reading width

**Objective.** Content is 18 dp from each edge below 600 dp, 24 dp from 600 dp, and never wider than 680 dp,
centred, on Discover, Read Later and History.

- **Scenarios:** `spec.md` §3, *handsets are unchanged* and *wide screens keep a reading width*.
- **Files, production:** a new `ui/layout/ReadingWidth.kt` (`design.md` D4); `ui/screens/discover/DiscoverScreen.kt`
  (the padding at `:116` and a `BoxWithConstraints` around the `Column`);
  `ui/screens/readlater/ReadLaterScreen.kt` and `ui/screens/history/HistoryScreen.kt` (the horizontal
  content padding only).
- **Files, tests:**
  1. a new JVM `android/app/src/test/kotlin/io/irodriguez/intentionalreading/ui/layout/ReadingWidthTest.kt`:
     360→18, 430→18, 599→18, 600→24, 728→24, 768→44, 1200→260 dp;
  2. a new `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/ReadingWidthInstrumentedTest.kt`:
     for each of Discover, Read Later and History at 360, 390, 430, 600 and 768 dp (`ForcedSize`), the
     content's laid-out left and right insets match the function within 1 dp. At 768, compose the full app
     scaffold once and show that the bottom navigation still spans the full width.

  Edit **two existing tests**, both source-text checks of the very line this slice changes:
  `ReadingSurfacePresentationTest.kt:76` (`"horizontal = spacing.mobileMargin"`) and
  `DiscoverScreenTest.kt:13` (`".padding(spacing.mobileMargin)"`). Change each to assert that the screen uses
  `readingHorizontalPadding`. Keep `ReadingSurfacePresentationTest.kt:77` (`top = spacing.tabletMargin`)
  as it is.
- **Must not touch:** `ui/theme/Spacing.kt` (its values are already right), `ArticleCard.kt`,
  `IntentionalReadingApp.kt`, `DestinationTransition.kt`, and the common list above.
- **RED commit.** Add the two new test files and a `readingHorizontalPadding` that returns `mobileMargin`
  regardless of width, so the tests compile. The JVM test fails from 600 dp up, the instrumented test fails
  at 600 and 768, and both pass at 360–430.
- **GREEN commit.** Implement D4 and wire the three screens. Edit the two listed source-text tests.
- **Definition of done:** all four gates green, with counts; `DestinationTransitionCoverageInstrumentedTest`,
  which checks that the incoming destination covers edge to edge, passes unedited; every 360 dp fold and
  layout test passes unedited.
- **Stop and report if:** any 360 dp test changes result; the destination coverage test fails; a screen's
  scroll stops responding at its edges.
- **Status:** done. RED `ed00f1b`, GREEN `b4761ae`. Gates 411 JVM / 74 instrumented, 1 skipped (reviewer re-run with `--rerun-tasks`: green). Slice review: PASS.

---

## Slice 4: Discover's automatic scrolls are tested under reduced motion

**Objective.** A test proves that both of Discover's automatic scrolls land on the first frame under
reduced motion and animate otherwise.

- **Scenarios:** `spec.md` §3, *Discover's automatic scrolls honour reduced motion*.
- **Files, tests:** a new `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/DiscoverAutoScrollReducedMotionTest.kt`.
  Use a host 360 dp wide and short enough that the card's top offset and the action-reveal target are both
  greater than 0. Check two triggers: (a) the card's article changes, which scrolls to the card's top
  (`DiscoverScreen.kt:77-90`); (b) `isOpened` turns true, which reveals the actions (`:94-111`). With the clock
  paused, assert from `ScrollState.value`, read through a test-owned `ScrollState` hook or from the scroll
  semantics range, that the scroll:
  - with reduced motion, equals its target one frame after the trigger;
  - with motion, lies strictly between start and target at about half the M3 default scroll duration.
- **Files, production:** none, unless the scroll position cannot be observed without a hook. If so, the
  only permitted change is an optional `scrollState: ScrollState = rememberScrollState()` parameter on
  `DiscoverScreen`, with its default unchanged. Report it if used.
- **Must not touch:** every other file, and the common list above.
- **Commits.** This is a characterisation test, because the behaviour already exists. **Commit 1
  (`test(android): …`):** the test, green. **Before committing, prove it can fail:** locally, without
  committing, change both reduced-motion branches to `animateScrollTo`, run the test, record that the
  reduced-motion cases fail, and revert. If the optional parameter is needed, it goes in a separate
  `refactor(android): …` commit **before** the test commit, and that commit must change no behaviour.
- **Definition of done:** all four gates green, with counts; the mutation run's failure output is in the
  report; the production diff is empty, or is only the optional parameter.
- **Stop and report if:** the scroll cannot be observed at all; either target is 0 at every test height.
- **Status:** done. Test `3d3b2ac`, no production change. The mutation run (both reduced-motion branches changed to `animateScrollTo`) failed both reduced-motion cases (expected 924 and 202 px, got 0) and was reverted. Gates 411 JVM / 78 instrumented, 1 skipped (reviewer re-run with `--rerun-tasks`: green). Slice review: PASS.

---

## Slice 2 follow-up: the shadow keeps its rounded corners while the card fades

**Found by the owner's walkthrough, 2026-10-02:** *"in the first half a second when it arrives the corners of
the shadow are square, then they get rounded."* This is a defect slice 2 exposed: the shadow was invisible
before. **Likely cause:** while the card's `graphicsLayer` alpha is below 1, during the §79.5 entrance and the
swipe exit (`ArticleCard.kt:216-229`), Compose draws the card into an offscreen layer the size of its bounds.
The shadow beyond the rounded outline is clipped to that rectangle, so the corners read square. At alpha 1
there is no offscreen layer, and the corners come out round.

- **Scenario:** while the card enters or leaves, its shadow keeps the card's 24 dp rounded corners, and it
  fades with the card.
- **Files, production:** `ui/components/ArticleCard.kt` (the card's layer and shadow modifiers at
  `:212-231` only).
- **Files, tests:** add cases to `ShadowRenderingInstrumentedTest.kt`. With the clock paused at about 50% of
  the entrance and about 50% of the exit, capture pixels at a corner: just outside the rounded outline but
  inside the card's bounding square, and just outside the bounding square. Neither may be darker than the
  same pixel at rest by more than 2/255 per channel, and the shadow's peak below the card must be no
  stronger than at rest, so the shadow fades with the card.
- **Fix:** prefer `compositingStrategy = CompositingStrategy.ModulateAlpha` on the card's `graphicsLayer`,
  which applies alpha without an offscreen buffer. If that cannot satisfy the test, report before trying
  anything else. Do not move the shadow outside the alpha layer, because a full-strength shadow under a
  transparent card is a new defect.
- **RED:** the new cases fail at mid-entrance and mid-exit. **If they do not reproduce the square corners,
  stop and report**, because the cause is then different from the one stated above.
- **Must not touch:** everything outside the two files above. Slice 2's calibrated alpha (0.65), the
  entrance and exit curves, and every swipe test (unedited).
- **Definition of done:** all four gates green, with counts; every 008/013/015/023/024 swipe test and
  `DiscoverScreenLayoutTest` passes unedited.
- **Status:** pending.

---

## Ship bookkeeping (orchestrator, not the implementer)

- `spec.md` §5.2 screenshots and measurements, and §5.3 owner walkthrough, recorded in `evidence.md` with the
  gate output, each RED, the calibrated alpha, the sheet outcome and slice 4's mutation run.
- **Known limitation**, recorded in `evidence.md`: on API 26–27 the card shadow is neutral grey
  (`spec.md` §1.1).
- `specs/backlog.md`: Road to done row 7b marked shipped; a new Debt line for the duplicated Standard
  easing (`design.md` D1); and, if the sheet shadow is dropped, an After done entry with the evidence.
- Reminders: complete *"Road 7b — toast motion, shadows, content cap"*.

---

## Environment (this Mac)

- Export `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
  `ANDROID_HOME=$HOME/Library/Android/sdk`, or Gradle fails before any test runs.
- AVD `Pixel_6_API_34`, already running as `emulator-5554`. The Codex sandbox cannot start the emulator.
- Codex needs approval for `git add` / `git commit` in a worktree; the orchestrator approves after reading the
  diff.
