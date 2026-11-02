package com.efina.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Palettes and semantic colours, following design.md sections 2.3 and 2.4.
 *
 * The reference app ships 14 palettes. Efina ships two — Orion (the default, per
 * design.md) and Mono — plus Material You on Android 12+ (the `"dynamic"` theme
 * value). DESIGN-PARITY.md records the reduction; the token names are identical, so
 * adding a palette is one more object here.
 *
 * Money semantics are Efina-specific and don't exist in the design doc: income
 * and expense must be instantly distinguishable, so they get dedicated tokens
 * rather than borrowing the app-store "Ready to install" colours.
 */

// ── Orion (design.md 2.3) ───────────────────────────────────────────

object OrionLight {
    val primary = Color(0xFF4C4ECD)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFF7478F8)
    val onPrimaryContainer = Color(0xFF0B0B36)
    val secondary = Color(0xFF5B5D72)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFE1E0FF)
    val onSecondaryContainer = Color(0xFF181A2C)
    val tertiary = Color(0xFF456900)
    val onTertiary = Color(0xFFFFFFFF)
    val tertiaryContainer = Color(0xFFCCFE82)   // acid lime
    val onTertiaryContainer = Color(0xFF121F00)
    val background = Color(0xFFFCF8FE)
    val onBackground = Color(0xFF1B1B21)
    val surface = Color(0xFFFCF8FE)
    val onSurface = Color(0xFF1B1B21)
    val surfaceVariant = Color(0xFFE4E1ED)
    val onSurfaceVariant = Color(0xFF46464F)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainerLow = Color(0xFFF6F2FB)
    val surfaceContainer = Color(0xFFF0ECF5)
    val surfaceContainerHigh = Color(0xFFEAE7F0)
    val surfaceContainerHighest = Color(0xFFE4E1ED)
    val outline = Color(0xFF777680)
    val outlineVariant = Color(0xFFC7C5D0)
    val error = Color(0xFFA8364B)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFD9DE)
    val onErrorContainer = Color(0xFF400013)
}

object OrionDark {
    val primary = Color(0xFFC0C1FF)
    val onPrimary = Color(0xFF1B1B60)
    val primaryContainer = Color(0xFF3F40C0)
    val onPrimaryContainer = Color(0xFFE1E0FF)
    val secondary = Color(0xFFC4C5DD)
    val onSecondary = Color(0xFF2D2F42)
    val secondaryContainer = Color(0xFF393953)
    val onSecondaryContainer = Color(0xFFE1E0FF)
    val tertiary = Color(0xFFF1FFD4)
    val onTertiary = Color(0xFF243600)
    val tertiaryContainer = Color(0xFFCAFB80)
    val onTertiaryContainer = Color(0xFF102000)
    val background = Color(0xFF0E0E12)
    val onBackground = Color(0xFFE5E2E9)
    val surface = Color(0xFF0E0E12)
    val onSurface = Color(0xFFE5E2E9)
    val surfaceVariant = Color(0xFF46464F)
    val onSurfaceVariant = Color(0xFFC7C5D0)
    val surfaceContainerLowest = Color(0xFF09090C)
    val surfaceContainerLow = Color(0xFF131318)
    val surfaceContainer = Color(0xFF1A1A21)
    val surfaceContainerHigh = Color(0xFF20202A)
    val surfaceContainerHighest = Color(0xFF25252E)
    val outline = Color(0xFF918F9A)
    val outlineVariant = Color(0xFF46464F)
    val error = Color(0xFFF97386)
    val onError = Color(0xFF5B0F24)
    val errorContainer = Color(0xFF7C2237)
    val onErrorContainer = Color(0xFFFFD9DE)
}

// ── Semantic and status (design.md 2.4) ─────────────────────────────

object EfinaSemantic {
    val safe = Color(0xFF10B981)
    val caution = Color(0xFFF59E0B)
    val danger = Color(0xFFEF4444)
    val warn = Color(0xFFEAB308)
    val info = Color(0xFF3B82F6)
}

/**
 * Money colours. Deliberately not part of the M3 scheme: a palette swap must not
 * turn an expense green. These are tuned to stay legible on both light and dark
 * surfaces, and are paired with a foreground for text drawn on the container.
 */
object MoneyColors {
    val incomeLight = Color(0xFF0E7C5A)
    val incomeOnLight = Color(0xFFD7FBE8)
    val expenseLight = Color(0xFFB3261E)
    val expenseOnLight = Color(0xFFFFDAD6)
    val transferLight = Color(0xFF2A5EA8)
    val transferOnLight = Color(0xFFD8E5FF)

    val incomeDark = Color(0xFF6FDCAF)
    val incomeOnDark = Color(0xFF00382A)
    val expenseDark = Color(0xFFFFB4AB)
    val expenseOnDark = Color(0xFF690005)
    val transferDark = Color(0xFFA8C8FF)
    val transferOnDark = Color(0xFF002F66)
}

/**
 * `tonal()` from design.md 2.5: derive a (container, onContainer) pair from an
 * arbitrary accent so backend-provided category colours always look native.
 * Lightness 0.88/0.26 for containers, 0.3/0.82 for foreground.
 */
fun tonal(accent: Color, dark: Boolean): Pair<Color, Color> {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(accent.toArgbInt(), hsl)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0f, 1f)

    val container = Color.hsl(h, (s * 0.55f).coerceIn(0.05f, 0.5f), if (dark) 0.26f else 0.88f)
    val onContainer = Color.hsl(h, (s * 0.75f).coerceIn(0.05f, 0.6f), if (dark) 0.82f else 0.30f)
    return container to onContainer
}

private fun Color.toArgbInt(): Int = this.toArgb()

/**
 * The 14 palettes design.md §2.2 names, as seed accents.
 *
 * Only Orion ships a hand-tuned full scheme. The other thirteen derive a complete
 * ColorScheme from a single seed through [tonal], which is how the doc's
 * material-color-utilities generator works in principle: one tonal spot, four
 * schemes. Deriving beats hand-writing 13 x 2 x 30 roles, and it means adding a
 * palette is one hex value rather than a file.
 *
 * `orion` and `mono` are absent here on purpose: they have hand-tuned schemes in
 * Theme.kt, which look better than a derived approximation for the default.
 */
val PaletteSeeds: Map<String, Color> = mapOf(
    "ocean" to Color(0xFF1E6FD9),
    "forest" to Color(0xFF2E7D46),
    "sunset" to Color(0xFFE85D3D),
    "rose" to Color(0xFFD24C7F),
    "grape" to Color(0xFF7B4BC4),
    "mint" to Color(0xFF19A97D),
    "ember" to Color(0xFFC2410C),
    "amber" to Color(0xFFC98A00),
    "cyber" to Color(0xFF00A8B5),
    "midnight" to Color(0xFF3B4B9C),
    "lime" to Color(0xFF5A9E10),
    "cocoa" to Color(0xFF8A5A3C)
)

/** Parses "#RRGGBB", "#AARRGGBB" or a named fallback into a Color. */
fun parseHexColor(value: String?, fallback: Color): Color {
    if (value.isNullOrBlank()) return fallback
    val hex = value.removePrefix("#")
    return runCatching {
        when (hex.length) {
            6 -> Color(hex.toLong(16) or 0xFF000000)
            8 -> Color(hex.toLong(16))
            else -> fallback
        }
    }.getOrDefault(fallback)
}
