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
| this slice's implementation commit | `feat(android): add ArticleDataset model and validator` |

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

- [ ] `ArticleRecord` holds the full `Article` snapshot
- [ ] transition function is pure, clock-injected, and returns applied / unchanged / invalid
- [ ] allowed-from sets match `js/state/article-state.js:70-77` exactly
- [ ] `open` sets opened only when no record existed; `openedAt` is write-once
- [ ] the four idempotent no-ops preserve timestamps
- [ ] `ArticleStateMachineTest` covers every table cell, no-op, and rejection
- [ ] Discover state is a sealed type encoding the four-state precedence
- [ ] Read Later orders by `savedAt` descending; History groups Today / Yesterday / Earlier
- [ ] `RelativeTimeTest` covers the ladder and the 31-day cutover
- [ ] no file under `domain/` imports `android.*` or `androidx.*`
- [ ] `./gradlew :app:testDebugUnitTest` green

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
| 1 | `334ba329ab217b61f1855cb640ddaa4fd67e770f` | compile-time RED; 0 executed because the validator/result/domain types were absent | this slice's implementation commit | 6 tests / 6 pass / 0 fail |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |

## Scenario traceability

| Scenario (`spec.md` §4) | Authority | Covering test |
|---|---|---|
| the bundled dataset loads and validates | contracts.md §ArticleDataset | `SampleDatasetTest`; `DatasetValidatorTest` file-order case |
| Discover offers exactly one article | 01-product.md §27; 06-ui-ux.md §3 | |
| a category with no articles reaches the empty state | 06-ui-ux.md §3 | |
| a degraded dataset is disclosed without alarm | contracts.md:133 | |
| dismissing advances the deck | 05-personalization-state.md §§27/32 | |
| saving reaches Read Later | 05-personalization-state.md §27 | |
| an opened article is acknowledged and held | 06-ui-ux.md §51 | |
| marking read reaches History under Today | 05-personalization-state.md §27 | |
| a contract-violating dataset claims nothing | contracts.md §ArticleDataset | `DatasetValidatorTest` rejection table and failure-code cases |
| three destinations and a modal settings surface | 06-ui-ux.md §18 | |

## Test infrastructure added

`src/main/assets` is added to the JVM test resource classpath, so `SampleDatasetTest` reads the exact
shipped bytes without Robolectric or an emulator. Kotlin's JUnit bridge is test-only. The production
validator rejects malformed UTF-8 before JSON decoding.

## Regression boundary held

- [x] no existing test modified
- [x] `scripts/build_pages.py` `ALLOWED_PATHS` unchanged
- [x] `.github/workflows/test.yml` and `deploy.yml` unchanged
- [x] no change under `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`

## Reviewer observations (not findings)

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
