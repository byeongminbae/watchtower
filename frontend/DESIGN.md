# Watchtower Design System

## 1. Atmosphere & Identity

Watchtower feels like a sunlit observation deck: calm, precise, and reassuring rather than nocturnal or ominous. Its signature is the **daylight beam**—a warm lighthouse glow travelling across cool sea-blue surfaces, with cursor-aware ambient light that adds depth without competing with content. The existing lighthouse blue and signal yellow remain recognizable, recomposed on an airy cloud-white canvas with Stripe-inspired chromatic depth.

## 2. Color

### Palette

| Role | Token | Value | Usage |
|---|---|---|---|
| Surface/canvas | `--wt-surface-canvas` | `#F6FAFE` | Page background |
| Surface/primary | `--wt-surface-primary` | `#FFFFFF` | Cards, dialogs, menus |
| Surface/soft | `--wt-surface-soft` | `#EDF6FC` | Secondary panels, disabled fields |
| Surface/tint | `--wt-surface-tint` | `#E2F1FB` | Selected and highlighted surfaces |
| Text/primary | `--wt-text-primary` | `#102A43` | Headings and body |
| Text/secondary | `--wt-text-secondary` | `#526D82` | Supporting copy and metadata |
| Text/tertiary | `--wt-text-tertiary` | `#7D94A6` | Disabled and low-emphasis text |
| Border/default | `--wt-border-default` | `#D7E5EF` | Controls and card edges |
| Border/luminous | `--wt-border-luminous` | `rgba(255, 255, 255, 0.78)` | Lit surface rim |
| Accent/primary | `--wt-accent-primary` | `#246B9E` | Links, controls, focus |
| Accent/hover | `--wt-accent-hover` | `#18547F` | Primary hover |
| Signal/primary | `--wt-signal-primary` | `#F4B83A` | Main CTA and lighthouse signal |
| Signal/hover | `--wt-signal-hover` | `#E6A622` | Signal hover |
| Signal/soft | `--wt-signal-soft` | `#FFF2C7` | Warm illumination |
| Status/success | `--wt-status-success` | `#16845B` | Success |
| Status/warning | `--wt-status-warning` | `#B86E10` | Warning |
| Status/error | `--wt-status-error` | `#C54444` | Errors and destructive actions |
| Status/info | `--wt-status-info` | `#2477B3` | Informational state |

### Rules

- Cool daylight neutrals establish hierarchy; pure white is reserved for foreground surfaces.
- Yellow is the signal color for primary calls to action, not a general decoration.
- Blue carries navigation, focus, and data. Status colors retain their semantic roles.
- Raw colors are not introduced outside this contract; translucent lighting derives from these tokens.

## 3. Typography

### Scale

| Level | Size | Weight | Line Height | Tracking | Usage |
|---|---|---|---|---|---|
| Display | `clamp(2.25rem, 6vw, 4rem)` | 760 | 1.08 | `-0.035em` | Home hero |
| H1 | `2.25rem` | 720 | 1.18 | `-0.025em` | Major title |
| H2 | `1.75rem` | 700 | 1.25 | `-0.02em` | Section heading |
| H3 | `1.375rem` | 680 | 1.35 | `-0.01em` | Card title |
| Body/lg | `1.125rem` | 400 | 1.7 | `0` | Lead copy |
| Body | `1rem` | 400 | 1.65 | `0` | Default text |
| Body/sm | `0.875rem` | 400 | 1.55 | `0` | Supporting text |
| Caption | `0.75rem` | 520 | 1.45 | `0.01em` | Metadata |

### Font Stack

- Primary: `"Pretendard Variable", Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`
- Mono: `"SFMono-Regular", Consolas, "Liberation Mono", monospace`

### Rules

- Korean display text uses balanced wrapping and body text uses pretty wrapping.
- Body text is never below 14px. Numeric dashboards use tabular numerals.
- Existing copy and semantic heading order stay unchanged during the redesign.

## 4. Spacing & Layout

### Base Unit

All spacing intent derives from 4px.

| Token | Value | Usage |
|---|---|---|
| `--wt-space-1` | `4px` | Optical adjustments |
| `--wt-space-2` | `8px` | Inline clusters |
| `--wt-space-3` | `12px` | Compact controls |
| `--wt-space-4` | `16px` | Default gaps |
| `--wt-space-5` | `20px` | Comfortable groups |
| `--wt-space-6` | `24px` | Card padding |
| `--wt-space-8` | `32px` | Card groups |
| `--wt-space-10` | `40px` | Page rhythm |
| `--wt-space-12` | `48px` | Major separation |
| `--wt-space-16` | `64px` | Section rhythm |
| `--wt-space-20` | `80px` | Hero rhythm |
| `--wt-space-24` | `96px` | Maximum section gap |

### Grid

- Max content width: MUI `lg` container, approximately 1200px.
- Mobile margin: 16px; tablet and desktop gutters follow the existing MUI container.
- Validation breakpoints: 375px mobile, 768px tablet, 1280px desktop.
- Existing responsive grid behavior and document scrolling remain unchanged.

## 5. Components

### Daylight Canvas

- **Structure**: root body canvas plus a fixed, pointer-inert ambient-light layer.
- **Variants**: resting daylight; cursor-lit on fine pointers; frozen with reduced motion.
- **States**: ambient position follows pointer without receiving input.
- **Accessibility**: no semantic content, no contrast dependency, hidden from assistive technology.
- **Motion**: CSS custom-property updates only; the rendered light uses opacity and filter.
- **Layout**: viewport atmosphere behind the document scroll owner.

