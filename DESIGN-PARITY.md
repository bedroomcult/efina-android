# DESIGN-PARITY.md — design.md → Compose

Every section of `design.md` (the Orion Store UI document) mapped to the Kotlin
file that implements it, or to the reason it is not implemented.

Referenced from `Color.kt`, `Type.kt`, `Theme.kt` and `Motion.kt`, which each cite
this file for the parts they deliberately omit.

Source document: `/storage/emulated/0/git/efina/design.md` (440 lines, 13 sections).

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
| §2.2 14 palettes | **2 of 14**: Orion (in `Color.kt`) and Mono (in `Theme.kt`) | `Color.kt`, `Theme.kt` |
| §2.2 Custom palette maker | no | — |
| §2.2 Material You dynamic color | **yes** — the `"dynamic"` theme value selects it; `EfinaAppRoot` passes `dynamicColor` through. Android 12+ only, as the API requires | `Theme.kt`, `EfinaAppRoot.kt:124` |
| §2.3 Brand palette | yes, full primary/secondary/tertiary/surface/error set | `OrionLight`, `OrionDark` |
| §2.4 Semantic + status colors | yes | `EfinaSemantic` (safe/caution/danger/warn/info) |
| §2.5 `tonal()` helper | yes, same HSL construction: container lightness 0.88 light / 0.26 dark, foreground 0.30 / 0.82 | `tonal()` in `Color.kt` |
| §2.6 Install-state colors | **n/a** — no finance analogue. `MoneyColors` supplies income/expense/transfer pairs, but they are **hardcoded hex, light and dark only** and do not follow the palette | `Color.kt:99-116` |

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
| Emphasized type rationed to headlines/selected/primary actions | **no** — `AmountText` defaults to `EfinaType.amountSmall`, so every list amount is emphasised; `EfinaType` is referenced by 15 screen files | `Components.kt:206` |
| Eyebrow labels | **defined but unused** — zero call sites | `Type.kt:57` |
| Monospace for code/package strings | yes, used for the invite code | `EfinaType.code`, `FamilyScreen.kt` |

Why the font is not bundled: a downloaded variable font causes a first-frame
flash and a network dependency at startup. A financial app opening on its balance
should not wait on a font. `Type.kt` documents this.

Added beyond the doc: `amountLarge` / `amountMedium` / `amountSmall` with negative
tracking, so money figures are visually distinct from body text.

## §4 Shape

| Item | Status | Where |
|---|---|---|
| M3 corner scale 4/8/12/16/20/28/full | **partial** — 4/8/12/16/28 present; no 20 slot and no full radius | `EfinaShapes`, `Shape.kt:17-23` |
| Hero / carousel / deck cards: 28dp | yes | `EfinaCorners.hero` |
| Dropdown menus: 20dp | yes | `EfinaCorners.menu` |
| Screenshots: 16–20dp | **defined but unused** — zero call sites | `Shape.kt:35` |
| Bottom action bar: 28dp top corners | **no** — no top-only shape exists; `hero` rounds all four corners | `Shape.kt:25` |
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
| "Disable Animations" → instant scheme | yes, `EfinaMotion.enabled` swaps every spec for a zero-duration tween. `snap()` was not used because it does not satisfy the `FiniteAnimationSpec` return type | `Motion.kt:37-39` |
| "Calm" setting stopping ambient loops | no — no ambient loops exist | — |
| `springClick`: 0.94 press scale | **not implemented** — every tappable row is `Surface(onClick)`, which keeps its InteractionSource private, so no press stream is observable. The helper was dead code and has been removed | — |
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
| §1 language | 5 / 8 | layout modes, tablet/TV, the other 10 selectable families | — |
| §2 color | 5 / 7 | Dusk, OLED, 12 palettes, palette maker | — |
| §3 typography | 5 / 8 | custom fonts, the other 10 families; eyebrow style unused | — |
| §4 shape | 6 / 10 | selected-row morph, top-only corners, media corners unused | `MaterialShapes`, `Morph` (internal) |
| §5 motion | 3 / 15 | dock, mascot, haptics, wave indicators, springClick | `MotionScheme` (internal), `SharedTransitionLayout` (unavailable) |

Corrected after an independent audit found 9 of 30 claim rows false. The earlier
counters overstated §1 (8 rows, not 9), §4 and §5.

## Not disclosed before the audit

These design.md sections were absent from this document entirely, which is a
silent omission rather than a documented skip:

- **§6 Iconography and imagery** — Material Symbols mapping, Coil, avatars, brand mark.
- **§7 App shell and navigation** — tab visibility rules, the 600dp rail breakpoint, width caps (1200/840/560), header anatomy, cleanup FAB, scroll-to-top.
- **§9 Sheets and overlays** — `OrionSheet`, sheet-scoped toasts.
- **§11 Key measurements** — not audited against the implementation.
- **§12 Accessibility and input** — content descriptions are now present on every `Icon` (verified: zero `Icon(` blocks lacking one). Focus rings, focus trapping and the contrast outline remain unimplemented.
- **§5 `Tilt` / `rememberTilt`** — no sensor code exists and the row was missing from the table.

The two "not possible" rows were attempted and rejected by the compiler; the
error was `Cannot access interface MotionScheme: it is internal in file`.
