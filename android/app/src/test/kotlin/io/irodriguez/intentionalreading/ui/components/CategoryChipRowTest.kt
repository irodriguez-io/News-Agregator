package io.irodriguez.intentionalreading.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.irodriguez.intentionalreading.ui.format.Labels
import io.irodriguez.intentionalreading.ui.theme.darkTokens
import io.irodriguez.intentionalreading.ui.theme.lightTokens
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryChipRowTest {
    @Test
    fun `selected chips use the primary role pair`() {
        assertEquals(
            Triple("primary", "onPrimary", "primary"),
            Triple(
                assignedToken("selectedContainerColor"),
                assignedToken("selectedLabelColor"),
                assignedToken("selectedBorderColor"),
            ),
            "selected chip fill, label, and border",
        )
    }

    @Test
    fun `unselected chips use surface muted ink and the control outline`() {
        assertEquals(
            Triple("surface", "muted", "outlineControl"),
            Triple(
                assignedToken("containerColor"),
                assignedToken("labelColor"),
                assignedToken("borderColor"),
            ),
            "unselected chip fill, label, and border",
        )
    }

    @Test
    fun `the control outline clears three to one against the chip surface in both schemes`() {
        listOf("light" to lightTokens(), "dark" to darkTokens()).forEach { (scheme, tokens) ->
            val ratio = contrastRatio(tokens.outlineControl, tokens.surface)

            assertTrue(ratio >= 3.0, "$scheme chip outline contrast ratio was $ratio")
        }
    }

    @Test
    fun `chips name the 40 dp pill and 48 dp target dimensions`() {
        assertEquals(40f, namedDp("CategoryChipVisibleHeight"), "visible pill height")
        assertEquals(48f, namedDp("CategoryChipMinimumTarget"), "minimum touch target")
        assertTrue(source().contains(".heightIn(min = CategoryChipVisibleHeight)"))
        assertTrue(
            source().contains(
                "LocalMinimumInteractiveComponentSize provides CategoryChipMinimumTarget",
            ),
        )
    }

    @Test
    fun `chips use the authored pill shape`() {
        assertEquals("chip", assignedShape())
    }

    @Test
    fun `selection retains a non-colour state description`() {
        val source = source()

        assertTrue(source.contains(".semantics { stateDescription ="))
        assertTrue(
            source.contains(
                """if (selected) "${'$'}selectedState: ${'$'}{option.label}" else option.label""",
            ),
        )
    }

    @Test
    fun `the fixed category options retain their order and labels`() {
        assertEquals(
            listOf(
                "all" to "All",
                "science" to "Science",
                "technology" to "Technology",
                "literature" to "Literature",
                "history" to "History",
                "weightlifting" to "Weightlifting",
                "iam" to "IAM",
                "identity_automation" to "Identity Automation",
            ),
            Labels.categoryOptions.map { it.id to it.label },
        )
        assertTrue(source().contains("items(Labels.categoryOptions"))
    }

    @Test
    fun `the chip source names no colour radius or font literal`() {
        val forbidden = listOf(
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\s*\("""),
        )

        forbidden.forEach { pattern ->
            assertFalse(pattern.containsMatchIn(source()), "Forbidden literal matched ${pattern.pattern}")
        }
    }

    private fun assignedToken(property: String): String? = Regex(
        """\b$property\s*=\s*tokens\.(\w+)""",
    ).find(source())?.groupValues?.get(1)

    private fun assignedShape(): String? = Regex(
        """\bshape\s*=\s*shapes\.(\w+)""",
    ).find(source())?.groupValues?.get(1)

    private fun namedDp(name: String): Float? = Regex(
        """\binternal val $name\s*=\s*Dp\(([0-9.]+)f\)""",
    ).find(source())?.groupValues?.get(1)?.toFloat()

    private fun source(): String = Path.of(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/CategoryChipRow.kt",
    ).readText()

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        val red = linearChannel((argb ushr 16 and 0xFF) / 255.0)
        val green = linearChannel((argb ushr 8 and 0xFF) / 255.0)
        val blue = linearChannel((argb and 0xFF) / 255.0)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun linearChannel(value: Double): Double = if (value <= 0.04045) {
        value / 12.92
    } else {
        ((value + 0.055) / 1.055).pow(2.4)
    }
}
