# 028 — The triage controls say what they do

**Surface:** Android only, the Discover card's action rail (`ui/components/ArticleCard.kt`,
`ui/components/SharedControls.kt`).
**Authority:** `docs/v1/06-ui-ux.md` §31, §35, §72.2, §73, §76.5 (second edition); **Amendment 14, authored by
this item**; `specs/backlog.md` Road to done row 5.
**Branch:** `feat/028-android-triage-inline-labels`, cut from `main` at `8602be0`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 The finding

Item 019 replaced the triage controls' visible labels with icon-only `56dp` outlined circles, so a sighted
reader sees bare `←` and `→` (`specs/019-android-m3-discover/evidence.md` §7). §76.5 allowed it; §35's
*"must not replace the labelled semantic understanding of the action"* reads against it. 019 raised it for
the owner; it was deferred to wave close and became Road to done row 5.

### 1.2 The owner's decision, 2026-10-01

**Restore the text, on the same line as the arrow, in one row with the primary control:**
`← Skip` · `Read article ↗` · `Save →`. Short visible words, because the full names do not fit (§1.3).
Each accessible name **contains its visible word**, so a reader using voice control can name the control by
what they see. Dismiss's name becomes *"Skip, not interested"*; save's stays *"Save for later"*.

**Rejected at the same decision:** keeping the icons only; two rows with full names (the extra row costs
about 48 dp and would very likely undo 019's 360 dp fold fix); a long-press tooltip (undiscoverable, and
new behaviour); keeping dismiss's name as *"Not interested"* (voice control could not reach it by *"Skip"*).

### 1.3 Fit at 360 dp: measured before design

Widths are measured from the bundled `res/font/roboto_flex_variable.ttf` at `labelLarge` (14 sp, SemiBold,
0.02 em). The card's content width at 360 dp is **≈287 dp**, measured from
`specs/019-android-m3-discover/walkthrough/item019-fold-closed-360dp.png`.

| Element | Width |
|---|---|
| `Read article ↗`: 79.9 text + 4 spacer + ≈10 arrow (fallback font) + 2 × 24 Button padding | **≈142 dp** |
| `← Skip`: 12.1 + 4 + 29.2, plus 2 × 4 padding | ≈53 dp |
| `Save →`: 32.3 + 4 + 12.1, plus 2 × 4 padding | ≈56 dp |
| two `stackGap` gaps | 24 dp |

- **Borderless, 4 dp padding:** `Read article` gets 287 − 53 − 56 − 24 = **≈154 dp against ≈142 needed,
  about 11 dp spare.** It fits with no change to the primary control or the gaps.
- **With M3's default 12 dp text-button padding, or an outline needing that padding:** the row needs
  ≈307 dp. **Does not fit.**
- **Full names (`Not interested`, `Save for later`):** each control alone runs to ≈130–165 dp. Does not fit.

These are estimates, because `↗` falls back to another font. The gate is the measurement: §4 asserts that no
label wraps or overflows at 360 dp.

---

## 2. Story

As a **reader**, I want the card's triage controls to say what they do, so that I can decide without
learning what the arrows mean.

---

## 3. Amendment 14, authored by this item and approved at the plan gate

Recorded in `docs/v1/README.md`. Edits: §35 (shared bullets; §35.1; §35.2 rewritten), §31, §76.5, §72.2,
§73.1, §77.2, §78.3, §80 row 32–35, and `09-testing-acceptance.md` §72.1. **Android only.** The authored
strings `Skip`, `Save` and `Skip, not interested` are authorized. No other behaviour changes: actions,
callbacks, swipe semantics, the swipe cue's text, undo text, keyboard bindings and §32's primary control
all stay as they are.

---

## 4. Scenarios

### Scenario: the reader sees what each triage control does
Given Discover shows an article card
Then the rail shows, on one row and in this order, `← Skip`, `Read article ↗` and `Save →`
And `Skip` and `Save` are drawn in the `secondary` colour with no outline

### Scenario: the row fits the narrowest supported width
Given a 360 dp handset showing the long dataset card of 019's fold gate
Then `Skip`, `Save` and `Read article` each lay out on one line with no visual overflow
And all three controls lie inside the viewport above the fold
And the same holds at 411 dp

### Scenario: each control announces one name, beginning with its visible word
Given TalkBack is on
When focus reaches the dismiss control
Then it announces "Skip, not interested", once, and not the arrow
When focus reaches the save control
Then it announces "Save for later", once, and not the arrow

### Scenario: the targets stay at least 48 dp
Then each triage control's bounds are at least 48 × 48 dp

### Scenario: the controls still do what they did
When the reader taps `Skip`
Then the article is dismissed exactly as before, with the same undo offer
When the reader taps `Save`
Then the article is saved exactly as before, with the same undo offer

### Scenario: swiping is unchanged
Given the reader drags the card
Then the swipe cue still reads "← Not interested" and "Save for later →"
And every swipe test from items 008, 013, 015, 023 and 024 passes unedited

### Scenario: an unusable name is still refused
Given a triage control is given a blank accessible name
Then it fails fast, as `triageAccessibleName` does today

---

## 5. Out of scope

- The primary control (`FilledPrimaryControl`, §32), its padding, and the rail's gaps.
- The swipe cue (`ArticleCard.kt:376-385`), the undo toast text, Read Later and History row actions.
- Font-scale behaviour above 1.0. No section of `docs/v1/**` requires it, and the primary control already
  wraps under large font scales today.
- Long arrow tails (`←──`): they do not fit in the 11 dp spare, and `─` falls back to another font.
- The browser runtime.

---

## 6. Verification

### 6.1 Gates

With `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
`ANDROID_HOME=$HOME/Library/Android/sdk` exported, from `android/`:
`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.
Baseline **404 JVM / 30 instrumented**; the slice reports the counts after.

### 6.2 Screenshots, orchestrator

Discover at 360 dp and 411 dp, light and dark, recorded in `evidence.md`.

### 6.3 TalkBack spot check, orchestrator

Each triage control is announced once, with the names in §4.

### 6.4 Owner walkthrough on a signed release build

Check that `animator_duration_scale` reads `1`. The owner judges whether the borderless `← Skip` / `Save →`
reads as tappable and whether the row still reads as one primary action with two secondary ones (§32
*"only one visually dominant primary action"*). Uninstall the release build afterwards.
