# Intentional Reading — V1 UI and UX Specification

**Status:** Approved for V1\
**Document:** `docs/v1/06-ui-ux.md`\
**Role:** Authoritative visual, responsive, interaction, accessibility, navigation, and presentation specification\
**Edition:** Second. Reorganised and de-conflicted under Amendment 9; section numbers are unchanged from the
first edition so that existing citations continue to resolve.

---

## 0. How to read this document

V1 ships **two clients** against one product: the browser runtime and the native Android client
(Amendment 6). They behave identically and they do not look alike.

The first edition of this document was written for the browser and later inherited by Android without
being told apart. That produced contradictions — a hard clamp forbidden in one section and required by the
Android design sources, rows that must not be cards and an Android queue row that is one. This edition
removes them by **scoping every rule at the point where it is stated.**

**Every section opens with a `Binds:` line.** It says which surfaces the section governs. There are four forms:

```text
Binds: both surfaces.
Binds: both surfaces — presentation differs; see the numbered blocks.
Binds: browser only.
Binds: Android only.
```

If a section carries per-surface blocks, **only the block for your surface applies to you**, and the other
block is not a contradiction of it. If a section carries no blocks, it binds you whole.

**Behaviour is never scoped.** Every rule about what the application *does* — state transitions, counts,
signals, action semantics, gesture outcomes, keyboard bindings, live status, undo, accessibility — binds
both surfaces without exception. Only *presentation* is ever split, and a presentation section may never
change behaviour. Where you find one that appears to, it is a defect in this document and not a licence.

**Nothing here restates the contracts.** `contracts.md` owns the Article schema, statuses, actions,
signals and storage. Where this document names a value that also appears there, `contracts.md` wins.

Sections **76–79** are new in this edition and carry Android presentation that the first edition had no
home for. Section **80** is the concordance: it records where every first-edition rule now lives, so a
reader arriving from an older citation can find what they were sent to.

---

## 1. Purpose

**Binds:** both surfaces.

This document defines V1's user interface and interaction behaviour.

It translates the approved product specification, the approved design systems, the shared application
contracts, and the responsive and accessibility requirements into implementation requirements.

V1 must not be visually reinterpreted as:

- a generic SaaS dashboard;
- a minimalist developer demo;
- Hacker News;
- a Tinder clone;
- a social-media feed;
- a card-heavy administration interface.

The approved product character, which both surfaces express in their own visual language, is:

> Editorial publication + premium reading application + restrained tactile triage.

---

# 2. Design Authority

**Binds:** both surfaces — sources differ; see 2.1 and 2.2.

Each surface has its own approved visual source. A surface's source is authoritative for that surface's
exact design-system values and for nothing else. **Neither source may be cited against the other surface.**

Both sources are subordinate to this document: where a source and this document disagree, **this document
is the specification and the source is a reference.**

## 2.1 Browser

```text
design-reference/DESIGN.md
design-reference/intentional-reading-prototype.png
```

`DESIGN.md` owns exact design-system values.

The screenshot is the approved visual reference for overall character, visual density, typography
relationship, spatial composition, card presence and desktop tone.

The screenshot is **not** authoritative for example article content, prototype publisher names, example
counts, fabricated topic values, or a repeated masthead or Settings control caused by illustrative
stitching. Actual application content comes from the V1 Article and state contracts.

## 2.2 Android

```text
specs/design/m3-expressive-DESIGN.md
specs/design/m3-expressive-PRD.md
```

These two sources disagree with each other, and `m3-expressive-DESIGN.md`'s prose disagrees with its own
token block. **The precedence order is fixed:**

1. **`m3-expressive-DESIGN.md`'s YAML token block** — authoritative for token *values*.
2. **`m3-expressive-PRD.md`** — authoritative for component *states*, *motion specifications* and
   *information architecture*.
3. **`m3-expressive-DESIGN.md`'s prose** — *illustrative*. It describes intent and binds nothing alone.

A colour the prose names that no token block carries is **mapped onto an existing role** (§77.3), never
admitted as a literal.

**Two seed values are settled by owner decision as named exceptions to this order**, recorded in §77.2.
The order governs everything it does not name.

## 2.3 One masthead per view

Each rendered view has exactly one application masthead and one Settings control. There must not be a
duplicated `Intentional Reading` masthead or a duplicated Settings control on any screen.

---

# 3. Design Principles

**Binds:** both surfaces.

## 3.1 Attention is finite

Every screen should help the reader:

```text
choose
read
leave
```

Avoid infinite-feed cues, streaks, engagement counters, reward effects, celebratory animation, urgency,
and artificial continuation prompts.

## 3.2 Text is the visual material

**The application has no article imagery.** Visual hierarchy comes from typography, spacing, rules,
surfaces, metadata, tags and restrained accent use.

Article thumbnails, stock imagery, generated imagery and decorative content photography are prohibited.
See §74.2 for why this is a contract boundary and not a taste preference.

## 3.3 Editorial, not administrative

Prefer editorial rhythm, typographic contrast, rules, quiet surfaces and intentional whitespace over
dashboard grids, metric cards, dense control panels and enterprise UI patterns.

## 3.4 Gestures always have equivalents

Swipe is a fast interaction, never the only interaction. Every swipe action must also exist as a visible
control, and as a keyboard action on any surface with a keyboard.

## 3.5 Queue state stays honest

Read Later and History counts update immediately after successful state transitions. No decorative or
approximate counts are permitted.

---

# 4. Colour System

**Binds:** both surfaces — token architecture differs; see 4.1 and 4.2.

One rule is common to both surfaces and is the reason this section exists: **colour is authored as a small
named seed set, and every other value is derived from it in Oklch.** Neither surface admits arbitrary
independent palette values, and no component on either surface may name a colour of its own.

## 4.1 Browser — six seeds

Six authored tokens per theme. Derived colours use:

```css
color-mix(in oklch, ...)
```

Tokens are in §5 (light) and §6 (dark); derivation rules are in §7.

## 4.2 Android — ten seeds

Ten authored seeds per theme, because Material 3 carries primary, secondary, tertiary and error role
families that the browser's six-token system has no equivalent for. Seeds are in §77; derivation is in §78.

The count is the only architectural difference. The discipline is identical.

---

# 5. Browser Light Theme Tokens

**Binds:** browser only.

```css
:root {
  --bg: oklch(0.972 0.008 255);
  --surface: oklch(0.995 0.003 255);
  --fg: oklch(0.205 0.03 260);
  --muted: oklch(0.49 0.025 260);
  --border: oklch(0.875 0.018 260);
  --accent: oklch(0.322424 0.12543 262.24);
}
```

The light-theme accent corresponds exactly to `#0B2D72`.

---

# 6. Browser Dark Theme Tokens

**Binds:** browser only.

```css
[data-theme="dark"] {
  --bg: oklch(0.145 0.025 260);
  --surface: oklch(0.195 0.03 260);
  --fg: oklch(0.94 0.012 255);
  --muted: oklch(0.72 0.025 255);
  --border: oklch(0.34 0.03 260);
  --accent: oklch(0.74 0.12543 262.24);
}
```

Dark mode preserves the accent's hue and chroma and raises lightness for accessible contrast. **This rule
generalises to both surfaces** and is restated for Android in §77.5.

---

# 7. Browser Derived Colour Rules

**Binds:** browser only. The prohibitions in 7.1 bind both surfaces.

Derived values may include concepts such as:

```text
accent-soft
surface-hover
strong-border
quiet-ink
toast-surface
```

and must derive from the authored six-token system.

## 7.1 Prohibited on either surface

- independent rainbow category palettes;
- neon positive/negative swipe colours;
- unrelated one-off hexadecimal values across components.

---

# 8. Accent Usage

**Binds:** both surfaces — the accent's role differs; see 8.1 and 8.2.

The shared principle: **the accent is restrained.** It marks the primary action, the content-type badge,
focus, and active category state. It is not distributed across the interface as decoration.

## 8.1 Browser

Use the accent for the content-type badge, the primary `Read article` action, focus indication and subtle
active-category treatment.

**Navigation selection uses primary ink rather than accent colour.**

## 8.2 Android

Use `primary` for the filled `Read article` control, the active category chip, and focus. Use the derived
`primarySoft` for the content-type badge.

**Navigation selection uses the `tonal` container** — a tonal pill indicator, not primary ink and not a
primary fill. This differs from 8.1 and satisfies the same restraint: the pill is a tonal container, so
the accent is marking state without being sprayed across the bar.

---

# 9. Category Identity

**Binds:** both surfaces.

Categories may receive restrained emphasis through soft accent fill, type treatment or border treatment.

