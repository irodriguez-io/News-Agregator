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
| `70ecc98a46cf8c6b5d7174bead3f662221339ba6` | `feat(android): add article status state machine and screen state` |
| `2fca25db04470bf2969d47ca6cc54bf2f80a3732` | `chore(android): add Compose dependencies` |
| `80a6d9d2eb66809204e127bee06cb381cc2ee016` | `test(android): cover destination and appearance state` |
| `df6192f9f35c4caba812ca6ce1b804b8eef5aa9e` | `fix(android): drop the unsupported regex flag from title validation` |
| this commit | `feat(android): add Compose shell and Discover screen` |
| `8be14ed10730ccba8ac758d09c09d6e7b796b1ba` | `feat(android): expose localized read time and topic labels` |
| `9a3ed82c1f686fa7c56fc79c431c51468ca51b0e` | `test(android): cover Read Later History and settings state` |
| this commit | `feat(android): add Read Later History and settings surfaces` |

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

### Authorized slice 2 contract extension for 3b

Slice 3b exposed two omissions in the slice 2 brief, both verified against the browser and authorized
in the 3b follow-up before protected files changed:

- `RelativeTime.localDateTime` now formats an already-validated `Instant?` with injected zone and locale,
  and `HistoryRowUiState` carries the result. Null renders `""`; malformed text cannot reach this layer
  because `DatasetValidator` has already rejected or parsed it at the boundary.
- `AggregateUiState` retains `firstTagId` and now carries `firstTagLabel`, resolved from the same ordered
  record list by matching the selected ID to the first authored tag label.

The targeted pre-implementation run was compile-time RED with **0 tests executed** and five unresolved
contract references. Commit `8be14ed10730ccba8ac758d09c09d6e7b796b1ba` made the forced full suite
**56 tests / 56 pass / 0 fail / 0 skipped**. The extension added three tests: localized date-time plus
null input, null topic ID/label for untagged records, and label/date-time assertions in mapper cases.
No other protected-layer file changed.

## Slice 3 — theme, navigation shell, three screens

**Split taken:** 3a + 3b — slice 3a is complete here; the exact slice 3b remainder is recorded below.

**Slice 3a definition of done**

- [x] three destinations, ordered Read Later / Discover / History, Discover centered
- [x] badges on Read Later and History only
- [x] the Settings entry point toggles `settingsOpen`; Settings is not a destination
- [x] Read Later and History are single-line placeholders marked `// slice 3b`, with no 3b body work
- [x] Discover presents exactly one card, never a list or feed
- [x] all four Discover body states render exclusively, degraded notice included
- [x] card anatomy and action set complete, including `Mark read` and the opened acknowledgment
- [x] `Read article` validates the scheme, dispatches `ACTION_VIEW`, and the card is held then released
- [x] dismiss and save advance the deck and update counts immediately
- [x] the six authored tokens and six derived values per theme match the approved table; dynamic colour off
- [x] navigation selection uses ink, not accent, and surfaces have no Material tonal tint
- [x] `DatasetRepository.load()` is suspend and performs asset IO on its injected IO dispatcher
- [x] back navigation and edge-to-edge insets behave
- [x] `AppViewModelTest` and the theme-derivation tests are green
- [x] `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` are green
- [x] the available `spec.md` §5 paths were walked on a Pixel 10 API 37 emulator

### Slice 3a RED → GREEN

The feature RED commit `80a6d9d2eb66809204e127bee06cb381cc2ee016` failed at compile time before
any test could execute because `AppViewModel`, destination and appearance state, and the theme APIs did
not exist. The completed implementation runs **53 tests / 53 pass / 0 fail / 0 skipped**: 8
`AppViewModelTest`, 2 theme derivation, 7 validator, and 36 inherited tests.

The six authored light tokens and six authored dark tokens are kept as the only colour literals. The
other six values per theme are derived in Kotlin and asserted exactly, including the 42% backdrop alpha.
Every Material colour-scheme role is supplied explicitly, and dynamic colour is disabled.

### API 37 emulator walkthrough

Observed on the `Pixel_10` AVD (Android API 37), from a cleared app state:

- cold launch completed with `MainActivity` resumed and no `AndroidRuntime` error; Discover showed one
  card, `166 available in All`, and the calm degraded notice for the snapshot's five failed sources
