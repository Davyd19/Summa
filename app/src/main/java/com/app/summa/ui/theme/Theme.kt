package com.app.summa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val DarkColorScheme = darkColorScheme(
    primary = DeepTealLight, // Teal cerah untuk Dark Mode
    onPrimary = DeepCharcoal,
    primaryContainer = DeepTeal.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = GoldAccent, // Emas tetap cerah
    onSecondary = DeepCharcoal,
    secondaryContainer = GoldAccent.copy(alpha = 0.2f),
    onSecondaryContainer = GoldAccent,
    background = DarkBackground,
    onBackground = OffWhite.copy(alpha = 0.9f),
    surface = DarkSurface, // Card di dark mode
    onSurface = OffWhite.copy(alpha = 0.9f),
    surfaceVariant = Color(0xFF424242), // Latar belakang netral (cth: heatmap)
    onSurfaceVariant = WarmGray,
    outline = Color(0xFF5E5E5E), // Border
    outlineVariant = Color(0xFF333333) // Divider
)

private val LightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = DeepTealContainer, // Latar belakang Hero (Teal sangat muda)
    onPrimaryContainer = DeepTeal,
    secondary = GoldAccent,
    onSecondary = DeepCharcoal,
    secondaryContainer = GoldContainer, // Latar belakang Aksi (Emas sangat muda)
    onSecondaryContainer = Color(0xFFB38600),
    background = OffWhite, // Latar belakang utama (bukan putih murni)
    onBackground = DeepCharcoal,
    surface = Color.White, // Latar belakang Card
    onSurface = DeepCharcoal,
    surfaceVariant = LightGrayBorder, // Latar belakang netral (cth: heatmap)
    onSurfaceVariant = WarmGray, // Teks abu-abu
    outline = DeepTeal.copy(alpha = 0.5f), // Border aktif (cth: sel kalender)
    outlineVariant = LightGrayBorder // Border card / Divider
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
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                // PERBAIKAN: Mengatur warna navigation bar
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}