**V1 must not create a seven-colour category rainbow.** There are exactly seven categories (§17.2), which
is precisely why this rule exists.

Topic tags remain neutral on both surfaces.

---

# 10. Swipe State Colour Semantics

**Binds:** both surfaces.

Saving and rejection must not depend on green/red, or on any colour pair, alone.

State communication requires:

```text
direction
text
icon/arrow
motion
```

For example:

```text
← Not interested

Save for later →
```

Colour may support the state. It must never be the sole signal. This is an accessibility requirement
(§73), not a stylistic one.

---

# 11. Typography

**Binds:** both surfaces — families differ; see 11.1 and 11.2.

Both surfaces use a **two-register pairing**: an editorial face for article titles, screen titles and
statistics, and a functional face for navigation, labels, buttons, body and metadata. The registers carry
meaning — editorial type marks content, functional type marks interface — and no surface may collapse them
into one family.

**Neither surface downloads a font at runtime.**

## 11.1 Browser — system stacks

Display stack:

```css
font-family: "Iowan Old Style", Charter, "Palatino Linotype", Georgia, serif;
```

Body/UI stack:

```css
font-family: "Avenir Next", Avenir, "Segoe UI", system-ui, sans-serif;
```

Metadata stack:

```css
font-family: SFMono-Regular, Consolas, monospace;
```

No web-font downloads are used.

## 11.2 Android — two bundled families

| Register | Family | Used for |
|---|---|---|
| Editorial | **Playfair Display** (serif) | article titles, screen titles, section headings, statistics |
| Functional | **Roboto Flex** (sans) | navigation, labels, buttons, body, excerpts, metadata |

Both ship as **`res/font/` assets inside the APK**, which adds no Gradle dependency and fetches nothing at
runtime. `androidx.compose.ui.text.googlefonts` is **not authorised**; requesting it is a supervisor
decision under `AGENTS.md`.

Android has no separate metadata register. Metadata uses the functional face at `label-md` (§76.1), whose
weight and tracking carry the same at-a-glance role the browser's monospace stack does.

---

# 12. Typography Scale

**Binds:** both surfaces — scales differ; see 12.1 and 12.2.

## 12.1 Browser — viewport-fluid

### H1

```css
font-size: clamp(34px, 6vw, 56px);
line-height: 0.98;
letter-spacing: -0.035em;
```

### H2

```css
font-size: clamp(28px, 4vw, 40px);
line-height: 1.05;
letter-spacing: -0.025em;
```

### H3 / reading-list title

```text
21–27px
line-height 1.12
```

### Discover article title

Desktop/tablet:

```css
font-size: clamp(30px, 5.2vw, 47px);
```

Phone:

```css
font-size: clamp(30px, 9vw, 41px);
```

### Body

```text
16px / 1.55
```

### Discover excerpt

```text
display serif
18px / 1.48
```

### Metadata

```text
12px
uppercase
monospace
```

## 12.2 Android — fixed scale

`clamp()` is a browser mechanism with no Compose equivalent, so the Android scale is fixed. It is
specified in §76.1.

**Android sizes are declared in `sp`, not `dp`**, so that type scales with the reader's system font-size
setting. A fixed-`dp` type scale is an accessibility defect under §73.

---

# 13. Text Wrapping and Truncation

**Binds:** both surfaces — the rule genuinely differs; see 13.1 and 13.2.

**This is the one section where the two surfaces reach opposite conclusions, and it is deliberate.** The
shared requirement is what both conclusions protect: *a real, long publisher title must never break the
layout or collide with metadata or actions.*

## 13.1 Browser — wrap, never clamp

Use balanced heading wrapping where supported:

```css
text-wrap: balance;
```

Use readable paragraph wrapping where supported:

```css
text-wrap: pretty;
```

**Never force article titles to one line, and never apply an arbitrary hard clamp.** Long real-world
titles must remain readable and fully visible. A browser viewport can grow; the page scrolls.

## 13.2 Android — clamp, with stated limits

A handset deck card cannot grow. Item 012 measured a real dataset title —

> Nonviral delivery of chemically modified tRNA rescues nonsense mutations in cystic fibrosis | Science

— wrapping to **six lines** at the deck headline size and consuming a 360 dp viewport by itself, which
puts the card's own action rail below the fold. No arrangement of the surrounding layout fixes this,
because the card's height is unbounded in its title
(`specs/012-android-discover-card-first/spec.md` §1.4).

So the Android client clamps, at limits that are specified and testable:

```text
Discover card headline      max 3 lines, then ellipsis
Discover card description   max 2 lines, then ellipsis
Read Later / History title  max 2 lines, then ellipsis
```

**What is given up:** seeing the whole of a very long title on the card. **What that buys:** the card's
excerpt, tags and all three action controls stay above the fold at the narrowest supported width. The full
title is always available on the opened publisher page (§50).

---

# 14. Spacing System

**Binds:** both surfaces — rhythms differ; see 14.1 and 14.2.

Both surfaces build on a **4px/4dp base unit**. Neither admits ad-hoc spacing values.

## 14.1 Browser

Primary rhythm:

```text
8px
```

Approved tokens:

| Token | Value |
|---|---:|
| `XS` | 8px |
| `SM` | 12px |
| `MD` | 20px |
| `LG` | 32px |
| `XL` | 56px |
| `2XL` | 96px |

Page gutters:

```css
clamp(18px, 4vw, 36px)
```

Implementation may express these as CSS custom properties.

## 14.2 Android

```text
base unit       4dp
stack gap      12dp   within a card or a tightly associated group
gutter         16dp   between columns
mobile margin  18dp   screen edge, handset
tablet margin  24dp   screen edge, tablet and foldable
section gap    32dp   between major functional areas
```

Central content is capped at **680 dp** on large screens so headlines stay readable.

The 18 dp mobile margin is DESIGN's token-block value and supersedes the PRD's 24 px under §2.2.

---

# 15. Radii

**Binds:** both surfaces — values differ; see 15.1 and 15.2.

The shared rule: **large rounded-card styling is not applied indiscriminately to every container.** A
generous radius marks the primary decision surface. Everything else is quieter.

## 15.1 Browser

```text
controls       10px
article card   20px
dialogs        20px
pills/tags     fully rounded
```

Read Later and History rows use **rules rather than floating-card containers** (§57).

## 15.2 Android

```text
primary card                    24dp
queue rows, StatBand, small containers   16dp
modal sheet, bottom bar         28dp, top corners only
filled primary button           16dp or fully rounded
chips, badges, pills            fully rounded
icon buttons                    circular
media slots, if ever added      20dp internal
```

24 dp is the **primary card's** radius, not a default. Android queue rows are tonal containers at 16 dp
and carry **no shadow and no elevation** (§58.2) — which satisfies §57's intent, because what §57 prohibits
is the raised floating card, not a tonal fill.

---

# 16. Shadows

**Binds:** both surfaces — values differ; see 16.1 and 16.2.

The shared rule: **one soft shadow on the primary article card, and no enclosing shadow on reading-list
rows.**

## 16.1 Browser

Primary article card:

```text
1px hairline
+
0 18px 50px soft warm-ink shadow at ~11%
```

Toast/dialog:

```text
0 14px 35px warm-ink shadow at ~16%
```

## 16.2 Android

Hierarchy comes from **tonal layers first, shadow second**:

```text
canvas layer     the bg seed; furthest back
container layer  the card role; the deck card
tonal highlight  the tonal role; active states and meta zones
modal sheet      8dp+ elevation with a dimming scrim
```

The deck card's separation is a **soft, wide, diffuse ambient shadow tinted with the `tertiary` seed —
Midnight Navy — at approximately 10% opacity**, not neutral grey. In the dark scheme the ambient shadow
is near-black; `tertiary` is a light-scheme tint and is not used for shadow in dark (§78.4).

Queue rows, the StatBand and chips carry no shadow on either surface.

---
# 17. Primary Navigation Terminology

**Binds:** both surfaces.

## 17.1 Destinations

The exact user-facing primary destinations are:

```text
Read Later
Discover
History
```

Do not use `Read`, `Saved`, `Queue` or `Archive` as replacements for these labels.

## 17.2 Categories

The exact category options are:

```text
All
Science
Technology
Literature
History
Weightlifting
IAM
Identity Automation
```

Seven categories plus `All`. Category IDs come from `contracts.md`; these are their display labels.

---

# 18. Primary Navigation — Mobile and Handset

**Binds:** both surfaces — presentation differs; see 18.1 and 18.2.

Shared and non-negotiable:

- a fixed bottom navigation bar;
- exact order `Read Later | Discover | History`;
- Discover is the centre and the visual anchor;
- **Read Later and History display counts**, and those counts are immediately truthful (§3.5);
- every navigation target meets the touch floor in §72.

