# Intentional Reading — V1 UI and UX Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/06-ui-ux.md`\
**Role:** Authoritative visual, responsive, interaction, accessibility, navigation, and presentation specification\
**Visual source of truth:** `design-reference/DESIGN.md` and `design-reference/intentional-reading-prototype.png`

---

## 1. Purpose

This document defines the V1 user interface and interaction behavior for Intentional Reading.

It translates:

- the approved product specification;
- the approved Open Design design system;
- the shared application contracts;
- the responsive and accessibility requirements

into implementation requirements for the Frontend UI workstream.

V1 must not be visually reinterpreted as:

- a generic SaaS dashboard;
- a minimalist developer demo;
- Hacker News;
- a Tinder clone;
- a social-media feed;
- a card-heavy administration interface.

The approved visual direction is:

> Editorial publication + premium reading application + restrained tactile triage.

---

# 2. Design Authority

Implementation must use the following visual references:

```text
design-reference/DESIGN.md
design-reference/intentional-reading-prototype.png
```

`DESIGN.md` owns exact design-system values.

The screenshot is the approved visual reference for:

- overall character;
- visual density;
- typography relationship;
- spatial composition;
- card presence;
- desktop tone.

The screenshot is not authoritative for:

- example article content;
- prototype publisher names;
- example counts;
- fabricated topic values;
- a repeated masthead or Settings control caused by illustrative stitching/reference composition.

Actual application content comes from the V1 Article and state contracts.

Each rendered application view has exactly one application masthead/header. There must not be a duplicated `Intentional Reading` masthead or duplicated Settings control. The prototype remains authoritative for overall visual intent, but any apparent repeated masthead/Settings treatment is an artifact rather than a product requirement.

---

# 3. Design Principles

V1 follows five primary interface principles.

## 3.1 Attention is finite

Every screen should help the user:

```text
choose
read
leave
```

Avoid:

- infinite-feed cues;
- streaks;
- engagement counters;
- reward effects;
- celebratory animation;
- urgency;
- artificial continuation prompts.

---

## 3.2 Text is the visual material

The application has no article imagery.

Visual hierarchy comes from:

- typography;
- spacing;
- rules;
- surfaces;
- metadata;
- tags;
- restrained accent use.

Article thumbnails, stock imagery, generated imagery, and decorative content photography are prohibited.

---

## 3.3 Editorial, not administrative

Prefer:

```text
editorial rhythm
typographic contrast
rules
quiet surfaces
intentional whitespace
```

over:

```text
dashboard grids
metric cards
dense control panels
enterprise UI patterns
```

---

## 3.4 Gestures always have equivalents

Swipe is a fast interaction, never the only interaction.

Every swipe action must also exist as:

- a visible control;
- a keyboard action.

---

## 3.5 Queue state stays honest

Read Later and History counts must update immediately after successful state transitions.

No decorative or approximate counts are permitted.

---

# 4. Color System

V1 uses six authored color tokens per theme.

Derived colors must use:

```css
color-mix(in oklch, ...)
```

rather than introducing arbitrary independent palette values.

---

# 5. Light Theme Tokens

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

The light-theme accent corresponds exactly to:

```text
#0B2D72
```

---

# 6. Dark Theme Tokens

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

Dark mode preserves the accent hue/chroma while increasing lightness for accessible contrast.

---

# 7. Derived Color Rules

Derived values may include concepts such as:

```text
accent-soft
surface-hover
strong-border
quiet-ink
toast-surface
```

but must derive from the authored six-token system.

Do not create:

- independent rainbow category palettes;
- neon positive/negative swipe colors;
- unrelated one-off hexadecimal values across components.

---

# 8. Accent Usage

The accent is intentionally restrained.

Use it primarily for:

- content-type badge treatment;
- primary Read Article action;
- focus indication;
- subtle active-category treatment.

Navigation selection uses primary ink rather than accent color.

---

# 9. Category Identity

Categories may receive restrained emphasis through:

```text
accent-soft
type treatment
border treatment
```

but V1 must not create a seven-color category rainbow.

Topic tags remain neutral.

