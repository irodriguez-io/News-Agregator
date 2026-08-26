# 007 — Design note

Decisions for `spec.md`. Where the web client already answers a question, the answer is ported and
cited rather than re-derived.

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths: `android/**`. Forbidden:
`pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`.
The `ArticleDataset v1` contract is consumed as frozen.

## D1 — Undo is engine-only in this item; the trigger is 008's

Settled with the owner, 2026-08-25, against the browser source rather than against the wave brief's
assumption.

The browser's undo-eligible call sites are exhaustively two, and both are in `js/ui/swipe.js`:
`attachSwipe`'s `onCommit` (`js/ui/discover.js:263`) and `installDiscoverShortcuts`' `onDismiss`/
`onSave` (`:268-269`). The three labeled triage buttons at `:246-248` call the same `perform` helper
without the flag, and its default is `false` (`:211`). `contracts.md` §31 confirms it: *"Only the most
recent eligible swipe action must be retained."*

Two alternatives were put to the owner and declined:

- **Extend Undo to Android's labeled buttons.** Rejected: it invents a requirement. It would also
  leave the clients disagreeing about which surfaces are reversible once 008 lands, and would need
  `contracts.md` §31 amended — which this workstream may not do (`AGENTS.md`: escalate, do not
  silently change contracts).
- **Pull 008 into wave A and design the two together.** Rejected: `execution-model.md` §5 sized wave A
  as one hard review plus two mechanical ones, and two hard reviews in one session is the exact
  ceiling that section warns about.

So this item delivers the slot, the inversion, the result codes, and the strings, and wires no
producer. The `undoable` parameter reaches `AppViewModel` and every caller in the tree passes `false`.
That is one wave of unreachable capability, accepted knowingly, in exchange for the `signalsApplied`
timing in `spec.md` §1.

**Instruction to the implementer:** do not "helpfully" wire the Save and Not interested buttons to pass
`true`. A diff that does is a finding, not a bonus.

## D2 — The undo record is a domain type, and it stores the whole prior record

The browser stores `{ articleId, action, previousRecord, preferenceSignal }`
(`js/state/article-state.js:141-147`); `contracts.md` §31 names the fourth field
`preferenceReversalData`. The Android type mirrors it:

```kotlin
data class UndoRecord(
    val articleId: String,
    val action: ArticleAction,          // constrained to SAVE or DISMISS
    val previousRecord: ArticleRecord?, // null means "there was no record"
    val preferenceReversal: PreferenceReversal? = null,  // always null in this item — D5
)
```

`previousRecord` is the **entire prior record**, not a status. That is what makes undo exact: restoring
it puts `firstSeenAt`, `openedAt`, and every `signalsApplied` flag back to the values they had, with no
timestamp rewritten to the moment of the undo (`js/state/article-state.js:180-181` assigns the stored
clone wholesale). A status-only record would silently lose `openedAt` and turn an undone dismiss into a
different article than the one dismissed.

`previousRecord == null` is meaningful and distinct from "absent": it means the article had no record,
so undo **deletes** the record rather than writing one (`:180`, `delete next.articles[...]`). This is
the only place in the Android client that removes a key from `LocalState.articles`; the implementer
should expect `LocalStateValidator` to be indifferent to it, and prove that with a test rather than
assume it.

The reachable set of `previousRecord` is provably small and the tests should say so. `allowedFrom`
(`ArticleStateMachine.kt:118-140`) admits `DISMISS` only from `UNSEEN`, `OPENED`, `DISMISSED`, and
`SAVE` only from `UNSEEN`, `OPENED`, `SAVED`; the same-status cases are idempotent no-ops that produce
no record (`:104-116`). So `previousRecord` is either `null` or an `OPENED` record. Nothing else.

## D3 — The slot lives in `AppViewModel`, in memory, behind the existing mutex

`contracts.md` §31 is explicit: *"Undo state is not persisted to localStorage. It exists only in active
application memory. Reloading the page clears Undo availability."*

The Android equivalent of "reloading the page" is process recreation, so the slot is a plain private
field on `AppViewModel` — **not** in `LocalState`, not in `SavedStateHandle`, not on disk. It therefore
survives a configuration change (the `ViewModel` does) and dies with the process, which is the closest
honest analogue and is stated in `spec.md` §4 as its own scenario so the behaviour is chosen rather
than discovered.

It is guarded by the existing `stateMutex` (`AppViewModel.kt:77`) alongside every other local-state
mutation. **Do not introduce a second lock** — the constraint item 004 established and this item
inherits.

Cleared on: a successful undo (take, not peek-and-leave), and a local data reset. The browser clears on
import and reset (`js/app.js:352`, `:361`); import is item 009, which will need to clear it too — noted
here so 009's designer does not have to rediscover it.

## D4 — The existing announcement timer is the wrong vehicle, and this item builds no replacement surface

The wave brief asks whether `IntentionalReadingApp.kt:80-87`'s six-second announcement is the right
toast. It is not, on three counts: it is a polite live-region announcement with **no action slot**, its
six seconds is not the browser's 4500 ms (`js/ui/toast.js:66`), and it auto-acknowledges rather than
offering anything to press. `AppAnnouncementKind` (`AppViewModel.kt:47-56`) is a fixed enum of
outcome messages, which is exactly what an undo toast is not.

