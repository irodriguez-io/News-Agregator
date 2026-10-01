# 027 — slice plan

**One slice, two commits.** One wrapper, one extraction, one test file. It fits one context window
comfortably; there is nothing to split.

**Amendment 13 and the `docs/v1/**` edits land with the design commit**, before slice 1, approved at the
plan gate — as Amendments 10–12 did.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **The incoming destination gets an opaque background; nothing else about the motion changes.**
   `durationMillis = 300`, the Emphasized `PathEasing`, `targetScale = 0.95f`, `targetAlpha = 0.8f`, the
   `initialOffsetX` and `destinationSlideDirection` are byte-identical before and after (`spec.md` §6.2).
2. **The colour is `LocalIntentionalReadingTokens.current.bg`, read inside the composition** (`design.md`
   D1). No literal, no `MaterialTheme.colorScheme`, no colour captured outside the child.
3. **The background goes on the `AnimatedContent` child, once.** `ReadLaterScreen`, `HistoryScreen`,
   `DiscoverScreen` and everything under `ui/screens/**` and `ui/components/**` are not touched.
4. **The extraction moves code; it does not change it** (`design.md` D2). The reduced-motion branch, the
   `initialState == targetState` branch and the `label` move as they are.
5. **No `zIndex`, no `targetContentZIndex`.** If the incoming destination is not already drawn on top, stop
   and report.
6. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing the
   emulator back.

---

## Slice 1: the incoming destination covers the outgoing one

**Objective.** Part-way through any destination transition, nothing of the outgoing destination is visible
inside the area the incoming destination has slid over.

- **Scenarios:** all six in `spec.md` §5.
- **Files — production** (under `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`):
  `ui/IntentionalReadingApp.kt`, `ui/DestinationTransition.kt`.
- **Files — tests:** new `android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/DestinationTransitionCoverageInstrumentedTest.kt`.
- **Must not touch:** any other file — in particular `DestinationTransitionInstrumentedTest.kt`,
  `test/…/ui/DestinationTransitionTest.kt`, `ui/screens/**`, `ui/components/**`, `ui/theme/**`;
  `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `.github/workflows/**`, `pipeline/**`,
  `config/**`, `docs/**`, `specs/**`, the web runtime.
- **Hub-file edges (execution-model §2.1):** writes — `IntentionalReadingApp.kt` (consumes the extracted
  host) and `DestinationTransition.kt` (gains it); asserted by — `DestinationTransitionInstrumentedTest` and
  `DestinationTransitionTest`, both unedited and both must stay green; receives — nothing. All edges close
  inside this slice.
- **RED commit (`test(android): …`).**
  - Move the `AnimatedContent` block at `IntentionalReadingApp.kt:271-385` — the `emphasizedEasing`
    `remember` and the `AnimatedContent` call — into an `internal @Composable` in `DestinationTransition.kt`
    taking `destination: Destination`, `reducedMotion: () -> Boolean`, `modifier: Modifier` and
    `content: @Composable (Destination) -> Unit`. `IntentionalReadingApp` calls it with today's `when` as
    the slot. No behaviour change.
  - Add `DestinationTransitionCoverageInstrumentedTest` hosting the extracted composable inside
    `IntentionalReadingTheme` with `reducedMotion = { false }`, `mainClock.autoAdvance = false`, and slot
    content where `DISCOVER` is a full-size solid `Color.Magenta` box and `READ_LATER` / `HISTORY` are empty.
    Two tests, one per direction (Discover → Read Later enters from the left; Discover → History from the
    right). Each switches destination, advances the clock to part-way through the slide (around `50ms`,
    when Emphasized has carried the incoming destination about 40% of the width — `0.40` at 50 ms,
    `specs/024-android-card-arrival-gap/spec.md` §1.2), captures the host, and asserts that a pixel
    inside the covered area — about 10% of the width in from the edge the incoming destination entered
    from — equals the theme's `bg`, and that a pixel about 20% in from the far edge is **not** `bg` (so the
    test cannot pass with no transition, or with the outgoing destination simply gone). Not closer to the
    far edge: the outgoing destination's `0.95` scale leaves a `bg` margin there. Follow `DiscoverScreenLayoutTest`'s pixel-sampling idiom (`:449-458`) and its colour-distance
    tolerance.
  - Both new tests fail on this commit because the covered pixel is magenta at `0.8` over `bg`. Record the
    failure output.
- **GREEN commit (`fix(android): …`).** In the extracted composable, wrap the slot call in
  `Box(Modifier.fillMaxSize().background(LocalIntentionalReadingTokens.current.bg)) { content(it) }`.
  Nothing else.
- **Reaches green alone because:** the only producer and both consumers of the extracted composable are in
  this slice, and the existing transition tests exercise it unedited.
- **Definition of done:**
  - all four gates green (`spec.md` §6.1): **404 JVM / 30 instrumented** (28 + 2);
  - the existing `DestinationTransitionInstrumentedTest` and `DestinationTransitionTest` pass unedited;
  - `spec.md` §6.2 holds;
  - the GREEN diff is the `Box` wrapper and its imports only;
  - the diff touches no file outside the lists above.
- **Stop and report if:** the incoming destination is not drawn above the outgoing one; the covered pixel is
  not `bg` after GREEN; either existing transition test needs editing; the extraction needs a parameter
  beyond the four named; any change to §79.1's values seems necessary.
- **Status:** pending.

---

## Ship bookkeeping (orchestrator, not the implementer)

- `spec.md` §6.3 slowed-motion captures and §6.4 owner walkthrough, recorded in `evidence.md` with gate
  output and the RED failure.
- `specs/backlog.md` Road to done row 4: mark shipped, citing the PR.
- Reminders: complete *"Road 4 — stop tab transitions showing both labels"*.

---

## Environment (this Mac)

- Export `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
  `ANDROID_HOME=$HOME/Library/Android/sdk`, or Gradle fails before any test runs.
- AVD `Pixel_6_API_34`. The Codex sandbox cannot start the emulator; the orchestrator boots it.
- If a release build was ever installed, `adb uninstall io.irodriguez.intentionalreading` first.
