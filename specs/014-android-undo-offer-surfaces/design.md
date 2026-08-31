# 014 — design note

Companion to `spec.md`. One structural decision and its consequences, plus the two rejected shapes and the
list of existing assertions this item changes.

---

## D1 — Undo eligibility becomes a property of the action, not an argument from the caller

Today eligibility is supplied by whoever calls the ViewModel:

```kotlin
// AppViewModel.kt:179-188, :372-401
fun launchArticleAction(article, action, undoable: Boolean = false, onComplete)
suspend fun onArticleAction(article, action, undoable: Boolean = false)
// ArticleStateMachine.kt:144
val undoRecord = if (undoable && action in reversibleActions) { … } else null
// IntentionalReadingApp.kt:290 — the only true in the app
undoable = true,
```

After this item, the `undoable` parameter does not exist. `ArticleStateMachine.transition` builds an undo
record whenever `action in reversibleActions` and the transition applied something, and
`AppViewModel.persistArticleTransition` raises the offer whenever the persisted transition carries one:

```kotlin
// ArticleStateMachine.kt
val undoRecord = if (action in reversibleActions) { … } else null
// AppViewModel.persistArticleTransition
transition.undoRecord?.let { record ->
    undoRecord = record
    raiseUndoOffer(record.action)
}
```

Four things follow, and they are the whole justification:

- **It is assertable where it sits.** The decision moves from a composable, which no JVM test can observe,
  into the state machine and the ViewModel, which 66 + 15 existing cases already drive directly. Every
  scenario in `spec.md` §4 becomes a real assertion. The alternative shape (D2) cannot be tested at all
  without an emulator, and instrumented tests are out of CI (002 slice 4).
- **It cannot be got wrong per surface.** The defect this item fixes exists *because* eligibility was a
  per-call-site opt-in and eleven call sites forgot it. Removing the opt-in removes the class.
- **It makes item 016 a one-line wiring change.** 016 widens `reversibleActions` and the offer follows to
  every surface automatically, with no new call-site audit. That is the re-cut in `waves/wave-d.md` paying
  off.
- **It deletes a redundant gate.** `undoable && action in reversibleActions` had two conditions and only
  one of them carried information. `waves/wave-d.md` names "two independent gates" as 016's hazard; this
  removes one of them before 016 arrives.

Cost, stated: any *future* surface that performs a `SAVE` or `DISMISS` raises an offer whether its author
thought about it or not. Today `SAVE` and `DISMISS` are performed only from Discover — the swipe and the two
triage buttons (`IntentionalReadingApp.kt:276`, `:280`, `:286`) — so nothing changes anywhere else in this
item. After 016 the same property is what makes Read Later's *Remove* work, so it is the intended direction
rather than a leak.

## D2 — Rejected: pass `undoable = true` at the two button call sites

Two lines, no signature change, no test churn. Rejected on assertability alone: the change lives entirely
inside `IntentionalReadingApp`, and the only JVM test that could be written for it is a test of the
ViewModel path that was already passing. That is the shape of wave C's third plan defect — a DoD bullet
demanding proof at a layer with no observer, satisfied with an assertion that proves nothing
(`waves/wave-c-note.md` §2).

A Compose instrumented test *could* observe it — `ui-test-junit4` is already a declared dependency and the
`androidTest` source set exists with three tests in it. Rejected too: instrumented tests are out of CI, so
the assertion would not run at any gate, and 013 established that this harness idles until composition
settles, which makes it a poor place to prove anything about when an offer appears.

## D3 — Rejected: derive eligibility from the destination or from a surface enum

`onArticleAction(article, action, source = ActionSource.DISCOVER_SWIPE)` keeps the old rule expressible and
would let the offer be raised per surface. It is more code, it re-introduces the per-call-site opt-in this
item exists to remove, and it encodes a distinction Amendment 8 deletes. The amendment's whole content is
that the offer follows the *action*; the code should say the same thing in one place.

## D4 — What Amendment 8 has to carry for this item, and what item 016 adds

This item needs only the **surface** half of the amendment:

