# 016 — Widen what is reversible

**Status:** draft (awaiting plan gate). **A forecast, not a design** — written against a tree that does not
exist yet (§7).\
**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/016-android-reversible-actions/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`. **`docs/v1/**` is amended for this item by Amendment 8, which is
written and committed before item 014's branch is cut** — the implementer does not edit `docs/v1/**`.\
**Authority:** `docs/v1/contracts.md` §§20/21/22/**23**/24/31, `docs/v1/05-personalization-state.md`
§§36–41, `docs/v1/06-ui-ux.md` §§45/70, `docs/v1/README.md` Amendment 6 and **Amendment 8**\
**Wave:** D (`specs/waves/wave-d.md`), last · **Branch:** `feat/016-android-reversible-actions` → `main`\
**Cut from:** merged `main`, after item 014's PR merges and its hosted CI is green on the merge commit.

> **Re-scoped 2026-08-31** on the reversibility line (was: "Undo in Read Later and History"). Every citation
> to 016 still resolves. `waves/wave-d.md` has the table.

---

## 1. Why this item exists

### 1.1 What the reader sees, confirmed by hand

The owner reproduced all three on 2026-08-31 from the running app:

- **Read Later — *Mark read*** raises no undo offer.
- **Read Later — *Remove*** raises no undo offer.
- **History — *Mark unread*** raises no undo offer.

Plus **Discover's own *Mark read***, moved here from item 014 by the re-cut, on the same cause.

### 1.2 Two independent gates, and this item must close both

1. `domain/state/ArticleStateMachine.kt:254` — `reversibleActions = setOf(SAVE, DISMISS)`. `transition`
   builds no `UndoRecord` for the other three, and `reverse` returns `UNDO_UNAVAILABLE`.
2. The offer is only raised for an action that produced a record. **Widening `reversibleActions` alone
   changes nothing on screen.**

Item 014 has already closed the second gate in the general case — after it, the offer follows any action
that carries a record, from any surface (`specs/014-android-undo-offer-surfaces/design.md` D1). So what is
left here is the first gate, the arithmetic behind it, and the toast copy the three new actions need.

### 1.3 There is no per-pane affordance to build

`UndoToast` is already hosted globally in `IntentionalReadingApp` (`:313-331`), outside the destination
`when` and gated only on `!settingsOpen`. It renders on Read Later and History today. **016's original title
overstated its UI work: this item raises an offer, it does not build a surface.**

### 1.4 The amendment is the long pole, and it is not a formality

`contracts.md` §23 ties Undo to the `signalsApplied` reversal guard item 005 introduced. Widening what is
reversible changes which preference deltas can be reversed and when. That coupling is why item 007 led the
whole programme. §2 below is the arithmetic, settled on paper at design time as `waves/wave-d.md` required,
and it is carried into **Amendment 8** — the same amendment item 014 needs, because `contracts.md` §31,
`05-personalization-state.md` §36 and `06-ui-ux.md` §70 each state both the surface rule and the action rule
in one sentence.

---

## 2. The reversal arithmetic — settled

Derived from the frozen contracts and from the code as it stands. Every row was checked against
`ArticleStateMachine.transition` and `PreferenceLearning`, not inferred from the action's name.

| Action | Forward preference effect | Previous record can be | Undo restores | Undo's preference effect |
|---|---|---|---|---|
| `SAVE` | Save for Later, +0.45 / +0.30, once (`contracts.md` §21/§22) | any of `UNSEEN`, `OPENED`, `SAVED`, or absent | `previousRecord` exactly | reverse Save, **iff the forward transition applied it** — unchanged from today |
| `DISMISS` | Not Interested, −0.35 / −0.20, once | as above for `DISMISSED` | `previousRecord` exactly | reverse Not Interested, iff applied — unchanged from today |
| `REMOVE` | **none** | always `SAVED` | `previousRecord` exactly, i.e. back to `SAVED` | **none** |
| `MARK_READ` | Mark Read, +0.25 / +0.20, once | `UNSEEN`, `OPENED`, `SAVED`, `READ` | `previousRecord` exactly | reverse Mark Read, iff the forward transition applied it |
| `MARK_UNREAD` | **reverses** Mark Read, iff `signalsApplied.read` (`contracts.md` §23) | always `READ` | `previousRecord` exactly, i.e. back to `READ` with `signalsApplied.read = true` | **re-applies** Mark Read, iff the forward transition reversed it |

### 2.1 `REMOVE` has no preference arithmetic at all, and that is specified

