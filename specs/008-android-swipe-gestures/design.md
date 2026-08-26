# 008 — Design note

Decisions for `spec.md`. Where the web client already answers a question, the answer is ported and
cited rather than re-derived. Where the two platforms genuinely differ — units, motion settings,
gesture arbitration — the decision is stated here rather than offered to the implementer as a choice
(`waves/wave-a-note.md` §4).

## Workstream role

`android-client`, as established by item 002 under Amendment 6. Owned paths: `android/**`. Forbidden:
`pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`, `tests/**`, `docs/v1/**`.
The `ArticleDataset v1` contract is consumed as frozen.

**Concurrent with item 009 in wave B.** 009 owns `ui/screens/settings/**`, `data/local/state/**`, and
`MainActivity.kt`. This item owns `ui/components/ArticleCard.kt`, `ui/screens/discover/**`, and a new
`ui/gesture/` package. Both edit `ui/AppViewModel.kt` and `res/values/strings.xml`; both edits are
additive and small, and the merge order is 008 first (`waves/wave-b.md`). 009 rebases after 008 merges.

## D1 — The threshold is `90.dp`, not `150.dp`

`06-ui-ux.md` §40 fixes the commit threshold at `90px horizontal travel`. That is a CSS pixel, defined
against a 96 dpi reference; Android's `dp` is defined against 160 dpi. Read as a *physical distance*,
90 CSS px is 0.9375 in, which is **150 dp** — roughly 36% of a 411 dp phone's width, and an
uncomfortable gesture.

**Parity is with the number, and the number is right, because on a touch device the browser's own CSS
pixel already is a dp.** A mobile browser's layout viewport is expressed in CSS pixels that map 1:1 to
density-independent pixels — a 411 dp-wide phone reports a 411 CSS px viewport. So a reader swiping the
web client on a phone travels 90 CSS px ≈ 90 dp, about 22% of the screen. `90.dp` reproduces what the
browser actually feels like on the device class this client runs on; `150.dp` would reproduce what it
feels like on a desktop trackpad, which is not the comparison that matters.

Both values were considered and this is recorded so it is not re-litigated at review. `90.dp` also sits
comfortably above the platform's own touch slop (~8 dp at typical densities), so the threshold is never
reached before the gesture is recognised.

**The threshold is not a fraction of screen width.** Material's `SwipeToDismissBox` uses a positional
fraction; the specification names an absolute distance and the browser implements one
(`js/ui/swipe.js:3`). Absolute it stays.

## D2 — The gesture is a pure object; the Composable is a shell

The one hard constraint on this item is that `:app:testDebugUnitTest` cannot see a Composable
(`spec.md` §1.2). The response is the same one 002–007 have used: put every decision somewhere the JVM
gate can reach, and leave the Composable holding nothing but plumbing.

`«pkg»/ui/gesture/SwipeGesture.kt` — a plain Kotlin object plus a small mutable state holder, **no
`android.*` import**, working in `Float` pixels:

```kotlin
object SwipeGesture {
    const val THRESHOLD_DP = 90f      // 06-ui-ux.md §40, D1
    const val INTENT_SLOP_DP = 8f     // js/ui/swipe.js:82
    const val HORIZONTAL_BIAS = 1.15f // js/ui/swipe.js:83
    const val ROTATION_DIVISOR = 34f  // js/ui/swipe.js:43
    const val MAX_ROTATION_DEGREES = 4.5f
    const val EXIT_FRACTION = 0.82f   // js/ui/swipe.js:104
    const val EXIT_MINIMUM_DP = 620f
    const val EXIT_DURATION_MS = 280  // 06-ui-ux.md §44
}
```

`Density` conversion (`dp` → px) happens in the Composable through `LocalDensity`; the object never
sees a `Dp`. That is what keeps its test a plain JUnit test with no Android runtime.

The holder is a state machine over pointer positions with three inputs — `down(x, y)`, `move(x, y)`,
`release()` / `cancel()` — and it answers three questions the Composable asks: *what is the intent*,
*what is the visual transform*, *what action, if any, is committed*. `spec.md` §4.1's ten scenarios are
exactly this object's tests, driven as synthetic pointer sequences. No Compose type appears in them.

