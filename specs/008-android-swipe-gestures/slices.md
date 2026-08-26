# 008 — slice plan

Sized **M → 3 ordered slices**. One item branch (`feat/008-android-swipe-gestures`), one PR targeting
`main`. Each slice closes as a failing-first test commit plus an implementation commit, and must fit one
fresh implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

**Every Gradle invocation needs both of these exported first** — `java` is not on this machine's
`PATH`, and a worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android && ./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

Baseline on `main` at `75f2821`: **163 tests**, green.

Fixed for every slice — do not re-decide these mid-implementation:

- **The threshold is `90.dp`.** Not 150 dp, not a fraction of screen width (`design.md` D1). The
  reasoning is written down; re-deriving it is not part of the job.
- **The gesture arithmetic contains no `android.*` import and no Compose type** (`design.md` D2). If a
  decision cannot be expressed there, that is a design question to raise, not a reason to move logic
  into the Composable.
- **Pointer changes are consumed only while the intent is horizontal, and only in the default pass**
  (`design.md` D3). Consuming earlier breaks the parent scroll; using `PointerEventPass.Initial` breaks
  the card's own buttons.
- **The labeled buttons stay non-undoable.** Wiring Not interested or Save for later to
  `undoable = true` is a finding, not a bonus (`design.md` D8) — the same instruction 007 carried.
- **A failed write is never visually finalized.** The card returns to rest and no offer is raised
  (`06-ui-ux.md` §43, `design.md` D4).
- **The offer is not the slot.** A toast timing out withdraws the offer and leaves Undo available
  (`design.md` D6). Clearing the slot on timeout is a defect.
- **007's engine is consumed, not edited.** `«pkg»/domain/state/ArticleStateMachine.kt` and
  `UndoRecord.kt` are read-only in this item. A diff touching them is a report to the supervisor.
- **`preferences` is never read or written by any path in this item.**
- One lock. The existing `stateMutex` guards everything in `AppViewModel`; do not add a second.
- **No new dependency and no new string resource.** `android/gradle/libs.versions.toml` is untouched;
  the five undo strings and the two triage labels already exist (`design.md` D9, D10). If a slice
  appears to need either, that is a report to the supervisor, not a decision to make.
- **Item 009 is running concurrently on its own branch.** `«pkg»/ui/screens/settings/**`,
  `«pkg»/data/**`, and `MainActivity.kt` belong to it and are not touched here.
- Everything outside `android/` is untouched in all three slices, except this item's own `specs/008-*/`
  documents.

---

## Slice 1: the gesture as arithmetic

Pure Kotlin. No Compose, no ViewModel, no resources. This slice ends with every decision `spec.md` §4.1
describes exercised from plain JUnit over synthetic pointer sequences.

- **Scenarios:** all ten in `spec.md` §4.1 — "a touch that has barely moved locks no intent", "a
  mostly-vertical drag never becomes a swipe", "a decisively horizontal drag locks horizontal", "intent
  is locked once and does not change mid-gesture", "rotation follows travel and is clamped", "releasing
  short of the threshold changes nothing", "releasing past the threshold emits the direction's action",
  "a cancelled gesture restores whatever its travel was", "a second gesture is refused while a commit is
  in flight", "reduced motion removes the rotation and the exit travel".
- **Files:** `«pkg»/ui/gesture/SwipeGesture.kt` (new — the constants and the state holder) and
  `android/app/src/test/kotlin/**/ui/gesture/SwipeGestureTest.kt` (new).
- **Must not touch:** `«pkg»/domain/**`, `«pkg»/data/**`, `«pkg»/di/**`, `«pkg»/ui/**` outside the new
  `gesture/` package, `res/**`, any Gradle file.
- **Reuse:** the constants come from `js/ui/swipe.js` and are cited in `design.md` D2 with their source
  lines — port them, do not tune them. Follow the existing `domain/state/` test style: plain JUnit,
  no runtime, one behaviour per test.
- **Definition of done:**
  - Both gates green.
  - A test proving the 1.15 bias: a drag of `x = 10, y = 9` locks **horizontal**, and `x = 10, y = 9.5`
    locks **vertical**. The asymmetry is the point; a test that only checks `abs(x) > abs(y)` does not
    cover this slice.
  - A test proving the lock survives a later reversal of dominance within the same gesture.
  - A test proving rotation is clamped at both ±4.5°, and that it is `travel / 34` below the clamp.
  - A test proving the emitted action is `DISMISS` left of −90 dp-equivalent and `SAVE` right of +90,
    and nothing at exactly the threshold boundary the browser excludes.
  - A test proving `cancel()` past the threshold emits nothing.
  - A test proving reduced motion zeroes rotation and exit distance **without** changing which action is
    emitted.
  - No `android.` or `androidx.` import in the new main-source file — assert it by inspection in the PR.
  - No assertion from the existing suite deleted.
- **Status:** pending

## Slice 2: the offer — undo eligibility, its identity, and the two announcements

Still no Composable. This slice makes the swipe path's *consequences* observable to the JVM gate before
anything renders them.

- **Scenarios:** "a committed swipe is undo-eligible", "a labeled button press is still not
  undo-eligible", "each committed swipe raises its own offer", "the offer's message names the action",
  "the offer expires without withdrawing Undo", "Undo from the offer restores the article and
  announces", "a refused Undo announces its failure and keeps the offer", "resetting local data
  withdraws the offer".