`contracts.md` §24: removing an article from Read Later sets `status → dismissed` and **does not** apply the
negative Not Interested signal, because removing an item from a backlog does not mean the topic or source is
unwanted. The code agrees — `preferenceEvents` has no `REMOVE` entry
(`ArticleStateMachine.kt:256-261`) — so nothing is applied forward and nothing needs reversing.

And `allowedFrom[REMOVE] = setOf(SAVED)` (`:251`), so **the previous record is always `SAVED`.** That
answers `waves/wave-d.md`'s question of what `REMOVE` reverses *to*: restoring `previousRecord` exactly is
sufficient and correct, and no generic "unremoved" state is needed.

### 2.2 `MARK_UNREAD` is the double negative, and the existing record shape cannot express it

`transition`'s `MARK_UNREAD` branch (`ArticleStateMachine.kt:121-127`) reverses the Read signal and clears
`signalsApplied.read`, and it deliberately leaves `preferenceSignalApplied` **false** — nothing was applied,
something was reversed. `UndoRecord.preferenceReversal` can only say *"reverse event E"*
(`domain/state/UndoRecord.kt:19`).

So widening `reversibleActions` alone would make undo of `MARK_UNREAD` restore a record claiming
`signalsApplied.read = true` while the Read weight it claims stays subtracted. **The record and the weights
would disagree, silently, and nothing in the app would notice.** That is exactly the place
`waves/wave-d.md` predicted the arithmetic would be got wrong.

The fix is a second, opposite direction on the undo record — see `design.md` D1. The owner chose this on
2026-08-31 over the two alternatives (restore the record with `read = false`, which contradicts
`05-personalization-state.md` §38's *"restore the exact pre-action state"*; or leave `MARK_UNREAD`
irreversible, which leaves one of the three reported defects unfixed).

**Undo of `MARK_UNREAD` is the only place in V1 where reversing an action re-applies a signal.** It is
called out in Amendment 8 for that reason.

### 2.3 A pre-existing asymmetry, recorded and not fixed here

`PreferenceLearning.reverse` subtracts the delta and clamps to `[-5.0, +5.0]`
(`domain/state/PreferenceLearning.kt:67-86`), and it prunes an entry that reaches zero weight and zero
interactions. So apply-then-reverse is not lossless at the clamp boundary, and neither is reverse-then-apply.

This is **already true of Save and Dismiss undo** as shipped by item 007 and is not introduced by this item.
It is recorded so that a reviewer meeting it in §2's new rows does not read it as a new defect. Fixing it
would be a change to `contracts.md` §21's clamp, which is not this item's scope.

### 2.4 One record, and it survives a destination change

`contracts.md` §31's single-record rule is unchanged: any eligible action replaces the slot. Because the
record names the article (`UndoRecord.articleId`), an offer raised on Read Later stays valid if the reader
switches to History inside the 4.5 s window — that is the answer to `waves/wave-d.md`'s third question, and
§4 carries the scenario that proves the reader cannot double-apply or double-reverse a delta by moving.

---

## 3. Out of scope

- **`ArticleAction.OPEN`.** Reading is not a triage decision and un-reading an article the reader has read
  is not a reversal anyone asked for. It stays out of `reversibleActions`.
- **Any change to the deltas in `contracts.md` §21**, to the clamp, or to `PreferenceLearning`'s apply and
  reverse arithmetic itself. §2.3.
- **Any change to `allowedFrom`, to `isIdempotentNoOp`, or to the forward transitions.** This item changes
  what can be *taken back*, not what can be *done*.
- **A per-pane undo affordance.** §1.3 — there is nothing to build.
- **The offer's 4.5 s lifetime**, `UndoToast.kt`, and the announcement copy (`undo_completed`,
  `undo_failed`).
- **Persisting the undo record.** `contracts.md` §31 and `05-personalization-state.md` §37 keep it in memory
  only, and import and reset keep clearing it (`AppViewModel.kt:282-283`, `:330-331`).
- **The browser.** Amendment 8 is permissive and does not oblige `js/**` to change (owner decision,
  2026-08-31).
- **`docs/v1/**`.** Amendment 8 is committed before item 014's branch is cut, well ahead of this item.
- **Item 015's identity guard and item 012's layout.** Neither is touched.

---

## 4. Scenarios

### Scenario: Remove from Read Later is reversible and moves no weight

Given article A is saved and showing in Read Later\
When the reader presses **Remove**\
Then A's status becomes dismissed and an undo offer is raised\
And no preference weight or interaction count has changed for A's source or topics\
When the reader taps **Undo**\
Then A is saved again, exactly as it was\
And still no preference weight or interaction count has changed

