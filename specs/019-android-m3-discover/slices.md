# 019 — slice plan

Three slices, strictly sequential. Runs concurrently with item 020, which shares no file with it.

**Cut from `main` only after 018 has merged.**

---

## Fixed for this item — do not re-decide these mid-implementation

1. **Amendment 7's ordering.** Masthead, then card, then operational block. Item 012's tests are the guard
   and must stay green (`design.md` D3).
2. **The clamps are 3 lines and 2 lines** (§13.2). Not a judgement call.
3. **No image area, no placeholder, no reserved height** (D4, §74.2).
4. **Adopt 018's controls; do not restyle the inline ones** (D2).
5. **No swipe behaviour changes.** Threshold, cues, commitment sequence and undo are §39–§43's and §70's.
6. **No new user-facing string.** §75.2. If one seems necessary, report.
7. **No value is chosen here.** All tokens from 017.

---

## Slice 1: the deck card's surface, shape and type

**Objective.** 24 dp radius, the card surface, the tertiary-tinted ambient shadow, the Playfair headline as
the dominant element, and both clamps.

- **Scenarios:** the headline clamps at three lines; the description clamps at two lines; an empty excerpt
  is omitted, not filled; the card is text-first with no reserved image area; the badge shows the
  authoritative content-type label; reading time and publication age are omitted when unknown; tags are
  neutral, capped, and not controls.
- **Files:** `ui/components/ArticleCard.kt` and its tests.
- **Must not touch:** the action rail's controls (slice 2), `ui/screens/discover/**` (slice 3).
- **Reaches green alone because:** every change is inside one composable's body with no signature change, so
  `DiscoverScreen.kt` compiles untouched; the excerpt test that asserts four lines is updated **in this
  slice**, alongside the change that breaks it.
- **Definition of done:** both gates green; both clamps asserted on the text elements that carry them; no
  literals; item 012's tests still green.
- **Status:** **done** — RED `6649a19`, GREEN `7d245ff`. Gate reproduced independently with
  `--rerun-tasks`: **355 tests, 0 failures, 0 errors**, `assembleDebug` and `assembleDebugAndroidTest` both
  successful (baseline 343). Slice review PASS.

**Genuine value-failing RED**, reproduced as `355 tests completed, 8 failed`, including the two that matter:
`excerpt maxLines expected:<2> but was:<4>` and `headline maxLines expected:<3>`. Both clamps are applied via
named constants with ellipsis, asserted on the text elements that carry them.

**The literal scan is scoped to the four composables this slice owns** — `ArticleCard`, `ArticleMetadata`,
`MetadataText`, `TopicTags` — which is honest scoping rather than a whole-file scan that would have forced
this slice into a later one's ground.

### A gap in this slice plan, found at review — for slice 2

Four `RoundedCornerShape(10.dp)` literals remain in `ArticleCard.kt`. **10 dp is §15.1's *browser* control
radius**, not anything Android's scale defines.

| Line | Composable | Owned by |
|---|---|---|
| 474, 496 | `ArticleActions` | **slice 2** — the action rail |
| 305 | `SwipeCue` | **nobody** |
| 423 | `OpenedAcknowledgment` | **nobody** |

`SwipeCue` and `OpenedAcknowledgment` are named in no slice of this plan. As written, item 019 would ship
with browser radii on two Android surfaces.

**Slice 2 is extended to cover both.** They live in the file it already edits, they are presentational, and
neither carries behaviour this item may touch — `SwipeCue` renders §41/§42's directional cue and
`OpenedAcknowledgment` renders §51's return-state acknowledgement. **Restyle them; do not change what either
says or when it appears.**

---

## Slice 2: the action rail adopts 018's shared controls

**Objective.** Replace the three inline treatments with 018's filled primary and circular triage controls.

- **Scenarios:** the action rail uses the shared controls; the triage controls keep compliant targets; swipe
  behaviour is unchanged.
- **Files:** `ui/components/ArticleCard.kt` (rail only) and its tests.
- **Must not touch:** 018's control definitions — if one needs changing, that is a report, not an edit.
- **Reaches green alone because:** the three actions keep their callbacks and their semantics; only the
  composables drawing them change.
- **Definition of done:** both gates green; all three actions present and operable without a gesture; targets
  ≥ 48 dp; accessible names intact; the swipe tests still green.
- **Status:** **done** — RED `9d2797d`, GREEN `dd0b680`. Gate reproduced independently: **359 tests, 0
  failures, 0 errors**, `assembleDebug` and `assembleDebugAndroidTest` successful (baseline 355). Slice
  review PASS.

RED reproduced as `359 tests completed, 5 failed`, including `circular triage control count expected:<2>`
and the literal scan. All three rail actions now use item 018's shared controls, and
`OpenedAcknowledgment`'s §51 action uses `TonalSecondaryControl`.

**The two orphaned composables are closed.** `ArticleCard.kt` now carries **no radius, colour, size or font
literal anywhere in the file** — the literal scan was widened from slice 1's four owned composables to the
whole file and asserts it. `SwipeCue` and `OpenedAcknowledgment` are restyled with their text, timing and
offered choices untouched.

`SharedControls.kt` is unmodified and no gesture test was edited.

**Read 018's actual signatures at preflight.** This plan deliberately does not name them (D2).

---

## Slice 3: the Discover screen and its operational block

**Objective.** Re-lay out the screen around the new card — masthead, card, operational block — on 017's
spacing and shape, with Amendment 7 intact.

- **Scenarios:** the card still leads the viewport; the non-card states keep their composition; nothing
  outside the theme names a value.
