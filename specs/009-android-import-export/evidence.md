# 009 — Import and export — evidence

**Branch:** `feat/009-android-import-export` → `main`\
**Wave:** B (`specs/waves/wave-b.md`), second in the merge order, rebased onto merged `main` after 008\
**Implementer:** Codex, six sessions — three slices plus three findings follow-ups\
**Reviewer:** Claude, this session — authored the spec, design note, slice plan and this file, and
wrote no product or test code

---

## Commit chain

| Commit | Kind | Contents |
|---|---|---|
| `47435d8` | `docs(spec)` | `spec.md`, `design.md`, `slices.md` |
| `a4feaa6` / `aac8d2f` | RED / GREEN s1 | the store's export and import, the bounded read, the cap |
| `ff5a709` / `059f6d3` | RED / GREEN s1 fix 1 | canonical import encoding; export result-wrapped |
| `d90af0d` / `2343d24` | RED / GREEN s1 fix 2 | `LocalStateExport` as its own type |
| `3659b5e` | `docs(spec)` | slice 1 marked done |
| `04d61aa` / `0c3f33e` | RED / GREEN s2 | adoption, the clears, the filename, the announcements |
| `8451a30` / `dc3bbbe` | RED / GREEN s2 fix | state lock released before the destination write |
| `feff121` | `docs(spec)` | slice 2 marked done |
| `6b74ff7` | s3 | the SAF surface and the Settings local-data section |
| `391c338` | `docs(spec)` | slice 3 marked done |
| `a871a73` | s3 walkthrough fix | the sheet's status message pinned; refusal clears the panel |

| `661d953` | RED (rebase) | `pendingUndoOffer` rename following item 008 |
| `46f624c` | GREEN (rebase) | import clears the undo offer — `design.md` D11 |

SHAs above are post-rebase onto merged `main` at `f3ced7e`.

## Gates

Reproduced by the reviewer with `--rerun-tasks` in a throwaway detached worktree at every round, never
read from the implementer's report.

| Round | RED | GREEN |
|---|---|---|
| Slice 1 | compile failure on unresolved `readBounded`, `exportState`, `importState`, `MAX_IMPORT_BYTES`, `IMPORT_TOO_LARGE` | 170 tests, 0 failures |
| s1 fix 1 | 1 assertion failure — expected 976 bytes stored, found 5,242,880 | 171 tests, 0 failures |
| s1 fix 2 | compile failure on unresolved `LocalStateExport` | 171 tests, 0 failures |
| Slice 2 | compile failure on unresolved `importLocalData`, `IMPORT_COMPLETE` | 178 tests, 0 failures |
| s2 fix | **`TimeoutCancellationException` — a genuine deadlock**, 179 completed, 1 failed | 179 tests, 0 failures |
| Slice 3 | none; Compose and framework only, declared in the slice plan in advance | 179 tests, 0 failures |
| s3 walkthrough fix | none; Compose-only | 179 tests, 0 failures |

`:app:assembleDebug` green at every round. **163 → 179 tests.**

## What review caught

Four findings. None would have failed a gate; two were latent faults sitting one slice ahead of the code
that would have sprung them.

1. **`importState` persisted the candidate's raw bytes** rather than the canonical encoding of what it
   had validated. A valid backup padded with whitespace to 5 MiB imported successfully and left a 5 MiB
   local-state file on disk permanently, re-read and re-parsed on every load. More broadly, the stored
   document and the in-memory state agreed only by virtue of `ignoreUnknownKeys = false` in a different
   file. The browser persists the validated object (`js/state/storage.js:344`). Fixed to 976 bytes.
2. **`exportState` threw** where every other entry point on the class returns `LocalStateResult`, and sat
   outside the `boundary` guard.
3. **The first fix put `encodedBytes: ByteArray?` on the shared `LocalStateResult.Success`** — a nullable
   member on a type built in six places, five of which cannot populate it, with four `assertNotNull`
   calls in the tests restoring the guarantee the type had just given up. This is the pattern
   `waves/wave-a-note.md` §3 recorded from item 007. Export got its own `LocalStateExport` type instead.
4. **`exportLocalData` held `stateMutex` across the caller's `writeBytes` lambda.** Harmless while
   nothing called it; in slice 3 that lambda becomes a Storage Access Framework write to a user-chosen
   document, possibly Drive-backed and seconds long, dispatched by default on `Dispatchers.Main.immediate`
   and bypassing the repository's `withContext(ioDispatcher)`. Export mutates nothing, so the lock only
   ever needed to cover reading the state and encoding it. The RED for this fix is a real deadlock.

**Slice 3 passed first time with no findings.** The reason is worth recording: the IO-dispatch hazard
that finding 4 exposed was written into slice 3's brief before it was dispatched, so it was closed
before it could be written.

