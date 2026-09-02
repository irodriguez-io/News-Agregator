package io.irodriguez.intentionalreading.ui.components

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleCardTest {
    @Test
    fun `the headline text clamps at three lines and ellipsises`() {
        val headline = textCall("text = article.title")

        assertEquals(3, maxLines(headline), "headline maxLines")
        assertEquals("Ellipsis", assignedMember(headline, "overflow"), "headline overflow")
    }

    @Test
    fun `the excerpt text clamps at two lines and ellipsises`() {
        val excerpt = textCall("text = article.excerpt")

        assertEquals(2, maxLines(excerpt), "excerpt maxLines")
        assertEquals("Ellipsis", assignedMember(excerpt, "overflow"), "excerpt overflow")
    }

    @Test
    fun `the deck card uses the authored primary card shape`() {
        assertEquals("primaryCard", assignedScale(articleCardSurface(), "shape", "shapes"))
    }

    @Test
    fun `the deck card uses the card fill`() {
        assertEquals("card", assignedScale(articleCardSurface(), "color", "tokens"))
    }

    @Test
    fun `the deck card shadow uses the theme shadow tint for both shadow channels`() {
        val surface = articleCardSurface()

        assertTrue(
            surface.contains("ambientColor = MaterialTheme.colorScheme.surfaceTint"),
            "ambient shadow tint was absent",
        )
        assertTrue(
            surface.contains("spotColor = MaterialTheme.colorScheme.surfaceTint"),
            "spot shadow tint was absent",
        )
    }

    @Test
    fun `the headline is the authored editorial headline style`() {
        assertEquals("headlineLarge", assignedMember(textCall("text = article.title"), "style"))
    }

    @Test
    fun `an empty excerpt is omitted without placeholder copy`() {
        val body = functionSource("ArticleCard")

        assertTrue(body.contains("if (article.excerpt.isNotEmpty())"))
        assertFalse(body.contains("No description available"))
    }

    @Test
    fun `the badge preserves the content type label and uses badge roles`() {
        val metadata = functionSource("ArticleMetadata")

        assertTrue(metadata.contains("article.contentType.label.uppercase(Locale.ROOT)"))
        assertEquals("badge", assignedScale(metadata, "shape", "shapes"))
        assertEquals("primarySoft", assignedScale(metadata, "color", "tokens"))
        assertEquals("primary", assignedScale(metadata, "contentColor", "tokens"))
    }

    @Test
    fun `unknown reading time and publication age are omitted`() {
        val metadata = functionSource("ArticleMetadata")

        assertTrue(metadata.contains("if (publicationAge.isNotEmpty()) MetadataText(publicationAge)"))
        assertTrue(metadata.contains("if (readingTime.isNotEmpty()) MetadataText(readingTime)"))
        assertFalse(metadata.contains("?: 0"))
    }

    @Test
    fun `tags are capped neutral outlined pills without interaction`() {
        val tags = functionSource("TopicTags")

        assertTrue(tags.contains("article.tags.take(5)"), "tag cap was not five")
        assertTrue(tags.contains("tokens.outlineVariant"), "neutral decorative outline was absent")
        assertTrue(tags.contains("shapes.pill"), "authored pill shape was absent")
        assertFalse(Regex("\\b(?:clickable|selectable|toggleable)\\s*\\(").containsMatchIn(tags))
        assertFalse(Regex("\\bcolor\\s*=\\s*tokens\\.").containsMatchIn(tags), "tags carried a fill")
    }

    @Test
    fun `the card reserves no image or media region`() {
        val body = functionSource("ArticleCard")

        listOf("Image(", "AsyncImage(", "thumbnail", "mediaPlaceholder").forEach { forbidden ->
            assertFalse(body.contains(forbidden), "reserved media marker found: $forbidden")
        }
    }

    @Test
    fun `the slice owned card source names no colour radius or font literal`() {
        val ownedSource = buildString {
            append(functionSource("ArticleCard"))
            append(functionSource("ArticleMetadata"))
            append(functionSource("MetadataText"))
            append(functionSource("TopicTags"))
        }
        val forbidden = listOf(
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bCircleShape\b"""),
            Regex("""\bFont(?:Family|Style|Weight)\s*\("""),
            Regex("""\b\d+(?:\.\d+)?\.sp\b"""),
        )

        forbidden.forEach { pattern ->
            assertFalse(
                pattern.containsMatchIn(ownedSource),
                "Forbidden literal matched ${pattern.pattern}",
            )
        }
    }

    private fun maxLines(textCall: String): Int {
        val assigned = Regex("""\bmaxLines\s*=\s*([A-Za-z0-9_.]+)""")
            .find(textCall)
            ?.groupValues
            ?.get(1)
            ?: return Int.MAX_VALUE
        return assigned.toIntOrNull() ?: namedInt(assigned)
    }

    private fun namedInt(name: String): Int = Regex(
        """\b(?:private\s+|internal\s+)?const\s+val\s+$name\s*=\s*(\d+)""",
    ).find(source)?.groupValues?.get(1)?.toInt()
        ?: error("No integer value was declared for $name")

    private fun assignedMember(block: String, property: String): String? = Regex(
        """\b$property\s*=\s*(?:[A-Za-z0-9_]+\.)+([A-Za-z0-9_]+)""",
    ).find(block)?.groupValues?.get(1)

    private fun assignedScale(block: String, property: String, scale: String): String? = Regex(
        """\b$property\s*=\s*$scale\.([A-Za-z0-9_]+)""",
    ).find(block)?.groupValues?.get(1)

    private fun articleCardSurface(): String {
        val body = functionSource("ArticleCard")
        val start = body.indexOf("Surface(")
        check(start >= 0) { "ArticleCard has no Surface" }
        return balancedBlock(body, start + "Surface".length, '(', ')')
    }

    private fun textCall(marker: String): String {
        val body = functionSource("ArticleCard")
        val markerIndex = body.indexOf(marker)
        check(markerIndex >= 0) { "No text element carries $marker" }
        val start = body.lastIndexOf("Text(", markerIndex)
        check(start >= 0) { "No Text call carries $marker" }
        return balancedBlock(body, start + "Text".length, '(', ')')
    }

    private fun functionSource(name: String): String {
        val signature = "fun $name("
        val start = source.indexOf(signature)
        check(start >= 0) { "No function named $name" }
        val parametersStart = source.indexOf('(', start)
        val parametersEnd = balancedEnd(source, parametersStart, '(', ')')
        val bodyStart = source.indexOf('{', parametersEnd + 1)
        check(bodyStart >= 0) { "$name has no body" }
        return balancedBlock(source, bodyStart, '{', '}')
    }

    private fun balancedBlock(text: String, start: Int, open: Char, close: Char): String {
        val end = balancedEnd(text, start, open, close)
        return text.substring(start, end + 1)
    }

    private fun balancedEnd(text: String, start: Int, open: Char, close: Char): Int {
        check(text[start] == open) { "Expected $open at $start" }
        var depth = 0
        for (index in start until text.length) {
            when (text[index]) {
                open -> depth += 1
                close -> {
                    depth -= 1
                    if (depth == 0) return index
                }
            }
        }
        error("Unbalanced $open$close block")
    }

    private val source: String by lazy {
        Path.of(
            "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/ArticleCard.kt",
        ).readText()
    }
}
