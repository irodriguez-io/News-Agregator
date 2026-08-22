# 002 — evidence

**Branch:** `feat/002-android-client-foundation`\
**Base SHA:** `9e524eb3ea02f79ab626faa7aab65ef3ecf3ceaa` (`main`, identical to `origin/main` at branch
creation)\
**Base gate state:** `npm test` → 105 tests, 105 pass, 0 fail, verified locally before dispatch. Python
gates were not run locally — this working copy has no `.venv` — so hosted CI on the PR is the
authoritative run for `pytest`, `--validate-config`, and `pip_audit`.

**Implementer:** Codex, one fresh session per slice.\
**Reviewer:** Claude (non-author) at each slice gate. No slice self-merges.

---

## Commits

| SHA | Message |
|---|---|
| `409137a9201b23e15cb5521538a5a799592b7306` | `chore(android): scaffold Gradle project` |
| `334ba329ab217b61f1855cb640ddaa4fd67e770f` | `test(android): cover ArticleDataset v1 parsing and validation` |
| `c8bfdf80b911ce941d5b870293fab6f6e332ebc5` | `feat(android): add ArticleDataset model and validator` |
| `566218ca4e6b299b37308be11ce8a2eb49af8423` | `chore(android): raise compileSdk to 37` |
| `f1283332a0bf1fbdbe12ab6fec2629f8cafefbdb` | `test(android): cover article status transitions and screen state` |

## Snapshot profile

Recorded at slice 1, from the bytes actually committed to
`android/app/src/main/assets/sample_articles.json`. `spec.md` §5 drives its manual walkthrough from this
table rather than from hard-coded numbers.

| Property | Value |
|---|---|
| Source URL | `https://irodriguez.io/News-Agregator/data/articles.json` |
| Fetched | 2026-08-22 |
| `generatedAt` | `2026-08-22T12:59:34Z` |
| ETag | `"6a899d51-29708"` |
| Bytes / article count | 169,736 / 166 |
| `pipeline` counts | 20 enabled / 15 successful / 5 failed |
| Articles per category | technology 96 / science 35 / history 14 / iam 11 / literature 10 / weightlifting 0 / identity_automation 0 |
| Empty categories | `weightlifting`, `identity_automation` |
| Null `publishedAt` / `author` / `readingTimeMinutes` / empty `excerpt` | 25 / 70 / 121 / 33 |
| Articles with zero tags / maximum tag count | 122 / 2 |
| Titles containing non-ASCII characters | 25 |

## Slice 1 — Gradle foundation, dataset model, validator, snapshot asset

**Definition of done**

- [x] `./gradlew :app:assembleDebug` succeeds
- [x] `Json` configured strictly; no DTO property carries a default value
- [x] `DatasetValidator` returns a result type and never throws across the data boundary
- [x] unsupported `schemaVersion` carries a code distinct from malformed
- [x] `DatasetValidatorTest` covers every rejection listed in `slices.md`
- [x] `SampleDatasetTest` validates the shipped asset bytes off the test classpath
- [x] canonical sort order, approved source IDs, and taxonomy tag IDs asserted
- [x] `android/README.md` records provenance, refresh command, allowlist exclusion, `JAVA_HOME` note
- [x] snapshot profile recorded above
- [x] manifest declares zero permissions
- [x] `./gradlew :app:testDebugUnitTest` green

**Serialization plugin under AGP 9 built-in Kotlin:** the primary path applied cleanly:
`org.jetbrains.kotlin.plugin.serialization` 2.4.10 is applied directly beside
`com.android.application`; neither `org.jetbrains.kotlin.android` nor the temporary
`android.builtInKotlin=false` fallback is present.

**Version verification:** all specified versions resolve and remain unchanged in the catalog: AGP
9.3.1, Gradle 9.5.0, Kotlin 2.4.10, Compose BOM 2026.08.00, `kotlinx.serialization` 1.11.0, Core 1.19.0,
Activity 1.13.0, Lifecycle 2.11.0, and Navigation3 1.1.6. A verification build that temporarily placed
the AndroidX UI stack on the runtime classpath failed `checkDebugAarMetadata`: Compose 1.12.0, Core
1.19.0, Activity 1.13.0, and Lifecycle 2.11.0 require compileSdk 37, conflicting with the fixed
compileSdk 36. The failed build reported 13 AAR-metadata incompatibilities; no SDK check was suppressed
and no approved version was silently changed. This does not block slice 1 because it has no Compose UI
and those coordinates are cataloged but unused; it must be resolved before slice 3 applies them.

