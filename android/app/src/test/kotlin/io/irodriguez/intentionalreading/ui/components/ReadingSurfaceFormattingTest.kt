package io.irodriguez.intentionalreading.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingSurfaceFormattingTest {
    @Test
    fun `queue positions are zero padded to two digits`() {
        assertEquals("Queue 01", queuePositionLabel(1))
        assertEquals("Queue 09", queuePositionLabel(9))
        assertEquals("Queue 10", queuePositionLabel(10))
    }

    @Test
    fun `known reading time never presents a bare zero`() {
        assertEquals("Unavailable", knownReadingTimeValue(0))
        assertEquals("Unavailable", knownReadingTimeValue(-1))
        assertEquals("~12 min", knownReadingTimeValue(12))
    }

    @Test
    fun `missing topic values are explicitly unavailable`() {
        assertEquals("Unavailable", availableStatValue(null))
        assertEquals("Unavailable", availableStatValue(""))
        assertEquals("OAuth", availableStatValue("OAuth"))
    }

    @Test
    fun `history group counts use singular and plural article labels`() {
        assertEquals("1 article", historyGroupCount(1))
        assertEquals("2 articles", historyGroupCount(2))
    }
}
