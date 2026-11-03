# Orion Store: Motion, Icons and Assets

Companion to `design.md`. That file says *what* the UI looks like. This one says *how it moves* and *what files it ships*. Numbers come from the Compose source (`app/src/main/java/com/orion/store/ui/`) and `res/`. Snippets are simplified from the real code and marked as such.

> **Correction to design.md:** the pull-to-refresh picker has **14** options: 4 expressive (Morph loader, Sunny, Cookie, Flower) and 10 pixel characters (cat, dog, pokeball, shield, batman, owl, robot, ghost, kitty, bunny).

---

## Part 1: Motion

### 1.1 Rules the whole app follows

1. **Spatial vs effects springs.** Anything that *moves or changes shape* uses `motionScheme.defaultSpatialSpec()` or `slowSpatialSpec()`. Anything that changes *color or alpha* uses `fastEffectsSpec()` or `slowEffectsSpec()`. Spatial springs overshoot, effects springs don't. Mixing them up is what makes motion look cheap.
2. **Every tappable surface reacts.** There are no plain ripples on cards. Press = squash.
3. **Entrances are staggered, once.** Lists rise in; they never replay on scroll-back.
4. **Ambient motion is slow.** Idle loops run at 14 to 60 s per rotation, so the screen feels alive but never busy.
5. **One kill switch.** `Disable Animations` swaps in a motion scheme where every spec is `snap()`. `calm` stops only the ambient loops (`if (still) 0f else …`). Every looping animation checks `calm`.
6. **Every meaningful event has a haptic** (LIGHT, MEDIUM, HEAVY, SELECTION, SUCCESS, WARNING, ERROR, each with its own waveform).

### 1.2 Interaction animations

| Name | Behavior | Numbers |
|---|---|---|
| **`springClick`** | Card or tile squashes on press, springs back on release, fires a SELECTION haptic. On D-pad focus it lifts and draws a ring | press scale **0.94**, focus scale **1.04**, focus ring **3dp** primary |
| **Morph on press** | Category blobs and social buttons morph toward a circle and rotate | rotate **30–45°** |
| **Button shapes** | `ButtonDefaults.shapes()` / `IconButtonDefaults.shapes()` change corner radius on press | M3 built-in |
| **Nav icon pop** | Selected dock icon scales up | **0.7 → 1.0** |
| **Dock hide** | Slides off-screen on scroll down, returns on scroll up | spatial spring |
| **Swipe row** | Horizontal drag reveals an action. See 1.6 | trip **88dp**, fling **1600 px/s** |

```kotlin
// Simplified from Common.kt: the shape of springClick, not the full source
fun Modifier.springClick(shape: Shape, onClick: () -> Unit) = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .94f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(), label = "press")
    this.graphicsLayer { scaleX = s; scaleY = s }
        .clip(shape)
        .clickable(src, indication = null) { haptic(SELECTION); onClick() }
}
```

### 1.3 Entrance and transition animations

| Name | Behavior | Numbers |
|---|---|---|
| **`reveal(index)`** | Item rises and fades in | rise **28dp**, **45 ms** stagger per item, first **9** items only, played once (survives rotation and tab switches) |
| **Tab change** | Fade-through, new tab scales in slightly | scale-in from **0.98**, *effects* springs (a spatial spring made the whole screen wobble) |
| **Shared-element icon** | App icon flies from card or row to the detail header | `SharedTransitionLayout`, key `"from|appId"` |
| **Sheets** | Close through `LocalSheetClose` so the hide animation finishes before dismissal | M3 `ModalBottomSheet` |
| **Swipe-remove** | Row slides off, then collapses | `tween(160, FastOutLinearIn)` then `shrinkVertically + fadeOut` |
| **Waiting text** (AI) | Status line slides up and out, next slides in | every **1400 ms**, `slideInVertically + fadeIn` |
| **Count-up numbers** | Stats count from 0 when shown | `tween(1100, FastOutSlowIn)` |
| **Boss progress bar** | Fills from 0 when the card appears | `slowSpatialSpec` |

```kotlin
// Simplified reveal(): stagger a rise + fade by list index
fun Modifier.reveal(index: Int) = composed {
    var shown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (index < 9) delay(index * 45L); shown = true }
    val p by animateFloatAsState(if (shown || index >= 9) 1f else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(), label = "reveal")
    this.graphicsLayer { alpha = p; translationY = (1f - p) * 28.dp.toPx() }
}
```

