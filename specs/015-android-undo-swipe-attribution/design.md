# 015 — design note

Companion to `spec.md`. Records the decisions a slice plan cannot carry, and the two things
`waves/wave-d.md` asked this item to settle for the wave: **where the fix lands**, and therefore **whether
014 can follow it**.

---

## D1 — The fix is an identity guard in `AppViewModel`, not a change to the gesture

`ArticleCard`'s capture-at-DOWN is where the wrong article *comes from*, but it is not where the wrong
article can be *rejected*. The composable does not know what the published head is; the ViewModel does,
and it holds the mutex under which every article action is serialized.

So: `AppViewModel.onArticleAction` refuses an action whose article is not the article the last published
`DiscoverUiState.Card` names.

```kotlin
suspend fun onArticleAction(
    article: Article,
    action: ArticleAction,
    undoable: Boolean = false,
    expectDiscoverHead: Boolean = false,
): ArticleActionResult = stateMutex.withLock {
    val head = (_uiState.value.discover as? DiscoverUiState.Card)?.article?.id
    if (expectDiscoverHead && head != null && head != article.id) {
        return@withLock ArticleActionResult(
            transition = ArticleTransition.Invalid(
                records = localState.articles,
                action = action,
                fromStatus = localState.articles[article.id]?.status,
                preferences = localState.preferences,
            ),
            persisted = false,
            allowNavigation = false,
        )
    }
    …
}
```

Three properties matter, and they are the reason this shape was chosen:

- **It is mechanism-independent.** It does not depend on why the capture was stale — frame timing, pointer
  arbitration, attach latency, or something not yet named. Any stale capture produces a mismatch and any
  mismatch is refused. This item therefore does not need, and does not have, a 013-style diagnosis gate:
  the mechanism is already named (`spec.md` §1.2) *and* the fix does not rest on that naming being
  complete.
- **It compares against the last published state**, which is exactly the identity the composition would
  have had if it had kept up. Publish always precedes composition, so the comparison can produce false
  refusals inside the race window and never a false acceptance.
- **It is assertable where it sits.** `AppViewModelTest` drives `onArticleAction` directly and can name the
  article id in every assertion, which is the one thing `spec.md` §1.4 requires of any evidence here.

No new `ArticleTransitionErrorCode` is added. A distinct code would read better in a log, but it would
mean editing `domain/state/ArticleStateMachine.kt`, which is item 016's ground, to buy something no
assertion needs — the tests assert *no record and no weight moved for that article id*, which is the real
invariant and is stronger than a code.

## D2 — The guard is opt-in, and that is a deliberate trade against a destination check

The obvious alternative is to need no parameter at all: refuse when `_destination.value == DISCOVER` and
the article is not the head. On Discover the only articles a reader can reach *are* the head card's, so
the predicate is equivalent for real input.

It was rejected on test cost, and the reason is wave C's:

`AppViewModelTest` has 66 cases. `Destination` defaults to `DISCOVER`, and many cases drive an article
action without selecting a destination — including cases about Read Later and History. A
destination-keyed guard would refuse an unknown number of them, and the item's cost would land in
rewriting tests that are about something else. **That is precisely how wave C's cost landed**
(`waves/wave-c-note.md` §7: the collision matrix orders hub files by who *writes* them and asks nobody who
*asserts against* them).

`expectDiscoverHead: Boolean = false` moves the declaration to the caller. Four call sites in
`IntentionalReadingApp.kt` pass `true` — `onDismiss` (`:276`), `onSave` (`:280`), `onMarkRead` (`:283`) and
`onSwipeCommit` (`:286`). Everything else keeps the default and no existing test moves. It also reads as
what it is: *this action came from the Discover card, so it must be the card the reader is looking at.*

Cost of the choice, stated: a future call site that acts on the Discover card and forgets the flag is
unguarded. It is a four-line surface in one file and 014 and 016 both edit those same four lambdas, so both
of their slice plans carry an assumption naming it.

## D3 — Why this is not the artificial lag that 013 rejected

