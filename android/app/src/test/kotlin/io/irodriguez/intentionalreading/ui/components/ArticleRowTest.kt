package io.irodriguez.intentionalreading.ui.components

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleRowTest {
    @Test
    fun `the Queue Row uses the authored tonal container without elevation`() {
        assertEquals("queueRow", queueSurfaceRole("shape"), "Queue Row shape role")
        assertEquals("container", queueSurfaceRole("color"), "Queue Row fill role")
        assertFalse(source().contains(".shadow("), "Queue Row must not draw a shadow")
        assertFalse(source().contains("shadowElevation"), "Queue Row must not name shadow elevation")
        assertFalse(source().contains("tonalElevation"), "Queue Row must not name tonal elevation")
    }

    @Test
    fun `the title element clamps at two lines and ellipsises`() {
        assertEquals(2, titleMaxLines(), "title max lines")
        assertEquals("Ellipsis", titleOverflow(), "title overflow")
    }

    @Test
    fun `row actions name a 48 dp target in both dimensions`() {
        assertEquals(48f, actionTargetDp(), "row action target dp")
        assertTrue(source().contains("minWidth = ArticleRowMinimumTarget"))
        assertTrue(source().contains("minHeight = ArticleRowMinimumTarget"))
    }

    @Test
    fun `the leading action is tonal and the remaining actions are outlined pills`() {
        val source = source()

        assertTrue(source.contains("TonalSecondaryControl("))
        assertTrue(source.contains("OutlinedButton("))
        assertTrue(source.contains("shape = shapes.pill"))
        assertTrue(source.contains("contentColor = tokens.quiet"))
        assertTrue(source.contains("tokens.outlineControl"))
    }

    @Test
    fun `the Queue Row has no media region and reserves no space for one`() {
        val forbidden = Regex(
            """\b(?:AsyncImage|Image|Spacer|mediaSlot|thumbnail)\b""",
            RegexOption.IGNORE_CASE,
        )

        assertFalse(forbidden.containsMatchIn(source()), "ArticleRow must not contain a media region")
    }

    @Test
    fun `the row source names no colour radius dimension or font literal`() {
        val forbidden = listOf(
            Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b"""),
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\b"""),
        )

        forbidden.forEach { pattern ->
            assertFalse(pattern.containsMatchIn(source()), "Forbidden literal matched ${pattern.pattern}")
        }
    }

    @Test
    fun `the composable contract and every displayed input stay unchanged`() {
        assertEquals(
            "fun ArticleRow( articleTitle: String, position: String, positionDetail: String?, " +
                "kicker: List<ArticleKickerPart>, tags: List<String>, actions: List<ArticleRowAction>, " +
                "modifier: Modifier = Modifier, ) {",
            articleRowSignature(),
        )

        listOf(
            "text = position",
            "text = positionDetail",
            "text = part.text.uppercase(Locale.ROOT)",
            "text = articleTitle",
            "text = tag",
            "Text(action.label",
            "tags.take(3).forEach",
        ).forEach { binding ->
            assertTrue(source().contains(binding), "Missing unchanged display binding: $binding")
        }
    }

    @Test
    fun `unknown reading time remains omitted by both row callers`() {
        val omission =
            "RelativeTime.readingTime(article.readingTimeMinutes).takeIf { it.isNotEmpty() }"

        assertTrue(readLaterSource().contains(omission))
        assertTrue(historySource().contains(omission))
        assertFalse(source().contains("0 min"))
    }

    private fun queueSurfaceRole(property: String): String? = Regex(
        """Surface\(\s*modifier\s*=\s*modifier\.fillMaxWidth\(\),[\s\S]*?\b$property\s*=\s*(?:shapes|tokens)\.(\w+)""",
    ).find(source())?.groupValues?.get(1)

    private fun titleMaxLines(): Int = Regex(
        """\bmaxLines\s*=\s*(\d+)""",
    ).find(titleBlock())?.groupValues?.get(1)?.toInt() ?: Int.MAX_VALUE

    private fun titleOverflow(): String? = Regex(
        """\boverflow\s*=\s*TextOverflow\.(\w+)""",
    ).find(titleBlock())?.groupValues?.get(1)

    private fun titleBlock(): String = Regex(
        """Text\(\s*text\s*=\s*articleTitle,[\s\S]*?\n\s{12}\)""",
    ).find(source())?.value.orEmpty()

    private fun actionTargetDp(): Float? = Regex(
        """\binternal val ArticleRowMinimumTarget\s*=\s*Dp\(([0-9.]+)f\)""",
    ).find(source())?.groupValues?.get(1)?.toFloat()
        ?: Regex("""\.heightIn\(min\s*=\s*([0-9.]+)\.dp\)""")
            .find(source())?.groupValues?.get(1)?.toFloat()

    private fun articleRowSignature(): String = source()
        .substringAfter("fun ArticleRow(", missingDelimiterValue = "")
        .substringBefore(") {", missingDelimiterValue = "")
        .let { body -> if (body.isEmpty()) "" else "fun ArticleRow($body) {" }
        .replace(Regex("""\s+"""), " ")

    private fun source(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/ArticleRow.kt",
    )

    private fun readLaterSource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/readlater/ReadLaterScreen.kt",
    )

    private fun historySource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/history/HistoryScreen.kt",
    )

    private fun sourceFile(relativePath: String): String = Path.of(relativePath).readText()
}
