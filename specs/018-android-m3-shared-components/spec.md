# 018 — Material 3 Expressive shared components

**Item:** 018\
**Branch:** `feat/018-android-m3-shared-components`\
**Wave:** E, second item — runs after 017 merges; blocks 019 and 020\
**Cut from:** `main`, after 017 is merged

Cites, and amends nothing: `docs/v1/06-ui-ux.md` §§8.2, 17, 18.2, 20.2, 22.2, 32.2, 33.2, 34.2, 35.2, 37.2,
72.2, 73, 73.1, 76.3, 76.4, 76.5, 78.3.

---

## 1. Why this item exists

Every screen in wave E is built from the same small set of controls. This item ships them once, so 019 and
020 re-lay out their screens without each inventing a button.

**It restyles what exists and adds what does not.** The top app bar and the bottom navigation bar are
already there; the chip row is already there; the three action-rail controls are not — they are written
inline inside `ArticleCard` and `ArticleRow`, which belong to 019 and 020.

### 1.1 The shared controls are added, not extracted

`06-ui-ux.md` §76.5 and §32.2/§35.2 specify a filled primary control at 52 dp, a tonal secondary, and 56 dp
circular triage buttons with a 1.5 dp outline. Those treatments live today inside two components this item
must not touch.

So this item **adds them as new shared composables that nothing consumes yet**, and 019 and 020 adopt them
when they re-lay out their own components. Extracting them from `ArticleCard` and `ArticleRow` here would
mean editing 019's and 020's files, which §3 forbids and which would put a merge conflict in front of both.

Same shape as item 017's token growth, and the same reason: land the producer while nothing consumes it.

### 1.2 The wave brief's file allocation is wrong, and this item uses the corrected one

`waves/wave-e.md`'s collision matrix gives `ui/components/**` to this item. That is too wide. Measured by
who calls each composable:

| Component | Called only by | Belongs to |
|---|---|---|
| `BottomNavigationBar` | `IntentionalReadingApp` | **018** |
| `CategoryChipRow` | Discover | **018** — chips are named as this item's work |
| `LocalStateRecoveryNotice`, `LiveStatusMessage` | `IntentionalReadingApp`, Settings | **018**, scaffold level |
| `ArticleCard` | Discover | 019 |
| `ArticleRow`, `EditorialHeader`, `EmptyStatePanel`, `StatBand` | Read Later **and** History only | **020** |
| `ImportConfirmation`, `ResetConfirmation` | Settings | nobody in wave E |
| `UndoToast` | `IntentionalReadingApp` | nobody in wave E — wave D's ground |

**Four files in `ui/components/**` are item 020's.** This item does not touch them. The matrix's
parenthetical — *"shared: app bar, nav bar, chips, buttons"* — was carrying the real allocation and the path
glob was misleading.

### 1.3 What this item must not disturb

The top app bar is **the single application masthead** §2.3 requires. `EditorialHeader` renders a *screen*
header — eyebrow, screen title, description — not a second masthead, so restyling the app bar creates no
conflict with it.

`BottomNavigationBar` carries the Read Later and History **counts**, which must stay immediately truthful
(§3.5), and the destination order is fixed by §17 and §18.

---

## 2. Story

As **the reader**, I want the controls to look and respond like one designed set, in both schemes, so the
application feels made rather than assembled.

As **the implementer of 019 and 020**, I want the primary, tonal and triage controls to already exist, so
my screen work is layout rather than button design.

---

## 3. Out of scope

- **`ui/components/ArticleCard.kt`** (019) and **`ArticleRow.kt`, `EditorialHeader.kt`,
  `EmptyStatePanel.kt`, `StatBand.kt`** (020). Named individually because the wave brief implies otherwise.
- **`ui/screens/**`** in its entirety. 019 and 020.
- **Adopting the new shared controls into any existing component.** They ship unconsumed (§1.1).
- **Motion**, including the destination transition indexed to this item's bar. Item 021.
- **Any token, colour, radius, font or spacing value.** All arrive from 017 (§77.1). If one is missing,
  that is a report to the supervisor, not a literal.
- **Any behaviour**: no state transition, count, ranking, undo path, gesture or authored string.
- `UndoToast`, `ImportConfirmation`, `ResetConfirmation`.

---

## 4. Scenarios

### Scenario: the bottom bar keeps its destinations, order and counts
Given the bottom navigation bar
When it is rendered
Then the destinations are Read Later, Discover, History in that order
And Read Later and History display their counts
And the counts equal the current local state

### Scenario: the active destination is marked by the tonal container
Given a selected destination
When the bar is rendered
Then that destination's indicator is filled with the tonal role
And the indicator is not the primary fill and not primary ink

### Scenario: the active destination is not communicated by colour alone
Given a selected destination and an unselected one
When both are rendered
Then they differ by more than colour

### Scenario: all three destinations sit on one baseline
Given the bottom navigation bar
When the three destinations are measured
Then none is offset vertically from the others

### Scenario: every navigation target meets the floor
Given the bottom navigation bar
When each destination's touch target is measured
Then each is at least 48 dp in both dimensions
And the bar is at least 54 dp high

### Scenario: the app bar carries one centred masthead and a trailing settings control
Given the top app bar
When it is rendered
Then the application name is centred and set in the editorial register
And exactly one settings control is present
And the settings control's touch target is at least 48 dp in both dimensions
And the settings control is not a navigation destination