---

# 10. Swipe State Color Semantics

Saving and rejection must not depend on green/red alone.

State communication requires:

```text
direction
text
icon/arrow
motion
```

Example:

```text
← Not interested

Save for later →
```

Color may support the state but must not be the sole signal.

---

# 11. Typography

## Display stack

```css
font-family:
  "Iowan Old Style",
  Charter,
  "Palatino Linotype",
  Georgia,
  serif;
```

## Body/UI stack

```css
font-family:
  "Avenir Next",
  Avenir,
  "Segoe UI",
  system-ui,
  sans-serif;
```

## Metadata stack

```css
font-family:
  SFMono-Regular,
  Consolas,
  monospace;
```

No web-font downloads are used.

---

# 12. Typography Scale

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

---

# 13. Text Wrapping

Use balanced heading wrapping where supported:

```css
text-wrap: balance;
```

Use readable paragraph wrapping where supported:

```css
text-wrap: pretty;
```

Never force article titles to one line.

Long real-world titles must remain readable without breaking layout.

---

# 14. Spacing System

Base unit:

```text
4px
```

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

---

# 15. Radii

Approved radii:

```text
controls       10px
article card   20px
dialogs        20px
pills/tags     fully rounded
```

Do not apply large rounded-card styling indiscriminately to every section.

Read Later and History rows specifically use rules rather than floating-card containers.

---

# 16. Shadows

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

Reading-list rows use no enclosing card shadow.

---

# 17. Primary Navigation Terminology

The exact user-facing primary destinations are:

```text
Read Later
Discover
History
```

Do not use:

```text
Read
Saved
Queue
Archive
```

as replacements for these primary destination labels.

---

# 18. Mobile Navigation

Mobile uses a fixed bottom navigation bar.

Exact order:

```text
Read Later | Discover | History
```

Discover:

- remains in the center;
- sits approximately 7px higher than adjacent destinations;
- acts as the visual anchor.

Read Later and History display counts.

Every navigation target must be at least:

```text
54px high
```

---

# 19. Desktop Navigation

At desktop widths, navigation becomes a sticky top treatment.

Semantic order remains:

```text
Read Later | Discover | History
```

Navigation is positioned between:

- application identity/brand;
- Settings control.

Current page is indicated with an ink underline or equivalent approved treatment.

Discover remains visually primary without looking like a floating mobile button.

---

# 20. Settings Entry

Settings is represented by a:

```text
44px circular gear control
```

It is not a primary navigation destination.

Settings opens a compact modal/dialog.

It must not become:

- a full-page dashboard;
- a fourth navigation tab.

---

# 21. Discover Header

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

The masthead should feel like publication masthead/content framing rather than dashboard chrome, and framing placed below its subject is still framing. A redesign may re-lay out either half provided the card still leads the viewport.

*Amended by Amendment 7.*

---

# 22. Category Selector

Available options:

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

Minimum control height:

```text
40px
```

Default/unselected state:

```text
neutral outline
```

Selected state:

```text
ink fill
surface-colored text
```

Category selector must remain keyboard accessible.

---

# 23. Discover Composition

Discover shows exactly one primary article card as the decision surface.

A subtly offset secondary neutral card may appear behind it to communicate:

```text
there is a finite queue
```

without creating:

- a visible infinite stack;
- gambling/deck gamification;
- exaggerated Tinder aesthetics.

---

# 24. Article Card Anatomy

The Discover card contains, in order:

1. source;
2. publication age/date;
3. content-type badge;
4. category context where appropriate;
5. article title;
6. excerpt;
7. topic tags;
8. action rail.

The card must remain text-first.

No reserved image area exists.

---

# 25. Article Title

The title is the dominant visual element.

Requirements:

- display serif;
- no fixed one-line treatment;
- no arbitrary hard two-line clamp;
- supports very long titles;
- remains balanced where browser support allows;
- does not collide with metadata or actions.

---

# 26. Excerpt

Discover displays approximately:

```text
2–4 lines
```

when content is available.

At very narrow phone widths:

```text
up to 3 lines may be visually preferred
```