**The intent lock is ported literally**, including the asymmetry: `js/ui/swipe.js:82-83` locks on the
first move that exceeds 8px in either axis, and locks horizontal only when
`abs(x) > abs(y) * 1.15`. Everything else is vertical, and once locked the intent never changes for the
rest of the gesture. The bias is what stops a diagonal scroll from stealing the card, and it is the
reason this is not `detectHorizontalDragGestures` — that detector's slop arbitration has no equivalent
of the 1.15 factor and would diverge from the browser silently.

## D3 — Pointer events are consumed only after the lock, and never in the initial pass

The card sits inside a `verticalScroll` `Column` (`DiscoverScreen.kt:57-62`) and contains three
clickable controls. Two conflicts have to be resolved, and both are resolved by *not* consuming:

- **Against the parent scroll.** The gesture runs in `pointerInput { awaitEachGesture { … } }` and
  calls `change.consume()` **only while the intent is horizontal**. A vertical or unlocked gesture
  leaves the pointer stream untouched, so `verticalScroll` sees it and scrolls normally. This is the
  documented Compose contract for cooperating with a scrolling ancestor.
- **Against the card's own controls.** The handler runs in the default (`Main`) pass, never
  `PointerEventPass.Initial`. Children get first refusal, and the buttons' own `clickable` consumes the
  down event before the card sees it — which reproduces `06-ui-ux.md` §39's "do not start card dragging
  from buttons, links, the category selector" without an equivalent of the browser's
  `INTERACTIVE_SELECTOR` list. `spec.md` §5 step 7 checks it on the device.

002 already hit a Discover card scroll defect on the emulator, and `waves/wave-b.md` names this as the
class of bug this item is most likely to introduce. §5 step 5 exists for it and is not optional.

## D4 — Commit, exit, and the refusal to finalize a failed write

`06-ui-ux.md` §43 orders it: card exits briefly → state action is processed → next card appears → toast
→ Undo becomes available. And: *"The UI must not visually finalize a save/dismiss if persistence
failed."* The browser does both by starting the 280 ms exit, awaiting `onCommit`, and calling
`restore()` when `actionFailed(result)` (`js/ui/swipe.js:103-131`).

Android does the same shape with the existing callback:

```kotlin
viewModel.launchArticleAction(article, action, undoable = true) { result ->
    if (result.persisted) raise the offer else restore the card
}
```

`ArticleActionResult.persisted` is the existing signal and the failure announcement is already emitted
by `persistArticleTransition` — **no new announcement kind for a failed swipe**, and no new failure
copy. The card restores; the live region already speaks (`local_state_write_failure`).

**The offset must be keyed to the article.** When the commit succeeds the deck advances and a *new*
article renders into the same Composable; if the translation state survives, the new card appears
already thrown off-screen. The gesture holder is `remember(article.id)`-scoped and reset on every
article change. This is the most likely defect in the item and its absence is invisible to every JVM
test.

## D5 — Reduced motion is read explicitly, not inferred from the animation system

Android has no `prefers-reduced-motion` media query. The platform signal a reader actually controls is
`Settings.Global.ANIMATOR_DURATION_SCALE == 0f` (Developer options → *Animator duration scale*, and
what accessibility "remove animations" guidance points at).

Compose may or may not scale animation durations by that setting on its own — current Android
documentation does not state it, and **the design does not depend on it either way**, because
`06-ui-ux.md` §48 does not ask for shorter motion. It asks for card rotation and large swipe exit
transitions to be *removed*. A zero-duration animation still applies the rotation; it just applies it
instantly. So the flag has to reach the arithmetic.

It reaches it the way 010's night-mode applier does (`specs/010-android-launch-theme/design.md` D4):
a `reducedMotion: () -> Boolean` capability with a `{ false }` default, real implementation reading
`Settings.Global` at the composition root, fake in tests. The `android.*` import stays out of both the
gesture object and `AppViewModel`. Under the flag, `rotationDegrees` and `exitDistance` return zero and
everything else — threshold, direction, action, toast — is unchanged (`spec.md` §4.1, last scenario).

## D6 — The undo offer is ViewModel state with an id, mirroring `AppAnnouncement`

007 exposed `AppUiState.undoAvailable` and `pendingUndoMessage` for this item to render
(`specs/007-android-undo/design.md` D4). Rendering them as-is does not work: two consecutive dismisses
produce byte-identical state, so a `LaunchedEffect` keyed on the value cannot tell the second offer
from the first and the toast would not re-appear.

