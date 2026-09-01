package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.irodriguez.intentionalreading.R

private val playfairDisplay = variableFontFamily(
    resourceId = R.font.playfair_display_variable,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

private val robotoFlex = variableFontFamily(
    resourceId = R.font.roboto_flex_variable,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

private val displayLarge = TextStyle(
    fontFamily = playfairDisplay,
    fontSize = 40.sp,
    lineHeight = 48.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.03).em,
)

private val headlineLarge = TextStyle(
    fontFamily = playfairDisplay,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.02).em,
)

private val headlineMedium = TextStyle(
    fontFamily = playfairDisplay,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.015).em,
)

private val headlineSmall = TextStyle(
    fontFamily = playfairDisplay,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.01).em,
)

private val statNumber = TextStyle(
    fontFamily = playfairDisplay,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.02).em,
)

private val bodyLarge = TextStyle(
    fontFamily = robotoFlex,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.01.em,
)

private val bodyMedium = TextStyle(
    fontFamily = robotoFlex,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.015.em,
)

private val labelLarge = TextStyle(
    fontFamily = robotoFlex,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.02.em,
)

private val labelMedium = TextStyle(
    fontFamily = robotoFlex,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.06.em,
)

val IntentionalReadingTypography = Typography(
    displayLarge = displayLarge,
    displayMedium = statNumber,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleMedium = headlineSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodyMedium,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelMedium,
)

@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resourceId: Int, vararg weights: FontWeight): FontFamily = FontFamily(
    *weights.map { weight ->
        Font(
            resId = resourceId,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    }.toTypedArray(),
)
