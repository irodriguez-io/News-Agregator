# 005 — Preference learning and personalized ranking

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/05-personalization-state.md` §§10–16, §§17–31, §§54–58, §§61–62,
`docs/v1/contracts.md` §§15, 20–24, `docs/v1/README.md` Amendment 6\
**Wave:** C (`specs/waves/wave-c.md`) · **Branch:** `feat/005-android-preference-learning` → `main`

---

## 1. Problem

Android persists `preferences.sources` and `preferences.topics`, validates them against
`contracts.md` §15, and round-trips them through export and import. **Nothing has ever written a
non-empty entry.** Every reader on Android sees the same Discover order as every other reader with the
same dataset, because Discover renders in pipeline order and `05-personalization-state.md` §54 and §58
require it to render in personalized order.

This item ports the two browser modules that produce that order — `js/state/preferences.js` (the delta
table, the clamp, apply, reverse) and `js/ranking/personalize.js` (the four score components) — and
wires them into the three files that already own the relevant seams.

**Deferred by** 002 §3, 003 §3, 004 §3. **Item 007 built the seam it lands in:**
`UndoRecord.preferenceReversal` exists today as `typealias PreferenceReversal = Nothing`, declared and
always null precisely so this item could fill it without reshaping the undo path
(`specs/007-android-undo/design.md` D5).

### 1.1 The inherited decision, and what was chosen

Every record on disk claims `signalsApplied` flags with no deltas behind them. Item 003 persisted the
flags because the frozen validator requires equality — `opened` must equal `openedAt` being non-null,
`read` must equal a `read` status (`LocalStateValidator.kt:137-145`; `js/state/storage.js:105-112`) —
and Android has never applied the matching arithmetic. The moment this item ships, those flags acquire
their two real jobs: the idempotency latch of `contracts.md` §22 and the reversal guard of §23.

**The drift is not immediate, and the reason matters for the fix.** The browser's reversal no-ops when
the preference entry is absent or its count is already zero (`js/state/preferences.js` `reverseFromEntry`),
so a `Mark Unread` against an empty `preferences` map does nothing. The damage starts at the reader's
**first post-005 signal on the same source or topic**, after which a pre-005 `Mark Unread` subtracts a
weight nobody added and — worse — decrements an `interactions` count that belongs to a different
article's signal. The count is the more expensive of the two: source exploration is +2 at one
interaction and +1 at two (§57), so a stolen count moves roughly 1.25 of ranking value in the wrong
direction against 0.25 of weight.

**Decision, taken by the owner on 2026-08-26: reconcile by invariant, on every load and after every
import.** `interactions` is exactly recomputable from the records, because every applied signal lives
in exactly one record's `signalsApplied` and every reversal decrements flag and count together
(§13; `contracts.md` §15). So the reconciliation compares each source's and topic's **claimed** signal
count against its stored count and folds in only the difference. It is idempotent by construction — the
second pass finds nothing — it covers documents that arrived by import as well as the device's own
history (`specs/009-android-import-export/design.md` D1), and it needs no new persisted key, which
`contracts.md` §14 would forbid.

Weight is deliberately *not* verified the same way: clamping is order-dependent, so a stored weight
cannot be recomputed from the record set. Only the missing signals' deltas are applied.

### 1.2 What 006 is not asked to fix, contrary to the wave brief

`waves/wave-c.md` states that the Android head article can differ from the browser's until 006 lands.
That is 005's gap, not 006's. Diversity penalties apply from the *second* selection step onward
(`js/ranking/deck.js` `penaltiesFor` reads `selected.at(-1)`), both clients render one card, and the
browser rebuilds the deck on every render, so the head article is chosen by personalized order alone in
both clients. 006 remains authoritative (§§59–61) and remains a separate item; it changes nothing a
reader can see while Discover shows one card. Recorded here so 005's walkthrough is not read as
evidence for 006.

## 2. Story

As a reader, I want Discover to lead with the sources and topics I have actually chosen to read, so
that a finite queue gets more useful the longer I use it rather than staying an arbitrary ordering of
the same dataset.

## 3. Out of scope

- **Diversity sequencing.** The −8 same-source and −5 third-consecutive-category penalties are item
  006, next in this wave, on its own branch. `design.md` D12.
- **Category preference weights.** `05-personalization-state.md` §16 prohibits them outright. A
  `preferences.categories` map in the diff is a contract violation, not a feature.
- **Any change to base scores, the dataset, or the pipeline.** §61: learning and ranking read
  `article.score.base` and never write it. `pipeline/**` and `config/**` are forbidden paths.
- **New delta values, new events, or tuning.** The four events and eight numbers of §11 are
  authoritative for V1. An item that "improves" them is an amendment.
- **A debug or explain surface.** The browser exposes score components under `?debug=1`
  (`js/app.js:227-230`); Android gets no equivalent here. `06-ui-ux.md` authors no such surface.
- **Read Later and History ordering.** §63: `savedAt` and `readAt` descending, no personalization, no
  penalties. Both stay as they are.
- **Re-deriving `signalsApplied` on load.** The stored flags become authoritative
  (`design.md` D4); the derivation helper 003 introduced is removed rather than kept as a fallback.
- **Recovering stranded weight exactly.** Where a document's stored count exceeds its claimed count,
  the count is corrected and the residual weight is left in place, because the document does not say
  which delta produced it (`design.md` D8).
- **Any change to `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `tests/**`, or
  `docs/v1/**`.** Amendment 6 confines this item to `android/` plus this item's own `specs/005-*/`.
- **New dependencies.** `android/gradle/libs.versions.toml` is untouched.

## 4. Scenarios

### 4.1 The deltas and the latch

### Scenario: each of the four events applies its exact V1 delta

Given an article whose source has no preference entry and which carries two distinct topics\
When the reader performs First Open, then Save for Later\
Then the source weight is the sum of +0.10 and +0.45\
And each topic weight is the sum of +0.05 and +0.30\
And the source interaction count is 2\
And each topic interaction count is 2

### Scenario: a topic that appears twice on one article is trained once

Given an article whose tag list contains the same topic ID twice\
When any learning signal is applied\
Then that topic's weight moves by exactly one delta\
And its interaction count moves by exactly one

### Scenario: a forced tag trains preferences like any other tag

Given an article carrying an approved forced tag\
When a learning signal is applied\
Then the forced tag's topic is trained on the same terms as an organically detected tag

### Scenario: repeating an action does not train twice

Given an article that has already been opened, saved, and marked read\
When the reader opens it again, saves it again, and marks it read again\
Then no preference weight changes\
And no interaction count changes

### Scenario: weights are clamped at both ends

Given a source weight at +5.0\
When a Save for Later signal is applied to an article from that source\
Then the weight stays at +5.0\
And the interaction count still increments\
And the symmetric case at −5.0 with Not Interested holds the weight at −5.0

### Scenario: a lazily created entry appears only when a signal reaches it

Given preferences with no entry for a source or topic\
When no signal has touched them\
Then no entry exists, and both are read as weight 0 with 0 interactions

### Scenario: Remove from Read Later trains nothing

Given a saved article whose Save signal is applied\
When the reader removes it from Read Later\
Then no preference weight changes\
And the Save signal stays applied

### 4.2 Reversal

### Scenario: Mark Unread reverses the Read signal it can prove was applied

Given a read article whose `read` signal is applied\
When the reader marks it unread\
Then the Read delta is subtracted from its source and from each of its topics\
And each interaction count decrements\
And the `read` signal is no longer applied\
And no Save signal is applied by the transition

### Scenario: Mark Unread on a record with no applied Read signal changes no weight

Given a read article whose `read` signal is not applied\
When the reader marks it unread\
Then no preference weight and no interaction count changes\
And the article still moves to Read Later

### Scenario: an interaction count never falls below zero

Given a preference entry at 0 interactions\
When a reversal reaches it\
Then the count stays at 0\
And the entry's weight is not decremented

### Scenario: an entry that reverses to nothing is removed

Given a preference entry at exactly one interaction whose weight is exactly its single delta\
When that signal is reversed\
Then the entry is gone from the map rather than left at zero

### Scenario: Undo Save and Undo Dismiss reverse the signal they applied

Given a Save that applied its signal and left an undo slot\
When the reader undoes it\
Then the Save delta is subtracted from source and topics\
And the counts decrement\
And the restored record is the one that existed before the Save\
And the symmetric case holds for Undo Not Interested

### Scenario: an Undo of an action that applied no signal touches no weight

Given a Save on an article whose Save signal was already applied, so the transition applied no new
signal\
When the reader undoes it\
Then no preference weight changes\
And the previous record is still restored

### Scenario: an undone action can be redone without training twice

Given a Save that was applied and then undone\
When the reader saves the same article again\
Then the source and topic weights equal their values after the first Save\
And the interaction counts equal their values after the first Save

### Scenario: reversal trains against the record's stored snapshot

Given a read record whose article is no longer present in the current dataset\
When the reader marks it unread from History\
Then the reversal applies to the source and topics of the record's stored snapshot

### 4.3 Reconciliation

### Scenario: a pre-learning record's claimed signals are folded in on load

Given stored state whose only record is `read` with `openedAt` populated, `opened` and `read` applied,
and an empty `preferences` map\
When local state loads\
Then its source holds the sum of the First Open and Mark Read deltas at 2 interactions\
And each of its topics holds the sum of their two deltas at 2 interactions

### Scenario: the fold runs once and is a no-op afterwards

Given state that was reconciled on a previous launch and persisted\
When local state loads again\
Then no preference weight and no interaction count changes\
And the reconciliation performs no write

### Scenario: a Mark Unread on a pre-learning record does not drift

Given stored state carrying one pre-learning `read` record from source S with an empty `preferences`
map\
When local state loads\
And the reader opens and saves a different article from source S\
And the reader then marks the pre-learning record unread\
Then S's weight equals exactly the deltas of the signals still applied across both records\
And S's interaction count equals the number of signals still applied across both records

### Scenario: an imported document is reconciled

Given a valid backup whose records claim signals its `preferences` map does not account for\
When it is imported\
Then the fold is applied to the imported state\
And the import's replacement semantics are unaffected — nothing from the previous state survives

### Scenario: a document claiming fewer signals than it counts has its counts corrected

Given stored state whose source entry reports more interactions than its records claim\
When local state loads\
Then that count is lowered to the claimed count\
And the entry's weight is left unchanged

### Scenario: the fold covers saved and dismissed signals, not only opened and read

Given an imported document with a record whose `saved` signal is applied and a record whose `dismissed`
signal is applied, and preferences that do not account for either\
When it is reconciled\
Then the Save for Later and Not Interested deltas are both folded in

### Scenario: the fold is deterministic

Given state whose fold requires clamping in more than one order\
When it is reconciled twice from the same bytes\
Then both results are equal field for field

### Scenario: a state that fails to load is not reconciled or written

Given a stored document that fails validation\
When the app loads\
Then the recovery notice is raised as it is today\
And no reconciliation write is attempted

### Scenario: an ordinary save is not reconciled

Given local state in memory after a successful article transition\
When it is persisted\
Then no fold runs on the saved result

### 4.4 Personalized ranking

### Scenario: Discover leads with the reader's preferred source

Given two Discover-eligible articles with equal base scores, from different sources\
And a positive weight on the second article's source large enough to overcome its exploration
disadvantage\
When Discover renders\
Then the second article is the card on screen

### Scenario: the personalized total is the sum of its four components

Given an eligible article\
When its candidate score is computed\
Then the total equals its base score plus its source weight plus its clamped topic sum plus its
exploration bonus

### Scenario: the topic component is clamped to ±6 independently of the ±5 weight clamp

Given an article carrying three topics whose weights sum above +6\
When its candidate score is computed\
Then its topic component is +6\
And each individual topic weight is unchanged

### Scenario: exploration follows the interaction counts, not randomness

Given an article whose source has 0 interactions and whose least-seen topic has 0 interactions\
Then its exploration component is 3, being the capped sum of +3 and +2\
And an article whose source has 2 interactions and whose least-seen topic has 2 gives 1.5\
And an article whose source and topics all have 3 or more gives 0

### Scenario: an article with no topics has no topic exploration

Given an eligible article with an empty tag list\
Then its topic preference component is 0\
And its topic exploration is 0

### Scenario: identical inputs produce identical order

Given a set of candidates with colliding totals and colliding base scores\
When the deck is built twice\
Then both orders are identical\
And ties resolve by publication date descending with unknown dates last, then source ID ascending, then
article ID ascending

### Scenario: the held card survives a rerank that would move it

Given an opened article held on screen\
And preferences that place it fifth in personalized order\
When Discover renders\
Then the held article is still the card on screen\
And the available and remaining counts are unchanged by the pin

### Scenario: ranking changes nothing it reads

Given any deck build\
Then no article base score, no preference entry, no interaction count, and no persisted record is
modified\
And no write to local state occurs

### Scenario: category filtering and eligibility are unchanged

Given a category selection and a mix of unseen, opened, saved, dismissed, and read records\
When the deck is built\
Then exactly the articles eligible today are candidates\
And the available count is unchanged by personalization

### 4.5 The queues and the counts

### Scenario: Read Later and History ignore personalization

Given weights that would reorder both queues if applied\
When Read Later and History render\
Then Read Later is ordered by `savedAt` descending and History by `readAt` descending

## 5. Verification

### 5.1 Gates

Both Android gates, re-run by the reviewer with `--rerun-tasks` in a throwaway worktree rather than read
from an implementer report, and with `app/build/test-results/testDebugUnitTest` deleted first
(`waves/wave-b-note.md` §7):

```sh
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

