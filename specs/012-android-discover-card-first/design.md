# 012 — design note

Companion to `spec.md`. Four decisions: how the header is split, what happens to the three scroll effects,
how a layout item gets a real failing-first test, and what Amendment 7 must say so wave E inherits a rule.

---

## D1 — Split the header into two composables, both owned by Discover

`EditorialHeader.kt` holds two overloads. The general one — eyebrow, title, description, optional action,
`supportingContent` slot — is shared with Read Later and History and **does not change**. The
Discover-specific one (`:22-79`) assembles the whole header from the operational state and is used by
`DiscoverScreen` alone.

That Discover-specific overload is replaced by two composables in a new file,
`ui/screens/discover/DiscoverHeader.kt`:

- `DiscoverMasthead()` — eyebrow and screen title. Nothing else, and no action button.
- `DiscoverOperationalBar(...)` — purpose copy, refresh affordance, content-age line, failed-refresh
  disclosure, degraded notice, available-article context, category chip row. Same parameters the old
  overload took, same strings, same styles, same order among themselves.

`DiscoverScreen`'s column then reads masthead → state body → operational bar, and the removed overload
leaves `EditorialHeader.kt` carrying only the shared general form. Moving the Discover assembly *out* of a
shared component file is worth the extra file: after this item, nothing in `ui/components/` knows about
Discover's operational state, which is one less thing for wave E to untangle.

**Rejected:** keeping the composables inline in `DiscoverScreen.kt`. It works and it is a smaller diff, but
`DiscoverScreen.kt` is already 241 lines with three scroll effects in it, and wave E will re-lay out both
halves separately.

**Rejected:** declaring the section order as data (an enum plus a render loop) so a JVM test could assert
it. It buys an assertion whose subject is the declaration rather than the screen — the shape of wave C's
vacuous-assertion defect, with more indirection. D3 gets a real assertion out of this item instead.

## D2 — The three scroll effects: one stays as is, one stays and loses its job, one must be re-aimed

| Effect | Today | After |
|---|---|---|
| `:71-73` category/state change → `scrollTo(0)` | returns the reader to the top | **unchanged.** Still correct, and now lands them on the card. |
| `:74-87` head-article change → scroll to `cardTopOffset` (008's D12) | pulls the incoming card into view, clamping at the content maximum | **unchanged code, smaller job.** `cardTopOffset` is now small. Kept because a large accessibility font scale can still push the card down (`spec.md` §1.3), and because deleting a working effect is not this item's business. |
| `:91-103` head article became opened → `scrollTo(scrollState.maxValue)` | reveals the **Mark read** button, which is at the bottom of the card and, today, at the bottom of the content | **must be re-aimed.** The content maximum is now the bottom of the chip row. Scrolling there puts Mark read back off screen and re-opens the wave B defect. |

The third one is the substance of this item beyond the reorder, and it is why `spec.md` §4 carries a
scenario for it.

## D3 — The re-aimed scroll is extracted as a pure function, and that is where the failing-first test lives

A layout item has nothing for `testDebugUnitTest` to observe. This one does, once the scroll target stops
being the literal `scrollState.maxValue`:

```kotlin
// ui/screens/discover/DiscoverScrollTargets.kt
object DiscoverScrollTargets {
    /**
     * The scroll value that brings the bottom of the Discover card — and therefore its action rail —
     * into view without scrolling past it into whatever follows the card.
     */
    fun revealCardActions(cardBottomOffset: Int, viewportHeight: Int, maxValue: Int): Int =
        (cardBottomOffset - viewportHeight).coerceAtLeast(0).coerceAtMost(maxValue)
}
```

`DiscoverScreen` captures `cardBottomOffset` from the same `onGloballyPositioned` that already supplies
`cardTopOffset` (`:146-148`) — `positionInParent().y + coordinates.size.height` — and takes the viewport
from `scrollState.viewportSize`. If that property is not present in the pinned Compose BOM
(`2026.08.00`), wrap the scrolling column in `BoxWithConstraints` and pass the constraint height; report
which was used.

**This is a genuine assertion, not a restatement of the code.** It has cases with different answers: a card
that already fits the viewport must yield `0`, a card taller than the viewport must yield exactly the offset
that puts its bottom edge at the bottom of the viewport, a target beyond the content must clamp to
`maxValue`, and a card whose bottom sits above the current viewport must not scroll backwards. Today's
behaviour — `maxValue` — is wrong for the first, second and fourth of those.

The failing-first commit for it is a compile failure, because the function is new. **Say so plainly in the
commit message rather than dressing it up:** the RED is `DiscoverScrollTargetsTest` not compiling, and the
behavioural proof that the old target was wrong is `spec.md` §5.3 step 3. This is the honest shape for a new
pure function and it is the strongest RED available at this layer.

## D4 — Amendment 7 states intent, not widget order

`waves/wave-d.md` is explicit that wave E rewrites whatever this item writes, so the amendment has to
survive being re-laid-out. It therefore binds one sentence — *on Discover, the first thing in the viewport
is the article card, not the controls that describe it* — and marks the widget list illustrative.

Consequences deliberately taken:

- The masthead is reduced on Discover only. `06-ui-ux.md` §21's *"publication masthead/content framing
  rather than dashboard chrome"* is preserved: an eyebrow above a display-scale title is the masthead; the
  purpose copy is the framing, and framing below the subject is still framing.
- Read Later and History keep the full three-part editorial header. §21 is Discover's section, so nothing
  else is touched.
- No contract, no state, no copy and no learning behaviour changes, so `contracts.md` is not in the
  amendment's affected-document list.

## D5 — What this item is not allowed to fix while it is in there

`DiscoverScreen.kt` is the file where wave B's *deck advancing returned the screen to the top* was fixed and
where 013's investigation spent two passes. Both are settled. This item reorders a column and re-aims one
scroll target. It does not touch the gesture path, the deck, the `heldArticleId` behaviour, or the two
effects D2 leaves alone — and it does not touch `AppViewModel.kt`, which is item 015's ground for the
duration of the wave.
