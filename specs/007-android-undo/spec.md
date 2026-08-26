# 007 — Undo

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/contracts.md` §§18/22/23/31/38, `docs/v1/06-ui-ux.md`,
`design-reference/DESIGN.md` §8, `docs/v1/README.md` Amendment 6\
**Wave:** A (`specs/waves/wave-a.md`) · **Branch:** `feat/007-android-undo` → `main`

---

## 1. Problem

No Discover action on Android can be taken back. Save and Not interested commit straight to disk
through `AppViewModel.persistArticleTransition`
(`android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/AppViewModel.kt:292-330`) and the
article leaves the deck permanently: `DiscoverDeck.isEligible` admits only a null record or `OPENED`
(`domain/state/DiscoverDeck.kt:34-35`), so a dismissed article is gone until a Reset destroys the
reader's whole history. The browser has had a way out since V1 — `createUndoManager` and
`undoArticleAction` (`js/state/article-state.js:151-190`) — and `contracts.md` §31 specifies it as a
contract, not an affordance. Android simply does not implement it.

**Why now, ahead of six other items.** `contracts.md` §23 ties Undo Not Interested and Undo Save for
Later to the reversal of the `dismissed` and `saved` learning signals. Item 005 introduces those
signals and the `INTERACTION_DELTAS` arithmetic behind them. Today `ArticleStateMachine` never sets
`signalsApplied.saved` or `signalsApplied.dismissed` true — the `saved` flag is copied forward from a
prior record that can never have carried it, and `dismissed` is likewise gated on an existing true it
can never receive (`domain/state/ArticleStateMachine.kt:88-95`). So every undo record this item creates
carries **no** reversal payload, and Undo is pure state-machine inversion. Built after 005, the same
item has to unwind weight arithmetic against records already on disk that claim signals with no deltas
behind them — the largest inherited decision in the project (`specs/backlog.md` §005). The ordering is
not a preference.

### 1.1 What this item can and cannot reach

**In the browser, Undo has exactly two trigger surfaces, and both belong to item 008.** The labeled
triage buttons commit with `undoEligible` left at its `false` default
(`js/ui/discover.js:211`, `:246-248`) and therefore never populate the slot. Only the swipe controller
(`js/ui/discover.js:263`) and the arrow-key shortcuts (`:268-269`) pass `undoable: true`, and both live
in `js/ui/swipe.js` — the module item 008 ports. `js/state/article-state.js:141` enforces this at the
source: a record is created only when `undoable && (action === "save" || action === "dismiss")`.
`contracts.md` §31 says the same thing in one line — "only the most recent eligible **swipe** action
must be retained."

The reason is design intent, not accident. Undo exists to recover a *mis-trigger*. A swipe or an arrow
key can be fired by accident; a press on a button labeled "Save for later" cannot, in the sense that
matters. `DESIGN.md` §8 requires a labeled equivalent for every gesture, never the reverse, so the
labeled buttons being un-undoable is the specified asymmetry rather than an oversight.

**Consequence, settled at design time and recorded here so it is not rediscovered as a defect:** this
item ships the undo engine with **no trigger wired**. Nothing a reader can do on Android will populate
the slot until item 008 lands swipe. That is deliberate. Extending Undo to Android's labeled buttons
was considered and rejected — it would invent a requirement the browser does not have and put the two
clients in disagreement about which surfaces are reversible (`design.md` D1).

## 2. Story

As a reader, I want a triage decision I did not mean to make to be reversible for a few seconds
afterwards, so that a mis-trigger costs me a tap rather than an article I wanted.

## 3. Out of scope

- **Swipe gestures and the keyboard-equivalent triggers.** Item 008. This item creates no undo-eligible
  call site, no gesture detector, and no swipe cue. §1.1 records why.
- **The actionable toast Composable.** The undo slot's *state* is built and tested here; the surface
  that renders it and carries the Undo button lands with the trigger in 008 (`design.md` D4).
  Instrumented tests are parked from CI (`specs/backlog.md` §Parked), so an unwired Composable would
  ship uncovered and unreachable.
- **Any preference-weight reversal.** `preferences` is always empty and `signalsApplied.saved` and
  `.dismissed` are never set true. The undo record carries the reversal field specified by
  `contracts.md` §31 and leaves it null; item 005 fills it and adds the arithmetic (`design.md` D5).
- **Mark Unread and Remove from Read Later.** `contracts.md` §23 lists Mark Unread among the reversible
  learning rules, but it is a first-class action with its own transition, already implemented
  (`ArticleStateMachine.kt:76-81`), not an undo of anything. §24 explicitly withholds the negative
  signal from Remove. Neither is touched.
- **Persisting the undo slot.** `contracts.md` §31 forbids it. See scenario "an undo offer does not
  survive the process".
- **Any change to `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `tests/**`, or
  `docs/v1/**`.** Amendment 6 confines this item to `android/`.
