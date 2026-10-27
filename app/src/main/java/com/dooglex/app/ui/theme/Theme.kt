package com.dooglex.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dooglex.app.model.AccentPalette
import com.dooglex.app.model.ThemeMode

val Lime = Color(0xFFC9F57A)
val Ink = Color(0xFF18231B)
val Paper = Color(0xFFF5F6F0)
val MutedInk = Color(0xFF667066)
val DarkPaper = Color(0xFF111711)
val DarkSurface = Color(0xFF1A231C)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6F9F22),
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

data class AccentColorValues(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
)

fun accentColors(palette: AccentPalette, darkTheme: Boolean): AccentColorValues =
    when (palette) {
        AccentPalette.LIME -> if (darkTheme) AccentColorValues(
            Color(0xFFC9F57A), Ink, Color(0xFF334528), Color(0xFFC9F57A),
        ) else AccentColorValues(Color(0xFF6F9F22), Ink, Lime, Ink)

        AccentPalette.SKY -> if (darkTheme) AccentColorValues(
            Color(0xFFA9D9F2), Color(0xFF12384E), Color(0xFF234554), Color(0xFFC7E7F6),
        ) else AccentColorValues(
            Color(0xFF397CA8), Color.White, Color(0xFFC7E7F6), Color(0xFF12384E),
        )

        AccentPalette.CORAL -> if (darkTheme) AccentColorValues(
            Color(0xFFFFB59D), Color(0xFF4C1E11), Color(0xFF553025), Color(0xFFFFD2C2),
        ) else AccentColorValues(
            Color(0xFFC85F3D), Color.White, Color(0xFFFFD2C2), Color(0xFF4C1E11),
        )

        AccentPalette.VIOLET -> if (darkTheme) AccentColorValues(
            Color(0xFFD2B5F2), Color(0xFF2B1945), Color(0xFF3D2C52), Color(0xFFE5D6F7),
        ) else AccentColorValues(
            Color(0xFF7954B3), Color.White, Color(0xFFE5D6F7), Color(0xFF2B1945),
        )

        AccentPalette.MINT -> if (darkTheme) AccentColorValues(
            Color(0xFF9DDCC8), Color(0xFF103B32), Color(0xFF234A40), Color(0xFFBEE9DC),
        ) else AccentColorValues(
            Color(0xFF287F6C), Color.White, Color(0xFFBEE9DC), Color(0xFF103B32),
        )

        AccentPalette.GRAPHITE -> if (darkTheme) AccentColorValues(
            Color(0xFFD6DDD2), Color(0xFF1D241D), Color(0xFF363D36), Color(0xFFD6DDD2),
        ) else AccentColorValues(
            Color(0xFF4B514B), Color.White, Color(0xFFDDE1DA), Color(0xFF1D241D),
        )
    }

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
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
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
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
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

private fun TextStyle.scaled(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
)

@Composable
fun DoogleXTheme(
    themeMode: ThemeMode,
    accentPalette: AccentPalette = AccentPalette.LIME,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = accentColors(accentPalette, darkTheme)
    val colors = (if (darkTheme) DarkColors else LightColors).copy(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.container,
        onPrimaryContainer = accent.onContainer,
    )
    val typography = remember(textScale) {
        val scale = textScale.coerceIn(0.85f, 1.4f)
        DoogleXTypography.copy(
            displaySmall = DoogleXTypography.displaySmall.scaled(scale),
            headlineMedium = DoogleXTypography.headlineMedium.scaled(scale),
            titleLarge = DoogleXTypography.titleLarge.scaled(scale),
            titleMedium = DoogleXTypography.titleMedium.scaled(scale),
            titleSmall = DoogleXTypography.titleSmall.scaled(scale),
            bodyLarge = DoogleXTypography.bodyLarge.scaled(scale),
            bodyMedium = DoogleXTypography.bodyMedium.scaled(scale),
            bodySmall = DoogleXTypography.bodySmall.scaled(scale),
            labelLarge = DoogleXTypography.labelLarge.scaled(scale),
            labelSmall = DoogleXTypography.labelSmall.scaled(scale),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content,
    )
}
