# 006 — Deck diversity sequencing

**Status:** draft (awaiting plan gate)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/05-personalization-state.md` §§58–62, `docs/v1/README.md` Amendment 6\
**Wave:** C (`specs/waves/wave-c.md`) · **Branch:** `feat/006-android-deck-diversity` → `main`\
**Runs after:** item 005 merged

---

## 1. Problem

`05-personalization-state.md` §59 and §60 specify two temporary sequencing penalties during deck
construction — **−8** for a candidate from the same source as the previously selected card, and **−5**
for a candidate that would make a third consecutive card from the same category in the `all` view. The
browser implements both (`js/ranking/deck.js:26-38`). Android implements neither.

Item 005 gives Android the personalized candidate order §58 requires and leaves `DiscoverDeck` holding
the ordered list these penalties sequence. This item adds the sequencing, and nothing else.

**Deferred by** 002 §3, restated by 004 §3.

### 1.1 What this item does not fix, stated up front

`waves/wave-c.md` says the Android head article can differ from the browser's until this lands. **That
was 005's gap and 005 closed it.** Diversity penalties apply from the *second* selection step onward —
`penaltiesFor` reads `selected.at(-1)`, which is undefined at the first step
(`js/ranking/deck.js:26-38`) — both clients render exactly one card, and both rebuild the deck on every
render (`js/app.js:210`; Android's `UiStateMapper` on every `publish()`). So the head card is chosen by
personalized order alone in both clients, and **no penalty this item adds can change what a reader
sees at today's surface.**

The owner confirmed on 2026-08-26 that it ships anyway. The reasons it is still worth doing:

- §§59–61 are authoritative V1 behaviour, and the browser's numbers are a complete specification, so the
  port is cheap and fully verifiable now.
- The moment any surface shows more than the head card — a next-up hint, a multi-card queue, item 012's
  re-laid-out Discover — the penalties become visible, and discovering then that they were never ported
  is more expensive than porting them now.
- Leaving one specified ranking rule unported keeps a permanent asterisk against "the Android client
  matches the browser".

**The consequence for evidence is stated here so it is not argued at review:** this item's proof is its
JVM tests, and its walkthrough is a *regression* check, not a demonstration. A walkthrough step claiming
to show a diversity penalty working would be wrong.

## 2. Story

As a reader, I want a queue that does not stack three cards from one source or one category in a row, so
that a finite deck stays varied as I work through it rather than clustering.

## 3. Out of scope

- **Making the penalties visible.** No next-up hint, no multi-card deck, no "more from this source"
  affordance. `06-ui-ux.md` authors none of them, and §1.1 is the reason this item is honest about it.
- **Changing what the head card is.** The head is chosen by §58's order and is unaffected by this item
  by construction (`design.md` D3). A change to the head card in this diff is a defect.
- **New penalty values, new penalty kinds, or tuning.** −8 and −5 are authoritative. A "third
  consecutive source" rule, or a category penalty in a category view, is an amendment.
- **Persisting anything.** §61: penalties are temporary. They are not stored, not exported, and not part
  of any record.
- **Preferences, deltas, reconciliation, and the score components.** All item 005, merged before this
  starts.
- **Eligibility, category filtering, and the counts.** Sequencing permutes the candidate list; it never
  changes its membership, so `availableCount` and `remainingCount` are untouched.
- **Read Later and History.** §63: `savedAt` and `readAt` descending, no penalties.
- **Any change to `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `tests/**`, or
  `docs/v1/**`.** Amendment 6 confines this item to `android/` plus this item's own `specs/006-*/`.
- **New dependencies.** `android/gradle/libs.versions.toml` is untouched.

## 4. Scenarios

The browser's own deck tests are the fixtures (`tests/js/ranking.test.js:92-143`), and their expected
orders are ported as literal expectations rather than re-derived.

### 4.1 Same-source diversity

### Scenario: the second card avoids the head card's source

Given three eligible candidates in one category, two from source A and one from source B, ordered A, A,
B by personalized total\
When the deck is sequenced\
Then the order is A, B, A\
And the third card's same-source penalty is 0

### Scenario: a sufficiently stronger candidate keeps its place despite the penalty

Given the second candidate from source A outranks the source B candidate by more than 8\
When the deck is sequenced\
Then the second card is still from source A\
And its same-source penalty is recorded as −8

### Scenario: the penalty compares against the previous card only

Given a sequence whose first and third selected cards are from source A, and a further source A
candidate\
When the fourth card is selected\
Then only the third card's source is considered\
And a candidate matching an earlier card's source carries no penalty for that reason

### Scenario: the first card carries no penalty

