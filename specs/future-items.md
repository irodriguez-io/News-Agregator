# Carry-forward notes for unstarted items

Not specifications. These are findings made while designing or implementing an earlier item that the
later item would otherwise have to rediscover. Each is a note to whoever runs `/feature-design` on that
item; none of them is approved scope, and none of them binds `docs/v1/**`.

Item numbers below are indicative. Numbers are allocated at design time, not here.

`backlog.md` tracks the whole queue — shipped items, everything still deferred, and what was
parked on purpose. The two notes here are the long-form detail behind two of its entries (005 and
009); the backlog carries the one-line version and points back at this file.

---

## The item that ports preference learning

*Raised by 003 (Android local state persistence). See `specs/003-android-local-state-persistence/design.md` D9.*

**Records written before learning existed carry `signalsApplied` flags with no deltas behind them.**

Item 003 persists `signalsApplied` because the frozen record validator requires it
(`js/state/storage.js:105-112`): `opened` must equal `openedAt` being non-null, and `read` must equal a
`read` status. Android derives those two and leaves `dismissed` and `saved` false. Nothing on Android
reads the field — it exists so the document stays interchangeable with the browser's, per D1.

The moment learning ships, the field stops being inert and acquires both of its real jobs: the
idempotency latch of `contracts.md` §22, and the reversal guard of §23, where `Mark Unread` subtracts
the Read delta *only if* one was previously applied. Undo Not Interested and Undo Save read it the same
way when Undo is ported.

Every record already on disk then looks exactly like a record whose deltas were applied. A `Mark Unread`
on an article read before learning shipped would decrement −0.25 on its source and −0.20 on each topic
that were never incremented. This needs no export and no second client; it is the app's own history.

**The fix is available precisely because the flags are there.** When learning initialises for the first
time, walk the stored records and fold in the deltas the flags claim were already applied — First Open
for every record with `opened`, Mark Read for every record with `read`. That makes the flags
retroactively true rather than merely structural, and the flags themselves say which deltas to apply.
It is a one-time reconciliation, it is bounded by the ±5.0 clamp either way, and it is far cheaper to
design in than to diagnose later as unexplained weight drift.

Whether to reconcile or to accept the drift is that item's decision. Making it deliberately is the
point of this note.

---

## The item that ports import and export

*Raised by 003. See `specs/003-android-local-state-persistence/design.md` D9 and §3.*

The same forced `read: true` carries the same hazard across a manual export/import, in the
Android → browser direction: an Android-read article imported into the web client and then marked unread
decrements weights the browser never incremented. Unlike the case above it requires a deliberate user
action and cannot occur while the two clients' stores stay separate, which they do today.

Also inherited from 003: the local-state validator is already ported to Kotlin
(`LocalStateValidator`), so the import path needs only the wrapper — the 5 MiB cap
(`05-personalization-state.md:1023-1048`), atomic replacement (§49), replacement-not-merge (§50), and
the Storage Access Framework surface. `exportState`/`importState` from `js/state/storage.js:303-345`
were deliberately not ported in 003.
