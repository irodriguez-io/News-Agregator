# 016 — design note

Companion to `spec.md`. `spec.md` §2 is the arithmetic; this is how the code expresses it, what was
rejected, and which existing assertions change.

---

## D1 — The undo record gains an opposite direction, as a second field rather than a rename

`UndoRecord.preferenceReversal: PreferenceReversal?` can only say *reverse event E*. Undo of `MARK_UNREAD`
has to say *apply event E* (`spec.md` §2.2). One field:

```kotlin
data class UndoRecord(
    val articleId: String,
    val action: ArticleAction,
    val previousRecord: ArticleRecord?,
    val preferenceReversal: PreferenceReversal? = null,
    val preferenceReapplication: PreferenceEvent? = null,
) {
    init {
        require(preferenceReversal == null || preferenceReapplication == null) {
            "An undo record reverses a signal or re-applies one, never both"
        }
    }
}
```

`ArticleStateMachine.transition` sets exactly one of them:

- the existing `preferenceSignalApplied` path sets `preferenceReversal`, and it must read
  `preferenceReversals[action]` rather than `.getValue(action)` once the action set is wider — `REMOVE` and
  `MARK_UNREAD` have no entry, and `getValue` on a missing key throws. It is unreachable today only because
  the wider set does not exist yet;
- a new `preferenceSignalReversed` flag, set in the `MARK_UNREAD` branch (`:121-127`), sets
  `preferenceReapplication = PreferenceEvent.MARK_READ`.

`ArticleStateMachine.reverse` gains the mirror of the branch it already has, using the same
`current.article` the reversal path uses:

```kotlin
val nextPreferences = when {
    undoRecord.preferenceReversal != null ->
        PreferenceLearning.reverse(preferences, current.article, undoRecord.preferenceReversal.event)
    undoRecord.preferenceReapplication != null ->
        PreferenceLearning.apply(preferences, current.article, undoRecord.preferenceReapplication)
    else -> preferences
}
```

**Rejected: replacing both fields with one `PreferenceRestore(event, direction)`.** It is the better type and
it would have been the choice on a green field. It renames a field that `SAVE` and `DISMISS` undo already
depend on, which churns assertions in two test files for a readability gain, against `AGENTS.md`'s "avoid
unrelated scope and refactoring". The `init` block buys the same invariant at the same place.

**Rejected: deriving the preference effect in `reverse` by diffing `signalsApplied` between the current and
previous records.** It removes the need for any field, and it is a second implementation of the delta rules
sitting beside `PreferenceLearning` — two sources of truth for §21's arithmetic, which is precisely the
coupling item 007 was ordered first to avoid.

## D2 — Widen the set in one place, and delete the duplicate of it

```kotlin
// ArticleStateMachine.kt:254
private val reversibleActions = setOf(
    ArticleAction.SAVE,
    ArticleAction.DISMISS,
    ArticleAction.MARK_READ,
    ArticleAction.MARK_UNREAD,
    ArticleAction.REMOVE,
)
// :263 — MARK_READ's reversal event; the enum case already exists in UndoRecord.kt:12
private val preferenceReversals = mapOf(
    ArticleAction.SAVE to PreferenceReversal.SAVE_FOR_LATER,
    ArticleAction.DISMISS to PreferenceReversal.NOT_INTERESTED,
    ArticleAction.MARK_READ to PreferenceReversal.MARK_READ,
)
```

`REMOVE` gets **no** `preferenceReversals` entry and **no** `preferenceEvents` entry, per `contracts.md` §24
(`spec.md` §2.1). That is the whole of `REMOVE`'s arithmetic: none.

`UiStateMapper.kt:70` currently reads:

```kotlin
undoAvailable = undoAction == ArticleAction.SAVE || undoAction == ArticleAction.DISMISS,
```

which is a **second copy of `reversibleActions`** in a different layer. It becomes
`undoAvailable = undoAction != null`, because the ViewModel only ever holds a record for a reversible
action — the state machine already decided. Deleting the duplicate is the point: the wave-C lesson is that a
rule asserted in two places gets changed in one.

## D3 — Where the offer comes from, and why it is one line

After item 014, `persistArticleTransition` raises the offer for any transition carrying a record
(`specs/014-android-undo-offer-surfaces/design.md` D1). So widening the set is enough to make every one of
`spec.md` §1.1's four reported surfaces raise an offer, with **no call-site audit** — Read Later's two rows,
History's row, and Discover's *Mark read* button all go through `launchArticleAction`
(`IntentionalReadingApp.kt:259-264`, `:283-285`, `:303-305`).

What still has to be written by hand is the message:

```kotlin
// AppUiState.kt
enum class PendingUndoMessage { SAVED, DISMISSED, MARKED_READ, MARKED_UNREAD, REMOVED }
// AppViewModel.raiseUndoOffer
ArticleAction.MARK_READ -> PendingUndoMessage.MARKED_READ
ArticleAction.MARK_UNREAD -> PendingUndoMessage.MARKED_UNREAD
ArticleAction.REMOVE -> PendingUndoMessage.REMOVED
ArticleAction.OPEN -> error("Open cannot raise an Undo offer")
```

The `error` branch stays and narrows to `OPEN` alone. It is unreachable — `OPEN` is not in
`reversibleActions`, so no record exists to raise an offer from — and it is kept as the guard that says so.

## D4 — The destination-crossing question, answered by the record's shape

