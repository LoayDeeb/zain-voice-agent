package com.zain.jordan.voiceapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ZainBlue,
    onPrimary = White,
    primaryContainer = ZainBlueDark,
    onPrimaryContainer = White,
    secondary = ZainBlueLight,
    onSecondary = White,
    secondaryContainer = ZainBlueDark,
    onSecondaryContainer = White,
    tertiary = ZainBlueLight,
    onTertiary = White,
    background = Gray900,
    onBackground = White,
    surface = Gray800,
    onSurface = White,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray200,
    error = ErrorRed,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = ZainBlue,
    onPrimary = White,
    primaryContainer = ZainBlueLight,
    onPrimaryContainer = White,
    secondary = ZainBlueDark,
    onSecondary = White,
    secondaryContainer = ZainBlueLight,
    onSecondaryContainer = White,
    tertiary = ZainBlueLight,
    onTertiary = White,
    background = White,
    onBackground = Gray900,
    surface = Gray100,
    onSurface = Gray900,
    surfaceVariant = Gray200,
    onSurfaceVariant = Gray700,
    error = ErrorRed,
    onError = White
)

@Composable
fun ZainVoiceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