- the bottom bar contained exactly Read Later / Discover / History in that order, with Discover centered;
  the left and right destinations showed their mandated single-line 3a placeholders and back returned to
  Discover
- Settings remained a top-bar entry rather than a fourth destination; after toggling its state, back was
  consumed and `MainActivity` remained resumed on Discover, ready for slice 3b to render the modal body
- selecting Weightlifting showed the exact empty-state title, permission-to-leave copy, and `View Read
  Later`; that action reached the Read Later placeholder while History remained reachable
- dismiss advanced the deck and changed 166 to 165; save advanced it to 164 and showed a Read Later badge
  of 1
- `Read article` moved the resumed activity to Chrome's first-run activity; returning resumed this app on
  the same card with `OPENED`, the acknowledgment copy, and `Mark read`
- `Mark read` advanced the deck from 164 to 163 and showed a History badge of 1; the opened card was no
  longer presented
- advancing from a scrolled long card resets the next article to the complete Discover header; returning
  from the browser to the same opened article retains its useful card position
- edge-to-edge system bars and minimum action targets were visible without content being trapped beneath
  the top or bottom app chrome

Loading is normally shorter than an emulator hierarchy capture, and the valid bundled dataset cannot
produce the Error state; both are covered by the sealed-state rendering branch and mapper/ViewModel unit
tests rather than claimed as manually observed. A missing-browser handler was not observable because
Chrome is installed. Light / Dark / System controls, Read Later content, and History content were not
walked because they belong to slice 3b.

`aapt2 dump permissions app-debug.apk` emitted only the package line: the built APK still declares zero
permissions.

### Authorized DatasetValidator repair

