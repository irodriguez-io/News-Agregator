# 013 — Undo must release the card's swipe lock

**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/013-android-undo-gesture-reset/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`, **and `docs/v1/**`** — this item proposes no specification
amendment and needs none.

**Cut from:** `main` at `2613959` (item 005 merged; `Android`, `Test` and `Pages` green on that commit).

---

## 1. Why this item exists

The owner reported two things after using the 005 build. Only one of them is a defect, and separating
them is most of this specification's work.

### 1.1 Undo on the labeled buttons is not a defect

Undo is offered for swipes and not for the card's labeled buttons. That is the specified behaviour, with
reasoning recorded in three places and agreement from the reference implementation:

- `contracts.md` §31 — Undo retains "only the most recent eligible **swipe** action".
- `06-ui-ux.md` §70 — "Undo is offered only for the most recent eligible Discover **swipe** and remains
  available for approximately the approved toast duration."
- The browser's button listeners at `js/ui/discover.js:246-249` call `perform(...)` with no
  `undoEligible` argument, so it takes its `false` default and the slot is never populated. Only
  `attachSwipe`'s `onCommit` (`:263`) and `installDiscoverShortcuts` (`:268-269`) pass `true`.
- `specs/007-android-undo/spec.md` §1.1 gives the intent: Undo exists to recover a *mis-trigger*. "A
  swipe or an arrow key can be fired by accident; a press on a button labeled 'Save for later' cannot."
- `specs/008-android-swipe-gestures/design.md` **D8** — *"No hardware-keyboard shortcuts, and the
  labeled buttons stay non-undoable."*

Android already matches all of it. **Nothing in this item changes which inputs offer Undo.**

### 1.2 The swipe lost after Undo is a defect

A swipe issued shortly after tapping Undo is **silently discarded**: no state change, no interaction
count change, no undo offer, and the card stays on screen. It is not a missing undo option — the swipe
itself is lost.

Reproduced over `adb` on the `Pixel_10` emulator against merged `main`, 2026-08-27. Timing-dependent:
with a 0.4 s delay between the Undo tap and the next swipe it failed **4 of 4** trials; at 0.1 s and
0.8 s it succeeded. In the failing trials the stored document was unchanged — record count and total
interaction counts identical before and after — confirming the gesture never reached the state machine.

**Root cause.** `SwipeGesture.State.commitInFlight` (`ui/gesture/SwipeGesture.kt:52`) is the only gate
that can swallow a gesture without a trace: `down()` returns `false` while it is set (`:63`), as do
`move()` (`:73`) and `release()` (`:96`). It is set in `release()` when a swipe commits (`:106`) and
cleared in exactly one place, `restore()` (`:122-126`). `restore()` is reached only from
`ArticleCard.restoreCard()`, whose two callers are a pointer lost mid-drag (`:142`) and a commit that
**failed** to persist (`:158-161`).

So after a *successful* commit the lock stays latched for the life of that state object. That is safe
only because the next card builds a fresh state — `remember(article.id, …)` at `ArticleCard.kt:83-98`.
**Undo violates the assumption**: it returns the same article to the same slot, so the restored card can
come back holding a latched, permanently deaf gesture state. The lock is never released precisely
because the commit succeeded.

---

## 2. Story

As a **reader**, I want a card that Undo returns to Discover to accept a swipe immediately, so that
undoing a mis-swipe does not cost me the card.

---

## 3. Out of scope

- **Which inputs offer Undo.** §1.1 — the labeled buttons stay non-undoable, and no keyboard shortcut is
  added (008 D8).
- **Undo in Read Later and History.** Neither client offers it and `contracts.md` §23 lists exactly two
  reversible corrective actions, Undo Not Interested and Undo Save for Later. Widening
  `ArticleStateMachine.reversibleActions` is new scope with its own specification amendment; the owner
  decided on 2026-08-27 that it becomes its own future item, recorded in `backlog.md`.
- **Any change to `docs/v1/**`.** None is required.
- **The undo offer's duration, copy, or affordance.** 008 D6 and D7 own those.
- **The held-article pin on undo.** 007 D6 settled it and it is unchanged.
- **Weakening the commit lock.** The lock is correct while a commit is in flight; see §4 scenario 3.

---

## 4. Scenarios

`SwipeGesture.State` is a pure object with no Android dependency (008 D2), so these are JVM-testable.

### Scenario: a resolved commit releases the lock

Given a gesture state that has committed a Save for Later\
When the commit resolves as persisted\
Then a new gesture is accepted\
And that gesture can lock horizontal intent, travel past the threshold, and emit its own action

### Scenario: a resolved failed commit still restores the travel

Given a gesture state that has committed a Save for Later\
When the commit resolves as **not** persisted\
Then the lock is released\
And the travel returns home, exactly as it does today

### Scenario: the lock still holds before the commit resolves

Given a gesture state that has just committed\
When a second gesture is attempted before the commit resolves\
Then the gesture is refused, nothing is consumed, and no second action is emitted\
And this is the existing assertion at `SwipeGestureTest.kt:164`, which does not change

### Scenario: releasing the lock does not fabricate an action

Given a gesture state whose commit has resolved as persisted\
When the committed action is inspected\
Then it still reports the action that was committed\
And a subsequent release that has travelled nowhere emits nothing

### Scenario: releasing the lock leaves the travel alone

Given a gesture state that has committed and been given its exit travel\
When the commit resolves as persisted\
Then the horizontal travel and the exit travel are unchanged\
And nothing animates the departing card back toward the centre

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

Baseline on `main` at `2613959`: **254 tests, 0 failures, `BUILD SUCCESSFUL`**. Delete `test-results`
before every run and read the `BUILD SUCCESSFUL` line, not the counts (`waves/wave-b-note.md` §7).

### 5.2 Walkthrough — required evidence, not optional

The unit tests cover the state object's contract. They cannot reach the recomposition path where the
defect actually appears, so a device check is part of this item's definition of done.

Driven over `adb` on the `Pixel_10` API 37 emulator against merged `main`:

1. Swipe a Discover card to save it. The undo offer appears.
2. Tap **Undo**. The card returns to Discover.
3. Swipe it again **immediately**. The swipe must commit and must raise its own undo offer.
4. Repeat steps 1–3 at roughly **0.2 s**, **0.4 s** and **0.8 s** between the Undo tap and the second
   swipe. 0.4 s was the reliable failure point before the fix; all three must pass after it.
5. Confirm against the pulled state document that each second swipe actually moved a weight and a count
   — a card leaving the deck is not on its own proof the action landed.

**If the failing-first test cannot reproduce the defect at the state level, stop and report rather than
substituting the walkthrough for a test.** That would mean the cause lies elsewhere in the composable
and §1.2's diagnosis is wrong, which is worth knowing before any fix is written.
