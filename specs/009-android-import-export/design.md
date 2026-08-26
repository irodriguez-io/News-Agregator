# 009 — Design note

Decisions for `spec.md`. Where the web client already answers a question, the answer is ported and
cited rather than re-derived.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths: `android/**`. Forbidden:
`pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`.
The `ArticleDataset v1` contract is consumed as frozen.

**Concurrent with item 008 in wave B.** 008 owns `ui/components/ArticleCard.kt`,
`ui/screens/discover/**`, and a new `ui/gesture/` package. This item owns
`ui/screens/settings/SettingsSheet.kt`, `data/local/state/**`, and new IO under `data/`. Both edit
`ui/AppViewModel.kt` and `res/values/strings.xml`; both edits are additive. **008 merges first**
(`waves/wave-b.md`), so this branch rebases onto merged `main` and re-runs both gates on the rebased
head before its final review — the pre-rebase numbers are discarded (`waves/wave-a-note.md` §5).

## D1 — The `signalsApplied` hazard is accepted and recorded

Owner decision, 2026-08-26, against the two alternatives rather than in the abstract.

**Sanitizing the flags on export is not available.** Both validators enforce equality, not permission:
`signalsApplied.read == (status == READ)` and `signalsApplied.opened == (openedAt != null)`
(`LocalStateValidator.kt:137-145`; `js/state/storage.js:105-112`). A document with the flags cleared and
the statuses intact is rejected by *both* clients' import paths, so "sanitize" produces a backup nobody
can restore.

**Downgrading the statuses to match cleared flags is available and was declined.** It would mean
exporting every `read` and `opened` record as unseen, which contradicts `05-personalization-state.md`
§47 — export must *"include all persisted Article snapshots"* — and would silently discard the reader's
history in the one operation whose entire purpose is not to. That is an amendment, not a design
decision.

**A warning was declined** because it describes the defect instead of fixing it, and because
`05-personalization-state.md` and `06-ui-ux.md` author no warning copy for export; this item would be
inventing product copy about an internal inconsistency.