- **Files:** `«pkg»/ui/AppViewModel.kt` (raise the offer in `persistArticleTransition` when an undo
  record was stored, `acknowledgeUndoOffer(id)`, the two announcements around `performUndo`, the
  withdrawal on reset), `«pkg»/ui/AppUiState.kt` (`PendingUndoOffer`, replacing the bare
  `pendingUndoMessage`), `«pkg»/ui/state/UiStateMapper.kt`, `«pkg»/ui/IntentionalReadingApp.kt`
  (**only** the two new branches in the announcement `when`), and
  `android/app/src/test/kotlin/**/ui/AppViewModelTest.kt` and `ui/state/UiStateMapperTest.kt`.
- **Must not touch:** `«pkg»/domain/**`, `«pkg»/data/**`, `«pkg»/ui/gesture/**`, `«pkg»/ui/screens/**`,
  `«pkg»/ui/components/**`, `res/**`, any Gradle file.
- **Reuse:** `AppAnnouncement`'s `nextAnnouncementId` counter pattern for the offer id (`design.md` D6)
  — the same shape, not a parallel mechanism. `recordPersistenceFailure` and the existing
  `PERSISTENCE_FAILED` for a failed undo write (`design.md` D7); the two new kinds are for undo
  *completing* and *being refused*, nothing else. `FakeLocalStateStore`'s existing write-failure mode
  drives the failure scenarios.
- **Definition of done:**
  - Both gates green.
  - A test proving two successive undo-eligible commits raise offers with **different ids**, and that
    the slot holds only the newer action.
  - A test proving `onArticleAction(..., undoable = false)` raises no offer for `SAVE` or `DISMISS`.
  - A test proving `acknowledgeUndoOffer` withdraws the offer while `undoAvailable` stays **true** —
    the browser-parity claim in `design.md` D6, and the one most likely to be "tidied" away.
  - A test proving a successful undo announces `UNDO_COMPLETED` and withdraws both the offer and the
    slot.
  - A test proving a stale undo announces `UNDO_FAILED`, writes nothing, and leaves the offer standing.
  - A test proving a failed write during a swipe commit raises **no** offer.
  - A test proving reset withdraws the offer.
  - A test proving `preferences` is byte-identical across a commit-and-undo round trip.
  - No assertion from the existing suite deleted.
- **Status:** pending

## Slice 3: the surface — the drag, the cues, the exit, and the toast

The Compose slice. **This slice carries no new JVM test and that is stated in advance, not discovered
at review:** its entire surface is Composable, and instrumented tests are parked from CI by decision
(`specs/backlog.md` §Parked; `spec.md` §1.2). Slices 1 and 2 exist so that everything decidable was
already decided and proven before this one starts. Its evidence is `:app:assembleDebug`, the unchanged
test count, and the walkthrough in `spec.md` §5.2.

- **Scenarios:** `spec.md` §5.2 steps 1–9. No §4 scenario is first proven here.
- **Files:** `«pkg»/ui/components/ArticleCard.kt` (the gesture modifier on the card `Surface`, the
  translation and rotation, the two cues, the exit animation),
  `«pkg»/ui/screens/discover/DiscoverScreen.kt` (threading the swipe commit callback),
  `«pkg»/ui/components/UndoToast.kt` (new — message, `Undo` action, 4500 ms, polite live region),
  `«pkg»/ui/IntentionalReadingApp.kt` (hosting the toast, passing the reduced-motion capability, wiring
  `performUndo` and `acknowledgeUndoOffer`), `«pkg»/di/AppContainer.kt` (the real reduced-motion
  reader).
- **Must not touch:** `«pkg»/domain/**`, `«pkg»/data/**`, `«pkg»/ui/gesture/SwipeGesture.kt` (frozen by
  slice 1), `«pkg»/ui/AppViewModel.kt` beyond calling slice 2's entry points, `res/values/strings.xml`,
  any Gradle file, `MainActivity.kt`.
- **Reuse:** `LiveStatusMessage`'s placement and live-region semantics for the toast, and
  `IntentionalReadingApp.kt`'s existing announcement `LaunchedEffect` as the model for the 4500 ms timer
  — the same shape at a different duration. `RoundTriageAction`'s arrow-plus-label composition for the
  cues (`design.md` D9). 010's injected-capability pattern for the reduced-motion reader
  (`design.md` D5): a `() -> Boolean` with a `{ false }` default, real implementation in
  `AppContainer`, so no `android.*` import reaches `AppViewModel` or the gesture object.
- **Fixed decisions — do not re-open mid-implementation:**
  - The gesture holder is `remember(article.id)`-scoped and reset on every article change
    (`design.md` D4). Without it, the next card renders already thrown off-screen.
  - The commit calls `launchArticleAction(article, action, undoable = true)` and branches on
    `result.persisted`: raise the offer, or restore the card. No new announcement on the failure path —
    `persistArticleTransition` already speaks.
  - The exit is 280 ms, `cubic-bezier(0.2, 0.8, 0.2, 1)` (`06-ui-ux.md` §44). Not a spring, not a
    bounce, not a fling.
  - The cues are hidden from the accessibility tree; the labeled buttons are not touched, moved, or
    relabeled.
  - The toast's timeout calls `acknowledgeUndoOffer(id)` and nothing else.
- **Definition of done:**
  - `:app:assembleDebug` green and `:app:testDebugUnitTest` still green at slice 2's count, with **no
    test deleted or weakened** to accommodate the Composable changes.
  - `git diff` shows `libs.versions.toml`, `strings.xml`, and `«pkg»/domain/**` untouched across the
    whole item.
  - `git grep 'undoable = true'` returns exactly the swipe commit call site and nothing else.
  - A screenshot from the emulator of a mid-drag card showing a cue, and of a committed swipe's toast,
    attached to the slice report.
- **Deferred to wave B's batched walkthrough:** all of `spec.md` §5.2, run against merged `main`. The
  slice is *done* when the build is green and the two screenshots exist; the **item** is not shippable
  until the walkthrough is recorded in `evidence.md`.
- **Status:** pending
