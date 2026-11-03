#!/usr/bin/env node
/**
 * Generates the seeded palettes as correct M3 tonal ramps.
 *
 * Adapted from Orion-Store's tools/gen-scheme.mjs. The previous implementation
 * derived colour schemes with HSL arithmetic, which is an approximation: Material 3
 * ramps are built in HCT (hue/chroma/tone), where tone is perceptual lightness. HSL
 * lightness and HCT tone diverge badly at the ends, so the hand-derived palettes had
 * primary containers that were too dark and on-colours that did not always clear
 * contrast.
 *
 * Usage (needs material-color-utilities available to node):
 *   npm i --no-save @material/material-color-utilities
 *   node tools/gen-scheme.mjs
 *
 * Writes app/src/main/java/com/efina/app/ui/theme/Palettes.kt.
 */
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const here = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(here, '..', 'app/src/main/java/com/efina/app/ui/theme/Palettes.kt');

let mcu;
try {
  mcu = await import('@material/material-color-utilities');
} catch {
  console.error(
    'material-color-utilities is not installed.\n' +
    'Run: npm i --no-save @material/material-color-utilities'
  );
  process.exit(2);
}
const { Hct, DynamicScheme, Variant, TonalPalette, argbFromHex, hexFromArgb, MaterialDynamicColors } = mcu;

// id, label, primary seed, tertiary seed, primary chroma, tertiary chroma.
// Hues chosen to match the twelve palettes design.md names.
const PALETTES = [
  ['ocean', 'Ocean', '#1E6FD9', '#00A8B5', 56, 44],
  ['forest', 'Forest', '#2E7D46', '#C98A00', 48, 52],
  ['sunset', 'Sunset', '#E85D3D', '#7B4BC4', 60, 48],
  ['rose', 'Rose', '#D24C7F', '#19A97D', 60, 44],
  ['grape', 'Grape', '#7B4BC4', '#D24C7F', 60, 48],
  ['mint', 'Mint', '#19A97D', '#3B4B9C', 48, 44],
  ['ember', 'Ember', '#C2410C', '#C98A00', 64, 52],
  ['amber', 'Amber', '#C98A00', '#00A8B5', 52, 44],
  ['cyber', 'Cyber', '#00A8B5', '#7B4BC4', 56, 56],
  ['midnight', 'Midnight', '#3B4B9C', '#1E6FD9', 52, 44],
  ['lime', 'Lime', '#5A9E10', '#19A97D', 56, 44],
  ['cocoa', 'Cocoa', '#8A5A3C', '#C98A00', 40, 40],
];

// The M3 role slots this app actually reads through ColorScheme.
const ROLES = [
  ['primary', 'primary'],
  ['onPrimary', 'onPrimary'],
  ['primaryContainer', 'primaryContainer'],
  ['onPrimaryContainer', 'onPrimaryContainer'],
  ['secondary', 'secondary'],
  ['onSecondary', 'onSecondary'],
  ['secondaryContainer', 'secondaryContainer'],
  ['onSecondaryContainer', 'onSecondaryContainer'],
  ['tertiary', 'tertiary'],
  ['onTertiary', 'onTertiary'],
  ['tertiaryContainer', 'tertiaryContainer'],
  ['onTertiaryContainer', 'onTertiaryContainer'],
  ['error', 'error'],
  ['onError', 'onError'],
  ['errorContainer', 'errorContainer'],
  ['onErrorContainer', 'onErrorContainer'],
  ['background', 'background'],
  ['onBackground', 'onBackground'],
  ['surface', 'surface'],
  ['onSurface', 'onSurface'],
  ['surfaceVariant', 'surfaceVariant'],
  ['onSurfaceVariant', 'onSurfaceVariant'],
  ['surfaceContainerLowest', 'surfaceContainerLowest'],
  ['surfaceContainerLow', 'surfaceContainerLow'],
  ['surfaceContainer', 'surfaceContainer'],
  ['surfaceContainerHigh', 'surfaceContainerHigh'],
  ['surfaceContainerHighest', 'surfaceContainerHighest'],
  ['outline', 'outline'],
  ['outlineVariant', 'outlineVariant'],
  ['scrim', 'scrim'],
];

