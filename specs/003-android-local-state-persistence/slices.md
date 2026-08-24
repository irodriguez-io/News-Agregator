# 003 — slice plan

Sized **M → 3 ordered slices**. One item branch (`feat/003-android-local-state-persistence`), one PR.
Each slice closes as a failing-first test commit plus an implementation commit, and must fit one fresh
implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

Fixed for every slice — do not re-decide these mid-implementation:

- The persisted document is the browser local-state root verbatim (`design.md` D1). No Android-shaped
  variant, no extra field, no omitted field.
- The document lives at `filesDir/intentional-reading-v1.json` and is written temp-file → `fd.sync()` →
  `renameTo`. Exactly one class opens a file.
- **No new dependency.** Nothing is added to `android/gradle/libs.versions.toml` in this item. If a
  slice appears to need one, that is a report to the supervisor, not a decision to make.
- Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*`. `filesDir` is resolved by the
  caller and handed to the store as a `java.io.File`, which keeps the store JVM-testable against a temp
  directory.
- The manifest still declares **zero permissions**.
- Values read from a document written by another client — non-empty `preferences`, `signalsApplied`
  entries that are `true` — are preserved on rewrite, never zeroed. Android does not apply learning
  signals; it must not destroy someone else's.
- Persist before publish (`design.md` D5). No UI state changes on a failed write, with the Open
  exception as the single carve-out.

## Slice 1: the state document — model, validator, and the store

No Compose, no ViewModel, no wiring. This slice ends with a store that can be exercised entirely from
JVM tests against a temp directory.

- **Scenarios:** the storage halves of "a fresh install starts from the default local state",
  "malformed stored state is preserved, not overwritten", "an unsupported schema version is refused, not
  reinterpreted", and "a structurally invalid document is rejected whole". The persistence half of
  "triage survives process death" — round-tripping a record through the store, not yet through the app.
- **Files:** `«pkg»/domain/model/Appearance.kt` (moved out of `ui/AppViewModel.kt`),
  `«pkg»/domain/model/LocalState.kt` (`LocalState`, `PreferenceEntry`, `SignalsApplied`),
  `«pkg»/domain/model/ArticleRecord.kt` (gains `signalsApplied`),
  `«pkg»/domain/state/ArticleStateMachine.kt` (constructs new records with all-`false` signals),
  `«pkg»/domain/validation/{LocalStateValidator,LocalStateResult}.kt`,
  `«pkg»/data/local/state/{LocalStateDto,LocalStateMapper,LocalStateFile,LocalStateStore}.kt`,
  `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/xml/data_extraction_rules.xml`,
  the import updates in `ui/AppViewModel.kt`, `ui/screens/settings/SettingsSheet.kt`,
  `ui/theme/Theme.kt`, and their tests, plus new `android/app/src/test/kotlin/**` for
  `LocalStateValidatorTest` and `LocalStateStoreTest`.
- **Must not touch:** `«pkg»/ui/**` beyond the mechanical `Appearance` import move; `«pkg»/data/`
  dataset classes; everything outside `android/`.
- **Reuse:** port `js/state/storage.js` rule for rule — `validateState` at `:140-175`, the preference
  entry rules at `:60-76`, the record rules at `:80-138`, `migrateState` at `:177-180`, the four
  `loadState` outcomes at `:213-273`, the recovery lock at `:189-211`, `resetState` at `:288-297`. The
  enumerations and patterns come from `js/data/validation.js:3-26` and are already mirrored in
  `«pkg»/domain/model/{Category,ArticleStatus}.kt` and `DatasetValidator` — reuse those, do not restate
  them. Follow the `DatasetResult` / `DatasetErrorCode` shape from item 002 for the result type.
- **Definition of done:**
  - `LocalStateStore` exposes `load()`, `save(state)`, and `reset()`, returns a result type, and never
    throws across the boundary;
  - `load()` returns the four specified outcomes with distinct codes: absent file → default state
    flagged as default; valid → parsed state; malformed JSON → recoverable error; `schemaVersion != 1` →
    a *distinct* compatibility code. Malformed, incompatible, and structurally invalid reads all leave
    the file's bytes untouched — asserted by comparing bytes before and after;
  - after any failed read, `save()` is refused with a recovery-required code until `reset()` runs;
  - `save()` writes via temp file plus `renameTo`, and a test proves the previous document survives
    intact when the write fails partway;
  - the migration entry point exists, is the only path from a parsed document to a `LocalState`, and
    rejects any version other than 1;
  - `LocalStateValidatorTest` is table-driven from one valid document, mutating a single field per case,
    covering at minimum: unknown top-level key, missing top-level key, `schemaVersion` 0 and 2 and
    `"1"`, unknown key inside `preferences`/`settings`/`session`/a record, a preference weight of −5.1
    and 5.1 and a non-number, negative and non-integer `interactions`, a preference key that is not a
    valid identifier, a 19-character and an uppercase-hex article ID, a record key that disagrees with
    the snapshot's `article.id`, a status of `unseen` and of `archived`, a non-UTC and a
    non-ISO-8601 timestamp, a `firstSeenAt` of `null`, a `signalsApplied` missing a key and carrying an
    extra one, an Article snapshot that fails the item-002 article rules, an appearance of `Light`, and
    a `lastCategory` of `everything`;
  - `LocalStateStoreTest` exercises a real temp directory: absent file, round trip of a document
    carrying every field populated, a document with non-empty `preferences` and `signalsApplied` that
    survives a save unchanged, each failure outcome, the recovery lock, and reset;
  - the manifest sets `android:allowBackup="false"` and `android:dataExtractionRules`, and the rules
    file excludes the state document from both backup and device transfer;
  - `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green.

## Slice 2: wiring — restore at launch, persist on every action

- **Scenarios:** "a fresh install starts from the default local state", "displaying an article creates
  no persisted record", "triage survives process death", "an opened article is restored as opened",
  "Read Later survives the article leaving the dataset", "appearance and category selection persist",
  "a failed write does not claim success", "reading still works when persistence fails".
- **Files:** `«pkg»/data/LocalStateRepository.kt`, `«pkg»/di/AppContainer.kt`, `«pkg»/ui/AppViewModel.kt`,
  `«pkg»/ui/IntentionalReadingApp.kt`, `«pkg»/MainActivity.kt` if the composition gate needs it, and
  `android/app/src/test/kotlin/**` for the extended `AppViewModelTest` plus a fake store.
- **Must not touch:** `«pkg»/domain/**` and the slice 1 store, other than calling them — if the
  ViewModel needs something they do not expose, that is a slice 1 defect to report; the three screen
  composables; anything outside `android/`.
- **Reuse:** the commit order at `js/app.js:232-242`; the Open carve-out at `:259-297`; the load-time
  sequence at `js/app.js:389-403` (load state → apply appearance → render → announce on failure).
- **Definition of done:**
  - the ViewModel's records, appearance, and selected category are all sourced from the loaded document,
    and `session.lastCategory` round-trips through the null ↔ `"all"` mapping;
  - no content composes until the state load resolves, and the resolved appearance is in effect for the
    first composed frame;
  - `records` and `publish()` are updated only after a successful write; on failure the previous records
    stand, the navigation counts do not move, and a recoverable error reaches the UI layer as state
    rather than a log line;
  - `OPEN` still returns a transition that permits publisher navigation when the write failed, and
    carries the "not saved locally" condition outward;
  - a load producing a default state writes nothing — asserted by the fake store recording zero writes
    after launch and category browsing;
  - the held-card behaviour is **not** restored across launches: a restored `opened` record appears in
    normal Discover order, because holding is ephemeral presentation state;
  - Read Later and History render restored records whose article IDs are absent from the dataset, proven
    by a test whose fake store returns exactly that;
  - writes are serialised: concurrent actions cannot interleave two writes, and the test asserts the
    resulting document reflects both actions in order;
  - `AppViewModelTest` covers restore, every action's persist-then-publish path, both failure paths, the
    recovery-locked store, and the default-state launch;
  - `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green.

## Slice 3: the surfaces — storage errors and Reset

- **Scenarios:** "reset clears the device state after explicit confirmation", plus the user-visible
  halves of "a failed write does not claim success", "reading still works when persistence fails", and
  "malformed stored state is preserved, not overwritten".
- **Files:** `«pkg»/ui/screens/settings/SettingsSheet.kt`, a confirmation composable under
  `«pkg»/ui/components/`, `«pkg»/ui/IntentionalReadingApp.kt`, `«pkg»/ui/AppViewModel.kt` for the reset
  and error-acknowledgement entry points, `android/app/src/main/res/values/strings.xml`, and the test
  additions under `android/app/src/test/kotlin/**`.
- **Must not touch:** `«pkg»/domain/**`, `«pkg»/data/**`; anything outside `android/`.
- **Reuse:** the reset flow and its confirmation copy at `js/ui/settings.js:180-215` — reveal, then a
  panel naming exactly what is cleared, then Cancel and a destructive confirm, with the destructive
  action disabled while it runs and re-enabled on failure; the failure announcements at `:150,164,211`
  and `js/app.js:293-295,401`; the restrained destructive styling required by `06-ui-ux.md:890`.
- **Definition of done:**
  - Settings gains a local-data section whose only V1 action here is Reset; export and import are
    absent, not stubbed;
  - Reset requires an explicit second confirmation that names preferences, Read Later, History,
    dismissals, and settings, and says the change is to this device and cannot be undone;
  - cancelling leaves the document and every count untouched; confirming clears the document, returns
    appearance to `system` and the category to `all`, empties both queues, and lifts a recovery lock;
  - a persistence failure is announced through the same live-region mechanism item 002 used, never
    steals focus, and never accompanies a UI change that claims the action succeeded;
  - a load failure surfaces a persistent, dismissible notice — not a transient announcement — stating
    that stored data could not be read, that a temporary empty state is in use, and that Reset is the
    recovery, because this state blocks every subsequent write and the reader must be able to find the
    way out;
  - the Open-failed-to-persist warning is distinct from the Open-failed-to-navigate error;
  - `./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green, plus the owner walkthrough in
    `spec.md` §5 on a device or emulator, with the `run-as` byte checks recorded in `evidence.md`.

## Gates

`./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` for every slice. The instrumented launch
smoke test from item 002 slice 4 is run locally at slice 3 and remains outside CI.

The web and pipeline gates are untouched by this item but run in CI on the PR and must stay green:
`pytest`, `python -m pipeline.main --validate-config`, `python -m pip_audit -r requirements.txt`,
`npm test` (baseline 105/105 on `99016e6`).

Every slice is reviewed by a non-author reviewer at its gate before the next slice is dispatched, and no
slice self-merges.