### 1.4 Hero, carousel and parallax

- **Hero pager:** auto-advances every **5.5 s** with a slow glide. Off-center cards scale to **0.93**. Screenshots on the card move at a different depth than the text (parallax). A cookie shape rotates behind them. Page dots: the active dot stretches to **22dp**.
- **Recommended carousel:** M3 multi-browse. Card text fades as the card shrinks toward the edge.
- **Rank and spotlight rows:** staggered `reveal`, no extra animation.

### 1.5 Ambient (idle) animations

| Where | Motion | Period / values |
|---|---|---|
| Orion AI star (draggable button) | Cookie rotates | **14 s**, linear |
| Dev coin halo | Soft-burst shape rotates; showcased badges orbit it, scaling **0.85 → 1.05** and fading **0.7 → 1.0** with depth | **30 s** |
| Guide hero | Cookie 12-sided drifts; logo blob morphs to a new shape every | **2.6 s** shape swap, **50 s** drift |
| Gate (forced update) | Big shape spins; fill pulses between **0.35 ↔ 0.85** | **60 s** spin, **4 s** pulse |
| Pilot ID card | Rainbow foil sweeps across | **7 s** |
| Settings previews | Shape spinner | configurable period |
| Welcome pulse | Scale **1.0 ↔ 1.18** | **700 ms**, reverse |
| Skeleton shimmer | Alpha **0.45 ↔ 0.9** | **900 ms**, reverse |
| AI orb | Morphs between `AI_SHAPES` | **2.4 s** idle, **0.65 s** while thinking |
| Pull-to-refresh (refreshing) | Shapes spin; pixel characters bob | spin **900 ms**/turn, pulse ±6% |

### 1.6 Gesture physics

**Swipe-to-act (`SwipeRow`)**
- Trip distance **88dp**. Crossing it fires a SELECTION haptic and flips the background to the action's color.
- A fast fling (**> 1600 px/s**, over 40% of trip) counts as a swipe even if short.
- Actions that don't remove the row get **rubber-band resistance (×0.35)** past the trip point, so the row never looks like it's leaving.
- Release below trip → spring back. Release above → removing actions slide off and collapse (MEDIUM haptic), non-removing ones run and spring back (SUCCESS haptic).
- The action icon scales **0.4 → 1.0**, fades in and un-rotates (−45° → 0) as you drag.
- First-run **hint:** after **420 ms** the row nudges to 70% of trip, waits **450 ms**, springs back.

**Theme switch (`ThemeReveal`)**
1. Snapshot the current frame into a bitmap (**120 ms** capture timeout; if capture fails the theme change still lands).
2. Apply the new theme underneath.
3. Draw the old snapshot with a growing **circular hole** that starts at the tapped control and ends at `hypot(w, h) × 1.05`, driven by `slowEffectsSpec`.

**3D flip and tilt**
- Dev coin / pilot card flip **180°** on `slowSpatialSpec`, `cameraDistance = 12 × density`, the back face is un-mirrored (`rot > 90 ? rot − 180 : rot`), and the shadow scales with `|cos(rot)|` so it vanishes edge-on.
- Pilot card tilts with the gravity sensor (`rememberTilt`) and the finger: **±22°** max on each axis (sensor × 12 + drag ÷ 10). It has a rainbow foil (pink → cyan → lime → gold at ~40% alpha, shifting with tilt and time) and a white radial **specular glint** that moves opposite the tilt. The card stays still while its back is shown so signing works.

### 1.7 Pull-to-refresh indicator

A 60dp cookie-shaped container that fades in, slides down (−48dp → 0), and scales **0.6 → 1.0** as you pull.

- **Expressive options** (Morph loader, Sunny, Cookie, Flower): shape rotates `frac × 270°` while pulling, spins once per 900 ms while refreshing, with a refresh icon counter-rotating so it stays upright.
- **Pixel characters:** each face is baked once to a 1px-per-cell bitmap (11 faces in the picker; per-cell `drawRect` stuttered). Eyes **close** while pulling (a one-row swap). While refreshing the character bobs `sin(t / 110) × 55%` of a pixel-cell. Specials: **Poke Ball** shakes side to side (`sin(t / 38)`), **Cap Shield** spins **540°** over **440 ms** with a cubic ease-out.

### 1.8 Launch splash

