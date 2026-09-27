package com.opticast.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material 3 "expressive" type ramp: heavy, tight display faces for titles,
// comfortable reading sizes everywhere else.
val OptiCastTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp,
    ),
)

// Keep body copy readable; save space primarily in headlines and line spacing.
val CompactOptiCastTypography = OptiCastTypography.copy(
    displayLarge = OptiCastTypography.displayLarge.copy(fontSize = 36.sp, lineHeight = 40.sp),
    displayMedium = OptiCastTypography.displayMedium.copy(fontSize = 30.sp, lineHeight = 34.sp),
    displaySmall = OptiCastTypography.displaySmall.copy(fontSize = 26.sp, lineHeight = 30.sp),
    headlineLarge = OptiCastTypography.headlineLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
    headlineMedium = OptiCastTypography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 26.sp),
    headlineSmall = OptiCastTypography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 24.sp),
    titleLarge = OptiCastTypography.titleLarge.copy(fontSize = 18.sp, lineHeight = 22.sp),
    bodyLarge = OptiCastTypography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = OptiCastTypography.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
)
