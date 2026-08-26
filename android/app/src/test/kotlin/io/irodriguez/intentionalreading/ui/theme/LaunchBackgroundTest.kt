package io.irodriguez.intentionalreading.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class LaunchBackgroundTest {
    @Test
    fun `the launch background equals the composed background`() {
        val lightLaunchBackground = launchBackground("src/main/res/values/colors.xml")
        val darkLaunchBackground = launchBackground("src/main/res/values-night/colors.xml")

        assertEquals(argbHex(lightTokens().bg), lightLaunchBackground)
        assertEquals(argbHex(darkTokens().bg), darkLaunchBackground)
    }

    private fun launchBackground(path: String): String {
        val contents = Path.of(path).readText()
        return launchBackgroundPattern.find(contents)?.groupValues?.get(1)?.uppercase()
            ?: fail("$path does not define launch_background as an eight-digit hex colour")
    }

    private fun argbHex(color: Color): String =
        "#%08X".format(color.toArgb().toLong() and 0xFFFFFFFFL)

    private companion object {
        val launchBackgroundPattern = Regex(
            """<color\s+name=["']launch_background["']\s*>(#[0-9A-Fa-f]{8})\s*</color>""",
        )
    }
}