The first API 37 launch exposed an inherited runtime defect before any 3a content could render:
Android's regex implementation throws `IllegalArgumentException: UNICODE_CHARACTER_CLASS flag not
supported` while initializing `DatasetValidator`. The host JVM accepts that flag, which is why all prior
unit gates were green and could not expose the Android-only failure.

The authorized repair replaces the flag-dependent Java pattern with the flag-free Kotlin regex
`[^\p{Z}\p{C}\p{P}\p{S}]`; no other validation rule or domain file changed. Its targeted RED run was
**7 tests / 6 pass / 1 fail**, proving the old field was not a flag-free Kotlin regex. The GREEN run was
**7 / 7 pass**. Regression coverage asserts the regex has no options (with the Android reason recorded),
keeps the existing punctuation-only rejection, rejects Unicode separators plus punctuation, and accepts
a title containing non-ASCII letters.

Slice 4's instrumented smoke must launch `MainActivity` and assert visible Discover content, not merely
install the APK, so any future class-initialization/startup crash fails that gate.

### Slice 3b completion

- [x] Read Later renders its exact editorial header and empty copy, a non-empty-only three-value band,
      zero-padded queue positions, saved age, source-first kicker, title, up to three tags, and Read ↗ /
      Mark read / Remove in order
- [x] History renders its exact editorial header and empty copy, a non-empty-only three-value band,
      mapper-ordered Today / Yesterday / Earlier groups with counts and empty groups omitted, localized
      date/time fallback, category-first kicker, title, and Reopen ↗ / Mark unread
- [x] absent/zero stat values render `Unavailable`; positive known time renders `~{n} min`
- [x] shared editorial header, stat band, row shell, compact text action, and empty panel live under
      `ui/components/`; reading-list rows use rules rather than raised card shadows
- [x] row action descriptions name the article; compact actions are at least 44dp; external actions retain
      the visible ↗ affordance
- [x] Remove and Mark unread reach `ArticleStateMachine` only through `AppViewModel.onArticleAction`
- [x] Settings is a `ModalBottomSheet` with only Light / Dark / System, the approved token backdrop, a
      visible close control, and both Back and Escape dismissal before destination handling
- [x] Export, Import, and Reset are deliberately absent because item 002 has no persisted data
- [x] adaptive launcher foreground/background/monochrome resources use the authored accent and surface;
      manifest declares icon and round icon while retaining both permission-removal entries
- [x] no swipe, Undo, toast, new dependency, persistence, product logic in composables, or adjacent-runtime
      change was introduced

#### Slice 3b RED → GREEN

Commit `9a3ed82c1f686fa7c56fc79c431c51468ca51b0e` is a genuine compile-time RED: **0 tests
executed**, with eleven unresolved references for queue-position, stat-value, and group-count presentation
contracts. The ViewModel row-action tests in that commit compiled against the existing state-machine
boundary; no production failure was manufactured. The completed implementation runs **62 tests / 62
pass / 0 fail / 0 skipped**: 10 `AppViewModelTest`, 4 reading-surface formatting, 15 mapper, 10 relative
time, 2 theme derivation, and 21 inherited data/domain tests.

#### Slice 3b API 37 emulator walkthrough

Observed on the `Pixel_10` AVD (Android API 37), installed from the built debug APK and started from a
cleared app state:

- cold launch resumed `MainActivity` without a crash and rendered Discover at `166 available in All`;
  the crash log stayed empty
- Read Later and History both rendered their exact empty-state title, copy, and navigation action before
  any interaction
- Settings remained a top-bar modal rather than a fourth destination; its hierarchy contained only the
  appearance section and Light / Dark / System options, with no Export, Import, or Reset controls
- Light and Dark selection changed both the app and sheet palettes and updated the selected radio state;
  system Back and `KEYCODE_ESCAPE` each dismissed the sheet while leaving History selected
- saving `Transaction Tokens` advanced Discover to 165, raised the Read Later badge to 1, and placed it at
  `Queue 01` with `Saved Now`, source-first metadata, `Unavailable` known time, OAuth topic, and the three
  row actions in order
- two later saves proved ordering rather than mere membership: `Building a Quantum Computer, One Fragile
  Qubit at a Time` was `Queue 01` and the earlier `Genomes of Poaceae relatives…` was `Queue 02`; the
  badge and overview count both showed 2
- Mark read emptied Read Later, raised History to 1, and rendered `Transaction Tokens` under Today with
  `1 article`, a localized `Aug 22, 2026, 10:57 AM` position, category-first metadata, and both actions
- Reopen handed the URL to Chrome's `IntentDispatcher`; returning left the record in History, and no
  missing-browser path was available to observe
- Mark unread emptied History and restored the same article to `Queue 01` with a Read Later count of 1;
  Remove then reached the exact empty Read Later state and returned both counts to zero
- `aapt2 dump permissions` emitted only the package line, confirming zero APK permissions after the icon
  and modal additions; the Pixel launcher app drawer rendered the authored navy-and-paper adaptive book
  icon instead of the prior system default

The committed snapshot's maximum tag count is two, so the three-tag cap cannot be stress-tested with
real bundled data; both call sites enforce `take(3)`. Fresh in-memory interactions can produce only Today,
so Yesterday/Earlier rendering and empty-group omission remain covered by `UiStateMapperTest` rather than
claimed as manually observed. A null read timestamp and a positive known-time aggregate were likewise not
reachable through valid fresh state in this walkthrough. Loading/error and a missing browser remain the
same unobservable device paths recorded for 3a.

The emulator exposed and the implementation fixed one 3b-specific defect before acceptance: the first
Escape handler was attached to an unfocused sheet modifier, so `KEYCODE_ESCAPE` did not dismiss. The
modal content now owns initial focus and handles Escape at its preview boundary; the repeated walkthrough
confirmed dismissal without the destination handler firing.

## Slice 4 — CI and instrumented smoke test

**Definition of done**

- [x] workflow path-filtered to `android/**` and its own file
- [x] runs unit tests and `assembleDebug` on JDK 17 with `working-directory: android`
- [x] wrapper-JAR validation enabled
- [x] every action pinned by full SHA with a `# vX` comment
- [x] `permissions: contents: read`, no secrets
- [x] instrumented smoke test present, and its exclusion from CI stated
- [x] not registered as a required status check

`.github/workflows/android.yml` has only `pull_request` and `push` to `main`, and both triggers carry the
same two path filters: `android/**` and `.github/workflows/android.yml`. Its one `ubuntu-latest` job uses
JDK 17 Temurin, `defaults.run.working-directory: android`, the basic GitHub Actions-backed Gradle cache,
explicit wrapper validation, and exactly
`./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug`. It has repository-content read
permission and no secret input.

The action pins were resolved from each official GitHub repository on 2026-08-22 with both
`git ls-remote` and GitHub's Git refs API:

- `actions/checkout` tag `v7.0.1` is a lightweight tag directly at commit
  `3d3c42e5aac5ba805825da76410c181273ba90b1`;
- `actions/setup-java` tag `v5.7.0` is a lightweight tag directly at commit
  `b6effb05e454b25005698d916606bdc6ffcbf961`;
- `gradle/actions` tag `v6.3.0` is an annotated tag object
  `67621b124fd2e251c5e8a0e6e3b91318f2287669`; dereferencing it with `refs/tags/v6.3.0^{}` and then
  resolving the tag object through the API both produce commit
  `9c971963bec38e04b3d30dcc455b5382be2fdbfb`, which is the SHA pinned in the workflow.

`MainActivityLaunchSmokeTest` first launches `MainActivity`, waits for the Compose tree to become idle,
and asserts that the activity remains `RESUMED` with the message "a startup crash is the likely cause."
It then asserts the three rendered destination controls are ordered Read Later / Discover / History by
their screen positions, selects Read Later, and proves the Discover header leaves the semantics tree as
the Read Later header enters it. On the `Pixel_10` API 37 emulator, the final
`:app:connectedDebugAndroidTest` run passed 1/1.

The guard was also made genuinely red: an uncommitted `error("Intentional startup crash for smoke-test
RED proof")` was temporarily inserted immediately after `MainActivity.onCreate` called `super`. The run
executed 1 test, failed 1, exited 1, reported `Unable to start activity`, and ended with
`Instrumentation run failed due to Process crashed`. A fatal activity-start exception kills the
in-process instrumentation before JUnit can replace Android's crash report with the assertion's custom
message; Android's runner nevertheless reports the process death explicitly. The temporary exception was
removed, `git diff -- android/app/src/main` returned empty, and the next run passed 1/1.

The instrumented test added stable AndroidX Test JUnit `1.3.0` and Espresso `3.7.0`; Compose
`ui-test-junit4` and the debug-only `ui-test-manifest` inherit `1.12.0` from the existing Compose BOM.
The brief's dependency list was incomplete for an executable JUnit4 instrumented test:
`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` was also required. This is
test-only build configuration and changes no shipped behavior.

No emulator job is present by deliberate decision. The instrumented smoke test is a local/on-demand
device gate; CI protects pull requests with the JVM suite and APK assembly without adding emulator boot
time. The path-filtered workflow is also deliberately not a required status check, because it produces no
check at all for pull requests outside the two filtered paths.

## Gate results

Failing-first is proven by a red test commit preceding implementation for slices 1 through 3. Slice 4
instead uses the brief's uncommitted startup-crash injection so no deliberately broken product commit is
added to history.

| Slice | Red commit | Red counts | Green commit | Green counts |
|---|---|---|---|---|
| 1 | `334ba329ab217b61f1855cb640ddaa4fd67e770f` | compile-time RED; 0 executed because the validator/result/domain types were absent | `c8bfdf80b911ce941d5b870293fab6f6e332ebc5` | 6 tests / 6 pass / 0 fail |
| 2 | `f1283332a0bf1fbdbe12ab6fec2629f8cafefbdb` | compile-time RED; 0 executed because the slice 2 production types and the `publishedAt: Instant?` boundary were absent | `feat(android): add article status state machine and screen state` (this commit) | 41 tests / 41 pass / 0 fail (35 slice 2 + 6 inherited) |
| 3a | `80a6d9d2eb66809204e127bee06cb381cc2ee016` | compile-time RED; 0 executed because the ViewModel, destination/appearance state, and theme APIs were absent | `feat(android): add Compose shell and Discover screen` (this commit) | 53 tests / 53 pass / 0 fail / 0 skipped |
| 3a validator repair | regression added over `80a6d9d2eb66809204e127bee06cb381cc2ee016` | 7 tests / 6 pass / 1 fail | `df6192f9f35c4caba812ca6ce1b804b8eef5aa9e` | 7 tests / 7 pass / 0 fail |
| 3b slice 2 extension | targeted pre-commit RED | compile-time RED; 0 executed, 5 unresolved contract references | `8be14ed10730ccba8ac758d09c09d6e7b796b1ba` | 56 tests / 56 pass / 0 fail / 0 skipped |
| 3b | `9a3ed82c1f686fa7c56fc79c431c51468ca51b0e` | compile-time RED; 0 executed, 11 unresolved presentation references | `feat(android): add Read Later History and settings surfaces` (this commit) | 62 tests / 62 pass / 0 fail / 0 skipped |
| 4 | temporary uncommitted startup-crash injection | 1 test / 0 pass / 1 fail; instrumentation process crashed; Gradle exit 1 | `f7c7ede8c2768f5654e6627653f5287a61e509f5` | 1 test / 1 pass / 0 fail on `Pixel_10` API 37; 62 JVM tests / 62 pass / 0 fail; `assembleDebug` green; web 105/105 |

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
| three destinations and a modal settings surface | 06-ui-ux.md §18 | `AppViewModelTest.destination switching preserves the fixed declaration order`; `AppViewModelTest.settings entry point toggles open and closed`; `AppViewModelTest.appearance changes between the three frozen values`; API 37 modal walkthrough |

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

### Slice 4 gate: PASS

Reviewer: Claude, non-author. Verified independently — commit range `9d5a76b..df57cad`, a forced
`--rerun-tasks` run (**62 tests, 62 pass**), and the diff read in full.

- **Every action pin re-verified from upstream, not taken on trust.** `git ls-remote` against each
  repository confirms `actions/checkout` v7.0.1 → `3d3c42e5…`, `actions/setup-java` v5.7.0 →
  `b6effb05…`, and `gradle/actions` v6.3.0 → `9c971963…`. The last is an annotated tag and the pin is its
  dereferenced commit, which is the correct target.
- The workflow is path-filtered to `android/**` and its own file, carries `permissions: contents: read`
  with no secrets, runs `working-directory: android` on JDK 17 temurin, and enables
  `validate-wrappers: true`.
- **The wrapper provenance item is closed.** The regenerated jar's SHA-256 is
  `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`, matching Gradle's published
  `gradle-9.5.0-wrapper.jar.sha256` exactly, and `distributionSha256Sum` is intact.
- **Nothing under `android/app/src/main/**` changed**, and `test.yml`, `deploy.yml`, and
  `dependabot.yml` are untouched — this slice adds no behaviour.
- **The smoke test was demonstrated to fail, not merely to pass.** With a startup exception temporarily
  reintroduced it went red (Gradle exit 1, "Unable to start activity", "Process crashed"); reverted, it
  passes 1/1 on the Pixel_10 API 37 emulator. A guard that has never failed is not yet a guard.
- It exceeds what was asked: destination order is verified from actual horizontal positions rather than
  mere presence, and the screen switch is asserted by one screen's eyebrow appearing as the other's
  disappears.

Two honest notes recorded by the implementer and confirmed here:

1. A **fatal** startup crash kills in-process instrumentation before the custom assertion message can be
   emitted; Android's runner reports the process crash instead. The custom message covers non-fatal launch
   failures. The guard still catches the defect class either way — only the wording differs.
2. Executable JUnit4 instrumentation also needed the `AndroidJUnitRunner` configuration, which the brief
   omitted.

**The instrumented test is deliberately not a CI gate.** It needs an emulator, and an emulator job would
add several minutes and a flake surface to every Android pull request. The decision, not an oversight.

**Caveat on the `paths:` filter.** A path-filtered workflow never reports on pull requests that do not
touch `android/**`, so it must **not** be registered as a required status check — it would leave those
pull requests permanently pending. If it is ever to be required, the filter has to go first.

### Slice 3b gate: PASS

Reviewer: Claude, non-author. Verified independently — commit range `7bedecc..5078204`, a forced
`--rerun-tasks` run read from the JUnit XML (**62 tests, 62 pass, 0 skipped**), and the diff read in full.

**Two more gaps in the orchestrator's slice 2 brief, found before any code was written.** The implementer
stopped twice in this slice rather than working around a boundary, and was right both times:

1. `HistoryUiState` exposed `readAt` and a relative `readAge`, but `js/ui/history.js:24` renders an
   absolute localized date and time with the fallback "Read date unavailable". The slice 2 brief asked for
   `relativeDate`, `historyGroup`, and `readingTime` and never mentioned `localDateTime`
   (`js/ui/format.js:39-49`).
2. The aggregate exposed `firstTagId`, but both overview bands display a topic **label**.
   `js/state/selectors.js:42` yields only the id; resolution happens a layer up in `topicLabel`
   (`js/app.js:75-82`), which `js/app.js:192` and `:204` call before the view sees it. The brief said
   "the first tag ID", which is what was built, and is not what the band shows.

Both were authorized as a narrow slice 2 contract extension, landed as its own commit `8be14ed` ahead of
the 3b feature commit. `firstTagId` was kept alongside the new label so the existing derivation tests
stayed meaningful, and `topicLabel` was ported faithfully rather than shortcut to "the first tag of the
first tagged record".

**Root cause worth naming:** all three implementer stops in this item trace to the same place. The web
client assembles its view models in `js/app.js`, one layer above `js/state/selectors.js`, and the
orchestrator's briefs repeatedly treated the selectors as the whole derivation contract. The state and
formatting layers ported cleanly; the *assembly* layer is where the instructions kept coming up short.

Also confirmed by the reviewer:

- Only the two authorized files and their tests changed in the protected layers. `domain/**` and
  `data/**` untouched. `npm test` still 105/105.
- **Every user-visible string is verbatim from the web client**, checked string by string against
  `js/ui/read-later.js`, `js/ui/history.js`, and `js/ui/settings.js` — eyebrows, descriptions, both stat
  band label sets, both empty states and their actions, the row action labels including the `↗`
  affordance, and the settings appearance section.
- Copy is externalized to `res/values/strings.xml` rather than Kotlin constants, which is better Android
  practice than the brief required.
- Zero permissions in the built APK (`aapt2 dump permissions`), with both `tools:node="remove"` entries
  intact after adding the launcher icon. An adaptive launcher icon now exists, resolving the observation
  carried from 3a.
- Export, import, and reset are absent from the settings sheet, as `spec.md` §3 requires.

The implementer found and fixed a third emulator-only defect: `Escape` did not dismiss the settings sheet
because it did not own keyboard focus. Like the scroll-offset bug in 3a, no scenario covered it and no
unit test could have.

**Observation 9 — split copy convention, deliberate but worth knowing.** Discover's four body-state
strings, the degraded notice, the side note, and the category labels live in Kotlin constants in
`ui/format/Labels.kt`, while every other screen's copy lives in `strings.xml`. This is forced rather than
careless: `UiStateMapper` chooses the Discover state *and* carries its copy, and being pure Kotlin with no
Android imports it cannot read a resource. The consequence is that Discover's body-state copy cannot be
localized while the rest of the app can. No scenario is violated and nothing is broken — the web client is
English-only — but if localization ever matters, the fix is for the mapper to emit a state key and let the
composable resolve the string. Cheaper to do then than to discover then.

### Slice 3a gate: PASS

Reviewer: Claude, non-author. Verified independently — commit range `6232036..5d6596e`, a forced
`--rerun-tasks` run read from the JUnit XML (**53 tests, 53 pass, 0 skipped**), and the diff read in full.

**The significant event of this slice was an escaped slice 1 defect.** `DatasetValidator` compiled its
title-readability pattern with `Pattern.UNICODE_CHARACTER_CLASS` in a companion object. Android's regex
engine rejects that flag, so the app threw `IllegalArgumentException` at first touch of the class — app
startup. Every JVM unit test passed throughout, because the host JVM supports the flag.

Three things about how it was found are worth recording, because they change how later slices should be
briefed:

1. **The orchestrator's brief was wrong.** It stated no emulator was available. A `Pixel_10` AVD and an
   `android-37.0` system image are both installed. The implementer booted one anyway, which is the only
   reason this surfaced before owner acceptance.
2. **The slice 1 review missed it.** The reviewer read that exact line and verified the *rule* was a
   faithful port of `js/data/validation.js:127`, without asking whether the *mechanism* exists on the
   target platform. Porting a rule and porting a working implementation are different checks.
3. **The implementer stopped instead of patching.** `domain/**` was forbidden to slice 3a, so it reported
   the defect and waited for authorization rather than making a silent cross-boundary edit. That is the
   forbidden-path rule working exactly as intended.

The authorized repair (`df6192f`, deliberately its own commit because it fixes slice 1, not slice 3a)
replaces the flag with a flag-free Unicode class, `[^\p{Z}\p{C}\p{P}\p{S}]` — separators, control and
format characters, punctuation, symbols — which preserves the JavaScript rule's intent without depending
on any flag, and agrees with `pipeline/validation.py:54-59`.

**Guard against the class of defect, not the instance:** a test asserts the pattern carries no regex
options at all, since no JVM test can reproduce an Android-only limitation. Two further tests prove
Unicode whitespace and punctuation are still unreadable and that a non-ASCII *letter* title is accepted —
load-bearing, given 25 non-ASCII titles in the snapshot. Slice 4's definition of done has been tightened
to require the instrumented smoke test to fail on a startup crash.

Also confirmed by the reviewer:

- The twelve theme values per theme are **derived**, not pasted: `mixOklch` interpolates in OKLCH with
  correct shortest-arc hue handling, and `ThemeDerivationTest` asserts all 24 against the table in
  `design.md` §Six authored colour tokens. That table was computed independently by the orchestrator, so
  two separate implementations agree — including the 42%-alpha backdrop as `#6B…`.
- **Accent discipline holds** (`DESIGN.md:36-40`): navigation selection uses `tokens.fg` for icon, label,
  and indicator, and accent appears in exactly two places — the content-type badge and the primary
  `Read article` action.
- **Zero permissions survived adding AndroidX.** The manifest strips both the `<permission>` and the
  `<uses-permission>` that AndroidX injects for `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Verified three
  ways: source manifest, merged manifest, and `aapt2 dump permissions` on the built APK.
- Slice 1 observation 2 is **resolved**: `DatasetRepository.load()` is now `suspend` with an injected
  dispatcher defaulting to `Dispatchers.IO`.
- No `Instant.now()`, `ZoneId.systemDefault()`, or `LocalDate.now()` appears inside any composable; the
  zone is an injectable provider on the ViewModel. No dynamic colour anywhere.
- Only the authorized `DatasetValidator` files changed in the protected layers; `domain/` otherwise
  untouched, `ui/state/**` and `ui/format/**` untouched. `npm test` still 105/105.

The implementer also found and fixed a second real defect on the emulator: a new Discover card inherited
the previous card's scroll offset. Not in any scenario, and a genuine bug.

Outstanding for 3b and beyond: the app still declares no `android:icon`, so the launcher shows the system
default. Cosmetic, but it should not reach owner acceptance that way.

### Slice 2 gate: PASS

Reviewer: Claude, non-author. Verified independently — commit range `eb243b7..70ecc98`, a forced
`--rerun-tasks` run read from the JUnit XML (**41 tests, 41 pass, 0 skipped**: 13 state machine, 14
mapper, 8 relative time, 5 validator, 1 sample dataset), and the diff read in full.

Confirmed rather than taken on trust:

- The allowed-from table matches `js/state/article-state.js:70-77` cell for cell, and the four idempotent
  no-ops match `:79-86`. The `open` idempotency translation to `openedAt != null` is sound and commented.
- Seeding a brand-new record with `status = OPENED` looked wrong until checked: `makeRecord` does exactly
  the same (`js/state/article-state.js:28`). It is a faithful port, and it is unobservable anyway because
  every action that can create a record sets the status explicitly.
- Every Discover string is **verbatim** from `js/ui/discover.js:282-315` — loading, error title and copy,
  the retry label, empty title and copy, the Read Later action, and the degraded notice.
- The side-note visibility condition matches: the browser shows it when `eligible > 1`, and
  `remainingChoices` returns null at `remainingCount <= 0`, which is the same boundary.
- Aggregates match `js/state/selectors.js:35-57` including `unknownReadingTimeCount` and the
  first-tag-of-the-first-tagged-record rule; navigation counts match `:25-33`.
- `historyGroup` correctly translates the browser's *local calendar day* delta (not elapsed hours) using
  `LocalDate` in an injected zone. `relativeDate` reproduces the ladder and the negative-delta clamp.
- All eight category options match `js/ui/format.js:1-10` in order and label.
- The `publishedAt` change to `Instant?` kept full strict validation ahead of parsing, and the only edit
  to a slice 1 test was a type change in a comparator — no assertion weakened. `npm test` still 105/105.
- No file under `domain/`, `ui/state/`, or `ui/format/` imports `android.*` or `androidx.*`.

Observations carried forward from slice 2:

6. **`remainingChoices` reproduces a grammatical error in the browser's copy.** `js/ui/discover.js:328`
   renders "1 more choice wait quietly behind this one." — it switches the noun for the singular case but
   leaves the verb plural. Porting it verbatim is the right call for parity, and diverging unilaterally
   would be inventing a requirement. Report it to the web owner alongside the two validator gaps in
   §Outstanding; fix both clients together or neither.
7. **`DiscoverUiState.Card.isOpened` tests status only.** The browser also requires `openedAt !== null`
   (`js/app.js:118-122`). Equivalent today, because no code path yields `OPENED` with a null `openedAt`.
   Tighten it when persistence lands and records can arrive from storage rather than from a transition.
8. **`DatasetPhase.Error` carries no error code**, so the `UNSUPPORTED_SCHEMA` versus malformed
   distinction the validator produces cannot reach the UI. This matches the browser, which renders one
   error panel for all four codes and is forbidden from leaking payload text
   (`tests/js/articles.test.js:39-60`), so it is correct for now — but the code is what a networking
   milestone needs to say "this app is out of date" rather than "something went wrong".

Slice 1 observation 1 is **resolved**: `publishedAt` is parsed once into `Instant?` at the boundary.
Slice 1 observation 2 (synchronous `DatasetRepository.load()`) remains open and is slice 3's to fix.

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

- **Gradle wrapper provenance (closed in slice 4).** On 2026-08-22, the required second
  `./gradlew wrapper --gradle-version 9.5.0` pass ran from the pinned, locally cached Gradle 9.5.0
  distribution on Android Studio's bundled JBR 21. The regenerated `gradle-wrapper.jar` SHA-256 is
  `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`, exactly matching Gradle's
  published `gradle-9.5.0-wrapper.jar.sha256`. This replaces the upstream-verified Gradle 8.10.2 jar
  recorded by slice 1; wrapper provenance is no longer outstanding.
- **Distribution pinning.** `distributionSha256Sum` is set to
  `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746`, retrieved from
  `services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256`. Gradle 9.5.0 is the version AGP 9.3
  documents as its default; 9.7.1 is the latest stable and was deliberately not chosen for a foundation
  slice.
- **Instrumented tests are not gated.** They require an emulator and are excluded from CI by decision,
  not by oversight. `MainActivityLaunchSmokeTest` is the local/on-demand startup guard; the path-filtered
  CI job intentionally remains emulator-free and runs the 62-test JVM suite plus `assembleDebug`.
- **No persistence.** State does not survive process death in this milestone; every launch is a fresh
  queue. Stated in `spec.md` §3 so it is not filed as data loss.
- **Web-side validator gaps observed, not fixed.** `js/data/validation.js:148-163` enforces no `tags`
  length limit, and `:145` accepts `readingTimeMinutes >= 1` where `contracts.md` and
  `pipeline/validation.py:81-83` require 2. Reported for the web owner; out of scope here per
  `design.md` §Divergences.

## Item status

All four slices are done and each passed an independent non-author gate. Verified on the final branch
head: **62 JVM unit tests passing**, `:app:assembleDebug` green, the instrumented launch smoke test
passing on a Pixel_10 API 37 emulator, and the repository's existing gates untouched — `npm test` still
105/105, and no file under `pipeline/**`, `config/**`, `js/**`, `css/**`, `scripts/**`, `tests/**`, or
`index.html` changed at any point in this item.

| Slice | Scope | Gate |
|---|---|---|
| 1 | Gradle foundation, dataset model, validator, snapshot asset | PASS |
| 2 | Article status state machine, screen-state derivation | PASS |
| 3a | Theme, navigation shell, Discover | PASS |
| 3b | Read Later, History, Settings sheet | PASS |
| 4 | CI workflow, launch smoke test, wrapper provenance | PASS |

Three defects were found that no JVM unit test could have caught, all on the emulator: the
`UNICODE_CHARACTER_CLASS` startup crash, Discover cards inheriting the previous card's scroll offset, and
the settings sheet not accepting `Escape` because it did not own keyboard focus. That is the strongest
single lesson from this item — **a green JVM suite is not evidence that an Android app runs.**

## Outstanding — owner verification

The implementer walked the scenarios on a Pixel_10 API 37 emulator and the results are recorded per slice
above. The **owner walkthrough in `spec.md` §5 has not been performed**, and these specific checks were
not device-observable with the committed snapshot and so rest on unit coverage alone:

- Discover's Loading and Error states, and the missing-browser-handler path.
- History's Yesterday and Earlier groups — fresh interactions only ever produce Today.
- The three-tag row truncation boundary — the snapshot's articles carry at most two tags.
- A positive known-reading-time aggregate and a null read timestamp.

Also outstanding, and deliberate rather than forgotten: no persistence, so every launch is a fresh queue
(`spec.md` §3). The reviewer authored the specification and governance documents in this item and the
per-slice reviews; the reviewer did not author any product code.

