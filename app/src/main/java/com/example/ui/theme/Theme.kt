package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = LuxuryGold,
    onPrimary = Color.Black,
    primaryContainer = LuxuryGoldContainer,
    onPrimaryContainer = LuxuryGoldBright,
    secondary = PostOrange,
    onSecondary = Color.Black,
    secondaryContainer = PostOrangeContainer,
    onSecondaryContainer = Color(0xFFFFB74D),
    tertiary = InfoBlue,
    background = ObsidianDarkBg,
    onBackground = Color(0xFFF3F4F6),
    surface = ObsidianSurface,
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = ObsidianBorder,
    error = DangerRed,
    onError = Color.White
)

private val ClassicCorporateColorScheme = lightColorScheme(
    primary = ClassicNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = ClassicNavyDark,
    secondary = ClassicOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF7A2700),
    tertiary = Color(0xFF006A6A),
    background = ClassicLightBg,
    onBackground = Color(0xFF111827),
    surface = ClassicLightSurface,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = ClassicLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    appThemeMode: AppThemeMode = AppThemeMode.LUXURY,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (appThemeMode) {
        AppThemeMode.LUXURY -> LuxuryDarkColorScheme
        AppThemeMode.CLASSIC -> ClassicCorporateColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
