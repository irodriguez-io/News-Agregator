# 003 — Android Local State Persistence

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/README.md` Amendment 6, `docs/v1/contracts.md` §§14–19/29/30/32,
`docs/v1/05-personalization-state.md` §§3–7/42–45/51–53, `docs/v1/01-product.md` §§21–23,
`docs/v1/06-ui-ux.md` §§64/66, `docs/v1/08-security-dependencies.md` §§26–27/32

---

## 1. Problem

Item 002 shipped a native client that renders the real product against the real dataset, and left one
limitation stated rather than hidden (`specs/002-android-client-foundation/spec.md` §3): **state is
in-memory only.** `AppViewModel` holds `records: Map<String, ArticleRecord>` in a field
(`android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/AppViewModel.kt:49`), appearance in a
`MutableStateFlow` initialised to `SYSTEM` (`:57`), and the selected category in another initialised to
`null` (`:60`). Every launch begins a fresh queue: saving an article, reading it, and dismissing three
others is erased by the next cold start.

That is not a small gap. It removes the product's premise. `01-product.md:558` — "Read Later Is Not an
Infinite Backlog" — and `:341` — the Article State Model — both describe a queue the reader returns to,
and `01-product.md:631-643` requires that an interacted article stay meaningful *after it leaves the
generated feed*. A client that forgets on exit has Read Later and History as decorations.

The behaviour to implement is not open: the web client already implements it, and its shape is frozen.

- `contracts.md:595-707` fixes the local-state root, the preference entry, and the persisted article
  record — including `signalsApplied` and the five timestamps.
- `05-personalization-state.md:918-1003` fixes the transactional commit order, the storage-failure
  rules, the Open exception, and the four `loadState()` outcomes.
- `js/state/storage.js` (349 lines) is the working implementation of all of it, including a recovery
  lock that refuses further writes after a malformed read rather than clobbering the stored bytes.

So this item is a port, not a design exercise. The one genuinely new decision — that the Android
document adopts the browser root shape verbatim instead of inventing an Android-shaped one — is recorded
in `design.md`.

**One defect is in scope because this item creates it.** `android/app/src/main/AndroidManifest.xml:14`
declares `android:allowBackup="true"`. Today that is inert: the app writes nothing. The moment reading
state lands in `filesDir`, Android Auto Backup copies it to the user's Google Drive account. That is a
remote copy of the personal reading profile, which `01-product.md:605-629` ("No analytics or telemetry…
must not introduce… remote user profiles") and Amendment 6 ("introduces no backend, no authentication,
no telemetry") both refuse. Persisting state and leaving that flag as-is would ship the exact thing the
specification forbids, by default and silently.

## 2. Story

As a reader, I want the articles I have saved, read, and dismissed — and how I have set the app up — to
still be there when I open the app tomorrow, so that Read Later is a real finite queue on my phone
rather than a list that empties itself every time Android reclaims the process.

## 3. Out of scope

- **Import and export.** `01-product.md:596-598` lists both under Settings and
  `05-personalization-state.md:1023-1125` specifies the format, the 5 MiB cap, validation, and atomic
  replacement. They need the Storage Access Framework, a document picker and creator, and the import
  validator's own error surface. Deferred to item 004. **Reset is not deferred**, because
  `08-security-dependencies.md:629-640` makes explicit reset the only recovery path out of corrupt
  stored state; persistence without it can strand the reader in an app that refuses every write.
- **Preference learning.** `preferences.sources` and `preferences.topics` are persisted, validated
  against the bounds in `contracts.md:632-655`, and round-tripped, but nothing in this item ever writes
  a non-empty entry. `signalsApplied` is likewise persisted and always all-`false`, which is the truth:
  the Android client applies no learning signals. Learning arrives with personalised ranking in a later
  item, and it will find its storage already in place.
- **Undo.** `contracts.md:1043-1064` states Undo is *not* persisted; it is memory-only and cleared by
  reload. This item therefore changes nothing about Undo, which remains unimplemented from 002 §3.
- **Networking.** No dataset fetch, no `INTERNET` permission. The bundled snapshot remains the only
  article source; a restored record whose article is absent from that snapshot renders from its own
  stored snapshot, which is precisely why §7 of the local-state spec exists.
- **Encryption of the stored document.** `08-security-dependencies.md:617-627` explicitly declines to
  encrypt local state in V1 and states no secret is stored there. Android matches: app-private storage,
  no keystore, no `EncryptedFile`.
- **Multi-process or cross-device state.** One process, one document, no ContentProvider, no sync.
- **Migration logic.** `schemaVersion` is 1 and no version 0 exists in the wild. A centralised
  migration entry point is required (`05-personalization-state.md:1003-1021`); a migration is not.

## 4. Scenarios

### Scenario: a fresh install starts from the default local state
Given the application has never been launched on the device
When the reader opens the application
Then Discover presents an article, Read Later and History show a count of zero, appearance is `system`,
and the category filter is `all`
And no state document exists on disk, because displaying articles is not a persistent interaction.

### Scenario: displaying an article creates no persisted record
Given a launched application with no stored state
When the reader browses Discover without acting on any card
Then no state document is written
And no article record exists.

### Scenario: triage survives process death
Given the reader saves one article, dismisses a second, and marks a third read
When the application process is killed and the reader launches it again
Then Read Later contains the saved article and its badge reads one
And History contains the read article grouped under Today
And the dismissed article is not presented in Discover
And each restored record carries the same status and the same timestamps it was persisted with.

### Scenario: an opened article is restored as opened
Given the reader opens an article from Discover and the process is then killed
When the reader launches the application again
Then the article's record is `opened` with `openedAt` populated
And the article remains eligible for Discover, per `contracts.md:713-719`
And its card offers `Mark read`.

### Scenario: Read Later survives the article leaving the dataset
Given a persisted `saved` record whose article ID is absent from the bundled dataset
When the reader opens Read Later
Then the article renders with its stored title, source, category, excerpt, tags, and reading time
And `Read article` opens its stored canonical URL.

### Scenario: appearance and category selection persist
Given the reader selects Dark appearance and the Technology category
When the process is killed and the application is launched again
Then the application renders in Dark from its first composed frame, without a light frame appearing first
And the Technology filter is already selected.

### Scenario: a failed write does not claim success
Given persistence will fail for the next write
When the reader activates `Save for later`
Then the article does not appear in Read Later, the badge count does not change, and the card is not
advanced
And a recoverable local-storage error is surfaced
And the previously stored document is left unchanged.

### Scenario: reading still works when persistence fails
Given persistence will fail for the next write
When the reader activates `Read article`
Then the publisher URL is still opened
And the reader is told the Open was not saved locally
And the application does not present the article as opened.

### Scenario: malformed stored state is preserved, not overwritten
Given the stored document contains bytes that are not valid JSON
When the reader launches the application
Then the application starts on a temporary default state and says so
And the stored bytes are unchanged
And any subsequent save, dismiss, or mark-read is refused with a recoverable error rather than replacing
the stored document.

### Scenario: an unsupported schema version is refused, not reinterpreted
Given the stored document is valid JSON whose `schemaVersion` is `2`
When the reader launches the application
Then a recoverable compatibility error is surfaced
And the document is neither reset nor reinterpreted as version 1.

### Scenario: a structurally invalid document is rejected whole
Given the stored document is valid JSON with `schemaVersion` 1 but carries an unknown top-level key, or a
status outside `opened|saved|dismissed|read`, or an article ID that is not twenty lowercase hex
characters, or a preference weight outside −5.0…+5.0
When the reader launches the application
Then no part of it is adopted
And the outcome is the same recoverable error and the same preserved bytes as a malformed document.

### Scenario: reset clears the device state after explicit confirmation
Given stored state containing preferences, saved articles, history, dismissals, Dark appearance, and a
selected category
When the reader opens Settings and activates Reset
Then a confirmation states that preferences, Read Later, History, dismissals, and settings will be
cleared on this device
And cancelling changes nothing
And confirming clears all of them, returns appearance to `system` and the category to `all`, removes the
stored document, and restores the ability to persist after a recovery-locked read.

## 5. Verification

Beyond the JVM suite, the owner walks these on a device or emulator with a debug build. Process death
means `adb shell am force-stop io.irodriguez.intentionalreading` — not backgrounding, and not the
activity recreation that a rotation produces, both of which item 002 already survives.

1. **Cold start, clean device.** `adb shell pm clear io.irodriguez.intentionalreading`, launch, confirm
   `adb shell run-as io.irodriguez.intentionalreading ls files/` lists nothing. Browse Discover, list
   again, confirm still nothing.
2. **Triage round trip.** Save one, dismiss one, mark one read, note the three titles. `force-stop`,
   relaunch, confirm all three landed as the scenarios describe. `run-as … cat files/…json` and confirm
   the document matches `contracts.md:657-692` field for field.
3. **Appearance and category.** Set Dark and a category, `force-stop`, relaunch, and watch the first
   frame for a light flash.
4. **Aged-out article.** Edit the stored document under `run-as` to hold a `saved` record whose ID is not
   in the bundled snapshot, relaunch, and confirm Read Later renders it in full.
5. **Corruption.** `run-as … sh -c 'echo not-json > files/…json'`, relaunch, confirm the error state, try
   to save an article, confirm the refusal, and confirm the file still reads `not-json`. Then reset from
   Settings and confirm saving works again.
6. **Schema refusal.** Replace `"schemaVersion": 1` with `2`, relaunch, confirm the compatibility error
   and that the file is untouched.
7. **Backup exclusion.** Confirm the merged manifest carries `android:allowBackup="false"` and that
   `adb shell bmgr backupnow io.irodriguez.intentionalreading` reports the package as not backed up.