## 18.1 Browser

Discover sits approximately **7px higher** than the adjacent destinations, which is how its primacy is
expressed.

Navigation targets are at least `54px` high.

## 18.2 Android

**Material 3 Expressive bar**, `28dp` top corner radius, three destinations on **one baseline**.

Discover's primacy is expressed by its centre position and by the active-destination **`tonal` pill
indicator** (§8.2), not by a vertical lift. The lift is dropped deliberately: with a pill indicator marking
the active destination, a raised centre destination reads as a floating action button, which §19 prohibits
in spirit on both surfaces.

Navigation targets are at least `48×48dp` and the bar is at least `54dp` high.

---

# 19. Primary Navigation — Desktop

**Binds:** browser only.

The Android client ships no desktop width (§71.2), so this section does not apply to it.

At desktop widths, navigation becomes a sticky top treatment.

Semantic order remains:

```text
Read Later | Discover | History
```

Navigation is positioned between application identity/brand and the Settings control. The current page is
indicated with an ink underline or an equivalent approved treatment.

Discover remains visually primary **without looking like a floating mobile button.**

---

# 20. Settings Entry

**Binds:** both surfaces — the control's form differs; see 20.1 and 20.2.

Shared and non-negotiable:

- Settings is **not a primary navigation destination** and must not become a fourth tab;
- it opens a compact modal or dialog;
- it must not become a full-page dashboard;
- it meets the touch floor in §72.

## 20.1 Browser

A `44px` circular gear control.

## 20.2 Android

A trailing settings icon in the top app bar, which carries a centred `Intentional Reading` brand title
(§76.4). The control's visible glyph may be smaller than its target; the target is at least `48×48dp`.

---

# 21. Discover Header

**Binds:** both surfaces. *Amended by Amendment 7.*

Discover's header is split, and the article card sits between the two halves.

The **masthead** comes first and is deliberately compact:

- small metadata/eyebrow;
- application/screen title.

The **operational block** follows the article card:

- concise purpose copy;
- refresh affordance and content-age context;
- failed-refresh disclosure;
- available/current article context;
- category selector.

The binding rule is the ordering intent, not the widget list:

```text
the first thing in the viewport on Discover is the article card,
not the controls that describe it
```

The masthead should feel like publication masthead or content framing rather than dashboard chrome, and
framing placed below its subject is still framing.

**A redesign may re-lay out either half provided the card still leads the viewport.** This is the licence
Amendment 7 granted and it is also the constraint: a redesign that restores the pre-amendment ordering
while re-laying out Discover has broken this rule, not exercised the licence.

---

# 22. Category Selector

**Binds:** both surfaces — presentation differs; see 22.1 and 22.2.

Shared:

- options exactly as §17.2;
- the selector is keyboard accessible on any surface with a keyboard;
- selection state is communicated by more than colour (§10, §73);
- the selector meets the touch floor in §72.

Selecting a category changes which Articles are eligible. It changes nothing else — no ranking rule, no
status, no signal.

## 22.1 Browser

Minimum control height `40px`.

```text
unselected   neutral outline
selected     ink fill, surface-coloured text
```

## 22.2 Android

Chips are `40dp` high and fully rounded, with a target of at least `48×48dp` (§72).

```text
selected     solid primary fill, high-emphasis label
unselected   surface fill, control-boundary outline (§78.3), medium-emphasis label
```

The unselected chip's outline is the only thing marking it as a control, so it is subject to the
control-boundary contrast rule in §78.3 and must not use the decorative hairline role.

---

# 23. Discover Composition

**Binds:** both surfaces.

Discover shows **exactly one primary article card** as the decision surface.

A subtly offset secondary neutral card may appear behind it to communicate:

```text
there is a finite queue
```

without creating a visible infinite stack, deck gamification, or exaggerated Tinder aesthetics.

---

# 24. Article Card Anatomy

**Binds:** both surfaces.

The Discover card contains, in order:

1. source;
2. publication age/date;
3. content-type badge;
4. category context where appropriate;
5. article title;
6. excerpt;
7. topic tags;
8. action rail.

The card must remain **text-first**.

**No reserved image area exists on either surface.** See §74.2.

---

# 25. Article Title

**Binds:** both surfaces — the truncation rule differs; see §13.

The title is the **dominant visual element** of the card, on both surfaces, set in the editorial register
(§11).

Shared requirements:

- editorial/display face;
- no fixed one-line treatment;
- supports very long real-world titles without breaking the layout;
- does not collide with metadata or actions.

**Truncation is specified in §13** — the browser wraps and never clamps (§13.1); Android clamps at three
lines (§13.2). Both satisfy the last two requirements above; they differ in how.

---

# 26. Excerpt

**Binds:** both surfaces — line allowance differs; see 26.1 and 26.2.

Shared, and binding on both surfaces:

- if the Article excerpt is empty, **omit the excerpt area gracefully**;
- do not insert filler text;
- do not display `No description available.`

## 26.1 Browser

Discover displays approximately `2–4` lines when content is available: up to 3 lines may be visually
preferred at very narrow phone widths, up to 4 at normal mobile widths.

## 26.2 Android

Exactly `2` lines, then ellipsis (§13.2).

---

# 27. Topic Tags

**Binds:** both surfaces — presentation differs; see 27.1 and 27.2.

Shared:

- at most `5` tags in normal Discover presentation;
- at very narrow widths, prioritise the first `3`;
- tags are **neutral** (§9) and carry no category colour;
- tags are **informative, not interactive filters** in V1;
- at most three tags on a Read Later row (§56).

## 27.1 Browser

```text
12px body text
neutral border
muted text
no fill
fully rounded
```

## 27.2 Android

```text
label-md or body-md
decorative hairline outline (§78.3)
muted label
no fill
fully rounded
```

Tags are not controls, so their outline is decorative and is **not** subject to the 3:1 control-boundary
rule. If tags ever become interactive, that rule begins to apply and this section must change with it.

---

# 28. Content-Type Badge

**Binds:** both surfaces — presentation differs; see 28.1 and 28.2.

Shared, and the important part:

**The UI displays the authoritative Article `contentType.label`. It must not infer, shorten, retitle or
substitute a different label.** Real values include:

```text
STANDARDS UPDATE
ENGINEERING DEEP DIVE
REPORTED SCIENCE
OFFICIAL RELEASE NOTES
RESEARCH & SCIENCE
LITERARY ESSAY
HISTORICAL ESSAY
EVIDENCE-BASED TRAINING
ENGINEERING JOURNALISM
REPORTED JOURNALISM
```

Uppercase presentation is a type treatment, not a rewrite of the label.

## 28.1 Browser

```text
10px uppercase mono
accent-soft background
accent text
```

## 28.2 Android

```text
label-md, uppercase
primarySoft background
primary text
fully rounded
```

---

# 29. Reading Time

**Binds:** both surfaces.

When `readingTimeMinutes != null`, the UI may display:

```text
~7 min
```

or an equivalent concise editorial format.

**When null:**

- omit reading-time metadata entirely;
- **do not display zero**;
- do not estimate locally.

This applies wherever reading time appears — the Discover card, Read Later rows, History rows, and the
summed values in §54 and §61.

---

# 30. Publication Age

**Binds:** both surfaces.

When `publishedAt` exists, the UI may display an understandable relative age such as:

```text
5h
2d
12d
```

or an appropriately formatted date for older material.

When the publication date is unknown:

- omit the age/date;
- **do not use the dataset generation time as the article's age.**

---

# 31. Discover Action Rail

**Binds:** both surfaces.

The card's primary action rail provides:

```text
Not interested
Read article
Save for later
```

corresponding exactly to:

```text
dismiss
open
save
```

**The explicit controls must remain usable independently of swipe gestures** (§3.4), and the rail must be
reachable without hover (§72).

Presentation of the three controls is in §32, §35 and §76.5.

---

# 32. Primary Button

**Binds:** both surfaces — values differ; see 32.1 and 32.2.

Shared:

- the primary accent-filled button carries the main `Read article` action;
- it includes external-navigation indication (§50);
- **only one visually dominant primary action per screen or primary content surface.**

## 32.1 Browser

Minimum height `48px`.

## 32.2 Android

Height `52dp`, `primary` fill, `onPrimary` label, radius per §15.2.

---

# 33. Secondary Buttons

**Binds:** both surfaces — values differ; see 33.1 and 33.2.

## 33.1 Browser

```text
transparent background
border
```

Hover:

```text
soft ink-derived surface
stronger border
```

## 33.2 Android

Tonal: `tonal` fill with `onTonal` label (§78.2). Android has no hover state; the pressed and disabled
states in §37.2 apply instead.