The repository already solved this once. `AppAnnouncement(id, kind)` with a monotonic
`nextAnnouncementId` (`AppViewModel.kt:107-109`) is the established pattern for a transient message
that can repeat. The undo offer takes the same shape:

```kotlin
data class PendingUndoOffer(val id: Long, val message: PendingUndoMessage)
```

replacing the bare `pendingUndoMessage` field on `AppUiState`. Raised inside `persistArticleTransition`
when — and only when — an undo record was stored; withdrawn by `acknowledgeUndoOffer(id)`, by a
successful undo, and by reset.

**The offer is not the slot, and withdrawing one must not withdraw the other.** The browser's toast
disappears after 4500 ms while `undoManager.peek()` stays populated until the next commit, reset, or
import (`js/ui/toast.js:52-59`; `js/app.js:352`, `:361`) — which is why its `Z` shortcut still works
after the toast has gone. `spec.md` pins this as its own scenario so a future implementer does not
"tidy" the timeout into clearing the slot.

The alternative — holding the toast in local Compose state with its own token, as the browser does —
was rejected for one reason: it would make the entire offer lifecycle invisible to the only gate that
runs (`spec.md` §1.2).

## D7 — Undo's two outcomes get the two announcement kinds 007 left unwired

`undo_completed` and `undo_failed` were authored by 007 and are referenced by nothing.
`AppAnnouncementKind` has no undo member. This item adds exactly two — `UNDO_COMPLETED` and
`UNDO_FAILED` — and the two branches in `IntentionalReadingApp.kt`'s `when`. The browser announces the
same two strings at `js/app.js:317` and `js/ui/discover.js:230`.

A *failed write during undo* is a third outcome and is **not** one of these: 007 already routes it to
the existing `PERSISTENCE_FAILED` and retains the slot so the reader can try again
(`specs/007-android-undo/design.md` D7). That behaviour is consumed, not redesigned.

## D8 — No hardware-keyboard shortcuts, and the labeled buttons stay non-undoable

`06-ui-ux.md` §49 gives Discover Left / Right / Z. That is the browser's input model. Android's
accessible path to the same actions is TalkBack reaching the labeled controls that are already on the
card, and `06-ui-ux.md` §3.4's requirement — *"every swipe action must also exist as a visible
control"* — is satisfied today and stays satisfied. Adding `onKeyEvent` handling would add an
un-walkable surface (no hardware keyboard on the target device) to an enrichment item.

**The labeled buttons remain non-undoable**, exactly as 007 decided and for the same reason: Undo
exists to recover a mis-*trigger*, and `contracts.md` §31 scopes it to swipe. A TalkBack double-tap on
"Save for later" is not a mis-trigger. `spec.md` §5 step 6 checks the absence of a toast on the button
path, because "helpfully" wiring the buttons to `undoable = true` is the single most predictable
unrequested change in this item.

**TalkBack must not lose an action**, and it does not: no existing control is replaced or moved, no
custom accessibility action is added, and the gesture is additive. The swipe cues are decorative
duplicates of the button labels and are hidden from the accessibility tree — the browser marks them
`aria-hidden` (`js/ui/discover.js:257-258`).

## D9 — The cues reuse the existing strings and are not carried by colour

`06-ui-ux.md` §10 requires direction, text, and icon, with colour only supporting. §41/§42 fix the copy:
`← Not interested` and `Save for later →`. `RoundTriageAction` already composes an arrow glyph with
`R.string.not_interested` / `R.string.save_for_later` (`ArticleCard.kt:258-291`), so the cue does the
same and **no new string resource is authored in this item**. The cue's visibility follows the locked
direction, as the browser's `data-swipe-state` does (`css/app.css:433-436`).

## D10 — No dependency is added, including for testing

The temptation here is Robolectric plus `compose-ui-test-junit4`, which would let the gesture be tested
on the JVM. It is declined:

- It reverses 002 slice 4's parked decision about instrumented tests through the side door, in an
  **enrichment** item, without an amendment.
- `waves/wave-b.md` states plainly that verification here is *"substantially emulator-only"* and sizes
  the item on that basis.
- D2 already moves the part worth testing out of Compose. What Robolectric would add is coverage of the
  plumbing, which is what the walkthrough is for.

If the implementer believes a dependency is required, that is a report to the supervisor, not a
decision to make (`AGENTS.md`).
