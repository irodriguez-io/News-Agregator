# 030 — evidence

**Item:** The toast eases in, the shadows show, and wide screens keep a reading width (Road to done row 7b).
No amendment.
**Branch:** `feat/030-android-toast-shadows-content-cap`, cut from `main` at `e730efc`.

| Commit | What |
|---|---|
| `96d19ec3f259c927f7166ae6f5b4de1ed06b2d80` | design: spec, design, slices |
| `1fbbd84694e919f9d5860b74897e14dc3281e49c` | slice 1 RED: `ToastMotionInstrumentedTest` plus `ToastRegion` extracted unchanged (Codex) |
| `c98c3c23f4950d81f7647c500eb2159446e2874d` | slice 1 GREEN: toast motion (Codex) |
| `9af7d079e2f1655eabaa872027082ae1cd650992` | slice 2 RED: `ShadowRenderingInstrumentedTest` (Codex) |
| `f5b328294d9c07dc90e821d2545c31ea7a180872` | slice 2 GREEN: `deckShadow`; sheet shadow dropped (Codex) |
| `5c526f3b6ceeb579f602fd2c2f0e000a9d7b7fa5` | slices.md fix: the 600 dp threshold is a named constant (raised by Codex) |
| `ed00f1b4ff9d972ea6c2a72fb1aa3799d54d677b` | slice 3 RED: `ReadingWidthTest`, `ReadingWidthInstrumentedTest` (Codex) |
| `b4761aea99339c64beee65d55fd26d839674d17d` | slice 3 GREEN: `readingHorizontalPadding` on three screens (Codex) |
| `3d3b2acd0315dd68a991cf1d9716e6fafcee397a` | slice 4: `DiscoverAutoScrollReducedMotionTest` (Codex) |

`329e7de`, `340c9bc`, `74bf437` and `9c2eae5` mark each slice done in `slices.md`.

---

## 1. Slice 1 — the toast

**RED** at `1fbbd84`. `ToastRegion` forwarded to today's instant `if`, extracted unchanged. The motion checks
failed for the intended reasons, and the reduced-motion and resting checks passed:

```
toastEasesIn…: Entry starts 8 dp below rest expected:<1820.0> but was:<1799.0>
toastEasesOut…WhenCleared…: exit at 96 ms must retain the old drawing: expected a blended surface … fraction=-0.0
toastEasesOut…WhenReplaced…: old replacement still draws: expected a blended surface … fraction=-0.0
statusMessageMovesTheSameWay…WhenCleared…: exit at 96 ms must retain the old drawing …
```

**GREEN** at `c98c3c2`. `ToastRegion` animates each slot with `AnimatedContent`: 200 ms on
`CubicBezierEasing(0.2f, 0f, 0f, 1f)`, fade 0↔1 and 8 dp travel, with `None` transitions under reduced
motion. A leaving copy consumes pointer input and clears its semantics. In `IntentionalReadingApp.kt` the
old `Column` is replaced by the call, and the timers are untouched. The resting bounds match the old layout
within 1 px. The test file is byte-identical between RED and GREEN, and no existing test changed.

## 2. Slice 2 — the shadows

**RED** at `9af7d07`:

```
light card bg=#F7F9FD peak=#F5F7FB tertiary=#212B56 RGB percentages=[0.93, 0.97, 1.20]
sheet top=277 near4dp=#999DA0 far48dp=#999DA0
```

**GREEN** at `f5b3282`:
- **Card.** A new `deckShadow` token role: light is `tertiary` at **0.65** alpha (0.85 measured 13.5%), and
  dark is `bg` at 10%, unchanged. Rendered peak in the test: `#E1E4EB`, **10.28 / 10.19 / 10.78 %** across
  RGB. `colorScheme.surfaceTint` and the window theme are untouched, and `ThemeColorSchemeTest` and
  `AppearanceTransitionTest` pass unedited. The source-text test `ArticleCardTest` *"the deck card shadow
  uses the theme shadow tint…"* was removed, as `slices.md` permits, and the rendered test is named as its
  successor.
- **Sheet: dropped under the owner's pre-approval (2026-10-01).** With
  `shadow(8.dp, shapes.modalSheet, clip = false)` on the sheet, the scrim 4 dp above the sheet's top edge
  and the scrim 48 dp above it were still identical. Material's sheet draws in its own window, and this
  modifier does not reach it. The modifier was reverted, and the test carries the single permitted
  `@Ignore("Owner's pre-approved drop: the sheet shadow does not render — 030 evidence")`. Recorded in
  `specs/backlog.md` After done.

## 3. Slice 3 — the reading width

**RED** at `ed00f1b`, with a stub that always returns `mobileMargin`. JVM: `expected 24 dp but was 18 dp` from
600 dp up, and `expected 260 dp` at 1200. Instrumented: Discover, Read Later and History fail only at 600 and
768 dp, for example `DISCOVER 768 dp left inset expected:<44.0> but was:<17.78>`.

