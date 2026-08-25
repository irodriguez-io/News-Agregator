# 004 — Android Dataset Refresh

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/README.md` Amendment 6, `docs/v1/02-architecture.md` §§12–13/21,
`docs/v1/07-pipeline-deployment.md` §§10/13/14/22/23/43/49,
`docs/v1/08-security-dependencies.md` §§6/16/18/19/32/40/52/55, `docs/v1/01-product.md` §24,
`docs/v1/06-ui-ux.md` §§67–70

---

## 1. Problem

The Android client cannot obtain a dataset it was not compiled with. `AssetDatasetSource` reads
`sample_articles.json` out of the APK (`android/app/src/main/kotlin/io/irodriguez/intentionalreading/
data/local/DatasetSource.kt:9-12`), `AppContainer` wires it as the only source
(`di/AppContainer.kt:10-12`), and item 002 stated the limitation plainly:
"No `INTERNET` permission, no HTTP client, no refresh"
(`specs/002-android-client-foundation/spec.md:45-47`). The committed snapshot holds **166 articles
generated 2026-08-22**; the pipeline has published **203 articles** since, and refreshes every six
hours (`07-pipeline-deployment.md:1024`). The gap only widens.

**Item 003 turned that limitation into a defect.** `DiscoverDeck.build` treats any article carrying a
record as ineligible unless it is `OPENED`
(`domain/state/DiscoverDeck.kt:21-24`, `:33-34`). Before persistence, records died with the process
and every launch refilled the queue, so a finite snapshot was survivable. Now records survive
(`data/local/state/LocalStateStore.kt`), and the arithmetic is one-directional: triage 166 articles and
Discover is empty **forever**. The only exit the app offers is Reset, which destroys the reader's Read
Later and History. Normal, correct use of the product walks it into a dead end.

That contradicts the product directly. `01-product.md:558` describes a queue the reader returns to,
and §24 (`:648-676`) requires the interface to behave intentionally when content is *unavailable* —
not to become permanently contentless as a consequence of being used.

The behaviour is not open-ended. The dataset is a static artifact, not an API
(`02-architecture.md:601-620`), the browser already consumes it with a plain request
(`js/data/articles.js:11-45`), and the Android side already owns the hard half: `DatasetValidator`
enforces `ArticleDataset v1` field by field, and `DatasetResult`/`DatasetErrorCode` already model
success, unsupported schema, and malformed input. What is missing is transport, a cache, and the
disclosure that makes staleness visible instead of silent.

Three properties the web client gets for free must be built deliberately here. The browser refetches
on every load and holds nothing, so it is never stale without knowing it; an installed app that keeps
a copy on disk can be. Hence: the last good copy is retained and used offline, a bad response can
never replace it, and the age of what is on screen is stated where the reader is deciding what to
read.

**This item declares the app's first permission.** Item 003's evidence records a manifest with zero
permissions. `INTERNET` retires that property. It is a normal permission with no runtime prompt, it is
the minimum this item cannot avoid, and no other permission is added.

## 2. Story

As a reader, I want the app to fetch the articles the pipeline has actually published — every time I
open it, and on request — so that my finite reading queue refills with today's writing instead of
emptying permanently into the snapshot that shipped with the app.

## 3. Out of scope

- **Background and periodic refresh.** No `WorkManager` (a new dependency), no `JobScheduler`, no
  alarms, no push. Refresh happens on cold start and when the reader asks. The app fetches while it is
  open and never otherwise.
- **Any second endpoint.** One compile-time HTTPS URL. No user-editable address, no environment
  switching, no discovery, no mirror, no publisher fetching of any kind
  (`08-security-dependencies.md` §52 — the client fetches its own dataset and nothing else).
- **Preference learning, personalized ranking, and deck diversity.** Discover keeps rendering in
  dataset order, which remains legitimate because the pipeline emits a total order
  (`07-pipeline-deployment.md` §31, `pipeline/retention.py:12-20`). Deferred as recorded in
  `specs/future-items.md`.
- **Undo and swipe gestures.** Still deferred from item 002 §3.
- **Import and export.** Still deferred; `specs/future-items.md` records what that item inherits.
- **The pre-Compose launch frame** recorded as outstanding by item 003. Untouched here.
- **Delta or partial updates.** The dataset is fetched whole or not at all.
- **Any change to `pipeline/**`, `config/**`, the web runtime, the source catalog, the taxonomy, or
  the `ArticleDataset v1` contract.** Amendment 6 confines this item to `android/`.
- **New dependencies.** Nothing is added to `android/gradle/libs.versions.toml`.

## 4. Scenarios

### Scenario: a reachable dataset replaces the queue with what the pipeline published

Given the app starts with no cached dataset and the dataset endpoint is reachable\
When the client fetches it\
Then the response is validated as `ArticleDataset v1` before it is used\
And Discover offers the articles from that response\
And the validated bytes and the response `ETag` are cached on the device

### Scenario: an unchanged dataset is recognized, not re-downloaded

Given a cached dataset and its `ETag`\
When the client refreshes and the server answers `304 Not Modified`\
Then the refresh is reported as successful and current\
And the cached dataset stays exactly as it was on disk\
And Discover does not change

### Scenario: no network keeps the last good dataset and says so

Given a cached dataset from an earlier refresh\
When the app starts with no usable network\
Then Discover offers the cached articles\
And the interface discloses that the content was not refreshed\
And the cache is not modified

### Scenario: a first launch with nothing cached and no network is recoverable

Given a fresh install, no cached dataset, and no usable network\
When the app starts\
Then Discover shows the dataset failure state with a retry action\
And the reader can reach Read Later, History, and Settings\
And retrying once the network is available loads the dataset

### Scenario: a structurally invalid response cannot replace good content

Given a cached dataset\
When a refresh returns a body that fails `ArticleDataset v1` validation\
Then the cached dataset remains on disk byte for byte\
And Discover keeps offering the cached articles\
And the refresh is reported as failed

### Scenario: an unsupported schema version is refused, not reinterpreted

Given a refresh returns a well-formed document whose `schemaVersion` is not 1\
When the client validates it\
Then it is refused as unsupported\
And no field of it is read as though it were version 1\
And the cached dataset and the displayed articles are unchanged

### Scenario: an oversized response is abandoned

Given a refresh whose response body exceeds the response-size ceiling\
When the ceiling is reached\
Then the client stops reading the body\
And nothing is written to the cache\
And the refresh is reported as failed

### Scenario: only the client's own HTTPS origin is contacted

Given the dataset URL\
When the client prepares a request\
Then a URL that is not `https` is refused before any connection is made\
And a redirect response is treated as a failed refresh rather than a target to follow\
And the application permits no cleartext traffic

### Scenario: a refresh does not move the article being read

Given the reader is looking at an article in Discover\
When a refresh adopts a newer dataset that still contains that article\
Then that article remains the one on screen\
And the available and remaining counts reflect the newer dataset

### Scenario: triaged articles stay triaged across a refresh

Given articles that were saved, dismissed, or marked read before a refresh\
When a newer dataset containing those same articles is adopted\
Then none of them returns to Discover\
And Read Later and History still list them\
And genuinely new articles are the ones that refill the queue

### Scenario: a failed refresh never implies lost reading data

Given saved and read articles in local state\
When a refresh fails for any reason\
Then Read Later, History, and Settings remain fully available\
And nothing in the interface suggests the reader's stored data was affected

### Scenario: the age of the content is disclosed where it matters

Given a dataset with a known `generatedAt`\
When Discover is displayed\
Then it states how old the content is in relative terms\
And Settings states the exact generation timestamp in local time\
And the last refresh outcome is stated alongside it

### Scenario: an explicit refresh reports what it is doing

Given the reader asks for a refresh\
When the request is in flight\
Then the interface shows that a refresh is running and does not start a second one\
And the outcome — updated, already current, or failed — is announced through a live region

### Scenario: the client declares one permission

Given the built application\
When the merged manifest is inspected\
Then `android.permission.INTERNET` is declared\
And no other permission is declared\
And cleartext traffic is not permitted

## 5. Verification

Both Android gates, re-run by the reviewer rather than read from an implementer report:

```sh
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

No unit test may touch the network; the fetch is behind an interface with a fake
(`design.md` D10), and CI stays offline.

Owner walkthrough on a Pixel API 37 emulator:

1. Fresh install, online. Discover offers the current dataset — roughly 203 articles today, not 166 —
   and the header states an age in minutes or hours.
2. Airplane mode, force-stop, relaunch. The same articles are offered, the interface says the refresh
   failed, and Read Later and History are intact.
3. Clear app data, airplane mode, launch. The dataset failure state appears with a retry action.
   Leave airplane mode, retry, and the queue arrives.
4. Triage several articles, then refresh. They do not come back; the new arrivals do.
5. `run-as` inspection of `filesDir`: the cached payload and its sidecar are present, and the stored
   `ETag` matches the header the endpoint served.
6. `aapt dump permissions` or the merged manifest: `INTERNET` and nothing else.
