# 009 — Import and export

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/05-personalization-state.md` §§46–50, `docs/v1/contracts.md` §§31/32/37,
`docs/v1/08-security-dependencies.md` Boundary C and §27, `docs/v1/README.md` Amendment 6\
**Wave:** B (`specs/waves/wave-b.md`) · **Branch:** `feat/009-android-import-export` → `main`

---

## 1. Problem

Reading state on Android is trapped on the device. `05-personalization-state.md` §§46–50 specify export
and import as V1 capability, the browser implements them (`js/state/storage.js:306-345`,
`js/ui/settings.js:98-172`), and Android's Settings sheet offers Appearance and Reset and nothing else.
A reader who changes phones loses their Read Later queue, their History, and their dismissals.

**It is cheaper than it looks, and the reason is item 003.** `LocalStateValidator` is already ported and
already enforces the whole of §48 — parseable JSON, `schemaVersion = 1`, root structure, preference
entries, article IDs, statuses, snapshot structures, category IDs, the appearance enum, and
timestamp/null shapes. `LocalStateFile.write` already does the temporary-file-plus-rename-plus-fsync
that §49's atomicity needs. `AppViewModel.adoptPersistedState` already rebuilds appearance and the
selected category from a state it is handed. This item is the **wrapper** and the Storage Access
Framework surface, not a new subsystem.

### 1.1 The inherited hazard, and the decision taken on it

Item 003 derived `signalsApplied.read` and `.opened` from each record because the frozen validator
demands equality, not free-standing bookkeeping (`specs/003-android-local-state-persistence/design.md`
D9; `LocalStateValidator.kt:137-145`). Android has never applied the matching preference deltas — that
is item 005. So an Android record claims a signal with nothing behind it.

Export makes those records portable, and that is the hazard: an Android-read article imported into the
web client and then marked unread decrements source and topic weights the browser never incremented.
**The mirror case is also real:** a browser record imported into Android and marked unread here clears
its `read` flag while Android leaves the weight untouched, stranding a positive weight that can never
be reversed. Both are the same defect seen from two sides — Android has records but no learning — and
both need a deliberate user action.

**Decision, taken by the owner on 2026-08-26: accept and record.** Export ships the state as it stands.
The reasoning, including why the two obvious alternatives are unavailable, is in `design.md` D1. Item
005 already owes a one-time reconciliation across every record on disk that claims a flag with no delta
behind it (`specs/backlog.md` §005); imported records are the same population and are covered by the
same decision. This item introduces no second mechanism for 005 to unpick.

## 2. Story

As a reader, I want to write my reading state to a file I control and put it back later, so that
changing devices — or recovering from a corrupt store — costs me a file rather than my history.

## 3. Out of scope

- **Merge.** `05-personalization-state.md` §50: import replaces. It does not reconcile two histories,
  conflicting weights, or duplicate saved queues. An import that merges is a defect even if it looks
  kinder (`waves/wave-b.md`).
- **A second backup schema.** §46: export is the exact V1 local-state root object. No wrapper, no
  metadata envelope, no compression.
- **Migration of older schema versions.** §48 requires `schemaVersion = 1`; anything else is rejected.
  `migrateState` (§45) is not ported by this item.
- **Cloud, share-sheet, or automatic backup.** No `ACTION_SEND`, no `android:allowBackup` change, no
  network path. Export performs no network request (§47).
- **Any storage permission.** The Storage Access Framework grants access per URI; the manifest gains
  nothing (`design.md` D2).
- **Any preference-weight arithmetic**, and any change to how `signalsApplied` is derived. §1.1.
- **Swipe, the Discover card, and the undo *engine*.** Item 008, concurrent on its own branch. This
  item touches Undo in exactly one way: an import clears the slot (`design.md` D6).
- **New dependencies.** `androidx.activity.compose` is already a dependency and supplies the launchers.
  `android/gradle/libs.versions.toml` is untouched.
- **Any change to `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `tests/**`, or
  `docs/v1/**`.** Amendment 6 confines this item to `android/`.

## 4. Scenarios

### 4.1 Export

### Scenario: export serializes the current state as the V1 root object

Given local state holding preferences, article snapshots, settings, and a last selected category\
When the reader exports\
Then the written bytes parse as the V1 local-state root object\
And every persisted article snapshot, every preference entry, the appearance, and the last category are
present\
And the document is accepted by the same validator that guards import

### Scenario: an exported document round-trips through import unchanged

Given a local state\
When it is exported and the resulting bytes are imported\
Then the resulting local state equals the original field for field

### Scenario: export writes nothing when the destination cannot be written

Given the reader chooses a destination that cannot be opened for writing\
When the export runs\
Then no local state is changed\
And the failure is announced\
And no partial document is reported as a success

### Scenario: the suggested filename carries a UTC timestamp

Given the reader starts an export\
When the document creator is offered a name\
Then it is `intentional-reading-backup-YYYYMMDD-HHMMSSZ.json`, in UTC

