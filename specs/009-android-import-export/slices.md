# 009 — slice plan

Sized **M → 3 ordered slices**. One item branch (`feat/009-android-import-export`), one PR targeting
`main`. Each slice closes as a failing-first test commit plus an implementation commit, and must fit one
fresh implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

**Every Gradle invocation needs both of these exported first** — `java` is not on this machine's
`PATH`, and a worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android && ./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

Baseline on `main` at `75f2821`: **163 tests**, green.

Fixed for every slice — do not re-decide these mid-implementation:

- **Import replaces; it never merges.** No partial import, no "keep my Read Later", no reconciliation of
  conflicting weights (`05-personalization-state.md` §50, `design.md` D10). An import that merges is a
  defect even if it looks kinder.
- **The cap is enforced on bytes actually read**, not on the size the document provider reported
  (`design.md` D3). The reported size may trigger an early refusal; it may never be the only check.
- **No new atomicity mechanism.** `LocalStateFile.write` already does temp-plus-fsync-plus-rename and is
  reused as-is (`design.md` D4). A second one in the diff is a finding.
- **A rejected candidate touches nothing** — not the stored file, not memory, not the recovery lock
  (§49).
- **Import clears the recovery lock only after a successful write** (`design.md` D4).
- **The failure surface discloses nothing about the candidate.** `LocalStateResult.Failure.path` is
  never rendered (`design.md` D7).
- **No storage permission.** `AndroidManifest.xml` is expected to need no change; a permission in the
  diff is a report to the supervisor, not a decision (`design.md` D2).
- **`AppViewModel` gains no `android.*` import.** The URI is resolved to a stream at the composition
  root and handed in as bytes (`design.md` D2) — the constraint 004 and 010 both preserved.
- **No new dependency.** `android/gradle/libs.versions.toml` is untouched;
  `androidx.activity.compose` already supplies the launchers.
- **The `signalsApplied` hazard is accepted, not mitigated.** No sanitizing, no downgrading, no warning
  copy (`design.md` D1, owner decision 2026-08-26). Adding a mitigation is a finding.
