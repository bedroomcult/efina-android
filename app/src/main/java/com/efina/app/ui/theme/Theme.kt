package com.efina.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Theme, following design.md section 2.1.
 *
 * Available modes: Orion Light (default), Orion Dark, Mono Light, Mono Dark, and
 * Material You on Android 12+. design.md also lists a "Dusk" mode (a lifted dark)
 * and an OLED black; both are recorded as not implemented in DESIGN-PARITY.md
 * rather than silently dropped.
 *
 * A [MoneyPalette] is provided alongside the M3 scheme because income/expense
 * must not change hue with the theme palette.
 */

data class MoneyPalette(
    val income: Color,
    val onIncome: Color,
    val expense: Color,
    val onExpense: Color,
    val transfer: Color,
    val onTransfer: Color
)

val LocalMoneyPalette = staticCompositionLocalOf {
    MoneyPalette(
        income = MoneyColors.incomeLight,
        onIncome = MoneyColors.incomeOnLight,
        expense = MoneyColors.expenseLight,
        onExpense = MoneyColors.expenseOnLight,
        transfer = MoneyColors.transferLight,
        onTransfer = MoneyColors.transferOnLight
    )
}

/** True when the active theme is dark, so `tonal()` picks the right lightness. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private fun orionLightScheme(): ColorScheme = lightColorScheme(
    primary = OrionLight.primary,
    onPrimary = OrionLight.onPrimary,
    primaryContainer = OrionLight.primaryContainer,
    onPrimaryContainer = OrionLight.onPrimaryContainer,
    secondary = OrionLight.secondary,
    onSecondary = OrionLight.onSecondary,
    secondaryContainer = OrionLight.secondaryContainer,
    onSecondaryContainer = OrionLight.onSecondaryContainer,
    tertiary = OrionLight.tertiary,
    onTertiary = OrionLight.onTertiary,
    tertiaryContainer = OrionLight.tertiaryContainer,
    onTertiaryContainer = OrionLight.onTertiaryContainer,
    background = OrionLight.background,
    onBackground = OrionLight.onBackground,
    surface = OrionLight.surface,
    onSurface = OrionLight.onSurface,
    surfaceVariant = OrionLight.surfaceVariant,
    onSurfaceVariant = OrionLight.onSurfaceVariant,
    surfaceContainerLowest = OrionLight.surfaceContainerLowest,
    surfaceContainerLow = OrionLight.surfaceContainerLow,
    surfaceContainer = OrionLight.surfaceContainer,
    surfaceContainerHigh = OrionLight.surfaceContainerHigh,
    surfaceContainerHighest = OrionLight.surfaceContainerHighest,
    outline = OrionLight.outline,
    outlineVariant = OrionLight.outlineVariant,
    error = OrionLight.error,
    onError = OrionLight.onError,
    errorContainer = OrionLight.errorContainer,
    onErrorContainer = OrionLight.onErrorContainer
)

private fun orionDarkScheme(): ColorScheme = darkColorScheme(
    primary = OrionDark.primary,
    onPrimary = OrionDark.onPrimary,
    primaryContainer = OrionDark.primaryContainer,
    onPrimaryContainer = OrionDark.onPrimaryContainer,
    secondary = OrionDark.secondary,
    onSecondary = OrionDark.onSecondary,
    secondaryContainer = OrionDark.secondaryContainer,
    onSecondaryContainer = OrionDark.onSecondaryContainer,
    tertiary = OrionDark.tertiary,
    onTertiary = OrionDark.onTertiary,
    tertiaryContainer = OrionDark.tertiaryContainer,
    onTertiaryContainer = OrionDark.onTertiaryContainer,
    background = OrionDark.background,
    onBackground = OrionDark.onBackground,
    surface = OrionDark.surface,
    onSurface = OrionDark.onSurface,
    surfaceVariant = OrionDark.surfaceVariant,
    onSurfaceVariant = OrionDark.onSurfaceVariant,
    surfaceContainerLowest = OrionDark.surfaceContainerLowest,
    surfaceContainerLow = OrionDark.surfaceContainerLow,
    surfaceContainer = OrionDark.surfaceContainer,
    surfaceContainerHigh = OrionDark.surfaceContainerHigh,
    surfaceContainerHighest = OrionDark.surfaceContainerHighest,
    outline = OrionDark.outline,
    outlineVariant = OrionDark.outlineVariant,
    error = OrionDark.error,
    onError = OrionDark.onError,
    errorContainer = OrionDark.errorContainer,
    onErrorContainer = OrionDark.onErrorContainer
)

/** Neutral palette: same structure, zero chroma, for users who want no colour. */
private fun monoScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme(
        primary = Color(0xFFE2E2E6),
        onPrimary = Color(0xFF1A1A1D),
        primaryContainer = Color(0xFF3A3A3F),
        onPrimaryContainer = Color(0xFFF0F0F4),
        secondaryContainer = Color(0xFF2B2B30),
        onSecondaryContainer = Color(0xFFE2E2E6),
        tertiary = Color(0xFFD0D0D6),
        tertiaryContainer = Color(0xFF3A3A3F),
        onTertiaryContainer = Color(0xFFF0F0F4),
        background = Color(0xFF0D0D0F),
        surface = Color(0xFF0D0D0F),
        onSurface = Color(0xFFE4E4E7),
        surfaceContainerLow = Color(0xFF141416),
        surfaceContainer = Color(0xFF1A1A1D),
        surfaceContainerHigh = Color(0xFF212124),
        surfaceContainerHighest = Color(0xFF28282C),
        outline = Color(0xFF8C8C93),
        error = Color(0xFFF97386)
    ) else lightColorScheme(
        primary = Color(0xFF2C2C30),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDEDEE3),
        onPrimaryContainer = Color(0xFF1A1A1D),
        secondaryContainer = Color(0xFFE8E8EC),
        onSecondaryContainer = Color(0xFF2C2C30),
        tertiary = Color(0xFF5C5C63),
        tertiaryContainer = Color(0xFFE0E0E6),
        onTertiaryContainer = Color(0xFF1A1A1D),
        background = Color(0xFFFCFCFD),
        surface = Color(0xFFFCFCFD),
        onSurface = Color(0xFF1A1A1D),
        surfaceContainerLow = Color(0xFFF6F6F8),
        surfaceContainer = Color(0xFFF1F1F4),
        surfaceContainerHigh = Color(0xFFEAEAEE),
        surfaceContainerHighest = Color(0xFFE4E4E9),
        outline = Color(0xFF77777E),
        error = Color(0xFFA8364B)
    )