**What is actually true, and is why accepting is defensible:** the drift is not created by this item. It
already exists on every Android record on disk, and item 005 already owes a one-time reconciliation for
exactly that population (`specs/backlog.md` §005 — *"every record already on disk claims `signalsApplied`
flags with no deltas behind them"*). Imported records are the same population, reached by a different
road. Building a second mechanism here would give 005 something extra to unpick.

**Recorded for 005, so it is not rediscovered:** the reconciliation must consider records that arrived
by import, not only records this device created, and it must handle both directions — Android-derived
flags with no weight behind them, and browser weights whose flag was cleared by an Android
`MARK_UNREAD` (`ArticleStateMachine.kt:92-97` sets status `SAVED`, so `read` derives false) with no
decrement.

## D2 — The Storage Access Framework, with no dependency and no permission

`androidx.activity.compose` is already a dependency (`android/app/build.gradle.kts:57`), so
`rememberLauncherForActivityResult` with `ActivityResultContracts.CreateDocument("application/json")`
and `ActivityResultContracts.OpenDocument(arrayOf("application/json"))` is reachable today. Nothing is
added to `libs.versions.toml`.

**No storage permission is required or permitted.** SAF grants access to the single URI the reader
picked; `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `MANAGE_EXTERNAL_STORAGE` are all
unnecessary and all disproportionate. `AndroidManifest.xml` is expected to need **no change**; a
permission appearing in the diff is a report to the supervisor, not a decision. `spec.md` §5.2 step 9
checks the built APK with `aapt dump permissions`, the technique item 004 already used.

The launchers live at the composition root next to the existing Settings wiring, and the URI is handed
to the ViewModel as a stream, never as a `Uri` — `AppViewModel` keeps its property of having no
`android.*` import, which is what makes it JVM-testable and is the constraint 004 and 010 both worked
to preserve.

## D3 — The 5 MiB cap is enforced on bytes actually read

`05-personalization-state.md:1023-1048` sets the cap; `08-security-dependencies.md` Boundary C says an
imported backup is untrusted input *"even when the user believes they created it previously"*.

The browser checks `file.size` before reading (`js/ui/settings.js:121-127`) and can, because the handle
came from the local filesystem. A `content://` URI does not have that property: `OpenableColumns.SIZE`
is metadata supplied by whichever DocumentsProvider answered the picker, and it can be absent, stale,
or wrong. Trusting it is the Android-specific way to get this wrong.

So the cap is enforced twice, and the second one is the real one:

1. **Cheap early refusal** on the reported size when the provider offers one — the browser's pre-read
   notice, ported.
2. **Bounded read**: read at most `MAX_IMPORT_BYTES + 1` bytes from the stream and refuse if the
   stream is not exhausted at that point. Never allocate on a length the provider claimed.

The bounded read is a plain `java.io.InputStream` helper with no Android type in its signature, so
`spec.md` §4.3's "a candidate that lies about its size" scenario is a JVM test, not a device check.

## D4 — Import is a `LocalStateStore` operation and clears the recovery lock

`LocalStateStore` already refuses every save after a failed load, returning `RECOVERY_REQUIRED` until a
reset (`LocalStateStore.kt:49-57`). That is `08-security-dependencies.md` §27 implemented: malformed
state is not silently overwritten, and recovery requires explicit user action.

**Import is that explicit action**, which is why the browser passes `explicitRecovery: true` on the
import path and nowhere else (`js/state/storage.js:344`). So `LocalStateStore` gains one method:
validate the candidate bytes with the same `LocalStateValidator` the load path uses, write through the
same `LocalStateFile.write`, and clear `recoveryLocked` **only after the write succeeds**.

Everything §49 requires falls out of reusing that path rather than writing a new one: validation
precedes any write, a rejected candidate touches neither the file nor memory, and the write itself is
already temporary-file-plus-fsync-plus-rename (`LocalStateFile.kt:34-51`), so a failure partway leaves
the previous document intact. **No new atomicity mechanism is written for this item**, and a diff that
introduces one is a finding.

## D5 — Export serializes the validated in-memory state, never the file on disk

§47: export serializes *the current valid local state*. `LocalStateMapper.validate` then
`LocalStateMapper.encode` (`LocalStateMapper.kt:74-86`) is that operation and already exists; export
reuses it and adds no second encoder and no pretty-printer. Copying the stored file instead would
export a document that could be stale relative to memory, and would happily export a corrupt one.

The encoder is compact where the browser's is 2-space indented (`js/state/storage.js:307`). That
difference is deliberate and harmless: §46 fixes the *object*, not its whitespace, and each client's
import accepts the other's spacing. Authoring a second encoder to match the browser's formatting is
scope this item does not take.

## D6 — An import is adopted through the path that already exists

`js/app.js:349-355` does five things on a successful import: replace the state, clear the undo slot,
apply the appearance, re-render, report success. Android's equivalents already exist and are called in
that order — `adoptPersistedState` alone covers appearance, the night-mode push, and the last selected
category (`AppViewModel.kt:475-484`), and `publish()` rebuilds counts and the deck, which is §49's
*"reload logical state; rebuild counts/deck/theme"*.

Two clears are additions, and both were written down before this item existed:

- **The undo slot**, as `js/app.js:352` does, recorded in `specs/007-android-undo/design.md` D3 —
  *"import is item 009, which will need to clear it too"*. With 008 landing the producer in the same
  wave, this stops being theoretical between the two merges.
- **The held article and the recovery notice**, because the imported state may not contain the held
  record at all and the notice's cause has just been resolved.

All of it happens under the existing `stateMutex`. No second lock.

## D7 — One new error code, and a failure surface that discloses nothing

`LocalStateErrorCode` gains `IMPORT_TOO_LARGE` and nothing else; `MALFORMED_JSON`, `UNSUPPORTED_SCHEMA`,
`INVALID_STATE`, `READ_FAILED`, and `WRITE_FAILED` already exist and already carry every other cause.

The reader sees exactly two surfaces, which is what the browser shows:

| Cause | Surface |
|---|---|
| Too large | The inline notice, before anything is read (`js/ui/settings.js:123-127`) |
| Everything else | `Import was not completed. Current local data was not changed.` (`js/app.js:59`, verbatim) |

**`LocalStateResult.Failure.path` must not be rendered.** It names the field that failed validation,
which is a property of the candidate's contents; `08-security-dependencies.md` treats the backup as
untrusted input and the browser surfaces no field path either. It stays a debugging affordance.

This mirrors the constraint `specs/backlog.md` §Debt already records for `DatasetPhase.Error` — one
panel for four codes, no payload text — and is the same reasoning, not a new rule.

## D8 — Import confirms before it replaces, in the sheet, reusing the reset pattern

The browser confirms with the file name and an explicit destructive label
(`js/ui/settings.js:131-137`). Android already has that shape: `ResetConfirmation`
(`ui/components/ResetConfirmation.kt`) is an in-sheet confirmation panel with a cancel and a
destructive action, reachable and labeled. The import confirmation follows it rather than introducing
a system `AlertDialog`, so the sheet keeps one interaction idiom.

Copy is ported verbatim from the browser where the browser has it — the section copy at
`js/ui/settings.js:110`, the confirmation question at `:133`, and the two button labels at `:136-137`,
with the file name interpolated. New string resources are unavoidable here and are the only new strings
in wave B.

## D9 — The suggested filename is generated from the ViewModel's clock

§46: `intentional-reading-backup-YYYYMMDD-HHMMSSZ.json`. The `Z` is literal and the timestamp is UTC —
`js/app.js:68-80` is the reference implementation. `AppViewModel` already carries `nowProvider` and
`zoneProvider`, so the name is generated there and is JVM-testable against a fixed clock; the
Composable only passes it to `CreateDocument`.

SAF may rename on collision, and the provider may append or correct the extension. That is the
provider's business and is not something this item fights.

## D10 — Replacement, not merge, stated as a fixed decision

§50. There is no merge path, no partial import, no "keep my Read Later" option, and no reconciliation of
conflicting weights. This is listed among the slice plan's fixed decisions rather than left implicit,
because "merge would be kinder" is the single most predictable unrequested improvement in this item —
and `waves/wave-b.md` says so in as many words: *"An import that merges is a defect even if it looks
kinder."*

## D11 — Import clears the undo **offer** as well as the undo slot

Found at the wave B rebase, after item 008 merged, by re-running the gates on the rebased head. Recorded
because neither item could have found it alone.

Item 007's D3 required import to clear the undo slot, and D6 above carries that through:
`importLocalData` sets `undoRecord = null`. Item 008 then introduced `AppUiState.pendingUndoOffer` as
state **deliberately independent of the slot** — its own D6 says *"the offer is not the slot, and
withdrawing one must not withdraw the other"*, because the browser's toast disappears after 4500 ms while
`undoManager.peek()` stays populated.

Both decisions are right. Together they leave a gap: on the merged tree, an import empties the slot and
leaves the offer standing, so the reader is shown an Undo toast whose action is guaranteed to fail with
`UNDO_UNAVAILABLE` and announce *"Undo could not be completed."* A dead button, offered by the app itself.

**Import clears both.** The rule that separates them is about *time* — an offer expiring must not withdraw
Undo — not about *scope*. An import replaces the entire local state, so the record the offer refers to no
longer exists in any meaningful sense; there is nothing left to undo and nothing to keep the offer for.
Reset already clears both (item 008 slice 2), which is the same reasoning applied to the same kind of
event.

**Why it was invisible until the rebase.** On item 008's branch, import did not exist. On item 009's
branch, the offer did not exist. The defect exists only where both do, which is the merged tree — and the
only reason it surfaced before shipping is that `execution-model.md` §4 requires both gates re-run on the
rebased head rather than carrying the pre-rebase numbers forward. `waves/wave-b.md`'s merge order put 008
first precisely so this seam would be crossed once, deliberately, with a gate on the other side.