The Compose, Core, Activity, Lifecycle, and Navigation3 coordinates are cataloged but intentionally not
on the slice-1 runtime classpath: the placeholder uses platform `Activity`. Besides matching the no-UI
scope, this prevents AndroidX startup/profile tooling from contributing a generated receiver permission;
the merged debug manifest therefore has zero `uses-permission` entries.

## Slice 2 — article status state machine and screen-state derivation

**Definition of done**

- [x] `ArticleRecord` holds the full `Article` snapshot
- [x] transition function is pure, clock-injected, and returns applied / unchanged / invalid
- [x] allowed-from sets match `js/state/article-state.js:70-77` exactly
- [x] `open` sets opened only when no record existed; `openedAt` is write-once
- [x] the four idempotent no-ops preserve timestamps
- [x] `ArticleStateMachineTest` covers every table cell, no-op, and rejection
- [x] Discover state is a sealed type encoding the four-state precedence
- [x] Discover eligibility is unseen/opened, held eligible articles remain presented, and remaining is
      `available - 1`
- [x] Read Later orders by `savedAt` descending; History orders by `readAt` descending and groups Today /
      Yesterday / Earlier in an injected zone with empty groups omitted
- [x] aggregates include count, known reading-time sum, unknown-time count, and the first available tag ID
- [x] navigation counts derive from saved/read records; degraded derives from `failedSourceCount > 0`
- [x] `RelativeTimeTest` covers Now / hours / days / absolute, the 31-day cutover, negative-delta clamp,
      local-calendar history grouping, and reading-time rules
- [x] all eight category options match `js/ui/format.js:1-10` in order and label
- [x] no file under `domain/`, `ui/state/`, or `ui/format/` imports `android.*` or `androidx.*`
- [x] `publishedAt` is parsed once into `Instant?` by `DatasetValidator`, never per projection/render
- [x] `./gradlew :app:testDebugUnitTest` green: 41 tests / 41 pass / 0 fail

**`publishedAt` decision:** `Article.publishedAt` is now `Instant?`. `DatasetValidator` performs the
existing strict UTC/calendar validation and converts a non-null value once while constructing the domain
Article. This keeps invalid or unparseable timestamps at the dataset boundary, makes canonical ordering
a direct temporal comparison, and prevents Discover projection or later Compose rendering from parsing
the same string repeatedly. Absence remains `null`, which `RelativeTime` renders as an empty label.

**compileSdk resolution:** the preliminary `566218c` chore raised only `compileSdk` from 36 to 37;
`targetSdk = 36` and `minSdk = 26` remain unchanged. `./gradlew :app:assembleDebug` passed with the
pinned stack, resolving the slice 1 carry-forward observation without adding a dependency.

## Slice 3 — theme, navigation shell, three screens

**Split taken:** 3 as one slice / 3a + 3b —

**Definition of done**

