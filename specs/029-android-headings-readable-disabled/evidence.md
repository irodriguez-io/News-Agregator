# 029 — evidence

**Item:** Screens announce their headings, and disabled text stays readable (Road to done row 7a). No
amendment.
**Branch:** `feat/029-android-headings-readable-disabled`, cut from `main` at `9ea2749`.

| Commit | What |
|---|---|
| `fffa7617011d6206aa70d96d3e7c09d5e4ffcdad` | design: spec, design, slices |
| `59d3dda8cb4a87d53918443e4675e5b58e8cabe9` | spec fix: the Settings section is titled *"Content"* (§2) |
| `47b506d71770b43bd0039725e1546bd75ef2bd79` | slice 1 RED: `HeadingSemanticsInstrumentedTest` (Codex) |
| `e5e29526d21be07cd65128d919c2aedd0a82bf59` | slice 1 GREEN: eleven `heading()` flags (Codex) |
| `028cbecfda467addbbbd72596657181eab8ef347` | slice 1 marked done |
| `53132fa5293aec1c25f63e7b71f0317a9ad5e483` | slice 2 RED: `DisabledTextContrastInstrumentedTest` (Codex) |
| `9beec065d08ed8db0ef74cb825bb5e08b0ab12e1` | slice 2 GREEN: `disabledContentColor = tokens.muted` on two buttons (Codex) |
| `37d129528d9701772f94f169c2c3aee118bb419c` | slice 2 marked done |

---

## 1. Slice 1 — headings

**RED** at `47b506d`: it compiled, and all eight tests failed because no node was a heading. Each failure
reads, for example:

```
everyScreensSectionTitlesAreHeadings_givenDiscoverCard_thenOnlyItsTitlesAreHeadings FAILED
  Reason: Expected exactly '1' node but could not find any node that satisfies:
  ((Text … contains 'Discover') && (Heading is defined))
```

The eight cover: Discover with a card, in error and empty; Read Later populated and empty; History with
Today, Yesterday and Earlier, and empty; and the Settings sheet. Each test asserts:
- every expected title is a heading;
- the full set of heading nodes is exactly the expected texts, with no extras and no duplicates;
- no expected title has a merging ancestor or a content description that would replace its text.

**GREEN** at `e5e2952`: `git diff` on `android/app/src/main` is eleven `Modifier.semantics { heading() }`
additions and their imports, in the eight files `slices.md` lists. No existing test changed. The test file
is byte-identical between RED and GREEN.

## 2. Spec fix found by slice 1's first RED run

The first session stopped correctly: `spec.md` named the Settings section *"Content status"*, but the visible
string `content_status` reads *"Content"* (`strings.xml:66`). The orchestrator had quoted the resource
name. Corrected in `59d3dda`. It is the same section the owner approved at the plan gate.

## 3. Slice 2 — readable busy text

**RED** at `53132fa`. Contrast of the drawn label, composited onto its real background:

| | Light | Dark |
|---|---|---|
| *"Refreshing…"* on `bg` | 1.94:1 | 2.04:1 |
| *"Try again"* on `container` | 1.91:1 | 2.05:1 |

These are worse than the spec's estimate of about 2.3 and 3.1, which assumed Material's default was `fg` at
38%. The slice's stop condition caught the difference. The orchestrator released it, because the test
composites correctly and the fix sets the colour explicitly, so it does not depend on the default.

**GREEN** at `9beec06`: one added argument on each of the two `OutlinedButton`s. The four checks that
passed at RED still pass: the button stays disabled, a tap does not call `onRetry`, the disabled colour
differs from the enabled one, and the enabled colour is `tokens.fg`.

**Not asserted by a test:** that the outline differs when disabled (`spec.md` §3). The outline is
Material's default and this item does not touch it, but outline colour is not exposed to the semantics
tree. The screenshots in §5 show it fading.

## 4. Gates

Both slices were re-run by the reviewer with `--rerun-tasks` on their GREEN heads:
`:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.

| Head | JVM | Instrumented |
|---|---|---|
| baseline `9ea2749` | 405 | 30 |
| slice 1 `e5e2952` | 405 | 38 |
| slice 2 `9beec06` | 405 | **46** |

## 5. Pixel check (orchestrator, debug build of `37d1295`, `emulator-5554`)

The network was throttled (`adb emu network delay 8000`, `speed gsm`) and Refresh was tapped, with
`screencap` run on the device in the same shell command. The label's ink is measured against the
most common pixel in its box:

| | Ink | Background | Ratio |
|---|---|---|---|
| Light | `#454655` (`muted`) | `#F6F8FC` | **8.74:1** |
| Dark | `#A2A3B4` (`muted`) | `#0A0E1A` | **7.73:1** |

Screenshots: `walkthrough/029-refreshing-light.png` and `walkthrough/029-refreshing-dark.png`. In both, the
outline is visibly fainter than the enabled *"Refresh"*. Afterwards the emulator's network, night mode and
`animator_duration_scale` (1) were restored.

## 6. Owner walkthrough (signed release build)

*Pending.* With TalkBack on and its reading control set to *Headings*, swipe down through Discover, Read
Later, History and Settings. Every stop should be a section title from `spec.md` §3, and nothing else.
