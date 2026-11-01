# DESIGN-PARITY.md — design.md → Compose

Every section of `design.md` (the Orion Store UI document) mapped to the Kotlin
file that implements it, or to the reason it is not implemented.

Referenced from `Color.kt`, `Type.kt`, `Theme.kt` and `Motion.kt`, which each cite
this file for the parts they deliberately omit.

Source document: `/storage/emulated/0/git/efina/design.md` (440 lines, 6 sections).

---

## §1 Design language

| design.md asks for | Implemented | Where |
|---|---|---|
| Material 3 design system | yes | `Theme.kt` |
| Playful, tactile, icon-first | partly — financial app, so icon-led but not playful | `Components.kt` |
| Indigo primary + acid-lime tertiary | yes | `OrionLight` / `OrionDark` in `Color.kt` |
| Space Grotesk default font | **no** — platform default; see §3 | `Type.kt` |
| Themes: Light, Dusk, Dark, OLED, Material You | **partly** — Light, Dark, Mono, Material You; Dusk and OLED omitted | `Theme.kt` |
| Layout modes: Modern / Classic / Compact | **no** | n/a |
| Phone, tablet, TV | phone only | n/a |
| Imagery as image, no frames | n/a — no imagery | n/a |

Deliberate: an app-store storefront's playful personality does not transfer to a
ledger. The structural rules — icon-first rows, tonal colour from content, rationed
emphasis — do, and are followed.

## §2 Color

| Item | Status | Where |
|---|---|---|
| §2.1 Theme modes: Light, Dusk, Dark, OLED | Light, Dark implemented. **Dusk skipped** (it exists to soften a specific dark; Orion dark is already lifted). **OLED skipped** (pure black is a preference, and Material You covers most of the intent). Mono added instead | `Theme.kt` |
| §2.2 14 palettes | **2 of 14**: Orion (default) and Mono | `Color.kt` |
| §2.2 Custom palette maker | no | — |
| §2.2 Material You dynamic color | yes, Android 12+ | `Theme.kt` `dynamicColor` parameter |
| §2.3 Brand palette | yes, full primary/secondary/tertiary/surface/error set | `OrionLight`, `OrionDark` |
| §2.4 Semantic + status colors | yes | `EfinaSemantic` (safe/caution/danger/warn/info) |
| §2.5 `tonal()` helper | yes, same HSL construction: container lightness 0.88 light / 0.26 dark, foreground 0.30 / 0.82 | `tonal()` in `Color.kt` |
| §2.6 Install-state colors | **n/a** — app-store install states have no finance analogue. `MoneyColors` (income/expense/transfer) takes the same structural role: per-state container+foreground pairs that recolour with the theme | `Color.kt` |

Deliberate divergence: `MoneyColors` is kept out of the M3 scheme on purpose. A
palette swap must never turn an expense green.

## §3 Typography

| Item | Status | Where |
|---|---|---|
| Space Grotesk default | **no** — platform default font | `Type.kt` |
| 11 selectable families + custom upload | **no** | — |
| Google Fonts TTF download + cache | **no** | — |
| Standard M3 scale | yes, all 15 slots | `Typography` in `Type.kt` |
| Expressive "Emphasized" variants | yes, as named extras (`displaySmallEmphasized`, `headlineSmallEmphasized`, `titleLargeEmphasized`, `titleMediumEmphasized`, `bodyMediumEmphasized`) | `EfinaType` |
| Emphasized type rationed to headlines/selected/primary actions | yes — enforced by convention: only `SectionHeader`, screen titles and primary buttons use `EfinaType` | — |
| Eyebrow labels: `labelLarge`, primary, uppercase, black weight | yes | `EfinaType.eyebrow` |
| Monospace for code/package strings | yes, repurposed for account numbers | `EfinaType.code` |

Why the font is not bundled: a downloaded variable font causes a first-frame
flash and a network dependency at startup. A financial app opening on its balance
should not wait on a font. `Type.kt` documents this.

