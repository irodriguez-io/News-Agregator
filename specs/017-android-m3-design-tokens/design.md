# 017 — design note

Seven decisions. The palette is not among them; it was settled before this item was designed.

---

## D1 — The token object grows. It is never replaced.

**Decision.** `IntentionalReadingTokens` keeps all thirteen existing field names with new values, and gains
the new roles alongside them. No consumer file is touched by this item.

**Why.** 17 files hold 205 token call sites. A rename is a 205-site mechanical diff landing inside a token
item, it breaks every consumer's compile at the slice boundary, and it is item 014's failure repeated —
`execution-model.md` §2.1, the *asserts/compiles* edge.

The general rule is §2.1 rule 4: when a producer widens, land the consumers first. A producer that widens
what it **offers** rather than what it **emits** inverts safely — extend it while nothing consumes the new
fields, and each consumer turns them on in its own item against an already-green tree.

**Mapping, so no consumer is surprised by a value change:**

| Legacy field | New value | Retires in |
|---|---|---|
| `bg` `surface` `fg` `muted` `border` | the same-named new seeds | never — these are seeds |
| `accent` | `primary` | wave E close |
| `accentSoft` | `primarySoft` | wave E close |
| `quietInk` | `quiet` | wave E close |
| `strongBorder` | `outlineControl` | wave E close |
| `surfaceHover` `toastSurface` `toastInk` `backdrop` | re-derived from the new seeds | never — no new-name equivalent |

**Rejected:** a second parallel token object. It doubles what 018–020 must reason about and gives the
duplication no end date.

**Cost, accepted:** for the length of wave E, `accent` and `primary` are one colour under two names. The
wave note records the retirement.

---

## D2 — Variable fonts, and no API version gate

**Decision.** Load both families as variable fonts via `FontVariation.Settings`, one `Font()` per authored
weight. **No `Build.VERSION.SDK_INT` branch.**

**Why no gate.** Android's own documentation gates `variationSettings` on `Build.VERSION_CODES.O` — API 26
— which is this project's exact `minSdk`. The fallback branch is unreachable, and unreachable code in a
theme is a maintenance trap rather than a safety measure.

Verified against current Compose documentation at design time, not from memory. If `variationSettings`
turns out to need an experimental opt-in under Compose BOM 2026.08.00, add it; that is a compile fact, not
a design decision.

**Weights needed:** Playfair Display 600/700/800, inside its `wght` 400–900 range. Roboto Flex
400/500/600/700, inside its far wider range. Only the `wght` axis is used, of Playfair's one and Roboto
Flex's thirteen.

**Rejected:** static per-weight instances. Seven files instead of two, and Roboto Flex publishes no static
cuts, so it would mean substituting a different family.

---

## D3 — Two outline roles, both derived, neither frozen

**Decision.** `outlineVariant` is the decorative hairline — the `border` seed moved one `0.15` lightness
step off the card, below it in light and above it in dark. `outlineControl` is the control boundary —
whichever of `border` and `muted` reaches at least 3:1 against the card **in that scheme**.

**Why two.** `06-ui-ux.md` §73.1 holds a control's only boundary to 3:1 and exempts a divider. One token
cannot satisfy both: the hairline that reads correctly as a divider is 1.6:1, and a boundary at 4.5:1 reads
as a heavy rule where a divider belongs.

**Why derived rather than picked.** Light resolves to `border` at 4.5:1; dark resolves to `muted` at 6.6:1,
because in dark the `border` seed is *darker* than the card and reaches only 1.4:1. A single hand-picked
value is wrong in one scheme or the other, and which one is not obvious by eye — all three rejected
candidates looked fine on screen.

**Test shape.** Assert **the ratio, not the hex.** A frozen hex proves nothing about the floor after a
reseed, and the floor is the requirement.

---

## D4 — A near-achromatic endpoint adopts the other's hue

**Decision.** In Oklch mixing, when one endpoint's chroma is negligible it takes the other endpoint's hue
rather than contributing its own.

**Why.** Pure white's hue is an artefact of rounding, not a colour. The shipped `mixOklch` interpolates hue
unconditionally, so `mix(primary 12%, white)` — the content-type badge — came out **`#E4E6D3`, an olive**,
where a pale blue belongs. Found by computing the palette during the design pass; it would otherwise have
reached the code and been resolved by picking a literal.

`06-ui-ux.md` §78.1 now carries the rule. Its regression test is a scenario in §4, because the failure is
plausible-looking rather than obviously wrong.

---

## D5 — `stat-num` is authored now and unused until 020

**Decision.** Author it, map it to an unused Material 3 display slot, and leave it unconsumed.

**Why.** It is one of §76.1's nine and this item ships the type scale. Holding it back would put a colour
or type value inside item 020, which §77.1 forbids. Same additive shape as the fonts.

---

## D6 — Spacing and shape are new theme files; the literals stay where they are

**Decision.** Add the §14.2 spacing rhythm and the §15.2 shape scale to the theme package. **Migrate none
of the ~160 `dp` literals** in `ui/components/**` and `ui/screens/**`.

**Why.** Migrating them touches every consumer file, which §3 forbids and which would make the diff
unreviewable as a token change. Items 018–020 re-lay out those components and move their own literals in
the process.

**Consequence, stated so nobody reads it as an oversight:** after this item, the scales exist and almost
nothing uses them. `1.dp` appears 27 times, `8.dp` 25 times, `20.dp` 17 times, and all of them survive
this item unchanged.

---

## D7 — Twelve consumed slots, nine authored styles, nothing invented

**Decision.** Assign the three consumed slots §76.1 does not author to authored styles:

```text
bodySmall   → body-md
labelSmall  → label-md
titleMedium → headline-sm
```

**Why.** `AGENTS.md` forbids inventing requirements where a specification is silent. Choosing new sizes for
these three would be exactly that. Assignment reuses authored metrics and invents nothing.

**This also closes a latent defect.** `titleMedium` is consumed at `LocalStateMessages.kt:48` and is
undefined today, so that string has always rendered in Material 3's default face rather than the authored
stack. The scenario asserting that no consumed slot equals the M3 default is what keeps it closed.

---

## What this note deliberately does not decide

**Any colour.** Ten seeds, both schemes, in `06-ui-ux.md` §77.4 and §77.5, owner-approved 2026-09-01.
An implementer that finds itself choosing a colour has found a defect in this plan.