### 4.2 Import — what is accepted

### Scenario: a valid backup replaces local state wholesale

Given local state holding a Read Later queue and a History\
And a valid backup holding a different queue and history\
When the backup is imported\
Then local state equals the backup\
And nothing from the previous state survives — no merged record, no retained preference entry

### Scenario: an import rebuilds appearance, category, and counts

Given a backup whose appearance is Dark and whose last category differs from the current one\
When it is imported\
Then the applied appearance is Dark\
And the selected category is the backup's\
And the navigation counts and the Discover deck reflect the imported records

### Scenario: an import clears the undo slot

Given a populated undo slot\
When a valid backup is imported\
Then the undo slot is empty\
And Undo is reported as unavailable

### Scenario: an import recovers a store that was locked for recovery

Given stored local state that failed validation on load, so writes are refused pending a reset\
When a valid backup is imported\
Then it is written\
And subsequent ordinary writes succeed\
And the recovery notice is withdrawn

### 4.3 Import — what is refused, and what it leaves behind

### Scenario: an oversized file is refused without being read into state

Given a candidate larger than 5 MiB\
When it is imported\
Then it is refused as too large\
And local state is unchanged\
And the refusal does not depend on the size the document provider reported

### Scenario: a candidate that lies about its size is still refused

Given a candidate whose provider reports a small size but whose stream exceeds 5 MiB\
When it is imported\
Then it is refused as too large\
And no more than the cap plus one byte is ever held

### Scenario: malformed, wrong-schema, and structurally invalid candidates are all refused atomically

Given a candidate that is not JSON, or whose `schemaVersion` is not 1, or that fails record validation\
When it is imported\
Then local state is unchanged\
And the stored file is unchanged\
And the reader is told the import was not completed and current local data was not changed

### Scenario: a refusal does not disclose the candidate's contents

Given any refused import\
When the failure is surfaced\
Then the message names neither the candidate's contents nor the validator's field path

### Scenario: a half-written import cannot be observed

Given a valid candidate\
When the write fails partway\
Then the previously stored document is still the one that loads\
And local state is unchanged\
And the failure is announced

### Scenario: cancelling the picker changes nothing

Given the reader opens the document picker and dismisses it without choosing\
Then no import is attempted\
And nothing is announced

### 4.4 The surface

### Scenario: import asks before it replaces

Given the reader has chosen a candidate\
When the confirmation is presented\
Then it names the file and states that current preferences, Read Later, History, dismissals, and local
settings will be replaced\
And cancelling it imports nothing

### Scenario: the local-data section states the limit and the replacement rule

Given the Settings sheet is open\
Then the import copy states the 5 MiB limit\
And states that a valid import replaces current local data rather than merging it

## 5. Verification

### 5.1 Gates

Both Android gates, re-run by the reviewer with `--rerun-tasks` in a throwaway worktree rather than
read from an implementer report:

```sh
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Baseline at wave B open: **163 tests**, green on `main` at `75f2821`.

### 5.2 Owner walkthrough

Driven over `adb` on the `Pixel_10` API 37 emulator, batched at the end of wave B against merged
`main`. Screenshot every step.

1. **Build a state worth exporting.** Save two articles, dismiss one, open and mark read a third, set
   Appearance to Dark, select a category.
2. **Export.** Settings → Export local data. Choose a destination in Files. Confirm the suggested name
   matches §46's pattern. Pull the file with `adb` and confirm it parses, carries `schemaVersion: 1`,
   and contains all four records.
3. **Destroy the state.** Settings → Reset. Confirm Read Later, History, and the appearance are back to
   defaults.
4. **Import it back.** Settings → Import local data. Pick the file. Confirm the confirmation panel names
   the file. Accept. All four records return, Dark returns, and the category returns.
5. **Refuse a malformed file.** `adb push` a truncated copy and import it. The message is
   `Import was not completed. Current local data was not changed.` and step 4's state is intact.
6. **Refuse an oversized file.** `adb push` a 6 MiB file with a `.json` name and import it. It is
   refused as too large, and nothing is read into state.
7. **Cancel the picker.** Open the picker and back out. Nothing is announced and nothing changes.
8. **Recovery path.** Corrupt the stored file directly (`run-as` + a truncating write), relaunch to get
   the recovery notice, then import the good backup. It is accepted, the notice clears, and an ordinary
   action — save an article — persists afterwards.
9. **No permission was added.** `aapt dump permissions` on the built APK shows the same permission set
   as at wave A close.
10. **TalkBack.** The export button, the import button, the confirmation's two controls, and the
    too-large notice are all reachable and labeled.

**What the owner is asked for, and only this:** a **real SAF round trip on hardware** — export to
Drive or Files on a physical device, reboot, import back. Provider behaviour differs between the
emulator's stub provider and a real one, and that difference is exactly what the emulator cannot show
(`waves/wave-b.md` §Owner checkpoints 3).
