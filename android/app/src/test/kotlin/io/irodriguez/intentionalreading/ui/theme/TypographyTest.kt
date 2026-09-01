package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.irodriguez.intentionalreading.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTextApi::class)
class TypographyTest {
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

    @Test
    fun `the nine authored styles carry their specified metrics`() {
        val styles = listOf(
            ExpectedStyle("display-lg", IntentionalReadingTypography.displayLarge, playfairDisplay, 40.sp, 48.sp, FontWeight.ExtraBold, (-0.03).em),
            ExpectedStyle("headline-lg", IntentionalReadingTypography.headlineLarge, playfairDisplay, 30.sp, 36.sp, FontWeight.Bold, (-0.02).em),
            ExpectedStyle("headline-md", IntentionalReadingTypography.headlineMedium, playfairDisplay, 24.sp, 30.sp, FontWeight.Bold, (-0.015).em),
            ExpectedStyle("headline-sm", IntentionalReadingTypography.headlineSmall, playfairDisplay, 20.sp, 26.sp, FontWeight.SemiBold, (-0.01).em),
            ExpectedStyle("stat-num", IntentionalReadingTypography.displayMedium, playfairDisplay, 28.sp, 32.sp, FontWeight.ExtraBold, (-0.02).em),
            ExpectedStyle("body-lg", IntentionalReadingTypography.bodyLarge, robotoFlex, 16.sp, 24.sp, FontWeight.Normal, 0.01.em),
            ExpectedStyle("body-md", IntentionalReadingTypography.bodyMedium, robotoFlex, 14.sp, 20.sp, FontWeight.Normal, 0.015.em),
            ExpectedStyle("label-lg", IntentionalReadingTypography.labelLarge, robotoFlex, 14.sp, 20.sp, FontWeight.SemiBold, 0.02.em),
            ExpectedStyle("label-md", IntentionalReadingTypography.labelMedium, robotoFlex, 12.sp, 16.sp, FontWeight.Bold, 0.06.em),
        )

        styles.forEach { expected ->
            assertEquals(expected.family, expected.actual.fontFamily, "${expected.name} family")
            assertEquals(expected.size, expected.actual.fontSize, "${expected.name} size")
            assertEquals(expected.lineHeight, expected.actual.lineHeight, "${expected.name} line height")
            assertEquals(expected.weight, expected.actual.fontWeight, "${expected.name} weight")
            assertEquals(expected.tracking, expected.actual.letterSpacing, "${expected.name} tracking")
        }
    }

    @Test
    fun `every configured slot is authored and every consumed slot differs from Material defaults`() {
        val defaults = Typography()
        val configuredSlots = listOf(
            "displayLarge" to (IntentionalReadingTypography.displayLarge to defaults.displayLarge),
            "displayMedium" to (IntentionalReadingTypography.displayMedium to defaults.displayMedium),
            "headlineLarge" to (IntentionalReadingTypography.headlineLarge to defaults.headlineLarge),
            "headlineMedium" to (IntentionalReadingTypography.headlineMedium to defaults.headlineMedium),
            "headlineSmall" to (IntentionalReadingTypography.headlineSmall to defaults.headlineSmall),
            "titleMedium" to (IntentionalReadingTypography.titleMedium to defaults.titleMedium),
            "bodyLarge" to (IntentionalReadingTypography.bodyLarge to defaults.bodyLarge),
            "bodyMedium" to (IntentionalReadingTypography.bodyMedium to defaults.bodyMedium),
            "bodySmall" to (IntentionalReadingTypography.bodySmall to defaults.bodySmall),
            "labelLarge" to (IntentionalReadingTypography.labelLarge to defaults.labelLarge),
            "labelMedium" to (IntentionalReadingTypography.labelMedium to defaults.labelMedium),
            "labelSmall" to (IntentionalReadingTypography.labelSmall to defaults.labelSmall),
        )

        configuredSlots.forEach { (name, styles) ->
            assertNotEquals(styles.second, styles.first, "$name still equals the Material 3 default")
        }

        assertEquals(IntentionalReadingTypography.bodyMedium, IntentionalReadingTypography.bodySmall)
        assertEquals(IntentionalReadingTypography.labelMedium, IntentionalReadingTypography.labelSmall)
        assertEquals(IntentionalReadingTypography.headlineSmall, IntentionalReadingTypography.titleMedium)
    }

    @Test
    fun `both bundled families expose every requested weight on the variable weight axis`() {
        assertEquals(playfairDisplay, IntentionalReadingTypography.displayLarge.fontFamily)
        assertEquals(robotoFlex, IntentionalReadingTypography.bodyLarge.fontFamily)
    }

    private fun variableFontFamily(resourceId: Int, vararg weights: FontWeight): FontFamily = FontFamily(
        *weights.map { weight ->
            Font(
                resId = resourceId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        }.toTypedArray(),
    )

    private data class ExpectedStyle(
        val name: String,
        val actual: TextStyle,
        val family: FontFamily,
        val size: TextUnit,
        val lineHeight: TextUnit,
        val weight: FontWeight,
        val tracking: TextUnit,
    )
}
