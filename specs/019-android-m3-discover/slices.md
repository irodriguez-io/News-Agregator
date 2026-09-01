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
- **Status:** pending

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
- **Status:** pending

**Read 018's actual signatures at preflight.** This plan deliberately does not name them (D2).

---

## Slice 3: the Discover screen and its operational block

**Objective.** Re-lay out the screen around the new card — masthead, card, operational block — on 017's
spacing and shape, with Amendment 7 intact.

- **Scenarios:** the card still leads the viewport; the non-card states keep their composition; nothing
  outside the theme names a value.
- **Files:** `ui/screens/discover/DiscoverScreen.kt`, `DiscoverHeader.kt`, and tests.
- **Must not touch:** `components/CategoryChipRow.kt` (018's — this slice calls it), `ArticleCard.kt`.
- **Reaches green alone because:** item 012's composition-order tests are the acceptance criteria for this
  slice and already exist; the slice is done when they are still green against the new layout.
- **Definition of done:** both gates green; 012's ordering and scroll-target tests green; empty, loading and
  failed-dataset states each still truthful with a route onward; the 360 dp walkthrough screenshot captured.
- **Status:** pending

---

## Assumptions, each checkable at dispatch

1. **017 and 018 have both merged.** This item consumes 017's tokens and 018's controls.
2. **`ArticleCard.kt:210` still clamps the excerpt at 4 and the title still has no clamp.** If the title is
   already clamped, report before changing it — something else moved.
3. **Item 012's composition-order and scroll-target tests still exist and still pass.** They are this
   item's guard rail; if they are gone, stop and report.
4. **No dataset article requires a sixth tag or a longer badge label** than the card can carry at 360 dp.
5. **018's shared controls expose the callbacks this rail needs.** If not, report — do not edit 018's files.

---

## On existing assertions

**Two items merge beneath this one between design and dispatch.** `spec.md` §5.3 names four cases with
reasons and is **not a freeze**.

Per `execution-model.md` §2.1 rule 5: read the tree at preflight, and **report any unlisted failure before
editing it.** This is exactly the situation that made item 016's D5 stale — accurate when written, stale
when used, because another item merged in between.