Baseline on `main` at `48abb67`: **198 tests, 0 failures, `BUILD SUCCESSFUL`**, verified 2026-08-26.

Two evidence obligations this wave adds (`waves/wave-c.md` §Gates), both discharged by §4.3:

- the reconciliation runs exactly once and is idempotent across restarts;
- a `Mark Unread` on a pre-learning record is proven not to drift.

### 5.2 Owner walkthrough

Driven over `adb` on the `Pixel_10` API 37 emulator, against merged `main`, screenshotting every step.
**This item's walkthrough must run on accumulated history, not fresh state** (`waves/wave-c.md`
§Owner checkpoints 3) — the behaviour only exists after a reader has used the app for a while.

**Every step carries two questions, not one** (`waves/wave-b-note.md` §2): first the assertion below,
then *and is what the reader needs now actually on screen?* A step that answers only the first is not
complete.

1. **Start from the pre-005 build's state.** Install the 005 build over an existing install carrying
   real history — saves, dismissals, reads, and at least one article opened and returned from. Do not
   reset. Pull the state file with `adb` **before** first launch and keep it as the pre-fold baseline.
2. **First launch.** Pull the state file again. `preferences.sources` and `preferences.topics` are now
   populated, and for every source and topic the interaction count equals the number of applied signals
   the records claim. Discover renders a card.
