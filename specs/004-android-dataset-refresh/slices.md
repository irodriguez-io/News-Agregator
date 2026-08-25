# 004 — slice plan

Sized **M → 3 ordered slices**. One item branch (`feat/004-android-dataset-refresh`), one PR.
Each slice closes as a failing-first test commit plus an implementation commit, and must fit one fresh
implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

Fixed for every slice — do not re-decide these mid-implementation:

- **No new dependency.** Nothing is added to `android/gradle/libs.versions.toml`. Transport is
  `java.net.HttpURLConnection` (`design.md` D1). If a slice appears to need a dependency, that is a
  report to the supervisor, not a decision to make.
- One compile-time HTTPS URL. Non-`https` refused before connecting; redirects **not** followed;
  `android:usesCleartextTraffic="false"` (D2).
- `304` is a **success** meaning "cache is current" — never the failure path, and it rewrites nothing
  (D3).
- Connect 10s, read 20s, body ceiling 10 MiB enforced while reading and never from `Content-Length`
  (D4).
- Bytes become a dataset only after `DatasetValidator` accepts them. Any failure leaves the cache
  byte-identical and the displayed dataset untouched (D5). No client-side size or article-count gate
  (D6).
- Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*`. Directories are handed in as
  `java.io.File` so stores stay JVM-testable against a temp directory — the constraint item 003
  established and this item inherits.
- **No test may touch the network** (D10). The transport sits behind an interface with a fake.
- `INTERNET` and no other permission. No `ACCESS_NETWORK_STATE` — reachability is not asked about, it
  is discovered by attempting the fetch (D11).
- Reuse, do not restate: `DatasetValidator`, `DatasetResult`/`DatasetErrorCode`,
  `ui/format/RelativeTime.kt`, the `AppAnnouncement` live-region mechanism from item 003 slice 3, and
  the write sequence in `LocalStateFile.write` (`:34-51`) — copied deliberately per D9, not extracted.
- Everything outside `android/` is untouched in every slice.

## Slice 1: transport and cache — JVM-testable, no UI

No Compose, no ViewModel, no manifest change. This slice ends with a repository that can be exercised
entirely from JVM tests against fakes and a temp directory.

- **Scenarios:** "an unchanged dataset is recognized, not re-downloaded", "a structurally invalid
  response cannot replace good content", "an unsupported schema version is refused, not
  reinterpreted", "an oversized response is abandoned", "only the client's own HTTPS origin is
  contacted" (the code half — the manifest half is slice 2), and the storage half of "no network keeps
  the last good dataset and says so".
- **Files:** `«pkg»/data/remote/{DatasetFetcher,DatasetFetchResult,HttpDatasetFetcher}.kt`,
  `«pkg»/data/local/dataset/{DatasetCache,DatasetCacheFile,DatasetCacheMetadata}.kt`, a reshaped
  `«pkg»/data/local/DatasetSource.kt` (the `fun interface … read(): ByteArray` at `:5-7` cannot express
  not-modified or a typed failure), a reshaped `«pkg»/data/DatasetRepository.kt` exposing a cache read
  and a refresh, and new tests under `android/app/src/test/kotlin/**` for the fetcher's response
  mapping, the cache's write/recovery rules, and the repository's adopt/reject decisions.
- **Must not touch:** `«pkg»/ui/**`, `«pkg»/di/**`, `AndroidManifest.xml`, `app/build.gradle.kts`,
  the bundled asset. `AssetDatasetSource` keeps compiling and stays wired until slice 2 replaces it.
- **Reuse:** `DatasetValidator.validate(bytes)` unchanged — the fetcher hands it bytes and never
  interprets them. Follow `LocalStateStore`/`LocalStateFile` for the store/file split and the
  `LocalStateFileRead` `Absent`/`Present`/`Failed` shape; follow `DatasetResult` for result modelling.
- **Definition of done:** `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green; the
  fetcher's HTTPS refusal, redirect refusal, ceiling, and `304` mapping each pinned by a test; a
  cache write proven atomic-by-rename and a failed refresh proven to leave the payload byte-identical;
  no assertion from the existing suite deleted.
- **Status:** done

## Slice 2: wiring — cold-start refresh, and the APK stops carrying articles

- **Scenarios:** "a reachable dataset replaces the queue with what the pipeline published", "no
  network keeps the last good dataset and says so", "a first launch with nothing cached and no network
  is recoverable", "a refresh does not move the article being read", "triaged articles stay triaged
  across a refresh", "the client declares one permission".
- **Files:** `«pkg»/di/AppContainer.kt` (builds the fetcher, cache, and repository from `filesDir`),
  `«pkg»/ui/AppViewModel.kt` (cache-first start per D7, refresh state, `reload()` becomes a real
  refresh, held-card preservation across adoption per D8),
  `«pkg»/ui/AppUiState.kt` if the phase needs a refresh dimension,
  `android/app/src/main/AndroidManifest.xml` (`INTERNET`, `usesCleartextTraffic="false"`),
  `android/app/build.gradle.kts:41` (the `sourceSets … srcDir("src/main/assets")` line), the move of
  `android/app/src/main/assets/sample_articles.json` to test resources, `SampleDatasetTest` (same bytes,
  now a frozen-contract fixture rather than shipped content), `android/README.md` (the bundled-dataset
  section becomes a fixture-provenance section), and `«pkg»/ui/AppViewModelTest.kt` plus
  `FakeLocalStateStore`'s dataset counterpart.
- **Must not touch:** `«pkg»/domain/**`, the transport and cache classes from slice 1 beyond
  construction, `ui/screens/**` and `ui/components/**` (slice 3 owns the visible surfaces).
- **Reuse:** the existing `DatasetPhase` `Loading`/`Error`/`Ready` states and
  `UiStateMapper.discover`'s handling of them — the first-launch-offline scenario is the existing
  `DiscoverUiState.Error` with the existing "Try again" action, not a new surface. `heldArticleId` and
  the `stateMutex` discipline in `AppViewModel` are the mechanisms for D8; do not introduce a second
  lock.
- **Definition of done:** both gates green; a test proving the deck excludes already-triaged articles
  after adopting a newer dataset; a test proving a failed refresh leaves Read Later and History
  populated; a test proving the displayed article survives adoption when still present and the deck
  advances when it is not; `SampleDatasetTest` still validating the real dataset bytes; the merged
  manifest carrying `INTERNET` and no other permission.
- **Status:** done

## Slice 3: the disclosure surfaces

- **Scenarios:** "a failed refresh never implies lost reading data", "the age of the content is
  disclosed where it matters", "an explicit refresh reports what it is doing".
- **Files:** `«pkg»/ui/components/EditorialHeader.kt` (freshness line beside the existing degraded
  notice), `«pkg»/ui/screens/discover/DiscoverScreen.kt` and `DiscoverUiState.kt` (refresh affordance
  and its in-progress state), `«pkg»/ui/screens/settings/SettingsSheet.kt` (absolute `generatedAt` and
  last refresh outcome), `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/ui/format/Labels.kt`,
  `android/app/src/main/res/values/strings.xml`, and the corresponding tests.
- **Must not touch:** `«pkg»/data/**`, `«pkg»/domain/**`, the manifest, the Gradle files.
- **Reuse:** `RelativeTime.relativeDate` / `localDateTime` for both formats — do not write new date
  formatting; `Labels.DEGRADED_NOTICE` and its `failedSourceCount > 0` trigger stay exactly as they
  are (D12); announcements go through the existing `AppAnnouncement` / `AppAnnouncementKind` and the
  polite live region item 003 slice 3 introduced, not a new mechanism; the refresh affordance follows
  `EditorialHeader`'s existing `actionLabel`/`onAction` parameter pair where it fits.
- **Definition of done:** both gates green; freshness derived from `generatedAt` and not from fetch
  time, pinned by a test; the refresh outcome — updated, already current, failed — announced and
  pinned by a test; a second refresh refused while one is in flight; `spec.md` §5 owner walkthrough
  performed and recorded in `evidence.md`.
- **Status:** pending