**GREEN** at `b4761ae`. `readingHorizontalPadding` takes the margins and the 680 dp cap from `spacing`, with a
named 600 dp threshold. Each screen is wrapped in `BoxWithConstraints` and passes `maxWidth`. Most of the diff
is re-indentation. Every caller passes only `fillMaxSize()`, so `maxWidth` is the screen's real width. Two
source-text tests were updated as listed (`ReadingSurfacePresentationTest.kt:76`, `DiscoverScreenTest.kt:13`).
The 360 dp tests and `DestinationTransitionCoverageInstrumentedTest` pass unedited.

**Spec conflict raised by the implementer:** `slices.md` said the threshold came from `spacing`, which has
none. It was corrected in `5c526f3`.

## 4. Slice 4 — Discover's scroll test

`3d3b2ac`, test only, with no production change. Four cases: next card and article opened, each with and
without reduced motion. **The mutation run proves the test can fail.** With both reduced-motion branches
changed locally to `animateScrollTo`, both reduced-motion cases failed:

```
Reduced motion nextCard=true must land on the first frame expected:<202> but was:<0>
Reduced motion nextCard=false must land on the first frame expected:<924> but was:<0>
```

The mutation was reverted before the gates.

## 5. Gates

Each slice was re-run by the reviewer with `--rerun-tasks` on its GREEN head:
`:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.

| Head | JVM | Instrumented |
|---|---|---|
| baseline `e730efc` | 405 | 46 |
| slice 1 `c98c3c2` | 405 | 55 |
| slice 2 `f5b3282` | 404 | 58 (1 skipped: the sheet) |
| slice 3 `b4761ae` | 411 | 74 (1 skipped) |
| slice 4 `3d3b2ac` | 411 | 78 (1 skipped) |
| slice 2 follow-up `5a8e397` | 411 | **79 (1 skipped)** |

## 6. On the emulator (orchestrator, debug build of `9c2eae5`)

**An environment note first.** After 029's walkthrough the emulator kept TalkBack's accessibility floating
button, and it dimmed every app on screen (`bg` read `#EDEFF3`). It was cleared with
`settings delete secure accessibility_button_targets` before any measurement. Instrumented captures are of
the app's own pixels and were not affected.

| Check | Result |
|---|---|
| Discover, left inset of the *"Discover"* title, at 360 / 600 / 768 dp (`wm density` 480 / 288 / 225) | **18 / 24 / 44 dp** (54 px ÷ 3, 43 px ÷ 1.8, 62 px ÷ 1.406) |
| Card shadow, light, full density, darkest pixel just below the card | `#DEE0E8`: **11.7 / 12.1 / 12.6 %** toward `tertiary`, channels balanced, so a navy tint and not neutral |
| Toast, with `animator_duration_scale` 10, 1.1 s after tapping Skip | 4 px below rest, surface `#272B2E` against `#23272A` at rest: still rising and fading in |

The on-screen shadow reads slightly stronger than the test's 10.3%. Both are inside *"approximately
10%"*. Screenshots are in `walkthrough/`. Afterwards the density was reset and `animator_duration_scale`
restored to 1.

## 7. Known limitation

On Android 8.0 and 8.1 (API 26–27) the card shadow is drawn neutral grey, because coloured shadows need API
28 (`spec.md` §1.1).

## 8. Debt recorded

The M3 Standard curve is now declared twice: `Theme.kt:61` (`AppearanceStandardEasing`, private) and
`ToastRegion.kt` (`ToastStandardEasing`). This is in addition to 023's two copies of the Emphasized curve.

## 9. Owner walkthrough (signed release builds of `bc4efcd`, then `daa6644`, 2026-10-02): pass

- **Toast: pass.** *"The toast arrive and leave calmly."*
- **Sheet: drop confirmed.** *"Settings page doesnt have a shadow, but … it's bottom reaches to the bottom
  of the viewport so it will never show shadow, ther is just no space to show it."* Only the top edge could
  cast one, and §2 measured that it does not render there.
- **Card: defect found.** *"the card does stand of the page but in the first half a second when it arrives
  the corners of the shadow are square, then they get rounded."* Fixed in the slice 2 follow-up:
  - **Cause, confirmed on the device** (`screenrecord`, `animator_duration_scale` 5). While the card's layer
    alpha is below 1, the card is drawn into an offscreen buffer the size of its bounds, so the shadow is
    clipped to that rectangle. What remains is a hard square behind each rounded corner, and nothing below
    the card.
  - **RED `dc3fac8`:** mid-entrance, the peak below the card is `#F7F9FD` (no shadow) against `#EAECF3` at
    rest.
  - **Exit case withdrawn, `30fc586`:** in the test host the exiting card is already transparent at 75 ms.
  - **GREEN `5a8e397`:** `compositingStrategy = CompositingStrategy.ModulateAlpha` on the card's layer, which
    applies alpha without an offscreen buffer.
  - **After:** the recording shows a soft, rounded shadow mid-entrance
    (`walkthrough/030-shadow-corner-before-after.png`, before on the left, after on the right).
  - Gates: 411 JVM / 79 instrumented, 1 skipped.
  - **Owner re-check on the release build of `daa6644`: pass.** *"Card shadow is fixed."*
