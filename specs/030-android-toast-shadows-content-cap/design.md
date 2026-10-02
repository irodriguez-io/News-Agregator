# 030 — design note

## D1. One toast host, animated per slot

A new `ui/components/ToastRegion.kt` takes over the body of `IntentionalReadingApp.kt:339-355`, which is
the bottom-centred `Column` holding the Undo toast and the status message, unchanged in position,
padding and order. Each slot is an `AnimatedContent` keyed on its message:

- **Enter:** `fadeIn` + `slideInVertically` from +8 dp, both `tween(200, Standard)`, where Standard is
  `CubicBezierEasing(0.2f, 0f, 0f, 1f)`. The same curve exists as `private val AppearanceStandardEasing`
  (`Theme.kt:61`), but it is private and `Theme.kt` is out of bounds for this slice, so `ToastRegion.kt`
  declares its own private copy. The duplication is recorded as debt, as 023 recorded its own
  (`023/evidence.md`).
- **Exit:** `fadeOut` + `slideOutVertically` to +8 dp, same spec.
- **Replacement:** the same pair, run together.
- **Reduced motion:** `EnterTransition.None` / `ExitTransition.None`, from the same `reducedMotion`
  provider the rest of the app uses (`IntentionalReadingApp.kt:92-94`).
- **The leaving copy is inert:** wrap it in a pointer-input consumer and `clearAndSetSemantics {}`, so a tap
  or a TalkBack action during the 200 ms exit does nothing (`spec.md` §1.1). `UndoToast` and
  `LiveStatusMessage` themselves are unchanged.

`IntentionalReadingApp.kt` keeps its two `LaunchedEffect` timers (`:150-163`, 6 s and 4.5 s). Only the
drawing moves.

## D2. The card shadow gets its own derived role, calibrated

Android multiplies a shadow colour's alpha by the window theme's `ambientShadowAlpha` and
`spotShadowAlpha`. That is why `tertiary` at 10% draws at about 1.4%.

- **Add a derived role `deckShadow`** to `IntentionalReadingTokens` (`ui/theme/Tokens.kt`): light is
  `tertiary` at a calibrated alpha, dark is `bg` at 10%, unchanged. Derive it in `tokensFrom`
  (`Tokens.kt:146`) and add it to `blendTokens` (`Tokens.kt:40-70`), so the appearance cross-fade carries
  it. `ArticleCard.kt:228-229` uses `tokens.deckShadow` for both channels.
- **`colorScheme.surfaceTint` is not touched.** Material also uses it to tint any surface with tonal
  elevation, and `ThemeColorSchemeTest.kt:25-30,73` pins it.
- **Calibrate by measurement.** The implementer chooses the light alpha so that the rendered shadow's
  darkest pixel below the card sits 8–12% of the way from `bg` to `tertiary`, and records the value. No
  window-theme attribute changes: they would change every shadow in the app.

## D3. The sheet shadow is one modifier, and may be dropped

`SettingsSheet.kt`'s `sheetModifier` gains `Modifier.shadow(elevation = 8.dp, shape = shapes.modalSheet,
clip = false)` with the default (black) colour, because §16.2 tints only the deck card. Material's sheet
draws in its own window, so this may not render. The test measures the scrim just above the sheet's top
edge against the scrim 48 dp higher. If no darkening is measurable, the implementer **reverts the
modifier**, keeps the measurement in the report, and the item proceeds under the owner's pre-approved drop.

## D4. One pure function decides the reading width

A new `ui/layout/ReadingWidth.kt`:

```text
readingHorizontalPadding(available: Dp): Dp
  margin = if (available >= 600.dp) tabletMargin else mobileMargin
  return max(margin, (available - contentMaxWidth) / 2)
```

At 360 dp it returns 18; at 600, 24; at 768, 44. It is unit-tested on the JVM. The three screens read their
own width with `BoxWithConstraints` and pass the result where they pass `mobileMargin` today:
- `DiscoverScreen.kt:116`, horizontal only; its vertical padding stays `mobileMargin`;
- `ReadLaterScreen.kt:42` and `HistoryScreen.kt:48`, as the horizontal content padding.

Because padding narrows the content and the scroll container stays full width, scrolling still works across
the whole screen, and §79.1's opaque destination background (027) still covers edge to edge.

## D5. Discover's scroll test proves it can fail

The scrolls already behave correctly, so a new test passes on its first run. To show that it can fail, the
implementer runs it once against an **uncommitted** local change that uses `animateScrollTo` under reduced
motion, records the failure, and reverts the change. Nothing about that run is committed except the record
in the report.
