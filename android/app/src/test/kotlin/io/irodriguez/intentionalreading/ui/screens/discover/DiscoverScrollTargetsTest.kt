package io.irodriguez.intentionalreading.ui.screens.discover

import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoverScrollTargetsTest {
    @Test
    fun `a card that fits entirely within the viewport yields no scroll`() {
        assertEquals(
            0,
            DiscoverScrollTargets.revealCardActions(
                cardBottomOffset = 600,
                viewportHeight = 640,
                maxValue = 1_000,
            ),
        )
    }

    @Test
    fun `a card taller than the viewport aligns its bottom edge with the viewport`() {
        assertEquals(
            260,
            DiscoverScrollTargets.revealCardActions(
                cardBottomOffset = 900,
                viewportHeight = 640,
                maxValue = 1_000,
            ),
        )
    }

    @Test
    fun `a target beyond the content clamps to the maximum scroll value`() {
        assertEquals(
            700,
            DiscoverScrollTargets.revealCardActions(
                cardBottomOffset = 1_800,
                viewportHeight = 640,
                maxValue = 700,
            ),
        )
    }

    @Test
    fun `a card bottom above the viewport never produces a negative scroll`() {
        assertEquals(
            0,
            DiscoverScrollTargets.revealCardActions(
                cardBottomOffset = 320,
                viewportHeight = 640,
                maxValue = 1_000,
            ),
        )
    }
}
