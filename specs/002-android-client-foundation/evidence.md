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
| | |

## Snapshot profile

Recorded at slice 1, from the bytes actually committed to
`android/app/src/main/assets/sample_articles.json`. `spec.md` §5 drives its manual walkthrough from this
table rather than from hard-coded numbers.

| Property | Value |
|---|---|
| Source URL | |
| Fetched | |
| `generatedAt` | |
| ETag | |
| Bytes / article count | |
| `pipeline` counts | |
| Articles per category | |
| Empty categories | |
| Null `publishedAt` / `author` / `readingTimeMinutes` / empty `excerpt` | |
| Articles with zero tags / maximum tag count | |
| Titles containing non-ASCII characters | |

## Slice 1 — Gradle foundation, dataset model, validator, snapshot asset

**Definition of done**

- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] `Json` configured strictly; no DTO property carries a default value
- [ ] `DatasetValidator` returns a result type and never throws across the data boundary
- [ ] unsupported `schemaVersion` carries a code distinct from malformed
- [ ] `DatasetValidatorTest` covers every rejection listed in `slices.md`
- [ ] `SampleDatasetTest` validates the shipped asset bytes off the test classpath
- [ ] canonical sort order, approved source IDs, and taxonomy tag IDs asserted
- [ ] `android/README.md` records provenance, refresh command, allowlist exclusion, `JAVA_HOME` note
- [ ] snapshot profile recorded above
- [ ] manifest declares zero permissions
- [ ] `./gradlew :app:testDebugUnitTest` green

**Serialization plugin under AGP 9 built-in Kotlin:** path taken, and why —

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
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |

## Scenario traceability

| Scenario (`spec.md` §4) | Authority | Covering test |
|---|---|---|
| the bundled dataset loads and validates | contracts.md §ArticleDataset | |
| Discover offers exactly one article | 01-product.md §27; 06-ui-ux.md §3 | |
| a category with no articles reaches the empty state | 06-ui-ux.md §3 | |
| a degraded dataset is disclosed without alarm | contracts.md:133 | |
| dismissing advances the deck | 05-personalization-state.md §§27/32 | |
| saving reaches Read Later | 05-personalization-state.md §27 | |
| an opened article is acknowledged and held | 06-ui-ux.md §51 | |
| marking read reaches History under Today | 05-personalization-state.md §27 | |
| a contract-violating dataset claims nothing | contracts.md §ArticleDataset | |
| three destinations and a modal settings surface | 06-ui-ux.md §18 | |

## Test infrastructure added

## Regression boundary held

- [ ] no existing test modified
- [ ] `scripts/build_pages.py` `ALLOWED_PATHS` unchanged
- [ ] `.github/workflows/test.yml` and `deploy.yml` unchanged
- [ ] no change under `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`

## Reviewer observations (not findings)

## Outstanding

- **Gradle wrapper provenance.** `gradle-wrapper.jar` is the repository's first committed binary. Record
  how it was generated and its SHA-256 here.
- **Instrumented tests are not gated.** They require an emulator and are excluded from CI by decision,
  not by oversight.
- **No persistence.** State does not survive process death in this milestone; every launch is a fresh
  queue. Stated in `spec.md` §3 so it is not filed as data loss.
- **Web-side validator gaps observed, not fixed.** `js/data/validation.js:148-163` enforces no `tags`
  length limit, and `:145` accepts `readingTimeMinutes >= 1` where `contracts.md` and
  `pipeline/validation.py:81-83` require 2. Reported for the web owner; out of scope here per
  `design.md` §Divergences.
