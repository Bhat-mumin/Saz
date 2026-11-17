package app.saz.music.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.saz.music.R

internal val Mineral = Color(0xFFF0EEE8)
internal val Walnut = Color(0xFF352B28)
internal val Plum = Color(0xFF703D57)
internal val SecondaryText = Color(0xFF625451)
internal val SurfaceMuted = Color(0xFFE5DADD)
internal val Outline = Color(0xFFC9BABD)
internal val Paper = Color(0xFFFAF8F3)

@OptIn(ExperimentalTextApi::class)
internal val Editorial = FontFamily(Font(R.font.literata, weight = FontWeight.Medium,
    variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.Setting("opsz", 32f))))
@OptIn(ExperimentalTextApi::class)
internal val Reading = FontFamily(
    Font(R.font.source_sans, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.source_sans, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))))

internal val WalnutPalette = lightColorScheme(
    primary = Plum, onPrimary = Color.White, primaryContainer = SurfaceMuted, onPrimaryContainer = Walnut,
    secondary = Plum, onSecondary = Color.White, secondaryContainer = SurfaceMuted, onSecondaryContainer = Walnut,
    background = Mineral, onBackground = Walnut, surface = Mineral, onSurface = Walnut,
    surfaceVariant = SurfaceMuted, onSurfaceVariant = SecondaryText, outline = SecondaryText,
    outlineVariant = Outline, error = Color(0xFFA42B32), onError = Color.White)

internal val WalnutTypography = Typography(
    displaySmall = TextStyle(fontFamily = Editorial, fontWeight = FontWeight.Medium, fontSize = 38.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Editorial, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 39.sp),
    headlineMedium = TextStyle(fontFamily = Editorial, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 33.sp),
    titleLarge = TextStyle(fontFamily = Editorial, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 29.sp),
    titleMedium = TextStyle(fontFamily = Reading, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Reading, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Reading, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Reading, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Reading, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Reading, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Reading, fontSize = 12.sp, lineHeight = 16.sp))

internal object WalnutLayout {
    val gutter = 24.dp
    val gap = 8.dp
    val section = 24.dp
    val artworkShape = RoundedCornerShape(8.dp)
    val panelShape = RoundedCornerShape(12.dp)
    const val transitionMs = 240
}

internal val WalnutShapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(20.dp))
