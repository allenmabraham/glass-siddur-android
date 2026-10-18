package com.rowshr.siddur.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.rowshr.siddur.settings.ReaderTheme

/** App accent: a slate blue that reads well on both the light parchment and the dark indigo backdrops. */
val Brand = Color(0xFF4D78B8)
val BrandDeep = Color(0xFF3A5F96)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E4F7),
    onPrimaryContainer = Color(0xFF14315C),
    secondary = Color(0xFF5D6B85),
    surface = Color(0xFFF7F7FB),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF454A58),
    background = Color(0xFFF3F4FB),
    outlineVariant = Color(0xFFD5D8E2),
)

private val SepiaScheme: ColorScheme = LightScheme.copy(
    surface = Color(0xFFFBF4E6),
    background = Color(0xFFF8EFDD),
    surfaceVariant = Color(0xFFEFE4CF),
    onSurface = Color(0xFF2B2416),
    onSurfaceVariant = Color(0xFF5B4F3A),
    outlineVariant = Color(0xFFDCCFB4),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9DBAF0),
    onPrimary = Color(0xFF0E2A52),
    primaryContainer = Color(0xFF2D4A7A),
    onPrimaryContainer = Color(0xFFD9E4F7),
    secondary = Color(0xFFB9C3D9),
    surface = Color(0xFF14131F),
    onSurface = Color(0xFFE7E6F0),
    surfaceVariant = Color(0xFF26253A),
    onSurfaceVariant = Color(0xFFC3C2D4),
    background = Color(0xFF0F0E1A),
    outlineVariant = Color(0xFF3B3A52),
)

enum class Appearance { LIGHT, SEPIA, DARK }

val LocalAppearance = compositionLocalOf { Appearance.LIGHT }

@Composable
fun SiddurTheme(theme: ReaderTheme, content: @Composable () -> Unit) {
    val appearance = when (theme) {
        ReaderTheme.SYSTEM -> if (isSystemInDarkTheme()) Appearance.DARK else Appearance.LIGHT
        ReaderTheme.LIGHT -> Appearance.LIGHT
        ReaderTheme.SEPIA -> Appearance.SEPIA
        ReaderTheme.DARK -> Appearance.DARK
    }
    val scheme = when (appearance) {
        Appearance.LIGHT -> LightScheme
        Appearance.SEPIA -> SepiaScheme
        Appearance.DARK -> DarkScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = appearance != Appearance.DARK
                    isAppearanceLightNavigationBars = appearance != Appearance.DARK
                }
            }
        }
    }
    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(colorScheme = scheme, typography = SiddurTypography, content = content)
    }
}
