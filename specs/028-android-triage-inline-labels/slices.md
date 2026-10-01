# 028 — slice plan

**One slice, two commits.** Three production files, three test files, and one control replaced at its only
call site. It fits comfortably in one context window.

**Amendment 14 and the `docs/v1/**` edits land with the design commit**, before slice 1, as Amendments 10–13
did.

---

## Fixed for this item: do not re-decide these mid-implementation

1. **Visible:** `← Skip` and `Save →`. **Announced:** *"Skip, not interested"* and *"Save for later"*, once
   each, with no arrow (`design.md` D2).
2. **Borderless, 4 dp horizontal padding, 4 dp arrow-to-word gap, `labelLarge`, `tokens.secondary`, and at
   least 48×48 dp** (`design.md` D1). **`SharedControls.kt` must keep passing its literal guard**
   (`SharedControlsTest`, *"the shared control source names no colour radius dimension or font literal"*,
   unedited). Take the 4 dp values from `spacing.baseUnit`, the minimum from the existing
   `MinimumTouchTarget` (`Dp(48f)`) and the press-overlay shape from `shapes.pill`. Do not write a `4.dp`,
   `48.dp` or `CircleShape` literal.
3. **`FilledPrimaryControl`, `TonalSecondaryControl`, the rail's `stackGap` gaps and `Read article`'s
   content are byte-identical before and after.**
4. **The swipe cue (`ArticleCard.kt:376-385`) is untouched**, and so are `not_interested` and
   `save_for_later`.
5. **`CircularTriageControl` is replaced, not kept.** It has one caller.
6. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing the
   emulator back.

---

## Slice 1: the triage controls show their word beside their arrow

**Objective.** The Discover rail is `← Skip` · `Read article ↗` · `Save →` on one row at 360 dp, with each
triage control announcing one name that begins with its visible word.

- **Scenarios:** all seven in `spec.md` §4.
- **Files, production** (under `android/app/src/main/`):
  `kotlin/io/irodriguez/intentionalreading/ui/components/SharedControls.kt`,
  `kotlin/io/irodriguez/intentionalreading/ui/components/ArticleCard.kt` (the `ArticleActions` function
  only), `res/values/strings.xml` (three additions only).
- **Files, tests:** `android/app/src/test/kotlin/io/irodriguez/intentionalreading/ui/components/SharedControlsTest.kt`,
  `android/app/src/test/kotlin/io/irodriguez/intentionalreading/ui/components/ArticleCardTest.kt`,
  `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/DiscoverScreenLayoutTest.kt`.
- **Must not touch:** any other file, in particular `ui/theme/**`, `ui/screens/**`, other `ui/components/**`
  files, the swipe cue in `ArticleCard.kt`, `DestinationTransitionInstrumentedTest.kt` (it clicks by
  *"Save for later"*, which is unchanged), `android/app/build.gradle.kts`,
  `android/gradle/libs.versions.toml`, `.github/workflows/**`, `pipeline/**`, `config/**`, `docs/**`,
  `specs/**` and the web runtime.
- **Hub-file edges (execution-model §2.1):**
  - `SharedControls.kt` is written by this slice. It is asserted by `SharedControlsTest` and
    `ArticleCardTest` (both edited here), and it is received only by `ArticleCard.kt`'s `ArticleActions`
    (edited here).
  - `ArticleCard.kt` is asserted by `ArticleCardTest` (source-reading tests) and `DiscoverScreenLayoutTest`
    (fold gate), both edited here. It is also asserted by the swipe tests from 008, 013, 015, 023 and 024,
    which must stay green **unedited**.

  Every edge closes inside this slice.