function schemeFor(seed, tertiarySeed, primaryChroma, tertiaryChroma, darkness) {
  // material-color-utilities 0.4 takes a source Hct, not an ARGB int.
  const seedHct = Hct.fromInt(argbFromHex(seed));
  const scheme = new DynamicScheme({
    sourceColorHct: seedHct,
    variant: Variant.TONAL_SPOT,
    contrastLevel: 0.0,
    isDark: darkness !== 'light',
    primaryPalette: TonalPalette.fromHueAndChroma(seedHct.hue, primaryChroma),
    tertiaryPalette: TonalPalette.fromHueAndChroma(Hct.fromInt(argbFromHex(tertiarySeed)).hue, tertiaryChroma),
  });
  const out = {};
  for (const [kotlinName, dynName] of ROLES) {
    const dyn = MaterialDynamicColors[dynName];
    if (!dyn) continue;
    out[kotlinName] = hexFromArgb(dyn.getArgb(scheme));
  }
  return out;
}

/** Dusk and OLED are the dark scheme with surfaces lifted or flattened. */
function duskOf(dark) {
  return { ...dark, background: '#1E1E2E', surface: '#1E1E2E' };
}
function oledOf(dark) {
  return {
    ...dark,
    background: '#000000',
    surface: '#000000',
    surfaceContainerLowest: '#000000',
    surfaceContainerLow: '#0A0A0C',
    surfaceContainer: '#101013',
    surfaceContainerHigh: '#16161A',
    surfaceContainerHighest: '#1C1C21',
  };
}

const emit = (prefix, scheme) =>
  Object.entries(scheme)
    .map(([k, v]) => `        ${k} = Color(0xFF${v.replace('#', '').toUpperCase()})`)
    .join(',\n');

let body = '';
for (const [id, label, seed, tertiary, pc, tc] of PALETTES) {
  const light = schemeFor(seed, tertiary, pc, tc, 'light');
  const dark = schemeFor(seed, tertiary, pc, tc, 'dark');
  body += `    "${id}" to PaletteSet(\n        label = "${label}",\n        light = lightColorScheme(\n${emit(id, light)}\n    ),\n        dark = darkColorScheme(\n${emit(id, dark)}\n    ),\n        dusk = darkColorScheme(\n${emit(id, duskOf(dark))}\n    ),\n        oled = darkColorScheme(\n${emit(id, oledOf(dark))}\n    )\n    ),\n`;
}

const file = `package com.efina.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * GENERATED by tools/gen-scheme.mjs — do not edit by hand.
 *
 * Each palette is a full M3 role set in four lightings, built from a seed accent
 * through material-color-utilities (HCT tonal spot), the same approach design.md
 * specifies. Replaces the earlier hand-derived HSL schemes, which produced
 * containers that were too dark and on-colours that could miss contrast.
 *
 * Regenerate with: npm i --no-save @material/material-color-utilities && node tools/gen-scheme.mjs
 */
data class PaletteSet(
    val label: String,
    val light: ColorScheme,
    val dark: ColorScheme,
    val dusk: ColorScheme,
    val oled: ColorScheme
)

val GeneratedPalettes: Map<String, PaletteSet> = mapOf(
${body})

/** Resolves a palette id and lighting to a scheme; Orion/Mono are hand-tuned. */
fun generatedScheme(id: String, dark: Boolean): ColorScheme? {
    val set = GeneratedPalettes[id] ?: return null
    return when {
        id.endsWith("-dusk") -> set.dusk
        id.endsWith("-oled") -> set.oled
        dark -> set.dark
        else -> set.light
    }
}
`;

writeFileSync(OUT, file);
console.log(`wrote ${PALETTES.length} palettes to ${path.relative(path.resolve(here, '..'), OUT)}`);