---

# 34. Ghost and Text Actions

**Binds:** both surfaces — presentation differs; see 34.1 and 34.2.

Shared, and the part that matters: **touch dimensions must remain accessible even where the visible
treatment is compact** (§72). A compact row action is compact to look at, never to hit.

## 34.1 Browser

Read Later and History compact row actions may use ghost/text treatment, with underline or clear text-state
indication on hover.

## 34.2 Android

Row actions use an outlined pill with a `quiet` label and a control-boundary outline (§78.3), or the tonal
treatment in §33.2 for the row's leading action.

---

# 35. Round Triage Controls

**Binds:** both surfaces — values differ; see 35.1 and 35.2.

Shared:

- circular outlined controls with directional icons and **accessible names**;
- they must not replace the labelled semantic understanding of the action (§31);
- the outline is the control's only boundary, so it is subject to §78.3 on Android and to §73's contrast
  requirement on both surfaces.

## 35.1 Browser

`48px` circular outlined buttons.

## 35.2 Android

`56dp` circular targets with a `1.5dp` outline in the `secondary` seed (§77.2).

**Why `secondary` and not the tonal colour:** these buttons have no fill, so the outline is the only thing
identifying them as controls, which puts them under the 3:1 non-text contrast floor (§78.3). The Android
design source's prose names a lighter periwinkle for this outline; it reaches only **2.9:1** against the
card and fails. The `secondary` seed reaches **6.5:1**.

---

# 36. Destructive Treatment

**Binds:** both surfaces.

Reset and similar destructive actions use restrained styling.

```text
default   transparent, ink/border treatment
active    ink fill, surface text
```

**Do not introduce an alarming neon-red treatment inconsistent with the editorial design.**

On Android, the `error` seed exists for genuine error states and validation failures. It is **not** the
styling for Reset.

The destructive action's weight comes from its wording and its confirmation (§66), not from its colour.

---

# 37. Control States

**Binds:** both surfaces — treatments differ; see 37.1 and 37.2.

Shared:

- pressed feedback is **restrained and tactile**, never a bounce (§47);
- a disabled control is non-interactive and visibly so;
- **no state may be discoverable only by hover or only by gesture** (§3.4, §72);
- any text a reader is expected to read in a disabled state still meets §73's contrast requirement.

## 37.1 Browser

Pressed controls move approximately `1px` downward.

Hover and focus colour transitions are `80–160ms` (§46.1).

## 37.2 Android

```text
pressed     12% overlay + 0.95 scale-down
disabled    38% opacity, non-interactive
```

Chip selected and unselected states are in §22.2.

A 0.95 scale-down is restrained and is not the bounce §47 prohibits.

---

# 38. Focus Treatment

**Binds:** both surfaces.

**Every focusable control must display a visible focus ring**, and it must remain obvious in **both light
and dark schemes** on both surfaces.

Browser target:

```text
3px accent-derived ring
3px offset
```

Android uses the platform focus indication driven from `primary`, at an equivalent visible weight.

Focus visibility in both themes is an acceptance requirement (§73), not a preference.

---
# 39. Swipe Interaction Surface

**Binds:** both surfaces.

The article card may be dragged horizontally **from non-interactive card areas only**.

Do not start card dragging from:

- buttons;
- links;
- the category selector;
- other interactive descendants.

Where practical, use a unified pointer abstraction so that touch, stylus and mouse behave identically.

---

# 40. Swipe Threshold

**Binds:** both surfaces.

Commit threshold:

```text
90px / 90dp horizontal travel
```

On pointer release:

```text
abs(horizontalTravel) >= 90
→ commit the corresponding action
```

Below threshold:

```text
snap/return to default
no state change
no preference signal
```

---

# 41. Swipe Left

**Binds:** both surfaces.

Dragging left:

- translates the card left;
- applies a slight counterclockwise rotation;
- displays a clear directional cue:

```text
← Not interested
```

Release beyond threshold emits `dismiss`.

---

# 42. Swipe Right

**Binds:** both surfaces.

Dragging right:

- translates the card right;
- applies a slight clockwise rotation;
- displays:

```text
Save for later →
```

Release beyond threshold emits `save`.

---

# 43. Swipe Commitment

**Binds:** both surfaces.

After a committed swipe:

1. the active card exits briefly;
2. the state action is processed;
3. the next eligible card appears;
4. a toast announces the result;
5. Undo becomes temporarily available (§70).

**The UI must not visually finalise a save or dismiss if persistence failed** (§50).

---

# 44. Swipe Motion

**Binds:** both surfaces — curve differs; see 44.1 and 44.2.

**The character constraint binds both surfaces and is not negotiable.** Card response must feel:

```text
tactile
quiet
controlled
```

and must not feel:

```text
bouncy
playful
rewarding
```

Only the curve and duration differ below. **Swipe behaviour — the surface, threshold, cues and commitment
of §39–§43 — is identical on both surfaces and is not affected by either.**

## 44.1 Browser

```text
280ms
cubic-bezier(0.2, 0.8, 0.2, 1)
```

## 44.2 Android

Material 3 Emphasized easing at the M3 equivalent duration, for consistency with the rest of the Android
motion system (§79).

---

# 45. Toast

**Binds:** both surfaces — transition values differ; see 45.1 and 45.2.

Shared and binding:

- the toast remains visually available for approximately **4.5 seconds**;
- it announces its outcome through an accessible live status (§70);
- **it must not steal focus**;
- example messages:

```text
Saved to Read Later — Undo
Not interested — Undo
```

Browser semantics use:

```html
role="status"
aria-live="polite"
```

Android uses the equivalent accessibility live-region announcement.

## 45.1 Browser

```text
180ms
opacity + subtle vertical movement
```

## 45.2 Android

M3 standard easing at an equivalent short duration, with the same opacity-and-slight-translation character.

**The Android toast is hosted globally, outside the destination branch**, so it renders on every
destination. This is what makes §70's cross-destination undo offer possible.

---

# 46. Control Transitions

**Binds:** both surfaces — values differ; see 46.1 and 46.2.

Shared rule: **no slow ornamental transitions.**

## 46.1 Browser

Normal hover/focus colour transitions: `80–160ms`.

## 46.2 Android

M3 standard easing at M3 standard short durations.

---

# 47. Prohibited Motion

**Binds:** both surfaces.

Do not use:

- bounce;
- confetti;
- pulse;
- autoplay;
- reward animation;
- celebratory motion;
- continuous card movement.

This binds the Android motion system in §79 exactly as it binds the browser. "Expressive" authorises
directional and spatial motion; it does not authorise anything on this list.

---

# 48. Reduced Motion

**Binds:** both surfaces.

Under a reduced-motion preference — `@media (prefers-reduced-motion: reduce)` in the browser, the
equivalent system setting on Android — **remove or effectively eliminate**:

- card rotation;
- large swipe exit transitions;
- decorative translation;
- directional destination transitions (§79.1);
- the modal sheet's slide (§79.2).

Transitions and animations become **effectively immediate**.

The semantic outcome of every swipe and every control must remain fully clear through:

- text;
- state changes;
- live status.

**Every animation either surface adds must honour this, and a test must assert that it does.** An
animation that ignores the preference is a defect, not a feature.

---

# 49. Keyboard Controls

**Binds:** browser only, and any surface that exposes a physical keyboard.

Discover supports:

```text
Left Arrow   → Not interested
Right Arrow  → Save for later
Z            → Undo
```

Keyboard shortcuts must not fire when doing so would conflict with a focused form control, dialog control,
category selection, or other interactive context.

The Android client has no keyboard requirement in V1. If it ever adds one, these are the bindings it uses.

---

# 50. Read Article Interaction

**Binds:** both surfaces.

`Read article`:

- visibly includes external-navigation indication;
- emits `open`;
- attempts Open-state local persistence **first**;
- opens the publisher **after** the persistence attempt, whether that attempt succeeded or failed;
- **does not mark the article read.**

When Open persistence fails, show an accessible local-state warning and **do not claim that the Open
interaction was persisted.** Publisher navigation must still proceed, so reading remains available.

**For every persistent state action — Save, Dismiss, Mark Read, Mark Unread, Remove, Import, Reset — do
not visually claim a successful state transition when persistence failed.**

The external marker may use:

```text
↗
```

or the approved equivalent.

---

# 51. Opened Article Return State

**Binds:** both surfaces.

When the reader returns after opening an article, the application may visibly acknowledge that the article
was opened.

The UI should support clear next choices such as:

```text
Mark read
Save for later
Not interested
```

according to current state.

**Do not automatically assume completion.**

