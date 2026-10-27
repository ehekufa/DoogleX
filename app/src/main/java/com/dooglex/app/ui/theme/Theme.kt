package com.dooglex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dooglex.app.model.ThemeMode

val Lime = Color(0xFFC9F57A)
val LimeStrong = Color(0xFF91C83E)
val Ink = Color(0xFF18231B)
val Paper = Color(0xFFF5F6F0)
val MutedInk = Color(0xFF667066)
val DarkPaper = Color(0xFF111711)
val DarkSurface = Color(0xFF1A231C)

private val LightColors = lightColorScheme(
    primary = LimeStrong,
    onPrimary = Ink,
    primaryContainer = Lime,
    onPrimaryContainer = Ink,
    secondary = Color(0xFF586C55),
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9ECE3),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD9DED3),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = Color(0xFF334528),
    onPrimaryContainer = Lime,
    secondary = Color(0xFFB5C8AE),
    onSecondary = Ink,
    background = DarkPaper,
    onBackground = Color(0xFFEDF2E9),
    surface = DarkSurface,
    onSurface = Color(0xFFEDF2E9),
    surfaceVariant = Color(0xFF29342B),
    onSurfaceVariant = Color(0xFFB2BCB0),
    outline = Color(0xFF3B473D),
    error = Color(0xFFFFB4AB),
)

private val DoogleXTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 45.sp,
        letterSpacing = (-1.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
)

@Composable
fun DoogleXTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DoogleXTypography,
        content = content,
    )
}
