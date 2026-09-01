# 020 — Material 3 Expressive Read Later and History

**Item:** 020\
**Branch:** `feat/020-android-m3-readlater-history`\
**Wave:** E, fourth item — runs after 018 merges, concurrently with 019\
**Cut from:** `main`, after 018 is merged

Cites, and amends nothing: `docs/v1/06-ui-ux.md` §§13.2, 27.2, 28.2, 29, 34.2, 52.2, 53, 54, 55, 56, 56.1,
58.2, 59, 60, 61, 62, 63, 71.2, 72.2, 73, 74.2, 75.2, 76.6.

---

## 1. Why this item exists

Read Later and History are lists, and this item makes them look like the design's Queue Row and StatBand
without changing one value either of them shows.

### 1.1 It owns four files the wave brief assigned elsewhere

`waves/wave-e.md`'s matrix gives `ui/components/**` to item 018. Measured by caller, four of that
directory's files are consumed **only** by Read Later and History and are therefore this item's:

```text
ui/components/ArticleRow.kt        the Queue Row
ui/components/EditorialHeader.kt   both screen headers
ui/components/EmptyStatePanel.kt   both empty states
ui/components/StatBand.kt          the overview band
```

Item 018's spec records the correction (`018/spec.md` §1.2) and does not touch them.

### 1.2 The null rules are already decided *and already implemented*

`wave-e.md` asks this item to "decide the null presentation" for `readingTimeMinutes`. **It is not this
item's decision and it never was.**

§29 and §54 settle it: omit the value, never display `0 min`, never estimate locally. And
`StatBand.kt:75–77` already implements exactly that — `if (minutes > 0) "~$minutes min" else "Unavailable"`,
with `availableStatValue` omitting empty values.

**So this item preserves behaviour that already exists.** A scenario asserts it, because a re-layout is
precisely when a working omission rule gets replaced by a tidy-looking zero.

### 1.3 This is a one-gate item, and here is why

`wave-e.md` asks whether shared copy makes this a two-gate item. **It does not.** Every string §63's
high-fidelity empty state needs already exists:

```text
history_empty_title   "No reading history yet"
history_empty_copy    "Articles appear here after you explicitly mark them read. …"
go_to_discover        the action label
read_later_empty_title / read_later_empty_copy
```

§63's "high-fidelity" is a **visual** upgrade to an existing composition with existing copy. `android.yml`
only.

**This holds only while no new string is introduced.** §75.2 makes any new user-facing string shared copy,
which would pull in `js/**` and its validators and change the gate surface. **If this item finds itself
needing a string, that is a report to the supervisor before it is added**, not a decision to take mid-slice.

### 1.4 A wave D observation this item must fix

Wave D's walkthrough recorded that **the Undo toast overlaps the bottom row's action rail on Read Later and
History while it is showing.** The toast is hosted globally (§45.2) so it renders over both lists.

A reader who saves, then scrolls to the last row, cannot reach that row's actions while an offer is up. This
item re-lays out both lists and is where the bottom inset gets fixed.

### 1.5 The Queue Row has no thumbnail

The design sources put an 80 dp square thumbnail on this row. `ArticleDataset v1` has no image field
(§74.2). The horizontal space goes to the headline and metadata, laid out so a media slot could be added
later without a re-layout.

---

## 2. Story

As **the reader**, I want my queue and my history to read like a considered list rather than a table, and I
want every number in them to stay true.

---

## 3. Out of scope

- **`ui/components/ArticleCard.kt` and `ui/screens/discover/**`.** Item 019's.
- **`ui/components/{BottomNavigationBar, CategoryChipRow}.kt`, `ui/IntentionalReadingApp.kt`.** Items 018
  and 021. **Including the Undo toast itself** — §1.4's fix is a bottom inset in these screens, not a change
  to `UndoToast.kt` or its hosting.
- **Any value shown.** Counts, sums, topics, dates, statuses. Presentation only.
- **Any behaviour**: Mark read, Remove, Reopen, Mark unread, and their undo paths and count updates
  (§56, §62, §70) are untouched.
- **Any new user-facing string** (§1.3). Report instead.
- **Any token value.** All from 017.
- **Motion.** Item 021.
- **Imagery**, in any form.

---

## 4. Scenarios

### Scenario: the Queue Row is a tonal container with no shadow
Given a Read Later row
When it is rendered
Then it has a 16 dp radius and a tonal fill
And it carries no shadow and no elevation

### Scenario: the Queue Row has no thumbnail and reserves no space for one
Given any row on Read Later or History
When it is rendered
Then no image, thumbnail or media placeholder is present
And no region is reserved for one

### Scenario: the row title clamps at two lines
Given an article whose title is long
When its row is rendered
Then the title occupies at most two lines and is ellipsised

### Scenario: row actions keep compliant targets
Given a row's actions
When each is measured
Then each is at least 48 dp in both dimensions
And each is operable without hover

### Scenario: the StatBand groups three values in a pill container
Given Read Later with saved articles
When the overview band is rendered
Then it is a three-column container with a 16 dp radius and a tonal fill
And its numerals are set in the editorial register