> An eligible action may raise the undo offer from any surface that performs it. The trigger — swipe,
> labelled control, or keyboard shortcut where one exists — does not determine reversibility; the action
> does.

Item 016 needs the **action** half (which actions become reversible, and the reversal arithmetic). Both
halves are written in one amendment because §31, §36 and §70 each state both in a single sentence
(`spec.md` §1.3). Nothing in the amendment obliges the browser to change, so this stays an Android-only item.

`06-ui-ux.md` §45's toast examples are labelled *"Examples"*, so they are illustrative and no new copy is
needed here — `SAVE` and `DISMISS` already have their strings.

## D5 — Existing assertions this item changes, named, with why

`waves/wave-d.md`'s rule 1: state what should still be proved, never write "no existing assertion may be
edited", and say why each change is necessary. This is the complete list as the tree stands today; the
implementer reports any case not on it before touching it.

| Test | What happens | Why |
|---|---|---|
| `ArticleStateMachineUndoTest.kt:85` `a commit that is not marked undo-eligible offers nothing` | **deleted** | It asserts the behaviour of the `undoable` flag. The flag ceases to exist, so the case has no subject. Nothing it proved is lost: `only save and dismiss are reversible` (`:99`) proves the remaining half. |
| `ArticleStateMachineUndoTest.kt:99` `only save and dismiss are reversible` | **kept, and must still pass**; drop the `undoable = true` argument | This item does not widen the action set. This case is the guard that it did not, and item 016 is the item that rewrites it. |
| `ArticleStateMachineUndoTest.kt:30`, `:61`, `:127`, `:147`, `:184`, `:207`, `:232`, `:314`, `:352`, `:385` | **kept**; drop the `undoable = true` argument where present | Mechanical. Every assertion keeps its subject and its expected values. |
| `AppViewModelTest.kt:978` `a labeled button press is still not undo-eligible` | **replaced** by `a labeled button press raises the same offer as a swipe` | It encodes 007 §1.1 and 008 D8 — the rule Amendment 8 overturns. Its `MARK_READ` and `OPEN` cases survive in the replacement, because those two still raise nothing. |
| `AppViewModelTest.kt:1073` `launchArticleAction threads undo eligibility to the slot` | **rewritten** as `a launched save raises the offer` | There is no eligibility left to thread. What must still be proved is that the launcher's asynchronous path publishes the offer. |
| `AppViewModelTest.kt:962`, `:1040`, `:1094`, `:1147`, `:1164`, `:1198`, `:1223`, `:1246`, `:1318`, `:1340`, `:1459` | **kept**; drop the `undoable = true` argument | Mechanical. |
| `UiStateMapperTest.kt:543-549` | **unchanged** | `undoAvailable` still derives from the action, and the action set is unchanged. |
| `specs/008-android-swipe-gestures/spec.md` §5 step 6 (walkthrough: *absence* of a toast on the button path) | **superseded by Amendment 8** — recorded in this item's `evidence.md`, not edited | A shipped item's evidence is history. The amendment is the record of the reversal. |

Nothing in `ArticleCardGestureTest`, `ArticleCardScrollGestureTest` or `SwipeGestureTest` changes.

## D6 — Collision record for the wave

| Hub file | 014, as designed |
|---|---|
| `domain/state/ArticleStateMachine.kt` | ● one condition at `:144` |
| `ui/AppViewModel.kt` | ● two signatures, `persistArticleTransition`'s offer branch |
| `ui/IntentionalReadingApp.kt` | ● remove `undoable = true` at `:290`; **preserve** 015's `expectDiscoverHead = true` at four lambdas |
| `ui/components/ArticleCard.kt`, `ui/gesture/**`, `ui/screens/**` | — untouched |
| `res/values/strings.xml`, `ui/AppUiState.kt`, `ui/state/UiStateMapper.kt` | — untouched (item 016's) |
| `docs/v1/**` | Amendment 8, committed before the branch is cut |

**014 and 016 still cannot be concurrent** — 016 edits `ArticleStateMachine.kt:254` and
`AppViewModel.raiseUndoOffer`, both of which this item touches. The wave's sequence stands.
