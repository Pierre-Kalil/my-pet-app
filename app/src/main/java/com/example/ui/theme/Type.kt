package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

/** Material 3 roles mapped to the Inter type scale in DESIGN.md. */
private fun interStyle(
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight
) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight
)

val Typography = Typography(
    displayLarge = interStyle(57.sp, 64.sp, FontWeight.Normal),
    headlineLarge = interStyle(32.sp, 40.sp, FontWeight.SemiBold),
    headlineMedium = interStyle(28.sp, 36.sp, FontWeight.SemiBold),
    headlineSmall = interStyle(24.sp, 32.sp, FontWeight.Medium),
    titleLarge = interStyle(22.sp, 28.sp, FontWeight.SemiBold),
    titleMedium = interStyle(16.sp, 24.sp, FontWeight.SemiBold),
    titleSmall = interStyle(14.sp, 20.sp, FontWeight.SemiBold),
    bodyLarge = interStyle(16.sp, 24.sp, FontWeight.Normal),
    bodyMedium = interStyle(14.sp, 20.sp, FontWeight.Normal),
    bodySmall = interStyle(12.sp, 16.sp, FontWeight.Normal),
    labelLarge = interStyle(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = interStyle(12.sp, 16.sp, FontWeight.Medium),
    labelSmall = interStyle(11.sp, 16.sp, FontWeight.Medium)
)