3. **Second launch.** Force-stop, relaunch, pull again. The document is byte-identical to step 2's.
4. **The order moved, and in the right direction.** Note the head card at step 2. Confirm its source or
   topics are ones the accumulated history favours, and that it is not simply the pipeline's first
   article.
5. **Train a source visibly.** Save two articles from one source, then return to Discover. The head
   card is from that source or from one of its topics unless a stronger candidate outranks it — read the
   pulled weights to confirm which.
6. **Mark Unread on a pre-005 record.** From History, mark unread an article that was read before this
   build. Pull the state file. Its source's weight and count fell by exactly the Mark Read delta and by
   exactly one — not by more, and not below the values of the signals still applied.
7. **Undo a Save.** Save an article, note the weight, undo from the toast, pull. The weight and count
   returned to their pre-Save values, and the card is back in Discover.
8. **Redo it.** Save the same article again. The weight equals its value after step 7's original save,
   not twice the delta.
9. **The held card.** Open an article, return from the publisher, and confirm the same card is still on
   screen and Mark read is reachable without scrolling — the wave B defect this item's rerank could
   resurrect.
10. **Import an old backup.** Import the wave B export from `specs/009-*/evidence.md` — the one whose
    record claims `"read": true` with `preferences` empty. Pull. The fold has been applied to the
    imported document, and Undo is unavailable.
11. **Reset.** Confirm preferences return to empty and Discover returns to an unpersonalized order.
12. **TalkBack.** Nothing new is announced by this item; confirm the Discover card, the undo toast, and
    the queues are unchanged in what they say.

**What the owner is asked for, and only this:** step 1's real accumulated history and a judgement at
steps 4 and 5 on whether the order is *better*, not merely different. Ranking quality is a product
question no test can answer.
