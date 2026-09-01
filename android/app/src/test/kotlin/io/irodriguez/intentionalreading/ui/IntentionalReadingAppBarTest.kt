package io.irodriguez.intentionalreading.ui

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.irodriguez.intentionalreading.R
import io.irodriguez.intentionalreading.ui.theme.IntentionalReadingTypography
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTextApi::class)
class IntentionalReadingAppBarTest {
    @Test
    fun `the app bar uses the centred Material component`() {
        assertEquals("CenterAlignedTopAppBar", topAppBarName())
    }

    @Test
    fun `the masthead uses the small authored editorial style`() {
        assertEquals("headlineSmall", assignedTypographyStyle())
    }

    @Test
    fun `the masthead style is Playfair and not Roboto Flex`() {
        val playfairDisplay = variableFontFamily(
            resourceId = R.font.playfair_display_variable,
            FontWeight.SemiBold,
            FontWeight.Bold,
            FontWeight.ExtraBold,
        )
        val robotoFlex = variableFontFamily(
            resourceId = R.font.roboto_flex_variable,
            FontWeight.Normal,
            FontWeight.Medium,
            FontWeight.SemiBold,
            FontWeight.Bold,
        )

        assertEquals(playfairDisplay, IntentionalReadingTypography.headlineSmall.fontFamily)
        assertNotEquals(robotoFlex, IntentionalReadingTypography.headlineSmall.fontFamily)
    }

    @Test
    fun `one accessible settings control remains outside navigation`() {
        val source = appBarSource()

        assertEquals(1, Regex("""\bIconButton\s*\(""").findAll(source).count())
        assertEquals(1, Regex("""\bR\.drawable\.ic_settings\b""").findAll(source).count())
        assertEquals(1, Regex("""\bR\.string\.settings\b""").findAll(source).count())
        assertEquals(1, Regex("""\bR\.string\.app_name\b""").findAll(source).count())
        assertTrue(source.contains("contentDescription = stringResource(R.string.settings)"))
        assertTrue(source.contains("onClick = viewModel::toggleSettings"))
        assertFalse(source.contains("NavigationBarItem"))
        assertFalse(source.contains("Destination."))
    }

    @Test
    fun `the settings target names and meets the 48 dp floor`() {
        assertEquals(48f, namedDp("AppBarMinimumTarget"))
        assertTrue(appBarSource().contains(".size(AppBarMinimumTarget)"))
    }

    private fun topAppBarName(): String? = Regex("""\b(\w*TopAppBar)\s*\(""")
        .find(appBarSource())
        ?.groupValues
        ?.get(1)

    private fun assignedTypographyStyle(): String? = Regex(
        """\bstyle\s*=\s*MaterialTheme\.typography\.(\w+)""",
    ).find(appBarSource())?.groupValues?.get(1)

    private fun namedDp(name: String): Float? = Regex(
        """\binternal val $name\s*=\s*Dp\(([0-9.]+)f\)""",
    ).find(applicationSource())?.groupValues?.get(1)?.toFloat()

    private fun appBarSource(): String = applicationSource()
        .substringAfter("topBar = {")
        .substringBefore("bottomBar = {")

    private fun applicationSource(): String = Path.of(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/IntentionalReadingApp.kt",
    ).readText()

    private fun variableFontFamily(resourceId: Int, vararg weights: FontWeight): FontFamily = FontFamily(
        *weights.map { weight ->
            Font(
                resId = resourceId,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        }.toTypedArray(),
    )
}