At normal mobile widths:

```text
up to 4 lines
```

If the Article excerpt is empty:

- omit the excerpt area gracefully;
- do not insert filler text;
- do not display "No description available."

---

# 27. Topic Tags

Normal Discover presentation displays at most:

```text
5 tags
```

At very narrow widths:

```text
prioritize first 3
```

Topic tags use:

```text
12px body text
neutral border
muted text
no fill
fully rounded shape
```

Tags are informative, not interactive filters in V1 unless explicitly added later.

---

# 28. Content-Type Badge

Content-type badge uses:

```text
10px uppercase mono
accent-soft background
accent text
```

Examples:

```text
STANDARDS UPDATE
ENGINEERING DEEP DIVE
REPORTED SCIENCE
OFFICIAL RELEASE NOTES
```

The UI displays the authoritative Article `contentType.label`.

It must not infer a different label.

---

# 29. Reading Time

When:

```text
readingTimeMinutes != null
```

the UI may display:

```text
~7 min
```

or an equivalent concise editorial format.

When null:

- omit reading-time metadata;
- do not display zero;
- do not estimate locally.

---

# 30. Publication Age

When `publishedAt` exists, the UI may display an understandable relative age such as:

```text
5h
2d
12d
```

or an appropriately formatted date for older material.

When publication date is unknown:

- omit the age/date;
- do not use current dataset generation time as article age.

---

# 31. Discover Action Rail

The card's primary action rail provides:

```text
Not interested
Read article
Save for later
```

The actions correspond exactly to:

```text
dismiss
open
save
```

The explicit controls must remain usable independently of swipe gestures.

---

# 32. Primary Button

Use the primary accent-filled button for the main:

```text
Read article ↗
```

action.

Minimum height:

```text
48px
```

Use only one visually dominant primary action per screen/primary content surface.

---

# 33. Secondary Buttons

Secondary controls use:

```text
transparent background
border
```

Hover:

```text
soft ink-derived surface
stronger border
```

---

# 34. Ghost/Text Actions

Read Later and History compact row actions may use ghost/text treatment.

On hover:

```text
underline or clear text-state indication
```

Touch dimensions must remain accessible even if the visible treatment is compact.

---

# 35. Round Triage Controls

Compact triage controls may use:

```text
48px circular outlined buttons
```

with directional arrow icons and accessible labels.

They must not replace the labeled semantic understanding of the action.

---

# 36. Destructive Treatment

Reset and similar destructive actions use restrained styling.

Default:

```text
transparent
ink/border treatment
```

Hover:

```text
ink fill
surface text
```

Do not introduce alarming neon-red visual treatment inconsistent with the editorial design.

---

# 37. Button Active State

Pressed controls move approximately:

```text
1px downward
```

to provide restrained tactile feedback.

---

# 38. Focus Treatment

Every focusable control must display a visible focus ring.

Target:

```text
3px accent-derived ring
3px offset
```

Focus must remain obvious in both light and dark themes.

---

# 39. Swipe Interaction Surface

The article card may be dragged horizontally from non-interactive card areas.

Do not start card dragging from:

- buttons;
- links;
- category selector;
- other interactive descendants.

Use pointer events where practical to unify:

- touch;
- stylus;
- mouse.

---

# 40. Swipe Threshold

Commit threshold:

```text
90px horizontal travel
```

On pointer release:

```text
abs(horizontalTravel) >= 90px
→ commit corresponding action
```

Below threshold:

```text
snap/return to default
no state change
no preference signal
```

---

# 41. Swipe Left

Dragging left:

- translates card left;
- applies slight counterclockwise rotation;
- displays a clear directional cue:

```text
← Not interested
```

Release beyond threshold emits:

```text
dismiss
```

---

# 42. Swipe Right

Dragging right:

- translates card right;
- applies slight clockwise rotation;
- displays:

```text
Save for later →
```

Release beyond threshold emits:

```text
save
```

---

# 43. Swipe Commitment

After a committed swipe:

1. active card exits briefly;
2. state action is processed;
3. next eligible card appears;
4. toast announces result;
5. Undo becomes temporarily available.

