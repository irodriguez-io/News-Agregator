# 019 — Material 3 Expressive Discover

**Item:** 019\
**Branch:** `feat/019-android-m3-discover`\
**Wave:** E, third item — runs after 018 merges, concurrently with 020\
**Cut from:** `main`, after 018 is merged

Cites, and amends nothing: `docs/v1/06-ui-ux.md` §§13.2, 21, 22.2, 23, 24, 25, 26.2, 27.2, 28.2, 29, 30, 31,
32.2, 35.2, 71.2, 73, 74.1, 74.2, 76.2, 76.5; **Amendment 7**;
`specs/012-android-discover-card-first/spec.md` §1.4.

---

## 1. Why this item exists

Discover is the decision surface. This item makes it look like the design's Article Deck Card without
changing a single thing it does.

### 1.1 It inherits two rules it must not break

**Amendment 7, the placement rule.** Discover's compact masthead comes first, then the article card, then
the operational block. The binding form is the intent, not the widget order: *the first thing in the
viewport on Discover is the article card, not the controls that describe it* (§21).

Amendment 7 explicitly grants a redesign the licence to re-lay out either half. **This item exercises that
licence, and the licence is not permission to restore the old ordering while doing so.** Re-laying out
Discover and quietly putting the operational block back on top would break the rule item 012 exists to have
established.

**Item 012's 360 dp measurement.** 012 recorded that the card's height is unbounded in its title — a real
*Science / AAAS* headline wraps to six lines and consumes a 360 dp viewport on its own, putting the card's
own action rail below the fold. 012 could not fix it, named it as wave E's ground, and handed it here
(`012/spec.md` §1.4).

### 1.2 The three-line clamp is the fix, and it is now specified

§13.2 clamps the deck headline at three lines and the description at two. That is the amendment 012 said
would be needed against §25's prohibition, and it has been made.

**So this item is where 012's finding is closed**: with the headline clamped, the card's height is bounded,
and the excerpt, tags and all three action controls fit above the fold at 360 dp. The measurement is this
item's evidence, not a nice-to-have.

Today `ArticleCard.kt:210` clamps the excerpt at four lines and applies **no clamp at all** to the title.

### 1.3 It adopts 018's controls rather than restyling its own

The action rail's three controls are written inline in this file today. Item 018 ships shared versions
(§76.5, §32.2, §35.2). This item **replaces the inline treatments with calls to 018's**, which is the other
half of the transitional duplication 018 accepted.

### 1.4 There is no image slot, and no gap where one was

The design sources put a 16:9 media slot at the top of this card. `ArticleDataset v1` has no image field
(§74.2). The card leads with the Playfair headline in the slot's place.

**Laid out so a media slot could be added later without a re-layout** (§74.2), and judged at walkthrough on
whether it reads as a composition rather than as something missing.

---

## 2. Story

As **the reader**, I want the article I am deciding about to be the first and most substantial thing on
Discover, set like a headline rather than a form field, so the decision feels worth making.

---

## 3. Out of scope

- **`ui/screens/readlater/**`, `ui/screens/history/**`, and `components/{ArticleRow, EditorialHeader,
  EmptyStatePanel, StatBand}.kt`.** All item 020's.
- **`components/CategoryChipRow.kt` and `components/BottomNavigationBar.kt`.** Item 018's. This item calls
  the chip row; it does not restyle it.
- **`ui/IntentionalReadingApp.kt`.** Items 018 and 021.
- **Any swipe behaviour** — the surface, the 90 dp threshold, the cues, the commitment sequence, the undo
  offer (§39–§43, §70). Presentation of the card may change; what a swipe *does* may not.
- **Any state, count, ranking, deck ordering or preference signal.**
- **Any token value.** All from 017.
- **Motion.** Item 021. §44's swipe character constraint binds regardless.
- **Imagery**, in any form.
- **Any new user-facing string.** §75.2.

---

## 4. Scenarios

### Scenario: the card still leads the viewport
Given Discover with an article available
When the screen is composed
Then the compact masthead is the first element
And the article card follows it
And the operational block follows the article card

### Scenario: the headline clamps at three lines
Given an article whose title wraps to more than three lines at the deck headline size
When the card is rendered
Then the title occupies at most three lines and is ellipsised

### Scenario: the description clamps at two lines
Given an article with a long excerpt
When the card is rendered
Then the excerpt occupies at most two lines and is ellipsised

### Scenario: the whole card fits above the fold at 360 dp
Given the widest real dataset title and a 360 dp viewport
When Discover is composed cold
Then the card's headline, excerpt, tags and all three action controls are visible without scrolling

### Scenario: an empty excerpt is omitted, not filled
Given an article whose excerpt is empty
When the card is rendered
Then no excerpt area is shown
And no placeholder text appears

### Scenario: the card is text-first with no reserved image area
Given any article
When the card is rendered
Then no image, thumbnail or media placeholder is present
And no empty region is reserved for one

### Scenario: the badge shows the authoritative content-type label
Given an article with a content type
When the badge is rendered
Then it displays that content type's own label, unaltered except for case