The browser's undo toast is a genuinely different component: `showToast` renders a message plus an
`actionLabel` button, announces *"{message}. Undo available."*, and dismisses itself on the action or
after the duration (`js/ui/toast.js:22-64`). That is new UI on Android, and it is the one place the
wave brief allowed for new UI rather than reuse.

**It is still deferred to 008, and here is the reason.** Instrumented tests are parked from CI
(`specs/backlog.md` §Parked; 002 slice 4), so `:app:testDebugUnitTest` cannot observe a Composable. A
toast built in this item would be both unreachable *and* unverified by the only gate that runs. What
this item ships instead is the part the JVM suite can hold: the slot's state — availability, the
message kind, and the strings — exposed on `AppUiState` for 008 to render. 008 then adds one
`@Composable` over state that is already proven.

The two strings are authored here so 008 inherits copy rather than inventing it, and they are the
browser's, verbatim from `js/ui/discover.js:227-229`:

| Resource | Value |
|---|---|
| `undo_toast_saved` | `Saved to Read Later` |
| `undo_toast_dismissed` | `Not interested` |
| `undo_action` | `Undo` |
| `undo_completed` | `Undo completed.` |
| `undo_failed` | `Undo could not be completed.` |

`undo_completed` is `js/app.js:317`; `undo_failed` is `js/ui/discover.js:234`.

## D5 — The reversal field is carried and left null

`contracts.md` §23 requires Undo Not Interested and Undo Save for Later to reverse the `dismissed` and
`saved` signals and decrement the interaction counts. Neither signal is ever applied on Android today:
`ArticleStateMachine.kt:88-95` sets `saved` from `existing?.signalsApplied?.saved ?: false` and
`dismissed` from `existing?.signalsApplied?.dismissed == true && …`, and no code path anywhere sets
either to true in the first place. The browser's own gate is the same shape —
`preferenceSignalApplied ? EVENT_FOR_ACTION[action] : null` (`js/state/article-state.js:144`).

So `UndoRecord.preferenceReversal` is declared, always null, and never read. The undo path must **not**
touch `preferences`, and a test should assert that `preferences` is byte-identical across an undo.
Item 005 fills the field and adds `reverseInteraction`; leaving the field present now is what keeps 005
from having to reshape the record.

`signalsApplied` on the restored record is not recomputed — it is restored, as part of `previousRecord`
(D2). That is what `contracts.md` §22's at-most-once idempotency needs: re-deriving flags would let an
undone-then-redone action apply a signal twice once 005 exists.

## D6 — The held-article pin is **not** re-established on undo

The wave brief assumed it must be. The browser says otherwise, and the browser is the authority.

`heldOpenedArticleId` is assigned in exactly one place — a successful `open` (`js/app.js:287`) — and
cleared in two: an explicit category change (`:327`), and a render-time revalidation that drops it the
moment the held record stops being opened or leaves the deck (`:213-219`). The undo branch at
`:312-319` sets `state` and re-renders; it never touches the pin. Android's structure is already
identical: `_heldArticleId` is set only on `OPEN` (`AppViewModel.kt:319-320`) and
`clearHeldArticleIfNeeded()` (`:410-415`) drops it when the status leaves `OPENED`.

The reader is not bounced, because **dataset order returns the article on its own**.
`DiscoverDeck.build` falls through to `eligible.firstOrNull()` when there is no pin
(`DiscoverDeck.kt:25`). An article that was on screen was, by definition, ahead of every other eligible
article in dataset order; dismissing it removed it from `eligible` and promoted its successor; undoing
the dismiss makes it eligible again at its original index, which still precedes that successor. It
returns to head without a pin. `spec.md` §4 pins this with a scenario that asserts both halves —
the article is back on screen, *and* `heldArticleId` is still null — so a future implementer cannot
"fix" it by re-pinning and quietly diverge from the browser.

The pin exists for a different job: holding an `OPENED` card in place while a refresh adopts a new
dataset (004, D8) and while the reader returns from the publisher. Undo is not that job.

## D7 — Result modelling follows the existing `ArticleActionResult`, not a new shape

The browser returns `{ ok: false, code: "UNDO_UNAVAILABLE" | "UNDO_STALE" }`
(`js/state/article-state.js:152-158`). Android already has `ArticleTransition.Invalid` and
`ArticleActionResult` carrying `persisted` and an optional `failure`; the undo path returns the same
family rather than a parallel one, so `spec.md`'s "fails as unavailable" and "fails as stale" are two
distinguishable outcomes on the existing type. `contracts.md` §38's state-transition result contract
is what both sides are conforming to.

A failed **write** during undo is a third, different outcome: state unchanged, slot retained, and the
existing `AppAnnouncementKind.PERSISTENCE_FAILED` announced — reusing `recordPersistenceFailure`
(`AppViewModel.kt:405-408`), not a new kind. The slot is retained on write failure specifically so the
reader can try again; the browser does the same by peeking rather than taking until persistence
succeeds (`js/state/article-state.js:231-240`).
