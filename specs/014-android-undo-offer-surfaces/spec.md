# 014 — The undo offer follows the reversible action, not the gesture

**Status:** draft (awaiting plan gate). **A forecast, not a design** — written against a tree that does not
exist yet (§6).\
**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/014-android-undo-offer-surfaces/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`. **`docs/v1/**` is amended for this item by Amendment 8, which is
written and committed before the item branch is cut** — the implementer does not edit `docs/v1/**`.\
**Authority:** `docs/v1/contracts.md` §§22/23/31, `docs/v1/05-personalization-state.md` §§36–41,
`docs/v1/06-ui-ux.md` §§43/45/70, `docs/v1/01-product.md` §14, `docs/v1/README.md` Amendment 6 and
**Amendment 8**\
**Wave:** D (`specs/waves/wave-d.md`), after 015 merges · **Branch:** `feat/014-android-undo-offer-surfaces` → `main`\
**Cut from:** merged `main`, after item 015's PR merges and its hosted CI is green on the merge commit.

> **Re-scoped 2026-08-31** on the reversibility line (was: "The Discover card's buttons in the Undo
> window"). Every citation to 014 — including `specs/013-android-undo-gesture-reset/spec.md` §1.8 — still
> resolves.
>
> **Re-premised 2026-08-31, at this design pass.** `waves/wave-d.md` and `specs/backlog.md` both said this
> item needs no `docs/v1/**` amendment. **That was wrong**, and §1.2 is the finding. The owner confirmed the
> item proceeds with an amendment on 2026-08-31.

---

## 1. Why this item exists

### 1.1 What the reader sees, and it is not a failing button

Tap **Save for later** on the Discover card. The save commits — the record is written, Read Later
increments — and no undo offer appears. Screencaps at 0.2 s, 1.0 s and 2.0 s show no toast, while the swipe
path raises one at 0.35 s. Reproduces identically on the 006 and pre-006 builds
(`specs/006-android-deck-diversity/evidence.md` §5 step 5).

**The button is not failing. It is succeeding silently.** `undoable = true` is passed at exactly one call
site in the whole app — `IntentionalReadingApp.kt:290`, inside `onSwipeCommit` — and every other call takes
the `undoable = false` default. `SAVE` and `DISMISS` are already in
`ArticleStateMachine.reversibleActions` (`domain/state/ArticleStateMachine.kt:254`), so the reversal exists
and is simply never requested.

**This item inherits no cause from 013.** 013's mechanism is an ancestor scroll consuming the pointer DOWN
before `ArticleCard`'s gesture handler adopts it. A `Button` is not a `pointerInput` gesture and does not
call `awaitFirstDown`. Starting from 013's diagnosis cost 013 two of its four passes; the evidence here says
there is no failure to diagnose.

### 1.2 The finding this design pass produced: the current behaviour is specified, twice, deliberately

The `undoable = false` default on the buttons is not an oversight. It is a decision the corpus records in
five places and that two shipped items made on purpose:

| Source | What it says |
|---|---|
| `contracts.md` §31 | "Only the most recent eligible **swipe** action must be retained." |
| `05-personalization-state.md` §36 | "V1 Undo supports **only the most recent successful Discover swipe** action." |
| `06-ui-ux.md` §70 | "Undo is offered **only** for the most recent eligible **Discover swipe**." |
| `01-product.md` §14 | "V1 supports undo for the **most recent swipe action**." |
| `09-testing-acceptance.md` §50 | "Only the most recent eligible save / dismiss **swipe** must be undoable." |

And the rationale, stated at length by item 007 and re-affirmed by item 008:

> Undo exists to recover a *mis-trigger*. A swipe or an arrow key can be fired by accident; a press on a
> button labeled "Save for later" cannot, in the sense that matters. — `specs/007-android-undo/spec.md`
> §1.1, and `specs/008-android-swipe-gestures/design.md` D8, which adds that wiring the buttons to
> `undoable = true` is "the single most predictable unrequested change in this item" and put a walkthrough
> step in place to catch it.

The browser does the same thing at `js/ui/discover.js:211` and `:246-248`. Item 013 cited §31 and §70 as its
reason for leaving the buttons alone (`013/spec.md` §3).

**So this item does not fix a defect. It reverses a design decision.** The owner decided on 2026-08-31 to
reverse it, because the alternative is incoherent once item 016 lands: Read Later's *Remove* would offer a
reversal while Discover's *Save for later* — the same reversible act, one screen away — would not.
`waves/wave-d.md`'s stated end state is that Undo means the same thing everywhere in the app, and it cannot
mean the same thing everywhere while it depends on which finger movement started it.

### 1.3 Amendment 8, and its reach

**Amendment 8, `Undo Scope: Reversible Actions and Offer Surfaces`,** is written before this item's branch
is cut. It covers both dimensions at once — which *surfaces* raise the offer (this item) and which *actions*
are reversible (item 016) — because §31, §36 and §70 each say both things in one sentence and editing those
sentences twice would leave `docs/v1/**` self-contradictory in between.

It is **permissive, not binding on the browser** (owner decision, 2026-08-31): an eligible action *may*
raise the offer from any surface, and the browser's existing swipe-and-keyboard scope remains compliant. So
this item stays Android-only, touches no `js/**`, and fires `android.yml` alone.

### 1.4 What must not be lost with the decision

The old rule protected something real, and two parts of it survive as constraints on this item:

- **`OPEN` never becomes reversible.** Reading is not a triage decision and reversing it would mean
  un-reading an article the reader has read. It is not in `reversibleActions` and this item does not add it.
- **One offer at a time.** `contracts.md` §31's single-record rule is unchanged: any eligible action
  replaces the slot, exactly as a second swipe does today.

---

## 2. Story

As a **reader**, I want the way I take an action not to decide whether I can take it back, so that saving an
article with the labelled button is as reversible as saving it with a swipe.

---

## 3. Out of scope

- **`ArticleAction.MARK_READ`, anywhere — including Discover's own *Mark read* button.** It is not
  reversible today and making it so is item 016, along with `MARK_UNREAD` and `REMOVE`. Splitting it out is
  what keeps this item to one dimension.
- **Widening `reversibleActions`.** This item changes *who asks*, not *what is possible*
  (`ArticleStateMachine.kt:254` keeps `setOf(SAVE, DISMISS)`).
- **`ArticleAction.OPEN`.** §1.4.
- **New toast copy.** `SAVE` and `DISMISS` already have their strings (`undo_toast_saved`,
  `undo_toast_dismissed`) and `PendingUndoMessage` already has both cases. Item 016 authors the three new
  ones.
- **Keyboard shortcuts.** 008 D8 declined them for a target device with no hardware keyboard and that
  stands.
- **013's diagnosis, commits and instrumented guards.** §1.1. `ArticleCard.kt`, `SwipeGesture.kt` and the
  `androidTest` source set are all untouched.
- **015's identity guard.** This item preserves `expectDiscoverHead = true` on the four Discover lambdas and
  changes nothing about it (§6, assumption 2).
- **`docs/v1/**`.** Amendment 8 is committed before the branch is cut.
- **The browser.** §1.3.
- **The offer's 4.5 s lifetime, its affordance and `UndoToast.kt`.**

---

## 4. Scenarios

### Scenario: the Save for later button raises the offer

Given article A is on the Discover card\
When the reader presses **Save for later**\
Then the save is applied and persisted\
And an undo offer is raised naming the save\
And undoing it restores article A exactly and reverses the Save signal it applied

### Scenario: the Not interested button raises the offer

Given article A is on the Discover card\
When the reader presses **Not interested**\
Then the dismissal is applied and persisted\
And an undo offer is raised naming the dismissal\
And undoing it restores article A exactly and reverses the Dismiss signal it applied

### Scenario: a swipe behaves exactly as it does today

Given article A is on the Discover card\
When the reader swipes it in either direction\
Then the action is applied and persisted and the offer is raised\
And nothing about the swipe path's behaviour has changed

### Scenario: a button press and a swipe compete for the one slot

Given the reader has pressed **Save for later** on article A and the offer is showing\
When they then swipe article B\
Then the slot holds only article B's action\
And undoing reverses article B's action and leaves article A saved

### Scenario: Read article still offers nothing

Given article A is on the Discover card\
When the reader presses **Read article**\
Then the open is applied and no undo offer is raised\
And Undo remains unavailable

### Scenario: Mark read still offers nothing

Given article A on the Discover card has been opened, so **Mark read** is showing\
When the reader presses it\
Then the article is marked read and no undo offer is raised\
And this remains true until item 016 lands

### Scenario: an action whose write fails offers nothing

Given local persistence is failing\
When the reader presses **Save for later**\
Then the failure is announced\
And no undo offer is raised for an action that was not persisted

### Scenario: an idempotent press offers nothing

Given article A is already saved\
When a `SAVE` is committed against it again\
Then the transition is unchanged, no signal is applied, and no offer is raised

---

## 5. Verification

### 5.1 Gates

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

Delete `test-results` first; read the `BUILD SUCCESSFUL` line, not the counts; record the count at the
moment of the run.

### 5.2 Every scenario in §4 is assertable, and that is a design output

The naive implementation of this item — pass `undoable = true` at two more call sites in
`IntentionalReadingApp.kt` — puts the whole change inside a composable, where `testDebugUnitTest` has no
observer and the only available assertion is a vacuous one. That is exactly the trap wave C shipped and
review caught (`waves/wave-c-note.md` §2).

`design.md` D1 moves the decision instead: **undo eligibility becomes a property of the action, not an
argument supplied by the caller.** Every scenario in §4 is then assertable in `ArticleStateMachineUndoTest`
and `AppViewModelTest`, on the JVM, with no emulator and no instrumented test.

### 5.3 Walkthrough — required evidence

Driven over `adb` on the `Pixel_10`, inside one on-device `adb shell`. `uiautomator dump` cannot see the
Undo toast; `screencap` at 0.35 s shows it plainly. **Card action buttons sit roughly 150 px above their
text labels — read the clickable node's bounds, not the label's.**

1. **Save for later**, `screencap` at 0.35 s: the toast is showing. Tap **Undo**; pull the state document
   and confirm **by article id** that the record is gone and the source weight is back
   (`waves/wave-d.md`: prove the reversal by the article id, not by a count and not by a weight moving).
2. **Not interested**, same evidence.
3. **Save for later**, then let the offer expire without tapping Undo. The save stands and the record and
   weight are unchanged after expiry.
4. **Save for later** on article A, then swipe article B inside the window. Undo reverses B and leaves A
   saved.
5. **Read article**, and **Mark read** on an opened card: no toast at 0.35 s, 1.0 s or 2.0 s.
6. Each step carries the second question: *and is what the reader needs next actually on screen?*

### 5.4 Stop conditions

Stop and report rather than proceeding if: Amendment 8 is not on `main` when the branch is cut; item 015's
merged diff does not match §6's assumptions; removing the `undoable` parameter requires editing any test
whose subject is something other than undo eligibility; or `ArticleAction.OPEN` acquires an undo record at
any point.

---

## 6. This is a forecast — what it assumes about a tree that does not exist yet

`waves/wave-d.md` requires this to be stated as a forecast, because item 006 was designed against an
unmerged item's output and its plan failed at dispatch, weeks after plan-mode approval. Each assumption
below names the file and the fact, and `/feature-implementation` Step 0.4 must check every one against the
tree that exists rather than against this document.

1. **Amendment 8 is committed on `main`,** with `contracts.md` §31, `05-personalization-state.md` §36,
   `06-ui-ux.md` §70, `01-product.md` §14 and `09-testing-acceptance.md` §50 already reworded.
2. **Item 015 has added `expectDiscoverHead: Boolean = false` to `onArticleAction` and
   `launchArticleAction`, and passes `true` at `IntentionalReadingApp.kt`'s four Discover lambdas**
   (`onDismiss`, `onSave`, `onMarkRead`, `onSwipeCommit`). This item removes the `undoable` parameter from
   the same signature and **must preserve `expectDiscoverHead` at all four**.
3. **Item 015 has not touched `domain/state/ArticleStateMachine.kt`, `ui/components/ArticleCard.kt` or
   `ui/screens/discover/**`** (`specs/015-android-undo-swipe-attribution/design.md` D6). If 015's merged
   diff touches `ArticleStateMachine.kt`, this item's slice 1 must be re-planned before dispatch.
4. **`reversibleActions` is still `setOf(SAVE, DISMISS)`** — item 016 has not landed and must not land first.
5. **The undo-offer plumbing is unchanged:** `raiseUndoOffer` still maps only `SAVE` and `DISMISS`
   (`AppViewModel.kt:675-683`), `PendingUndoMessage` still has two cases (`AppUiState.kt:40-43`), and
   `UiStateMapper` still derives `undoAvailable` from the action (`ui/state/UiStateMapper.kt:70`).
6. **`AppViewModelTest` still contains `a labeled button press is still not undo-eligible` at or near
   `:978`, and `ArticleStateMachineUndoTest` still contains `a commit that is not marked undo-eligible
   offers nothing` at or near `:85`.** Both encode the rule Amendment 8 overturns; `slices.md` names what
   happens to each and why. If either has already moved, report the current shape before editing it.