And the standing question behind this section, which applies to every screen: *is what the reader needs
next actually on screen?* An acknowledgement that appears while the action it invites has scrolled out of
reach has not delivered this section.

---

# 52. Read Later Screen

**Binds:** both surfaces — header presentation differs; see 52.1 and 52.2.

Read Later uses an **editorial list, not swipe cards**, on both surfaces.

Unlike Discover, Read Later keeps its **full editorial header above its content**. Amendment 7's ordering
rule applies to Discover only.

## 52.1 Browser

The header uses the same overall rhythm as Discover:

```text
mono eyebrow
serif H1
short muted description
secondary navigation/action
```

## 52.2 Android

```text
label-md eyebrow
display/headline Playfair title
optional body-md description
```

---

# 53. Read Later Overview Band

**Binds:** both surfaces.

Read Later may display a three-part overview band containing:

1. queue count;
2. summed known estimated reading time;
3. next/top queue topic.

**Values must come from current local state. Do not fabricate values.**

This section is what authorises the band. It is therefore **not** the "dashboard metric grid" §74 prohibits
— but it must not grow into one: three values, derived from state, and nothing else.

Android's presentation of this band is the StatBand (§76.6).

---

# 54. Read Later Reading-Time Summary

**Binds:** both surfaces.

Sum only saved Articles whose `readingTimeMinutes != null` (§29).

If no saved item has a known duration:

```text
display unavailable / omit the value
```

**Do not display `0 min`** as though every unknown article takes zero time.

This is the null-presentation rule for the whole surface. It is already decided here and no item redecides
it.

---

# 55. Read Later Topic Summary

**Binds:** both surfaces.

Use the first/default queue Article with at least one tag, and display:

```text
article.tags[0].label
```

If no saved Article contains tags:

- omit the topic value;
- **do not infer one in the UI.**

---

# 56. Read Later Rows

**Binds:** both surfaces — layout differs; see §57 and §58.

Each row displays, as appropriate:

- queue position;
- saved age;
- source;
- category;
- reading time when known (§29);
- content type (§28);
- title;
- up to three topic tags (§27).

Actions:

```text
Read
Mark read
Remove
```

## 56.1 Rows are not raised cards — on either surface

A reading list is a list. **No row on either surface is wrapped in its own raised, shadowed, floating
card.** Rows carry no ambient shadow and no elevation.

The browser expresses this with **rules between rows** (§57, §58.1). Android expresses it with a **tonal
container at 16 dp with no shadow** (§58.2). Both satisfy this rule: what it prohibits is the *raised
floating card*, not a tonal fill.

---

# 57. Read Later Rows — Desktop

**Binds:** browser only.

Desktop row structure uses approximately three semantic regions:

```text
queue position / date
article information
right-aligned actions
```

Rows are separated by rules (§56.1).

---

# 58. Read Later Rows — Mobile and Handset

**Binds:** both surfaces — presentation differs; see 58.1 and 58.2.

Shared:

- rows collapse to one column;
- actions align left;
- interactive actions meet the touch floor in §72.

## 58.1 Browser

Rows are separated by rules. Interactive actions remain at least `44px` in usable touch size.

## 58.2 Android

Each row is a **tonal container**: `16dp` radius, the derived `container` fill, **no shadow and no
elevation** (§56.1). Rows are separated by the layout gap rather than by a rule.

**There is no thumbnail.** The Android design sources specify an 80 dp square thumbnail on this row; no
such image exists (§74.2), and the horizontal space it would have occupied goes to the headline and
metadata instead. The row is laid out so that a media slot could be introduced later without a re-layout.

Row titles clamp at two lines (§13.2). Row actions meet at least `48×48dp` (§72).

---
# 59. Empty Read Later

**Binds:** both surfaces.

Empty-state messaging should explain:

> Save worthwhile articles from Discover to build your reading queue.

Provide one clear route back to `Discover`.

**Avoid guilt language.** No streaks, no urgency, no implication that an empty queue is a failure.

---

# 60. History Screen

**Binds:** both surfaces.

History follows the same editorial header rhythm as Read Later (§52) and presents read Articles
chronologically.

Group headings:

```text
Today
Yesterday
Earlier
```

according to the reader's **local** display date.

---

# 61. History Overview

**Binds:** both surfaces.

Where the approved design includes an overview band, its values derive from local History.

Potential values:

- History count;
- summed known reading time (§54's rule applies unchanged);
- latest topic.

**Do not infer values unavailable from Article snapshots.**

---

# 62. History Rows

**Binds:** both surfaces — layout follows §58.

Display:

- read date/time context;
- category;
- source;
- content type (§28);
- title;
- reading time when available (§29).

Actions:

```text
Reopen
Mark unread
```

**Mark unread returns the Article to Read Later and updates both counts immediately** after the successful
transition (§3.5). This is behaviour and is identical on both surfaces.

---

# 63. Empty History

**Binds:** both surfaces.

Explain that Articles appear after the reader marks them read. Provide a clear route to Discover or Read
Later **without guilt, streak or urgency language** (§59).

Android's empty History is a high-fidelity composition rather than a line of text, per
`m3-expressive-PRD.md` §4. It says the same thing at more visual weight; it does not say more.

**Any new user-facing string introduced here is shared copy** and follows §75.2.

---

# 64. Settings

**Binds:** both surfaces — container differs; see 64.1 and 64.2.

Settings contains only V1 controls:

- appearance: `Light`, `Dark`, `System`;
- export local data;
- import local data;
- reset local data;
- optional debug visibility where approved for development.

Shared requirements, binding on both surfaces:

- an accessible name;
- focus trapped while open;
- dismissal by Escape or the platform back affordance;
- **focus restoration on close**;
- a visible close control.

## 64.1 Browser

A compact modal dialog.

## 64.2 Android

A modal bottom sheet: `28dp` top radius, surface-card toggles, and a scrim dimming the content behind it
(§16.2, §79.2).

**Appearance offers all three options on both surfaces**, which is why both schemes are mandatory
(§77.5) — a light-only palette would regress a shipped control.

---

# 65. Import and Export Presentation

**Binds:** both surfaces.

Export produces a user-downloadable JSON backup.

Import:

- requires explicit file selection;
- **validates before replacement**;
- reports errors without destroying current state;
- asks for confirmation before replacing valid existing state.

A refused import must tell the reader why. Reporting nothing is a defect.

Do not display raw local-state contents unnecessarily.

---

# 66. Reset Presentation

**Binds:** both surfaces.

Reset requires explicit confirmation and clearly states that preferences, Read Later, History, dismissals
and settings will be cleared **on this device**.

The destructive action stays visually restrained (§36) but unmistakable through wording and confirmation.

---

# 67. Loading State

**Binds:** both surfaces.

Loading preserves the editorial composition and announces status accessibly.

**Avoid fake article text that could be mistaken for real content.**

---

# 68. Empty Discover State

**Binds:** both surfaces.

When no eligible Articles remain for the selected category:

- say so truthfully;
- offer another category or `All`;
- keep navigation and Settings available;
- do not invent recommendations;
- do not create infinite-feed behaviour.

---

# 69. Dataset Failure State

**Binds:** both surfaces.

When the static dataset cannot be loaded or validated, show a concise recoverable error with a retry
action.

Existing persisted Read Later and History snapshots remain available where possible.

---

# 70. Toast, Live Status and Undo

**Binds:** both surfaces. *Amended by Amendment 8.*

Successful save, dismiss, read/unread, import, export and reset outcomes are announced through an
appropriate live region. **Toasts must not steal focus** (§45).

Undo is offered for the most recent eligible action, **from whichever surface performed it**, and remains
available for approximately the approved toast duration.

The eligible action set is in `contracts.md` §23. The trigger — a swipe, a labelled control, or a keyboard
shortcut where one exists — **does not determine reversibility; the action does.**

Because the undo record names an article rather than a screen, an offer raised on one destination remains
valid if the reader changes destination inside the offer's lifetime.

**Amendment 8 is permissive, not obligatory.** It authorises the wider scope without requiring every client
to implement all of it; the browser's existing scope remains compliant.

---

# 71. Widths

**Binds:** both surfaces — ranges differ; see 71.1 and 71.2.

At **every** width on either surface:

- no horizontal page scrolling;
- readable titles and excerpts;
- reachable actions;
- no collision with fixed navigation;
- dialogs and sheets fit the viewport;
- long real-world content wraps or clamps safely (§13).

## 71.1 Browser

Verify at:

```text
360  390  430  600  768  820  1024  1366  1440  1920
```

## 71.2 Android

The Android client ships no desktop width. Verify at the handset and tablet range it actually ships:

```text
360  390  430  600  768
```

**360 dp is the narrowest supported width and the one that constrains the deck card** — it is the width at
which item 012 measured the unbounded-title problem that §13.2 exists to solve. It is not an optional
check.

---

# 72. Touch and Pointer Targets

**Binds:** both surfaces — floors differ; see 72.1 and 72.2.

Shared and absolute: **hover can never be required to discover or operate an action.**

A control's *visible* treatment may be compact. Its *target* may not.

## 72.1 Browser

Primary touch controls meet the design-specified minimums. Compact row actions retain at least a `44px`
usable target. Mobile navigation targets are at least `54px` high.

## 72.2 Android

**Minimum `48×48dp` for every interactive element, without exception.**

The `56dp` icon buttons (§35.2) and the `52dp` primary button (§32.2) satisfy this directly. The `40dp`
category chip (§22.2) does **not** and must carry a `48dp` target around its shorter visible pill.

**Nothing in the Android client may shrink a target below 48 dp**, including a compact row action (§34.2).

---

# 73. Accessibility Acceptance

**Binds:** both surfaces.

V1 requires:

- semantic landmarks and headings;
- keyboard-complete operation on any surface with a keyboard;
- **visible focus in both light and dark schemes** (§38);
- meaningful accessible names, including for every icon-only control;
- **non-colour-only state communication** (§10);
- sufficient text and control contrast, verified in **both** schemes;
- **reduced-motion support** (§48);
- truthful live-status announcements (§70);
- focus restoration for dialogs and sheets (§64);
- labels for icon-only controls.

## 73.1 Control boundaries are a contrast requirement

"Sufficient control contrast" above has a specific consequence that is easy to miss and has already been
missed three times in one palette:

**Where a control's boundary is the only thing identifying it as a control, that boundary must reach at
least 3:1 against the surface behind it, in both schemes.** This applies to the unfilled triage buttons
(§35), the unselected category chip (§22), and any outlined row action (§34).

A decorative divider — a rule between rows, a column separator inside the overview band — divides rather
than identifies, and is not subject to this floor.

Android's derivation of these two distinct outline roles is in §78.3. **The distinction must be derived and
stated, not eyeballed**, because three of the Android palette's candidate outline values failed this floor
and looked fine.

---

# 74. Visual Non-Goals

**Binds:** both surfaces.

Do not add engagement metrics, streaks, infinite scroll, category rainbows, dashboard metric grids,
celebratory effects, or gesture-only actions.

## 74.1 Article imagery is prohibited

Do not add article thumbnails, stock imagery, generated imagery or decorative content photography (§3.2).

## 74.2 Why imagery is a contract boundary, not a taste preference

**`ArticleDataset v1` carries no image field of any kind.** The Article contract is `id`, `title`, `url`,
`source`, `category`, `publishedAt`, `author`, `excerpt`, `readingTimeMinutes`, `tags`, `contentType`,
`score` — no image, no thumbnail, no media, no enclosure.

Both design sources call for imagery the data cannot supply: a 16:9 top-aligned slot on the Android deck
card, an 80 dp square thumbnail on the queue row, and 20 dp internal rounding on media slots. **None of it
is built** (§24, §58.2).

Adding imagery is not a UI change. It is a frozen-contract change: a new Article field, a `schemaVersion`
bump, image extraction in the pipeline, validator and web-runtime updates, and a decision about hotlinking
third-party images. It also requires amending this section and §3.2. That is its own workstream, and until
it runs this prohibition stands.

**Design components so that imagery could be added later without a re-layout.** The Android deck card
leads with its headline where a media slot would sit; the queue row spends the thumbnail's width on type.
Neither should need rebuilding if a media workstream ever runs.

---

# 75. UI Completion Criteria

**Binds:** both surfaces.

The UI is complete when:

- the approved design tokens and editorial character are preserved **for that surface**;
- Discover, Read Later, History and Settings implement their defined states;
- mouse, touch and keyboard paths are equivalent, on every surface that has them;
- **counts remain immediately truthful** (§3.5);
- **both schemes and reduced motion work** (§48, §77.5);
- all required widths pass without horizontal scrolling (§71);
- every requirement in §73, including §73.1, is verified in both schemes.

## 75.1 Behaviour parity is required; visual parity is not

The two surfaces must agree on every state transition, count, signal, action semantic and undo path. They
are **not** required to look alike, and Amendment 6 states that feature parity is not required either.

A visual difference between the surfaces is compliant. A behavioural difference is a defect.

## 75.2 New user-facing strings are shared copy

Any new user-facing string is shared-copy territory regardless of which surface introduces it. Item 011
(`011-web-validator-parity`) is the precedent for how a string is introduced and validated. A presentation
change should not be introducing strings at all; if it is, that is worth a second look at its scope.

---
# 76. Android Type Scale and Component Presentation

**Binds:** Android only.

The sections above own what every component *is* and *does*. This section owns how the Android client
draws the parts of it that the first edition had no home for.

## 76.1 Type scale

Nine styles, from `m3-expressive-DESIGN.md`'s token block. Sizes are `sp` (§12.2).

| Style | Family | Size / line | Weight | Tracking |
|---|---|---|---|---|
| `display-lg` | Playfair Display | 40 / 48 | 800 | −0.03em |
| `headline-lg` | Playfair Display | 30 / 36 | 700 | −0.02em |
| `headline-md` | Playfair Display | 24 / 30 | 700 | −0.015em |
| `headline-sm` | Playfair Display | 20 / 26 | 600 | −0.01em |
| `stat-num` | Playfair Display | 28 / 32 | 800 | −0.02em |
| `body-lg` | Roboto Flex | 16 / 24 | 400 | +0.01em |
| `body-md` | Roboto Flex | 14 / 20 | 400 | +0.015em |
| `label-lg` | Roboto Flex | 14 / 20 | 600 | +0.02em |
| `label-md` | Roboto Flex | 12 / 16 | 700 | +0.06em |

DESIGN's 30 sp `headline-lg` supersedes the PRD's 32 px under §2.2. The PRD's "Inter/Sans-Serif" is
superseded by Roboto Flex, which both DESIGN sections name.

## 76.2 Article deck card

`24dp` radius, the derived `card` fill, and the ambient shadow in §16.2. The Playfair headline is the
dominant element and sits where the design sources' media slot would have been (§74.2).

Content and order per §24. Truncation per §13.2. **Placement per §21 and Amendment 7** — the operational
block follows the card.

## 76.3 Bottom navigation bar

Structure and order are §18.2's. Presentation: `28dp` top corner radius, the active destination marked by
a `tonal` pill indicator behind its icon, the destination label below.

## 76.4 Top app bar

Small, centred `Intentional Reading` brand title in the editorial register, with a trailing settings icon
(§20.2). It is the single masthead required by §2.3.

## 76.5 Action rail

The three controls of §31:

- **filled primary** — `Read article`, per §32.2;
- **two circular triage controls** — per §35.2, flanking or following the primary control;
- icon-only controls carry accessible names (§73).

## 76.6 StatBand

Android's presentation of the overview band authorised by §53 and §61: a **three-column pill container**,
`16dp` corners, the derived `container` fill, one column per value.

Numerals use `stat-num`; labels use `label-md`.

**Values and their absence are §53–§55's, unchanged.** Sum only known reading times, omit rather than show
`0 min`, omit an unavailable topic, never fabricate. A column with no value collapses to its label or is
omitted; it never shows a zero.

## 76.7 Modal bottom sheet

`28dp` top radius, surface-card toggles, a dimming scrim. Behaviour and contents per §64.

---

# 77. Android Colour Seeds

**Binds:** Android only.

## 77.1 The ten seeds

```text
bg  surface  fg  muted  border  primary  secondary  tonal  tertiary  error
```

Ten rather than the browser's six (§4.2) because Material 3 carries primary, secondary, tertiary and error
role families, and because the `tonal` container cannot be derived from any other seed (§77.2).

**These ten are the only authored colour values in the Android client.** Everything else is derived (§78).
No file outside the theme package may name a colour. An item that needs a role the theme does not define
reports it to the supervisor; it does not write a literal.

## 77.2 The two seeds settled by owner decision

`primary` and `tonal` were contested between the design sources. Both are settled by owner decision on
2026-09-01, as **named exceptions** to the precedence order in §2.2. The order continues to govern
everything it does not name here.

| Seed | Approved | Rejected | Why the exception |
|---|---|---|---|
| `primary` | `#1B2CC1` | `#00129A` | The approved value is what DESIGN's prose and the PRD both name for the filled primary control; the token block files it under `primary-container`. The precedence order would have selected `#00129A`, which is materially darker than the "high-intensity Electric Royal Blue" the brand section describes. Both clear AAA against white — 9.7:1 and 13.5:1 — so contrast did not decide it, and character did. |
| `tonal` | `#ABD2FA` | `#7692FF` | The approved value appears in DESIGN's **prose** and the PRD but in no token block. It is also the better value on contrast: with the Midnight Navy ink it reaches **8.6:1**, against **4.7:1** for the periwinkle, on a component that appears on every screen. |

**`tonal` requires its own seed and cannot be derived.** It sits at hue 249° against the periwinkle's
271° — a different hue family — so no lightness or chroma transform of another seed produces it. This is
why the seed set is ten rather than nine.

**`secondary` = `#3856BF`** is DESIGN's own token-block `secondary`, which nothing else was using. It
carries the triage-button outline, for the contrast reason in §35.2.

## 77.3 Prose colours, mapped

`m3-expressive-DESIGN.md`'s prose relies on colours its own token block does not carry. Under §2.2 they
are mapped onto roles rather than admitted as literals:

| Prose colour | Prose use | Bound to |
|---|---|---|
| `#ABD2FA` "Cerulean Mist" | tonal fill, active nav pill, tonal highlighting | the `tonal` seed (§77.2) |
| `#C3C9DA` | inactive chip outline | the control-boundary outline (§78.3) |
| `#EBF0F7` | StatBand container fill | the derived `container` role (§78.2) |
| `#F8FAFE` | canvas | the `bg` seed |
| `#7692FF` | triage-button outline | **not used** — fails §73.1; see §35.2 |

## 77.4 The approved light palette

Normative:

| Seed | Value | Source |
|---|---|---|
| `bg` | `#F7F9FD` | DESIGN `surface` / `background` |
| `surface` | `#FFFFFF` | DESIGN `surface-container-lowest` |
| `fg` | `#181C1F` | DESIGN `on-surface` |
| `muted` | `#454655` | DESIGN `on-surface-variant` |
| `border` | `#757686` | DESIGN `outline` |
| `primary` | `#1B2CC1` | owner decision (§77.2) |
| `secondary` | `#3856BF` | DESIGN `secondary` |
| `tonal` | `#ABD2FA` | owner decision (§77.2) |
| `tertiary` | `#212B56` | DESIGN `tertiary` — Midnight Navy |
| `error` | `#BA1A1A` | DESIGN `error` |

## 77.5 The dark scheme is derived, not authored

**The Android design sources ship no dark scheme.** Settings offers Light / Dark / System on both surfaces
(§64), so a light-only palette would regress a shipped control.

The dark scheme is derived from **the same ten seeds**, by the rule §6 already states for the browser:
**preserve each seed's hue and chroma, and move only lightness.** Dark is consistent with light by
construction rather than by review.

Neutral seeds take a fixed dark lightness ladder; brand seeds keep hue and chroma and rise or fall to an
accessible lightness:

| Seed | Dark treatment | Value |
|---|---|---|
| `bg` | L 0.145, C 0.025 | `#060A15` |
| `surface` | L 0.195, C 0.030 | `#0E1523` |
| `fg` | L 0.940, C 0.012 | `#E4ECF3` |
| `muted` | L 0.720, C 0.025 | `#A2A3B4` |
| `border` | L 0.340, C 0.030 | `#353647` |
| `primary` | hue and chroma held, L → 0.74 | `#6C9DFF` |
| `secondary` | hue and chroma held, L → 0.70 | `#7197FF` |
| `tonal` | hue and chroma held, L → 0.42 | `#2D5072` |
| `tertiary` | hue and chroma held, L → 0.30 | `#202A55` |
| `error` | hue and chroma held, L → 0.72 | `#FF6A5D` |

In the dark scheme the tonal container becomes dark and its ink becomes light, which is Material 3's own
inversion of the container/on-container pair. `tertiary` is a light-scheme shadow tint and is close to
unused in dark (§78.4).

---

# 78. Android Colour Derivation

**Binds:** Android only.

## 78.1 The rule

Every non-seed value is produced from the seeds by Oklch mixing — the same operation the browser expresses
as `color-mix(in oklch, A t%, B)` (§4.1), where `t` is the weight of `A`.

**One correctness rule that is not optional.** A near-achromatic colour has no meaningful hue; pure white's
is an artefact of rounding. Interpolating a saturated blue toward it drags the result off-hue and lands on
olive. **When one endpoint's chroma is negligible, it adopts the other endpoint's hue** rather than
contributing its own.

## 78.2 Derived roles

The rules are normative. The values are **informative** — they are what the rules produce, and a rounding
difference in implementation is not a specification violation.

| Role | Rule | Light | Dark |
|---|---|---|---|
| `card` | light: the `surface` seed. dark: `mix(fg 6%, surface)` | `#FFFFFF` | `#18202E` |
| `container` | light: `mix(primary 5%, bg)`. dark: `mix(fg 10%, surface)` | `#EAF0FB` | `#1F2735` |
| `primarySoft` | `mix(primary 12%, surface)`; 14% in dark | `#E0E8FA` | `#1A253D` |
| `quiet` | `mix(fg 78%, muted)` | `#20252A` | `#D4DCE5` |
| `outlineVariant` | the decorative hairline — §78.3 | `#CBCCDE` | `#434456` |
| `outlineControl` | the control boundary — §78.3 | `#757686` | `#A2A3B4` |
| `onPrimary` | whichever of white and the `bg` seed reads better on `primary` | `#FFFFFF` | `#060A15` |
| `onTonal` | whichever of the Midnight Navy ink and the light ink reads better on `tonal` | `#212B56` | `#E4ECF3` |

Where used: `card` is the deck card (§76.2); `container` is the StatBand and queue rows (§76.6, §58.2);
`primarySoft` is the content-type badge (§28.2); `quiet` is source names, triage glyphs and row action
labels.

The full Material 3 role set is produced from these; nothing in it is authored separately.

## 78.3 Two outline roles, and why they are not one

§73.1 requires a control's only boundary to reach **3:1** against the surface behind it, in both schemes,
while a decorative divider is exempt. One outline token cannot satisfy both, and three candidate values
failed the floor while looking acceptable:

| Candidate | Light, on the card | Dark, on the card |
|---|---|---|
| `mix(fg 30%, border)` — the shipped browser rule | too dark; reads as a heavy rule | — |
| a hairline one lightness step off the card | **1.6:1 — fails** | **1.7:1 — fails** |
| the `border` seed | 4.5:1 — passes | **1.4:1 — fails**, because in dark the border seed is darker than the card |
| the `muted` seed | 9.3:1 — passes | 6.6:1 — passes |

So there are two derived roles:

- **`outlineVariant`** — the **decorative hairline**. The `border` seed moved one `0.15` lightness step off
  the card: below it in light, above it in dark. Used for row dividers, StatBand column separators and
  topic-tag outlines (§27.2). Exempt from the 3:1 floor because it divides rather than identifies.
- **`outlineControl`** — the **control boundary**. Derived as *whichever seed among `border` and `muted`
  reaches at least 3:1 against the card in this scheme*. Light resolves to `border`; dark resolves to
  `muted`. Used for the unselected category chip (§22.2) and outlined row actions (§34.2).

The `1.5dp` triage-button outline uses the `secondary` seed rather than `outlineControl`, because the design
gives that control a brand-coloured ring and `secondary` clears the floor at 6.5:1 (§35.2).

**The derivation must be computed and asserted, not eyeballed.** Every value in the failing column above
looked fine on screen.

## 78.4 Shadow tint

The deck card's ambient shadow is tinted with the `tertiary` seed at approximately 10% opacity in the light
scheme (§16.2).

In the dark scheme the ambient shadow is near-black. A navy tint on a dark ground is indistinguishable from
black and costs a derivation for nothing.

---

# 79. Android Motion

**Binds:** Android only. **Bounded by §47 and §48, which are not Android-scoped.**

## 79.1 Directional destination transitions

Indexed to bottom-bar position, so this section is defined by §18's fixed destination order.

```text
duration   300ms
easing     Material 3 Emphasized
incoming   lateral slide, entering from the side the destination lies on
outgoing   subtle scale-down and fade to 0.8 opacity
```

Moving toward `Read Later` and moving toward `History` are opposite directions, because they sit on
opposite sides of `Discover`.

## 79.2 Modal sheet reveal

```text
entrance   350ms vertical slide-up with simultaneous fade-in, Decelerated (Out) curve
exit       reverse tuck — slide down and fade out
backdrop   semi-transparent scrim dimming the content behind
```

## 79.3 Reduced motion

**§48 binds this section without exception.** Under a reduced-motion preference both transitions above
become effectively immediate, and the destination change and the sheet's presence remain fully clear from
text, state and live status.

`reducedMotion` is already resolved in the composable that owns both the navigation scaffold and the
settings sheet, so both animations sit where the flag already is.

**A test must assert that each animation honours it.**

---

# 80. Concordance with the First Edition

**Binds:** nothing. This section is a finding aid.

Section numbers are unchanged, so every existing citation still resolves to its subject. What changed
inside each section is recorded here, so a reader arriving from an older document knows whether the rule
they were sent to still reads the way they expect.

| § | Subject | This edition |
|---|---|---|
| 1 | Purpose | Unchanged in substance; the visual direction is restated as the shared *product character*, with each surface expressing it in its own language. |
| 2 | Design authority | **Split.** 2.1 browser sources; 2.2 Android sources and their precedence order; 2.3 the one-masthead rule, unchanged. |
| 3 | Design principles | Unchanged. 3.2's "no article imagery" now cross-references §74.2. |
| 4 | Colour system | **Split.** The seed-and-derive discipline is stated once as shared; 4.1 six browser seeds, 4.2 ten Android seeds. |
| 5–6 | Browser theme tokens | Unchanged, now explicitly browser-scoped. §6's hue-and-chroma rule is identified as generalising to both surfaces. |
| 7 | Derived colour rules | Unchanged; 7.1's prohibitions are marked as binding both surfaces. |
| 8 | Accent usage | **Split.** 8.1 browser, accent restraint with ink-based navigation selection; 8.2 Android, tonal pill selection. Both express the same restraint. |
| 9–10 | Category identity, swipe colour | Unchanged, both surfaces. §10 is identified as an accessibility requirement. |
| 11 | Typography | **Split.** The two-register principle is stated once; 11.1 browser system stacks, 11.2 Android bundled Playfair Display and Roboto Flex. |
| 12 | Type scale | **Split.** 12.1 browser `clamp()` scale unchanged; 12.2 points to §76.1 and requires `sp`. |
| 13 | Text wrapping | **Rewritten, and the one place the surfaces genuinely conflict.** 13.1 keeps the browser's wrap-never-clamp rule verbatim. 13.2 introduces the Android 3-line/2-line clamp, with item 012's measurement as its justification. |
| 14 | Spacing | **Split.** 14.1 browser tokens unchanged; 14.2 Android rhythm. |
| 15 | Radii | **Split.** 15.1 browser unchanged; 15.2 Android. The apparent conflict with the Android queue row is resolved in §56.1. |
| 16 | Shadows | **Split.** 16.1 browser unchanged; 16.2 Android tonal-first hierarchy. The shared rule — one card shadow, no row shadow — is stated first. |
| 17 | Terminology | **Widened.** 17.1 destinations, unchanged. 17.2 now also carries the category labels, moved here from §22. |
| 18 | Mobile navigation | **Split.** Shared rules first, including the counts. 18.1 keeps the browser's 7px Discover lift; 18.2 drops it for Android and explains why. |
| 19 | Desktop navigation | Unchanged; now marked browser-only, as the Android client ships no desktop width. |
| 20 | Settings entry | **Split.** All substantive rules are shared; only the control's form differs. |
| 21 | Discover header | **Unchanged**, including Amendment 7. One sentence added distinguishing the licence from the constraint. |
| 22 | Category selector | **Split.** Options moved to §17.2. 22.2 subjects the unselected chip's outline to §73.1. |
| 23 | Discover composition | Unchanged. |
| 24 | Card anatomy | Unchanged; "no reserved image area" now cross-references §74.2. |
| 25 | Article title | **Narrowed.** Keeps the dominance and no-collision requirements; hands truncation to §13, where the surfaces differ. |
| 26 | Excerpt | **Split.** Shared empty-excerpt rules first; 26.1 browser 2–4 lines, 26.2 Android exactly 2. |
| 27 | Topic tags | **Split** on presentation. Adds that tags are not controls and so are exempt from §73.1 — conditional on their staying non-interactive. |
| 28 | Content-type badge | **Split** on presentation. The label-fidelity rule is unchanged and the real label list is extended to all ten shipped values. |
| 29–30 | Reading time, publication age | Unchanged, both surfaces. §29 is identified as the null rule for every place reading time appears. |
| 31 | Action rail | Unchanged. |
| 32–35 | Buttons and triage controls | **Split** on values. §35.2 records why the Android triage outline uses `secondary` rather than the design's prose value. |
| 36 | Destructive treatment | Unchanged; adds that Android's `error` seed is not Reset's styling. |
| 37 | Control states | **Renamed and widened** from "Button Active State". 37.1 keeps the browser's 1px nudge; 37.2 adds the Android pressed and disabled treatments. |
| 38 | Focus treatment | Unchanged, both surfaces. |
| 39–43 | Swipe interaction | **Unchanged, both surfaces.** Behaviour. |
| 44 | Swipe motion | **Split** on curve only. The character constraint is stated as binding both surfaces and non-negotiable. |
| 45 | Toast | **Split** on transition values. Duration, focus rule and live-status semantics are shared. Adds that the Android toast is globally hosted. |
| 46 | Control transitions | **Split** on values; the no-ornamental-transitions rule is shared. |
| 47 | Prohibited motion | **Unchanged**, and explicitly binding on the Android motion system. |
| 48 | Reduced motion | **Widened, both surfaces.** Now enumerates the Android transitions it covers and requires a test. |
| 49 | Keyboard controls | Unchanged; scoped to the browser and any surface with a physical keyboard. |
| 50–51 | Read article, return state | **Unchanged, both surfaces.** §51 adds the standing "is what the reader needs next on screen?" question. |
| 52 | Read Later screen | **Split** on header presentation. Adds explicitly that Amendment 7's ordering applies to Discover only. |
| 53–55 | Overview band and its values | **Unchanged, both surfaces.** §53 now states why it is not the metric grid §74 prohibits. §54 is marked as already-decided. |
| 56 | Read Later rows | **Widened.** Content and actions unchanged. New 56.1 carries the shared no-raised-card rule and shows how each surface satisfies it — this is where §15/§57's apparent conflict with the Android queue row is resolved. |
| 57 | Desktop rows | Unchanged; now browser-only, with the shared rule moved up to §56.1. |
| 58 | Mobile rows | **Split.** 58.1 browser unchanged; 58.2 Android tonal container, no thumbnail, 2-line clamp. |
| 59 | Empty Read Later | Unchanged. |
| 60–62 | History | Unchanged, both surfaces. §62 marks Mark unread's count behaviour as behaviour. |
| 63 | Empty History | Unchanged; notes Android's high-fidelity composition says the same thing at more visual weight. |
| 64 | Settings | **Split** on container. All behavioural requirements shared. Adds why both schemes are mandatory. |
| 65–66 | Import, export, reset | Unchanged; §65 adds that a refused import must say why. |
| 67–69 | Loading, empty Discover, dataset failure | Unchanged. |
| 70 | Toast, live status, Undo | **Unchanged**, including Amendment 8, restated in full rather than by reference. |
| 71 | Widths | **Split.** 71.1 keeps the browser's ten widths; 71.2 gives Android its five and marks 360 dp non-optional. |
| 72 | Touch targets | **Split** on floors. The no-hover rule and the visible-versus-target distinction are shared. 72.2 flags the 40 dp chip as needing a 48 dp target. |
| 73 | Accessibility acceptance | **Widened, both surfaces.** New 73.1 makes the control-boundary contrast floor explicit. |
| 74 | Visual non-goals | **Widened.** 74.1 keeps the imagery prohibition; new 74.2 records that it is a frozen-contract boundary, what the design sources asked for, and the layout obligation that follows. |
| 75 | Completion criteria | **Widened.** New 75.1 states behaviour parity is required and visual parity is not; new 75.2 carries the shared-copy rule. |
| 76–79 | — | **New.** Android type scale and component presentation, colour seeds, colour derivation, motion. |
| 80 | — | **New.** This table. |

---

# Amendment Record

| Amendment | Effect on this document |
|---|---|
| **6** — Native Android Client Authorization | Admitted the Android client as a second surface consuming the frozen dataset contract read-only. This edition is the consequence: §0's two-surface structure exists because Amendment 6 created a surface this document had not been told apart from the browser. |
| **7** — Discover Composition Ordering | §21. Unchanged by this edition. |
| **8** — Undo Scope: Reversible Actions and Offer Surfaces | §70, and the swipe-scoped wording it superseded throughout. Unchanged by this edition. |
| **9** — Android Client Visual Direction: Material 3 Expressive | This edition. Authorised the Android visual direction, the reorganisation above, and §§76–79. Changed no contract, no state, no status, no learning behaviour, no count, no ranking, no undo path, no gesture semantic, no keyboard binding and no authored string. |
