package com.app.summa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val BrutalistDarkColorScheme = darkColorScheme(
    primary = BrutalBlue,
    onPrimary = BrutalWhite,
    primaryContainer = Color(0xFF0D1B4A),
    onPrimaryContainer = BrutalWhite,
    secondary = BrutalWhite,
    onSecondary = BrutalBlack,
    secondaryContainer = Color(0xFF151515),
    onSecondaryContainer = BrutalWhite,
    tertiary = BrutalBlue,
    onTertiary = BrutalWhite,
    tertiaryContainer = Color(0xFF12234F),
    onTertiaryContainer = BrutalWhite,
    background = BrutalBlack,
    onBackground = BrutalWhite,
    surface = Color(0xFF0D0D0D),
    onSurface = BrutalWhite,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFE2E2E2),
    outline = Color(0xFF2F2F2F),
    outlineVariant = Color(0xFF202020),
    error = Color(0xFFFF6B6B),
    onError = BrutalBlack,
    errorContainer = Color(0xFF3D1C1C),
    onErrorContainer = BrutalWhite
)

private val BrutalistLightColorScheme = lightColorScheme(
    primary = BrutalBlue,
    onPrimary = BrutalWhite,
    primaryContainer = BrutalWhite,
    onPrimaryContainer = BrutalBlack,
    secondary = BrutalBlack,
    onSecondary = BrutalWhite,
    secondaryContainer = BrutalMuted,
    onSecondaryContainer = BrutalBlack,
    tertiary = BrutalBlue,
    onTertiary = BrutalWhite,
    tertiaryContainer = Color(0xFFDDE5FF),
    onTertiaryContainer = BrutalBlack,
    background = BrutalWhite,
    onBackground = BrutalBlack,
    surface = BrutalWhite,
    onSurface = BrutalBlack,
    surfaceVariant = BrutalPaper,
    onSurfaceVariant = BrutalBlack,
    outline = BrutalBlack,
    outlineVariant = Color(0xFF222222),
    error = Color(0xFFCC2D2D),
    onError = BrutalWhite,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = BrutalBlack
)

private val MorningColorScheme = BrutalistLightColorScheme.copy(
    primary = Color(0xFF1D4ED8),
    primaryContainer = BrutalPaper,
    surface = BrutalPaper
)

@Composable
fun SummaTheme(
    appMode: String = "Normal",
    // Ignore system dark theme by default to ensure design consistency
    darkTheme: Boolean = false, 
    content: @Composable () -> Unit
) {
    // FORCE THEME BASED ON MODE
    // Normal / Pagi -> Light (Brutalist White)
    // Fokus -> Dark (Brutalist Black)
    // Malam -> Dark
    val colorScheme = when (appMode) {
        "Fokus", "Malam" -> BrutalistDarkColorScheme
        else -> BrutalistLightColorScheme // Force Light for Normal/Pagi
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()

                // Determine content color (status bar icons)
                val isDarkContent = when (appMode) {
                    "Fokus", "Malam" -> false // White text -> Dark background -> Light icons? No, White text means Dark Background means Light Icons (isAppearanceLightStatusBars = false)
                    else -> true   // Black text -> Light background -> Dark icons (isAppearanceLightStatusBars = true)
                }

                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isDarkContent
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isDarkContent
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

val Shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
)