# 028 — design

One page and two decisions, both settled by the owner.

## D1 — Borderless inline text controls, 4 dp padding (owner's sketch, made to fit)

**Chosen:** each triage control is a text control: an arrow and a short word on one line, with no outline,
no fill, a `secondary` label on `labelLarge`, 4 dp horizontal padding, a 4 dp arrow-to-word gap, and
`48×48dp` minimum bounds. Pressing it shows the shared pressed overlay (`SharedControlState`), clipped to
the `pill` shape.

**Why:** `spec.md` §1.3. It is the only form that keeps all three controls on one row at 360 dp without
touching the primary control. An outline needs the padding that tips the row over its width.

**The primary control and the gaps do not change.** If the row does not fit, the stop condition fires. The
fix is not to shrink `Read article` or the gaps silently.

**Implementation shape:** `CircularTriageControl` has exactly one caller, `ArticleActions`. It is replaced,
not kept beside the new control, by `InlineTriageControl` in `SharedControls.kt`. The new control takes
`accessibleName`, `onClick`, `modifier`, `enabled` and a `RowScope` content slot. `triageSize` and
`triageOutlineWidth` leave `SharedControlLayout`, and `triageOutline` is renamed `triageLabel`, still bound
to `tokens.secondary`. `triageAccessibleName` and `sharedControlState` are reused unchanged.

## D2 — One announced name, which begins with the visible word (owner, 2026-10-01)

**Chosen:** dismiss announces *"Skip, not interested"* and save announces *"Save for later"*. The control's
semantics are `clearAndSetSemantics { contentDescription = name; role = Button; onClick }`, or an
equivalent, so TalkBack reads the name once. It does not read "left arrow" or repeat the word.

**Why:** a voice-control user says the word they see. A name that does not contain *"Skip"* cannot be
reached that way, and *"Save for later"* already begins with *"Save"*. Clearing child semantics stops the
glyph `←` from being read aloud.

**New strings:** `skip` (`Skip`), `save` (`Save`) and `skip_not_interested` (`Skip, not interested`).
`not_interested` and `save_for_later` stay: the swipe cue uses both (`ArticleCard.kt:376-385`), and
`save_for_later` remains the save control's name.
