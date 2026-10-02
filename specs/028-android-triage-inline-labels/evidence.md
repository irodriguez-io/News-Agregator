# 028 — evidence

**Item:** The triage controls say what they do (Road to done row 5). **Amendment 14** authored at design.
**Branch:** `feat/028-android-triage-inline-labels`, cut from `main` at `8602be0`.

| Commit | What |
|---|---|
| `3fff6091a9aea61fd27da15d0740bc521348a9c9` | design: spec, design, slices, Amendment 14, backlog row |
| `031e5e3be3d68bbba6a20820396cf186b889756c` | RED: the five listed assertions edited (Codex) |
| `06e70c4c25aaa454cd6f37aef1bfeb6683ace4cb` | slices.md: fit check corrected, owner-approved (§2) |
| `97dae9ca87d00c1fee38226d0f38e5cf9c6c37fb` | test: the corrected fit check (Codex) |
| `fc8190c04f15811a49f251d7e2e81536f71944cc` | GREEN: `InlineTriageControl` replaces `CircularTriageControl` (Codex) |
| `28b43a915d64474406edd2c0d0e6aac50d1ed97e` | slice-review fix, RED: focus indication required (Codex) |
| `826f06c568e44d9169b9691e6fe0be0bfd565df6` | slice-review fix: `ripple()` restores §38 focus (Codex) |
| `f57da6ceab239927f113529b30e0228f7d629e36` | slice 1 marked done |

---

## 1. RED

At `031e5e3`, compiled and failing for the intended reason (Codex's run, `--rerun-tasks`):

```
SharedControlsTest > the inline triage control has a 48 dp minimum, 4 dp padding and the secondary label
  AssertionError: InlineTriageControl is missing
ArticleCardTest > the adopted triage controls keep compliant targets and accessible names
  AssertionError: inline triage control count expected:<2> but was:<0>
ArticleCardTest > the action rail uses item 018 shared controls with the existing callbacks
  AssertionError: action rail control types expected:<[InlineTriageControl, FilledPrimaryControl,
  InlineTriageControl]> but was:<[CircularTriageControl, FilledPrimaryControl, CircularTriageControl]>
DiscoverScreenLayoutTest > longDatasetCardFitsAboveTheFoldAt{360,411}Dp
  AssertionError: Failed: assertExists. ... (ContentDescription = 'Skip, not interested')
```

## 2. The fit check, as planned, could not pass

`slices.md` asked for `getTextLayoutResult` with `lineCount == 1 && !hasVisualOverflow`. With GREEN in place
both fold tests failed on `Skip` at **360 and 411 dp**. A failure at 411 dp, with about 50 dp more room,
pointed at the check itself rather than the row. Codex measured it:

| Label | lineCount | layout width | maxIntrinsicWidth | reported paragraph width at 360 / 411 |
|---|---|---|---|---|
| Skip | 1 | 76 px | 75.5 px | 692 / 826 px |
| Save | 1 | 84 px | 83.5 px | 562 / 696 px |
| Read article | 1 | 209 px | 208.5 px | 277 / 411 px |

Every label is one line and wide enough for its text. On Compose 1.12.0 the `GetTextLayoutResult`
semantics action rebuilds the paragraph at the incoming max width but keeps the measured size, so
`didOverflowWidth` (`size.width < multiParagraph.width`) is true for every label at every width. The
reported paragraph width grows with the viewport while the label does not.

**Owner approved the correction on 2026-10-01:** `lineCount == 1`, `!isLineEllipsized(0)` and
`multiParagraph.maxIntrinsicWidth <= size.width`. It is as strict as the plan intended, and it now passes at
both widths. The measured control widths match `spec.md` §1.3's estimates: Skip ≈54 dp, Save ≈57 dp,
`Read article` ≈154 dp.

## 3. Slice review: one finding, fixed

`InlineTriageControl` passed `indication = null` to `clickable`, so Skip and Save showed no focus indication.
That breaks §38: *"Every focusable control must display a visible focus ring"*. The replaced control had the
Material ripple through `OutlinedIconButton`. Fixed failing-first (`28b43a9` → `826f06c`) with Material 3
`ripple()`, clipped by the existing pill `graphicsLayer`. Re-review: **PASS**.

Keyboard focus, reached by Tab on the emulator after the fix:
`walkthrough/item028-focus-{skip,save}-{light,dark}.png`. The indication is a secondary-tinted pill on the
focused control, in both themes. It is faint in dark; see §7.

## 4. Gates

`spec.md` §6.1, run by the orchestrator at `826f06c` with `--rerun-tasks`: **405 JVM / 30 instrumented,
0 failures, 0 skipped.** That is one JVM test more than baseline (the focus test); instrumented is unchanged.
The swipe tests from 008, 013, 015, 023 and 024 and `DestinationTransitionInstrumentedTest` pass unedited.
`git diff main -- android/app/src/main` shows no change to `FilledPrimaryControl`, `TonalSecondaryControl`,
the swipe cue, `not_interested` or `save_for_later`. Hosted runs are recorded at the PR.

## 5. §6.2: screenshots

Debug build of `826f06c`, emulator `Pixel_6_API_34`. 411 dp is the native density (420); 360 dp is
`wm density 480`. `walkthrough/item028-discover-{360,411}dp-{light,dark}.png`. `← Skip` · `Read article ↗` ·
`Save →` sit on one row at both widths and in both themes. Skip and Save are secondary-coloured and
borderless, and the primary control is unchanged.

## 6. §6.3: TalkBack spot check

**Accessibility tree, checked (debug build, `uiautomator dump`):** dismiss is one node,
`content-desc="Skip, not interested"`, and save is one node, `content-desc="Save for later"`. Each is
focusable, with **no child text nodes**, so neither the arrow nor the visible word is exposed separately.
Compare `Read article`, whose child `Text` nodes are visible in the same dump. The instrumented check in
`DiscoverScreenLayoutTest` asserts the same (one `ContentDescription`, no merged `Text`).

**Spoken output was not observed.** TalkBack was enabled on the emulator, but adb taps activated the
controls instead of moving TalkBack focus: one article was saved and one dismissed, in emulator data only.
TalkBack was then turned off. Hearing the announcement moves to the owner walkthrough (§6.4).

## 7. Known limitations and open points for the owner

- **Focus visibility in dark** is the Material 3 focus state layer: present but faint (§3 screenshots).
  §38 asks that it be "obvious in both light and dark". This is the same platform indication every shared
  control uses, so whether it is obvious enough is the owner's judgement, and it is not specific to this item.
- **The TalkBack announcement is unheard** (§6).

## 8. §6.4: owner walkthrough, signed release build

Pending.
