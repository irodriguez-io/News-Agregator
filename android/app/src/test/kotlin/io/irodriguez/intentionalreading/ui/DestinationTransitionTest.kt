package io.irodriguez.intentionalreading.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DestinationTransitionTest {
    @Test
    fun `slide direction follows all six ordered destination pairs`() {
        val expectedDirections = listOf(
            Triple(
                Destination.READ_LATER,
                Destination.DISCOVER,
                DestinationSlideDirection.FROM_RIGHT,
            ),
            Triple(
                Destination.READ_LATER,
                Destination.HISTORY,
                DestinationSlideDirection.FROM_RIGHT,
            ),
            Triple(
                Destination.DISCOVER,
                Destination.READ_LATER,
                DestinationSlideDirection.FROM_LEFT,
            ),
            Triple(
                Destination.DISCOVER,
                Destination.HISTORY,
                DestinationSlideDirection.FROM_RIGHT,
            ),
            Triple(
                Destination.HISTORY,
                Destination.READ_LATER,
                DestinationSlideDirection.FROM_LEFT,
            ),
            Triple(
                Destination.HISTORY,
                Destination.DISCOVER,
                DestinationSlideDirection.FROM_LEFT,
            ),
        )

        expectedDirections.forEach { (current, target, expected) ->
            assertEquals(
                expected,
                destinationSlideDirection(current, target),
                "$current -> $target",
            )
        }
    }
}
