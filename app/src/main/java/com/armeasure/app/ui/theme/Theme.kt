package com.armeasure.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ───────────────────────────────────────────────────────────────────
object AppColors {
    val Background     = Color(0xFF060A12)      // near-black blue
    val Surface        = Color(0xFF0D1626)
    val SurfaceAlt     = Color(0xFF111E35)
    val Accent         = Color(0xFF00D4FF)       // electric cyan
    val AccentSecond   = Color(0xFFFF6B35)       // vivid orange
    val AccentThird    = Color(0xFFFFE53B)       // measurement yellow
    val TextPrimary    = Color(0xFFF0F6FF)
    val TextSecondary  = Color(0xFF6B8CAE)
    val Success        = Color(0xFF3DFF9A)
    val Error          = Color(0xFFFF3D6B)
    val Grid           = Color(0xFF1A2E4A)
}

// ── Typography (system fallback – replace with downloadable font) ──────────────
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize   = 72.sp,
        letterSpacing = (-2).sp,
        color = AppColors.TextPrimary
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 48.sp,
        letterSpacing = (-1).sp,
        color = AppColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        color = AppColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 20.sp,
        color = AppColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        color = AppColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        color = AppColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        letterSpacing = 1.sp,
        color = AppColors.TextSecondary
    )
)

// ── Color scheme ──────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = AppColors.Accent,
    onPrimary          = AppColors.Background,
    secondary          = AppColors.AccentSecond,
    onSecondary        = AppColors.Background,
    tertiary           = AppColors.AccentThird,
    background         = AppColors.Background,
    surface            = AppColors.Surface,
    surfaceVariant     = AppColors.SurfaceAlt,
    onBackground       = AppColors.TextPrimary,
    onSurface          = AppColors.TextPrimary,
    onSurfaceVariant   = AppColors.TextSecondary,
    error              = AppColors.Error
)

@Composable
fun ARMeasureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
