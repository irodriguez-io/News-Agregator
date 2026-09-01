package io.irodriguez.intentionalreading.ui.components

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun assignedToken(property: String): String {
        val source = Path.of(
            "src/main/kotlin/io/irodriguez/intentionalreading/ui/components/BottomNavigationBar.kt",
        ).readText()
        return requireNotNull(
            Regex("""\b$property\s*=\s*tokens\.(\w+)""").find(source),
        ) { "No token assignment found for $property" }.groupValues[1]
    }
}
