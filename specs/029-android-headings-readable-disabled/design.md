# 029 — design note

Short, because nothing here is new structure. It is one semantics flag and one colour argument.

## D1. Headings use Compose's `heading()` semantics on the title `Text` itself

`Modifier.semantics { heading() }` sets the accessibility node's *is heading* flag, which TalkBack's
*Headings* navigation reads. It goes on the title `Text`, not on a parent container, so it marks exactly
the title. **No card or row merges its descendants** (`ArticleCard.kt` and `ArticleRow.kt` have no
`mergeDescendants`; checked 2026-10-01), so the flag cannot spread to a whole card.

The two shared components, `EditorialHeader` (Read Later and History titles) and `EmptyStatePanel`
(both empty states), and the shared `ArticleRow` (Read Later and History rows), get the flag once each and
cover both screens.

## D2. Readable disabled text uses `muted`, the app's existing disabled-label colour

Both controls are `OutlinedButton`s. They gain `disabledContentColor = tokens.muted` in
`ButtonDefaults.outlinedButtonColors(...)` and change nothing else.

- **Precedent:** `ResetConfirmation.kt:64,80` and `ImportConfirmation.kt:65,81` already draw disabled labels in
  `muted`. Reusing that colour means no new colour decision.
- **Contrast:** `muted` reaches 8.81:1 on `bg` and 8.12:1 on `container` in light, and 7.94:1 and 6.02:1
  in dark. All clear 4.5:1.
- **Still visibly disabled:** the outline keeps Material's disabled border (`onSurface` at 12%), which
  differs from the enabled `outlineControl`. The text moves from `fg` to `muted`. *"Refreshing…"* also
  says what is happening.

## D3. How the tests see the rendered result

- **Headings:** a Compose instrumented test reads `SemanticsProperties.Heading` from the semantics tree, the
  same tree TalkBack reads, not the source. Each screen gets two checks: the exact
  heading set (D1's list, nothing more), and that each listed title is a heading.
- **Colour:** `SemanticsActions.GetTextLayoutResult` returns the paragraph as laid out. Its
  `layoutInput.style.color` is the colour the text was drawn with, after the button's content colour is
  applied. The test computes the WCAG contrast against the background token in both schemes. `Tokens.kt:204`'s
  `contrastRatio` is private, so the test carries its own copy of the formula, as `CategoryChipRowTest.kt:131`
  already does. Production code is not widened for a test. The orchestrator's pixel
  measurement (`spec.md` §5.2) is the backstop.