Given any set of candidates\
When the deck is sequenced\
Then the first card's same-source penalty is 0\
And its category penalty is 0\
And it is the same article §58's order alone would put first

### 4.2 Category diversity

### Scenario: the all view breaks a third consecutive category

Given four eligible candidates ordered technology, technology, technology, science by personalized
total\
And the all view is selected\
When the deck is sequenced\
Then the order is technology, technology, science, technology

### Scenario: a category view disables the penalty entirely

Given the same four candidates filtered to a single category\
When the deck is sequenced\
Then the order is the personalized order\
And every candidate's category penalty is 0

### Scenario: a sufficiently stronger third candidate keeps its place

Given a third consecutive same-category candidate that outranks the alternative by more than 5\
When the deck is sequenced\
Then it is selected third\
And its category penalty is recorded as −5

### Scenario: two consecutive cards from one category are not penalized

Given the all view and two candidates from the same category at the head of the order\
When the deck is sequenced\
Then the second card's category penalty is 0

### Scenario: the penalty needs two previously selected cards

Given the all view and a deck of exactly two candidates from the same category\
When the deck is sequenced\
Then no category penalty is applied to either

### 4.3 Composition with 005's weights

### Scenario: the penalty is applied to the personalized total, not to the base score

Given a candidate whose source weight and exploration bonus place it first by personalized total\
And it shares the previously selected card's source\
When its sequencing score is computed\
Then that score is its personalized total minus 8\
And its personalized total and its four components are unchanged

### Scenario: a preference weight can outweigh a penalty and a penalty can outweigh a weight

Given two candidates whose personalized totals differ by less than 8, the stronger sharing the previous
card's source\
Then the weaker is selected next\
And when the totals differ by more than 8, the stronger is selected next despite the penalty

### Scenario: ties after penalties fall through to §58's order

Given two candidates whose sequencing scores are equal\
When the next card is selected\
Then the winner is the one §58's five keys choose — total descending, base descending, publication date
descending with unknown last, source ID ascending, article ID ascending

## 4.4 Purity and determinism

### Scenario: sequencing modifies nothing

Given any deck build\
Then no article base score, no personalized component, no preference entry, no interaction count, and no
persisted record differs afterwards\
And no penalty is written to any article or record\
And no local-state write occurs

### Scenario: identical inputs produce identical decks

Given the same articles, records, preferences, and category\
When the deck is built twice\
Then both decks are equal, candidate for candidate, including the recorded penalties

### Scenario: the deck is a permutation of the candidate set

Given any eligible candidate set\
When the deck is sequenced\
Then it contains exactly the same articles, each once\
And the available and remaining counts are unchanged from item 005's values

### Scenario: the held card still wins

Given an opened article held on screen whose sequenced position is not first\
When Discover renders\
Then the held article is the card on screen

## 5. Verification

### 5.1 Gates

Both Android gates, re-run by the reviewer with `--rerun-tasks` in a throwaway worktree, with
`app/build/test-results/testDebugUnitTest` deleted first (`waves/wave-b-note.md` §7):

```sh
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

Baseline is **item 005's merged count on `main`**, recorded at the moment 005 merges — not the 198 this
wave opened at, and not a number carried over from 005's slice reports.

**A parity cross-check is part of this item's gate, not an extra:** `npm test` still green, unchanged, to
show `js/**` was not touched while its numbers were being read as the specification.

### 5.2 Owner walkthrough

**This is a regression check.** Nothing this item adds is visible at today's surface (§1.1), so no step
below claims to demonstrate a penalty. Driven over `adb` on the `Pixel_10` API 37 emulator against
merged `main`, screenshotting each step, with `waves/wave-b-note.md` §2's second question at every one:
*and is what the reader needs now actually on screen?*

1. **The head card did not move.** Note the head card and the available count on the 005 build. Install
   this build over it without resetting. The same article and the same count are on screen.
2. **A swipe still advances correctly.** Dismiss three cards in a row. Each incoming card is fully
   visible, the counts decrement by one each time, and no card repeats.
3. **The category selector still filters.** Switch to a specific category and back to all. The counts
   and the head card match the 005 build's for both selections.
4. **The held card still holds.** Open an article, return from the publisher, and confirm the same card
   is on screen with Mark read reachable without scrolling.
5. **Undo still returns the card.** Save an article, undo from the toast, and confirm it is back at the
   head.
6. **No jank at full dataset size.** With a full 500-article dataset cached, swipe through ten cards and
   confirm no visible stall between cards (`design.md` D4).

**What the owner is asked for, and only this:** step 6's judgement on responsiveness at full dataset
size, and step 1's confirmation that the head card genuinely did not move — the two things this item
could plausibly break.