Android 12+ splash is an **animated vector, 900 ms**, on a `splash_bg` color. The static mark is the Orion logo on a 9-sided cookie.

| Element | Starts at | Motion |
|---|---|---|
| Cookie blob | 0 | scale **0.2 → 1.0** (overshoot, peak at 55%), rotation **−120° → 0°** (fast-out-slow-in, done at 90%) |
| Triangle | 30% | scale 0 → 1 with overshoot at 65%, drops in from **−14** units |
| Circle | 42% | scale 0 → 1, overshoot at 77% |
| Square | slightly later | scale + rotation |
| "RE" pill | last | pops in under the logo |

Splash colors: light `#FCF8FE` bg / `#4C4ECD` shape / `#FBF7FF` logo; dark `#0E0E12` bg / `#C0C1FF` shape / `#2A29AD` logo. Older Android draws the static `ic_splash` as-is.

### 1.9 Loading and progress

- **Skeletons** mirror the real layout (hero 176dp / 28dp corners, lane titles, 76dp tiles, or 7 rows in Classic) with the 900 ms alpha shimmer.
- **Download state:** the icon shrinks 20% and its corners animate toward a circle while a **wavy circular progress ring** wraps it. On the detail screen the primary button becomes a **wavy linear progress bar** with a % pill.
- **M3 Expressive** `LoadingIndicator` / `ContainedLoadingIndicator` for indeterminate waits.

### 1.10 Easter eggs and delight

Offline **Chrome Dino** in a WebView (`assets/dino/`), tied to badges (Dino Rookie, Master, Legend). A gamified **ad quest** ("Fuel the Code") with ~70 story cards, 7 bosses, and badge drops. **Developer mode** unlocks after 8 taps on the title. These matter for tone: the app is willing to be silly.

---

## Part 2: Icons

### 2.1 In-app icon set

| Set | Count | Notes |
|---|---|---|
| **Material Symbols Rounded** (`Sym.*`) | **292** vectors | Generated by `tools/gen-symbols.mjs` into `Symbols.kt` as path strings, parsed lazily and cached. 24dp, 960-unit viewport. **30** have `Filled` variants, used for the selected nav tab and active toggles |
| **Brand icons** | 7 | GitHub, GitLab, Codeberg, Discord, X, Ko-fi, PayPal |
| **Font Awesome mapping** (`faIcon`) | ~70 names | Remote config sends FA class names (`"fas fa-bolt"`); `faIcon()` maps them to Material equivalents, with fallback `Apps` |
| **Badges** | 16 | Each is an icon plus an accent color, shown through `tonal()` so it fits every theme |

Badge accents: Legend `#EAB308`, Guardian `#EF4444`, Void Walker `#A855F7`, Supernova `#EC4899`, Backer `#06B6D4`, Dev Mode `#D946EF`, Dino Rookie `#10B981`, Dino Master `#3B82F6`, Dino Legend `#F97316`, Merge Tamer `#14B8A6`, Spaghetti Slayer `#F59E0B`, Sudo Sovereign `#E11D48`, Icon Collector `#8B5CF6`, Dock Master `#22C55E`, Bronze / Silver / Gold Wings.

**Usage rules**
- Selected = filled, unselected = outlined.
- Icons in lists sit in a **tonal circle** (40dp) or a morphing shape, never bare.
- Remote app icons are shown raw (no frame), with install state as a corner badge.

### 2.2 Launcher icons (7, switchable)

All are adaptive icons with a **monochrome layer** for Android 13+ themed icons. The default is inset **16.7%** from PNG layers. Variants are separate `activity-alias` entries, so only one is enabled at a time. The default alias keeps the old `.MainActivity` name so home-screen shortcuts survive updates.

| Variant | Background | Foreground | Unlock |
|---|---|---|---|
| **Orion** (default) | indigo gradient | white triangle, circle, square | free |
| **Midnight** | `#1E1B3A → #05050A` | white + lime `#C6FF3D` | free |
| **Sunset** | `#FFB347 → #FF2E7E` | white | free |
| **Ocean** | `#00D2B4 → #0049E6` | white | free |
| **Mint** | `#E8FFF4 → #B5F5D6` | green `#0E7A55` | free |
| **Neon** | `#2A0B5E → #0A0118` | cyan `#00F0FF`, lime `#B6FF00`, pink `#FF2BD6` | **50** ad-quest ads |
| **Gold** | `#2B2416 → #0B0906` | gold gradient `#FFE9A8 → #C8922B` | **100** ads |

