---
name: Obsidian & Gold
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1b1b1b'
  surface-container: '#1f1f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353535'
  on-surface: '#e2e2e2'
  on-surface-variant: '#d0c5af'
  inverse-surface: '#e2e2e2'
  inverse-on-surface: '#303030'
  outline: '#99907c'
  outline-variant: '#4d4635'
  surface-tint: '#e9c349'
  primary: '#f2ca50'
  on-primary: '#3c2f00'
  primary-container: '#d4af37'
  on-primary-container: '#554300'
  inverse-primary: '#735c00'
  secondary: '#c7c6c6'
  on-secondary: '#303031'
  secondary-container: '#464747'
  on-secondary-container: '#b5b5b5'
  tertiary: '#d0cdcd'
  on-tertiary: '#303030'
  tertiary-container: '#b4b2b2'
  on-tertiary-container: '#454545'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffe088'
  primary-fixed-dim: '#e9c349'
  on-primary-fixed: '#241a00'
  on-primary-fixed-variant: '#574500'
  secondary-fixed: '#e3e2e2'
  secondary-fixed-dim: '#c7c6c6'
  on-secondary-fixed: '#1b1c1c'
  on-secondary-fixed-variant: '#464747'
  tertiary-fixed: '#e4e2e1'
  tertiary-fixed-dim: '#c8c6c6'
  on-tertiary-fixed: '#1b1c1c'
  on-tertiary-fixed-variant: '#474747'
  background: '#131313'
  on-background: '#e2e2e2'
  surface-variant: '#353535'
  high-contrast-text: '#FFFFFF'
  surface-elevation-1: '#121212'
  surface-elevation-2: '#1E1E1E'
  success-gold: '#C5A028'
  error-red: '#FF4444'
typography:
  display-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Be Vietnam Pro
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Work Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Be Vietnam Pro
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style
The design system embodies an aura of **prestige, security, and institutional authority**. It is designed for a distribution network that prioritizes the high-value perception of its logistics operations. The brand personality is **exclusive, meticulous, and impenetrable**.

The visual style is a sophisticated blend of **Minimalism** and **High-Contrast Modernism**. By utilizing a "Dark Mode first" philosophy, the interface creates a focused environment where critical information—tracking numbers, secure codes, and status updates—emerges from the shadows with clarity. The use of gold accents against a deep black backdrop signals a premium tier of service, transforming standard logistics into a curated experience. This aesthetic ensures that users feel they are interacting with a highly secure, elite network.

## Colors
The palette is rooted in **Deep Black (#000000)**, which serves as the primary canvas for all surfaces, creating a void-like depth that minimizes eye strain and maximizes focus. 

**Elegant Gold (#D4AF37)** is the signature accent color. It is reserved strictly for high-priority actions, primary brand signifiers, and critical status information. This color must be used sparingly to maintain its perceived value.

**Neutrals** are handled through a tiered gray scale:
- **Light Gray (#808080):** Used for secondary text, icons, and non-essential labels.
- **Dark Gray (#333333):** Used for borders, dividers, and subtle surface elevations.
- **White (#FFFFFF):** Reserved exclusively for high-contrast body text and headers to ensure maximum readability against the black background.

## Typography
Typography is the primary vehicle for the "professional and secure" narrative. **Be Vietnam Pro** provides a clean, contemporary structure for headings, lending an air of modern efficiency. **Work Sans** is the workhorse for body copy, chosen for its neutral tone and clarity in low-light (dark mode) settings.

**JetBrains Mono** is vital for the technical aspect of the distribution network. All tracking IDs, package dimensions, and security tokens must be rendered in this monospaced face to prevent character confusion and reinforce the "high-tech" security nature of the system.

In the RTL context, ensure font weights are robust enough to remain crisp against black backgrounds. Avoid "Thin" or "Extra Light" weights to prevent visual vibration.

## Layout & Spacing
The system employs a **fluid grid** for mobile and a **fixed 12-column grid** for desktop (max-width 1280px). The spacing rhythm is based on a strict **4px baseline**, ensuring that every element—from the height of a button to the padding inside a card—is a multiple of four.

The layout philosophy emphasizes **intentional negative space**. By allowing the black background to breathe between components, the design system avoids the cluttered feel typical of logistics apps. 

**Reflow Rules:**
- **Mobile:** Elements stack vertically with 16px margins. Primary actions are pinned to the bottom of the viewport in a persistent "Gold Bar" area.
- **Desktop:** Dashboard views utilize side-navigation to maximize horizontal space for data-rich package lists.
- **RTL:** All horizontal flows mirror; however, monospaced tracking codes should remain LTR-aligned for global standardization of shipping identifiers.

## Elevation & Depth
In a pure black environment, depth is not created with shadows, but through **Tonal Layers** and **Low-Contrast Outlines**.

- **Level 0 (Global):** Pure #000000.
- **Level 1 (Cards/Surfaces):** #121212. These surfaces should have a subtle 1px border of #333333 to define their edges.
- **Level 2 (Active/Selected):** #1E1E1E. Used for hovered items or active containers.
- **Accents:** Gold is never used for surfaces; it is only used for high-impact strokes or fills on small interactive elements.

Shadows are replaced by **Ambient Glows** only for primary Gold buttons, using a very low-opacity gold outer glow (`0 0 15px rgba(212, 175, 55, 0.2)`) to suggest illumination rather than physical height.

## Shapes
The shape language is **Soft (0.25rem / 4px)**. This choice moves away from the "friendly" roundedness of the previous system toward a more **precise, architectural feel**. 

Standard components use a 4px radius. Large containers (cards) may scale to 8px, but should never feel "bubbly." The sharp, disciplined corners reinforce the professional and secure nature of the network. Status badges remain slightly more rounded (pill-shaped) to ensure they are visually distinct from the structural grid of the UI.

## Components

### Buttons
- **Primary:** Elegant Gold (#D4AF37) background with Black (#000000) text. This provides the highest visual weight.
- **Secondary:** Dark Gray (#333333) background with White text.
- **Outline/Ghost:** Transparent background with a Gold 1px border.

### Input Fields
Inputs utilize the #121212 surface with a 1px border of #333333. When focused, the border transitions to Gold, and the label (in JetBrains Mono) remains high-contrast white.

### Distribution Cards
Package cards should be #121212. The Gold accent is used as a vertical "strip" on the leading edge (right side for RTL) to indicate a high-priority or active shipment.

### Progress Tracking
The "Ham-Mahalleh" tracker uses a solid Gold line for completed segments and a Dark Gray line for pending ones. Current status nodes are indicated by a Gold "diamond" shape rather than a circle, echoing the architectural shape language.

### List Items
Logistics lists use Gray (#333333) dividers. Every tracking ID in a list must be highlighted with a subtle Gray (#808080) background tint to make it scannable.