### Scenario: reading time and publication age are omitted when unknown
Given an article with a null reading time and an unknown publication date
When the card is rendered
Then no reading-time value appears
And no age or date appears
And no zero is shown in either place

### Scenario: tags are neutral, capped, and not controls
Given an article with more than five tags
When the card is rendered
Then at most five are shown
And they carry no category colour
And they are not interactive

### Scenario: the action rail uses the shared controls
Given the card's action rail
When it is rendered
Then Not interested, Read article and Save for later are present
And each is one of item 018's shared controls
And each is operable without a gesture

### Scenario: the triage controls keep compliant targets
Given the two circular triage controls
When each is measured
Then each is at least 48 dp in both dimensions
And each carries an accessible name

### Scenario: swipe behaviour is unchanged
Given the card
When it is dragged past the threshold in either direction
Then the same action is emitted as before this item
And the threshold, the directional cue and the commitment sequence are unchanged

### Scenario: the non-card states keep their composition
Given no eligible article, a loading dataset, and a failed dataset in turn
When each state is composed
Then each still presents its own truthful message and its route onward

### Scenario: nothing outside the theme names a value
Given the files this item touched
When they are inspected for colour, radius and font literals
Then there are none

---

## 5. Verification

### 5.1 Gates

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

One gate surface: `android.yml`. No `js/**` change, so no shared-copy gate — and **no new string may be
introduced** (§3).

**Definition-of-done diff check** — must print nothing:

```bash
git diff --name-only main... \
  | grep -vE '^android/app/src/(main/kotlin/io/irodriguez/intentionalreading/ui/(components/ArticleCard\.kt|screens/discover/)|test/|androidTest/)' \
  | grep -v '^specs/'
```

### 5.2 What is assertable, and what is not

**Assertable at the JVM layer:** the clamp values; the omission rules for excerpt, reading time and age; the
badge label's fidelity; the tag cap; the presence of the three rail actions; the absence of literals.

**Assertable only in a Compose UI test:** composition order — that the card precedes the operational block.
Item 012 asserted this and its test is the precedent to follow rather than re-invent.

**Assertable only by measurement on a device:** *the whole card fits above the fold at 360 dp.* This is the
scenario that closes item 012's finding and **it cannot be proven by a JVM test.** It requires a 360 dp
walkthrough with a screenshot, against the same long title 012 used.

**Item 006's lesson applies to the clamp.** Assert the clamp on the text element that carries it, not
through the screen that composes it.

### 5.3 On the existing assertion surface

Named with reasons, per `execution-model.md` §2.1 rule 5. **017 and 018 both merge between this design and
this dispatch, so read the tree at preflight and report anything unlisted before editing it.**

| Test | Why this item reaches it | Expected |
|---|---|---|
| item 012's composition-order tests | assert the card precedes the operational block | **must stay green** — this item re-lays out around them, it does not relax them |
| item 012's opened-article scroll test | asserts the scroll target is the action rail | must stay green; the clamp changes the rail's position, not its identity |
| item 008's / 013's swipe and gesture tests | assert threshold, cues, commitment, undo reset | untouched — no behaviour change |
| any `ArticleCard` excerpt test asserting four lines | §26.2 changes it to two | **expected to fail and to be updated by this item** |

The fourth row is the one to watch: a test asserting `maxLines = 4` is asserting the *browser's* rule
(§26.1) against the Android client. Updating it is correct; deleting it is not.

### 5.4 Walkthrough — required evidence

**This item's walkthrough is not optional and one step is its definition of done.**

1. **At 360 dp, cold, with the longest available title**: are the headline, excerpt, tags and all three
   controls visible without scrolling? Screenshot. This closes item 012's §1.4.
2. At the emulator's native width, both schemes.
3. Is the headline in Playfair, or has it fallen back to a platform serif?
4. Swipe left and right past the threshold: unchanged outcome, undo offered.
5. Read article, return: is Mark read on screen (§51, item 012's inherited scenario)?
6. Change category: does the deck return to the top?
7. Empty, loading and failed-dataset states.

And after each: *and is what the reader needs next actually on screen?*

### 5.5 Owner checkpoints

A walkthrough look at merge. **Plus one judgment §74.2 asks for explicitly:** does the card, leading with
type where a media slot would be, read as a deliberate composition or as something missing?

### 5.6 Stop conditions

- A token this item needs was not defined by 017, or a control was not shipped by 018.
- The card still cannot fit its action rail above the fold at 360 dp with the clamp applied — that would
  mean §13.2's values are insufficient and is an amendment question, not an implementation one.
- Preserving Amendment 7 and the new layout appear to conflict.
- A test not listed in §5.3 fails.

---

## 6. The gap this item does not close

**Imagery.** The media slot the design specifies is not built and cannot be until the dataset carries
pictures (§74.2).

**Motion.** The card arrives without the directional destination transition; item 021.

**020's screens are untouched**, so between this merge and 020's the two halves of the application look
different. That is the cost of running the pair concurrently and it is expected.