Unlocking icons and docks feeds the **Icon Collector** and **Dock Master** badges.

### 2.3 The logo

Three white primitives: a rounded **triangle** (top), a **circle** (bottom left), a rounded **square** (bottom right), on an indigo gradient rounded square (`#7B7FE8 → #5B5FDE`, 226/1024 corner radius). The same three shapes are rebuilt as separate animated groups in the splash.

---

## Part 3: Other assets

### 3.1 Android resources (`res/`)

| Asset | Purpose |
|---|---|
| `ic_splash.xml`, `ic_splash_animated.xml`, `ic_splash_logo.xml` | Static mark, animated splash, plain logo glyph (reused as an icon inside the Guide hero, tinted `onPrimary`) |
| `ic_launcher_monochrome.xml` | Themed-icon layer, logo at 72% scale |
| `ic_icon_<variant>_bg/fg.xml` | Layers for the 6 alternate launcher icons |
| `tv_banner.xml` | Android TV banner, **320 × 180dp**, gradient `#3F40C0 → #7478F8` at 315°, 120dp logo centered |
| `values(-night)/colors.xml` | Only the 3 splash colors. All in-app color is generated in code |
| `Theme.Orion` | `forceDarkAllowed = false` (Orion does its own theming) |
| `Theme.Orion.Installer` | Fully transparent, no dim, no window animation, so the APK-install sheet floats over whichever app opened the file |
| `xml/file_paths.xml` | FileProvider paths for APK install and export |

### 3.2 Bundled files (`app/src/main/assets/`)

| File | Purpose |
|---|---|
| `dino/` (`game.html`, `index.js` ≈ 90 KB, `index.css`, 1× and 2× sprite PNGs) | Offline Chrome Dino game, bridged to Kotlin for score and game-over |
| `local_apps.json` (≈ 30 KB) | Fallback catalog used when the remote source is unreachable (the footer shows "Source: Local") |

### 3.3 Downloaded at runtime

- **Fonts:** Google Fonts TTFs fetched once into `filesDir/fonts` (variable weight 400–700). Custom user fonts up to 8 MB.
- **Avatars:** DiceBear SVG, requested as PNG.
- **App icons and screenshots:** Coil 3 from the catalog.

### 3.4 Notifications

| Channel | Importance | Small icon |
|---|---|---|
| Download Progress | Low, no badge | system download glyph |
| Orion Updates | Default | system download-done glyph, or the monochrome launcher glyph for summary notifications |

### 3.5 Repository marketing assets (`/assets`, not shipped in the APK)

| File | Use |
|---|---|
| `shape-1.svg … shape-9.svg` | Nine 48dp `MaterialShapes` polygons exported as SVG for the README |
| `feature-ai / dev / install / palette / release / sentinel / tools / update.svg` | Eight feature illustrations |
| `m3-chips-*`, `m3-divider-*`, `m3-hero-*` (light + dark) | README banners drawn in the app's own style |
| `orion-logo.svg`, `orion_logo_512.png`, `icon*.png`, `orion-badge.png`, `store-badge.jpg` | Brand marks and badges |
| `screenshots/` | Store screenshots |

### 3.6 Build-time generators (`tools/`)

- `gen-scheme.mjs`: builds `Palettes.kt` (14 palettes × light / dark / dusk / OLED) from material-color-utilities. **Don't hand-edit.**
- `gen-symbols.mjs`: builds `Symbols.kt` from Material Symbols. **Don't hand-edit.**

---

## Part 4: Porting this to another app, motion checklist

When the UI looks "static", it is usually missing these, in roughly this order of impact:

1. `springClick` on every card, row and tile
2. `reveal(index)` stagger on first load of every list
3. Number **count-up** on every headline figure (1100 ms)
4. Progress bars that **fill from 0** on entry, and switch to wavy for active work
5. Color from content (`tonal()` of a category, account or icon color) instead of one accent everywhere
6. Shape morphing on icon avatars and on selected nav items
7. Shared-element transitions between list and detail
8. Swipe actions with haptics on list rows
9. A themed pull-to-refresh and a circular theme reveal
10. One slow ambient loop in a hero area (about 14 to 60 s), disabled by `calm`
11. A real empty state for every empty list (shape + icon + one action)
12. Haptics on success, warning and error