`waves/wave-d.md` asks whether an offer raised in Read Later should survive a destination change. **Yes, and
no code is needed for it.** `selectDestination` does not clear the record (`AppViewModel.kt:151-153`); the
record names an article id, so `reverse` restores that article regardless of which pane is on screen; and
`UndoToast` is already hosted outside the destination `when`.

What *is* needed is the assertion. The wave's second extra evidence obligation — *a reader who undoes in one
pane and then acts in another does not double-apply or double-reverse a delta* — is closed by one
`AppViewModelTest` case that exercises the hardest combination available:

mark A read in `READ_LATER` → `selectDestination(HISTORY)` → mark B unread → `performUndo()`.

That crosses panes, uses the single-slot replacement rule, and lands on the re-application path. Assert B
read with `signalsApplied.read` true and its weights restored, **and A untouched with its weights moved
exactly once** — by source id and topic id, not by an aggregate.

## D5 — Existing assertions this item changes, named, with why

`waves/wave-d.md`'s rule 1. Complete as the tree stands today; the implementer reports anything not on this
list before touching it.

**One row was added after the design pass** — see the `:30` row and its reason. That is the protocol
working, not a defect in it: the list is named cases with reasons, the implementer reported before editing,
and the addition is recorded here rather than absorbed into the diff.

| Test | What happens | Why |
|---|---|---|
| `ArticleStateMachineUndoTest.kt:99` `only save and dismiss are reversible` | **rewritten** as `only Open is not reversible`, keeping the per-action table shape | Its subject is exactly what this item changes. What must still be proved: that a per-action list is checked, and that `OPEN` carries no record. `MARK_READ`, `MARK_UNREAD` and `REMOVE` move from the "no record" column to the "record" column with their §2 arithmetic. |
| `ArticleStateMachineUndoTest.kt:30` `reversible actions carry undo state without caller eligibility` | **rewritten** — `MARK_READ` drops out of the non-reversible loop at `:49`, leaving `OPEN` alone | **Added 2026-08-31, at slice 1's preflight, on the implementer's report.** This case did not exist in this form when the design pass read the tree — item 014 created it to prove that the offer follows the record rather than a caller argument. Its subject is caller eligibility, and that subject is untouched: `SAVE` still carries a record with no eligibility argument and `OPEN` still carries none. Only the incidental claim that `MARK_READ` is non-reversible moves, and Amendment 8 is what overturns it. `MARK_READ`'s new reversibility is asserted in the rewritten `only Open is not reversible`, so keeping it in this loop would duplicate that case rather than add cover. |
| `ArticleStateMachineUndoTest.kt:127` `an idempotent no-op produces no undo record` | **kept, must still pass**, and extended with `MARK_READ` on an already-read article | `isIdempotentNoOp` is unchanged, so an idempotent `MARK_READ` must still produce nothing. Widening reversibility must not widen what counts as a change. |
| `ArticleStateMachineUndoTest.kt:314` `Undo Dismiss reverses the signal applied by the forward transition` and `:352`, `:385` | **kept unchanged** | The `SAVE` and `DISMISS` arithmetic does not move. If any of these three breaks, D1's new field has leaked into the existing path — stop and report. |
| `UiStateMapperTest.kt:543-549` | **extended**; the four existing assertions keep their expected values | `null → false`, `SAVE → true`, `DISMISS → true` all stay true under `undoAction != null`. Three cases are added for the new actions. |
| `AppViewModelTest` — any case that performs `MARK_READ`, `MARK_UNREAD` or `REMOVE` and asserts no offer or `undoAvailable == false` | **each reported, then updated** | These encode the narrow set. The implementer lists them at slice 2 before editing, because the count is not knowable from this document — item 014's diff will have moved some of them. |
| `ArticleStateMachineTest.kt` (forward transitions, 23 cases) | **expected unchanged** | This item does not touch `allowedFrom`, `isIdempotentNoOp` or any forward transition. A failure here means the change reached further than intended. |
| `PreferenceLearningTest.kt` | **expected unchanged** | `apply` and `reverse` are not modified (`spec.md` §2.3). **Count corrected 2026-08-31:** the file holds two classes — `PreferenceLearningDeltaTest` (parameterized) and `PreferenceLearningTest` (10 cases). The "11" here counted `@Test` annotations across the file, not cases in the class. Flagged by slice B's implementer against the gate XML; nothing is skipped. |

## D6 — Collision record for the wave

| Hub file | 016, as designed |
|---|---|
| `domain/state/ArticleStateMachine.kt` | ● `reversibleActions`, `preferenceReversals`, the `MARK_UNREAD` branch, `reverse` |
| `domain/state/UndoRecord.kt` | ● one field and an `init` |
| `ui/AppViewModel.kt` | ● `raiseUndoOffer` only |
| `ui/AppUiState.kt` | ● three enum cases |
| `ui/state/UiStateMapper.kt` | ● one line (`:70`) |
| `ui/IntentionalReadingApp.kt` | ● the `PendingUndoMessage` → string mapping at `:185-192` |
| `res/values/strings.xml` | ● three strings (`spec.md` §5) |
| `ui/screens/readlater/**`, `ui/screens/history/**` | — **untouched.** §1.3: there is no per-pane affordance to build, and the row actions already route through `launchArticleAction`. |
| `ui/components/ArticleCard.kt`, `ui/gesture/**`, `ui/screens/discover/**` | — untouched |
| `docs/v1/**` | Amendment 8, committed before item 014's branch is cut |

`waves/wave-d.md`'s matrix listed `ui/screens/readlater/**` and `ui/screens/history/**` for this item. **The
design pass removes both** — the screens already call the actions; only what happens inside the ViewModel and
the state machine changes.
