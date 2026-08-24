package io.irodriguez.intentionalreading.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceTest {
    @Test
    fun `appearance membership order and wire values are frozen`() {
        val expected = listOf(
            Appearance.LIGHT to "light",
            Appearance.DARK to "dark",
            Appearance.SYSTEM to "system",
        )

        assertEquals(expected.map { it.first }, Appearance.entries)
        assertEquals(expected.map { it.second }, Appearance.entries.map(Appearance::wireValue))
        expected.forEach { (appearance, wireValue) ->
            assertEquals(appearance, Appearance.fromWireValue(wireValue))
        }
    }
}