- [ ] three destinations, ordered Read Later / Discover / History, Discover centered
- [ ] badges on Read Later and History only
- [ ] Settings is a modal surface offering Light / Dark / System
- [ ] Discover presents exactly one card, never a list or feed
- [ ] all four Discover body states render, degraded notice included
- [ ] card anatomy and action set complete, including `Mark read` and the opened acknowledgment
- [ ] `Read article` validates the scheme, dispatches `ACTION_VIEW`, and the card is held then released
- [ ] only the six authored tokens and their derivations are used; dynamic colour off
- [ ] back navigation and edge-to-edge insets behave
- [ ] `AppViewModelTest` green
- [ ] `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green
- [ ] manual walkthrough of `spec.md` §5 performed

## Slice 4 — CI and instrumented smoke test

**Definition of done**

- [ ] workflow path-filtered to `android/**` and its own file
- [ ] runs unit tests and `assembleDebug` on JDK 17 with `working-directory: android`
- [ ] wrapper-JAR validation enabled
- [ ] every action pinned by full SHA with a `# vX` comment
- [ ] `permissions: contents: read`, no secrets
- [ ] instrumented smoke test present, and its exclusion from CI stated
- [ ] not registered as a required status check

## Gate results

Failing-first is proven per slice by a red test commit preceding its implementation commit.

| Slice | Red commit | Red counts | Green commit | Green counts |
|---|---|---|---|---|
| 1 | `334ba329ab217b61f1855cb640ddaa4fd67e770f` | compile-time RED; 0 executed because the validator/result/domain types were absent | `c8bfdf80b911ce941d5b870293fab6f6e332ebc5` | 6 tests / 6 pass / 0 fail |
| 2 | `f1283332a0bf1fbdbe12ab6fec2629f8cafefbdb` | compile-time RED; 0 executed because the slice 2 production types and the `publishedAt: Instant?` boundary were absent | `feat(android): add article status state machine and screen state` (this commit) | 41 tests / 41 pass / 0 fail (35 slice 2 + 6 inherited) |
| 3 | | | | |
| 4 | | | | |

## Scenario traceability

| Scenario (`spec.md` §4) | Authority | Covering test |
|---|---|---|
| the bundled dataset loads and validates | contracts.md §ArticleDataset | `SampleDatasetTest`; `DatasetValidatorTest` file-order case |
| Discover offers exactly one article | 01-product.md §27; 06-ui-ux.md §3 | `UiStateMapperTest.Discover offers one dataset-ordered card with available and remaining counts` |
| a category with no articles reaches the empty state | 06-ui-ux.md §3 | `UiStateMapperTest.a category with no articles uses exact permission-to-leave copy` |
| a degraded dataset is disclosed without alarm | contracts.md:133 | `UiStateMapperTest.navigation counts ignore opened and dismissed records and degraded follows pipeline failures` |
| dismissing advances the deck | 05-personalization-state.md §§27/32 | `ArticleStateMachineTest.dismiss sets dismissedAt and clears savedAt and readAt while preserving openedAt`; `UiStateMapperTest.dismissing advances the deck and immediately decreases both counts` |
| saving reaches Read Later | 05-personalization-state.md §27 | `ArticleStateMachineTest.save sets savedAt and clears dismissedAt and readAt while preserving openedAt`; `UiStateMapperTest.saving removes Discover head reaches top of Read Later and increments navigation` |
| an opened article is acknowledged and held | 06-ui-ux.md §51 | `ArticleStateMachineTest.open from unseen creates an opened record with write-once metadata`; `UiStateMapperTest.an eligible held article remains presented and visibly acknowledged when opened` |
| marking read reaches History under Today | 05-personalization-state.md §27 | `ArticleStateMachineTest.mark read sets readAt clears queue timestamps and preserves openedAt`; `UiStateMapperTest.marking read removes Discover head and groups it under Today without undo state` |
| a contract-violating dataset claims nothing | contracts.md §ArticleDataset | `DatasetValidatorTest` rejection table and failure-code cases |
| three destinations and a modal settings surface | 06-ui-ux.md §18 | |

## Test infrastructure added

`src/main/assets` is added to the JVM test resource classpath, so `SampleDatasetTest` reads the exact
shipped bytes without Robolectric or an emulator. Kotlin's JUnit bridge is test-only. The production
validator rejects malformed UTF-8 before JSON decoding.

## Regression boundary held

- [x] no existing test weakened, skipped, or deleted; `SampleDatasetTest` changed only to compare the new
      parsed `Instant?` directly instead of parsing strings inside its comparator
- [x] `scripts/build_pages.py` `ALLOWED_PATHS` unchanged
- [x] `.github/workflows/test.yml` and `deploy.yml` unchanged
- [x] no change under `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`

## Reviewer observations (not findings)

Slice 1 gate: **PASS** (reviewer: Claude, non-author; implementer: Codex). Verified independently of the
implementer's report — commit range `947cacc..c8bfdf8`, a forced clean `--rerun-tasks` test run read from
the JUnit XML, and the diff read in full.

Confirmed by the reviewer rather than taken on trust:

- 6 test functions, 6 pass, 0 fail on a forced re-run; the low count is table-driven counting, not thin
  coverage. `a contract violating dataset claims nothing` holds 21 rejection cases, with unsupported
  schema version and missing-versus-null author as two further tests — 23 rejections against the 22 the
  slice plan named.
- The committed asset is byte-identical to the bytes profiled at design time — SHA-256
  `235e4df6…`, matching `android/README.md`. Every number in the snapshot profile above was
  re-measured and matched.
- `approvedSourceIds` is exactly the 20 IDs in `config/sources.json`, and `approvedTopicIds` is exactly
  the 72 IDs in `config/topics.json` — no drift, nothing invented.
- The 200-character limits on `source.name`, `author`, `tag.label`, and `contentType.label` are faithful
  ports of `js/data/validation.js:134,141,157,165`, not invented bounds.
- Zero `uses-permission` entries in both the source and merged debug manifests. No file under `domain/`
  imports `android.*` or `androidx.*`. Nothing outside `android/` and this file was touched.
- The failing-first commit `334ba32` precedes the implementation commit `c8bfdf8`, and no test was
  weakened or deleted. `npm test` remains 105/105.
- UTF-8 decoding is strict (`CodingErrorAction.REPORT`), which exceeds what the slice asked for and is
  the right call given 25 non-ASCII titles in the snapshot.

Observations carried forward, none of which block this slice:

1. **`Article.publishedAt` is a validated `String?`, not a temporal type.** Nothing in `spec.md` or
   `design.md` required parsing, and canonical ordering over a fixed-format UTC string is correct. But
   slice 2 introduces relative-age rendering and day bucketing, so decide there whether to parse once in
   the validator or per projection — not per recomposition.
2. **`DatasetRepository.load()` is synchronous.** Harmless today because `AppContainer` only constructs
   it and nothing calls it, but slice 3 wires it to a ViewModel: make it `suspend` on an IO dispatcher at
   that point rather than reading 170 KB on the main thread.
3. **`DatasetValidator` lives under `domain/validation/` but imports `data.DatasetJson` and `data.dto`.**
   The stated rule — no `android.*`/`androidx.*` under `domain/` — holds, and the JVM-testability
   guarantee holds with it. Worth revisiting only if the module is ever split.
4. **Deferred build settings.** No launcher icon (`android:icon` is absent, so the app shows the system
   default), `.idea/` missing from `android/.gitignore`, `isMinifyEnabled = false` for release, and the
   Compose compiler plugin not yet in the catalog. All belong to slice 3 or 4; none contradict slice 1.
5. **The AGP deprecation warning** on `sourceSets.getByName("test").resources.srcDir("src/main/assets")`
   is accepted for now — the mechanism is what keeps the suite emulator-free. Revisit if AGP 10 removes it.

## Outstanding

- **Gradle wrapper provenance.** `gradle-wrapper.jar` is the repository's first committed binary.
  Generated by `gradle wrapper --gradle-version 9.5.0` run from a locally cached Gradle 8.10.2
  distribution on Android Studio's bundled JBR 21, in a throwaway build outside the repository, then
  copied in. The committed jar is therefore Gradle **8.10.2**'s wrapper jar, whose SHA-256
  `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046` matches Gradle's published
  `gradle-8.10.2-wrapper.jar.sha256` exactly — a verified upstream artifact, which is what
  `gradle/actions/setup-gradle` wrapper validation checks. It bootstraps the pinned 9.5.0 distribution
  correctly; a wrapper jar built by 9.5.0 itself (published checksum
  `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`) is not separately downloadable, so
  regenerating it requires a second `./gradlew wrapper` pass after the 9.5.0 distribution has been
  fetched. The optional second pass was not run in slice 1; the already committed, upstream-verified jar
  was left unchanged.
- **Distribution pinning.** `distributionSha256Sum` is set to
  `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746`, retrieved from
  `services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256`. Gradle 9.5.0 is the version AGP 9.3
  documents as its default; 9.7.1 is the latest stable and was deliberately not chosen for a foundation
  slice.
- **Instrumented tests are not gated.** They require an emulator and are excluded from CI by decision,
  not by oversight.
- **No persistence.** State does not survive process death in this milestone; every launch is a fresh
  queue. Stated in `spec.md` §3 so it is not filed as data loss.
- **Web-side validator gaps observed, not fixed.** `js/data/validation.js:148-163` enforces no `tags`
  length limit, and `:145` accepts `readingTimeMinutes >= 1` where `contracts.md` and
  `pipeline/validation.py:81-83` require 2. Reported for the web owner; out of scope here per
  `design.md` §Divergences.