The UI must not visually finalize a save/dismiss if persistence failed.

---

# 44. Swipe Motion

Default card response:

```text
280ms
cubic-bezier(0.2, 0.8, 0.2, 1)
```

Motion should feel:

```text
tactile
quiet
controlled
```

not:

```text
bouncy
playful
rewarding
```

---

# 45. Toast

Toast transition:

```text
180ms
opacity + subtle vertical movement
```

Examples:

```text
Saved to Read Later — Undo
Not interested — Undo
```

Toast remains visually available for approximately:

```text
4.5 seconds
```

Use:

```html
role="status"
aria-live="polite"
```

or equivalent accessible status semantics.

---

# 46. Button/Color Transitions

Normal hover/focus color transitions:

```text
80–160ms
```

Avoid slow ornamental transitions.

---

# 47. Prohibited Motion

Do not use:

- bounce;
- confetti;
- pulse;
- autoplay;
- reward animation;
- celebratory motion;
- continuous card movement.

---

# 48. Reduced Motion

Under:

```css
@media (prefers-reduced-motion: reduce)
```

remove or effectively eliminate:

- card rotation;
- large swipe exit transitions;
- decorative translation.

Transitions/animations should become effectively immediate.

The semantic swipe/control outcome must remain clear through:

- text;
- state changes;
- live status.

---

# 49. Keyboard Controls

Discover supports:

```text
Left Arrow   → Not interested
Right Arrow  → Save for later
Z            → Undo
```

Keyboard shortcuts must not fire when doing so would conflict with focused:

- form controls;
- dialog controls;
- category selection;
- other interactive contexts.

---

# 50. Read Article Interaction

`Read article`:

- visibly includes external-navigation indication;
- emits `open`;
- attempts Open-state local persistence first;
- opens the publisher in a new tab/window after the persistence attempt whether that attempt succeeds or fails;
- does not mark the article read.

When Open persistence fails, show an accessible local-state warning and do not claim that the Open interaction was persisted. The publisher navigation must still proceed so reading remains available.

For persistent queue/state actions such as Save, Dismiss, Mark Read, Mark Unread, Remove, Import, and Reset, do not visually claim a successful state transition when persistence fails.

The external marker may use:

```text
↗
```

or the approved equivalent.

---

# 51. Opened Article Return State

When the user returns after opening an article, the application may visibly acknowledge that the article has been opened.

The UI should support clear next choices such as:

```text
Mark read
Save for later
Not interested
```

according to current state/integration.

Do not automatically assume completion.

---

# 52. Read Later Screen

Read Later uses an editorial list, not swipe cards.

It begins with the same overall header rhythm as Discover:

```text
mono eyebrow
serif H1
short muted description
secondary navigation/action
```

---

# 53. Read Later Overview Band

Read Later may display a three-part overview band containing:

1. queue count;
2. summed known estimated reading time;
3. next/top queue topic.

Values must come from current local state.

Do not fabricate values.

---

# 54. Read Later Reading-Time Summary

Sum only saved Articles whose:

```text
readingTimeMinutes != null
```

If no saved item has known duration:

```text
display unavailable/omit value
```

Do not display:

```text
0 min
```

as though every unknown article takes zero time.

---

# 55. Read Later Topic Summary

Use the first/default queue Article with at least one tag.

Display:

```text
article.tags[0].label
```

If no saved Article contains tags:

- omit the topic value;
- do not infer one in UI.

---

# 56. Read Later Rows

Each row displays, as appropriate:

- queue position;
- saved age;
- source;
- category;
- reading time when known;
- content type;
- title;
- up to three topic tags.

Actions:

```text
Read
Mark read
Remove
```

---

# 57. Read Later Desktop Rows

Desktop row structure uses approximately three semantic regions:

```text
queue position / date
article information
right-aligned actions
```

Rows are separated by rules.

Do not wrap every row in its own raised shadow card.

---

# 58. Read Later Mobile Rows

Mobile rows collapse to one column.

Actions align left.

Interactive actions remain at least:

```text
44px
```

in usable touch size.

---

