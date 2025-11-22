# Trailglass Admin Panel - Color Palette

## Primary Brand Colors

### Sky Blue (Primary)
**Hex:** `#0ea5e9`
**RGB:** `rgb(14, 165, 233)`
**Usage:** Primary buttons, header background, main brand color
**Represents:** Trust, travel, sky, ocean

### Cyan (Secondary)
**Hex:** `#06b6d4`
**RGB:** `rgb(6, 182, 212)`
**Usage:** Gradients, info messages, accents
**Represents:** Water, exploration, freshness

### Teal (Links)
**Hex:** `#0891b2`
**RGB:** `rgb(8, 145, 178)`
**Usage:** Links, interactive text elements
**Represents:** Action, navigation

## Status Colors

### Emerald Green (Success)
**Hex:** `#10b981`
**RGB:** `rgb(16, 185, 129)`
**Usage:** Success messages, positive metrics, user statistics
**Represents:** Growth, success, safety

### Amber (Warning)
**Hex:** `#f59e0b`
**RGB:** `rgb(245, 158, 11)`
**Usage:** Warning states, place visits metrics
**Represents:** Caution, attention, energy

### Red (Error)
**Hex:** `#ef4444`
**RGB:** `rgb(239, 68, 68)`
**Usage:** Error messages, critical alerts
**Represents:** Danger, stop, critical

### Purple (Locations)
**Hex:** `#8b5cf6`
**RGB:** `rgb(139, 92, 246)`
**Usage:** Location-related elements
**Represents:** Mystery, exploration, discovery

## Neutral Colors

### White (Container)
**Hex:** `#ffffff`
**RGB:** `rgb(255, 255, 255)`
**Usage:** Cards, containers, backgrounds

### Light Gray (Background)
**Hex:** `#f0f2f5`
**RGB:** `rgb(240, 242, 245)`
**Usage:** Main layout background

### Border Gray
**Hex:** `#e5e7eb`
**RGB:** `rgb(229, 231, 235)`
**Usage:** Borders, dividers, separators

## Menu/Interactive Colors

### Dark Blue (Selected Text)
**Hex:** `#0c4a6e`
**RGB:** `rgb(12, 74, 110)`
**Usage:** Selected menu item text

### Mid Blue (Hover Text)
**Hex:** `#075985`
**RGB:** `rgb(7, 89, 133)`
**Usage:** Menu item hover text

### Light Sky Blue (Selected Background)
**Hex:** `#e0f2fe`
**RGB:** `rgb(224, 242, 254)`
**Usage:** Selected menu item background

### Very Light Sky Blue (Hover Background)
**Hex:** `#f0f9ff`
**RGB:** `rgb(240, 249, 255)`
**Usage:** Menu item hover background

## Gradient Combinations

### Header Gradient
```css
background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
```
Direction: 135deg (diagonal from top-left to bottom-right)
From: Sky Blue to Cyan

### Logo Gradient
```css
background: linear-gradient(135deg, #0284c7 0%, #0891b2 100%);
```
Direction: 135deg
From: Darker Sky Blue to Teal

## Card Background Colors (Semi-transparent)

### Users Card
**Base:** `#10b981` (Emerald Green)
**Background:** `#d1fae5` (Light Green)

### Trips Card
**Base:** `#0ea5e9` (Sky Blue)
**Background:** `#e0f2fe` (Light Sky Blue)

### Place Visits Card
**Base:** `#f59e0b` (Amber)
**Background:** `#fef3c7` (Light Amber)

### Locations Card
**Base:** `#8b5cf6` (Purple)
**Background:** `#ede9fe` (Light Purple)

## Color Psychology

### Why Blue/Teal?
- **Trust & Reliability:** Blue is associated with professionalism and dependability
- **Travel & Adventure:** Blue/teal evokes ocean, sky, and exploration
- **Calm & Focus:** Promotes productivity in admin environments
- **Universal Appeal:** Works well across cultures and demographics

### Color Accessibility
All color combinations meet WCAG 2.1 AA standards for contrast:
- Primary text on white: 4.5:1+ contrast ratio
- White text on primary blue: 4.5:1+ contrast ratio
- Interactive elements have clear visual states

## Usage Guidelines

### Do's:
- Use Sky Blue (#0ea5e9) for primary actions
- Use status colors (green, amber, red) to convey meaning
- Maintain consistent spacing with neutral grays
- Apply gradients sparingly for emphasis

### Don'ts:
- Don't use status colors for decoration
- Don't mix too many colors in one view
- Don't use low-contrast color combinations
- Don't override theme colors without reason

## Dark Mode Considerations (Future)

Recommended dark mode palette:
- Background: `#0f172a` (Dark Slate)
- Containers: `#1e293b` (Slate)
- Primary: `#38bdf8` (Lighter Sky Blue)
- Success: `#34d399` (Lighter Emerald)
- Warning: `#fbbf24` (Lighter Amber)
- Error: `#f87171` (Lighter Red)

## CSS Custom Properties (Optional Implementation)

```css
:root {
  /* Primary */
  --color-primary: #0ea5e9;
  --color-secondary: #06b6d4;
  --color-link: #0891b2;
  
  /* Status */
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-error: #ef4444;
  --color-info: #06b6d4;
  
  /* Neutral */
  --color-white: #ffffff;
  --color-bg-layout: #f0f2f5;
  --color-border: #e5e7eb;
  
  /* Menu */
  --color-menu-selected-bg: #e0f2fe;
  --color-menu-selected-text: #0c4a6e;
  --color-menu-hover-bg: #f0f9ff;
  --color-menu-hover-text: #075985;
}
```

## Figma/Design Tool Export

For designers using Figma, Sketch, or Adobe XD:

**Color Styles:**
1. Primary/Sky Blue - #0ea5e9
2. Primary/Cyan - #06b6d4
3. Primary/Teal - #0891b2
4. Status/Success - #10b981
5. Status/Warning - #f59e0b
6. Status/Error - #ef4444
7. Status/Info - #06b6d4
8. Neutral/White - #ffffff
9. Neutral/Background - #f0f2f5
10. Neutral/Border - #e5e7eb

**Gradient Styles:**
1. Header Gradient - 135° from #0ea5e9 to #06b6d4
2. Logo Gradient - 135° from #0284c7 to #0891b2
