# Intentional Reading — Design System

## Design principles

1. **Attention is finite.** Every screen should help the reader choose, read, or leave. Avoid streaks, infinite-feed cues, celebratory effects, and engagement metrics.
2. **Text is the visual material.** Titles, excerpts, source names, and rhythm carry the hierarchy. No article images are required.
3. **Editorial, not administrative.** Use rules, typographic contrast, and quiet surfaces instead of dashboard grids or oversized utility cards.
4. **Gestures have equivalents.** Swipe is fast, but every gesture also has a labeled, keyboard-accessible control.
5. **The queue stays honest.** Counts reflect the local queue and reading history immediately after every state change.

## Color tokens

The palette is built around the user's favorite blue, `#0B2D72`, whose exact OKLCH equivalent is `oklch(0.322424 0.12543 262.24)`. The six source tokens are the only authored palette values per theme. Derived colors must use `color-mix(in oklch, …)`.

### Light

```css
--bg: oklch(0.972 0.008 255);              /* cool paper */
--surface: oklch(0.995 0.003 255);         /* lifted reading surface */
--fg: oklch(0.205 0.03 260);               /* blue-black ink */
--muted: oklch(0.49 0.025 260);            /* secondary copy */
--border: oklch(0.875 0.018 260);          /* blue-gray rules */
--accent: oklch(0.322424 0.12543 262.24);  /* exact #0B2D72 */
```

### Dark

```css
--bg: oklch(0.145 0.025 260);              /* midnight blue-black */
--surface: oklch(0.195 0.03 260);          /* raised reading surface */
--fg: oklch(0.94 0.012 255);               /* cool paper text */
--muted: oklch(0.72 0.025 255);            /* secondary copy */
--border: oklch(0.34 0.03 260);            /* low-glare blue rule */
--accent: oklch(0.74 0.12543 262.24);      /* accessible lightness shift of brand blue */
```

- The light theme uses `#0B2D72` exactly. Dark mode preserves its hue and chroma while increasing lightness for accessible contrast.
- The accent is reserved for the content-type badge and the main “Read article” action on Discover.
- Navigation selection uses ink, not the accent.
- Rejection and saving use ink plus directional text and arrows. Neither state relies on color alone.
- Category identity is restrained: use accent-soft for the active article category and neutral outlined tags for topics. Do not create a rainbow category system.

## Typography

- **Display:** Iowan Old Style → Charter → Palatino Linotype → Georgia → serif.
- **Body/UI:** Avenir Next → Avenir → Segoe UI → system UI → sans-serif.
- **Metadata:** SFMono-Regular → Consolas → monospace.
- H1: `clamp(34px, 6vw, 56px)`, 0.98 line-height, -0.035em tracking.
- H2: `clamp(28px, 4vw, 40px)`, 1.05 line-height, -0.025em tracking.
- H3/list title: 21–27px, 1.12 line-height.
- Article title: `clamp(30px, 5.2vw, 47px)`; on phone, `clamp(30px, 9vw, 41px)`.
- Body: 16px/1.55. Excerpt: display serif, 18px/1.48. Metadata: 12px uppercase mono.
- Use balanced heading wrapping and pretty paragraph wrapping. Never force titles to one line.

## Spacing scale

Base unit: 4px, with an 8-point primary rhythm.

| Token | Value | Use |
|---|---:|---|
| XS | 8px | compact inline gaps |
| SM | 12px | controls and tag groups |
| MD | 20px | component spacing |
| LG | 32px | section groups |
| XL | 56px | desktop columns |
| 2XL | 96px | major separation |

Page gutters use `clamp(18px, 4vw, 36px)`.

## Radii and shadows

- Controls: 10px.
- Primary article card and dialogs: 20px.
- Pills and tags: fully rounded.
- Article card: `0 1px 0` hairline plus `0 18px 50px` soft warm-ink shadow at 11%.
- Floating toast/dialog: `0 14px 35px` warm-ink shadow at 16%.
- Reading-list rows use rules and no surrounding card shadow.

## Article card anatomy

1. Source, age, and content-type badge.
2. Display-serif title with no fixed line count.
3. Two-to-four-line excerpt; mobile may visually clamp at four lines and 390px at three lines.
4. Up to five neutral topic tags; very narrow screens prioritize the first three.
5. Bottom action rail: Not interested, Read article, Save for later.
6. A slightly offset neutral card behind the active card suggests a finite queue without gamification.

The entire card can drag horizontally except from interactive descendants. At 90px horizontal travel, release commits the action. Before release, the card rotates slightly and shows a bordered text-and-arrow cue.

## Navigation anatomy

- **Mobile:** fixed bottom bar, exact order Read Later / Discover / History. Discover sits 7px higher and is the visual anchor. All targets are at least 54px high. Counts belong to Read Later and History.
- **Desktop:** sticky top bar with the same semantic order centered between brand and Settings. Current page uses an ink underline.
- Settings is a 44px circular gear control and opens a compact modal, never a full navigation destination.