# 59. Empty Read Later

Empty-state messaging should explain:

> Save worthwhile articles from Discover to build your reading queue.

Provide one clear route back to:

```text
Discover
```

Avoid guilt language.

---

# 60. History Screen

History follows the same editorial header rhythm.

It presents read Articles chronologically.

Group headings:

```text
Today
Yesterday
Earlier
```

according to the user's local display date.

---

# 61. History Overview

Where the approved design includes an overview band, values derive from local History.

Potential values:

- History count;
- summed known reading time;
- latest topic.

Do not infer values unavailable from Article snapshots.

---

# 62. History Rows

Display:

- read date/time context;
- category;
- source;
- content type;
- title;
- reading time when available.

Actions:

```text
Reopen
Mark unread
```

Mark unread returns the Article to Read Later and updates both counts immediately after the successful transition.

---

# 63. Empty History

Explain that Articles appear after the user marks them read. Provide a clear route to Discover or Read Later without guilt, streak, or urgency language.

---

# 64. Settings Dialog

Settings contains only V1 controls:

- appearance: Light, Dark, System;
- export local data;
- import local data;
- reset local data;
- optional debug visibility where approved for development.

The dialog must have an accessible name, trapped focus while open, Escape dismissal, focus restoration, and a visible close control.

---

# 65. Import and Export Presentation

Export produces a user-downloadable JSON backup.

Import requires explicit file selection, validates before replacement, reports errors without destroying current state, and asks for confirmation before replacing valid existing state.

Do not display raw local-state contents unnecessarily.

---

# 66. Reset Presentation

Reset requires explicit confirmation and clearly states that preferences, Read Later, History, dismissals, and settings will be cleared on this device.

The destructive action remains visually restrained but unmistakable through wording and confirmation.

---

# 67. Loading State

Loading should preserve the editorial composition and announce status accessibly. Avoid fake article text that could be mistaken for real content.

---

# 68. Empty Discover State

When no eligible Articles remain for the selected category:

- say so truthfully;
- offer another category or All;
- keep navigation and Settings available;
- do not invent recommendations;
- do not create infinite-feed behavior.

---

# 69. Dataset Failure State

When the static dataset cannot be loaded or validated, show a concise recoverable error with a retry action. Existing persisted Read Later and History snapshots remain available where possible.

---

# 70. Toast and Live Status

Successful save, dismiss, read/unread, import, export, and reset outcomes are announced through an appropriate live region. Toasts must not steal focus.

Undo is offered for the most recent eligible action, from whichever surface performed it, and remains available for approximately the approved toast duration. The eligible set is in `contracts.md` §23.

*Amended by Amendment 8.*

---

# 71. Responsive Widths

The implementation must be verified at:

```text
360
390
430
600
768
820
1024
1366
1440
1920
```

pixels wide.

At every width:

- no horizontal page scrolling;
- readable titles and excerpts;
- reachable actions;
- no collision with fixed navigation;
- dialogs fit the viewport;
- long real-world content wraps safely.

---

# 72. Touch and Pointer Targets

Primary touch controls must meet the design-specified minimums. Compact row actions must retain at least a `44px` usable target, and mobile navigation targets at least `54px` height.

Hover cannot be required to discover or operate an action.

---

# 73. Accessibility Acceptance

V1 requires:

- semantic landmarks and headings;
- keyboard-complete operation;
- visible focus in both themes;
- meaningful accessible names;
- non-color-only state communication;
- sufficient text/control contrast;
- reduced-motion support;
- truthful live-status announcements;
- focus restoration for dialogs;
- labels for icon-only controls.

---

# 74. Visual Non-Goals

Do not add article imagery, engagement metrics, streaks, infinite scroll, category rainbows, dashboard metric grids, celebratory effects, or gesture-only actions.

---

# 75. UI Completion Criteria

The UI is complete when the approved design tokens and editorial character are preserved; Discover, Read Later, History, and Settings implement their defined states; mouse, touch, and keyboard paths are equivalent; counts remain immediately truthful; both themes and reduced motion work; and all required widths pass without horizontal scrolling.
