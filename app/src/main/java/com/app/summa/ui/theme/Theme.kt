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
    primary = Color(0xFF4FD1C5), // Soft Teal
    onPrimary = Color(0xFF003B36),
    primaryContainer = Color(0xFF1A544F),
    onPrimaryContainer = Color(0xFFB2F5EA),
    secondary = Color(0xFFFFB951), // Warm Gold
    onSecondary = Color(0xFF4A3000),
    secondaryContainer = Color(0xFF6B4800),
    onSecondaryContainer = Color(0xFFFFDEA3),
    tertiary = Color(0xFFB794F6),
    onTertiary = Color(0xFF3B1F5C),
    tertiaryContainer = Color(0xFF523677),
    onTertiaryContainer = Color(0xFFE9D8FD),
    background = Color(0xFF0F1419), // Very dark blue-gray
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF1A1F26),
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF2D3748),
    onSurfaceVariant = Color(0xFFA0AEC0),
    outline = Color(0xFF4A5568),
    outlineVariant = Color(0xFF2D3748),
    error = Color(0xFFFC8181),
    onError = Color(0xFF4A0000),
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFFE4E4)
)

// Modern Light Color Scheme - Clean & Breathable
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF003B36),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF3B1F5C),
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

// Pagi Scheme (Warm/Energetic) - Variant of Light
private val MorningColorScheme = LightColorScheme.copy(
    primary = Color(0xFFEA580C), // Orange-ish
    primaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFFFFF7ED), // Very warm white
    surface = Color(0xFFFFFBF5)
)

@Composable
fun SummaTheme(
    // PARAMETER BARU: Menerima Mode Aplikasi
    appMode: String = "Normal",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // LOGIKA PEMILIHAN TEMA DINAMIS
    val colorScheme = when (appMode) {
        "Fokus" -> DarkColorScheme // Fokus selalu Gelap (Deep Work)
        "Pagi" -> MorningColorScheme // Pagi selalu Hangat/Terang
        else -> if (darkTheme) DarkColorScheme else LightColorScheme // Normal ikut sistem
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb() // Match bottom nav

                // Atur icon status bar (gelap/terang)
                val isDarkContent = when (appMode) {
                    "Fokus" -> false // Text putih
                    "Pagi" -> true   // Text hitam
                    else -> !darkTheme
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)