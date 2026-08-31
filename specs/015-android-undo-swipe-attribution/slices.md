# 015 — slice plan

**Size: S — one slice.** One objective, one hub file plus four lambda arguments in a second, one test file.
It fits one fresh implementer context window.

Read before starting: `spec.md` §1.2 (the mechanism), §1.4 (**score by which article moved**), `design.md`
D1 and D2 (the shape and why it is opt-in).

---

## Slice 1: refuse a Discover-card action whose article is not the published head

- **Scenarios:** all five in `spec.md` §4.
- **Files:**
  - `android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/AppViewModel.kt` — add
    `expectDiscoverHead: Boolean = false` to `onArticleAction` and `launchArticleAction`; implement the
    guard inside the existing `stateMutex.withLock`, **before** `ArticleStateMachine.transition`, exactly
    as `design.md` D1 sketches.
  - `android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/IntentionalReadingApp.kt` — pass
    `expectDiscoverHead = true` in the four Discover lambdas at `:276` (`onDismiss`), `:280` (`onSave`),
    `:283` (`onMarkRead`) and `:286` (`onSwipeCommit`). Nothing else in this file changes.
  - `android/app/src/test/kotlin/io/irodriguez/intentionalreading/ui/AppViewModelTest.kt` — the new cases.
- **Must not be touched:** `ui/components/ArticleCard.kt`, `ui/gesture/SwipeGesture.kt`,
  `ui/screens/discover/**`, `domain/**`, `res/**`, `docs/v1/**`, and everything outside `android/`.
- **Failing-first commit:** the new `AppViewModelTest` cases, RED against the current tree. The
  head-mismatch case must fail by *committing the action* — assert the absence of a record and of a weight
  movement **for the displaced article's id**, so that a green-by-accident (`persisted == false` for some
  other reason) cannot be mistaken for the fix.
- **Definition of done:**
  - After a dismiss of A, an Undo, and an action against B, `localState.articles` has no entry for B, the
    preference maps hold no entry or an unchanged entry for B's source and each of B's topic ids, and the
    published Discover head is still A. **Asserted by article id and by source/topic id, never by a
    count** (`spec.md` §1.4).
  - An action against the current head still applies, persists and raises the offer.
  - An action on Read Later against a non-head article still applies and persists — the guard does not
    reach it.
  - A refused action raises no undo offer and leaves any pending offer untouched.
  - `./gradlew :app:testDebugUnitTest --rerun-tasks` and `:app:assembleDebug` both green, `test-results`
    deleted first, count recorded at the moment of the run.
  - **No existing test edited.** If one fails, stop and report — `spec.md` §5.4.
- **Not in this slice, and not assertable at this layer:** that the *defect* is gone. The unit tests prove
  the invariant; `spec.md` §5.3's walkthrough proves the defect. Stated here rather than closed with an
  assertion that cannot see it.
- **Status:** pending

---

## Assumptions, each checkable at dispatch

Per `waves/wave-d.md`'s rule 2. `/feature-implementation` Step 0.4 must confirm all four against the tree
that exists, not against this document.

1. **`AppViewModel.onArticleAction` still takes `(article, action, undoable)` and is still `suspend` and
   public**, and `launchArticleAction` still wraps it. If item 014 has landed first — it must not have —
   `undoable` will be gone and the guard's placement is unchanged but the signature is not.
2. **`IntentionalReadingApp.kt`'s Discover lambdas are still at `:276-294`** in the shape quoted in
   `spec.md` §1.2 and `design.md` D6.
3. **`DiscoverUiState.Card` still carries `article: Article`** (`ui/screens/discover/DiscoverUiState.kt:41`),
   so the published head is readable as `(_uiState.value.discover as? DiscoverUiState.Card)?.article?.id`.
4. **`_uiState` is still published under `stateMutex` by `publish()`**, so reading it inside the lock is
   consistent with `localState`.

## On existing assertions

**No existing assertion is frozen and none is expected to change.** The guard is opt-in and defaults to
off, so every current call in `AppViewModelTest` keeps its present behaviour. That is a prediction, not a
rule: if the implementer finds an existing case that must change, `spec.md` §5.4 says stop and report the
case name and the reason, because it means the guard is reaching further than this design intends.
