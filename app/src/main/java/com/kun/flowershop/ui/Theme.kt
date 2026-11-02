package com.kun.flowershop.ui

import com.kun.flowershop.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object KunColors {
    val Background = Color(0xFFF6EFE3)
    val Orange = Color(0xFFFFB100)
    val Field = Color(0xFFF0E5D3)
    val Tile = Color(0xFFEEE1CC)
    val Green = Color(0xFF294A3A)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val TextGray = Color(0xFF62605B)
    val TextMuted = Color(0xFF6F6C66)
    val TextSoft = Color(0xFF4A4846)
    val Placeholder = Color(0xFF777166)
    val CatalogPlaceholder = Color(0xFF666158)
    val Brown = Color(0xFF6B5B49)
    val Dot = Color(0xFFDACBAA)
    val Arrow = Color(0xFF5E5E5E)
    val CheckboxBorder = Color(0xFF7B7872)
    val Divider = Color(0xFF9A9791)
    val Error = Color(0xFFB3261E)
    val Line = Color(0xFFE2D6BF)
    val Scrim = Color(0x59000000)
    val Logout = Color(0xFF8B1A1A)
}

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

fun kunText(
    size: Float,
    weight: FontWeight = FontWeight.Normal,
    color: Color = KunColors.Black,
    lineHeight: Float? = null,
): TextStyle = TextStyle(
    fontFamily = InterFamily,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    lineHeight = if (lineHeight != null) lineHeight.sp else TextUnit.Unspecified,
)

private val KunColorScheme = lightColorScheme(
    primary = KunColors.Orange,
    onPrimary = KunColors.White,
    background = KunColors.Background,
    onBackground = KunColors.Black,
    surface = KunColors.Background,
    onSurface = KunColors.Black,
    error = KunColors.Error,
)

@Composable
fun KunTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KunColorScheme, content = content)
}
