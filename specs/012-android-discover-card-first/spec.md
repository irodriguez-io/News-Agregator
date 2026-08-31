# 012 — The Discover card leads the viewport

**Status:** draft (awaiting plan gate)\
**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/012-android-discover-card-first/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`. **`docs/v1/**` is amended for this item by Amendment 7, which is
written and committed before the item branch is cut** — the implementer does not edit `docs/v1/**`.\
**Authority:** `docs/v1/06-ui-ux.md` §§21/22/23/24/48/71, `docs/v1/README.md` Amendment 6 and
**Amendment 7**\
**Wave:** D (`specs/waves/wave-d.md`), concurrent with 015 · **Branch:** `feat/012-android-discover-card-first` → `main`\
**Cut from:** `main`, at the merge of PR #19. Record the SHA in `evidence.md` at branch creation.

---

## 1. Why this item exists

The owner, after testing wave B's build on 2026-08-26, framed it precisely: **the card should be the first
thing in the viewport, not the thing you scroll to.**

Discover renders one scrolling column (`ui/screens/discover/DiscoverScreen.kt:104-151`) whose first child
is the full editorial header — eyebrow, display title, purpose copy, refresh affordance, content-age line,
failed-refresh disclosure, degraded notice, available-article context, and the category chip row
(`ui/components/EditorialHeader.kt:22-79`). On a 360 × 640 dp device that block runs to roughly 160 dp
before the card starts, and the card's action rail is the last thing in it. The reader opens the app and
scrolls to reach the decision.

### 1.1 What moves, and what the owner decided about the masthead

Moving only the operational widgets below the card leaves eyebrow + display title + purpose copy above it —
still enough to push the card's action rail below the fold on the narrowest supported width. The owner
chose, on 2026-08-31, to **compact the masthead**: the eyebrow and the screen title stay on top, and the
purpose copy moves below the card with the operational block.

| Position | Contents |
|---|---|
| **Above the card** | eyebrow (`discover_eyebrow`), screen title (`discover`) |
| **The card** | `ArticleCard` and the remaining-choices side note |
| **Below the card** | purpose copy (`discover_description`), refresh affordance, content-age line, failed-refresh disclosure, degraded notice, available-article context, category chip row |

### 1.2 It needs Amendment 7, and it is narrow

`06-ui-ux.md` §21 says *"Discover begins with an editorial header area"* — an ordering rule — and then
lists the category selector and the available-article context among what the header *may* include. So the
widgets are already permitted to be there or not; what changes is the order. §23 (Discover Composition)
constrains the card itself and says nothing about order, and it is not amended.

**Wave E rewrites this layout wholesale, and the owner chose to take the fix now**
(`waves/wave-d.md`, 2026-08-31). Amendment 7 therefore states the **intent** and marks the widget order
illustrative, so wave E inherits a rule rather than a layout.

### 1.3 What it subsumes, and the one thing it must not break

**It largely subsumes item 008's D12.** That fix scrolls the incoming card into view after a swipe
(`DiscoverScreen.kt:74-87`) and currently clamps at the content maximum rather than placing the card at the
top, because Discover's content is shorter than the scroll that would require. With the operational block
below the card there is little left for it to correct. **The effect stays anyway** — at large accessibility
font scales the masthead can still push the card down, and deleting a working effect is not this item's
business.

**The became-opened scroll is a regression risk and this item owns it.** Wave B found *Mark read hidden
after returning from the publisher* — the owner found it by using the app, and no gate saw it
(`waves/wave-b-note.md` §9). The fix was `DiscoverScreen.kt:91-103`: when the head article becomes opened,
scroll to `scrollState.maxValue` so the newly revealed **Mark read** button at the bottom of the card is on
screen. Today the card is the last thing in the column, so `maxValue` lands on it. **After this item, the
content maximum is the bottom of the category chip row, and scrolling there scrolls the Mark read button
back off the screen.** Reordering without re-aiming that scroll re-opens a defect the owner personally
reported.

---

## 2. Story

As a **reader**, I want the article card to be the first thing I see on Discover, so that the decision is
in front of me when I open the app and the controls that describe it sit out of the way beneath it.

---

## 3. Out of scope

- **Any behaviour change.** No state transition, no action, no announcement, no count, no copy text, and no
  learning signal changes. The same widgets render the same strings from the same state; only their
  position changes.
- **Read Later, History, Settings and the top or bottom bars.** `EditorialHeader`'s general three-argument
  overload is shared with Read Later and History (`ui/screens/readlater/ReadLaterScreen.kt:42-45`,
  `ui/screens/history/HistoryScreen.kt`) and **its behaviour must not change**.
- **Deleting either of the other two scroll effects** (`DiscoverScreen.kt:71-73`, `:74-87`). §1.3.
- **The card's own anatomy and action rail** (`06-ui-ux.md` §24, §31) and `ui/components/ArticleCard.kt`.
- **Wave E's redesign.** No new tokens, type, radii, motion or chip styling. This item moves existing
  composables and adds no visual language.
- **`docs/v1/**`.** Amendment 7 is written by the orchestrator before the branch is cut; the implementer
  must not edit `docs/v1/**` (`AGENTS.md`; `docs/v1/README.md` §14).
- **The undo work.** Items 014, 015 and 016. This item touches nothing on the Undo path.

---

## 4. Scenarios

### Scenario: the card leads the viewport

Given a reader opens Discover on a 360 dp-wide device with a card available\
When the screen first renders and before any scrolling\
Then the article card and its full action rail are visible\
And only the eyebrow and the screen title appear above the card

### Scenario: the operational block follows the card

Given Discover is showing a card\
When the reader scrolls down\
Then the purpose copy, the refresh affordance, the content-age line, the available-article context and the
category selector appear below the card, in that order\
And each shows the same text it shows today

### Scenario: the Mark read button is on screen after returning from the publisher

Given the reader taps **Read article** and returns from the publisher\
And the head article has become opened, so the card now offers **Mark read**\
When the screen settles\
Then the **Mark read** button is visible without the reader scrolling\
And the scroll does not continue past the card to the category selector

### Scenario: a category change returns the reader to the top

Given the reader has scrolled down to the category selector\
When they select a different category\
Then the screen returns to the top and the card is again the first thing in the viewport

### Scenario: the non-card states keep their composition

Given Discover has no eligible article, or is loading, or failed to load\
When the screen renders\
Then the masthead is above the panel, the operational block is below it\
And the panel's title, copy and action are unchanged

### Scenario: the failed-refresh disclosure is still reachable

Given a refresh has failed\
When the reader looks for the disclosure\
Then it is present below the card with the text it has today\
And the degraded notice, when it applies, is present with it

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

Delete `test-results` first and read the `BUILD SUCCESSFUL` line, not the counts. Record the count at the
moment of the run.

### 5.2 What is assertable at the JVM layer, and what is not

**Composition order is not observable in `testDebugUnitTest`.** There is no composition, and the
instrumented source set is out of CI (002 slice 4). So:

- **Assertable, and slice 1's failing-first test:** the scroll target that reveals the card's action rail.
  It is arithmetic over offsets and it is extracted as a pure function precisely so that it has an
  observer (`design.md` D3).
- **Not assertable, and enforced by §5.3 instead:** that the widgets are in the stated order, that the card
  fits the first viewport, and that the reader sees what §4 describes. This is written down rather than
  closed with an assertion that cannot see it — wave C shipped a DoD bullet demanding proof from a layer
  with no observer and got a vacuous assertion in return (`waves/wave-c-note.md` §2).

The existing suite must stay green unedited. `UiStateMapperTest` asserts the *content* of the header's
strings, not their placement, so it is unaffected; if a case does move, report it.

### 5.3 Walkthrough — required evidence, and the definition of done

Driven over `adb` on the `Pixel_10`. `06-ui-ux.md` §71 requires verification at 360, 390 and 430 logical
widths; drive the two extremes at minimum and reset the device afterwards.

```sh
adb shell wm size 1080x2400 && adb shell wm density 420   # ≈ 411 dp wide
adb shell wm size 1080x1920 && adb shell wm density 480   # ≈ 360 dp wide
# and afterwards, without fail:
adb shell wm size reset && adb shell wm density reset
```

1. **Cold open, no scrolling, at 360 dp and at 411 dp.** `screencap` each. The card and its three action
   controls must be fully visible. Describe what a reader would see before checking the assertion
   (`execution-model.md` §9).
2. **Scroll down.** The operational block is in §1.1's order and reads the same text as today.
3. **Tap Read article, return, and settle.** `screencap`. The **Mark read** button must be on screen. This
   is the wave B defect and it is the step most likely to fail.
4. **Select a category, then another.** The screen returns to the top each time, and the card leads.
   The chip row scrolls and chip coordinates go stale after anything that moves the page — re-locate before
   every tap (`waves/wave-d.md`).
5. **Swipe to advance the deck.** The incoming card is in the viewport with its action rail, and the
   reader does not have to scroll to act again.
6. **Force a failed refresh** (airplane mode, then tap Refresh). The disclosure appears below the card.
7. **Enable the largest system font scale** and repeat step 1. The card may now start below the fold; record
   what the reader sees rather than asserting a pass. This is a §5.4 judgment call, not a gate.

Each step carries the second question: *and is what the reader needs next actually on screen?*

### 5.4 Owner checkpoints

- **The placement rule as drafted in Amendment 7** — that the intent is stated in a form wave E can inherit,
  and that dropping the purpose copy from above the card reads acceptably (`waves/wave-d.md` checkpoint 2).
- **Step 7's largest-font behaviour**, if the card no longer leads at that scale.

### 5.5 Stop conditions

Stop and report rather than proceeding if: step 3's **Mark read** button is off screen after the reorder;
any existing test needs editing; the reorder requires a change to `ArticleCard.kt` or to the shared
`EditorialHeader` overload used by Read Later and History; or the operational block cannot be assembled
without authoring new copy.
