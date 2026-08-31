# Project Brief & PRD: Intentional Reading Android App

## 1. Project Overview
**Intentional Reading** is a mobile news aggregator designed to transform a minimalistic, text-heavy experience into a vibrant, editorial-style application. The app focuses on "deliberate consumption" of news, providing users with tools to discover, save for later, and track their reading history.

## 2. Design Vision: Material 3 Expressive
The application utilizes the **Material 3 Expressive** design system. This direction moves away from utility-only interfaces toward a high-impact, editorial feel that emphasizes content hierarchy and brand personality.

### Visual Identity
- **Color Palette**: 
  - Primary: `#1B2CC1` (Deep Blue)
  - Secondary: `#7692FF` (Vibrant Blue)
  - Background/Surfaces: `#F7F9FD` (Soft Cool White)
  - Accents: `#ABD2FA` (Light Blue)
- **Typography**: 
  - **Headlines**: Playfair Display (Serif) - Creates a sophisticated, news-journalism feel.
  - **Body/Labels**: Clean Sans-Serif - Ensures high legibility for long-form reading.
- **Surface Strategy**: Large 24px corner radii on cards and containers to create a modern, approachable aesthetic.

## 3. Layout & Grid System
To ensure responsive fidelity across Android devices:
- **Grid**: 4-column responsive grid.
- **Margins**: 24px outer margins for a spacious, editorial feel.
- **Gutters**: 16px between columns.
- **Touch Targets**: Minimum 48x48dp for all interactive elements.

## 4. App Structure & Information Architecture
The app is organized into three primary views accessible via a Bottom Navigation Bar, with a global Settings overlay.

### Core Screens
1.  **Discover**: The primary discovery engine. Features a card-based feed of articles with tonal chips for categorization (Science, Technology, Literature).
2.  **Read Later**: The user's personal queue. Provides stats on the number of items and estimated reading time.
3.  **History**: A chronological record of completed readings. Includes a high-fidelity empty state to encourage discovery when the history is clear.
4.  **Settings (Overlay)**: A modal sheet for local preferences, including theme switching (Light/Dark/System) and data management.

## 5. Component States & Behaviors
- **Buttons (Primary/Read Article)**:
  - Default: Solid Primary color.
  - Pressed: 12% black overlay (or darker shade) with a subtle 0.95 scale-down.
  - Disabled: 38% opacity with no interaction.
- **Tonal Chips**:
  - Unselected: Surface-variant background with Medium-emphasis text.
  - Selected: Secondary background with High-emphasis text.
- **Text Truncation**:
  - Card Headlines: Max 3 lines, then ellipsis (`...`).
  - Card Descriptions: Max 2 lines, then ellipsis.

## 6. Motion & Interactivity Specifications
Motion is a core pillar of the "Expressive" system, used to establish spatial relationships.

### Directional Tab Navigation (Lateral Slide)
- **Logic**: Transitions are indexed to the bottom bar positions.
- **Interaction**:
  - Moving "Left" (e.g., Discover → Read Later): New screen slides in from the left.
  - Moving "Right" (e.g., Discover → History): New screen slides in from the right.
- **Specs**: 300ms duration using Material 3 Emphasized easing. Includes a subtle scale-down and 0.8 opacity fade on the outgoing screen.

### Settings Access (Modal Sheet Reveal)
- **Entrance**: Vertical slide-up from the bottom with a simultaneous fade-in.
- **Exit**: "Reverse tuck" (slide down and fade out).
- **Background**: A semi-transparent scrim dims the underlying feed to create depth.
- **Specs**: 350ms duration with a Decelerated (Out) curve for the entrance.

## 7. Shared Components
- **TopAppBar**: Small centered "Intentional Reading" brand title with a trailing Settings gear icon.
- **BottomNavBar**: Three-destination bar (Read Later, Discover, History) using active pill-shape indicators and Material 3 iconography.

## 8. Design Tokens (JSON)
```json
{
  "colors": {
    "primary": "#1B2CC1",
    "secondary": "#7692FF",
    "background": "#F7F9FD",
    "accent": "#ABD2FA",
    "on_primary": "#FFFFFF",
    "surface": "#FFFFFF",
    "surface_variant": "#E1E2EC"
  },
  "typography": {
    "headline_large": "Playfair Display, 32px, Bold",
    "headline_medium": "Playfair Display, 24px, SemiBold",
    "body_medium": "Inter/Sans-Serif, 16px, Regular",
    "label_small": "Inter/Sans-Serif, 12px, Medium"
  },
  "spacing": {
    "margin_mobile": "24px",
    "gutter": "16px",
    "radius_large": "24px"
  }
}
```
