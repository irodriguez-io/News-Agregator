# 026 — slice plan

**One slice, two commits.** 15 one-identifier edits in 7 files, plus the token file and two tests. It fits
one context window comfortably; splitting it would leave a slice that cannot compile alone, because removing
a field breaks every file still reading it.

No design note: no new dependency, mechanism or structure. The decision is 017's D1, already made.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **Exactly four fields retire:** `accent`, `accentSoft`, `quietInk`, `strongBorder` (`spec.md` §1.2, owner's
   decision 2026-09-30). The other nine legacy fields stay, unrenamed and unchanged.
2. **The mapping is fixed:** `accent` → `primary`, `accentSoft` → `primarySoft`, `quietInk` → `quiet`,
   `strongBorder` → `outlineControl`. **If you find yourself choosing a colour, stop and report it.**
3. **Each consumer line changes one identifier and nothing else.** No reformatting, no import tidying, no
   `dp` literal, no neighbouring cleanup.
4. **No assertion about the nine remaining fields is removed or loosened.** In `assertLegacyMappings`, only
   lines 167, 168, 170 and 171 go.
5. **`animator_duration_scale` is `0` after instrumented runs.** Restore it to `1` before handing the
   emulator back.

---

## Slice 1: each colour in the token object has one name

**Objective.** `IntentionalReadingTokens` no longer declares `accent`, `accentSoft`, `quietInk` or
`strongBorder`, and every former use reads the identically-valued replacement.

- **Scenarios:** all four in `spec.md` §4.
- **Files — production** (under `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`):
  `ui/theme/Tokens.kt`, `ui/components/LocalStateMessages.kt`, `ui/components/ResetConfirmation.kt`,
  `ui/components/ImportConfirmation.kt`, `ui/components/UndoToast.kt`, `ui/components/EditorialHeader.kt`,
  `ui/screens/settings/SettingsSheet.kt`, `ui/screens/discover/DiscoverHeader.kt`. The exact lines are in
  `spec.md` §1.3.
- **Files — tests** (under `android/app/src/test/kotlin/io/irodriguez/intentionalreading/ui/theme/`):
  `ThemeDerivationTest.kt`, `AppearanceTransitionTest.kt`.
- **Must not touch:** any other file; `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/theme/Type.kt`;
  `android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `.github/workflows/**`, `pipeline/**`,
  `config/**`, `docs/**`, `specs/**`, the web runtime.
- **Hub-file edges (execution-model §2.1):** writes — `Tokens.kt`, consumed by every file in the list;
  asserted by — `ThemeDerivationTest`, `AppearanceTransitionTest`, both edited here; receives — nothing. All
  edges close inside this slice.
- **RED commit (`test(android): …`).** Add one test to `ThemeDerivationTest.kt` asserting that
  `IntentionalReadingTokens::class.java.declaredFields` names none of the four, and still names
  `primary`, `primarySoft`, `quiet` and `outlineControl`. Java reflection only — `kotlin-reflect` is not a
  dependency and must not become one. It fails on the parent commit because the four fields exist; record
  the failure output.
- **GREEN commit (`refactor(android): …`).** Remove the four fields from the data class, from
  `blendTokens`, and from `tokensFrom`. Rewrite the 15 consumer call sites per the mapping. Delete
  `ThemeDerivationTest.kt:167, 168, 170, 171`. Remove the four entries from
  `AppearanceTransitionTest.namedColors()` (`:182, 183, 185, 186`) and change its comment at `:32` from
  *"all 26"* to *"all 22"*.
- **Reaches green alone because:** every producer and consumer of the four fields is in this slice.
- **Definition of done:**
  - all four gates green (`spec.md` §5.1), **404 JVM / 28 instrumented**;
  - `spec.md` §5.2's grep returns only the absence test's string literals;
  - `spec.md` §5.3's diff shows identifier-only changes in the consumer files;
  - the diff touches no file outside the lists above.
- **Stop and report if:** any of the four names appears somewhere not listed in `spec.md` §1.3; any test
  other than the new one fails on the RED commit; a replacement name does not exist or the compiler
  reports a type mismatch; any change beyond a rename seems necessary.
- **Status:** pending.

---

## Ship bookkeeping (orchestrator, not the implementer)

- `specs/backlog.md` Road to done row 3: correct *"13"* to the four, citing `017/design.md` D1 and the
  owner's decision; mark shipped.
- `specs/waves/wave-e-note.md:49` and `:228`: add a short correction that four names were owed, not
  thirteen.
- `evidence.md` with gate output, the RED failure, §5.2–5.3, and the §5.4 walkthrough.

---

## Environment (this Mac)

- Export `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
  `ANDROID_HOME=$HOME/Library/Android/sdk`, or Gradle fails before any test runs.
- AVD `Pixel_6_API_34`. The Codex sandbox cannot start the emulator; the orchestrator boots it.
- If a release build was ever installed, `adb uninstall io.irodriguez.intentionalreading` first.
