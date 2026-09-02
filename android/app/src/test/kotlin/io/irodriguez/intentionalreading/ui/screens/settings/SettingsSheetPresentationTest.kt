package io.irodriguez.intentionalreading.ui.screens.settings

import io.irodriguez.intentionalreading.ui.Destination
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsSheetPresentationTest {
    @Test
    fun `the modal consumes the 28 dp shape and dimming scrim tokens`() {
        assertEquals("shapes.modalSheet", modalArgument("shape"))
        assertEquals("tokens.backdrop", modalArgument("scrimColor"))
    }

    @Test
    fun `appearance choices are surface-card toggles`() {
        val appearanceChoices = source
            .substringAfter("Appearance.entries.forEach")
            .substringBefore("R.string.local_data")

        assertEquals("tokens.card", assignedValue(appearanceChoices, "color"))
        assertEquals("shapes.smallContainer", assignedValue(appearanceChoices, "shape"))
    }

    @Test
    fun `Settings remains a modal rather than a navigation destination`() {
        assertEquals(
            listOf(Destination.READ_LATER, Destination.DISCOVER, Destination.HISTORY),
            Destination.entries,
        )
    }

    private fun modalArgument(name: String): String? = assignedValue(
        source.substringAfter("ModalBottomSheet("),
        name,
    )

    private fun assignedValue(source: String, name: String): String? = Regex(
        """\b$name\s*=\s*([^,\n]+)""",
    ).find(source)?.groupValues?.get(1)?.trim()

    private val source: String = Path.of(
        "src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/settings/SettingsSheet.kt",
    ).readText()
}
