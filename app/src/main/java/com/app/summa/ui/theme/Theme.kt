package com.app.summa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Modern Dark Color Scheme - Inspired by Atoms
private val DarkColorScheme = darkColorScheme(
    // Primary - Soft Teal/Cyan untuk modern feel
    primary = Color(0xFF4FD1C5), // Soft Teal
    onPrimary = Color(0xFF003B36),
    primaryContainer = Color(0xFF1A544F),
    onPrimaryContainer = Color(0xFFB2F5EA),

    // Secondary - Warm accent untuk contrast
    secondary = Color(0xFFFFB951), // Warm Gold
    onSecondary = Color(0xFF4A3000),
    secondaryContainer = Color(0xFF6B4800),
    onSecondaryContainer = Color(0xFFFFDEA3),

    // Tertiary - Purple untuk variety
    tertiary = Color(0xFFB794F6), // Soft Purple
    onTertiary = Color(0xFF3B1F5C),
    tertiaryContainer = Color(0xFF523677),
    onTertiaryContainer = Color(0xFFE9D8FD),

    // Background & Surface - Deep but not pure black
    background = Color(0xFF0F1419), // Very dark blue-gray
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF1A1F26), // Slightly lighter than background
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF2D3748), // For cards that need elevation
    onSurfaceVariant = Color(0xFFA0AEC0),

    // Outline & Borders
    outline = Color(0xFF4A5568),
    outlineVariant = Color(0xFF2D3748),

    // Error
    error = Color(0xFFFC8181),
    onError = Color(0xFF4A0000),
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFFE4E4)
)

// Modern Light Color Scheme - Clean & Breathable
private val LightColorScheme = lightColorScheme(
    // Primary - Calm Teal
    primary = Color(0xFF0D9488), // Teal 600
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1), // Teal 100
    onPrimaryContainer = Color(0xFF003B36),

    // Secondary - Warm Amber
    secondary = Color(0xFFF59E0B), // Amber 500
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7), // Amber 100
    onSecondaryContainer = Color(0xFF78350F),

    // Tertiary - Elegant Purple
    tertiary = Color(0xFF8B5CF6), // Violet 500
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE9FE), // Violet 100
    onTertiaryContainer = Color(0xFF3B1F5C),

    // Background & Surface - Off-white untuk mengurangi eye strain
    background = Color(0xFFF9FAFB), // Gray 50
    onBackground = Color(0xFF1F2937), // Gray 800
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6), // Gray 100
    onSurfaceVariant = Color(0xFF6B7280), // Gray 500

    // Outline & Borders - Subtle
    outline = Color(0xFFD1D5DB), // Gray 300
    outlineVariant = Color(0xFFE5E7EB), // Gray 200

    // Error
    error = Color(0xFFEF4444), // Red 500
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2), // Red 100
    onErrorContainer = Color(0xFF7F1D1D) // Red 900
)

@Composable
fun SummaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                // Status bar color matches background
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                // Navigation bar color matches background
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
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

// Modern Shape System
val Shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)