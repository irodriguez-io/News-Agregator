# 007 — slice plan

Sized **S → 2 ordered slices**. One item branch (`feat/007-android-undo`), one PR targeting `main`.
Each slice closes as a failing-first test commit plus an implementation commit, and must fit one fresh
implementer context window.

Scenario names refer to `spec.md` §4. Package root is `io.irodriguez.intentionalreading`; the Kotlin
source root is `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`, abbreviated `«pkg»`
below.

Fixed for every slice — do not re-decide these mid-implementation:

- **No trigger is wired in this item.** The `undoable` flag reaches `AppViewModel` and every existing
  caller passes `false`. Wiring the Save or Not interested buttons to pass `true` is a finding, not a
  bonus (`design.md` D1). Item 008 supplies the producer.
- **No Composable is added.** No toast, no banner, no button. The slot's state is exposed for 008 to
  render (`design.md` D4). `ui/screens/**` and `ui/components/**` are untouched in both slices.
- **The slot is in memory only.** Not in `LocalState`, not in `SavedStateHandle`, not on disk
  (`contracts.md` §31, `design.md` D3).
- **`preferences` is never touched by the undo path**, and `UndoRecord.preferenceReversal` is declared,
  always null, and never read (`design.md` D5).
- **Undo restores `previousRecord` wholesale.** No timestamp is recomputed, no `signalsApplied` flag is
  re-derived (`design.md` D2).
- **The held-article pin is not re-established on undo** (`design.md` D6).
- One lock. The existing `stateMutex` guards the slot; do not add a second.
- Nothing under `«pkg»/domain/` may import `android.*` or `androidx.*` — the constraint item 003
  established.
- **No new dependency.** Nothing is added to `android/gradle/libs.versions.toml`. If a slice appears to
  need one, that is a report to the supervisor, not a decision to make.
- Everything outside `android/` is untouched in both slices, except this item's own `specs/007-*/`
  documents.

## Slice 1: the domain inversion — `UndoRecord` and the reverse transition

Pure `domain/`. No ViewModel, no UI, no resources. This slice ends with an inversion that is exercised
entirely from JVM tests over plain data.

- **Scenarios:** "an undo-eligible save records what it replaced", "an undo-eligible dismiss of an
  opened article records the opened record", "a commit that is not marked undo-eligible offers
  nothing", "only save and dismiss are reversible", "undoing a save returns the article to having no
  record", "undoing a dismiss restores the exact record it replaced", "Undo is refused when the article
  it names is gone", "the undo record carries a reversal field that is not yet used".
- **Files:** `«pkg»/domain/state/UndoRecord.kt` (new), `«pkg»/domain/state/ArticleStateMachine.kt`
  (an `undoable` parameter on `transition`, an undo record on `ArticleTransition.Applied`, and a
  reverse entry point), and `android/app/src/test/kotlin/**/domain/state/ArticleStateMachineTest.kt`
  plus a new undo test alongside it.
- **Must not touch:** `«pkg»/ui/**`, `«pkg»/data/**`, `«pkg»/di/**`, `res/**`, any Gradle file.
- **Reuse:** `ArticleTransition`'s existing `Applied`/`Unchanged`/`Invalid` shape and
  `ArticleActionResult` conventions — model the two refusals inside that family, not in a parallel one
  (`design.md` D7). `allowedFrom` and `isIdempotentNoOp` stay exactly as they are; the undo record is
  produced from the transition's own outcome, never by re-deriving eligibility.
- **Definition of done:** both gates green; a test proving the restored record is field-for-field equal
  to the pre-action record including `firstSeenAt`, `openedAt`, and every `signalsApplied` flag; a test
  proving `previousRecord == null` deletes the key rather than writing a record, and that
  `LocalStateValidator` accepts the result; a test proving `OPEN`, `MARK_READ`, `MARK_UNREAD`, and
  `REMOVE` produce no undo record even when marked undo-eligible; a test proving an idempotent no-op
  produces no undo record; no assertion from the existing suite deleted.
- **Status:** done

## Slice 2: the slot — `AppViewModel` availability, refusals, and strings

- **Scenarios:** "the slot holds one action, and the newest wins", "Undo is refused when there is
  nothing to undo", "a failed write leaves both the state and the offer intact", "resetting local data
  withdraws the offer", "an undo offer does not survive the process", "an undone article returns to the
  head of Discover on its own".
- **Files:** `«pkg»/ui/AppViewModel.kt` (the private slot, an `undoable` parameter threaded through
  `launchArticleAction`/`onArticleAction`/`persistArticleTransition`, a `performUndo` entry point, and
  the clear on reset), `«pkg»/ui/AppUiState.kt` and `«pkg»/ui/state/UiStateMapper.kt` (undo
  availability and the pending message, for 008 to render),
  `android/app/src/main/res/values/strings.xml` (the five strings in `design.md` D4, verbatim), and
  `android/app/src/test/kotlin/**/ui/AppViewModelTest.kt` and `ui/state/UiStateMapperTest.kt`.
- **Must not touch:** `«pkg»/domain/**` beyond calling slice 1's entry points, `«pkg»/data/**`,
  `«pkg»/ui/screens/**`, `«pkg»/ui/components/**`, `«pkg»/ui/IntentionalReadingApp.kt`,
  `AndroidManifest.xml`, any Gradle file.
- **Reuse:** `stateMutex` for the slot (`design.md` D3) — no second lock. `recordPersistenceFailure`
  and the existing `AppAnnouncementKind.PERSISTENCE_FAILED` for a failed undo write — no new
  announcement kind (`design.md` D7). `FakeLocalStateStore`'s existing write-failure mode drives the
  failed-write scenario; extend it only if it genuinely cannot express the case.
- **Definition of done:** both gates green; a test proving a second undo-eligible commit replaces the
  slot rather than stacking, and that only the newest is reversed; a test proving a failed write leaves
  `localState` unchanged *and* the slot still populated; a test proving reset empties the slot; a test
  proving the article returns to the head of the deck after undo **while `heldArticleId` is still
  null** (`design.md` D6); a test proving `preferences` is unchanged across an undo; a construction-time
  test proving a fresh `AppViewModel` reports Undo unavailable regardless of what is on disk; grep
  evidence in the PR that no caller in the tree passes `undoable = true`.
- **Status:** pending