/**
 * The theme the user picked, held in memory so [EfinaTheme] can read it without a
 * ViewModel being threaded through MainActivity.
 *
 * Settings writes `theme` to PUT /settings; before this existed nothing read it
 * back, so the appearance chips appeared to work and changed nothing on restart.
 * Set from SettingsViewModel via [ThemePreference.apply].
 */
object ThemePreference {
    private val _name = MutableStateFlow("orion")
    val name: StateFlow<String> = _name.asStateFlow()

    /** True when the stored value asks for a dark surface. */
    @Composable
    fun current(): Pair<String, Boolean> {
        val stored by name.collectAsState()
        return when (stored) {
            "dark", "mono-dark" -> (if (stored == "mono-dark") "mono" else "orion") to true
            "light", "mono" -> (if (stored == "mono") "mono" else "orion") to false
            "system" -> "orion" to isSystemInDarkTheme()
            // Older/unknown values fall back to following the system.
            else -> "orion" to isSystemInDarkTheme()
        }
    }

    fun apply(storedTheme: String?) {
        if (!storedTheme.isNullOrBlank()) _name.value = storedTheme
    }
}

@Composable
fun EfinaTheme(
    themeName: String = "orion",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        themeName == "mono" -> monoScheme(darkTheme)
        else -> if (darkTheme) orionDarkScheme() else orionLightScheme()
    }

    val money = if (darkTheme) MoneyPalette(
        income = MoneyColors.incomeDark,
        onIncome = MoneyColors.incomeOnDark,
        expense = MoneyColors.expenseDark,
        onExpense = MoneyColors.expenseOnDark,
        transfer = MoneyColors.transferDark,
        onTransfer = MoneyColors.transferOnDark
    ) else MoneyPalette(
        income = MoneyColors.incomeLight,
        onIncome = MoneyColors.incomeOnLight,
        expense = MoneyColors.expenseLight,
        onExpense = MoneyColors.expenseOnLight,
        transfer = MoneyColors.transferLight,
        onTransfer = MoneyColors.transferOnLight
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalMoneyPalette provides money,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = EfinaShapes,
            content = content
        )
    }
}
