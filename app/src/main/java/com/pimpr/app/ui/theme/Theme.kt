package com.pimpr.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pimpr.app.R

/** Encre : l'anthracite des textes et des actions. L'interface reste neutre pour laisser la couleur aux thèmes. */
val Ink = Color(0xFF1B1B1D)

/** Gris de travail proposés derrière un widget en cours de création. */
val StageGreys = listOf(Color(0xFF8E8E90), Color(0xFFE9E7E2), Color(0xFF2A2A2C))

private val LightColors: ColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E4DF),
    onPrimaryContainer = Ink,
    secondary = Color(0xFFB5523B),
    onSecondary = Color.White,
    background = Color(0xFFF3F2EF),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E7E2),
    onSurfaceVariant = Color(0xFF66645F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9F7),
    surfaceContainer = Color(0xFFF3F2EF),
    surfaceContainerHigh = Color(0xFFE9E7E2),
    surfaceContainerHighest = Color(0xFFDFDCD6),
    outline = Color(0xFFBDBAB3),
    outlineVariant = Color(0xFFE0DDD7),
    error = Color(0xFFB3261E),
    scrim = Color(0xFF101011),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFF1F0EC),
    onPrimary = Ink,
    primaryContainer = Color(0xFF3A3A3C),
    onPrimaryContainer = Color(0xFFF1F0EC),
    secondary = Color(0xFFE08A72),
    onSecondary = Color(0xFF3A120A),
    background = Color(0xFF111112),
    onBackground = Color(0xFFF1F0EC),
    surface = Color(0xFF1D1D1F),
    onSurface = Color(0xFFF1F0EC),
    surfaceVariant = Color(0xFF2A2A2C),
    onSurfaceVariant = Color(0xFFB4B2AD),
    surfaceContainerLowest = Color(0xFF0B0B0C),
    surfaceContainerLow = Color(0xFF171718),
    surfaceContainer = Color(0xFF1D1D1F),
    surfaceContainerHigh = Color(0xFF2A2A2C),
    surfaceContainerHighest = Color(0xFF343436),
    outline = Color(0xFF5C5B58),
    outlineVariant = Color(0xFF323234),
    error = Color(0xFFF2B8B5),
    scrim = Color.Black,
)

val Unbounded = FontFamily(
    Font(R.font.unbounded_medium, FontWeight.Medium),
    Font(R.font.unbounded_bold, FontWeight.Bold),
)

val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Unbounded, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = (-0.3).sp)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Figtree, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val PimprTypography = Typography(
    displaySmall = display(30, 36),
    headlineLarge = display(26, 32),
    headlineMedium = display(22, 28),
    headlineSmall = display(18, 24, FontWeight.Medium),
    titleLarge = display(16, 22, FontWeight.Medium),
    titleMedium = text(17, 22, FontWeight.Bold),
    titleSmall = text(15, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 24),
    bodyMedium = text(15, 21),
    bodySmall = text(13, 18),
    labelLarge = text(15, 20, FontWeight.SemiBold),
    labelMedium = text(13, 16, FontWeight.SemiBold),
    labelSmall = text(12, 16, FontWeight.Medium),
)

@Composable
fun PimprTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = PimprTypography, content = content)
}
