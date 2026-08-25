# Wave C — Preference learning and deck diversity

**Items:** 005, then 006 · **Prerequisite:** waves A and B merged, 007 in particular · **Cut from:** `main`

Self-contained brief. Read `AGENTS.md`, `docs/v1/README.md`, `specs/backlog.md`,
`specs/execution-model.md`, then this file. **Also read `specs/future-items.md` §"The item that ports
preference learning" in full before designing 005.** It is not a summary; it is the reasoning.

---

## Why this wave is different

**005 runs alone.** It touches `AppViewModel.kt`, `ArticleStateMachine.kt`, and `DiscoverDeck.kt` — the
three hub files of the entire Android client (`execution-model.md` §2). There is no second item that can
safely run beside it.

**006 follows immediately**, or lands as 005's final slice. Both change `DiscoverDeck.kt`, and 006 is
small enough that the sequencing costs almost nothing.

This is the largest item in the backlog and the only one carrying an irreversible data decision. Treat
the design pass as the deliverable, not as a preliminary.

---

## 005 — Preference learning and personalized ranking

Port `js/ranking/personalize.js` and the `INTERACTION_DELTAS` table (`js/state/preferences.js:1-6`).
`preferences.sources` and `preferences.topics` already persist, validate against `contracts.md:632-655`,
and round-trip; nothing writes a non-empty entry yet.

**Deferred by** 002 §3, 003 §3, 004 §3.

### The decision this item exists to make

Every record already on disk claims `signalsApplied` flags with no deltas behind them. 003 persists them
because the frozen record validator requires it (`js/state/storage.js:105-112`): `opened` must equal
`openedAt` being non-null, `read` must equal a `read` status. Android derives both and leaves `dismissed`
and `saved` false.

The moment learning ships, those flags acquire both of their real jobs — the idempotency latch of
`contracts.md` §22 and the reversal guard of §23 — and every stored record then looks exactly like a
record whose deltas were applied. A `Mark Unread` on an article read before learning shipped would
subtract −0.25 from its source and −0.20 from each topic that were never added. No export and no second
client is needed for this; it is the app's own history.

**The fix exists because the flags are there.** On first initialisation, walk the stored records and fold
in the deltas the flags claim were already applied — First Open for every record with `opened`, Mark Read
for every record with `read`. One-time, bounded by the ±5.0 clamp either way, and the flags themselves
say which deltas to apply.

**Reconcile or accept the drift is the owner's call.** Making it deliberately is the entire point.
Unexplained weight drift diagnosed six months from now is the expensive alternative.

### Also settle at design time

- **Where deltas are applied.** 007 put an undo slot in `AppViewModel`; §23's reversal guard now has to
  read `signalsApplied` on the way back out. Design the two together on paper before dispatching.
- **The ±5.0 clamp**, on both ends, and what happens at the boundary.
- **Whether personalized order replaces dataset order or reranks within it.** Discover currently renders
  in dataset order, legitimately, because the pipeline emits a total order (`pipeline/retention.py:12-20`).
- **`_heldArticleId` under reranking.** A rerank that moves the held card is a defect; the pin must
  still win (`DiscoverDeck.kt:25`).
- **Sequencing against 006.** If 006 is folded in as the last slice, say so in the slice plan.

---

## 006 — Deck diversity sequencing

The −8 same-source and −5 third-consecutive-category penalties (`js/ranking/deck.js:26-38`). Until this
lands, the Android head article can differ from the browser's for the same dataset.

**Deferred by** 002 §3, restated by 004 §3.

**Expected surface:** `domain/state/DiscoverDeck.kt` and its tests, only.

Small, well-specified, and fully JVM-testable — the browser's numbers are the specification. The only
real question is how the penalties compose with 005's weights, which is why it goes second.

---

## Gates

Per `execution-model.md` §8. Both fire `android.yml` only.

Two extra evidence obligations for this wave, given what it changes:

- **The reconciliation path needs a test proving it runs exactly once** and is idempotent across
  restarts.
- **A `Mark Unread` on a pre-learning record must be shown not to drift**, whichever decision the owner
  makes — a test that encodes the accepted behaviour either way.

---

## Owner checkpoints

1. **The reconciliation decision.** Blocks the design. Ask first, with the arithmetic in front of you.
2. **Whether personalized order replaces or reranks dataset order.** A product call about what Discover
   is.
3. **A walkthrough with real accumulated history** — not fresh state. The whole point of this item is
   behaviour that only appears after a reader has used the app for a while, and 004 showed that the
   walkthrough finds what the suite cannot.
4. **Wave sign-off** against merged `main`.

---

## Definition of wave done

Both merged; `evidence.md` per item; walkthroughs recorded; `backlog.md` updated. At that point the
Android client has closed every parity gap the shipped items deferred, and the only queue entries left
are the ones parked on purpose.
