# 029 — slice plan

**Two slices, each a RED and a GREEN commit, run in order on one branch.** Each slice is small: slice 1
adds one semantics flag at eleven call sites, and slice 2 changes one colour parameter at two. They are split
because they have different objectives and different tests, so a failure in one cannot hide the other. Both
touch `DiscoverHeader.kt` and `DiscoverScreen.kt`, so slice 2 starts from slice 1's head.

---

## Fixed for this item: do not re-decide these mid-implementation

1. **The heading set is `spec.md` §1.1's table, exactly.** Nothing is added and nothing is left out. If
   a title in the table cannot take the flag without a structural change, stop and report.
2. **`Modifier.semantics { heading() }` goes on the title `Text`**, not on a parent (`design.md` D1).
   **No other semantics change:** no `mergeDescendants`, `paneTitle`, `contentDescription`,
   `traversalIndex` or `clearAndSetSemantics` is added, removed or edited.
3. **No visual change in slice 1.** Text, style, colour, padding and layout are byte-identical apart from
   the added modifier.
4. **Slice 2 adds `disabledContentColor = tokens.muted` to the two named `OutlinedButton`s and nothing
   else** (`design.md` D2). Their enabled colours, the shape and the default border stay as they are.
5. **Tests assert the rendered result** (`design.md` D3). A test that reads Kotlin source text does not
   count toward this item's scenarios.
6. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing the
   emulator back.

---

## Slice 1: every section title is a heading, and nothing else is

**Objective.** Each title in `spec.md` §1.1 carries the heading flag, and no other node on those screens
does.

- **Scenarios:** `spec.md` §3, the first four (*every screen's section titles are headings*, *state panels
  title themselves*, *nothing else is a heading*, *a heading still reads as itself*).