- **New dependencies.** Nothing is added to `android/gradle/libs.versions.toml`.

## 4. Scenarios

### Scenario: an undo-eligible save records what it replaced

Given an article with no stored record\
When it is saved through a commit marked undo-eligible\
Then the article's status is `saved`\
And the undo slot holds that article's id, the `save` action, and the absence of a prior record\
And Undo is reported as available

### Scenario: an undo-eligible dismiss of an opened article records the opened record

Given an article whose stored record is `opened`\
When it is dismissed through a commit marked undo-eligible\
Then the article's status is `dismissed`\
And the undo slot holds the `opened` record exactly as it stood before the dismiss\
And Undo is reported as available

### Scenario: a commit that is not marked undo-eligible offers nothing

Given an article with no stored record\
When it is saved or dismissed through a commit that is not marked undo-eligible\
Then the undo slot is unchanged\
And Undo is reported as unavailable

### Scenario: only save and dismiss are reversible

Given an article being opened, marked read, marked unread, or removed from Read Later\
When the commit is marked undo-eligible\
Then the undo slot is unchanged\
And Undo is reported as unavailable

### Scenario: undoing a save returns the article to having no record

Given an undo-eligible save of an article that had no stored record\
When Undo is performed\
Then the article has no stored record at all\
And Discover offers it again\
And the undo slot is empty

### Scenario: undoing a dismiss restores the exact record it replaced

Given an undo-eligible dismiss of an article whose record was `opened`\
When Undo is performed\
Then the stored record equals the pre-dismiss record field for field, including `firstSeenAt`,
`openedAt`, and every `signalsApplied` flag\
And no timestamp is rewritten to the time of the undo\
And the undo slot is empty

### Scenario: an undone article returns to the head of Discover on its own

Given the reader dismissed the article Discover was showing, through an undo-eligible commit\
When Undo is performed\
Then Discover offers that article again as the article on screen\
And the held-article pin is not re-established to achieve it

### Scenario: the slot holds one action, and the newest wins

Given an undo-eligible dismiss followed by an undo-eligible save of a different article\
When Undo is performed\
Then only the save is reversed\
And the dismissed article stays dismissed\
And a second Undo is unavailable

### Scenario: Undo is refused when there is nothing to undo

Given an empty undo slot\
When Undo is requested\
Then it fails as unavailable\
And no local state is written\
And nothing is announced as having changed

### Scenario: Undo is refused when the article it names is gone

Given a populated undo slot\
And the record it names no longer exists in local state\
When Undo is requested\
Then it fails as stale\
And no local state is written

### Scenario: a failed write leaves both the state and the offer intact

Given a populated undo slot\
When Undo is performed and the local state write fails\
Then the stored state is unchanged\
And the undo slot still holds the same record\
And the persistence failure is announced through the existing live region

### Scenario: resetting local data withdraws the offer

Given a populated undo slot\
When local data is reset\
Then the undo slot is empty\
And Undo is reported as unavailable

### Scenario: an undo offer does not survive the process

Given a populated undo slot\
When the application process is recreated\
Then the undo slot is empty\
And nothing about the offer was written to local state

### Scenario: the undo record carries a reversal field that is not yet used

Given an undo-eligible save or dismiss\
When the undo record is created\
Then it carries the reversal field named by the Undo contract\
And that field is empty, because no learning signal was applied\
And performing Undo changes no preference entry

## 5. Verification

Both Android gates, re-run by the reviewer in a throwaway worktree rather than read from an implementer
report:

```sh
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

**There is no owner walkthrough for this item, and that is a stated consequence of §1.1, not an
omission.** Nothing a reader can do on the device reaches the undo slot until item 008 wires swipe. The
walkthrough for Undo is written and performed as part of 008. What the emulator *can* confirm here is a
negative, and it is folded into wave A's batched walkthrough against merged `main`:

1. Discover's Save and Not interested still commit, still advance the deck, and offer no new affordance.
2. No toast, banner, or button appeared anywhere as a side effect of this item.
3. Read Later, History, Settings, and Reset behave exactly as they did at 004.

The scenarios in §4 are therefore all JVM-testable by construction, against `AppViewModel` and
`ArticleStateMachine` with the existing fakes.
