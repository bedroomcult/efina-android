package com.efina.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VegasGold,
    onPrimary = JetBlack,
    primaryContainer = VegasGoldDark,
    onPrimaryContainer = Color.White,
    secondary = Onyx,
    onSecondary = Color.White,
    secondaryContainer = OnyxDark,
    onSecondaryContainer = Gray200,
    tertiary = Gray400,
    onTertiary = Color.Black,
    tertiaryContainer = Gray700,
    onTertiaryContainer = Color.White,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = JetBlack,
    onBackground = Gray100,
    surface = JetBlackLight,
    onSurface = Gray100,
    surfaceVariant = OnyxDark,
    onSurfaceVariant = Gray400,
    outline = Gray600,
    outlineVariant = Gray800,
    inverseSurface = Gray100,
    inverseOnSurface = Gray900,
    inversePrimary = VegasGoldDark
)

private val LightColorScheme = lightColorScheme(
    primary = VegasGold,
    onPrimary = JetBlack,
    primaryContainer = VegasGoldLight,
    onPrimaryContainer = JetBlackDark,
    secondary = Onyx,
    onSecondary = Color.White,
    secondaryContainer = OnyxLight,
    onSecondaryContainer = Color.White,
    tertiary = Gray500,
    onTertiary = Color.White,
    tertiaryContainer = Gray200,
    onTertiaryContainer = Gray900,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF410002),
    background = Gray50,
    onBackground = Gray900,
    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray400,
    outlineVariant = Gray200,
    inverseSurface = Gray900,
    inverseOnSurface = Gray50,
    inversePrimary = VegasGoldLight
)

@Composable
fun EfinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