- **Files, production** (under `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`):
  `ui/screens/discover/DiscoverHeader.kt` (the *"Discover"* title only),
  `ui/screens/discover/DiscoverScreen.kt` (`StatePanel`'s title only),
  `ui/components/EditorialHeader.kt`, `ui/components/EmptyStatePanel.kt`,
  `ui/components/ArticleCard.kt` (the article title `Text` only, `:244-250`),
  `ui/components/ArticleRow.kt` (the title `Text` only, `:92-98`),
  `ui/screens/history/HistoryScreen.kt` (the period label only, `:142-146`),
  `ui/screens/settings/SettingsSheet.kt` (the four titles at `:166`, `:187`, `:205` and `:254` only).
- **Files, tests:** a new
  `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/HeadingSemanticsInstrumentedTest.kt`.
  Compose each surface through the same fixtures the existing layout tests use (`DiscoverScreenLayoutTest`,
  `ReadingListLayoutTest`, `SettingsSheetInstrumentedTest`): Discover with a card, Discover in error,
  Discover empty, Read Later populated and empty, History with Today, Yesterday and Earlier groups and
  empty, and the Settings sheet. For each one, assert:
  1. every expected title is a node with `SemanticsProperties.Heading` (`onNode(hasText(…) and isHeading())`);
  2. the full set of heading nodes (`onAllNodes(isHeading(), useUnmergedTree = true)`) has exactly the
     expected texts. This is the *"nothing else"* check, and it fails if a heading is over-applied.
- **Must not touch:** any other file, in particular `ui/theme/**`, `ui/components/SharedControls.kt`,
  `CategoryChipRow.kt`, `BottomNavigationBar.kt`, `UndoToast.kt`, `LocalStateMessages.kt`, every existing
  test file, `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `.github/workflows/**`,
  `pipeline/**`, `config/**`, `docs/**`, `specs/**` and the web runtime.
- **RED commit (`test(android): …`).** Add the new test file only. It must compile, and it must fail
  because no node is a heading. Record the failure output.
- **GREEN commit (`feat(android): …`).** Add `Modifier.semantics { heading() }` to each listed title. Where
  a `Text` already has a `modifier`, chain onto it.
- **Reaches green alone because:** the flag changes no layout or text, so no existing test observes it.
- **Existing assertions:** none should change. If any existing test fails, report it **before** editing it
  (execution-model §2.1 rule 5).
- **Definition of done:** all four gates green (`spec.md` §5.1), with counts against 405 JVM / 30
  instrumented; `git diff main -- android/app/src/main` shows only added `heading()` modifiers and their
  imports; the diff touches no file outside the lists above.
- **Stop and report if:** a title sits inside a node that merges its descendants; a title is not a single
  `Text`; History's fixtures cannot produce all three groups; any existing test changes result.
- **Status:** done. RED `47b506d`, GREEN `e5e2952`. Gates 405 JVM / 38 instrumented (reviewer re-run with `--rerun-tasks`: green). Slice review: PASS.

---

## Slice 2: busy Discover controls stay legible

**Objective.** While disabled, *"Refreshing…"* and *"Try again"* draw their text in `muted`, which clears
4.5:1 in both schemes, and they still look and behave disabled.

- **Scenarios:** `spec.md` §3, the last four (*"Refreshing…" is legible*, *"Try again" is legible*,
  *disabled still looks disabled*, *the enabled controls are unchanged*).
- **Files, production:** `ui/screens/discover/DiscoverHeader.kt` (the `OutlinedButton` at `:71-78` only),
  `ui/screens/discover/DiscoverScreen.kt` (`StatePanel`'s `OutlinedButton` at `:215-222` only).
- **Files, tests:** a new
  `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/DisabledTextContrastInstrumentedTest.kt`.
  Compose `DiscoverScreen` in light and in dark, (a) with a card and the refresh in progress and (b) in
  error with a retry in progress. For each control:
  1. the button is not enabled (`assertIsNotEnabled()`), and clicking it does not invoke the callback;
  2. the label's drawn colour, read through `SemanticsActions.GetTextLayoutResult` on the **unmerged**
     `Text` node (the button merges its children) as `layoutInput.style.color`, has a WCAG contrast of at
     least 4.5:1 against `tokens.bg` for (a) and `tokens.container` for (b). Use the test's own copy of the
     formula, as `CategoryChipRowTest.kt:131` does;
  3. that colour differs from the same label's colour when enabled;
  4. when enabled, the label colour is `tokens.fg`, unchanged.
- **Must not touch:** everything slice 1 must not touch, plus slice 1's own test file and every production
  file except the two above.
- **RED commit.** Add the new test file only. It must fail on check 2, at about 2.3:1 light and 3.1:1 dark.
  Record the measured ratios.
- **GREEN commit.** Add `disabledContentColor = tokens.muted` to the two `ButtonDefaults.outlinedButtonColors(...)`
  calls.
- **Definition of done:** all four gates green, with counts reported; `git diff` on the two production
  files shows exactly one added argument each; the diff touches no file outside the lists above.
- **Stop and report if:** the resolved colour cannot be read from the text layout; the RED ratios differ
  from about 2.3 and 3.1 by more than 0.3, which would mean a different default than assumed; either
  control turns out to be drawn by something other than `OutlinedButton`.
- **Status:** pending.

---

## Ship bookkeeping (orchestrator, not the implementer)

- `spec.md` §5.2: screenshots and pixel contrast. §5.3: owner walkthrough. Record both in `evidence.md`
  with the gate output and both RED failures.
- `specs/backlog.md` Road to done row 7a: mark shipped, citing the PR.
- Reminders: complete *"Road 7a — headings and readable disabled text"*.

---

## Environment (this Mac)

- Export `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
  `ANDROID_HOME=$HOME/Library/Android/sdk`, or Gradle fails before any test runs.
- AVD `Pixel_6_API_34`. The Codex sandbox cannot start the emulator; the orchestrator boots it.
- If a release build was ever installed, `adb uninstall io.irodriguez.intentionalreading` first.
