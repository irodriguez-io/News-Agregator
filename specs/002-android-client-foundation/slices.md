# 002 — slice plan

Sized **L → 4 ordered slices**. One item branch (`feat/002-android-client-foundation`), one PR.
Each slice closes as a failing-first test commit plus an implementation commit, and must fit one fresh
implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

Fixed for every slice — do not re-decide these mid-implementation:

- compileSdk **37**, targetSdk 36, minSdk 26, JDK 17 source/target. compileSdk was 36 until the
  slice 1 review: the AndroidX stack pinned for this item requires 37, and the original reason for
  choosing 36 — that `platforms/android-37` was not installed and there are no `cmdline-tools` — was
  wrong. AGP installs the platform itself against the already-accepted licence. Verified: platform
  37.0 auto-installs, `checkDebugAarMetadata` passes with Compose 1.12.0 / Core 1.19.0 /
  Activity 1.13.0 / Lifecycle 2.11.0, and the debug APK builds. targetSdk stays 36 — inheriting
  Android 17 behaviour changes is its own slice, not a side effect of this one.
- AGP 9 removed `kotlinOptions {}` (use `kotlin { compilerOptions { } }`), defaults `buildConfig` and
  `resValues` to `false`, and now defaults `targetSdk` to `compileSdk` — so set `targetSdk` explicitly.
- Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*`.
- The manifest declares **zero permissions**.
- No image-loading, dependency-injection, or navigation library is added.
- Every version is declared in `android/gradle/libs.versions.toml` and nowhere else.

## Slice 0 (prerequisite, not an implementer slice)

`android/gradle/wrapper/gradle-wrapper.jar` is a binary and must be generated, not authored. There is no
system Java and no `gradle` on `PATH`; Android Studio's bundled JBR 21 is at
`/Applications/Android Studio.app/Contents/jbr/Contents/Home`. Generate the wrapper for Gradle 9.5.0
before dispatching slice 1, and record its checksum in `evidence.md`.

## Slice 1: Gradle foundation, dataset model, validator, and the snapshot asset

- **Scenarios:** "the bundled dataset loads and validates"; "a contract-violating dataset claims
  nothing" (validator half — the error *state* is slice 3).
- **Files:** `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`,
  `android/gradle/libs.versions.toml`, `android/gradle/wrapper/**`, `android/gradlew`,
  `android/gradlew.bat`, `android/.gitignore`, `android/README.md`, `android/app/build.gradle.kts`,
  `android/app/proguard-rules.pro`, `android/app/src/main/AndroidManifest.xml`,
  `android/app/src/main/res/**`, `android/app/src/main/assets/sample_articles.json`,
  `«pkg»/{IntentionalReadingApplication,MainActivity}.kt`, `«pkg»/di/AppContainer.kt`,
  `«pkg»/data/DatasetJson.kt`, `«pkg»/data/dto/ArticleDatasetDto.kt`,
  `«pkg»/data/local/DatasetSource.kt`, `«pkg»/data/DatasetRepository.kt`,
  `«pkg»/domain/model/{Article,ArticleDataset,Category,ContentTypeId}.kt`,
  `«pkg»/domain/validation/{DatasetValidator,DatasetResult}.kt`, and the matching
  `android/app/src/test/kotlin/**` for `DatasetValidatorTest` and `SampleDatasetTest`.
- **Must not touch:** everything outside `android/`. `MainActivity` is a placeholder in this slice; no
  theme, no screens, no navigation.
- **Reuse:** port `js/data/validation.js:119-225` rule for rule (article rules `:118-183`, dataset rules
  `:197-225`, the timestamp calendar check `:80-93`); the frozen enumerations at
  `pipeline/constants.py:3-24` and `js/data/validation.js:3-26`; the canonical ordering key at
  `pipeline/retention.py:12-20`. Fetch the asset with the `curl` command in `design.md`.
- **Definition of done:**
  - `./gradlew :app:assembleDebug` succeeds;
  - `Json` is configured with `ignoreUnknownKeys = false`, `explicitNulls = true`, `isLenient = false`,
    `coerceInputValues = false`, and no DTO property carries a default value, so a *missing* key and an
    explicit `null` are distinguishable;
  - `DatasetValidator` returns a result type and never throws across the data boundary, with
    `schemaVersion != 1` carrying a code distinct from "malformed";
  - `DatasetValidatorTest` is table-driven from one valid document, mutating a single field per case, and
    covers at minimum: unsupported `schemaVersion`, an unknown top-level key, a missing `author` key
    versus an explicit `null`, a 19-character ID, an uppercase-hex ID, an empty title, a 501-character
    title, a punctuation-only title, a non-HTTP URL, an unknown category, an unknown content-type ID, a
    malformed source ID, a duplicate tag ID, seven tags, `2026-02-30T12:00:00Z`, a timestamp without `Z`,
    a broken `score.base` sum, an out-of-range score component, `articleCount` disagreeing with the
    article count, `enabled != successful + failed`, a duplicate article ID, and 501 articles;
  - `SampleDatasetTest` reads the **shipped asset bytes** off the test classpath — enabled by
    `sourceSets.getByName("test").resources.srcDir("src/main/assets")` — passes them through the
    production validator, and additionally asserts canonical sort order, that every `source.id` is one of
    the twenty approved IDs, and that every `tags[].id` exists in the taxonomy, with those ID lists
    inlined in test source only;
  - `android/README.md` records the asset's provenance (URL, fetch date, snapshot `generatedAt`, ETag),
    the refresh command, the Pages-allowlist exclusion, and the `JAVA_HOME` note;
  - the snapshot's profile is recorded in `evidence.md` — article count, per-category counts, failed
    source count, and the null/empty counts per nullable field — because `spec.md` §5 drives the manual
    walkthrough from it rather than from hard-coded numbers;
  - `./gradlew :app:testDebugUnitTest` green.
- **Note:** this slice settled whether `org.jetbrains.kotlin.plugin.serialization` applies cleanly under
  AGP 9's built-in Kotlin. If it does not, the fallbacks in order are (a) `android.builtInKotlin=false`
  plus the `org.jetbrains.kotlin.android` plugin, recorded in `evidence.md` as dated debt because the
  flag is removed in AGP 10, or (b) drop the plugin and read fields explicitly from
  `Json.parseToJsonElement` inside `DatasetValidator`, which changes no behavior. Report which path was
  taken.
- **Status:** done (gate: PASS, slice review)

## Slice 2: article status state machine and screen-state derivation

No Compose in this slice. Every file is pure Kotlin with no Android imports.

- **Scenarios:** the state-transition halves of "dismissing advances the deck", "saving reaches Read
  Later", "an opened article is acknowledged and held", and "marking read reaches History under Today";
  the derivation half of "Discover offers exactly one article" and "a category with no articles reaches
  the empty state".
- **Files:** `«pkg»/domain/model/{ArticleStatus,ArticleRecord,ArticleAction}.kt`,
  `«pkg»/domain/state/{ArticleStateMachine,DiscoverDeck}.kt`, `«pkg»/ui/AppUiState.kt`,
  `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/ui/format/{RelativeTime,Labels}.kt`, the per-screen UiState
  types under `«pkg»/ui/screens/*/`, and `android/app/src/test/kotlin/**` for
  `ArticleStateMachineTest`, `UiStateMapperTest`, and `RelativeTimeTest`.
- **Must not touch:** anything from slice 1 other than adding to it; no Compose, no theme, no
  `MainActivity` change, nothing outside `android/`.
- **Reuse:** the allowed-from table and idempotent no-ops at `js/state/article-state.js:70-86`; the
  per-action timestamp effects at `:96-138`; eligibility at `js/state/selectors.js:3-5`; ordering at
  `:12-23`; badge counts at `:25-34`; aggregates at `:36-58`; formatting at `js/ui/format.js:1-66`; the
  Discover body-state precedence and copy at `js/ui/discover.js:276-336`.
- **Definition of done:**
  - `ArticleRecord` holds the full `Article` snapshot, not an ID;
  - the transition function is pure, takes an injected `Instant`, and returns applied, unchanged, or
    invalid — never throws;
  - the allowed-from sets match `js/state/article-state.js:70-77` exactly: `open` from unseen/opened/
    saved/read, `save` from unseen/opened/saved, `dismiss` from unseen/opened/dismissed, `mark_read` from
    unseen/opened/saved/read, `mark_unread` from read only, `remove` from saved only;
  - `open` sets `status = "opened"` only when no record existed, and `openedAt` is write-once;
  - the four idempotent no-ops return "unchanged" with timestamps preserved;
  - `ArticleStateMachineTest` covers every cell of the allowed-from table, every no-op, and every
    rejection;
  - Discover state is a sealed type whose four cases make the loading/error/empty/card precedence a
    compile-time property, with the available count and the remaining count both derived;
  - Read Later orders by `savedAt` descending; History groups into Today / Yesterday / Earlier against an
    injected clock and zone, omitting empty groups;
  - `RelativeTimeTest` covers the `Now` / `{n}h` / `{n}d` / absolute ladder and the 31-day cutover;
  - `./gradlew :app:testDebugUnitTest` green.
- **Status:** done (gate: PASS, slice review)

## Slice 3: theme, navigation shell, and the three screens

The largest surface. Signatures are fixed here so the implementer does not spend context designing them.
If the slice overflows a context window, it splits at the marked boundary into **3a** (theme, shell,
Discover) and **3b** (Read Later, History, Settings) — pre-authorized, no re-approval needed.

- **Scenarios:** all ten, end to end.
- **Files (3a):** `«pkg»/ui/theme/{Color,Tokens,Type,Theme}.kt`, `«pkg»/ui/AppViewModel.kt`,
  `«pkg»/ui/IntentionalReadingApp.kt`, `«pkg»/MainActivity.kt`,
  `«pkg»/ui/components/{BottomNavigationBar,EditorialHeader,CategoryChipRow,ArticleCard}.kt`,
  `«pkg»/ui/screens/discover/DiscoverScreen.kt`, navigation vector drawables and strings under
  `android/app/src/main/res/**`, and `AppViewModelTest`.
- **Files (3b):** `«pkg»/ui/components/{StatBand,ArticleRow}.kt`,
  `«pkg»/ui/screens/readlater/ReadLaterScreen.kt`, `«pkg»/ui/screens/history/HistoryScreen.kt`,
  `«pkg»/ui/screens/settings/SettingsSheet.kt`.
- **Must not touch:** `«pkg»/domain/**` and `«pkg»/data/**` — if a screen needs a value those layers do
  not expose, that is a slice 2 defect to report, not a place to compute presentation logic;
  anything outside `android/`.
- **Reuse:** the colour tokens at `css/app.css:1-42` with the conversions in `design.md`; the type roles
  at `DESIGN.md:45-53`; card anatomy and the action set at `js/ui/discover.js:134-274`; the header and
  chip row at `:25-62`; Read Later rows at `js/ui/read-later.js:28-99`; History rows and grouping at
  `js/ui/history.js:20-110`; the settings appearance section at `js/ui/settings.js:221-282`; the
  navigation bar at `js/ui/navigation.js:9-46`; the external-open behavior at `js/app.js:84-89`.
- **Definition of done:**
  - exactly three destinations, ordered Read Later / Discover / History with Discover centered, badges on
    Read Later and History only;
  - Settings is a modal surface, not a destination, and offers Light / Dark / System;
  - Discover presents exactly one card, the available count, and the remaining-choices note, and never a
    list or feed;
  - all four Discover body states render, including the degraded notice when
    `pipeline.failedSourceCount > 0`;
  - the card carries source, relative age, content-type badge, category, reading time when known, title,
    excerpt clamped to four lines, up to five topic tags, and the three actions Not interested / Read
    article / Save for later, plus `Mark read` when the record is opened, plus the opened acknowledgment;
  - `Read article` validates the URL scheme, dispatches `Intent(ACTION_VIEW, …)`, and the card is held
    rather than advanced, released when the record leaves opened or the category changes;
  - only the six authored tokens and values derived from them are used; dynamic colour is off; no
    hard-coded Material default colour appears;
  - back navigation from Read Later or History returns to Discover; the sheet consumes back first;
  - edge-to-edge is enabled and insets are respected;
  - `AppViewModelTest` covers destination switching, settings open and close, appearance change, and
    load success and failure mapping to the right phase;
  - `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green, plus a manual install walking the
    scenarios in `spec.md` §5.
- **Status:** done as 3a + 3b (gate: PASS, slice review on each)

## Slice 4: continuous integration and one instrumented smoke test

- **Scenarios:** none directly; this slice protects the others.
- **Files:** `.github/workflows/android.yml`, `android/app/src/androidTest/kotlin/**`, and the
  `androidTest`/`debug` test dependencies in `android/app/build.gradle.kts`.
- **Must not touch:** `.github/workflows/test.yml`, `.github/workflows/deploy.yml`,
  `.github/dependabot.yml`, and everything outside `android/` and the new workflow file.
- **Reuse:** the SHA-pinning house style at `.github/workflows/test.yml:16,18,33` — every action pinned
  by full SHA with a `# vX` trailing comment.
- **Definition of done:**
  - the workflow runs only for `android/**` and its own path, so web and pipeline pull requests are
    unaffected;
  - it runs `./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug` with
    `working-directory: android` on JDK 17;
  - `gradle/actions/setup-gradle` performs wrapper-JAR validation, this being the repository's first
    committed binary;
  - `permissions: contents: read` and no secrets are used;
  - one instrumented smoke test **launches the app and fails on a startup crash**, then asserts the three
    destination labels render and that selecting one changes the presented screen. The launch assertion is
    not optional: slice 3a found an Android-only regex defect that every JVM test passed through, and a
    smoke test that only inspects labels after a successful launch would have missed it too. It is **not**
    a CI gate and the omission is stated in `evidence.md`;
  - the workflow is **not** registered as a required status check, because a path-filtered required check
    never reports on pull requests that do not touch `android/**`.
- **Status:** done (gate: PASS, slice review)

## Gates

`./gradlew :app:testDebugUnitTest` for every slice, and `:app:assembleDebug` for slices 1, 3, and 4.

The web and pipeline gates are untouched by this item but run in CI on the PR and must stay green:
`pytest`, `python -m pipeline.main --validate-config`, `python -m pip_audit -r requirements.txt`,
`npm test` (baseline `105/105` on base SHA `9e524eb`).

Every slice is reviewed by a non-author reviewer at its gate before the next slice is dispatched, and no
slice self-merges.