- **RED commit (`test(android): …`).** Edit these existing assertions; each one is listed with its reason.
  - `SharedControlsTest`, *"the circular triage control is 56 dp with the secondary outline"*: replace it
    with *"the inline triage control has a 48 dp minimum, 4 dp padding and the secondary label"*. The
    56 dp circle is retired by Amendment 14.
  - `SharedControlsTest`, *"a circular triage control carries a non-empty accessible name"*: rename only.
    The behaviour is kept.
  - `ArticleCardTest`, *"the action rail uses item 018 shared controls…"* (`:306-323`): the expected types
    become `InlineTriageControl`, `FilledPrimaryControl`, `InlineTriageControl`, with the same three
    `onClick` callbacks.
  - `ArticleCardTest`, *"the adopted triage controls keep compliant targets and accessible names"*
    (`:325-336`): two `InlineTriageControl` calls, minimum target ≥ 48 dp, and accessible names
    `skipNotInterestedLabel` and `saveForLaterLabel`.
  - `DiscoverScreenLayoutTest`, `assertLongDatasetCardFits` (`:509-532`): find dismiss by
    `"Skip, not interested"`. Add three checks at both widths:
    1. the `Skip`, `Save` and `Read article` text nodes each lay out on one line with no visual overflow
       (`getTextLayoutResult`; `lineCount == 1 && !hasVisualOverflow`);
    2. each triage control's node has exactly one `ContentDescription` and no `Text` in its merged
       semantics;
    3. each triage control's bounds are ≥ 48 dp in both dimensions.

  The new and edited tests fail on this commit because `InlineTriageControl` does not exist and
  `"Skip, not interested"` is not found. **RED must compile.** If a reference to the missing symbol stops
  compilation, assert the rail's control types through `ArticleCardTest`'s source-reading idiom instead of
  calling the symbol. Record the failure output.
- **GREEN commit (`feat(android): …`).**
  - In `SharedControls.kt`: add `InlineTriageControl` per `design.md` D1 and D2, and remove
    `CircularTriageControl`, `TriageSize`, `TriageOutlineWidth` and the matching layout fields. Rename
    `triageOutline` to `triageLabel`.
  - In `ArticleActions`: build the two new controls with `Text("←")` and `Text(stringResource(R.string.skip))`,
    then `Text(stringResource(R.string.save))` and `Text("→")`, each pair separated by `spacing.baseUnit`,
    all in `labelLarge`.
  - In `strings.xml`: add `skip`, `save` and `skip_not_interested`.
- **Reaches green alone because:** the only producer (`SharedControls.kt`), its only consumer
  (`ArticleActions`) and every test that asserts against either are in this slice.
- **Existing assertions:** exactly the five listed above. If any other test fails, report it **before**
  editing it (execution-model §2.1 rule 5).
- **Definition of done:**
  - all four gates green (`spec.md` §6.1), with counts reported against 404 JVM / 30 instrumented;
  - the swipe tests and `DestinationTransitionInstrumentedTest` pass unedited;
  - fixed points 3 and 4 hold: `git diff main -- android/app/src/main` shows no change to
    `FilledPrimaryControl`, `TonalSecondaryControl` or the swipe cue;
  - the diff touches no file outside the lists above.
- **Stop and report if:** the row does not fit at 360 dp unless the primary control or the gaps change; any
  label wraps; any swipe test needs editing; TalkBack semantics need more than the one modifier;
  `CircularTriageControl` turns out to have a second caller.
- **Status:** pending.

---

## Ship bookkeeping (orchestrator, not the implementer)

- `spec.md` §6.2 screenshots, §6.3 TalkBack check and §6.4 owner walkthrough, recorded in `evidence.md` with
  the gate output and the RED failure.
- `specs/backlog.md` Road to done row 5: mark shipped, citing the PR.
- Reminders: complete *"Road 5 — build 028: inline-arrow triage labels"*.

---

## Environment (this Mac)

- Export `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
  `ANDROID_HOME=$HOME/Library/Android/sdk`, or Gradle fails before any test runs.
- AVD `Pixel_6_API_34`. The Codex sandbox cannot start the emulator; the orchestrator boots it.
- If a release build was ever installed, `adb uninstall io.irodriguez.intentionalreading` first.