### Scenario: a category chip is a pill with a compliant target
Given the category chip row
When a chip is measured
Then its visible height is 40 dp
And its touch target is at least 48 dp in both dimensions

### Scenario: chip selection states differ by fill and by more than colour
Given a selected chip and an unselected chip
When both are rendered
Then the selected chip is filled with the primary role
And the unselected chip carries the control-boundary outline
And the two differ by more than colour

### Scenario: the unselected chip's outline clears the control-boundary floor
Given the light scheme and the dark scheme
When the unselected chip's outline is measured against the surface behind it
Then the contrast ratio is at least 3:1 in each scheme

### Scenario: the shared primary control is filled and 52 dp
Given the shared filled primary control
When it is measured
Then its height is 52 dp
And it is filled with the primary role and labelled with the on-primary role

### Scenario: the shared triage control is 56 dp with a 1.5 dp outline
Given a shared circular triage control
When it is measured
Then its size is 56 dp
And its outline is 1.5 dp in the secondary role
And it carries an accessible name

### Scenario: pressed and disabled behave as specified
Given any shared control
When it is pressed
Then a 12% overlay is applied and it scales to 0.95
And when it is disabled it is rendered at 38% opacity and is not interactive

### Scenario: no colour, radius or font is named outside the theme package
Given the source tree after this item
When the files this item touched are inspected for colour, radius and font literals
Then there are none

### Scenario: selecting a destination or a category changes nothing but what is shown
Given any destination or category selection
When it is made
Then no article status, count, preference signal or undo record changes

---

## 5. Verification

### 5.1 Gates

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

`android.yml` fires; `test.yml` fires on every PR regardless of paths.

**Definition-of-done diff check** — must print nothing:

```bash
git diff --name-only main... \
  | grep -vE '^android/app/src/(main/kotlin/io/irodriguez/intentionalreading/ui/(IntentionalReadingApp\.kt|components/(BottomNavigationBar|CategoryChipRow)\.kt|components/Controls.*\.kt)|test/|androidTest/)' \
  | grep -v '^specs/'
```

Adjust only for the actual filename chosen for the new shared controls.

### 5.2 What is assertable, and what is not

**Assertable:** destination order; counts; the tonal indicator role; target sizes; the 52 dp and 56 dp
dimensions; the 1.5 dp outline; **the chip outline's contrast ratio in both schemes, computed**; the 12% /
0.95 / 38% state values; accessible names; the absence of literals.

**Not assertable at the JVM layer:** whether the pill reads as an indicator rather than a highlight;
whether a 0.95 scale feels restrained rather than bouncy (§44's character constraint, §47); whether the
centred editorial masthead reads as a publication rather than as chrome. Walkthrough evidence.

**A vertical-offset assertion needs care.** §18.2 drops the browser's 7 px Discover lift. Asserting "no
destination is offset" is straightforward in a Compose UI test and near-impossible in a pure JVM test; if
it cannot be asserted at the JVM layer, it belongs in the walkthrough with a screenshot, not in a vacuous
unit test. Item 006's lesson applies — do not assert through a mechanism that cannot see the property.

### 5.3 On the existing assertion surface

Named with reasons, per `execution-model.md` §2.1 rule 5. Accurate against `main` at the time 017 merges.
**An unlisted failure must be reported before it is edited.**

| Test | Why this item reaches it | Expected |
|---|---|---|
| `AppViewModelTest` | asserts destination selection and counts as behaviour | untouched — this item changes no behaviour |
| any bottom-bar or chip test present at dispatch | asserts the components this item restyles | enumerate at preflight; report anything unlisted |

**This enumeration is deliberately thin, and that is the honest state.** 017 merges between this design and
this dispatch, and it adds theme tests. Read the tree at preflight rather than trusting this table.

### 5.4 Walkthrough — required evidence

1. All three destinations, in both schemes: is the active pill legible as an indicator?
2. The counts, after a save and after a mark-read: still immediately truthful?
3. The chip row on Discover, active and inactive, both schemes.
4. Press-and-hold a chip, the primary control and a triage control: is the response restrained?
5. Settings opens from the app bar and returns focus on close (§64).
6. At 360 dp: does the chip row still scroll, and does the bar still fit three destinations with counts?

And after each step: *and is what the reader needs next actually on screen?*

### 5.5 Owner checkpoints

A walkthrough look at this item's merge, per `wave-e.md` checkpoint 4.

### 5.6 Stop conditions

- A token this item needs was not defined by 017 — report, do not write a literal.
- The chip's control-boundary outline cannot reach 3:1 in a scheme.
- The 40 dp chip cannot carry a 48 dp target without breaking the row's layout at 360 dp.
- A test not listed in §5.3 fails.
- The diff check in §5.1 cannot be satisfied.

---

## 6. The gap this item does not close

**The three shared controls ship unconsumed.** `ArticleCard` and `ArticleRow` keep their inline buttons
until 019 and 020 adopt the shared ones. Between this merge and theirs, two treatments of the same control
exist in the tree.

**The destination transition is not animated here.** The bar this item finalises is what item 021 indexes
its directional motion to; until then destinations change without motion.

**The `dp` literals inside the files this item touches are migrated to 017's scales; the rest are not.**
Items 019 and 020 carry their own.
