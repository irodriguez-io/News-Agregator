---
name: Material 3 Expressive News
colors:
  surface: '#f7f9fd'
  surface-dim: '#d8dade'
  surface-bright: '#f7f9fd'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f8'
  surface-container: '#eceef2'
  surface-container-high: '#e6e8ec'
  surface-container-highest: '#e0e3e6'
  on-surface: '#181c1f'
  on-surface-variant: '#454655'
  inverse-surface: '#2d3134'
  inverse-on-surface: '#eff1f5'
  outline: '#757686'
  outline-variant: '#c5c5d7'
  surface-tint: '#3c4cdb'
  primary: '#00129a'
  on-primary: '#ffffff'
  primary-container: '#1b2cc1'
  on-primary-container: '#a4acff'
  inverse-primary: '#bdc2ff'
  secondary: '#3856bf'
  on-secondary: '#ffffff'
  secondary-container: '#7692ff'
  on-secondary-container: '#002683'
  tertiary: '#212b56'
  on-tertiary: '#ffffff'
  tertiary-container: '#38426e'
  on-tertiary-container: '#a5b0e3'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dfe0ff'
  primary-fixed-dim: '#bdc2ff'
  on-primary-fixed: '#000865'
  on-primary-fixed-variant: '#1e2fc3'
  secondary-fixed: '#dde1ff'
  secondary-fixed-dim: '#b7c4ff'
  on-secondary-fixed: '#001552'
  on-secondary-fixed-variant: '#193ca7'
  tertiary-fixed: '#dde1ff'
  tertiary-fixed-dim: '#bac4f9'
  on-tertiary-fixed: '#0c1843'
  on-tertiary-fixed-variant: '#3a4471'
  background: '#f7f9fd'
  on-background: '#181c1f'
  surface-variant: '#e0e3e6'
typography:
  display-lg:
    fontFamily: Playfair Display
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  headline-lg:
    fontFamily: Playfair Display
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Playfair Display
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Playfair Display
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.01em
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.015em
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.06em
  stat-num:
    fontFamily: Playfair Display
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 32px
    letterSpacing: -0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 1.125rem
  margin-tablet: 1.5rem
  gutter: 1rem
  section-gap: 2rem
  stack-gap: 0.75rem
  base-unit: 0.25rem
---

## Brand & Style

The design system moves away from utilitarian minimalism toward a high-energy, tactile editorial experience. It is designed for modern Android surfaces, leveraging Material 3 "Expressive" principles to create a sense of depth, momentum, and premium curation. 

The brand personality is **authoritative yet vibrant**, pairing the intellectual weight of a classic broadsheet with the fluid responsiveness of a mobile-first startup. The interface should feel like a physical stack of high-quality printed materials—layered, rounded, and deeply satisfying to touch. The emotional response is one of **focused excitement**: reducing the friction of news triage while making the act of discovery feel like a rewarding, curated event.

**Key Stylings:**
- **Corporate / Modern (M3):** Adhering to the latest Android patterns with a focus on tonal containers and expressive motion.
- **Editorial High-Contrast:** Large serif headlines against clean, spacious surfaces.
- **Tactile Surfaces:** Large corner radii and soft ambient shadows that imply physical layers.

## Colors

The palette is anchored by a high-intensity **Electric Royal Blue** primary, used to drive action and brand recognition. This is balanced by **Cerulean Mist** containers which serve as the primary tonal highlight for active states and meta-information.

**Midnight Navy** serves as the tertiary anchor and the primary ink color, providing the necessary weight for editorial typography. The background utilizes a cool-toned ceramic white to maintain a crisp, news-focused canvas without the harshness of pure white. Secondary actions and decorative accents use **Periwinkle Neon** to add a layer of technological sophistication to the classic blue-and-white news aesthetic.

## Typography

This system employs a high-contrast pairing of **Playfair Display** (Serif) for headlines and **Roboto Flex** (Sans) for UI and body text. 

Serif styles are used for all "Editorial" content—article titles, section headers, and numerical statistics—to instill a sense of journalistic authority. Sans-serif styles are reserved for "Functional" elements—navigation, labels, excerpts, and buttons—to ensure high scanability on mobile screens. 

Large headings use negative letter-spacing to maintain a tight, professional rhythm, while small labels use increased tracking and bold weights to remain legible at a glance.

## Layout & Spacing

The design system utilizes a **fluid grid** model optimized for the Android handheld experience. Layouts are built on a 4-column structure for mobile, expanding to 8 or 12 columns for tablets and foldables.

**Spacing Rhythm:**
- A base 4px (0.25rem) unit controls all spacing.
- **Margins:** 18dp on mobile devices to provide a wide, breathable frame.
- **Section Gaps:** Large 32dp gaps separate major functional areas (e.g., Header vs. Feed).
- **Stacking:** Elements within a card or a group use a tight 12dp rhythm to maintain visual association.

The layout adapts to large screens by capping the central content container at 680dp, preventing article headlines from becoming too wide to read comfortably while allowing imagery to scale or reflow into side-by-side grids.

## Elevation & Depth

Visual hierarchy is established through a combination of **Tonal Layers** and **Ambient Shadows**.

1.  **Canvas Layer:** The base `#F8FAFE` surface acts as the furthest depth level.
2.  **Container Layer:** Elevated cards use `#FFFFFF` (Surface Container Lowest). They are separated from the background by a very soft, extra-diffused shadow with a subtle Midnight Navy tint (10% opacity) rather than a neutral gray, creating a more cohesive, vibrant feel.
3.  **Tonal Highlighting:** Primary interactive zones use the `#ABD2FA` container to draw the eye without the "heavy" visual weight of a solid dark fill.
4.  **Bottom sheets:** Modals use high elevation (8dp+) and a backdrop dimming effect to focus the user on settings or specific triage actions.

## Shapes

The shape language is **Expressive and Rounded**, reflecting the Material 3 design philosophy. 

- **Primary Cards:** Use a generous `24dp` (rounded-xl) radius to create a soft, approachable "sheet" feel.
- **Small Components:** Chips, badges, and small buttons use a full pill-shape (CircleShape) to differentiate them from the more structural card components.
- **Imagery:** Media slots within cards should feature `20dp` internal corner rounding to harmonize with the parent container's curve.

## Components

### Buttons
- **Primary:** Filled with `#1B2CC1`, white text. 52dp height, 16dp rounded corners or pill-shaped.
- **Tonal (Secondary):** Filled with `#ABD2FA`, Midnight Navy text. Used for "Mark Read" or secondary triage.
- **Icon Buttons:** 56dp circular targets. For triage (Not Interested / Save), use a 1.5dp border in `#7692FF`.

### Cards
- **Article Deck Card:** 24dp radius, white surface, 4dp ambient shadow. Feature a top-aligned image slot (16:9) and a bold serif headline.
- **Queue Row:** 16dp radius, `#F1F4FB` fill. Horizontal layout with an 80dp square thumbnail on the right.

### Navigation
- **Bottom Bar:** Material 3 Expressive style. 28dp top corner radius. Active icons are highlighted with a `#ABD2FA` pill indicator.
- **Category Chips:** 40dp height, pill-shaped. Active chips use a solid primary fill; inactive chips use a white background with a `#C3C9DA` outline.

### Specialized
- **StatBand:** A 3-column pill container with 16dp corners, using a `#EBF0F7` background to group reading metrics.
- **Modal Bottom Sheet:** 28dp top radius, containing expressive surface-card toggles for settings like "Appearance."