### Scenario: an unknown reading-time sum is omitted, never zeroed
Given saved articles of which none has a known reading time
When the overview band is rendered
Then no reading-time number is shown
And `0 min` does not appear

### Scenario: a known reading-time sum counts only known values
Given saved articles of which some have a known reading time
When the overview band is rendered
Then the sum includes only those with a known value

### Scenario: an unavailable topic is omitted, not inferred
Given saved articles none of which carry a tag
When the overview band is rendered
Then no topic value is shown
And none is invented

### Scenario: History's empty state is high fidelity and uses existing copy
Given no read articles
When History is composed
Then its empty state is rendered at full visual weight
And its title and copy are the strings that already exist
And it offers a route to Discover
And it contains no streak, urgency or guilt language

### Scenario: the last row's actions are reachable while the Undo toast shows
Given a list scrolled to its last row
And an Undo offer is showing
When the last row's actions are located
Then they are not overlapped by the toast

### Scenario: History groups by local date
Given read articles from today, yesterday and earlier
When History is composed
Then they are grouped Today, Yesterday, Earlier by the reader's local date

### Scenario: every value shown is unchanged by this item
Given the same local state before and after this item
When both screens are composed
Then every count, sum, topic, date and status displayed is identical

### Scenario: Mark unread still returns the article and updates both counts
Given a read article in History
When Mark unread succeeds
Then the article returns to Read Later
And both counts update immediately

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

**One gate surface: `android.yml`.** Settled in §1.3 and conditional on no new string.

**Definition-of-done diff check** — must print nothing:

```bash
git diff --name-only main... \
  | grep -vE '^android/app/src/(main/kotlin/io/irodriguez/intentionalreading/ui/(components/(ArticleRow|EditorialHeader|EmptyStatePanel|StatBand)\.kt|screens/(readlater|history)/)|test/|androidTest/)' \
  | grep -v '^specs/'
```

`res/values/strings.xml` appearing in that diff means §1.3's gate conclusion no longer holds. **Stop.**

### 5.2 What is assertable, and what is not

**Assertable at the JVM layer:** every value rule — the reading-time sum over known values only, the
omission of unknown sums and topics, the absence of `0 min`, the group boundaries; the title clamp; the
absence of new strings.

**Assertable in a Compose UI test:** target sizes; the absence of a media region; the empty state's content.

**Assertable only on a device:** *the last row's actions are reachable while the Undo toast shows.* This is
wave D's observation and the fix is an inset; **the toast is invisible to `uiautomator dump` and must be
captured with `screencap`** (wave D's recorded tooling lesson). A screenshot at the bottom of a scrolled
list with an offer up is the evidence.

**Item 006's lesson:** assert the sum rule against the summing function, not through the screen that renders
it.

### 5.3 On the existing assertion surface

Named with reasons, per `execution-model.md` §2.1 rule 5. **017 and 018 both merge beneath this item, and
019 runs concurrently.** Read the tree at preflight.

| Test | Why this item reaches it | Expected |
|---|---|---|
| StatBand / reading-time sum tests | assert the known-only sum and the omission rules | **must stay green untouched** — this item changes presentation, not arithmetic |
| History grouping tests | assert Today / Yesterday / Earlier by local date | must stay green |
| Mark read / Remove / Mark unread / undo tests | assert behaviour and count updates | untouched |
| any row test asserting a thumbnail or fixed row height | the row is re-laid out | enumerate at preflight |

**The first row is the important one.** A re-layout that "tidies" `if (minutes > 0) … else "Unavailable"`
into a formatted zero passes a visual check and fails this table.

### 5.4 Walkthrough — required evidence

Against **real accumulated history**, preserved with `adb install -r` — never uninstall.

1. Read Later and History, both schemes, with rows present.
2. The StatBand with a known sum, and with none known.
3. **Scroll to the last row, raise an Undo offer, and confirm the row's actions are reachable.**
   `screencap` at ~0.35 s; `uiautomator dump` cannot see the toast.
4. History's empty state at full weight, both schemes.
5. Mark unread from History: article returns, both counts move immediately.
6. At 360 dp: rows readable, actions reachable, StatBand's three columns intact.

The Undo tap target moves with the message width — **re-locate before every tap, including after an
action**, and score by article id. A missed Undo looks exactly like a passing run.

And after each step: *and is what the reader needs next actually on screen?*

### 5.5 Owner checkpoints

A walkthrough look at merge. **Plus §74.2's judgment for this surface:** does the row, spending the
thumbnail's width on type, read as a deliberate composition?

### 5.6 Stop conditions

- **Any new user-facing string appears necessary.** Report before adding it — it changes the gate surface
  (§1.3).
- A token this item needs was not defined by 017.
- The toast overlap cannot be fixed by an inset in these screens without touching the toast's hosting.
- A test not listed in §5.3 fails.

---

## 6. The gap this item does not close

**The thumbnail.** Not buildable until the dataset carries images (§74.2).

**Motion.** Item 021.

**The toast's own presentation** is untouched — only the space beneath the list changes. If the toast is
later re-styled, the inset may need revisiting.