Added beyond the doc: `amountLarge` / `amountMedium` / `amountSmall` with negative
tracking, so money figures are visually distinct from body text.

## §4 Shape

| Item | Status | Where |
|---|---|---|
| M3 corner scale 4/8/12/16/20/28/full | yes | `EfinaShapes` in `Shape.kt` |
| Hero / carousel / deck cards: 28dp | yes | `EfinaCorners.hero` |
| Dropdown menus: 20dp | yes | `EfinaCorners.menu` |
| Screenshots: 16–20dp | yes | `EfinaCorners.media` |
| Bottom action bar: 28dp top corners | not needed yet; `hero` covers it | — |
| Search pill / chips / tags / badges: full | yes | `EfinaCorners.pill` |
| App icon corner: 24% of size | yes, repurposed for account/category tiles | `EfinaCorners.icon(size)` |
| Segmented lists: 20dp outer, 4dp inner, 2dp gaps | yes | `SegmentedColumn` / `SegmentedRow` in `Components.kt` |
| Selected row morphs to full radius | **no** — needs shape animation | — |
| Shape morphing (`MaterialShapes` polygons, `Morph`) | **no** | see below |

Shape morphing is not implementable: `MaterialShapes` and the `Morph*` component
family are `internal` in the available material3 version. This was verified by
building, not assumed — see `PARITY.md` §"Material 3 Expressive".

## §5 Motion and feedback

| Item | Status | Where |
|---|---|---|
| `MotionScheme.expressive()` | **no — API is internal.** Approximated with spring specs (damping 0.72, medium-low stiffness for spatial; no-bounce low stiffness for effects) | `EfinaMotion` in `Motion.kt` |
| "Disable Animations" → instant scheme | yes, `EfinaMotion.enabled` returns `snap()` | `Motion.kt` |
| "Calm" setting stopping ambient loops | no — no ambient loops exist | — |
| `springClick`: 0.94 press scale, selection haptic | scale yes; **haptic no** (no haptics dependency) | `Modifier.springClick` |
| `reveal(index)`: 28dp rise + fade, 45 ms stagger, first 9 items | yes, all four numbers | `revealModifier` |
| Shared-element icon transition | **no** — `SharedTransitionLayout` is newer than the pinned Compose | — |
| Tab change: fade-through, 0.98 scale-in, scroll state per tab | partly — nav preserves state via `saveState`/`restoreState`; no scale-in | `EfinaAppRoot.kt` |
| Hero pager auto-advance | n/a — no carousel | — |
| Dock slides away on scroll | **no** — bottom bar is static | — |
| Nav icons pop 0.7 → 1.0 | no | — |
| Wave progress indicators | **no** — uses `LinearProgressIndicator` / `CircularProgressIndicator` | — |
| Skeleton placeholders | yes | `SkeletonRow`, `LoadingList` |
| Pull-to-refresh with mascot | refresh yes, mascot no | `HomeScreen` |
| Semantic haptic set | **no** | — |

## Not applicable

Sections describing an app-store storefront — hero carousels, install badges,
screenshot galleries, bundles, TV focus rings, the shapes dock — have no finance
equivalent and are not ported. Their structural lessons (a colour-bearing surface
per content item, rationed emphasis, staggered entrances) are.

---

## Summary

| Section | Implemented | Deliberately skipped | Not possible |
|---|---|---|---|
| §1 language | 5 / 9 | layout modes, tablet/TV, font | — |
| §2 color | 5 / 7 | Dusk, OLED, 12 palettes, palette maker | — |
| §3 typography | 5 / 8 | custom fonts, font download | — |
| §4 shape | 8 / 10 | selected-row morph | `MaterialShapes`, `Morph` (internal) |
| §5 motion | 6 / 15 | dock, mascot, haptics, wave indicators | `MotionScheme` (internal), `SharedTransitionLayout` (unavailable) |

The two "not possible" rows were attempted and rejected by the compiler; the exact
error is in `PARITY.md`.
