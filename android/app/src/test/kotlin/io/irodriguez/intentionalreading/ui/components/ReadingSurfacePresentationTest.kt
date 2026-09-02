package io.irodriguez.intentionalreading.ui.components

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingSurfacePresentationTest {
    @Test
    fun `the StatBand uses the authored pill container roles`() {
        assertEquals("statBand", statBandSurfaceRole("shape"), "StatBand shape role")
        assertEquals("container", statBandSurfaceRole("color"), "StatBand fill role")
    }

    @Test
    fun `the StatBand keeps all three values in equal columns`() {
        assertEquals(3, statBandSource().split("Modifier.weight(1f)").size - 1)
        assertFalse(statBandSource().contains("HorizontalDivider"))
    }

    @Test
    fun `the StatBand uses the functional label and editorial numeral slots`() {
        assertEquals("labelMedium", statStyleFor("stat.label.uppercase(Locale.ROOT)"))
        assertEquals("displayMedium", statStyleFor("stat.value"))
    }

    @Test
    fun `the screen header uses the new type scale`() {
        assertEquals("labelMedium", headerStyleFor("eyebrow.uppercase(Locale.ROOT)"))
        assertEquals("displayLarge", headerStyleFor("title"))
        assertEquals("bodyMedium", headerStyleFor("description"))
    }

    @Test
    fun `the empty state has full expressive visual weight without changing its copy inputs`() {
        assertEquals("primaryCard", emptyStateSurfaceRole("shape"), "Empty-state shape role")
        assertEquals("container", emptyStateSurfaceRole("color"), "Empty-state fill role")
        assertTrue(emptyStateSource().contains("FilledPrimaryControl("))
        listOf(
            "text = title",
            "text = copy",
            "Text(actionLabel)",
        ).forEach { binding ->
            assertTrue(emptyStateSource().contains(binding), "Missing unchanged empty-state binding: $binding")
        }
    }

    @Test
    fun `the reading screens use the authored spacing scale and separate rows by the stack rhythm`() {
        listOf(readLaterScreenSource(), historyScreenSource()).forEach { source ->
            assertTrue(source.contains("horizontal = spacing.mobileMargin"))
            assertTrue(source.contains("top = spacing.tabletMargin"))
            assertTrue(source.contains("Arrangement.spacedBy(spacing.stackGap)"))
        }
    }

    @Test
    fun `the two composable contracts and displayed inputs stay unchanged`() {
        assertEquals(
            "fun StatBand(stats: List<StatItem>, modifier: Modifier = Modifier) {",
            composableSignature(statBandSource(), "StatBand"),
        )
        assertEquals(
            "fun EditorialHeader( eyebrow: String, title: String, description: String, " +
                "modifier: Modifier = Modifier, actionLabel: String? = null, " +
                "onAction: (() -> Unit)? = null, supportingContent: @Composable ColumnScope.() -> Unit = {}, ) {",
            composableSignature(editorialHeaderSource(), "EditorialHeader"),
        )

        listOf(
            "text = stat.label.uppercase(Locale.ROOT)",
            "text = stat.value",
            "text = eyebrow.uppercase(Locale.ROOT)",
            "text = title",
            "text = description",
            "Text(actionLabel)",
            "supportingContent()",
        ).forEach { binding ->
            assertTrue(combinedSource().contains(binding), "Missing unchanged display binding: $binding")
        }
    }

    @Test
    fun `the omission formatters remain byte exact`() {
        val expected = """
            internal fun knownReadingTimeValue(minutes: Int): String =
                if (minutes > 0) "~${'$'}minutes min" else "Unavailable"

            internal fun availableStatValue(value: String?): String = value?.takeIf { it.isNotEmpty() } ?: "Unavailable"
        """.trimIndent()

        assertEquals(
            expected,
            statBandSource().substringAfter("internal fun knownReadingTimeValue").let {
                "internal fun knownReadingTimeValue$it"
            }.trimEnd(),
        )
    }

    @Test
    fun `the presentation sources name no colour radius dimension or font literal`() {
        val forbidden = listOf(
            Regex("""\b\d+(?:\.\d+)?\.(?:dp|sp)\b"""),
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\b"""),
        )

        forbidden.forEach { pattern ->
            assertFalse(
                pattern.containsMatchIn(allPresentationSource()),
                "Forbidden literal matched ${pattern.pattern}",
            )
        }
    }

    private fun emptyStateSurfaceRole(property: String): String? = Regex(
        """Surface\(\s*modifier\s*=\s*modifier\.fillMaxWidth\(\),[\s\S]*?\b$property\s*=\s*(?:shapes|tokens)\.(\w+)""",
    ).find(emptyStateSource())?.groupValues?.get(1)

    private fun statBandSurfaceRole(property: String): String? = Regex(
        """Surface\(\s*modifier\s*=\s*modifier\.fillMaxWidth\(\),[\s\S]*?\b$property\s*=\s*(?:shapes|tokens)\.(\w+)""",
    ).find(statBandSource())?.groupValues?.get(1)

    private fun statStyleFor(textBinding: String): String? = textStyleFor(statBandSource(), textBinding)

    private fun headerStyleFor(textBinding: String): String? = textStyleFor(editorialHeaderSource(), textBinding)

    private fun textStyleFor(source: String, textBinding: String): String? = Regex(
        """Text\(\s*text\s*=\s*${Regex.escape(textBinding)},[\s\S]*?style\s*=\s*MaterialTheme\.typography\.(\w+)""",
    ).find(source)?.groupValues?.get(1)

    private fun composableSignature(source: String, name: String): String = source
        .substringAfter("fun $name(", missingDelimiterValue = "")
        .substringBefore(") {", missingDelimiterValue = "")
        .let { body -> if (body.isEmpty()) "" else "fun $name($body) {" }
        .replace(Regex("""\s+"""), " ")

    private fun combinedSource(): String = statBandSource() + editorialHeaderSource()

    private fun allPresentationSource(): String = combinedSource() +
        emptyStateSource() +
        readLaterScreenSource() +
        historyScreenSource()

    private fun statBandSource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/StatBand.kt",
    )

    private fun editorialHeaderSource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/EditorialHeader.kt",
    )

    private fun emptyStateSource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/EmptyStatePanel.kt",
    )

    private fun readLaterScreenSource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/readlater/ReadLaterScreen.kt",
    )

    private fun historyScreenSource(): String = sourceFile(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/history/HistoryScreen.kt",
    )

    private fun sourceFile(relativePath: String): String = Path.of(relativePath).readText()
}