## The rebase onto merged `main`, and what it caught

Item 008 merged as `f3ced7e`; this branch was rebased onto it before its final review. Three conflicts,
**all additive** — both items had appended members to `AppAnnouncementKind`, branches to the same
announcement `when`, and declarations to the same block at the composition root. Every resolution kept
both sides; nothing was dropped.

**Then the rebased head failed to compile**, and that is the whole argument for `execution-model.md` §4's
rule that both gates are re-run on the rebased head rather than carrying pre-rebase numbers forward.

Two things came out of it:

1. **A stale reference.** Item 008 renamed `AppUiState.pendingUndoMessage` to `pendingUndoOffer`; one
   assertion in this item's `an import clears the undo slot` still used the old name. Git merged the file
   without a conflict, because the two items' edits touched different lines. Mechanical rename,
   `661d953`.

2. **A real cross-item defect** — `design.md` D11. Import cleared the undo *slot* but not the *offer*,
   because item 007 required the first and item 008 introduced the second as deliberately independent
   state. On the merged tree that means an import leaves a live Undo toast whose action is guaranteed to
   fail with `UNDO_UNAVAILABLE`. Neither branch could show it: 008 had no import, 009 had no offer. Fixed
   in `46f624c` by clearing both, mirroring what reset already does.

The implementer stopped and escalated rather than reshaping the failing test, which is the correct call —
a test failing on *behaviour* at an integration seam is information, not an obstacle.

**Gates on the rebased head, with prior results deleted first so the counts are unambiguously from that
run: 198 tests, 0 failures, `:app:assembleDebug` green.** 163 baseline + item 008's 19 + this item's 16.
The pre-rebase count of 179 is discarded — it described a tree that no longer exists.

## Owner walkthrough

Driven over `adb` on the `Pixel_10` API 37 emulator against the branch build, not handed over as a
checklist.

| Step | Result |
|---|---|
| Build a state, export it | Suggested name `intentional-reading-backup-20260826-154532Z.json` — §46's pattern, and genuinely UTC (device clock 09:45 local, UTC−6) |
| Inspect the exported document | Exact V1 root object: `schemaVersion` 1, four keys, all four records, `appearance: dark` |
| Reset | Local-state file removed; only the dataset cache remained |
| Import it back | All four records, Dark, and the category restored; Read Later 2, History 1 |
| Malformed candidate | Refused; stored file byte-identical — but see the defect below |
| Oversized candidate (6.29 MB) | Refused with the notice, visible and verbatim; nothing read into state |
| Cancel the picker | Nothing announced, nothing changed |
| Recovery path | Corrupt the stored file, relaunch to the recovery notice, import the good backup: accepted, notice cleared, and a subsequent save persisted (4211 → 5346 bytes) |
| Permissions on the built APK | `android.permission.INTERNET` only |
| Accessibility tree | Section copy, both buttons, reset and the radio labels all exposed and labeled |

**The defect the walkthrough found, which no JVM test could:** a refused import left the reader with no
indication anything had happened. The message was raised — verbatim from `js/app.js:59` — but the sheet
is a single scrolling column with the status as its last child, and the failure path deliberately kept
the ~250 dp confirmation panel above it, so the message rendered below the viewport and
auto-acknowledged after six seconds. The contrast made it unambiguous: the *too-large* refusal is
perfectly visible because it renders **instead of** the panel rather than after it. The stale panel also
survived closing and reopening the sheet. Fixed in `a871a73` by pinning the status outside the
scrollable column — which closes the same latent defect for the reset and export messages — and by
clearing the selection on a refusal.

**Not performed:** a full TalkBack gesture pass; the semantics tree was inspected instead.

**Owner judgement still outstanding:** a real SAF round trip on hardware — export to Drive or Files,
reboot, import back. Provider behaviour differs from the emulator's stub, and that difference is exactly
what the emulator cannot show.

## The inherited hazard, as shipped

`design.md` D1 records the decision to accept it. The walkthrough makes it concrete: the exported
document contains a record with `"read": true, "opened": true` while `preferences` is `{}`. A claimed
signal with no weight behind it, portable for the first time. Item 005 owns the reconciliation, and
`design.md` D1 records what it must cover — including the mirror case, where an Android `MARK_UNREAD` on
an imported browser record clears the flag while leaving the weight stranded.

## Scope

`git diff` across the item shows `android/gradle/libs.versions.toml` and `AndroidManifest.xml`
untouched. The Storage Access Framework needs no permission, and the built APK proves it.

## Outstanding

- The hardware SAF round trip and the TalkBack pass above.
- The recovery notice still reads *"Reset local data in Settings to recover."* Import is now also a
  recovery path, and the copy does not say so. Left alone deliberately: changing it means authoring copy
  the specification does not provide. Recorded in `specs/backlog.md` as debt.