### Scenario: Mark read from Read Later is reversible and its signal is reversed

Given article A is saved in Read Later and its Read signal has never been applied\
When the reader presses **Mark read**\
Then A becomes read, the Mark Read signal is applied, and an undo offer is raised\
When the reader taps **Undo**\
Then A is saved again with `signalsApplied.read` false\
And the Mark Read weight and interaction count for A's source and each of A's topics are back where they
were

### Scenario: Mark read that applies no signal reverses no weight

Given article A is saved in Read Later and its Read signal has already been applied\
And A has been marked unread and is saved again\
When the reader presses **Mark read** and then taps **Undo**\
Then A's record is restored exactly\
And the arithmetic matches what the forward transition actually did, not what the action is named

### Scenario: Mark unread from History is reversible and its signal is re-applied

Given article A is read in History and its Read signal is applied\
When the reader presses **Mark unread**\
Then A becomes saved, `signalsApplied.read` is false, the Mark Read signal has been reversed, and an undo
offer is raised\
When the reader taps **Undo**\
Then A is read again with `signalsApplied.read` true\
And the Mark Read weight and interaction count for A's source and each of A's topics are back where they
were before the mark-unread

### Scenario: Mark unread of an article carrying no Read signal reverses nothing either way

Given article A is read but `signalsApplied.read` is false\
When the reader presses **Mark unread** and then taps **Undo**\
Then A's record is restored exactly\
And no weight moves in either direction at any point

### Scenario: Discover's Mark read is reversible

Given article A on the Discover card has been opened, so **Mark read** is showing\
When the reader presses it\
Then an undo offer is raised\
And undoing it restores A's opened record exactly and reverses only the Mark Read signal, leaving the First
Open signal applied

### Scenario: the offer survives the reader changing destination

Given the reader marks article A read in Read Later and an offer is raised\
When they switch to History inside the window and tap **Undo**\
Then A is restored and its Mark Read signal is reversed

### Scenario: acting in a second pane does not double-apply or double-reverse

Given the reader marks article A read in Read Later\
When they switch to History and mark article B unread\
And then tap **Undo**\
Then B is read again with its Read signal re-applied\
And article A is still read, with its Mark Read signal still applied and its weights unchanged\
And no weight for A has moved twice

### Scenario: each new action's toast names what it did

Given each of Remove, Mark read and Mark unread has been performed\
When the offer appears\
Then its message names that action, distinctly from Saved and from Not interested

### Scenario: an action whose write fails offers nothing

Given local persistence is failing\
When the reader presses **Remove**\
Then the failure is announced and no undo offer is raised

### Scenario: Undo remains unavailable for Open

Given the reader opens an article from any destination\
Then no undo offer is raised\
And Undo remains unavailable

---

## 5. New copy

`06-ui-ux.md` §45 labels its two toast strings *"Examples"*, so they are illustrative and three more need no
amendment. Proposed, for the owner's judgment at the wave walkthrough:

| Action | String resource | Text |
|---|---|---|
| `MARK_READ` | `undo_toast_marked_read` | Marked as read |
| `MARK_UNREAD` | `undo_toast_marked_unread` | Returned to Read Later |
| `REMOVE` | `undo_toast_removed` | Removed from Read Later |

`MARK_UNREAD` moves the article to `SAVED` (`contracts.md` §23), so *Returned to Read Later* is what
actually happened rather than a restatement of the button label. The toast composes "— Undo" itself
(`undo_action`), as it does for the existing two.

---

## 6. Verification

### 6.1 Gates

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

### 6.2 Assert the arithmetic against the arithmetic

Wave C's transferable lesson: **assert a comparator against the comparator, not through the algorithm that
consumes it** (`waves/wave-c-note.md` §2). The same rule applies to §2's table.

- Every row of §2 is asserted in `ArticleStateMachineUndoTest` against `transition` and `reverse` directly,
  by comparing the `LocalState.Preferences` map before and after — **keyed by source id and topic id**, not
  by an aggregate and not by "a weight moved".
- The re-application in §2.2 is asserted as a round trip: mark read, mark unread, undo, and the preferences
  map must equal the map from before the mark-unread, entry for entry.
- `AppViewModelTest` asserts the offer, the destination-crossing case and the persistence-failure case at
  the ViewModel layer. It does not re-assert the arithmetic through the ViewModel.

### 6.3 Walkthrough — required evidence

