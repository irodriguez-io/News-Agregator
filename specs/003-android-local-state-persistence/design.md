# 003 — Design note

Short by intent. Most of this item is a port of a frozen, working implementation; only the decisions
below are new, and each one is here because it could reasonably have gone another way.

## Workstream role

`android-client`, as established by item 002. Owned paths: `android/**` and nothing else. Forbidden:
`pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`,
`.github/workflows/{test,deploy}.yml`, and every file under `docs/v1/**`. The authoritative
specifications are read here, never edited; a conflict is escalated, per `docs/v1/README.md` §9.

## D1 — The Android document is the browser local-state root, verbatim

`contracts.md` §14 is titled *Browser* Local-State Root and §32 names `js/state/storage.js`. Android
could therefore have defined its own shape without touching a frozen contract at all. It will not.

The Android document is byte-for-byte the same JSON object: `schemaVersion`, `preferences.{sources,
topics}`, `articles` keyed by article ID, `settings.appearance`, `session.lastCategory`, with records
carrying a complete Article snapshot, the five timestamps, and `signalsApplied`. Same enumerations, same
bounds, same timestamp format.

Why: Amendment 6 forbids the Android client from widening, reinterpreting, or forking the frozen
vocabulary, and a second state shape is exactly the fork it means. Sharing the shape also makes item
004 (import/export) a file-picker problem rather than a translation problem — a backup taken in the
browser will simply load on the phone. The cost is carrying two fields Android never populates
(`preferences`, `signalsApplied`), which is cheap and honest: empty and all-`false` are the true values
for a client that applies no learning signals.

This is not a contract amendment. Android adopts an existing contract; it does not change one.

## D2 — One JSON file, atomic replace, no new dependency

`filesDir/intentional-reading-v1.json`, serialised with `kotlinx.serialization` (already a dependency
from slice 1 of item 002), written as: serialise to a temp file in the same directory → flush →
`FileOutputStream.fd.sync()` → `renameTo` the real path. Same-directory rename on the app's private
filesystem is the atomic replace; a kill mid-write leaves either the previous complete document or a
temp file that is ignored and overwritten next time. Never a half-written state document.

DataStore was the obvious alternative and was rejected on two grounds. It is a new dependency, and
`08-security-dependencies.md` §32 with `README.md` §11 set a low-dependency posture that item 002 held
to (zero libraries beyond AndroidX/Compose/serialization). More specifically, its corruption handling
wants to *replace* an unreadable file, which is the opposite of `08-security-dependencies.md:629-640`
step 2 — preserve the existing raw value. Fighting a library's default to reach the specified behaviour
is worse than writing forty lines of file IO. Room was rejected as a database for one small document
that already has a frozen JSON shape.

One object owns file access, mirroring "`js/state/storage.js` is the only module permitted to access
`localStorage`" (`05-personalization-state.md:66-69`). Nothing else in the app opens a file.

## D3 — The recovery lock is ported

`js/state/storage.js:189-211` refuses to persist after a malformed or incompatible read until an
explicit reset or import clears the lock. That is how the web satisfies "do not silently overwrite;
permit explicit reset" (`08-security-dependencies.md:629-640`). Android ports it as an in-memory flag on
the single store instance, which lives in `AppContainer` and therefore has process lifetime — the same
lifetime the web's `WeakSet` entry has for a page session. Reset clears it. Nothing else does.

## D4 — Strict parsing, so a missing key is not a null

The state document is read with the same `Json` configuration slice 1 fixed for the dataset:
`ignoreUnknownKeys = false`, `explicitNulls = true`, `isLenient = false`, `coerceInputValues = false`,
and no DTO property carrying a default. The web validator's `expectExactKeys` rejects unknown *and*
missing keys (`js/state/storage.js:45-52`); strict decoding plus explicit nullability reproduces that
without hand-rolling key-set comparison. Validation of values — ID pattern, status enum, timestamp
shape, preference bounds, category and appearance enums — stays explicit, ported rule for rule.

## D5 — Persist first, publish second

`05-personalization-state.md:918-946` fixes the order: derive → apply → persist → *then* update the UI.
The web implements it in `js/app.js:232-242` (`persistStateChange`), and Android must do the same:
`AppViewModel` derives the next records via the existing pure `ArticleStateMachine`, awaits the write,
and only assigns `records` and calls `publish()` on success. On failure the previous records stand and a
recoverable error is surfaced. The single exception is Open (`:960-966`): navigation to the publisher
proceeds regardless, with a warning when the write failed.