- One lock. The existing `stateMutex` guards everything in `AppViewModel`; do not add a second.
- Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*` — the constraint item 003
  established.
- **Item 008 is running concurrently on its own branch and merges first.**
  `«pkg»/ui/components/ArticleCard.kt`, `«pkg»/ui/screens/discover/**`, and `«pkg»/ui/gesture/**` belong
  to it and are not touched here. After 008 merges, rebase and re-run both gates on the rebased head;
  pre-rebase numbers are discarded.
- Everything outside `android/` is untouched in all three slices, except this item's own `specs/009-*/`
  documents.

---

## Slice 1: the store — export bytes, import bytes, and the cap

Pure `data/local/state/` plus one IO helper. No ViewModel, no Compose, no resources. Everything in this
slice is JVM-testable against a temporary directory, which is how the existing `LocalStateStore` tests
already work.

- **Scenarios:** "export serializes the current state as the V1 root object", "an exported document
  round-trips through import unchanged", "an oversized file is refused without being read into state",
  "a candidate that lies about its size is still refused", "malformed, wrong-schema, and structurally
  invalid candidates are all refused atomically", "a half-written import cannot be observed", "an import
  recovers a store that was locked for recovery" (the store half).
- **Files:** `«pkg»/data/local/state/LocalStateStore.kt` (an export entry point returning the encoded
  validated state, and an import entry point taking candidate bytes),
  `«pkg»/domain/validation/LocalStateResult.kt` (`IMPORT_TOO_LARGE`), a new bounded-read helper under
  `«pkg»/data/` taking a `java.io.InputStream` and a byte limit, and the matching tests under
  `android/app/src/test/kotlin/**`.
- **Must not touch:** `«pkg»/ui/**`, `«pkg»/di/**`, `res/**`, `AndroidManifest.xml`, any Gradle file.
- **Reuse:** `LocalStateValidator` for the whole of §48 — it already enforces every bullet, and
  re-implementing any of them is a finding. `LocalStateFile.write` for atomicity. `LocalStateMapper`
  for encoding (`design.md` D5). `LocalStateFile`'s existing `beforeRename` hook is how the
  half-written-import scenario is provoked without a real disk failure.
- **Definition of done:**
  - Both gates green.
  - A test proving a state exported and re-imported is equal field for field, including `preferences`,
    `settings`, and `session`.
  - A test proving a candidate one byte over 5 MiB is refused, and one exactly at 5 MiB is accepted.
  - A test proving the bounded read never holds more than the cap plus one byte, driven by a stream
    that reports a small size and yields a large body.
  - A test per refusal cause — not JSON, `schemaVersion` 2, and a record failing validation — each
    asserting the stored file's bytes are **unchanged** afterwards.
  - A test proving a write failure during import leaves the previously stored document loadable.
  - A test proving import clears the recovery lock, and that a subsequent ordinary `save` succeeds; plus
    a test proving a **rejected** import leaves the lock in place.
  - No assertion from the existing suite deleted.
- **Status:** done

## Slice 2: the ViewModel — adoption, the undo slot, the filename, and the announcements

- **Scenarios:** "a valid backup replaces local state wholesale", "an import rebuilds appearance,
  category, and counts", "an import clears the undo slot", "an import recovers a store that was locked
  for recovery" (the notice half), "a refusal does not disclose the candidate's contents", "export writes
  nothing when the destination cannot be written", "the suggested filename carries a UTC timestamp".
- **Files:** `«pkg»/ui/AppViewModel.kt` (an export entry point, an import entry point taking bytes, the
  backup-filename generator, the clears, the announcements), `«pkg»/ui/AppUiState.kt` and
  `«pkg»/ui/state/UiStateMapper.kt` **only if** the failure surface genuinely needs a field there,
  `«pkg»/ui/IntentionalReadingApp.kt` (the new announcement branches only), and
  `android/app/src/test/kotlin/**/ui/AppViewModelTest.kt`.
- **Must not touch:** `«pkg»/data/**` beyond calling slice 1's entry points, `«pkg»/domain/**`,
  `«pkg»/ui/screens/**`, `«pkg»/ui/components/**`, `«pkg»/ui/gesture/**`, `res/values/strings.xml`
  beyond the announcement strings, any Gradle file.
- **Reuse:** `adoptPersistedState` for the whole of §49's *"rebuild counts/deck/theme"* — appearance,
  the night-mode push, and `lastCategory` are already handled there and must not be re-implemented
  (`design.md` D6). `publish()` for the deck and counts. `announce` and the existing
  `AppAnnouncementKind` conventions; `recordPersistenceFailure` is **not** the right vehicle for an
  import refusal, because nothing was persisted. `nowProvider`/`zoneProvider` for the filename, so it is
  testable against a fixed clock (`design.md` D9).
- **Definition of done:**
  - Both gates green.
  - A test proving an import replaces rather than merges: a record present only in the *current* state
    is gone afterwards.
  - A test proving the imported appearance reaches the night-mode applier and the imported
    `lastCategory` reaches `selectedCategory`.
  - A test proving an import empties the undo slot and leaves `undoAvailable` false.
  - A test proving a refused import announces the generic message and changes no local state.
  - A test proving no announcement or failure state carries the validator's `path`.
  - A test proving the filename generator emits `intentional-reading-backup-YYYYMMDD-HHMMSSZ.json` in
    UTC for a fixed instant, including for an instant in a non-UTC zone.
  - `git grep 'android\.'` over `«pkg»/ui/AppViewModel.kt` returns nothing.
  - No assertion from the existing suite deleted.
- **Status:** done

## Slice 3: the surface — the pickers, the confirmation, and the local-data section

The Compose and Android-framework slice. **It carries no new JVM test and that is stated in advance:**
its surface is a Composable plus two `ActivityResultContracts` launchers, and instrumented tests are
parked from CI by decision (`specs/backlog.md` §Parked). Slices 1 and 2 exist so that everything
decidable was already decided and proven before this one starts. Its evidence is `:app:assembleDebug`,
the unchanged test count, and the walkthrough in `spec.md` §5.2.

- **Scenarios:** `spec.md` §5.2 steps 1–10, plus §4.4's two surface scenarios and "cancelling the picker
  changes nothing". No §4.1–4.3 scenario is first proven here.
- **Files:** `«pkg»/ui/screens/settings/SettingsSheet.kt` (the local-data section: export button, import
  button, the too-large notice, the confirmation host), `«pkg»/ui/components/` (an import confirmation
  composable, if `ResetConfirmation` cannot be reused directly),
  `«pkg»/ui/IntentionalReadingApp.kt` (the two launchers and the URI-to-stream resolution),
  `android/app/src/main/res/values/strings.xml` (the section copy, the confirmation copy, the two button
  labels, the too-large notice — ported verbatim from `js/ui/settings.js` per `design.md` D8).
- **Must not touch:** `«pkg»/domain/**`, `«pkg»/data/**`, `«pkg»/ui/AppViewModel.kt` beyond calling
  slice 2's entry points, `«pkg»/ui/screens/discover/**`, `«pkg»/ui/components/ArticleCard.kt`,
  `AndroidManifest.xml`, any Gradle file.
- **Reuse:** `ResetConfirmation`'s panel shape, focus handling, and destructive-action styling for the
  import confirmation (`design.md` D8) — the sheet keeps one idiom. `LiveStatusMessage` for the
  announcements the sheet already surfaces via `statusMessage`.
- **Fixed decisions — do not re-open mid-implementation:**
  - `CreateDocument("application/json")` and `OpenDocument(arrayOf("application/json"))`, via
    `rememberLauncherForActivityResult`. Not `GetContent`, which does not grant a persistable URI and
    admits any provider's idea of a JSON file.
  - A null URI from either launcher means the reader cancelled: do nothing, announce nothing.
  - The stream is read at the composition root and handed to the ViewModel as bytes. The `Uri` does not
    cross into `«pkg»/ui/AppViewModel.kt`.
  - Confirmation precedes replacement, always, and names the file.
  - The copy is the browser's, verbatim, with the file name interpolated.
- **Definition of done:**
  - `:app:assembleDebug` green and `:app:testDebugUnitTest` still green at slice 2's count, with **no
    test deleted or weakened**.
  - `git diff` shows `libs.versions.toml`, `AndroidManifest.xml`, and `«pkg»/domain/**` untouched across
    the whole item.
  - Screenshots from the emulator of the local-data section, the import confirmation naming a file, and
    the too-large notice, attached to the slice report.
- **Deferred to wave B's batched walkthrough:** all of `spec.md` §5.2, run against merged `main`, plus
  the owner's hardware SAF round trip. The slice is *done* when the build is green and the screenshots
  exist; the **item** is not shippable until the walkthrough is recorded in `evidence.md`.
- **Status:** pending
