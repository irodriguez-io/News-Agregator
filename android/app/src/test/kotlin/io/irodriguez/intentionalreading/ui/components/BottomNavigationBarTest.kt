package io.irodriguez.intentionalreading.ui.components

import androidx.compose.ui.unit.dp
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BottomNavigationBarTest {
    @Test
    fun `the active indicator uses the tonal role`() {
        assertEquals("tonal", assignedToken("indicatorColor"), "navigation indicator")
    }

    @Test
    fun `the selected icon uses the on-tonal role`() {
        assertEquals("onTonal", assignedToken("selectedIconColor"), "selected icon")
    }

    @Test
    fun `the active indicator and selected icon use the tonal pair`() {
        assertEquals(
            "tonal" to "onTonal",
            assignedToken("indicatorColor") to assignedToken("selectedIconColor"),
            "navigation indicator and selected icon",
        )
    }

    @Test
    fun `the bar uses the bottom-bar shape and meets both size floors`() {
        val source = bottomNavigationSource()

        assertEquals(54.dp, BottomNavigationMinimumHeight)
        assertEquals(48.dp, BottomNavigationMinimumTarget)
        assertTrue(source.contains(".heightIn(min = BottomNavigationMinimumHeight)"))
        assertTrue(source.contains(".clip(shapes.bottomBar)"))
        assertTrue(source.contains("minWidth = BottomNavigationMinimumTarget"))
        assertTrue(source.contains("minHeight = BottomNavigationMinimumTarget"))
    }

    @Test
    fun `destinations stay ordered and counts stay truthful`() {
        val source = bottomNavigationSource()
        val readLater = source.indexOf("destination = Destination.READ_LATER")
        val discover = source.indexOf("destination = Destination.DISCOVER")
        val history = source.indexOf("destination = Destination.HISTORY")

        assertTrue(readLater >= 0)
        assertTrue(readLater < discover && discover < history)
        assertTrue(source.contains("count = counts.readLater"))
        assertTrue(source.contains("count = null"))
        assertTrue(source.contains("count = counts.history"))
    }

    @Test
    fun `selection owns the Material indicator as a non-colour state difference`() {
        val source = bottomNavigationSource()

        assertTrue(source.contains("selected = destination == selectedDestination"))
        assertNotEquals("transparent", assignedToken("indicatorColor"))
    }

    @Test
    fun `all destinations stay on one baseline`() {
        assertFalse(bottomNavigationSource().contains(".offset("))
    }

    @Test
    fun `the bar source names no colour radius or font literal`() {
        val source = bottomNavigationSource()
        val forbidden = listOf(
            Regex("""\bColor\s*\("""),
            Regex("""\bRoundedCornerShape\s*\("""),
            Regex("""\bFont(?:Family|Style|Weight)\s*\("""),
        )

        forbidden.forEach { pattern ->
            assertFalse(pattern.containsMatchIn(source), "Forbidden literal matched ${pattern.pattern}")
        }
    }

    private fun assignedToken(property: String): String {
        return requireNotNull(
            Regex("""\b$property\s*=\s*tokens\.(\w+)""").find(bottomNavigationSource()),
        ) { "No token assignment found for $property" }.groupValues[1]
    }

    private fun bottomNavigationSource(): String = Path.of(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/BottomNavigationBar.kt",
    ).readText()
}
