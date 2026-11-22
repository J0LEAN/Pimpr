package com.pimpr.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pimpr.app.R

/** Encre : le bleu nuit des textes, repris des icônes marine des écrans pastel. */
val Ink = Color(0xFF1E1B4B)

/** Dégradé « crépuscule » : réservé à ce qui est actif ou à ce qu'on applique. */
val DuskColors = listOf(Color(0xFF8E9BFF), Color(0xFFB99BFF), Color(0xFFFFA3CF), Color(0xFFFFC8A3))
val Dusk: Brush get() = Brush.linearGradient(DuskColors)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF5B54E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E1FF),
    onPrimaryContainer = Ink,
    secondary = Color(0xFFE0679C),
    onSecondary = Color.White,
    background = Color(0xFFF4F2FB),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEBE8F8),
    onSurfaceVariant = Color(0xFF5F5B87),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAF9FE),
    surfaceContainer = Color(0xFFF4F2FB),
    surfaceContainerHigh = Color(0xFFEBE8F8),
    surfaceContainerHighest = Color(0xFFE3DFF4),
    outline = Color(0xFFB9B4DA),
    outlineVariant = Color(0xFFE1DDF2),
    error = Color(0xFFC93A5B),
    scrim = Color(0xFF0F0E22),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB0ABFF),
    onPrimary = Color(0xFF1A1750),
    primaryContainer = Color(0xFF34307A),
    onPrimaryContainer = Color(0xFFE4E1FF),
    secondary = Color(0xFFFF9CCB),
    onSecondary = Color(0xFF4A1230),
    background = Color(0xFF0F0E22),
    onBackground = Color(0xFFEDEBFF),
    surface = Color(0xFF1B1A36),
    onSurface = Color(0xFFEDEBFF),
    surfaceVariant = Color(0xFF272549),
    onSurfaceVariant = Color(0xFFB7B3DC),
    surfaceContainerLowest = Color(0xFF0B0A1A),
    surfaceContainerLow = Color(0xFF15142C),
    surfaceContainer = Color(0xFF1B1A36),
    surfaceContainerHigh = Color(0xFF272549),
    surfaceContainerHighest = Color(0xFF312F58),
    outline = Color(0xFF55527F),
    outlineVariant = Color(0xFF302E55),
    error = Color(0xFFFF8FA6),
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