Driven over `adb` on the `Pixel_10`, inside one on-device `adb shell`. `uiautomator dump` cannot see the
Undo toast; `screencap` at 0.35 s shows it plainly. **Card and row action controls sit roughly 150 px above
their text labels — read the clickable node's bounds.**

**Run it against real accumulated history, not fresh state** — `waves/wave-d.md` owner checkpoint 3. Every
wave so far has found its most valuable defects this way and none of them by a gate.

1. **Read Later → Remove**, `screencap` at 0.35 s, then Undo. Pull the state document: the article is
   `saved` again **by id**, and the preference maps are byte-identical to before the remove.
2. **Read Later → Mark read**, then Undo. The article is `saved` again by id and the source weight is back
   to its pre-action value.
3. **History → Mark unread**, then Undo. The article is `read` again by id, `signalsApplied.read` is true,
   and the source weight is back **up** — this is the re-application and it is the step most likely to be
   wrong.
4. **Discover → Mark read** on an opened card, then Undo. The First Open signal survives.
5. **Cross-pane:** mark A read in Read Later, switch to History, mark B unread, tap Undo. Confirm from the
   state document that only B changed and that A's weights moved exactly once in total.
6. **Let an offer expire** in Read Later and in History. The action stands and nothing is reversed.
7. Read the three new toast strings on screen and judge the copy (§5).
8. Each step carries the second question: *and is what the reader needs next actually on screen?*

### 6.4 Owner checkpoints

- **Amendment 8's action half and §2's arithmetic**, before this item's branch is cut
  (`waves/wave-d.md` checkpoint 1). This is the wave's long pole.
- **The three toast strings** in §5, judged on screen at §6.3 step 7.
- **The §6.3 walkthrough against accumulated history** (`waves/wave-d.md` checkpoint 3).

### 6.5 Stop conditions

Stop and report rather than proceeding if: Amendment 8 is not on `main`; item 014's merged diff does not
match §7's assumptions; a round trip in §6.2 cannot be made to close entry-for-entry and the only way to make
it green is to weaken the assertion; or `ArticleAction.OPEN` acquires an undo record.

---

## 7. This is a forecast — what it assumes about a tree that does not exist yet

`/feature-implementation` Step 0.4 must check every one against the tree that exists.

1. **Amendment 8 is committed on `main`,** including §2's arithmetic and the `MARK_UNREAD` re-application.
2. **Item 014 has removed the `undoable` parameter** from `ArticleStateMachine.transition`,
   `AppViewModel.onArticleAction` and `AppViewModel.launchArticleAction`, and
   `persistArticleTransition` now raises the offer from `transition.undoRecord` alone
   (`specs/014-android-undo-offer-surfaces/design.md` D1). **If it did not — if 014 landed as two extra
   `undoable = true` arguments instead — then this item must also raise the offer at the Read Later,
   History and Discover *Mark read* call sites, and slice 3 grows accordingly.** Check this first; it is the
   single largest difference between this plan and the tree.
3. **`reversibleActions` is still `setOf(SAVE, DISMISS)` at `ArticleStateMachine.kt:254`,** and
   `preferenceReversals` still has entries for `SAVE` and `DISMISS` only (`:263-266`).
4. **`transition`'s `MARK_UNREAD` branch is unchanged** (`:121-127`): it reverses the Read signal when
   `current.signalsApplied.read` is true and leaves `preferenceSignalApplied` false.
5. **`UndoRecord` still has exactly four fields** (`domain/state/UndoRecord.kt:15-20`) and
   `PreferenceReversal` still has its four cases including `MARK_READ` (`:6-13`) — the enum case this item
   needs already exists.
6. **`PendingUndoMessage` still has two cases** (`ui/AppUiState.kt:40-43`), `raiseUndoOffer` still
   `error(...)`s on anything but `SAVE` and `DISMISS` (`ui/AppViewModel.kt:675-683`), and
   `UiStateMapper` still derives `undoAvailable` as `undoAction == SAVE || undoAction == DISMISS`
   (`ui/state/UiStateMapper.kt:70`).
7. **Item 015's `expectDiscoverHead = true` is still on the four Discover lambdas** in
   `IntentionalReadingApp.kt`, and this item preserves it — including on `onMarkRead`, whose offer this item
   raises.
8. **`ArticleStateMachineUndoTest` still contains `only save and dismiss are reversible` at or near `:99`,**
   and `UiStateMapperTest`'s `undoAvailable` cases are still at or near `:543-549`. Both assert what this
   item changes; `slices.md` names what happens to each.
