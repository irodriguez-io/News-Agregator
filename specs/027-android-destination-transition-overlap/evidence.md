# 027 — evidence

**Item:** One destination's text at a time (Road to done row 4). **Amendment 13** authored at design.
**Branch:** `feat/027-android-destination-transition-overlap`, cut from `main` at `2397ec4`.

| Commit | What |
|---|---|
| `25f7b981be23f170a47ea82e48a54513d25117d7` | design: spec, design, slices, Amendment 13, backlog row |
| `3b914cc` | RED — extraction into `DestinationTransition.kt` + `DestinationTransitionCoverageInstrumentedTest` (Codex) |
| `4e0841c7047da6822bd11f36461750d8e34ce4f6` | GREEN — the `Box` background on the `AnimatedContent` child (Codex) |
| `a35e7f5` | slice 1 marked done |

---

## 1. The defect, confirmed before the fix

A debug build of `main` (`2397ec4`) at `animator_duration_scale 10`, Discover → Read Later, mid-transition:
Read Later's header, copy and empty-state panel are drawn over Discover's card across the whole width,
including the area Read Later has already covered. `walkthrough/before-discover-to-read-later.png`. This is
`spec.md` §1.2's cause exactly — no destination draws a background.

## 2. RED — and why the first attempt had none

**The first session (`codex-027-s1`) wrote both tests and they passed before the fix.** It stopped without
committing, as the brief required. Its incoming slot emitted nothing. The follow-up session (`codex-027-s1b`)
logged the sampled pixels at 80 ms with both slot shapes:

| Incoming slot | Covered pixel | Far-side pixel |
|---|---|---|
| emits nothing | `fff7f9fd` (= `bg`) | `fffe12ff` |
| `Box(Modifier.fillMaxSize())`, like the real screens | `fffe12ff` (magenta through) | `fffe12ff` |

An empty incoming destination shows no bleed-through; a full-size one does. The test now uses a full-size
transparent slot. **Lesson, the same one 025 recorded: a test host must reproduce the real condition, not a
nearby one.** The design note's synthetic host was right in idea and wrong in one detail, and the RED step
caught it.

RED, reproduced by the orchestrator in a throwaway worktree at `3b914cc` with `--rerun-tasks`:

```
movingTowardHistory_coveredAreaShowsOnlyIncomingDestination FAILED
  AssertionError: Covered pixel must equal theme bg: target=HISTORY,
  expected=Color(0.96862745, 0.9764706, 0.99215686, 1.0), actual=Color(0.99607843, 0.07058824, 1.0, 1.0)
movingTowardReadLater_coveredAreaShowsOnlyIncomingDestination FAILED
  AssertionError: Covered pixel must equal theme bg: target=READ_LATER, (same values)
```

## 3. Gates

`spec.md` §6.1, reproduced by the orchestrator in a throwaway worktree at `4e0841c` with `--rerun-tasks`
(73 tasks executed): **404 JVM / 30 instrumented, 0 failures, 0 errors, 0 skipped.** BUILD SUCCESSFUL.
Hosted runs: recorded at the PR.

## 4. §6.2 — §79.1's values are untouched

The moved `emphasizedEasing` and `transitionSpec` are line-for-line identical to `IntentionalReadingApp.kt:271-311`
on `main`, modulo indentation (`diff` of the whitespace-stripped blocks). `durationMillis = 300`, the
`PathEasing` control points, `targetScale = 0.95f`, `targetAlpha = 0.8f` and `destinationSlideDirection` are
unchanged. `DestinationTransitionInstrumentedTest.kt` and `DestinationTransitionTest.kt` are unedited
(`git diff main` over both: empty) and pass.

The background reads `LocalIntentionalReadingTokens.current.bg` inside the `AnimatedContent` child, the same
token the `Scaffold` reads at `IntentionalReadingApp.kt:168` inside `IntentionalReadingTheme`, so §79.4's
blended tokens reach both in the same frame (`spec.md` §5, last scenario; structural, not a test).

## 5. §6.3 — slowed-motion capture after the fix

Debug build of `4e0841c` at `animator_duration_scale 10`, then restored to `1`:
`walkthrough/after-discover-to-read-later.png` and `walkthrough/after-discover-to-history.png`. The incoming
destination covers the outgoing one; the outgoing destination is visible only on the strip not yet reached.
No text is drawn over other text.

## 6. §6.4 — owner walkthrough, signed release build

2026-10-01, emulator `Pixel_6_API_34`, `app-release.apk` built from `4e0841c`, `animator_duration_scale` read
`1`. Read Later ↔ Discover ↔ History in both directions, light and dark. **Owner: pass** — no overlapping
text, and the move still reads as §79.1's directional slide. Release build uninstalled afterwards.

## 7. Known limitations

None introduced. At rest the screen is unchanged by construction (page background = scaffold background);
no pixel comparison at rest was run.
