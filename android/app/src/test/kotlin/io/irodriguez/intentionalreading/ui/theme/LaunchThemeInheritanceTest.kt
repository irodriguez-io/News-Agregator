package io.irodriguez.intentionalreading.ui.theme

import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.inputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.w3c.dom.Element

class LaunchThemeInheritanceTest {
    @Test
    fun `API 31 launch theme extends the single source of shared theme items`() {
        val baseResources = stylesIn("src/main/res/values/themes.xml")
        val api31Resources = stylesIn("src/main/res/values-v31/themes.xml")

        val sharedTheme = baseResources.requiredStyle(SHARED_THEME)
        assertEquals(MATERIAL_PARENT, sharedTheme.parent)
        assertEquals(SHARED_ITEMS, sharedTheme.items)

        val baseTheme = baseResources.requiredStyle(APP_THEME)
        assertEquals(SHARED_THEME, baseTheme.parent)
        assertTrue(baseTheme.items.isEmpty(), "$APP_THEME must not duplicate shared theme items")

        val api31Theme = api31Resources.requiredStyle(APP_THEME)
        assertEquals(SHARED_THEME, api31Theme.parent)
        assertEquals(mapOf(SPLASH_BACKGROUND to LAUNCH_BACKGROUND), api31Theme.items)
    }

    private fun stylesIn(path: String): Map<String, ParsedStyle> {
        val document = Path.of(path).inputStream().use { input ->
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
        }
        val styleNodes = document.getElementsByTagName("style")
        return buildMap {
            for (index in 0 until styleNodes.length) {
                val style = styleNodes.item(index) as? Element
                    ?: fail("$path contains a non-element style node")
                val name = style.getAttribute("name")
                if (name.isBlank()) fail("$path contains a style without a name")
                put(
                    name,
                    ParsedStyle(
                        parent = style.getAttribute("parent"),
                        items = style.items(path, name),
                    ),
                )
            }
        }
    }

    private fun Element.items(path: String, styleName: String): Map<String, String> {
        val itemNodes = getElementsByTagName("item")
        return buildMap {
            for (index in 0 until itemNodes.length) {
                val item = itemNodes.item(index) as? Element
                    ?: fail("$path contains a non-element item node in $styleName")
                val name = item.getAttribute("name")
                if (name.isBlank()) fail("$path contains an item without a name in $styleName")
                put(name, item.textContent.trim())
            }
        }
    }

    private fun Map<String, ParsedStyle>.requiredStyle(name: String): ParsedStyle =
        get(name) ?: fail("Expected style $name was not found")

    private data class ParsedStyle(
        val parent: String,
        val items: Map<String, String>,
    )

    private companion object {
        const val APP_THEME = "Theme.IntentionalReading"
        const val SHARED_THEME = "Theme.IntentionalReading.Base"
        const val MATERIAL_PARENT = "android:style/Theme.Material.Light.NoActionBar"
        const val SPLASH_BACKGROUND = "android:windowSplashScreenBackground"
        const val LAUNCH_BACKGROUND = "@color/launch_background"

        val SHARED_ITEMS = mapOf(
            "android:windowActionModeOverlay" to "true",
            "android:windowBackground" to LAUNCH_BACKGROUND,
            "android:windowNoTitle" to "true",
        )
    }
}