### Navigation Bar

- **Structure**: sticky app bar, brand cluster, route cluster, CTA, account trigger and menu.
- **Variants**: guest, member, admin.
- **Spacing**: `--wt-space-2`, `--wt-space-4`, `--wt-space-6`.
- **States**: translucent rest, brighter hover, visible blue focus, pressed feedback, disabled.
- **Accessibility**: current labels, keyboard path, ARIA names and menu behavior remain intact.
- **Motion**: 150ms color/opacity tint and 120ms press transform.
- **Layout**: cluster within the existing container; sticky document navigation.

### Lit Surface

- **Structure**: MUI Paper/Card/Dialog/Menu foreground material.
- **Variants**: default, outlined, elevated, interactive, disabled.
- **Spacing**: existing component spacing, aligned to the spacing scale.
- **States**: resting rim and chromatic shadow; interactive hover lifts 2px; focus receives a blue ring.
- **Accessibility**: foreground/background contrast remains WCAG AA; motion is not required to perceive state.
- **Motion**: 180ms transform, opacity, shadow tint; reduced motion removes lift.
- **Layout**: stack/grid children use existing route structures.

### Signal Action

- **Structure**: existing MUI Button content and icons.
- **Variants**: signal contained, blue contained, outlined, text, destructive.
- **States**: default, hover, active, focus-visible, disabled, loading.
- **Accessibility**: minimum 40px visual height where route layout permits, clear 2px focus ring.
- **Motion**: beui.dev `button` mechanism adapted to CSS—interruptible 120ms press scale and short tint transition; async labels remain existing behavior.

### Form Control

- **Structure**: existing MUI input, label, helper/error content.
- **Variants**: enabled, disabled, read-only, error.
- **States**: default, hover, focus, disabled, error.
- **Accessibility**: existing labels and disabled semantics remain unchanged; focus is visible without color alone.
- **Motion**: 150ms border/background tint only.

### Data Display

- **Structure**: existing tables, charts, list rows, chips and status indicators.
- **Variants**: loading, populated, empty, error.
- **States**: route behavior remains unchanged; rows expose hover only when already interactive.
- **Accessibility**: tabular numerals, readable status contrast, no meaning conveyed by glow alone.
- **Motion**: no decorative motion; existing status transitions only.

## 6. Motion & Interaction

| Token | Duration | Easing | Usage |
|---|---|---|---|
| `--wt-motion-press` | `120ms` | `ease-out` | Button press transform |
| `--wt-motion-micro` | `150ms` | `ease-out` | Focus, border and tint |
| `--wt-motion-surface` | `220ms` | `cubic-bezier(0.16, 1, 0.3, 1)` | Interactive surface lift |
| `--wt-motion-emphasis` | `480ms` | `cubic-bezier(0.16, 1, 0.3, 1)` | Existing hero entrance if used |

- Dynamic lighting follows beui.dev `shader-background` mechanics at a lower-cost CSS level: pointer coordinates control a composited radial-light layer; no canvas dependency is added.
- Button feedback follows the beui.dev `button` spring-press idea using interruptible CSS transform feedback.
- Motion signals interactivity or spatial lighting response only. Non-interactive cards do not animate on hover.
- Only `transform`, `opacity`, and `filter` animate. `prefers-reduced-motion: reduce` freezes ambient response and removes transforms.

## 7. Depth & Surface

### Strategy

Mixed tonal shift and Stripe-inspired chromatic shadows, with one consistent upper-left daylight source.

| Level | Value | Usage |
|---|---|---|
| Rim | `inset 0 1px 0 rgba(255,255,255,0.82)` | All foreground surfaces |
| Subtle | `0 1px 2px rgba(23,74,111,0.06)` | Controls and quiet panels |
| Default | `0 18px 45px -30px rgba(30,92,135,0.32), 0 8px 22px -18px rgba(16,42,67,0.18)` | Cards and menus |
| Prominent | `0 34px 70px -34px rgba(30,92,135,0.38), 0 18px 36px -24px rgba(16,42,67,0.22)` | Dialogs and major focal surfaces |

- Surfaces use a luminous white rim and blue-tinted far shadow, never a generic black shadow.
- Warm signal glows originate only near primary CTAs and lighthouse light.
- Radius hierarchy: 10px controls, 14px cards, 18px major focal surfaces.

## 8. Accessibility Constraints & Accepted Debt

### Constraints

- WCAG 2.2 AA: 4.5:1 body contrast and 3:1 large text/UI contrast.
- Every interactive element keeps visible keyboard focus and full keyboard reachability.
- Touch targets do not shrink from existing behavior.
- `prefers-reduced-motion` freezes ambient lighting and disables transform feedback.
- Dynamic lighting is decorative and may never carry information.
- Existing copy, routes, states, callbacks, API calls, and authorization behavior are immutable for this visual-only task.

### Personas

- A mobile member checks watches one-handed in bright daylight and needs strong hierarchy with no horizontal overflow.
- An administrator scans dense numeric information and needs stable alignment and tabular numerals.
- A motion-sensitive keyboard user needs a complete, visible path with the atmosphere frozen.

### Accepted Debt

| Item | Location | Why accepted | Owner / Exit |
|---|---|---|---|
| None | — | No new design or accessibility debt is accepted for this redesign. | — |