## Buttons

- **Primary:** accent fill, surface text, minimum 48px height. Use once per screen for the main action.
- **Secondary:** transparent, border-color border; hover adds a soft ink surface and stronger border.
- **Ghost/text:** transparent with underline on hover; used for queue row actions.
- **Round triage:** 48px circular outlined controls with directional arrow icons and accessible labels.
- **Destructive:** transparent ink border; hover inverts to ink background and surface text.
- Active state moves down 1px. Every focusable control gets a 3px accent-derived focus ring with 3px offset.

## Tags and badges

- Content-type badge: 10px uppercase mono, accent-soft background, accent text.
- Topic tag: 12px body, neutral border, muted text, no fill.
- Category selector: 40px minimum height, neutral outline; selected state uses ink fill with surface text.
- Keep tag copy short and show no more than five on Discover.

## Reading-list and history rules

- Both screens begin with the same editorial header rhythm as Discover: mono eyebrow, serif H1, short muted description, and one secondary navigation action.
- Use a three-part overview band with strong top rule and light internal dividers. Values must be calculated from the current local articles: queue/history count, summed estimated reading time, and next/latest topic.
- **Read Later:** show numbered queue positions, saved age, source, category, reading time, content type, title, and up to three topic tags. The compact row actions are Read, Mark read, and Remove.
- **History:** group chronological entries under Today, Yesterday, and Earlier. Show group counts, date read, category, source, content type, title, and estimated reading time. Actions are Reopen and Mark unread.
- Desktop rows use three columns: date or queue position, article information, and right-aligned actions.
- Mobile rows collapse to one column; actions align left and remain at least 44px high. The overview band becomes two columns with its final value spanning the full row at 600px and below.
- Read Later and History each have a dedicated empty state with one clear route back into the reading flow.

## Responsive breakpoints

- **320–390px:** compact card padding, three visible tags, three-line excerpt, persistent bottom navigation.
- **391–920px:** one-column mobile composition, four-line excerpt, bottom navigation.
- **921–1180px:** desktop navigation and three-part Discover layout; side rails may be compact.
- **1181px+:** maximum content width 1180px, centered; the article card never stretches to the full viewport.
- Verify at 360, 390, 430, 600, 768, 820, 1024, 1366, 1440, and 1920px. No horizontal scrolling is allowed.

## Interaction states

- **Default:** card square to the page with no state label.
- **Dragging left:** translate with slight counterclockwise rotation; show “Not interested” plus left arrow.
- **Dragging right:** translate with slight clockwise rotation; show “Save for later” plus right arrow.
- **Committed swipe:** card exits briefly; next card replaces it; toast offers Undo for 4.5 seconds.
- **Hover:** strengthen border or add a soft ink background; never reduce text contrast.
- **Active:** 1px press movement for buttons.
- **Loading:** quiet inline spinner with “Gathering a thoughtful queue…” copy.
- **Feed error:** explain that saved reading remains available and offer one retry action.
- **No new articles:** explicitly give permission to leave; offer Read Later as a secondary action.
- **Empty Read Later:** explain how to save an article and return to Discover.

## Animation guidelines

- Card response: 280ms, cubic-bezier(0.2, 0.8, 0.2, 1).
- Toast: 180ms opacity and vertical movement.
- Button/color transitions: 80–160ms.
- No bounce, confetti, pulse, autoplay, or reward animation.
- Under `prefers-reduced-motion: reduce`, remove card transforms, shorten all transitions/animations to effectively instant, and use immediate scrolling.

## Accessibility rules

- Never require a gesture: left/right buttons and keyboard arrows mirror swipes; Z triggers Undo.
- Touch targets are at least 44px; primary triage controls are 48px.
- Focus is always visible and never conveyed by color alone.
- Swipe cues include text and directional icons.
- Counts update in navigation and headings; the transient toast uses `role="status"` and `aria-live="polite"`.
- Dialogs use native `<dialog>` behavior and explicit close controls.
- Body copy and controls must meet WCAG AA contrast; normal text targets 4.5:1 and icons/large text 3:1.
- External links visibly include the ↗ marker and open in a new tab.
- Import errors are reported through the live status surface.

## Settings and debug

- Settings contains a compact Light / Dark / System appearance control, followed by Export local data, Import local data, and Reset all data.
- Theme preference persists locally. System tracks `prefers-color-scheme` changes, and reset returns appearance to System.
- Developer details are collapsed by default and visually subordinate to the article. It may display Base score, Source preference, Topic preference, Exploration boost, Final score, and detected tag count.
- Ranking values must never appear in the normal production surface. The prototype’s state simulator lives inside the developer details disclosure only.