013 §1.6 rejected a post-swipe lockout, on its own measurements, for three reasons. This proposal is not
that proposal:

| 013's objection | Does it apply here? |
|---|---|
| The gate would have to span 0–0.5 s to cover the band, and 0.2 s works today | **No.** Nothing is time-based. A swipe at any delay whose article matches the published head is committed. |
| A silent refusal is the defect wearing a policy hat — the reader still swipes and nothing still happens | **No, and this is the crux.** In the window the card already does not translate and shows no cue (`spec.md` §1.3), because `graphicsLayer` reads the current gesture values while the handler mutates the captured ones. The refusal makes the record agree with the screen instead of contradicting it. |
| Making it honest needs a visible non-interactive card state, which `06-ui-ux.md` §39–§45 does not specify, and `AGENTS.md` forbids inventing one | **No.** Nothing new is shown, nothing is disabled, no copy is authored, no `docs/v1/**` sentence is needed. |

The surviving form 013 pre-registered — *hold the restored head article until the frame clock is quiet* —
is **not** adopted either. It would trade a determinate rule for a heuristic about frame quiet, and it
would put the fix back in the publish path where it has to be tuned. The guard needs no tuning.

## D4 — `OPEN` is left outside the guard, and what that leaves open

`onOpenArticle` (`IntentionalReadingApp.kt:138-145`) is one lambda passed to Discover as `onReadArticle`,
to Read Later as `onReadArticle` and to History as `onReopen`. Passing a Discover-only flag through it
means splitting it per destination — three call sites and a second lambda, in a file items 014 and 016 both
edit next.

Residual risk: inside the same one-frame window, tapping **Read article** could open the displaced
article's publisher and apply its First Open signal. It is the same window, it is not what the owner
reported, and it is no worse than today. If a wave-D walkthrough surfaces it, it is a follow-up item with
its own number, not a silent widening of this one.

Note also that the guard's refusal returns `allowNavigation = false`. That is why `OPEN` staying out
matters for a second reason: `contracts.md` §25 guarantees navigation proceeds when *persistence* fails,
and a refusal-by-identity is not a persistence failure. Rather than argue that line, this item does not
reach it.

## D5 — Rejected: re-attributing the swipe to the current head

Read `gesture.article` at release instead of at DOWN — a one-line change in `ArticleCard.kt` — and the
window closes by attributing the swipe to whatever is head when the pointer lifts.

Rejected. It commits an action against an article the reader never dragged, and it introduces a *new*
failure on a path that works today: a dataset refresh or a category change mid-drag would silently
re-point a legitimate 300 ms gesture at a different article. Refusal is the conservative direction —
invisible outside the window, and never wrong in a way that writes to disk.

It is also the change that would have put this item in `ArticleCard.kt` and collided with 014.

## D6 — The collision matrix, corrected for the wave

`waves/wave-d.md` left `ArticleCard.kt` as `?` for this item and said to record the answer either way.

| Hub file | 015, as designed |
|---|---|
| `ui/AppViewModel.kt` | ● signature, guard, refusal result |
| `ui/IntentionalReadingApp.kt` | ● four lambdas gain `expectDiscoverHead = true` (`:276`, `:280`, `:283`, `:286`) |
| `ui/components/ArticleCard.kt` | — untouched (D1, D5) |
| `ui/gesture/SwipeGesture.kt`, `ui/screens/discover/**` | — untouched |
| `domain/state/**` | — untouched (D1) |
| `docs/v1/**` | — no amendment |

**015 ∥ 012 stands unchanged** — `AppViewModel.kt` against `DiscoverScreen.kt`, nothing shared.

**014 is not blocked by an `ArticleCard.kt` collision, but 015 does touch `IntentionalReadingApp.kt`,
which 014 and 016 both own.** They are sequential by the wave's order, so this is a rebase note, not a
concurrency problem: 014's and 016's slice plans each assume the four Discover lambdas already carry
`expectDiscoverHead = true` and must preserve it. Both say so as a checkable assumption.