- **Files:** `ui/screens/discover/DiscoverScreen.kt`, `DiscoverHeader.kt`, and tests.
- **Must not touch:** `components/CategoryChipRow.kt` (018's — this slice calls it), `ArticleCard.kt`.
- **Reaches green alone because:** the screen's inputs and callbacks are unchanged; this is a re-layout
  within existing signatures, and `DiscoverScrollTargetsTest` continues to cover the scroll arithmetic.
- **Definition of done:** both gates green; `DiscoverScrollTargetsTest` green **and unedited**; empty, loading
  and failed-dataset states each still truthful with a route onward; **plus the two instrumented tests
  below.**

### Amended at dispatch — this slice now writes the guard, rather than inheriting it

Assumption 3 was wrong: **no composition-order test exists.** Item 012 deliberately did not write one because
composition order is unobservable in JVM tests and the instrumented source set was out of CI. **PR #32 removed
that second constraint**, so this slice must close the gap it was designed to lean on:

1. **An instrumented test asserting Amendment 7's ordering** — the masthead precedes the card, and the card
   precedes the operational block, by their `boundsInRoot.top`. This is the first automated guard Amendment 7
   has ever had, and it is being written by the item most likely to break it.
2. **An instrumented test asserting the whole card fits above the fold at 360 dp** — headline, excerpt, tags
   and all three action controls, with the longest real dataset title. `spec.md` §5.2 called this
   *"assertable only by measurement on a device"* and assigned it to the walkthrough. **It is now gateable**,
   and it is the scenario that closes item 012 §1.4.

**Both must establish their own width, not inherit it** — `DeviceConfigurationOverride.ForcedSize`, with
every `dp.toPx()` baseline computed **inside** the override. `execution-model.md` §8.3 records why, and what
it costs to get wrong.

The walkthrough screenshot is still captured — it is evidence a reader would recognise, and §9's *"is what
the reader needs next actually on screen?"* is not a question a test answers.
- **Status:** **done** — RED `657ecdc` + `c276bdb`, GREEN `38b3272`. Gate reproduced independently:
  **362 unit tests, 0 failures**, `assembleDebug` and `assembleDebugAndroidTest` successful (baseline 359).
  **Instrumented suite run by the reviewer: 8 tests, 0 failed.** Slice review PASS.

### Item 012 §1.4 is closed, and it is now gated

**Both guards exist and assert what they claim.** Guard 1 asserts `masthead.top < card.top` and
`card.top < operationalBlock.top` — **the first automated assertion Amendment 7's ordering has ever had.**
Guard 2 (`longDatasetCardFitsAboveTheFoldAt360Dp`) forces 360 dp, locates all three rail controls by content
description, and asserts their bounds within the viewport. Verified passing at forced 360 dp, at 411 dp, and
with the emulator physically narrowed to 320 dp. Every `dp.toPx()` baseline is inside its `ForcedSize`
override, per `execution-model.md` §8.3.

`walkthrough/item019-fold-closed-360dp.png` is the visual proof: the headline clamped at three lines with
ellipsis, the excerpt at two, and **all three action controls above the bottom bar** — where item 017's
walkthrough had a five-line headline and `Read article` cut in half.

### One thing raised for the owner rather than decided here

**The triage controls lost their visible text labels.** They previously carried `Text` labels beneath the
circles; now *"Not interested"* and *"Save for later"* travel only as `accessibleName` content descriptions,
so a sighted reader sees bare `←` and `→` arrows.

**This is authorised** — §76.5 says *"icon-only controls carry accessible names (§73)"* — and the accessible
names are present and asserted. But §35's *"must not replace the labelled semantic understanding of the
action"* reads against it, and it is a material change to what a sighted reader sees.

It is also **plausibly load-bearing for the fold fix**: the labels cost vertical space at exactly the width
that was tight. *Whether the rail would still fit at 360 dp with them restored is unmeasured* — worth
measuring if the owner wants them back.

**An owner visual judgment at the walkthrough, not a review finding.**

---

## Assumptions, each checkable at dispatch

1. **017 and 018 have both merged.** This item consumes 017's tokens and 018's controls.
2. **`ArticleCard.kt:210` still clamps the excerpt at 4 and the title still has no clamp.** If the title is
   already clamped, report before changing it — something else moved.
3. **CORRECTED AT DISPATCH — item 012 has a scroll-target test and *no* composition-order test.**
   `DiscoverScrollTargetsTest` covers the scroll arithmetic and must stay green. **There is no ordering
   assertion anywhere**, and item 012 says why in its `spec.md` §5.2: *"Composition order is not observable
   in `testDebugUnitTest`. There is no composition, and the instrumented source set is out of CI."*

   **Both halves of that reason still held when this item was designed. The second no longer does.** PR #32
   put the instrumented source set into CI — it now compiles **and runs** on a pinned 411 dp emulator.

   So Amendment 7's ordering has never had an automated guard, and this item — the one most able to break it
   — is the first that can build one. See the amended slice 3.
4. **No dataset article requires a sixth tag or a longer badge label** than the card can carry at 360 dp.
5. **018's shared controls expose the callbacks this rail needs.** If not, report — do not edit 018's files.

---

## On existing assertions

**Two items merge beneath this one between design and dispatch.** `spec.md` §5.3 names four cases with
reasons and is **not a freeze**.

Per `execution-model.md` §2.1 rule 5: read the tree at preflight, and **report any unlisted failure before
editing it.** This is exactly the situation that made item 016's D5 stale — accurate when written, stale
when used, because another item merged in between.