Writes are serialised through a `Mutex` on `Dispatchers.IO` — one writer, ordered. The write happens on
every action rather than at `onStop`, because `onStop` is not guaranteed before process death, which is
the exact failure this item exists to fix.

## D6 — Two small representation moves

`Appearance` currently lives in `ui/AppViewModel.kt:32-36` as a presentation enum. It is persisted
state, so it moves to `domain/model/Appearance.kt` and gains a `wireValue` (`light|dark|system`),
matching how `ArticleStatus` already carries one. Callers update their import; nothing else changes.
`Destination` stays where it is — it is not persisted.

`session.lastCategory` is a string including `all` (`contracts.md:1016-1041`), while Android models the
unfiltered case as `Category? = null`. The null ↔ `"all"` mapping lives in the storage mapper and
nowhere else.

## D7 — Auto Backup is turned off

`android:allowBackup="true"` (`AndroidManifest.xml:14`) becomes `false`, and
`android:dataExtractionRules` is set for the API 31+ backup and device-transfer paths. Persisted
reading state is a personal profile; copying it to a Google account is a remote user profile by any
reading of `01-product.md:605-629`, and Amendment 6 admits no backend. The reader's backup path is
export, in item 004, under the reader's own hand.

## D8 — No frame renders before the stored appearance is known

Reading the document is IO and must not block the main thread, but composing a light frame and then
repainting dark is a visible defect. So the app composes no content until local state resolves: the
state load starts in `AppViewModel.init`, and the shell renders nothing above the window background
until it completes. The read is a few kilobytes from app-private storage; the gap is a frame or two.

## D9 — `signalsApplied` is derived where the contract forces it, and false everywhere else

Discovered during slice 1 dispatch, and it corrects an error in this note's first draft. The web record
validator does not treat `signalsApplied` as free-standing bookkeeping; it asserts cross-field
consistency (`js/state/storage.js:105-112`):

```text
signalsApplied.opened  === (openedAt !== null)      // equality — forced
signalsApplied.read    === (status === "read")      // equality — forced
signalsApplied.dismissed ==> status === "dismissed" // implication only
signalsApplied.saved                                 // unconstrained
```

So "Android writes all-`false`" produces a document the frozen validator rejects the moment a record is
opened or read, which would defeat D1 outright. Android therefore derives the two forced flags from the
record's own state and leaves the other two `false`.

The asymmetry is not an oversight in the web validator, and it must be preserved rather than tidied into
a uniform "mirror the status" rule: `contracts.md` §24 and `05-personalization-state.md:714-730` make
Remove-from-Read-Later produce a `dismissed` status carrying **no** dismiss signal, because Remove is
explicitly not negative training. An implication, not an equality, is the only rule that admits that
record. Deriving `dismissed` from the status would mark a Remove as negative training the reader never
gave.

Nothing is lost by setting the two forced flags. They are not a claim that a preference delta was
applied; for `opened` and `read` they are structurally determined by data the record already carries,
and `contracts.md` §22's duplicate-signal protection is keyed on exactly that state — a web client
loading such a document would not have re-applied those signals anyway.

## Divergences from the web client, deliberate

- **No import/export, so no `MAX_IMPORT_BYTES` and no import validator.** The *state* validator is
  ported now and item 004 will reuse it; only the import wrapper is missing.
- **`exportState`/`importState` are not ported**, per the above.
- **The error is presented, not styled to match.** `06-ui-ux.md` §64 governs the web's presentation.
  Android surfaces the same information through the platform's own affordances and the live-region
  equivalent already used in item 002.

## Risks

- **A stored record can reference an article the bundled snapshot no longer contains.** This is intended
  (`05-personalization-state.md:150-173`) and it means Read Later and History must render purely from
  `ArticleRecord.article`. Item 002 slice 2 already required the record to hold the full snapshot rather
  than an ID, so this should cost nothing — but it is the first time that decision is actually load-
  bearing, and it is worth a test rather than an assumption.
- **Item 002 found three defects that only appeared on a device**, none of which a green JVM suite
  caught. Persistence is the one area where JVM tests are genuinely strong — a temp directory is a real
  filesystem — but process death, `run-as` inspection, and the first-frame theme check are not
  simulable. §5 of `spec.md` is the gate that covers them.
